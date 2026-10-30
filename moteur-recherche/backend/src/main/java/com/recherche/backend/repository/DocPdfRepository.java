package com.recherche.backend.repository;

import com.recherche.backend.model.DocPdf;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface DocPdfRepository extends MongoRepository<DocPdf, String> {
    Optional<DocPdf> findByEmpreinte(String empreinte);
}