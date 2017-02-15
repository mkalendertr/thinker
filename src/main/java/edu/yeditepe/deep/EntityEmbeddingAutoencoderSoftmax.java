package edu.yeditepe.deep;

import it.cnr.isti.hpc.benchmark.Stopwatch;

import java.io.File;
import java.util.List;
import java.util.Random;

import org.apache.commons.lang.ArrayUtils;
import org.apache.log4j.Logger;
import org.encog.Encog;
import org.encog.engine.network.activation.ActivationTANH;
import org.encog.ml.data.MLDataSet;
import org.encog.ml.data.basic.BasicMLData;
import org.encog.ml.data.basic.BasicMLDataSet;
import org.encog.neural.networks.BasicNetwork;
import org.encog.neural.networks.training.propagation.Propagation;
import org.encog.neural.networks.training.propagation.resilient.ResilientPropagation;
import org.encog.persist.EncogDirectoryPersistence;

import vectorspace.CosineSimilarity;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

import edu.yeditepe.deep.autoencoder.AutoEncoder;
import edu.yeditepe.utils.FileUtils;
import edu.yeditepe.utils.Property;

/**
 * XOR: This example is essentially the "Hello World" of neural network
 * programming. This example shows how to construct an Encog neural network to
 * predict the output from the XOR operator. This example uses backpropagation
 * to train the neural network.
 * 
 * This example attempts to use a minimum of Encog features to create and train
 * the neural network. This allows you to see exactly what is going on. For a
 * more advanced example, that uses Encog factories, refer to the XORFactory
 * example.
 * 
 */
public class EntityEmbeddingAutoencoderSoftmax {
	private static final Logger LOGGER = Logger
			.getLogger(EntityEmbeddingAutoencoderSoftmax.class);

	private static int epoch = Integer.parseInt(Property.getInstance().get(
			"epoch"));
	private static int maxTrainingSize = Integer.parseInt(Property
			.getInstance().get("trainingSize"));
	// private static int maxNTrainingSize = Integer.parseInt(Property
	// .getInstance().get("maxNTrainingSize"));
	private static int inputSize = Integer.parseInt(Property.getInstance().get(
			"inputSize"));
	private static String experiment = "experiment_autoencoder.txt";
	private static String modelFile = "embedding_network_autoencoder_softmax.model";
	private BasicNetwork network;

	private static LoadingCache<String, double[]> cache;
	List<String> posSamples;
	List<String> negSamples;

	public EntityEmbeddingAutoencoderSoftmax() {

		cache = CacheBuilder.newBuilder().maximumSize(100000)
				.build(new CacheLoader<String, double[]>() {
					public double[] load(String key) {
						byte[] values = FileUtils.read(Property.getInstance()
								.get("featuresDirectory")
								+ File.separator
								+ key + ".dat");
						return toDoubleArray(values);
					}
				});
	}

	/**
	 * The main method.
	 * 
	 * @param args
	 *            No arguments are used.
	 */
	public static void main(final String args[]) {
		EntityEmbeddingAutoencoderSoftmax ee = new EntityEmbeddingAutoencoderSoftmax();
		ee.preTrain();
		ee.train();
		EncogDirectoryPersistence.saveObject(new File(modelFile),
				ee.getNetwork());
		ee.load(modelFile);
		ee.evaluateTrainData();
		Encog.getInstance().shutdown();
	}

	public BasicNetwork preTrain() {
		LOGGER.info("pretraining");
		AutoEncoder encoder = new AutoEncoder();
		posSamples = FileUtils.readFile(Property.getInstance().get("posFile"));
		negSamples = FileUtils.readFile(Property.getInstance().get("negFile"));

		int posSize = posSamples.size();
		int negSize = negSamples.size();
		int sampleSize = posSize;
		if (posSize < negSamples.size()) {
			sampleSize = negSize;
		}
		if (maxTrainingSize == -1) {
			maxTrainingSize = sampleSize;
		} else if (maxTrainingSize < sampleSize) {
			sampleSize = maxTrainingSize;
		}

		for (int j = 0; j < sampleSize; j += 1000) {

			for (int i = 0; i < 1000 && j + i < posSize; i += 10) {
				String[] p = posSamples.get(j + i).split("\t");
				double[] p1 = getFeatureVector(p[0]);
				double[] p2 = getFeatureVector(p[1]);
				double[] e1 = ArrayUtils.addAll(p1, p2);
				double[] e2 = ArrayUtils.addAll(p2, p1);
				encoder.addData(e1);
				encoder.addData(e2);
			}
			for (int i = 0; i < 1000 && j + i < negSize; i += 10) {
				String[] p = negSamples.get(j + i).split("\t");
				double[] p1 = getFeatureVector(p[0]);
				double[] p2 = getFeatureVector(p[1]);
				double[] e1 = ArrayUtils.addAll(p1, p2);
				double[] e2 = ArrayUtils.addAll(p2, p1);
				encoder.addData(e1);
				encoder.addData(e2);
			}
		}
		encoder.addLayer(new ActivationTANH(), 600);
		encoder.addLayer(new ActivationTANH(), 150);
		network = encoder.getNetworkSoftmax(2);
		return network;
	}

