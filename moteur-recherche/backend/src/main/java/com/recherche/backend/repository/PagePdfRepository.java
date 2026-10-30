package com.recherche.backend.repository;

import com.recherche.backend.model.PagePdf;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface PagePdfRepository extends MongoRepository<PagePdf, String> {
    List<PagePdf> findByDocId(String docId);
    void deleteByDocId(String docId);
}