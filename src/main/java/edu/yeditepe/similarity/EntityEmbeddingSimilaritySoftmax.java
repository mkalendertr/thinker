package edu.yeditepe.similarity;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import edu.yeditepe.deep.EntityEmbeddingSoftmax;

public class EntityEmbeddingSimilaritySoftmax {
	private static final Logger LOGGER = Logger
			.getLogger(EntityEmbeddingSimilaritySoftmax.class);

	private static String modelFile = "embedding_network_autoencoder.model";

	private EntityEmbeddingSoftmax nn = new EntityEmbeddingSoftmax();

	private static EntityEmbeddingSimilaritySoftmax instance = new EntityEmbeddingSimilaritySoftmax();

	public static EntityEmbeddingSimilaritySoftmax getInstance() {
		return instance;
	}

	public static void setInstance(EntityEmbeddingSimilaritySoftmax instance) {
		EntityEmbeddingSimilaritySoftmax.instance = instance;
	}

	public static void main(String[] args) {
		EntityEmbeddingSimilaritySoftmax es = EntityEmbeddingSimilaritySoftmax
				.getInstance();
		LOGGER.info(es.getSimilarity("393217", "958501"));

		List<String> list = new ArrayList<String>();
		list.add("958501");
		// list.add("10");
		double[] e1 = es.getFeatureVector("393217");
		double[] e2 = es.getFeatureVector(list);
		LOGGER.info(es.getSimilarity(e1, e2));

	}

	private EntityEmbeddingSimilaritySoftmax() {

		nn = new EntityEmbeddingSoftmax();
		nn.load(modelFile);

	}

	public double getSimilarity(String input1, String input2) {
		return nn.evaluate(input1, input2);
	}

	public double getSimilarity(double[] input1, double[] input2) {
		return nn.evaluate(input1, input2);
	}

	public double[] getFeatureVector(List<String> input) {
		return nn.getFeatureVector(input);
	}

	public double[] getFeatureVector(String input) {
		return nn.getFeatureVector(input);
	}
}
