package edu.yeditepe.similarity;

import java.io.IOException;

import org.apache.log4j.Logger;

public class LibLinearSimilarity {
	private static final Logger LOGGER = Logger
			.getLogger(LibLinearSimilarity.class);

	private static String modelFile = "embedding_network_softmax_test.model";

	private LibLinear ll = new LibLinear();

	private static LibLinearSimilarity instance = new LibLinearSimilarity();

	public static LibLinearSimilarity getInstance() {
		return instance;
	}

	public static void setInstance(LibLinearSimilarity instance) {
		LibLinearSimilarity.instance = instance;
	}

	public static void main(String[] args) {
		LibLinearSimilarity es = LibLinearSimilarity.getInstance();
		LOGGER.info(es.getSimilarity("393217", "958501"));
	}

	private LibLinearSimilarity() {

		ll = new LibLinear();
		try {
			ll.load();
		} catch (IOException e) {
			LOGGER.error(e);
		}

	}

	public double getSimilarity(String input1, String input2) {
		try {
			double sim = ll.evaluate(input1, input2);
			// if (sim > 0.5) {
			// return 1;
			//
			// } else {
			// return 0;
			// }
			return sim;
		} catch (Exception e) {
			// LOGGER.error(e);
		}
		return 0;

	}

}
