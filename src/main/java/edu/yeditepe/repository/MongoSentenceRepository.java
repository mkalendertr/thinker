package edu.yeditepe.repository;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.data.mongodb.repository.MongoRepository;

import edu.yeditepe.model.Sentence;

@EnableAutoConfiguration
public interface MongoSentenceRepository extends
		MongoRepository<Sentence, String> {

	public Sentence findByOrignalText(String orignalText);

}
