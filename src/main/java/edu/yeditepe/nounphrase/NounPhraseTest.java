package edu.yeditepe.nounphrase;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.List;

import org.apache.log4j.Logger;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import de.bwaldvogel.liblinear.Feature;
import de.bwaldvogel.liblinear.FeatureNode;
import de.bwaldvogel.liblinear.Linear;
import de.bwaldvogel.liblinear.Model;

public class NounPhraseTest {
	private static final Logger LOGGER = Logger.getLogger(NounPhraseTest.class);

	public static void main(String[] args) throws IOException {
		File modelFile = new File("spotter_lr.model");
		Model model = Model.load(modelFile);
		double[] probablity = new double[2];

		int testingSize = 10000;

		Reader reader = new FileReader("noun_neg_test.json");
		Gson gson = new GsonBuilder().create();
		List<List<Double>> neg = new Gson().fromJson(reader,
				new TypeToken<List<List<Double>>>() {
				}.getType());

		reader = new FileReader("noun_pos_test.json");
		gson = new GsonBuilder().create();
		List<List<Double>> pos = new Gson().fromJson(reader,
				new TypeToken<List<List<Double>>>() {
				}.getType());

		double[] yTraining = new double[testingSize * 2];
		Feature[][] xTraining = new Feature[testingSize * 2][];

		int i = 0;
		for (List<Double> list : pos) {
			FeatureNode[] features = new FeatureNode[list.size()];
			for (int j = 0; j < features.length; j++) {
				features[j] = new FeatureNode(j + 1, list.get(j));
			}
			xTraining[i] = features;
			yTraining[i] = 1;
			if (++i >= testingSize) {
				break;
			}
		}
		pos.clear();
		pos = null;
		for (List<Double> list : neg) {
			FeatureNode[] features = new FeatureNode[list.size()];
			for (int j = 0; j < features.length; j++) {
				features[j] = new FeatureNode(j + 1, list.get(j));
			}
			xTraining[i] = features;
			yTraining[i] = 0;
			if (++i >= testingSize * 2) {
				break;
			}
		}
		neg.clear();
		neg = null;
		float truePositive = 0;
		float falsePositive = 0;
		float trueNegative = 0;
		float falseNegative = 0;
		double confidence = 0.5;
		float notResponded = 0;
		for (int j = 0; j < xTraining.length; j++) {
			Linear.predictProbability(model, xTraining[j], probablity);
			if (probablity[0] > confidence || probablity[1] > confidence) {
				double prediction = Linear.predict(model, xTraining[j]);
				if (j < testingSize) {
					if (prediction == 1) {
						truePositive++;
					} else {
						falsePositive++;
					}
				} else {
					if (prediction == 0) {
						trueNegative++;
					} else {
						falseNegative++;
					}
				}
			} else {
				notResponded++;
			}
		}

		LOGGER.info("TP " + truePositive);
		LOGGER.info("FP " + falsePositive);
		LOGGER.info("TN " + trueNegative);
		LOGGER.info("FN " + falseNegative);
		LOGGER.info("Responded " + (xTraining.length - notResponded));
		LOGGER.info("Not Responded " + notResponded);
		float precision = (truePositive + trueNegative)
				/ (truePositive + trueNegative + falsePositive + falseNegative);
		float recall = (truePositive + trueNegative) / (xTraining.length);
		float f1 = 2 * (precision * recall) / (precision + recall);
		LOGGER.info("Confidence " + confidence);
		LOGGER.info("Precision " + precision);

		LOGGER.info("Recall " + recall);

		LOGGER.info("F1 " + f1);

	}
}
