package com.recherche.backend.repository;

import com.recherche.backend.model.Vocabulaire;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface VocabulaireRepository extends MongoRepository<Vocabulaire, String> {
    Optional<Vocabulaire> findByTerme(String terme);
}
