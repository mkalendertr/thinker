package edu.yeditepe.nounphrase;

import it.cnr.isti.hpc.text.Token;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import de.bwaldvogel.liblinear.FeatureNode;
import de.bwaldvogel.liblinear.Linear;
import de.bwaldvogel.liblinear.Model;
import edu.yeditepe.lucene.Word2VecSearchEngine;
import edu.yeditepe.nlp.TurkishNLP;
import edu.yeditepe.nlp.Zemberek;
import edu.yeditepe.utils.FileUtils;

public class MySpotter {
	private static final Logger LOGGER = Logger.getLogger(MySpotter.class);
	private static MySpotter instance = new MySpotter();
	public static Model model;
	public static int vectorSize = 100;
	public static int suffixSize = 64;
	public static List<String> suffixes;
	public static Set<String> names;
	public static Set<String> tdk;
	public static List<Double> emptyWordVector;

	public static MySpotter getInstance() {
		return instance;
	}

	public static void main(String[] args) {
		LOGGER.info(MySpotter.getInstance().merge("Barack", "Obama", 1));
		LOGGER.info(MySpotter.getInstance().merge("Obama", "geldi", 2));
		LOGGER.info(MySpotter.getInstance().merge("gece", "kulübünde", 1));
		LOGGER.info(MySpotter.getInstance().merge("Rusya", "Devleti", 1));
		LOGGER.info(MySpotter.getInstance().merge("Sekreteri", "Jens", 1));
	}

	private MySpotter() {
		try {
			int vectorSize = 100;
			suffixSize = 64;
			File modelFile = new File("spotter_lr.model");
			model = Model.load(modelFile);
			emptyWordVector = new ArrayList<Double>(vectorSize);
			for (int i = 0; i < vectorSize; i++) {
				emptyWordVector.add(0d);
			}

			suffixes = FileUtils.readFile("suffix.txt");
			names = FileUtils.readFileSet("names.txt");
			tdk = FileUtils.readFileSet("tdk.txt");
			String specialChars = "/*!@#$%^&*()\"{}_[]|\\?/<>,.";
			Set<Character> charSet = new HashSet<Character>();
			for (char c : specialChars.toCharArray()) {
				charSet.add(c);
			}
		} catch (Exception e) {
			LOGGER.error(e);
		}

	}

	public double merge(String word1, String word2, int length) {
		List<Double> f1 = getFeatureVector(word1);
		List<Double> f2 = getFeatureVector(word2);
		if (f1 != null && f2 != null) {
			if (f1.get(0) == 0 && f2.get(0) != 0) {
				return 0;
			}
			ArrayList<Double> featureVector = new ArrayList<Double>();
			featureVector.add((double) length);
			featureVector.addAll(f1);
			featureVector.addAll(f2);
			FeatureNode[] features = new FeatureNode[featureVector.size()];
			for (int j = 0; j < features.length; j++) {
				features[j] = new FeatureNode(j + 1, featureVector.get(j));
			}
			double[] prob_estimates = new double[2];
			Linear.predictProbability(model, features, prob_estimates);
			// double predict = Linear.predict(model, features);
			return prob_estimates[0];
		}

		return -1;

	}

	public static List<Double> getFeatureVector(String word) {
		if (word.length() < 2) {
			return null;
		}
		List<Token> disambiguateFindTokens = Zemberek.getInstance()
				.disambiguateFindTokens(word, false, false);
		double lc = 0;
		if (StringUtils.isAllLowerCase(word)) {
			lc = 0;
		} else if (StringUtils.isAllUpperCase(word)) {
			lc = 2;
		} else if (Character.isUpperCase(word.charAt(0))) {
			lc = 1;
		}
		if (disambiguateFindTokens.size() == 0) {
			return null;
		}
		Token t = disambiguateFindTokens.get(0);
		String pos = t.getPos();
		double type = 0;
		if (pos.equalsIgnoreCase("Noun")) {
			type = 0;
		} else if (pos.equalsIgnoreCase("Verb")) {
			type = 1;
		} else if (pos.equalsIgnoreCase("Adj")) {
			type = 2;
		} else if (pos.equalsIgnoreCase("Adv")) {
			type = 3;
		} else if (pos.equalsIgnoreCase("Interj")) {
			type = 4;
		} else if (pos.equalsIgnoreCase("Num")) {
			type = 5;
		} else if (pos.equalsIgnoreCase("Conj")) {
			type = 6;
		} else if (pos.equalsIgnoreCase("Det")) {
			type = 7;
		} else if (pos.equalsIgnoreCase("Pron")) {
			type = 8;
		} else if (pos.equalsIgnoreCase("Postp")) {
			type = 9;
		} else if (pos.equalsIgnoreCase("Ques")) {
			type = 10;
		} else if (pos.equalsIgnoreCase("Punc")) {
			return null;
		} else if (pos.equalsIgnoreCase("Dup")) {
			return null;
		} else if (pos.equalsIgnoreCase("")) {
			type = 11;
		} else {
			type = 12;
			LOGGER.info(pos);
		}
		String lemma = t.getMorphText();
		List<Double> wordVector = getWordVector(word);
		if (wordVector == null) {
			wordVector = getWordVector(t.getMorphText());
			if (wordVector == null) {
				if (wordVector == null && t.getDictionary() != null) {
					wordVector = getWordVector(t.getDictionary());

				}
			}
		}
		if (wordVector == null || wordVector.size() == 0) {
			wordVector = emptyWordVector;

		}
		String suffix = t.getSuffix();
		List<Double> suffixVector = getSuffixVector(suffix);

		double wordType = 0;
		if (tdk.contains(lemma)
				|| tdk.contains(word)
				|| (t.getDictionary() != null && tdk
						.contains(t.getDictionary()))) {
			wordType = 0;
		} else if (names.contains(lemma) || names.contains(word)) {
			wordType = 1;
		} else {
			wordType = 2;
		}

		List<Double> featureVector = new ArrayList<Double>();
		featureVector.add(lc);
		featureVector.add(wordType);
		featureVector.add(type);
		featureVector.addAll(suffixVector);
		featureVector.addAll(wordVector);

		return featureVector;

	}

	public static List<Double> getWordVector(String word) {
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

	private static List<Double> getSuffixVector(String s) {
		List<Double> vector = new ArrayList<Double>();
		for (String suffix : suffixes) {
			if (s.contains(suffix)) {
				vector.add(1d);
			} else {
				vector.add(0d);
			}
		}
		return vector;
	}
}
