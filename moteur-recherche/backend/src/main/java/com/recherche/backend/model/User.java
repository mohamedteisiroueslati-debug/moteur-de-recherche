package com.recherche.backend.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "users")
@Data
public class User {

    @Id
    private String id;

    private String nom;

    @Indexed(unique = true)
    private String email;               // unica: due account non possono avere la stessa email

    private String motDePasseHash;      // MAI la password in chiaro, solo l'hash BCrypt

    private String role = "USER";       // "USER" oppure "ADMIN"

    private boolean actif = true;       // permette di bloccare un utente

    private LocalDateTime dateCreation = LocalDateTime.now();
}