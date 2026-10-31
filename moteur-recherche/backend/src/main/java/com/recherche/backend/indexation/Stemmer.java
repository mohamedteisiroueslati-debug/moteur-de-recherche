package com.recherche.backend.indexation;

/** Racinisation d'un mot (étape 4.4). */
public interface Stemmer {

    /** @return la racine du mot (déjà normalisé, en minuscules) */
    String raciner(String mot);
}