	public BasicNetwork train() {
		LOGGER.info("training");
		posSamples = FileUtils.readFile(Property.getInstance().get("posFile"));
		negSamples = FileUtils.readFile(Property.getInstance().get("negFile"));
		// create a neural network, without using a factory
		Stopwatch stopwatch = new Stopwatch();
		CosineSimilarity cs = new CosineSimilarity();

		network.setBiasActivation(0.1);
		Propagation train = null;

		int posSize = posSamples.size();
		int negSize = negSamples.size();
		int sampleSize = posSize;
		if (posSize < negSamples.size()) {
			sampleSize = negSize;
		}
		if (maxTrainingSize == -1) {
			maxTrainingSize = sampleSize;
		} else if (maxTrainingSize < sampleSize) {
			sampleSize = maxTrainingSize;
		}

		double[] postive = { 0, 1 };
		double[] negative = { 1, 0 };
		for (int z = 0; z < epoch; z++) {
			LOGGER.info("Epoch " + (z + 1) + " started");
			for (int j = 0; j < sampleSize; j += 1000) {
				double[][] input = new double[4000][inputSize * 2];
				double[][] output = new double[4000][2];
				for (int i = 0; i < 1000 && j + i < posSize; i++) {
					String[] p = posSamples.get(j + i).split("\t");
					double[] p1 = getFeatureVector(p[0]);
					double[] p2 = getFeatureVector(p[1]);
					// double value = cs.cosineSimilarity(p1, p2);
					// LOGGER.info("P " + value);
					double[] e1 = ArrayUtils.addAll(p1, p2);
					double[] e2 = ArrayUtils.addAll(p2, p1);
					input[i * 2] = e1;
					input[i * 2 + 1] = e2;
					output[i * 2] = postive;
					output[i * 2 + 1] = postive;
				}
				for (int i = 0; i < 1000 && j + i < negSize; i++) {
					String[] p = negSamples.get(j + i).split("\t");
					double[] p1 = getFeatureVector(p[0]);
					double[] p2 = getFeatureVector(p[1]);
					// double value = cs.cosineSimilarity(p1, p2);
					// LOGGER.info("N " + value + " " + negSamples.get(i));
					double[] e1 = ArrayUtils.addAll(p1, p2);
					double[] e2 = ArrayUtils.addAll(p2, p1);
					input[2000 + (i * 2)] = e1;
					input[2000 + (i * 2) + 1] = e2;
					output[2000 + (i * 2)] = negative;
					output[2000 + (i * 2) + 1] = negative;
				}
				shuffleArray(input, output);
				MLDataSet trainingSet = new BasicMLDataSet(input, output);
				if (train == null) {
					// train = new Backpropagation(network, trainingSet);
					train = new ResilientPropagation(network, trainingSet);
					// train = new ManhattanPropagation(network, trainingSet,
					// 0.1f);

				} else {
					train.setTraining(trainingSet);
				}
				train.iteration();
				if (j % 100000 == 0) {
					double error = train.getError();
					LOGGER.info("iteration " + (j) + " Error : " + error);
					EncogDirectoryPersistence.saveObject(new File(modelFile
							+ "_backup"), network);
				}
			}
			double error = train.getError();
			LOGGER.info("Epoch #" + (z + 1) + " Error : " + error);
			EncogDirectoryPersistence.saveObject(new File(modelFile + (z + 1)),
					network);
			// evaluateTrainData();
			if (error < Double.parseDouble(Property.getInstance().get("error"))) {
				break;
			}
		}

		return network;
	}

