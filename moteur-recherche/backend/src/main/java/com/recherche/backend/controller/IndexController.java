package com.recherche.backend.controller;

import com.recherche.backend.indexation.AnalyseurTexte;
import com.recherche.backend.model.Posting;
import com.recherche.backend.model.Stats;
import com.recherche.backend.repository.PostingRepository;
import com.recherche.backend.repository.VocabulaireRepository;
import com.recherche.backend.service.IndexationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/index")
@RequiredArgsConstructor
public class IndexController {

    private final IndexationService indexationService;
    private final AnalyseurTexte analyseur;
    private final VocabulaireRepository vocabulaireRepo;
    private final PostingRepository postingRepo;

    /** Reconstruit tout l'index à partir des pages en base. */
    @PostMapping("/reconstruire")
    public Stats reconstruire() {
        return indexationService.reconstruire();
    }

    /** Statistiques de l'index (N, taille du vocabulaire, ...). */
    @GetMapping("/stats")
    public ResponseEntity<Stats> stats() {
        Stats s = indexationService.stats();
        return s == null ? ResponseEntity.notFound().build() : ResponseEntity.ok(s);
    }

    /** Montre chaque étape de l'analyse. Ex. : /api/index/analyser?texte=The systems are indexing */
    @GetMapping("/analyser")
    public AnalyseurTexte.Detail analyser(@RequestParam String texte) {
        return analyseur.detailler(texte);
    }

    /** Entrée du fichier inverse pour un mot (le mot est d'abord analysé). Ex. : /api/index/terme/retrieval */
    @GetMapping("/terme/{mot}")
    public ResponseEntity<Map<String, Object>> terme(@PathVariable String mot) {
        List<String> termes = analyseur.analyser(mot);
        if (termes.isEmpty()) return ResponseEntity.notFound().build();   // mot vide ou invalide
        String terme = termes.get(0);

        Map<String, Object> r = new LinkedHashMap<>();
        r.put("mot", mot);
        r.put("terme", terme);
        r.put("df", vocabulaireRepo.findByTerme(terme).map(v -> v.getDf()).orElse(0));
        List<Posting> postings = postingRepo.findByTerme(terme);
        r.put("postings", postings);
        return ResponseEntity.ok(r);
    }
}
