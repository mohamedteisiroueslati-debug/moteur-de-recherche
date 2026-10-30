package com.recherche.backend.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "documents_pdf")
@Data
public class DocPdf {

    @Id
    private String id;

    private String nomFichier;

    @Indexed(unique = true)
    private String empreinte;           // empreinte SHA-256 du fichier, évite les doublons

    private int nombrePages;
    private String fichierId;           // identifiant du PDF original dans GridFS
    private LocalDateTime dateIndexation;
}