	public void evaluateTrainData() {
		posSamples = FileUtils.readFile("pos_dis.txt");
		negSamples = FileUtils.readFile("neg_dis.txt");
		CosineSimilarity cs = new CosineSimilarity();
		int trueNum = 0;
		int falseNum = 0;
		int iteration = 0;
		for (String pos : posSamples) {
			String[] p = pos.split("\t");
			double[] p1 = getFeatureVector(p[0]);
			double[] p2 = getFeatureVector(p[1]);

			if (p[0].equals(p[1]) || p1 == null || p2 == null) {
				continue;
			} else if (iteration++ == maxTrainingSize) {
				break;
			}
			double[] both = ArrayUtils.addAll(p1, p2);
			BasicMLData d1 = new BasicMLData(both);
			double[] e1 = network.compute(d1).getData();
			String s = p[0] + "-" + p[1] + " similarity = " + e1[1] + "\n";
			FileUtils.writeFile(s, experiment);
			if (e1[1] >= e1[0]) {
				// LOGGER.info(s);
				trueNum++;
			} else {
				// LOGGER.info(s);
				falseNum++;
			}

		}
		LOGGER.info("Total positives correct:" + trueNum + " Incorrect:"
				+ falseNum + " Precision:" + trueNum * 100
				/ (trueNum + falseNum));

		iteration = 0;
		for (String pos : negSamples) {
			if (iteration++ == maxTrainingSize) {
				break;
			}
			String[] p = pos.split("\t");
			double[] p1 = getFeatureVector(p[0]);
			double[] p2 = getFeatureVector(p[1]);
			if (p1 == null || p2 == null) {
				continue;
			}
			double[] both = ArrayUtils.addAll(p1, p2);
			BasicMLData d1 = new BasicMLData(both);
			double[] e1 = network.compute(d1).getData();
			String s = p[0] + "-" + p[1] + " similarity = " + e1[1] + "\n";
			FileUtils.writeFile(s, experiment);
			if (e1[0] > e1[1]) {
				// LOGGER.info(s);
				trueNum++;
			} else {
				// LOGGER.info(s);
				falseNum++;
			}

		}
		String s = "Total  correct:" + trueNum + " Incorrect:" + falseNum
				+ " Precision:" + trueNum * 100 / (trueNum + falseNum);
		FileUtils.writeFile(s, experiment);
		LOGGER.info(s);

	}

	public double evaluate(String input1, String input2) {

		double[] p1 = getFeatureVector(input1);
		double[] p2 = getFeatureVector(input2);

		if (p1 == null || p2 == null) {
			return 0;
		}
		double[] both = ArrayUtils.addAll(p1, p2);
		BasicMLData d1 = new BasicMLData(both);
		double[] e1 = network.compute(d1).getData();
		return e1[1];

	}

	public double evaluate(double[] p1, double[] p2) {

		if (p1 == null || p2 == null) {
			return 0;
		}
		double[] both = ArrayUtils.addAll(p1, p2);
		BasicMLData d1 = new BasicMLData(both);
		double[] e1 = network.compute(d1).getData();

		return e1[1];

	}

	public void load(String fileName) {
		LOGGER.info("Loading network");

		network = (BasicNetwork) EncogDirectoryPersistence.loadObject(new File(
				fileName));
		LOGGER.info("Network loaded");

	}

	public double[] getFeatureVector(String f) {
		try {
			return cache.getUnchecked(f);
		} catch (Exception e) {
			// LOGGER.error(f);
		}
		return null;

		// byte[] values = FileUtils.read("C:\\data\\vector\\" + f + ".dat");
		// return toDoubleArray(values);

		// double[] doubles = new double[inputSize];
		// for (int i = 0; i < doubles.length; i++) {
		// doubles[i] = Math.round(Math.random());
		// }
		// return doubles;
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

	public static boolean equals(double[] e1, double[] e2) {
		for (int j = 0; j < e1.length; j++) {
			// if (e1[j] != e2[j]) {
			// return false;
			// }
			if (e1[j] == 1 || e2[j] == 1) {
				return false;
			}
		}
		return true;
	}

	public double[] getFeatureVector(List<String> entities) {
		double[] featureVector = new double[inputSize];
		for (String e : entities) {
			double[] data = getFeatureVector(e);
			if (data != null) {
				for (int i = 0; i < data.length; i++) {
					if (data[i] == 1) {
						featureVector[i] = 1;
					}
				}
			}

		}
		return featureVector;
	}

	public BasicNetwork getNetwork() {
		return network;
	}

	public void setNetwork(BasicNetwork network) {
		this.network = network;
	}

	static void shuffleArray(double[][] in, double[][] out) {
		Random rnd = new Random();
		for (int i = in.length - 1; i > 0; i--) {
			int index = rnd.nextInt(i + 1);
			// Simple swap
			double[] a = in[index];
			double[] b = out[index];
			in[index] = in[i];
			out[index] = out[i];
			in[i] = a;
			out[i] = b;
		}
	}

}
