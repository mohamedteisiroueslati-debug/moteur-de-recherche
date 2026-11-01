package com.recherche.backend.indexation;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AnalyseurTexteTest {

    private final AnalyseurTexte analyseur =
            new AnalyseurTexte(new EnglishStopWords(), new PorterStemmer());

    @Test
    void pipelineComplet() {
        assertEquals(List.of("inform", "retriev", "system", "index", "document"),
                analyseur.analyser("The information retrieval systems are indexing the documents."));
    }

    @Test
    void tokenisationEtNormalisation() {
        assertEquals(List.of("retrieval", "system", "index", "do", "they", "can", "cafe"),
                analyseur.tokeniser("Retrieval SYSTEM's index! Don't they? I can't... Café 2026"));
    }

    @Test
    void motCoupeEnFinDeLigne() {
        assertEquals(List.of("information", "retrieval"),
                analyseur.tokeniser("information retrie-\nval"));
    }

    @Test
    void motsComposesSepares() {
        assertEquals(List.of("full", "text", "search"), analyseur.tokeniser("full-text search"));
    }

    @Test
    void motsVidesSupprimes() {
        AnalyseurTexte.Detail d = analyseur.detailler("The index of the documents");
        assertEquals(List.of("the", "of", "the"), d.motsVides);
        assertEquals(List.of("index", "documents"), d.apresMotsVides);
        assertEquals(List.of("index", "document"), d.termes);
    }

    @Test
    void requeteEtDocumentDonnentLesMemesTermes() {
        // une requête "Retrieving information" doit retrouver une page "information retrieval"
        assertEquals(Set.copyOf(analyseur.analyser("Retrieving information")),
                Set.copyOf(analyseur.analyser("information retrieval")));
    }

    @Test
    void texteVide() {
        assertTrue(analyseur.analyser(null).isEmpty());
        assertTrue(analyseur.analyser("   ").isEmpty());
        assertTrue(analyseur.analyser("the of and 123 , ;").isEmpty());
    }
}
