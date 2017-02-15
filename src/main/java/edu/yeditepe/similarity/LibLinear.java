package edu.yeditepe.similarity;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;

import org.apache.commons.lang.ArrayUtils;
import org.apache.log4j.Logger;

import de.bwaldvogel.liblinear.Feature;
import de.bwaldvogel.liblinear.FeatureNode;
import de.bwaldvogel.liblinear.Linear;
import de.bwaldvogel.liblinear.Model;
import de.bwaldvogel.liblinear.Parameter;
import de.bwaldvogel.liblinear.Problem;
import de.bwaldvogel.liblinear.SolverType;
import edu.yeditepe.utils.FileUtils;
import edu.yeditepe.utils.Property;

public class LibLinear {
	private static final Logger LOGGER = Logger.getLogger(LibLinear.class);

	private static int inputSize = Integer.parseInt(Property.getInstance().get(
			"inputSize"));
	private static double similarityThreshold = 0.3;
	private static String modelAdress = "liblinear_all.model";
	private static String experiment = "experiment_linear.txt";
	private static String trainingFile = "liblinear_dis.txt";
	private static Model model;
	// private static int sampleSize = 10880000;
	private static int sampleSize = 319000;

	public LibLinear() {

	}

	public void train() throws IOException {
		LOGGER.info("training");

		// if (sampleSize == -1) {
		// sampleSize = samples.size();
		// }
		double[] y = new double[sampleSize];
		Feature[][] x = new Feature[sampleSize][];
		Feature[] featureCache = new Feature[inputSize * 2];
		for (int i = 0; i < featureCache.length; i++) {
			featureCache[i] = new FeatureNode(i, 1);
		}
		int counter = 0;
		// double max=0;
		FileInputStream fstream = new FileInputStream(trainingFile);
		BufferedReader br = new BufferedReader(new InputStreamReader(fstream));

		String strLine;
		while ((strLine = br.readLine()) != null) {

			String[] indexes = strLine.split(" ");
			y[counter] = Double.parseDouble(indexes[0]);
			Feature[] features = new Feature[indexes.length - 1];
			for (int i = 1; i < indexes.length; i++) {
				features[i - 1] = featureCache[Integer.parseInt(indexes[i])];
			}
			x[counter] = features;
			counter++;
			if (counter >= sampleSize) {
				break;
			}
			if (counter % 10000 == 0) {
				LOGGER.info(counter);
			}
		}
		br.close();
		// System.out.println(max);

		Problem problem = new Problem();
		problem.l = sampleSize;
		problem.n = inputSize * 2;
		problem.x = x;
		problem.y = y;

		SolverType solver = SolverType.L2R_LR; // -s 0
		double C = 1.0; // cost of constraints violation
		double eps = 0.01; // stopping criteria

		Parameter parameter = new Parameter(solver, C, eps);
		// double[] target = new double[problem.l];
		// Linear.crossValidation(problem, parameter, 2, target);
		model = Linear.train(problem, parameter);
		File modelFile = new File(modelAdress);
		model.save(modelFile);

	}

	public Model load() throws IOException {
		File modelFile = new File(modelAdress);
		model = Model.load(modelFile);
		return model;
	}

	public void evaluateTrainData() throws NumberFormatException,
			IllegalArgumentException, IOException {
		LOGGER.info("evaluating");
		int trueNum = 0;
		int falseNum = 0;
		double[] probablity = new double[2];
		FileInputStream fstream = new FileInputStream(trainingFile);
		BufferedReader br = new BufferedReader(new InputStreamReader(fstream));

		String strLine;
		while ((strLine = br.readLine()) != null) {

			String[] indexes = strLine.split(" ");
			Feature[] features = new Feature[indexes.length - 1];
			for (int i = 1; i < indexes.length; i++) {
				Feature f = new FeatureNode(Integer.parseInt(indexes[i]), 1);
				features[i - 1] = f;
			}
			double y = Double.parseDouble(indexes[0]);

			double prediction = Linear.predictProbability(model, features,
					probablity);
			if (y == prediction) {

				trueNum++;
			} else {
				falseNum++;
			}

		}
		int precison = trueNum * 100 / (trueNum + falseNum);
		String s = "Total  correct:" + trueNum + " Incorrect:" + falseNum
				+ " Precision:" + precison;
		FileUtils.writeFile(s, experiment);
		LOGGER.info(s);
		br.close();

	}

	public double evaluate(double[] featureVector) {
		double[] probablity = new double[2];
		Feature[] features = new Feature[featureVector.length];
		int counter = 0;
		for (int i = 0; i < featureVector.length; i++) {
			if (featureVector[i] == 1) {
				Feature f = new FeatureNode(i, 1);
				features[counter++] = f;
			}

		}
		features = Arrays.copyOf(features, counter);
		Linear.predictProbability(model, features, probablity);
		return probablity[0];
	}

	public double evaluate(String input1, String input2) {

		double[] p1 = getFeatureVector(input1);
		double[] p2 = getFeatureVector(input2);

		if (p1 == null || p2 == null) {
			return 0;
		}
		double[] both = ArrayUtils.addAll(p1, p2);
		return evaluate(both);

	}

	public double[] getFeatureVector(String key) {
		byte[] values = FileUtils.read(Property.getInstance().get(
				"featuresDirectory")
				+ File.separator + key + ".dat");
		return toDoubleArray(values);
	}

	public static double[] toDoubleArray(byte[] byteArray) {
		double[] doubles = new double[inputSize];
		for (int i = 0; i < inputSize; i++) {
			doubles[i] = byteArray[byteArray.length - inputSize + i];
			// if (doubles[i] == 1) {
			// LOGGER.info(doubles[i]);
			// }
		}
		return doubles;
	}

	public static void main(String[] args) {
		LibLinear ll = new LibLinear();
		try {
			ll.train();
			ll.load();
			ll.evaluateTrainData();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}
}
