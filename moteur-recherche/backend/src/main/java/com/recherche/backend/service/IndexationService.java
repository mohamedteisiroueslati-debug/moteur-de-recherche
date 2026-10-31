package com.recherche.backend.service;

import com.recherche.backend.indexation.AnalyseurTexte;
import com.recherche.backend.model.PagePdf;
import com.recherche.backend.model.Posting;
import com.recherche.backend.model.Stats;
import com.recherche.backend.model.Vocabulaire;
import com.recherche.backend.repository.DocPdfRepository;
import com.recherche.backend.repository.PagePdfRepository;
import com.recherche.backend.repository.PostingRepository;
import com.recherche.backend.repository.StatsRepository;
import com.recherche.backend.repository.VocabulaireRepository;
import lombok.RequiredArgsConstructor;
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

    private final AnalyseurTexte analyseur;
    private final DocPdfRepository docRepo;
    private final PagePdfRepository pageRepo;
    private final VocabulaireRepository vocabulaireRepo;
    private final PostingRepository postingRepo;
    private final StatsRepository statsRepo;

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

        // ⑦ stockage : on remplace l'ancien index
        postingRepo.deleteAll();
        vocabulaireRepo.deleteAll();
        enregistrerParLots(vocabulaire, vocabulaireRepo::saveAll);
        enregistrerParLots(postings, postingRepo::saveAll);
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

    private static <T> void enregistrerParLots(List<T> elements, java.util.function.Consumer<List<T>> sauver) {
        for (int i = 0; i < elements.size(); i += TAILLE_LOT) {
            sauver.accept(elements.subList(i, Math.min(i + TAILLE_LOT, elements.size())));
        }
    }
}
