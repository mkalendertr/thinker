package edu.yeditepe.deep;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.Reader;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.log4j.Logger;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.internal.LinkedTreeMap;
import com.google.gson.reflect.TypeToken;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;

import edu.yeditepe.model.Entity;
import edu.yeditepe.nlp.TurkishNLP;
import edu.yeditepe.repository.MONGODB;
import edu.yeditepe.repository.MYSQL;

public class EntityEmbeddingAutoencoderMongo {
	private static final Logger LOGGER = Logger
			.getLogger(EntityEmbeddingAutoencoderMongo.class);
	private static int vectorSize = 50;
	private static LinkedTreeMap<String, double[]> vectors;

	public static void main(String[] args) throws FileNotFoundException {

		try {

			Reader freader = new FileReader("word2vec150.json");
			Gson gson = new GsonBuilder().create();
			HashMap<String, List<Double>> word2vec = gson.fromJson(freader,
					HashMap.class);

			freader = new FileReader("autoencoder300.json");
			gson = new GsonBuilder().create();
			HashMap<String, List<Double>> autoencoder = gson.fromJson(freader,
					HashMap.class);
			LOGGER.info("embeddings are loaded");

			DBCollection entitiesDB = MONGODB
					.getCollection(Entity.COLLECTION_NAME);
			DBCursor cursor = entitiesDB.find();
			long counter = 0;
			while (cursor.hasNext()) {
				try {
					DBObject object = cursor.next();
					Entity e = convertJSONToPojo(object.toString());
					String id = String.valueOf(e.getId());
					if (id.startsWith("w")) {
						id = id.substring(1);
						String url = MYSQL.getTRTitleById(id);
						List<Double> e1 = word2vec.get(id);
						List<Double> e2 = autoencoder.get(id);
						object.put(Entity.URL, url);
						object.put(Entity.SEMANTICEMBEDDINGAUTOENCODER, e2);
						object.put(Entity.SEMANTICEMBEDDINGWORD2VEC, e1);
						entitiesDB.save(object);
					}
				} catch (Exception exp) {
					LOGGER.info(exp);
				}
				// LOGGER.info(desc);
			}
			LOGGER.info(counter);
		} catch (Exception e) {
			// TODO: handle exception
		}
	}

	private static Entity convertJSONToPojo(String json) {

		Type type = new TypeToken<Entity>() {
		}.getType();

		return new Gson().fromJson(json, type);

	}

	private static double[] getWordVector(String word) {
		double[] rawVector = null;
		rawVector = vectors.get(word);

		if (rawVector == null) {
			rawVector = vectors.get(TurkishNLP.toLowerCase(word));
		}
		if (rawVector != null) {
			return rawVector;
		}
		return null;
		// TODO Auto-generated method stub

	}

	private static double[] getAverageVector(Object[] objects) {
		double[] avg = new double[vectorSize];
		List<double[]> vectors = new ArrayList<double[]>();
		for (int i = 0; i < objects.length; i++) {
			double[] wordVector = getWordVector((String) objects[i]);
			if (wordVector != null) {
				vectors.add(wordVector);
			}
		}
		if (!vectors.isEmpty()) {
			for (int i = 0; i < avg.length; i++) {
				for (int j = 0; j < vectors.size(); j++) {
					avg[i] += (double) vectors.get(j)[i];
				}
				avg[i] /= vectors.size();
			}
			return avg;
		} else {
			return null;
		}

	}
}
