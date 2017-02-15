package edu.yeditepe.typeclassifier;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import com.google.common.collect.Multisets;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;

import de.bwaldvogel.liblinear.Feature;
import de.bwaldvogel.liblinear.FeatureNode;
import de.bwaldvogel.liblinear.Linear;
import de.bwaldvogel.liblinear.Model;
import de.bwaldvogel.liblinear.Parameter;
import de.bwaldvogel.liblinear.Problem;
import de.bwaldvogel.liblinear.SolverType;
import edu.yeditepe.lucene.Word2VecSearchEngine;
import edu.yeditepe.model.Entity;
import edu.yeditepe.model.EntityPage;
import edu.yeditepe.nlp.TurkishNLP;
import edu.yeditepe.nlp.Zemberek;
import edu.yeditepe.repository.MONGODB;
import edu.yeditepe.utils.FileUtils;
import edu.yeditepe.utils.Property;

public class TypeClassifier {
	private static final Logger LOGGER = Logger.getLogger(TypeClassifier.class);

	private static TypeClassifier instance = new TypeClassifier();

	private Map<String, Integer> typeIds2Levels = new HashMap<String, Integer>();

	private Map<String, String> typeIds2LevelsInverse = new HashMap<String, String>();

	private Map<String, Integer> typeIds1Level = new HashMap<String, Integer>();

	private Map<Integer, String> typeIds1LevelsInverse = new HashMap<Integer, String>();

	private Map<String, Integer> suffixIds = new HashMap<String, Integer>();

	private Map<String, String> typeParents = new HashMap<String, String>();

	private Set<String> firstLevel = new HashSet<String>();

	private Map<String, List<String>> typeChilds = new HashMap<String, List<String>>();

	private Map<String, Model> modelsMap = new HashMap<String, Model>();

	private static int featureSize = 565;

	private static int vectorSize = 100;

	private String folder = "type_classifier_data";

	private String modelFolder = "type_classifier_models";

	private static boolean twoLevels = Boolean.parseBoolean(Property
			.getInstance().get("typeclassifier.twolevel"));

	private static boolean probability = Boolean.parseBoolean(Property
			.getInstance().get("typeclassifier.probability"));

	private static boolean enabled = Boolean.parseBoolean(Property
			.getInstance().get("typeclassifier.enabled"));

