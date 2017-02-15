package edu.yeditepe.deep;

import it.cnr.isti.hpc.benchmark.Stopwatch;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
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
import org.encog.neural.networks.layers.BasicLayer;
import org.encog.neural.networks.training.propagation.Propagation;
import org.encog.neural.networks.training.propagation.quick.QuickPropagation;
import org.encog.persist.EncogDirectoryPersistence;

import vectorspace.CosineSimilarity;

import com.google.common.cache.LoadingCache;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

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
public class EntityEmbeddingAutoencoder2 {
	private static final Logger LOGGER = Logger
			.getLogger(EntityEmbeddingAutoencoder2.class);

	private static int epoch = Integer.parseInt(Property.getInstance().get(
			"epoch"));
	private static int maxTrainingSize = Integer.parseInt(Property
			.getInstance().get("trainingSize"));
	private static int inputSize = Integer.parseInt(Property.getInstance().get(
			"inputSize"));
	private static double similarityThreshold = 0.3;
	private static String experiment = "experiment.txt";
	private static String modelFile = "autoencoder2_embedding_network_test.model7";
	private BasicNetwork network;
	private static HashMap<String, double[]> embeddings;
	private static LoadingCache<String, double[]> cache;
	List<String> posSamples;
	List<String> negSamples;

	public EntityEmbeddingAutoencoder2() {

		Reader reader;
		try {
			reader = new FileReader("autoencoderembeddings_quick.json");
			Gson gson = new GsonBuilder().create();
			embeddings = new HashMap<String, double[]>();
			HashMap<String, List<Double>> embeddingsJson = gson.fromJson(
					reader, HashMap.class);
			for (String key : embeddingsJson.keySet()) {
				Double[] ds = embeddingsJson.get(key).toArray(
						new Double[embeddingsJson.get(key).size()]);
				double[] d = ArrayUtils.toPrimitive(ds);
				embeddings.put(key, d);
			}
			embeddingsJson = null;
		} catch (FileNotFoundException e) {
			LOGGER.error(e);
		}

	}

	/**
	 * The main method.
	 * 
	 * @param args
	 *            No arguments are used.
	 */
	public static void main(final String args[]) {
		EntityEmbeddingAutoencoder2 ee = new EntityEmbeddingAutoencoder2();
		// BasicNetwork network = ee.train();
		// EncogDirectoryPersistence.saveObject(new File(modelFile), network);
		//
		ee.load(modelFile);
		ee.createEmbeddingsFile("");
		// ee.evaluateTrainData();
		Encog.getInstance().shutdown();
	}

	public BasicNetwork train() {
		LOGGER.info("training");
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
		inputSize = 300;
		int outputSize = 150;
		// create a neural network, without using a factory
		Stopwatch stopwatch = new Stopwatch();
		CosineSimilarity cs = new CosineSimilarity();
		network = new BasicNetwork();
		network.addLayer(new BasicLayer(null, true, inputSize));
		network.addLayer(new BasicLayer(new ActivationTANH(), true, outputSize));
		network.addLayer(new BasicLayer(new ActivationTANH(), false, outputSize));
		network.getStructure().finalizeStructure();
		network.setBiasActivation(0.1);
		network.reset();

		Propagation train = null;
		int batchsize = Integer.parseInt(Property.getInstance().get(
				"training.batchsize"));
		for (int z = 0; z < epoch; z++) {
			LOGGER.info("Epoch " + (z + 1) + " started");
			for (int k = 0; k < sampleSize; k += batchsize) {
				HashMap<String, List<String>> posSet = new HashMap<String, List<String>>();
				for (int i = 0; i < batchsize && k + i < posSize; i++) {
					String[] p = posSamples.get(k + i).split("\t");
					if (posSet.containsKey(p[0])) {
						posSet.get(p[0]).add(p[1]);
					} else {
						List<String> list = new ArrayList<String>();
						list.add(p[1]);
						posSet.put(p[0], list);
					}
				}
				HashMap<String, List<String>> negSet = new HashMap<String, List<String>>();
				for (int i = 0; i < batchsize && k + i < negSize; i++) {
					String[] p = negSamples.get(k + i).split("\t");
					if (negSet.containsKey(p[0])) {
						negSet.get(p[0]).add(p[1]);
					} else {
						List<String> list = new ArrayList<String>();
						list.add(p[1]);
						negSet.put(p[0], list);
					}
				}

				for (String from : negSet.keySet()) {
					List<String> toList = negSet.get(from);
					double[] p1 = getFeatureVector(from);
					if (p1 == null)
						continue;
					BasicMLData d1 = new BasicMLData(p1);
					double[] e1 = network.compute(d1).getData();
					double[][] input = new double[toList.size()][inputSize];
					double[][] output = new double[toList.size()][outputSize];
					int count = 0;
					for (String to : toList) {
						double[] p2 = getFeatureVector(to);
						if (p2 == null)
							continue;
						BasicMLData d2 = new BasicMLData(p2);
						double[] e2 = network.compute(d2).getData();
						double value = cs.cosineSimilarity(cs.normalize(e1),
								cs.normalize(e2));
						for (int j = 0; j < e2.length; j++) {
							if (e1[j] > e2[j]) {
								e2[j] -= Math.random() * (value);
							} else {
								e2[j] += Math.random() * (value);
							}
						}
						input[count] = p2;
						output[count] = e2;
						count++;

					}
					shuffleArray(input, output);
					MLDataSet trainingSet = new BasicMLDataSet(input, output);
					if (train == null) {
						// train = new Backpropagation(network, trainingSet);
						// train = new ResilientPropagation(network,
						// trainingSet);
						train = new QuickPropagation(network, trainingSet, 0.01);
						train.setThreadCount(0);

						// for(int i = 0 ; i < 100; i ++) {

						// train = new ManhattanPropagation(network,
						// trainingSet,
						// 0.1f);

					} else {
						train.setTraining(trainingSet);
					}
					train.iteration();
				}

				for (String from : posSet.keySet()) {
					List<String> toList = posSet.get(from);
					double[] p1 = getFeatureVector(from);
					BasicMLData d1 = new BasicMLData(p1);
					double[] e1 = network.compute(d1).getData();
					double[][] input = new double[toList.size()][inputSize];
					double[][] output = new double[toList.size()][outputSize];
					int count = 0;
					for (String to : toList) {
						double[] p2 = getFeatureVector(to);
						input[count] = p2;
						output[count] = e1;
						count++;
					}
					MLDataSet trainingSet = new BasicMLDataSet(input, output);
					if (train == null) {
						// train = new Backpropagation(network, trainingSet);

						// train = new ManhattanPropagation(network,
						// trainingSet,
						// 0.1f);
						// LOGGER.info(network.dumpWeights().substring(1, 100));
						// train = new ResilientPropagation(network,
						// trainingSet);
						train = new QuickPropagation(network, trainingSet, 0.01);
						train.setThreadCount(0);
						// LOGGER.info(network.dumpWeights().substring(1, 100));

					} else {
						train.setTraining(trainingSet);
					}
					train.iteration();
				}
				// if (k > 0 && k % 200000 == 0) {
				// double error = train.getError();
				// LOGGER.info("iteration " + (k) + " Error : " + error);
				// EncogDirectoryPersistence.saveObject(new File(modelFile
				// + "_backup"), network);
				// }

			}
			double error = train.getError();
			LOGGER.info("Epoch " + (z + 1) + " Error : " + error);
			EncogDirectoryPersistence.saveObject(new File(modelFile + (z + 1)),
					network);
			LOGGER.info("Weights " + network.dumpWeights().substring(1, 1000));
			// int result = evaluateTrainData();
			if (error < Double.parseDouble(Property.getInstance().get("error"))) {
				break;
			}
		}

		return network;
	}

