package edu.yeditepe.deep;

import java.io.FileNotFoundException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;

import edu.yeditepe.lucene.Word2VecSearchEngine;
import edu.yeditepe.model.Entity;
import edu.yeditepe.nlp.Zemberek;
import edu.yeditepe.repository.MONGODB;

public class DescriptionEmbeddingAverage {
	private static final Logger LOGGER = Logger
			.getLogger(DescriptionEmbeddingAverage.class);
	private static int vectorSize = 100;

	public static void main(String[] args) throws FileNotFoundException {

		DBCollection entitiesDB = MONGODB.getCollection(Entity.COLLECTION_NAME);
		DBCursor cursor = entitiesDB.find();
		long counter = 0;
		while (cursor.hasNext()) {
			try {
				DBObject object = cursor.next();
				Entity e = convertJSONToPojo(object.toString());
				String title = e.getTitle();
				// if (title.equals("tapıklamak")) {
				// LOGGER.info("");
				// }
				String desc = e.getDescription();
				List<Set<String>> words = Zemberek.getInstance()
						.disambiguateForEmbedding(desc);
				Set<String> nouns = words.get(0);
				Set<String> verbs = words.get(1);
				Set<String> others = words.get(2);

				double[] nounAverage = getAverageVector(nouns.toArray());
				double[] verbAverage = getAverageVector(verbs.toArray());
				double[] otherAverage = getAverageVector(others.toArray());
				double[] v1 = new double[vectorSize * 3];
				for (int i = 0; i < vectorSize; i++) {
					v1[i] = nounAverage[i];
					v1[i + vectorSize] = verbAverage[i];
					v1[i + vectorSize + vectorSize] = otherAverage[i];
				}
				object.put(Entity.DESCRIPTIONEMBEDDINGAVERAGE, v1);
				// e.setDescriptionEmbeddingAverage(average);
				entitiesDB.save(object);
			} catch (Exception exp) {
				LOGGER.info(exp);
			}
			// LOGGER.info(desc);
		}
		LOGGER.info(counter);

	}

	public static double[] getAverageVector(String text) {
		List<Set<String>> words = Zemberek.getInstance()
				.disambiguateForEmbedding(text);
		Set<String> nouns = words.get(0);
		Set<String> verbs = words.get(1);
		Set<String> others = words.get(2);

		double[] nounAverage = getAverageVector(nouns.toArray());
		double[] verbAverage = getAverageVector(verbs.toArray());
		double[] otherAverage = getAverageVector(others.toArray());
		double[] v1 = new double[vectorSize * 3];
		for (int i = 0; i < vectorSize; i++) {
			v1[i] = nounAverage[i];
			v1[i + vectorSize] = verbAverage[i];
			v1[i + vectorSize + vectorSize] = otherAverage[i];
		}
		return v1;

	}

	public static List<Double> getAverageVectorList(String text) {
		try {
			List<Set<String>> words = Zemberek.getInstance()
					.disambiguateForEmbedding(text);
			Set<String> nouns = words.get(0);
			Set<String> verbs = words.get(1);
			Set<String> others = words.get(2);

			List<Double> nounAverage = getAverageVectorList(nouns.toArray());
			List<Double> verbAverage = getAverageVectorList(verbs.toArray());
			List<Double> otherAverage = getAverageVectorList(others.toArray());
			List<Double> v1 = new ArrayList<Double>(vectorSize * 3);
			v1.addAll(nounAverage);
			v1.addAll(verbAverage);
			v1.addAll(otherAverage);
			return v1;
		} catch (Exception e) {
			// TODO: handle exception
		}
		return null;

	}

	private static Entity convertJSONToPojo(String json) {

		Type type = new TypeToken<Entity>() {
		}.getType();

		return new Gson().fromJson(json, type);

	}

	private static double[] getAverageVector(Object[] objects) {
		Word2VecSearchEngine word2vec = Word2VecSearchEngine.getInstance();
		double[] avg = new double[vectorSize];
		try {
			List<List<Double>> vectors = new ArrayList<List<Double>>();
			for (int i = 0; i < objects.length; i++) {
				List<Double> wordVector = word2vec
						.getWordVector((String) objects[i]);
				if (wordVector != null) {
					vectors.add(wordVector);
				}
			}
			if (!vectors.isEmpty()) {
				for (int i = 0; i < avg.length; i++) {
					for (int j = 0; j < vectors.size(); j++) {
						avg[i] += vectors.get(j).get(i);
					}
					avg[i] /= vectors.size();
				}
				return avg;
			}
		} catch (Exception e) {
			// TODO: handle exception
		}
		return avg;

	}

	private static List<Double> getAverageVectorList(Object[] objects) {
		Word2VecSearchEngine word2vec = Word2VecSearchEngine.getInstance();
		List<Double> avg = new ArrayList<Double>(vectorSize);
		for (int j = 0; j < vectorSize; j++) {
			avg.add(0d);
		}
		try {
			List<List<Double>> vectors = new ArrayList<List<Double>>();
			for (int i = 0; i < objects.length; i++) {
				List<Double> wordVector = word2vec
						.getWordVector((String) objects[i]);
				if (wordVector != null) {
					vectors.add(wordVector);
				}
			}
			if (!vectors.isEmpty()) {
				for (int i = 0; i < avg.size(); i++) {
					for (int j = 0; j < vectors.size(); j++) {
						avg.set(i, avg.get(i) + vectors.get(j).get(i));
					}
					avg.set(i, avg.get(i) / vectors.size());
				}
				return avg;
			}
		} catch (Exception e) {
			// TODO: handle exception
		}
		return avg;

	}
}
