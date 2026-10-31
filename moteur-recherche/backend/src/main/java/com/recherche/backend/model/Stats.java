package com.recherche.backend.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

/** Statistiques globales de l'index (un seul enregistrement, id = "global"). */
@Document(collection = "stats")
@Data
public class Stats {

    public static final String ID_GLOBAL = "global";

    @Id
    private String id = ID_GLOBAL;

    private long nombrePages;           // N, utilisé pour idf = log(N / df)
    private long nombreDocuments;
    private long nombreTermes;          // taille du vocabulaire
    private long nombrePostings;
    private LocalDateTime dateIndexation;
    private long dureeMs;               // durée de la dernière indexation
}
