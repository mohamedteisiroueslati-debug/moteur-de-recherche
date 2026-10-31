package com.recherche.backend.config;

import com.recherche.backend.indexation.AnalyseurTexte;
import com.recherche.backend.indexation.EnglishStopWords;
import com.recherche.backend.indexation.PorterStemmer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Choix de la langue de l'analyseur. Pour le français plus tard :
 * new AnalyseurTexte(new FrenchStopWords(), new FrenchStemmer()).
 */
@Configuration
public class IndexationConfig {

    @Bean
    public AnalyseurTexte analyseurTexte() {
        return new AnalyseurTexte(new EnglishStopWords(), new PorterStemmer());
    }
}
