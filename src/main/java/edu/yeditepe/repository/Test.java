package edu.yeditepe.repository;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashSet;

import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.Mongo;
import com.mongodb.MongoException;

import edu.yeditepe.model.Entity;

public class Test {
	public static void main(String[] args) throws UnknownHostException,
			MongoException {

		Mongo mongo = new Mongo("localhost", 27017);
		DB db = mongo.getDB("videolization");

		Entity entity = new Entity();
		entity.setId("test1");
		entity.setTitle("Test");
		entity.setAlias(new HashSet<String>());
		entity.getAlias().add("t1");
		entity.getAlias().add("t2");

		entity.setDescription("desc");
		entity.setDomain("domain");
		entity.setLinks(new HashSet<String>());
		entity.getLinks().add("link1");
		entity.setLetterCase(0);
		// entity.setOutLinks(new HashSet<String>());
		// entity.getOutLinks().add("out1");
		entity.setRank(1);

		// entity.setDescriptionEmbeddingHash(new ArrayList<Double>());
		// entity.getDescriptionEmbeddingHash().add(2.2d);
		//
		// entity.setDescriptionEmbeddingAverage(new ArrayList<Double>());
		// entity.getDescriptionEmbeddingAverage().add(2.2d);
		//
		// entity.setSemanticEmbedding(new ArrayList<Double>());
		// entity.getSemanticEmbedding().add(3.2d);

		entity.setSentences(new ArrayList<String>());
		entity.getSentences().add("sentence");
		entity.setSource("source");
		entity.setSuffixes(new HashSet<String>());
		entity.getSuffixes().add("suffix");

		entity.setType("type");

		DBCollection employeeCollection = null;
		employeeCollection = db.getCollection(Entity.COLLECTION_NAME);

		employeeCollection.save(entity);

		System.err.println(employeeCollection.findOne());

	}
}
