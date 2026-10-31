package com.recherche.backend.indexation;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Exemples tirés de l'article de Porter (1980). */
class PorterStemmerTest {

    private final PorterStemmer stemmer = new PorterStemmer();

    private void verifier(String... pairesMotRacine) {
        for (int i = 0; i < pairesMotRacine.length; i += 2) {
            assertEquals(pairesMotRacine[i + 1], stemmer.raciner(pairesMotRacine[i]),
                    "raciner(\"" + pairesMotRacine[i] + "\")");
        }
    }

    @Test
    void etape1a() {
        verifier("caresses", "caress", "ponies", "poni", "ties", "ti", "caress", "caress", "cats", "cat");
    }

    @Test
    void etape1b() {
        verifier("feed", "feed", "agreed", "agre", "plastered", "plaster", "motoring", "motor",
                "sing", "sing", "conflated", "conflat", "troubled", "troubl", "sized", "size",
                "hopping", "hop", "tanned", "tan", "falling", "fall", "hissing", "hiss",
                "fizzed", "fizz", "failing", "fail", "filing", "file");
    }

    @Test
    void etape1c() {
        verifier("happy", "happi", "sky", "sky");
    }

    @Test
    void etape2() {
        verifier("relational", "relat", "conditional", "condit", "rational", "ration",
                "digitizer", "digit", "radicalli", "radic", "differentli", "differ",
                "vietnamization", "vietnam", "predication", "predic", "operator", "oper",
                "feudalism", "feudal", "decisiveness", "decis", "hopefulness", "hope",
                "callousness", "callous", "formaliti", "formal", "sensitiviti", "sensit",
                "sensibiliti", "sensibl");
    }

    @Test
    void etape3() {
        verifier("triplicate", "triplic", "formative", "form", "formalize", "formal",
                "electriciti", "electr", "electrical", "electr", "hopeful", "hope", "goodness", "good");
    }

    @Test
    void etape4() {
        verifier("revival", "reviv", "allowance", "allow", "inference", "infer", "airliner", "airlin",
                "gyroscopic", "gyroscop", "adjustable", "adjust", "defensible", "defens",
                "irritant", "irrit", "replacement", "replac", "adjustment", "adjust",
                "dependent", "depend", "adoption", "adopt", "communism", "commun",
                "activate", "activ", "angulariti", "angular", "homologous", "homolog",
                "effective", "effect", "bowdlerize", "bowdler");
    }

    @Test
    void etape5() {
        verifier("probate", "probat", "rate", "rate", "cease", "ceas", "controll", "control", "roll", "roll");
    }

    @Test
    void vocabulaireRechercheInformation() {
        verifier("retrieval", "retriev", "retrieving", "retriev", "information", "inform",
                "indexing", "index", "systems", "system", "documents", "document",
                "engines", "engin", "generalizations", "gener", "is", "is", "a", "a");
    }
}
