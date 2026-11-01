package com.recherche.backend.repository;

import com.recherche.backend.model.Posting;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface PostingRepository extends MongoRepository<Posting, String> {
    List<Posting> findByTerme(String terme);
    List<Posting> findByTermeIn(List<String> termes);
    void deleteByDocId(String docId);
}
