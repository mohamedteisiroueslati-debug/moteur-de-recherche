package com.recherche.backend.indexation;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Étape ④ du pipeline : transforme un texte en liste de termes d'indexation.
 *
 *   4.1 tokenisation   : découpe le texte en mots
 *   4.2 normalisation  : minuscules, accents, apostrophes ('s, n't, ...)
 *   4.3 mots vides     : supprime the, of, and, ...
 *   4.4 racinisation   : retrieval -> retriev
 *
 * La MÊME instance doit être utilisée pour les documents et pour les requêtes,
 * sinon les termes de la requête ne correspondront pas à ceux de l'index.
 *
 * Classe sans dépendance à Spring : elle est créée dans IndexationConfig.
 */
public class AnalyseurTexte {

    static final int LONGUEUR_MIN = 2;    // on ignore les lettres isolées
    static final int LONGUEUR_MAX = 30;   // au-delà, c'est en général du bruit d'extraction PDF

    /** Un mot : des lettres, éventuellement avec des apostrophes internes (don't, system's). */
    private static final Pattern MOT = Pattern.compile("[a-z]+(?:'[a-z]+)*");

    /** Mot coupé en fin de ligne dans le PDF : "retrie-\nval" -> "retrieval". */
    private static final Pattern CESURE = Pattern.compile("(\\p{L})-[ \\t]*\\R\\s*(\\p{L})");

    /** Accents (marques diacritiques) après décomposition Unicode. */
    private static final Pattern ACCENTS = Pattern.compile("\\p{M}+");

    /** Terminaisons de contractions anglaises, retirées du mot (testées dans cet ordre). */
    private static final String[] CONTRACTIONS = {"n't", "'s", "'re", "'ve", "'ll", "'d", "'m"};

    private final StopWords stopWords;
    private final Stemmer stemmer;

    public AnalyseurTexte(StopWords stopWords, Stemmer stemmer) {
        this.stopWords = stopWords;
        this.stemmer = stemmer;
    }

    /** Pipeline complet : texte -> termes d'indexation (avec répétitions, dans l'ordre du texte). */
    public List<String> analyser(String texte) {
        List<String> termes = new ArrayList<>();
        for (String mot : tokeniser(texte)) {
            if (stopWords.estVide(mot)) continue;          // 4.3
            String racine = stemmer.raciner(mot);          // 4.4
            if (racine.length() >= LONGUEUR_MIN) termes.add(racine);
        }
        return termes;
    }

    /** Étapes 4.1 et 4.2 : texte -> mots normalisés. */
    public List<String> tokeniser(String texte) {
        List<String> mots = new ArrayList<>();
        if (texte == null || texte.isBlank()) return mots;

        String t = CESURE.matcher(texte).replaceAll("$1$2");
        t = Normalizer.normalize(t, Normalizer.Form.NFD);   // é -> e + accent
        t = ACCENTS.matcher(t).replaceAll("");               // retire l'accent
        t = t.toLowerCase(Locale.ROOT)
             .replace('’', '\'')                        // apostrophes typographiques ’ ‘
             .replace('‘', '\'');

        Matcher m = MOT.matcher(t);
        while (m.find()) {
            String mot = normaliserApostrophes(m.group());
            if (mot.length() >= LONGUEUR_MIN && mot.length() <= LONGUEUR_MAX) {
                mots.add(mot);
            }
        }
        return mots;
    }

    /** system's -> system, don't -> do, they're -> they, can't -> can */
    static String normaliserApostrophes(String mot) {
        if (mot.indexOf('\'') < 0) return mot;
        switch (mot) {
            case "can't": return "can";
            case "won't": return "will";
            case "shan't": return "shall";
            default: break;
        }
        for (String fin : CONTRACTIONS) {
            if (mot.endsWith(fin)) {
                mot = mot.substring(0, mot.length() - fin.length());
                break;
            }
        }
        return mot.replace("'", "");                         // o'neill -> oneill
    }

    /** Détail de chaque étape, pour vérifier ou montrer le fonctionnement de l'analyseur. */
    public Detail detailler(String texte) {
        Detail d = new Detail();
        d.tokens = tokeniser(texte);
        for (String mot : d.tokens) {
            if (stopWords.estVide(mot)) {
                d.motsVides.add(mot);
            } else {
                d.apresMotsVides.add(mot);
                String racine = stemmer.raciner(mot);
                if (racine.length() >= LONGUEUR_MIN) d.termes.add(racine);
            }
        }
        return d;
    }

    /** Résultat de detailler(). */
    public static class Detail {
        public List<String> tokens = new ArrayList<>();          // après 4.1 + 4.2
        public List<String> motsVides = new ArrayList<>();       // supprimés en 4.3
        public List<String> apresMotsVides = new ArrayList<>();  // après 4.3
        public List<String> termes = new ArrayList<>();          // après 4.4 (= analyser())
    }
}
