package edu.yeditepe.deep;

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
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.apache.log4j.Logger;
import org.encog.engine.network.activation.ActivationTANH;
import org.encog.ml.data.basic.BasicMLData;
import org.encog.neural.networks.BasicNetwork;
import org.encog.persist.EncogDirectoryPersistence;

import pl.edu.pw.mini.msi.AutoEncoder2;
import pl.edu.pw.mini.msi.FileManager;
import pl.edu.pw.mini.msi.NetworkFactory;
import pl.edu.pw.mini.msi.Trainer;
import pl.edu.pw.mini.msi.Visualizer;
import vectorspace.CosineSimilarity;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import edu.yeditepe.deep.autoencoder.AutoEncoder;
import edu.yeditepe.model.Entity;
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
public class EntityEmbeddingAutoencoder3gram {
	private static final Logger LOGGER = Logger
			.getLogger(EntityEmbeddingAutoencoder3gram.class);

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

	public static int vectorSize = 5000;

	public EntityEmbeddingAutoencoder3gram() {

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
	 * @throws FileNotFoundException
	 */
	public static void main(final String args[]) throws FileNotFoundException {
		EntityEmbeddingAutoencoder3gram n = new EntityEmbeddingAutoencoder3gram();
		BasicNetwork network = n.load("INFOBOXEMBEDDINGHASH3GRAM.model");
		Map<String, double[]> embeddings = new HashMap<String, double[]>();

		Reader reader = new FileReader("hashvectors_5000.json");
		Gson gson = new GsonBuilder().create();
		Map<String, List<Double>> vectors = gson.fromJson(reader, Map.class);
		for (String title : vectors.keySet()) {
			double[] hashVector = new double[vectorSize];
			List<Double> hashSet = vectors.get(title);
			for (Double d : hashSet) {
				hashVector[d.intValue()] = 1;
			}
			BasicMLData d1 = new BasicMLData(hashVector);
			network.compute(d1);
			double[] out = new double[300];
			for (int i = 0; i < out.length; i++) {
				out[i] = network.getLayerOutput(2, i);
			}

			embeddings.put(title, out);
		}
		try {

			Writer writer = new FileWriter("hash_embeddings.json");
			gson = new GsonBuilder().create();
			gson.toJson(embeddings, writer);
			writer.close();
		} catch (Exception e) {
			// TODO: handle exception
		}

	}

	public static void train(final String args[]) throws FileNotFoundException {

		Reader reader = new FileReader("hashvectors_5000.json");
		Gson gson = new GsonBuilder().create();
		Map<String, List<Double>> vectors = gson.fromJson(reader, Map.class);

		boolean loaddata = true;
		Visualizer visualizer = new Visualizer();
		NetworkFactory networkFactory = new NetworkFactory();
		String outputDir = "output/";
		FileManager fileManager = new FileManager(outputDir, outputDir);
		Trainer trainer = new Trainer(0.01, 0.1, 2, 2);
		List<Integer> encoderHiddenLayersSizes = new ArrayList<Integer>();
		encoderHiddenLayersSizes.add(1250);
		encoderHiddenLayersSizes.add(300);

		double[][] trainingData = new double[7500][5000];
		double[][] validationData = new double[2500][5000];
		if (loaddata == true) {
			int training = 0, validation = 0, i = 0;
			int interval = 4;

			for (String title : vectors.keySet()) {
				double[] hashVector = new double[vectorSize];
				List<Double> hashSet = vectors.get(title);
				for (Double d : hashSet) {
					hashVector[d.intValue()] = 1;
				}
				if (i++ % interval != 3) {
					trainingData[training++] = hashVector;
				} else {
					validationData[validation++] = hashVector;
				}
				if (i >= 10000) {
					break;
				}
			}
			Writer writer;
			try {
				writer = new FileWriter("training3h.json");
				gson = new GsonBuilder().create();
				gson.toJson(trainingData, writer);

				writer.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			try {
				writer = new FileWriter("validation3h.json");
				gson = new GsonBuilder().create();
				gson.toJson(validationData, writer);

				writer.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} else {
			try {
				gson = new GsonBuilder().create();
				reader = new FileReader("training3h.json");
				trainingData = new Gson().fromJson(reader,
						new TypeToken<double[][]>() {
						}.getType());
				reader = new FileReader("validation3h.json");
				validationData = new Gson().fromJson(reader,
						new TypeToken<double[][]>() {
						}.getType());
			} catch (Exception e) {

			}
		}
		AutoEncoder2 autoEncoder = new AutoEncoder2(encoderHiddenLayersSizes,
				trainingData, validationData, networkFactory, trainer,
				visualizer, fileManager, "1");
		train(autoEncoder, trainingData, validationData, vectors);
	}

	public static void train(AutoEncoder2 autoEncoder, double[][] trainingData,
			double[][] validationData, Map<String, List<Double>> vectors) {
		Map<String, double[]> embeddings = new HashMap<String, double[]>();
		autoEncoder.train();
		try {
			autoEncoder.save("INFOBOXEMBEDDINGHASH3GRAM.model");
		} catch (Exception e) {
			// TODO: handle exception
		}
		for (String title : vectors.keySet()) {
			double[] hashVector = new double[vectorSize];
			List<Double> hashSet = vectors.get(title);
			for (Double d : hashSet) {
				hashVector[d.intValue()] = 1;
			}
			BasicMLData d1 = new BasicMLData(hashVector);
			double[] out = autoEncoder.compute(d1, 3, 300);
			embeddings.put(title, out);
		}
		try {

			Writer writer = new FileWriter("hash_embeddings.json");
			Gson gson = new GsonBuilder().create();
			gson.toJson(embeddings, writer);
			writer.close();
		} catch (Exception e) {
			// TODO: handle exception
		}
		// DBCollection entitiesDB =
		// MONGODB.getCollection(Entity.COLLECTION_NAME);
		// DBCursor cursor = entitiesDB.find();
		// while (cursor.hasNext()) {
		// try {
		// DBObject object = cursor.next();
		// Entity e = convertJSONToPojo(object.toString());
		// String id = e.getId().substring(1, e.getId().length());
		//
		// double[] hashVector = new double[vectorSize];
		// List<Double> hashSet = vectors.get(e.getUrl());
		// for (Double d : hashSet) {
		// hashVector[d.intValue()] = 1;
		// }
		// BasicMLData d1 = new BasicMLData(hashVector);
		// double[] out = autoEncoder.compute(d1, 3, 300);
		// // LOGGER.info(out);
		// object.put(Entity.SEMANTICEMBEDDINGAUTOENCODER3GRAM, out);
		// entitiesDB.save(object);
		// } catch (Exception e) {
		// // TODO: handle exception
		// }
		// }

	}

	public BasicNetwork preTrain() {
		LOGGER.info("pretraining");
		AutoEncoder encoder = new AutoEncoder();

		File folder = new File(Property.getInstance().get("featuresDirectory"));
		File[] listOfFiles = folder.listFiles();
		List<String> entities = new ArrayList<String>();
		int interval = Integer.parseInt(Property.getInstance().get(
				"autoencoder.interval"));
		int i = 0;
		for (File file : listOfFiles) {
			if (file.isFile()) {
				String key = file.getName().replace(".dat", "");
				// if (EntitySearchEngine.getInstance().getPage(
				// Integer.parseInt(key)) != null) {
				// entities.add(key);
				// }
				if (i++ % interval == 0) {
					double[] features = getFeatureVector(key);
					encoder.addData(features);
				}
			}
		}

		encoder.addLayer(new ActivationTANH(), 600);
		encoder.addLayer(new ActivationTANH(), 150);
		network = encoder.getNetwork(2);
		return network;
	}

	public BasicNetwork load(String fileName) {
		LOGGER.info("Loading network");

		network = (BasicNetwork) EncogDirectoryPersistence.loadObject(new File(
				fileName));
		LOGGER.info("Network loaded");
		return network;

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
		CosineSimilarity cs = new CosineSimilarity();
		File folder = new File(Property.getInstance().get("featuresDirectory"));
		File[] listOfFiles = folder.listFiles();
		HashMap<String, double[]> embeddings = new HashMap<String, double[]>();
		for (File file : listOfFiles) {
			if (file.isFile()) {
				String key = file.getName().replace(".dat", "");
				byte[] values = FileUtils.read(file.getAbsolutePath());
				double[] features = toDoubleArray(values);
				BasicMLData d1 = new BasicMLData(features);
				double[] e1 = cs.normalize(network.compute(d1).getData());
				embeddings.put(key, e1);
			}
		}
		Writer writer;
		try {
			writer = new FileWriter("autoencoder_embeddings_new.json");
			Gson gson = new GsonBuilder().create();
			gson.toJson(embeddings, writer);

			writer.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

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

	private static Entity convertJSONToPojo(String json) {

		Type type = new TypeToken<Entity>() {
		}.getType();

		return new Gson().fromJson(json, type);

	}
}
