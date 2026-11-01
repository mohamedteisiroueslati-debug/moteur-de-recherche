package com.recherche.backend.service;

import com.mongodb.MongoNamespace;
import com.mongodb.client.model.RenameCollectionOptions;
import com.recherche.backend.indexation.AnalyseurTexte;
import com.recherche.backend.model.PagePdf;
import com.recherche.backend.model.Posting;
import com.recherche.backend.model.Stats;
import com.recherche.backend.model.Vocabulaire;
import com.recherche.backend.repository.DocPdfRepository;
import com.recherche.backend.repository.PagePdfRepository;
import com.recherche.backend.repository.StatsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.IndexOperations;
import org.springframework.data.mongodb.core.index.IndexResolver;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Étapes ④ à ⑦ du pipeline : construit le fichier inverse à partir du texte des pages.
 *
 *   ④ analyse       : AnalyseurTexte (tokenisation, normalisation, mots vides, Porter)
 *   ⑤ pondération   : tf de chaque terme dans chaque page, N = nombre de pages
 *   ⑥ inversion     : terme -> liste de (page, tf), df = taille de la liste
 *   ⑦ stockage      : collections vocabulaire, postings, stats
 *
 * L'index est reconstruit entièrement à partir de la collection "pages" :
 * GridFS n'est pas relu, donc on peut changer l'analyseur et réindexer rapidement.
 */
@Service
@RequiredArgsConstructor
public class IndexationService {

    private static final int TAILLE_LOT = 10_000;   // insertion en base par lots
    private static final String SUFFIXE_TMP = "_tmp";

    private final AnalyseurTexte analyseur;
    private final DocPdfRepository docRepo;
    private final PagePdfRepository pageRepo;
    private final StatsRepository statsRepo;
    private final MongoTemplate mongo;

    /** Reconstruit tout l'index. Les pages sont chargées en mémoire (suffisant pour un corpus de cours). */
    public synchronized Stats reconstruire() {
        long debut = System.currentTimeMillis();

        Map<String, List<Posting>> index = new HashMap<>();   // le fichier inverse en mémoire
        List<PagePdf> pages = pageRepo.findAll();
        long nombrePages = 0;

        for (PagePdf page : pages) {
            // ④ analyse
            List<String> termes = analyseur.analyser(page.getContenu());
            page.setNombreTermes(termes.size());
            if (termes.isEmpty()) continue;
            nombrePages++;

            // ⑤ pondération : tf de chaque terme dans la page
            Map<String, Integer> tf = new HashMap<>();
            for (String terme : termes) {
                tf.merge(terme, 1, Integer::sum);
            }

            // ⑥ inversion : on ajoute la page à la liste de chaque terme
            for (Map.Entry<String, Integer> e : tf.entrySet()) {
                Posting p = new Posting();
                p.setTerme(e.getKey());
                p.setDocId(page.getDocId());
                p.setPageId(page.getId());
                p.setNumeroPage(page.getNumeroPage());
                p.setTf(e.getValue());
                index.computeIfAbsent(e.getKey(), k -> new ArrayList<>()).add(p);
            }
        }

        // ⑥ df = nombre de pages contenant le terme
        List<Vocabulaire> vocabulaire = new ArrayList<>(index.size());
        List<Posting> postings = new ArrayList<>();
        for (Map.Entry<String, List<Posting>> e : index.entrySet()) {
            Vocabulaire v = new Vocabulaire();
            v.setTerme(e.getKey());
            v.setDf(e.getValue().size());
            vocabulaire.add(v);
            postings.addAll(e.getValue());
        }

        // ⑦ stockage : le nouvel index est écrit à côté de l'ancien, puis échangé.
        //    Pendant la reconstruction, les recherches utilisent toujours l'ancien index ;
        //    en cas d'erreur, l'ancien index reste intact.
        String vocabulaireTmp = preparerCollectionTemporaire(Vocabulaire.class);
        String postingsTmp = preparerCollectionTemporaire(Posting.class);
        enregistrerParLots(vocabulaire, lot -> mongo.insert(lot, vocabulaireTmp));
        enregistrerParLots(postings, lot -> mongo.insert(lot, postingsTmp));
        remplacerCollection(vocabulaireTmp, Vocabulaire.class);
        remplacerCollection(postingsTmp, Posting.class);
        enregistrerParLots(pages, pageRepo::saveAll);          // met à jour nombreTermes

        Stats stats = new Stats();
        stats.setNombrePages(nombrePages);
        stats.setNombreDocuments(docRepo.count());
        stats.setNombreTermes(vocabulaire.size());
        stats.setNombrePostings(postings.size());
        stats.setDateIndexation(LocalDateTime.now());
        stats.setDureeMs(System.currentTimeMillis() - debut);
        statsRepo.save(stats);

        System.out.printf("Indexation : %d pages, %d termes, %d postings en %d ms%n",
                nombrePages, vocabulaire.size(), postings.size(), stats.getDureeMs());
        return stats;
    }

    public boolean indexVide() {
        return !statsRepo.existsById(Stats.ID_GLOBAL);
    }

    public Stats stats() {
        return statsRepo.findById(Stats.ID_GLOBAL).orElse(null);
    }

    /**
     * Crée une collection temporaire vide (ex. "postings_tmp") avec les mêmes index que la
     * collection finale. Une éventuelle collection laissée par une reconstruction interrompue est supprimée.
     */
    private String preparerCollectionTemporaire(Class<?> type) {
        String tmp = mongo.getCollectionName(type) + SUFFIXE_TMP;
        mongo.dropCollection(tmp);
        mongo.createCollection(tmp);
        IndexOperations indexOps = mongo.indexOps(tmp);
        IndexResolver.create(mongo.getConverter().getMappingContext())
                .resolveIndexFor(type)
                .forEach(indexOps::createIndex);
        return tmp;
    }

    /** Renomme la collection temporaire en collection finale ; l'ancienne est supprimée (opération atomique). */
    private void remplacerCollection(String tmp, Class<?> type) {
        MongoNamespace cible = new MongoNamespace(mongo.getDb().getName(), mongo.getCollectionName(type));
        mongo.getCollection(tmp).renameCollection(cible, new RenameCollectionOptions().dropTarget(true));
    }

    private static <T> void enregistrerParLots(List<T> elements, java.util.function.Consumer<List<T>> sauver) {
        for (int i = 0; i < elements.size(); i += TAILLE_LOT) {
            sauver.accept(elements.subList(i, Math.min(i + TAILLE_LOT, elements.size())));
        }
    }
}
