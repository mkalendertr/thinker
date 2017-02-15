package edu.yeditepe.deep;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.apache.log4j.Logger;
import org.encog.Encog;
import org.encog.engine.network.activation.ActivationTANH;
import org.encog.ml.data.basic.BasicMLData;
import org.encog.neural.networks.BasicNetwork;
import org.encog.persist.EncogDirectoryPersistence;

import com.google.common.cache.LoadingCache;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import edu.yeditepe.deep.autoencoder.AutoEncoder;
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
public class EntityEmbeddingAutoencoder {
	private static final Logger LOGGER = Logger
			.getLogger(EntityEmbeddingAutoencoder.class);

	private static int epoch = Integer.parseInt(Property.getInstance().get(
			"epoch"));
	private static int maxTrainingSize = Integer.parseInt(Property
			.getInstance().get("trainingSize"));
	// private static int maxNTrainingSize = Integer.parseInt(Property
	// .getInstance().get("maxNTrainingSize"));
	private static int inputSize = Integer.parseInt(Property.getInstance().get(
			"inputSize"));
	private static String experiment = "experiment_autoencoder.txt";
	private static String modelFile = "embedding_network_autoencoder.model";
	private BasicNetwork network;

	private static LoadingCache<String, double[]> cache;
	List<String> posSamples;
	List<String> negSamples;
	public static int vectorSize = 10000;

	public EntityEmbeddingAutoencoder() {
		//
		// cache = CacheBuilder.newBuilder().maximumSize(100000)
		// .build(new CacheLoader<String, double[]>() {
		// public double[] load(String key) {
		// byte[] values = FileUtils.read(Property.getInstance()
		// .get("featuresDirectory")
		// + File.separator
		// + key + ".dat");
		// return toDoubleArray(values);
		// }
		// });
	}

	/**
	 * The main method.
	 * 
	 * @param args
	 *            No arguments are used.
	 */
	public static void main(final String args[]) {
		EntityEmbeddingAutoencoder ee = new EntityEmbeddingAutoencoder();
		ee.preTrain();
		ee.createEmbeddingsFile();
		EncogDirectoryPersistence.saveObject(new File(modelFile),
				ee.getNetwork());
		ee.load(modelFile);
		Encog.getInstance().shutdown();
	}

	public BasicNetwork preTrain() {
		try {

			Reader reader = new FileReader("hashvectors_10000.json");
			Gson gson = new GsonBuilder().create();
			Map<String, List<Double>> vectors = gson
					.fromJson(reader, Map.class);

			LOGGER.info("pretraining");
			AutoEncoder encoder = new AutoEncoder();

			File folder = new File(Property.getInstance().get(
					"featuresDirectory"));
			List<String> entities = new ArrayList<String>();
			int interval = Integer.parseInt(Property.getInstance().get(
					"autoencoder.interval"));
			int i = 0;
			for (String title : vectors.keySet()) {
				double[] hashVector = new double[vectorSize];
				List<Double> hashSet = vectors.get(title);
				for (Double d : hashSet) {
					hashVector[d.intValue()] = 1;
				}
				if (i++ % 10 == 0) {
					encoder.addData(hashVector);
				}
			}

			encoder.addLayer(new ActivationTANH(), 2500);
			encoder.addLayer(new ActivationTANH(), 600);
			network = encoder.getNetwork(2);
			return network;

		} catch (Exception e) {
			// TODO: handle exception
		}
		return null;
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

	private void createEmbeddingsFile() {
		try {

			HashMap<String, double[]> embeddings = new HashMap<String, double[]>();
			Reader reader = new FileReader("hashvectors_10000.json");
			Gson gson = new GsonBuilder().create();
			Map<String, List<Double>> vectors = gson
					.fromJson(reader, Map.class);
			for (String title : vectors.keySet()) {
				double[] hashVector = new double[vectorSize];
				List<Double> hashSet = vectors.get(title);
				for (Double d : hashSet) {
					hashVector[d.intValue()] = 1;
				}
				BasicMLData d1 = new BasicMLData(hashVector);
				double[] e1 = network.compute(d1).getData();
				embeddings.put(title, e1);
			}
			try {

				Writer writer = new FileWriter("hash_embeddings_v2.json");
				gson = new GsonBuilder().create();
				gson.toJson(embeddings, writer);
				writer.close();
			} catch (Exception e) {
				// TODO: handle exception
			}
		} catch (Exception e) {
			// TODO: handle exception
		}
		// CosineSimilarity cs = new CosineSimilarity();
		// File folder = new
		// File(Property.getInstance().get("featuresDirectory"));
		// File[] listOfFiles = folder.listFiles();
		// HashMap<String, double[]> embeddings = new HashMap<String,
		// double[]>();
		// for (File file : listOfFiles) {
		// if (file.isFile()) {
		// String key = file.getName().replace(".dat", "");
		// byte[] values = FileUtils.read(file.getAbsolutePath());
		// double[] features = toDoubleArray(values);
		// BasicMLData d1 = new BasicMLData(features);
		// double[] e1 = cs.normalize(network.compute(d1).getData());
		// embeddings.put(key, e1);
		// }
		// }
		// Writer writer;
		// try {
		// writer = new FileWriter("autoencoder_embeddings_new.json");
		// Gson gson = new GsonBuilder().create();
		// gson.toJson(embeddings, writer);
		//
		// writer.close();
		// } catch (IOException e) {
		// // TODO Auto-generated catch block
		// e.printStackTrace();
		// }

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
