package edu.yeditepe.nounphrase;

import it.cnr.isti.hpc.text.Token;

import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import edu.yeditepe.lucene.Word2VecSearchEngine;
import edu.yeditepe.nlp.TurkishNLP;
import edu.yeditepe.nlp.Zemberek;
import edu.yeditepe.utils.FileUtils;

public class NounPhraseCreateFeatures {
	private static final Logger LOGGER = Logger
			.getLogger(NounPhraseCreateFeatures.class);
	private static int vectorSize = 100;
	private static int suffixSize = 64;
	private static List<String> suffixes;
	private static Set<String> names;
	private static Set<String> tdk;
	private static List<Double> emptyWordVector;

	public static void main(String[] args) {
		boolean flag = false;
		int trainingSize = 50000;
		int testingSize = 20000;
		List<ArrayList<Double>> posData = new ArrayList<ArrayList<Double>>();
		List<ArrayList<Double>> negData = new ArrayList<ArrayList<Double>>();
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
		List<String> readFile = FileUtils.readFile("nounphrases1.txt");
		for (String line : readFile) {
			String[] split = line.split("\t");
			if (split.length == 3) {
				String prev = split[0];
				String phrase = split[1];
				String next = split[2];
				String[] pWords = phrase.split(" ");
				for (int i = 1; i < pWords.length; i++) {
					ArrayList<Double> featureVector = new ArrayList<Double>();
					String p1 = pWords[i - 1];
					List<Double> f1 = getFeatureVector(p1);
					String p2 = pWords[i];
					List<Double> f2 = getFeatureVector(p2);
					if (f1 != null && f2 != null) {
						featureVector.add((double) i);
						featureVector.addAll(f1);
						featureVector.addAll(f2);
						posData.add(featureVector);
					}
				}
				if (next.length() > 1 && !charSet.contains(next.charAt(0))) {
					ArrayList<Double> featureVector = new ArrayList<Double>();
					String p1 = pWords[pWords.length - 1];
					List<Double> f1 = getFeatureVector(p1);
					String p2 = next;
					List<Double> f2 = getFeatureVector(p2);
					if (f1 != null && f2 != null) {
						featureVector.add((double) pWords.length);
						featureVector.addAll(f1);
						featureVector.addAll(f2);
						negData.add(featureVector);
					}
				}

				if (prev.length() > 1
						&& !charSet.contains(prev.charAt(prev.length() - 1))) {
					ArrayList<Double> featureVector = new ArrayList<Double>();
					String p1 = prev;
					List<Double> f1 = getFeatureVector(p1);
					String p2 = pWords[0];
					List<Double> f2 = getFeatureVector(p2);
					if (f1 != null && f2 != null) {
						featureVector.add((double) pWords.length);
						featureVector.addAll(f1);
						featureVector.addAll(f2);
						negData.add(featureVector);
					}
				}

			}
			if (posData.size() > testingSize && negData.size() > testingSize
					&& flag) {
				break;
			} else if (posData.size() > trainingSize
					&& negData.size() > trainingSize) {
				posData.clear();
				negData.clear();
				flag = true;
			}

		}
		saveFeatures(posData, "noun_pos_test");
		saveFeatures(negData, "noun_neg_test");
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

	public static List<Double> getSuffixVector(String s) {
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

	private static void saveFeatures(List<ArrayList<Double>> data, String name) {
		Writer writer;
		try {
			writer = new FileWriter(name + ".json");
			Gson gson = new GsonBuilder().create();
			gson.toJson(data, writer);
			writer.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
