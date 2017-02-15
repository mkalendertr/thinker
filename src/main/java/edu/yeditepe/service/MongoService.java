package edu.yeditepe.service;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Service;

import edu.yeditepe.TurkishTaggingApplication;
import edu.yeditepe.repository.MongoITURepository;
import edu.yeditepe.repository.MongoZemberekRepository;

@Service
public class MongoService {
	private static final Logger LOGGER = Logger.getLogger(TurkishTaggingApplication.class);

	@Autowired
	private MongoITURepository ITU;

	@Autowired
	private MongoZemberekRepository Zemberek;

	public MongoService() {
		LOGGER.info("Mongo service");
	}

	public MongoRepository getRepository(String repoName)
			throws IllegalArgumentException, IllegalAccessException,
			NoSuchFieldException, SecurityException {
		return (MongoRepository) this.getClass().getDeclaredField(repoName)
				.get(this);
	}
}
