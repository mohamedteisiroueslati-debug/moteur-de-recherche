package com.recherche.backend.repository;

import com.recherche.backend.model.Stats;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface StatsRepository extends MongoRepository<Stats, String> {
}
