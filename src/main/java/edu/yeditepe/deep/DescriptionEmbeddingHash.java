package edu.yeditepe.deep;

import java.io.File;
import java.io.FileNotFoundException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.apache.log4j.Logger;
import org.encog.engine.network.activation.ActivationTANH;
import org.encog.ml.data.basic.BasicMLData;
import org.encog.neural.networks.BasicNetwork;
import org.encog.persist.EncogDirectoryPersistence;

import pl.edu.pw.mini.msi.AutoEncoder2;
import pl.edu.pw.mini.msi.FileManager;
import pl.edu.pw.mini.msi.NetworkFactory;
import pl.edu.pw.mini.msi.Trainer;
import pl.edu.pw.mini.msi.Visualizer;
import vectorspace.CosineSimilarity;

import com.google.gson.Gson;
import com.google.gson.internal.LinkedTreeMap;
import com.google.gson.reflect.TypeToken;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;

import edu.yeditepe.deep.autoencoder.AutoEncoder;
import edu.yeditepe.model.Entity;
import edu.yeditepe.nlp.TurkishNLP;
import edu.yeditepe.nlp.Zemberek;
import edu.yeditepe.repository.MONGODB;
import edu.yeditepe.utils.Property;

public class DescriptionEmbeddingHash {
	private static final Logger LOGGER = Logger
			.getLogger(DescriptionEmbeddingHash.class);
	private static int hashsize = 2;
	private static LinkedTreeMap<String, double[]> vectors;

	public static void main(String[] args) throws FileNotFoundException {
		// FileReader reader = new FileReader("discovery_model.json");
		// Gson gson = new GsonBuilder().create();
		// vectors = new Gson().fromJson(reader,
		// new TypeToken<LinkedTreeMap<String, double[]>>() {
		// }.getType());

		// hashVector();
		// autoencoder();
		// BasicNetwork network = load("DESCRIPTIONEMBEDDINGHASH.model");
		// DBCollection entitiesDB =
		// MONGODB.getCollection(Entity.COLLECTION_NAME);
		// DBCursor cursor = entitiesDB.find();
		// while (cursor.hasNext()) {
		// DBObject object = cursor.next();
		// Entity e = convertJSONToPojo(object.toString());
		// List<Double> hashVector = e.getDescriptionHashVector();
		// BasicMLData d1 = new BasicMLData(convertListToArray(hashVector));
		// // double[] e1 = cs.normalize(network.compute(d1).getData());
		// double[] e1 = network.compute(d1).getData();
		// // embeddings.put(object, e1);
		// object.put(Entity.DESCRIPTIONEMBEDDINGHASH, e1);
		// entitiesDB.save(object);
		// // LOGGER.info(e1[0]);
		// }
		autoencoder2();

	}