	private void createEmbeddingsFile(String prefix) {
		CosineSimilarity cs = new CosineSimilarity();
		File folder = new File(Property.getInstance().get("featuresDirectory"));
		File[] listOfFiles = folder.listFiles();
		HashMap<String, double[]> embeddings = new HashMap<String, double[]>();
		for (File file : listOfFiles) {
			if (file.isFile()) {
				String key = file.getName().replace(".dat", "");
				BasicMLData d1 = new BasicMLData(getFeatureVector(key));
				double[] e1 = cs.normalize(network.compute(d1).getData());
				embeddings.put(key, e1);
			}
		}
		Writer writer;
		try {
			writer = new FileWriter(prefix + "2embeddings_new.json");
			Gson gson = new GsonBuilder().create();
			gson.toJson(embeddings, writer);

			writer.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public double[] evaluate(double[] featureVector) {
		BasicMLData d1 = new BasicMLData(featureVector);
		return network.compute(d1).getData();
	}

	public int evaluateTrainData() {
		posSamples = FileUtils.readFile(Property.getInstance().get("posFile"));
		negSamples = FileUtils.readFile(Property.getInstance().get("negFile"));

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
			BasicMLData d1 = new BasicMLData(p1);
			BasicMLData d2 = new BasicMLData(p2);
			double[] e1 = cs.normalize(network.compute(d1).getData());
			double[] e2 = cs.normalize(network.compute(d2).getData());
			double value = cs.cosineSimilarity(e1, e2);
			String s = p[0] + "-" + p[1] + " similarity = " + value + "\n";
			FileUtils.writeFile(s, experiment);
			if (value >= similarityThreshold) {
				// LOGGER.info(s);
				trueNum++;
			} else {
				// LOGGER.info(s);
				falseNum++;
			}

		}
		// LOGGER.info("Total positives correct:" + trueNum + " Incorrect:"
		// + falseNum + " Precision:" + trueNum * 100
		// / (trueNum + falseNum));

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
			BasicMLData d1 = new BasicMLData(p1);
			BasicMLData d2 = new BasicMLData(p2);
			double[] e1 = network.compute(d1).getData();
			double[] e2 = network.compute(d2).getData();
			double value = cs.cosineSimilarity(e1, e2);
			String s = p[0] + "-" + p[1] + " similarity = " + value + "\n";
			FileUtils.writeFile(s, experiment);
			if (value < similarityThreshold) {
				// LOGGER.info(s);
				trueNum++;
			} else {
				// LOGGER.info(s);
				falseNum++;
			}

		}
		int precison = trueNum * 100 / (trueNum + falseNum);
		String s = "Total  correct:" + trueNum + " Incorrect:" + falseNum
				+ " Precision:" + precison;
		FileUtils.writeFile(s, experiment);
		LOGGER.info(s);
		return precison;
	}

	public void load(String fileName) {
		LOGGER.info("Loading network");

		network = (BasicNetwork) EncogDirectoryPersistence.loadObject(new File(
				fileName));
		LOGGER.info("network loaded");

	}

	public double[] getFeatureVector(String f) {
		try {
			return embeddings.get(f);
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