	private TypeClassifier() {
		try {
			loadParentTypes();
			loadSuffixIds();
			loadModels();
			loadTypeIds();
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	public static void main(String[] args) {
		TypeClassifier tc = TypeClassifier.getInstance();
		// tc.typeHistogram();

		// tc.testDataCreation();
		if (args.length == 0) {
			// tc.testDataCreation();
			tc.createTrainingData();
		} else {
			if (twoLevels) {
				tc.train();
			} else {
				tc.train(args[0]);
			}

		}

	}

	private void loadModels() {

		try {
			File file = new File(modelFolder);
			File[] files = file.listFiles();

			for (int i = 0; i < files.length; i++) {
				String folder = files[i].getName();
				File fmodel = new File(modelFolder + File.separator + folder);
				Model model = Model.load(fmodel);
				modelsMap.put(folder.replace(".model", ""), model);
			}

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public double predict(EntityPage page, String title, String type,
			String sentence) {
		page.setPredictedType("");
		double sim = 0;

		try {
			double[] probs1 = new double[210];
			double[] probs2 = new double[210];
			FeatureNode[] featureVector = getFeatureVector(title, sentence);

			if (featureVector == null) {
				return 0;
			}
			if (twoLevels) {
				Model firstLevel = modelsMap.get("firstLevel");
				int prediction1 = (int) Linear.predictProbability(firstLevel,
						featureVector, probs1);
				String predicted1_type = typeIds1LevelsInverse.get(prediction1);
				Model model = modelsMap.get(typeParents.get(predicted1_type));
				int prediction2 = (int) Linear.predictProbability(model,
						featureVector, probs2);
				String predicted2_type = typeIds2LevelsInverse.get(prediction1
						+ "_" + prediction2);
				if (predicted2_type != null) {

					page.setPredictedType(predicted2_type);
				}
				if (type == null || !typeIds2Levels.containsKey(type)
						|| !enabled) {
					return 0;
				}
				int type1 = (int) typeIds2Levels.get(typeParents.get(type));
				int type2 = typeIds2Levels.get(type);
				// page.setPredictedType(typeParents.get(type));

				if (type1 == prediction1) {

					sim += (float) probs1[type1];

					sim += probs2[type2];
					if (probability) {
						return sim;
					} else if (prediction2 == type2) {
						return 1;
					} else {
						return 0;
					}
				} else {
					return 0;
				}
			} else {
				probs1 = new double[210];
				Model model = modelsMap.get("all");
				int prediction = (int) Linear.predictProbability(model,
						featureVector, probs1);
				String predicted1_type = typeIds1LevelsInverse.get(prediction);
				if (predicted1_type != null) {

					page.setPredictedType(predicted1_type);
				}
				if (type == null || !typeIds2Levels.containsKey(type)
						|| !enabled) {
					return 0;
				}
				if (probability) {
					return probs1[typeIds1Level.get(type)];
				} else if (prediction == typeIds1Level.get(type)) {
					return 1;
				}
				// if (index == prediction) {
				// return 1;
				// } else {
				// return 0.0;
				// }
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return sim;

	}

	public String predict(String title, String sentence) {
		double sim = 0;
		try {
			double[] probs = new double[210];

			FeatureNode[] featureVector = getFeatureVector(title, sentence);

			if (featureVector == null) {
				return "";
			}
			if (twoLevels) {
				Model firstLevelMap = modelsMap.get("firstLevel");
				double prediction1 = Linear.predictProbability(firstLevelMap,
						featureVector, probs);
				String type1 = null;
				for (String type : typeIds2Levels.keySet()) {
					if (typeIds2Levels.get(type) == prediction1
							&& firstLevel.contains(type)) {
						type1 = type;
						break;
					}
				}
				// System.out.println("First level type: \t"+type1);
				if (type1 != null) {
					Model model = modelsMap.get(type1);
					double prediction2 = Linear.predictProbability(model,
							featureVector, probs);
					// System.out.println("Prediction \t"+prediction);
					String type2 = null;
					for (String type : typeIds2Levels.keySet()) {
						try {
							if (typeIds2Levels.get(type) == prediction2
									&& typeParents.get(type).equals(type1)) {
								type2 = type;
								break;
							}
						} catch (Exception e) {
							// TODO: handle exception
						}

					}
					// System.out.println("Second level type: \t"+type2);
					return type2;
				}

			} else {
				probs = new double[210];
				Model model = modelsMap.get("all");
				int prediction = (int) Linear.predictProbability(model,
						featureVector, probs);
				String type1 = typeIds1LevelsInverse.get(prediction);
				return type1;

			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return "";

	}

	public void train(String folder) {

		try {
			File file = new File(folder);
			File[] files = file.listFiles();
			List<String> l1 = new ArrayList<String>();
			for (int i = 0; i < files.length; i++) {
				String f = files[i].getName();
				String type = f.replaceAll(".txt", "");
				l1.add(type);
				typeIds1Level.put(type, i);
			}
			train(l1, "all");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void train() {
		BufferedReader in;

		Map<String, ArrayList<String>> secondLevel = new HashMap<String, ArrayList<String>>();
		for (String type : typeParents.keySet()) {
			if (typeParents.containsKey(type)) {
				if (secondLevel.containsKey(typeParents.get(type))) {
					secondLevel.get(typeParents.get(type)).add(type);
				} else {
					ArrayList<String> l = new ArrayList<String>();
					l.add(type);
					secondLevel.put(typeParents.get(type), l);
				}
			} else {
				// firstLevel.add(type);
			}
		}

		System.out.println("First lvel");
		for (String s : firstLevel) {
			System.out.println(s);
		}
		List<String> l1 = new ArrayList<String>();
		for (String string : firstLevel) {
			l1.add(string);
		}
		train(l1, "firstLevel");

		System.out.println("Second level");
		for (String string : secondLevel.keySet()) {
			for (String s2 : secondLevel.get(string)) {

				System.out.println(string + "\t" + s2);
			}
			train(secondLevel.get(string), string);
		}

	}

	public void train(List<String> types, String name) {
		Gson gson = new GsonBuilder().create();

		try {
			Map<String, List<String>> fcontent = new HashMap<String, List<String>>();
			int trainingSize = 0;
			for (int i = 0; i < types.size(); i++) {
				String type = types.get(i);
				List<String> readFile = FileUtils.readFile(folder
						+ File.separator + type + ".txt");
				fcontent.put(type, readFile);
				trainingSize += readFile.size();
			}
			double[] yTraining = new double[trainingSize];
			Feature[][] xTraining = new Feature[trainingSize][];
			int counter = 0;
			int y = 0;
			for (String type : fcontent.keySet()) {
				List<String> readFile = fcontent.get(type);
				for (String s : readFile) {
					TypeClassifierFeature tf = gson.fromJson(s,
							TypeClassifierFeature.class);
					xTraining[counter] = tf.getFeatureVector();
					yTraining[counter++] = y;
				}
				typeIds2Levels.put(type, y);
				y++;

				readFile.clear();
				readFile = null;
			}
			System.gc();
			Problem problem = new Problem();
			problem.l = trainingSize;

			problem.n = featureSize;
			problem.x = xTraining;
			problem.y = yTraining;

			SolverType solver = SolverType.L2R_LR; // -s 0
			double C = 2.0; // cost of constraints violation
			double eps = 0.05; // stopping criteria

			Parameter parameter = new Parameter(solver, C, eps);
			Model model = Linear.train(problem, parameter);
			File modelFile = new File(modelFolder + File.separator + name
					+ ".model");
			model.save(modelFile);

			if (twoLevels) {
				Writer writer;
				try {
					writer = new FileWriter("typeIds2.json");
					gson.toJson(typeIds2Levels, writer);
					writer.close();
				} catch (IOException e2) {
					// TODO Auto-generated catch block
					e2.printStackTrace();
				}
			} else {
				Writer writer;
				try {
					writer = new FileWriter("typeIds1.json");
					gson.toJson(typeIds1Level, writer);
					writer.close();
				} catch (IOException e2) {
					// TODO Auto-generated catch block
					e2.printStackTrace();
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void testDataCreation() {
		Entity e = new Entity();
		e.setId("wtest");
		e.setType("futbolcu");
		e.setTitle("Yenişehir Barajı");
		List<String> s = new ArrayList<String>();
		s.add("Murat Kalender'in yeni geliştirdiği yazılım rekor kırdı");
		e.setSentences(s);
		createFeatureVector(e, "futbolcu");
	}

	public void createTrainingData() {
		DBCollection entitiesDB = MONGODB.getCollection(Entity.COLLECTION_NAME);
		DBCursor cursor = entitiesDB.find();
		while (cursor.hasNext()) {
			try {

				DBObject object = cursor.next();
				Entity e = convertJSONToPojo(object.toString());

				if (e.getId().startsWith("w")) {
					String type = e.getType();
					if (type == null || type.equals("")) {
						type = getInsideParanthesis(e.getUrl());
					}
					if (type == null || type.equals("")
							|| !typeIds2Levels.containsKey(type)) {
						continue;
					}
					if (e.getSentences() == null || e.getSentences().isEmpty()) {
						continue;
					}
					createFeatureVector(e, type);
				}

			} catch (Exception e) {
				// TODO: handle exception
			}
		}

	}

	public void createFeatureVector(Entity e, String type) {

		Gson gson = new GsonBuilder().create();
		List<String> sentences = e.getSentences();
		int senCounter = 0;
		for (String sentence : sentences) {
			try {
				List<Set<String>> words = Zemberek.getInstance()
						.disambiguateForEmbedding(sentence);
				Set<String> nouns = words.get(0);
				Set<String> verbs = words.get(1);
				Set<String> others = words.get(2);
				String title = e.getTitle();
				String[] split = title.split(" ");
				double[] titleAverage = getAverageVector(split);
				String title_morph = Zemberek.getInstance().getEntityLemma(
						split[split.length - 1]);
				List<Double> lastWord = getWordVector(title_morph);
				if (lastWord == null) {
					continue;
				}
				double[] nounAverage = getAverageVector(nouns.toArray());
				double[] verbAverage = getAverageVector(verbs.toArray());
				double[] otherAverage = getAverageVector(others.toArray());
				boolean lower = StringUtils.contains(sentence, e.getTitle()
						.toLowerCase());
				Set<String> suffixes = new HashSet<String>();

				suffixes.addAll(Zemberek.getInstance().getSuffix(sentence,
						e.getTitle()));

				double[] suffixVector = getSuffixVector(suffixes);
				FeatureNode[] featureVector = new FeatureNode[featureSize];
				int counter = 0;
				for (int i = 0; i < titleAverage.length; i++) {
					FeatureNode feature = new FeatureNode(counter + 1,
							titleAverage[i]);
					featureVector[counter++] = feature;
				}
				for (int i = 0; i < lastWord.size(); i++) {
					FeatureNode feature = new FeatureNode(counter + 1,
							lastWord.get(i));
					featureVector[counter++] = feature;
				}
				if (lower) {
					FeatureNode feature = new FeatureNode(counter + 1, 0);
					featureVector[counter++] = feature;
				} else {
					FeatureNode feature = new FeatureNode(counter + 1, 1);
					featureVector[counter++] = feature;
				}

				for (int i = 0; i < nounAverage.length; i++) {
					FeatureNode feature = new FeatureNode(counter + 1,
							nounAverage[i]);
					featureVector[counter++] = feature;
				}
				for (int i = 0; i < verbAverage.length; i++) {
					FeatureNode feature = new FeatureNode(counter + 1,
							verbAverage[i]);
					featureVector[counter++] = feature;
				}
				for (int i = 0; i < otherAverage.length; i++) {
					FeatureNode feature = new FeatureNode(counter + 1,
							otherAverage[i]);
					featureVector[counter++] = feature;
				}
				for (int i = 0; i < suffixVector.length; i++) {
					FeatureNode feature = new FeatureNode(counter + 1,
							suffixVector[i]);
					featureVector[counter++] = feature;
				}
				TypeClassifierFeature tf = new TypeClassifierFeature(
						featureVector, typeIds2Levels.get(type));
				String parent = typeParents.get(type);
				TypeClassifierFeature tfParent = new TypeClassifierFeature(
						featureVector, typeIds2Levels.get(parent));
				FileUtils
						.writeFile(gson.toJson(tf) + "\n",
								"type_classifier_data" + File.separator + type
										+ ".txt");
				if (senCounter == 0) {
					FileUtils.writeFile(gson.toJson(tfParent) + "\n",
							"type_classifier_data" + File.separator + parent
									+ ".txt");
				} else if (senCounter == 4) {
					break;
				}

				senCounter++;
			} catch (Exception e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		}

	}

	public FeatureNode[] getFeatureVector(String title, String sentence) {

		try {
			List<Set<String>> words = Zemberek.getInstance()
					.disambiguateForEmbedding(sentence);
			Set<String> nouns = words.get(0);
			Set<String> verbs = words.get(1);
			Set<String> others = words.get(2);
			String[] split = title.split(" ");
			double[] titleAverage = getAverageVector(split);
			String title_morph = Zemberek.getInstance().getEntityLemma(
					split[split.length - 1]);
			List<Double> lastWord = getWordVector(title_morph);
			if (lastWord == null) {
				return null;
			}
			double[] nounAverage = getAverageVector(nouns.toArray());
			double[] verbAverage = getAverageVector(verbs.toArray());
			double[] otherAverage = getAverageVector(others.toArray());
			boolean lower = StringUtils.contains(sentence, title.toLowerCase());
			Set<String> suffixes = new HashSet<String>();

			suffixes.addAll(Zemberek.getInstance().getSuffix(sentence, title));

			double[] suffixVector = getSuffixVector(suffixes);
			FeatureNode[] featureVector = new FeatureNode[featureSize];
			int counter = 0;
			for (int i = 0; i < titleAverage.length; i++) {
				FeatureNode feature = new FeatureNode(counter + 1,
						titleAverage[i]);
				featureVector[counter++] = feature;
			}
			for (int i = 0; i < lastWord.size(); i++) {
				FeatureNode feature = new FeatureNode(counter + 1,
						lastWord.get(i));
				featureVector[counter++] = feature;
			}
			if (lower) {
				FeatureNode feature = new FeatureNode(counter + 1, 0);
				featureVector[counter++] = feature;
			} else {
				FeatureNode feature = new FeatureNode(counter + 1, 1);
				featureVector[counter++] = feature;
			}

			for (int i = 0; i < nounAverage.length; i++) {
				FeatureNode feature = new FeatureNode(counter + 1,
						nounAverage[i]);
				featureVector[counter++] = feature;
			}
			for (int i = 0; i < verbAverage.length; i++) {
				FeatureNode feature = new FeatureNode(counter + 1,
						verbAverage[i]);
				featureVector[counter++] = feature;
			}
			for (int i = 0; i < otherAverage.length; i++) {
				FeatureNode feature = new FeatureNode(counter + 1,
						otherAverage[i]);
				featureVector[counter++] = feature;
			}
			for (int i = 0; i < suffixVector.length; i++) {
				FeatureNode feature = new FeatureNode(counter + 1,
						suffixVector[i]);
				featureVector[counter++] = feature;
			}
			return featureVector;
		} catch (Exception e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		return null;

	}

	public void loadTypeIds() {
		// List<String> ids = FileUtils.readFile("type_ids.txt");
		// for (String s : ids) {
		// String[] split = s.split("\t");
		// typeIds.put(split[0], Integer.parseInt(split[1]));
		// typeIdsInverse.put(Integer.parseInt(split[1]), split[0]);
		// }
		Reader reader;
		try {
			reader = new FileReader("typeIds2.json");
			Gson gson = new GsonBuilder().create();
			typeIds2Levels = gson.fromJson(reader,
					new TypeToken<Map<String, Integer>>() {
					}.getType());
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		try {
			reader = new FileReader("typeIds1.json");
			Gson gson = new GsonBuilder().create();
			typeIds1Level = gson.fromJson(reader,
					new TypeToken<Map<String, Integer>>() {
					}.getType());
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		for (String type : typeIds1Level.keySet()) {
			typeIds1LevelsInverse.put(typeIds1Level.get(type), type);
		}

		for (String type : typeIds2Levels.keySet()) {
			try {

				String parent_type = typeParents.get(type);
				int parent_id = (int) typeIds2Levels.get(parent_type);
				int type_id = typeIds2Levels.get(type);
				String id = parent_id + "_" + type_id;
				// if (typeIds2LevelsInverse.containsKey(id)) {
				// // LOGGER.info("");
				// }
				// LOGGER.info(id);
				typeIds2LevelsInverse.put(id, type);
			} catch (Exception e) {
				// TODO: handle exception
			}
		}

	}

	public void loadSuffixIds() {
		try {
			List<String> ids = FileUtils.readFile("suffix.txt");
			for (int i = 0; i < ids.size(); i++) {
				suffixIds.put(ids.get(i), i);
			}
		} catch (Exception e) {
			// TODO: handle exception
		}

	}

	public void loadParentTypes() {
		try {

			List<String> lines = FileUtils.readFile("parent_types.txt");
			for (String s : lines) {
				String[] split = s.split("\t");
				typeParents.put(split[0], split[1]);
				if (typeChilds.containsKey(split[1])) {
					typeChilds.get(split[1]).add(split[0]);
				} else {
					List<String> l = new ArrayList<String>();
					l.add(split[0]);
					typeChilds.put(split[1], l);
				}
				firstLevel.add(split[1]);
			}

		} catch (Exception e) {
			// TODO: handle exception
		}
	}

	public void typeHistogram() {
		Multiset<String> entityTypeFrequencyMultiset = HashMultiset.create();
		DBCollection entitiesDB = MONGODB.getCollection(Entity.COLLECTION_NAME);
		DBCursor cursor = entitiesDB.find();
		while (cursor.hasNext()) {
			try {

				DBObject object = cursor.next();
				Entity e = convertJSONToPojo(object.toString());
				if (e.getId().startsWith("w")) {
					if (e.getType() == null || e.getType().equals("")) {
						String url = getInsideParanthesis(e.getUrl());
						if (e.getType() != null && !e.getType().equals("")) {
							entityTypeFrequencyMultiset.add(url);
						}
					} else {
						entityTypeFrequencyMultiset.add(e.getType());

					}
				}

			} catch (Exception e) {
				// TODO: handle exception
			}
		}
		for (String type : Multisets.copyHighestCountFirst(
				entityTypeFrequencyMultiset).elementSet()) {
			LOGGER.info("\t" + type + "\t"
					+ entityTypeFrequencyMultiset.count(type));
		}
	}

	private static Entity convertJSONToPojo(String json) {

		Type type = new TypeToken<Entity>() {
		}.getType();

		return new Gson().fromJson(json, type);

	}

	public static String getInsideParanthesis(String str) {
		try {
			int firstBracket = str.indexOf('(');
			return str.substring(firstBracket + 1,
					str.indexOf(')', firstBracket));
		} catch (Exception e) {
			// TODO: handle exception
		}
		return "";

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

	private static List<Double> getWordVector(String word) {
		Word2VecSearchEngine word2vec = Word2VecSearchEngine.getInstance();
		try {
			List<Double> wordVector = word2vec.getWordVector(word);
			if (wordVector == null) {
				wordVector = word2vec.getWordVector(TurkishNLP
						.toLowerCase(word));
			}
			return word2vec.getWordVector(word);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
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

	private double[] getSuffixVector(Set<String> s) {
		double[] vector = new double[suffixIds.size()];
		try {
			for (String suffix : s) {
				vector[suffixIds.get(suffix)] = 1;
			}
		} catch (Exception e) {
			// TODO: handle exception
		}

		return vector;
	}

	public static TypeClassifier getInstance() {
		return instance;
	}

}
