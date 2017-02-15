package edu.yeditepe.repository;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.data.mongodb.repository.MongoRepository;

import edu.yeditepe.model.ZemberekAPIModel;

@EnableAutoConfiguration
public interface MongoZemberekRepository extends
		MongoRepository<ZemberekAPIModel, String> {

	ZemberekAPIModel findByFunctionAndInput(String function, String input);

}