	private static void hashVector() {

		DBCollection entitiesDB = MONGODB.getCollection(Entity.COLLECTION_NAME);
		DBCursor cursor = entitiesDB.find();
		int counter = 0;
		TreeSet<String> allWords = new TreeSet<String>();
		HashMap<DBObject, Set<String>> entityWords = new HashMap<DBObject, Set<String>>();
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
				words.addAll(Zemberek.getInstance().disambiguateForEmbedding(
						title));
				Set<String> all = new HashSet<String>();
				all.addAll(words.get(0));
				all.addAll(words.get(1));
				all.addAll(words.get(2));
				allWords.addAll(all);
				entityWords.put(object, all);
				// break;

				// double[] average = getAverageVector(words.toArray());
				// if (average == null) {
				// counter++;
				// LOGGER.info(title + " " + desc);
				// entitiesDB.remove(object);
				// }
				// object.put(Entity.DESCRIPTIONEMBEDDINGAVERAGE, average);
				// entitiesDB.save(object);
			} catch (Exception exp) {
				LOGGER.info(exp);
			}
			// LOGGER.info(desc);
		}
		LOGGER.info("Words size = " + allWords.size());
		// LOGGER.info(counter);

		HashMap<String, Integer> gramIndex = new HashMap<String, Integer>();
		HashMap<String, Set<Integer>> allWordsGrams = new HashMap<String, Set<Integer>>();
		counter = 0;
		for (String word : allWords) {
			if (word.length() >= 1) {
				Set<Integer> wordGrams = new HashSet<Integer>();
				String w = "#" + word + "#";
				// LOGGER.info(word);
				// if (word.equals("#1412#") || word.equals("#1142#")) {
				// LOGGER.info(word);
				// }
				for (int i = 0; i < w.length() - hashsize + 1; i++) {
					String e = w.substring(i, i + hashsize);
					if (!gramIndex.containsKey(e)) {
						gramIndex.put(e, counter++);
						// LOGGER.info(e);
					}
					wordGrams.add(gramIndex.get(e));
				}
				allWordsGrams.put(word, wordGrams);
			}
		}
		LOGGER.info("Unique grams " + gramIndex.size());
		HashMap<DBObject, double[]> entityHashVectors = new HashMap<DBObject, double[]>();
		for (DBObject object : entityWords.keySet()) {
			double[] hashVector = new double[gramIndex.size()];
			Set<String> words = entityWords.get(object);
			for (String word : words) {
				Set<Integer> wordGrams = allWordsGrams.get(word);
				for (Integer index : wordGrams) {
					hashVector[index] = 1;
				}
			}
			object.put(Entity.DESCRIPTIONGHASHVECTOR, hashVector);
			entitiesDB.save(object);
			// entityHashVectors.put(object, hashVector);
		}
		allWordsGrams = null;
		entityWords = null;
		gramIndex = null;

	}

	private static void autoencoder() {
		DBCollection entitiesDB = MONGODB.getCollection(Entity.COLLECTION_NAME);
		DBCursor cursor = entitiesDB.find();
		LOGGER.info("pretraining");
		AutoEncoder encoder = new AutoEncoder();

		int interval = Integer.parseInt(Property.getInstance().get(
				"autoencoder.interval"));
		int i = 0;
		while (cursor.hasNext()) {
			DBObject object = cursor.next();
			Entity e = convertJSONToPojo(object.toString());
			List<Double> hashVector = e.getDescriptionHashVector();
			if (i++ % interval == 0) {
				encoder.addData(convertListToArray(hashVector));

			}
			if (i % 10000 == 0) {
				LOGGER.info(i);
			}
		}

		encoder.addLayer(new ActivationTANH(), 600);
		LOGGER.info("layer 150");
		encoder.addLayer(new ActivationTANH(), 150);
		BasicNetwork network = encoder.getNetwork(2);
		HashMap<DBObject, double[]> embeddings = new HashMap<DBObject, double[]>();
		CosineSimilarity cs = new CosineSimilarity();
		LOGGER.info("autoencoder finished");
		EncogDirectoryPersistence.saveObject(new File(
				"DESCRIPTIONEMBEDDINGHASH.model"), network);
		cursor = entitiesDB.find();
		while (cursor.hasNext()) {
			DBObject object = cursor.next();
			Entity e = convertJSONToPojo(object.toString());
			List<Double> hashVector = e.getDescriptionHashVector();
			BasicMLData d1 = new BasicMLData(convertListToArray(hashVector));
			// double[] e1 = cs.normalize(network.compute(d1).getData());
			double[] e1 = network.compute(d1).getData();
			// embeddings.put(object, e1);
			object.put(Entity.DESCRIPTIONEMBEDDINGHASH, e1);
			entitiesDB.save(object);
			// LOGGER.info(e1[0]);
		}

	}

	private static void autoencoder2() {
		DBCollection entitiesDB = MONGODB.getCollection(Entity.COLLECTION_NAME);
		DBCursor cursor = entitiesDB.find();
		boolean train = false;
		LOGGER.info("pretraining");
		Visualizer visualizer = new Visualizer();
		NetworkFactory networkFactory = new NetworkFactory();
		String outputDir = "output/";
		FileManager fileManager = new FileManager(outputDir, outputDir);
		Trainer trainer = new Trainer(0.01, 0.1, 10, 20);
		List<Integer> encoderHiddenLayersSizes = new ArrayList<Integer>();
		encoderHiddenLayersSizes.add(1200);
		encoderHiddenLayersSizes.add(600);
		encoderHiddenLayersSizes.add(300);
		double[][] trainingData = new double[7500][2102];
		double[][] validationData = new double[2500][2102];
		AutoEncoder2 autoEncoder = new AutoEncoder2(encoderHiddenLayersSizes,
				trainingData, validationData, networkFactory, trainer,
				visualizer, fileManager, "1");
		if (train) {

			int interval = 4;
			int training = 0, validation = 0, i = 0;
			while (cursor.hasNext()) {
				DBObject object = cursor.next();
				Entity e = convertJSONToPojo(object.toString());
				List<Double> hashVector = e.getDescriptionHashVector();
				if (i++ % interval != 3) {
					trainingData[training++] = convertListToArray(hashVector);
				} else {
					validationData[validation++] = convertListToArray(hashVector);
				}
				// if (i % 10000 == 0) {
				// LOGGER.info(i);
				// }
				if (i >= 10000) {
					break;
				}
			}

			autoEncoder.train();
			try {
				autoEncoder.save("DESCRIPTIONEMBEDDINGHASH2.model");
			} catch (Exception e) {
				// TODO: handle exception
			}
		} else {
			autoEncoder.load("DESCRIPTIONEMBEDDINGHASH2.model");
		}
		// EncogDirectoryPersistence.saveObject(new File(
		// "DESCRIPTIONEMBEDDINGHASH2.model"), autoEncoder);
		cursor = entitiesDB.find();
		while (cursor.hasNext()) {
			try {

				DBObject object = cursor.next();
				Entity e = convertJSONToPojo(object.toString());
				List<Double> hashVector = e.getDescriptionHashVector();
				BasicMLData d1 = new BasicMLData(convertListToArray(hashVector));
				double[] out = autoEncoder.compute(d1, 3, 300);
				// LOGGER.info(out);
				object.put(Entity.DESCRIPTIONEMBEDDINGHASH2, out);
				entitiesDB.save(object);
			} catch (Exception e) {
				// TODO: handle exception
			}
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

	private static double[] convertListToArray(List<Double> input) {
		double[] out = new double[input.size()];
		for (int i = 0; i < out.length; i++) {
			out[i] = input.get(i).doubleValue();
		}
		return out;
	}

	private static BasicNetwork load(String fileName) {
		LOGGER.info("Loading network");

		return (BasicNetwork) EncogDirectoryPersistence.loadObject(new File(
				fileName));
		// LOGGER.info("Network loaded");

	}
}
