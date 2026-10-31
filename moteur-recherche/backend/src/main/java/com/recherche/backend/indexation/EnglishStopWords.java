package com.recherche.backend.indexation;

import java.util.Set;

/**
 * Mots vides anglais (base : liste NLTK + quelques ajouts).
 * Les mots sont écrits sans apostrophe car le tokeniseur les retire
 * ("don't" devient "do", "isn't" devient "is").
 */
public class EnglishStopWords implements StopWords {

    private static final Set<String> MOTS = Set.of(
            "a", "about", "above", "after", "again", "against", "all", "also", "am", "an", "and",
            "any", "are", "as", "at", "be", "because", "been", "before", "being", "below",
            "between", "both", "but", "by", "can", "could", "did", "do", "does", "doing", "down",
            "during", "each", "eg", "etc", "few", "for", "from", "further", "had", "has", "have",
            "having", "he", "her", "here", "hers", "herself", "him", "himself", "his", "how",
            "however", "ie", "if", "in", "into", "is", "it", "its", "itself", "just", "may", "me",
            "might", "more", "most", "must", "my", "myself", "no", "nor", "not", "now", "of",
            "off", "on", "once", "only", "or", "other", "our", "ours", "ourselves", "out", "over",
            "own", "same", "shall", "she", "should", "so", "some", "such", "than", "that", "the",
            "their", "theirs", "them", "themselves", "then", "there", "these", "they", "this",
            "those", "through", "thus", "to", "too", "under", "until", "up", "upon", "us", "very",
            "via", "was", "we", "were", "what", "when", "where", "which", "while", "who", "whom",
            "why", "will", "with", "would", "yet", "you", "your", "yours", "yourself", "yourselves"
    );

    @Override
    public boolean estVide(String mot) {
        return MOTS.contains(mot);
    }
}
