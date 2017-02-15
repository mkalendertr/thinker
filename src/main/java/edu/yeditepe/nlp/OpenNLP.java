package edu.yeditepe.nlp;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import opennlp.tools.cmdline.postag.POSModelLoader;
import opennlp.tools.postag.POSModel;
import opennlp.tools.postag.POSTaggerME;
import opennlp.tools.tokenize.Tokenizer;
import opennlp.tools.tokenize.TokenizerME;
import opennlp.tools.tokenize.TokenizerModel;

import org.apache.log4j.Logger;
import org.tartarus.snowball.ext.PorterStemmer;

import com.google.gson.internal.LinkedTreeMap;

import edu.yeditepe.discovery.EnFeatures;
import edu.yeditepe.discovery.PrepareTypeClassifierTrainingDataEn;

public class OpenNLP {
	private static final Logger LOGGER = Logger.getLogger(OpenNLP.class);

	private static OpenNLP nlp = new OpenNLP();
	private POSTaggerME tagger;
	private Tokenizer tokenizer;

	public static void main(String[] args) {

	}

	private OpenNLP() {
		try {
			InputStream is = new FileInputStream("en-token.bin");

			TokenizerModel tmodel = new TokenizerModel(is);

			tokenizer = new TokenizerME(tmodel);
			POSModel pmodel = new POSModelLoader().load(new File(
					"en-pos-maxent.bin"));
			tagger = new POSTaggerME(pmodel);
		} catch (Exception e) {
			// TODO: handle exception
		}

	}

	public static OpenNLP getInstance() {
		return nlp;
	}

	public void parse(String sentence, EnFeatures features,
			LinkedTreeMap<String, double[]> vectors) {
		String tokens[] = tokenizer.tokenize(sentence);
		String[] tags = tagger.tag(tokens);
		List<String> nouns = new ArrayList<String>();
		List<String> verbs = new ArrayList<String>();
		for (int i = 0; i < tags.length; i++) {
			if (tags[i].startsWith("NN")) {
				nouns.add(tokens[i]);
			} else if (tags[i].startsWith("VB")) {
				verbs.add(tokens[i]);
			} else {
				// LOGGER.info(tags[i]);
			}
		}
		features.setTitleVector(getAverageVector(
				features.getTitle().split(" "), vectors));
		features.setVerbVector(getAverageVector(verbs.toArray(), vectors));
		features.setNounVector(getAverageVector(nouns.toArray(), vectors));

	}

	private static double[] getAverageVector(Object[] words,
			LinkedTreeMap<String, double[]> vectorsMap) {
		double[] avg = new double[PrepareTypeClassifierTrainingDataEn.vectorSize];
		List<double[]> vectors = new ArrayList<double[]>();
		for (int i = 0; i < words.length; i++) {
			double[] wordVector = vectorsMap.get(stemmer((String) words[i]));
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

	public static String stemmer(String word) {
		PorterStemmer stemmer = new PorterStemmer();
		stemmer.setCurrent(word); // set string you need to stem
		stemmer.stem(); // stem the word
		return stemmer.getCurrent().toLowerCase();// get the stemmed word
	}
}
