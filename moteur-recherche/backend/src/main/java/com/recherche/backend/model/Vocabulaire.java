package com.recherche.backend.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

/** Une entrée du vocabulaire : un terme d'indexation et son df. */
@Document(collection = "vocabulaire")
@Data
public class Vocabulaire {

    @Id
    private String id;

    @Indexed(unique = true)
    private String terme;               // ex. "retriev"

    private int df;                     // nombre de pages qui contiennent le terme
}
