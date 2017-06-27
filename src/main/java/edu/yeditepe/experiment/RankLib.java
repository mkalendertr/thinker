package edu.yeditepe.experiment;

import org.apache.log4j.Logger;

import ciir.umass.edu.learning.DataPoint;
import ciir.umass.edu.learning.Ranker;
import ciir.umass.edu.learning.RankerFactory;
import edu.yeditepe.model.EntityScores;

public class RankLib {
	private static final Logger LOGGER = Logger.getLogger(RankLib.class);

	private static RankLib ranklib = new RankLib();

	// Invalid parameter: Usage:
	// GET or POST parameters: {tool}, {input} and {token}
	// token: you can find your token on your login space
	// tool: ner, morphanalyzer, isturkish, morphgenerator, tokenizer,
	// normalize, deasciifier, Vowelizer, depparser, spellcheck, disambiguator,
	// pipeline
	// input: utf-8 string
	// The response is a text/plain encoded in UTF-8
	Ranker rankModel;

	private RankLib() {
		RankerFactory rankerFactory = new RankerFactory();
		rankModel = rankerFactory.loadRankerFromFile("ranklib\\rf.model");
	}

	public double score(EntityScores e) {
		DataPoint allScores = new DenseProgramaticDataPoint(14);
		allScores.setFeatureValue(1, (float) e.getLinkScore());
		allScores.setFeatureValue(2, (float) e.getHashInfoboxScore());
		allScores.setFeatureValue(3, (float) e.getWordvecLinksScore());
		allScores.setFeatureValue(4, (float) e.getTypeContentScore());
		allScores.setFeatureValue(5, (float) e.getTypeScore());
		allScores.setFeatureValue(6, (float) e.getSimpleLeskScore());
		allScores.setFeatureValue(7, (float) e.getTypeClassifierkScore());
		allScores.setFeatureValue(8, (float) e.getNameScore());
		allScores.setFeatureValue(9, (float) e.getHashDescriptionScore());
		allScores.setFeatureValue(10,
				(float) e.getWordvecDescriptionLocalScore());
		allScores.setFeatureValue(11, (float) e.getSuffixScore());
		allScores.setFeatureValue(12, (float) e.getLeskScore());
		allScores.setFeatureValue(13, (float) e.getWordvecDescriptionScore());
		allScores.setFeatureValue(14, (float) e.getLetterCaseScore());

		float score = (float) rankModel.eval(allScores);
		// System.out.printf("Doc %d, score %f\n", docID(), score);
		return score;
	}

	public static RankLib getInstance() {
		return ranklib;
	}

	public static void main(String[] args) {

	}
}
