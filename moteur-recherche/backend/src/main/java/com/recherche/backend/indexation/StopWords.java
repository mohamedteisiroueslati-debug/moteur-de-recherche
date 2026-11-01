package com.recherche.backend.indexation;

/** Liste de mots vides d'une langue (étape 4.3). */
public interface StopWords {

    /** @return true si le mot (déjà normalisé, en minuscules) est un mot vide */
    boolean estVide(String mot);
}
