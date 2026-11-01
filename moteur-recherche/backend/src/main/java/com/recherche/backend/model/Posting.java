package com.recherche.backend.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Une ligne du fichier inverse : le terme apparaît tf fois dans cette page.
 * Une ligne par couple (terme, page), plutôt qu'une liste dans Vocabulaire,
 * car un document MongoDB est limité à 16 Mo.
 */
@Document(collection = "postings")
@Data
public class Posting {

    @Id
    private String id;

    @Indexed
    private String terme;               // clé de recherche : tous les postings d'un terme

    @Indexed
    private String docId;               // pour supprimer l'index d'un document

    private String pageId;
    private int numeroPage;
    private int tf;                     // fréquence du terme dans la page
}
