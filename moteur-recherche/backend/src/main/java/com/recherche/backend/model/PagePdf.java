package com.recherche.backend.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "pages")
@Data
public class PagePdf {

    @Id
    private String id;

    @Indexed
    private String docId;               // à quel PDF appartient la page

    private String nomFichier;
    private int numeroPage;
    private String contenu;             // texte extrait de la page

    private int nombreTermes;           // nombre de termes après analyse (longueur de la page),
                                        // utilisé plus tard pour normaliser les scores TF-IDF
}
