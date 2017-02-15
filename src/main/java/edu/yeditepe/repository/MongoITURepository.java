package edu.yeditepe.repository;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.data.mongodb.repository.MongoRepository;

import edu.yeditepe.model.ITUAPIModel;

@EnableAutoConfiguration
public interface MongoITURepository extends
		MongoRepository<ITUAPIModel, String> {

	ITUAPIModel findByFunctionAndInput(String function, String input);

}
