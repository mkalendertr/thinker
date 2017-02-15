package vectorspace;

import java.util.List;

import org.apache.log4j.Logger;

public class CosineSimilarity {
	private static final Logger LOGGER = Logger
			.getLogger(CosineSimilarity.class);

	public static void main(String[] args) {
		CosineSimilarity cs = new CosineSimilarity();
		double[] v1 = { 0, 1, 1 };
		double[] v2 = { 1, 1, -1 };
		v1 = cs.normalize(v1);
		v2 = cs.normalize(v2);
		LOGGER.info(cs.cosineSimilarity(v1, v2));
	}

	/**
	 * Method to calculate cosine similarity between two documents.
	 * 
	 * @param docVector1
	 *            : document vector 1 (a)
	 * @param docVector2
	 *            : document vector 2 (b)
	 * @return
	 */
	public double[] normalize(double[] docVector) {
		for (int i = 0; i < docVector.length; i++) {
			docVector[i] = (docVector[i] + 1) / 2;
		}
		return docVector;
	}

	public double cosineSimilarity(double[] docVector1, double[] docVector2) {

		double dotProduct = 0.0;
		double magnitude1 = 0.0;
		double magnitude2 = 0.0;
		double cosineSimilarity = 0.0;

		for (int i = 0; i < docVector1.length; i++) // docVector1 and docVector2
													// must be of same length
		{
			dotProduct += docVector1[i] * docVector2[i]; // a.b
			magnitude1 += Math.pow(docVector1[i], 2); // (a^2)
			magnitude2 += Math.pow(docVector2[i], 2); // (b^2)
		}

		magnitude1 = Math.sqrt(magnitude1);// sqrt(a^2)
		magnitude2 = Math.sqrt(magnitude2);// sqrt(b^2)

		if (magnitude1 != 0.0 | magnitude2 != 0.0) {
			cosineSimilarity = dotProduct / (magnitude1 * magnitude2);
		} else {
			return 0.0;
		}
		return cosineSimilarity;
	}

	public double cosineSimilarity(List<Double> docVector1,
			List<Double> docVector2) {
		try {
			double dotProduct = 0.0;
			double magnitude1 = 0.0;
			double magnitude2 = 0.0;
			double cosineSimilarity = 0.0;

			for (int i = 0; i < docVector1.size(); i++) // docVector1 and
														// docVector2
														// must be of same
														// length
			{
				dotProduct += docVector1.get(i) * docVector2.get(i); // a.b
				magnitude1 += Math.pow(docVector1.get(i), 2); // (a^2)
				magnitude2 += Math.pow(docVector2.get(i), 2); // (b^2)
			}

			magnitude1 = Math.sqrt(magnitude1);// sqrt(a^2)
			magnitude2 = Math.sqrt(magnitude2);// sqrt(b^2)

			if (magnitude1 != 0.0 | magnitude2 != 0.0) {
				cosineSimilarity = dotProduct / (magnitude1 * magnitude2);
			} else {
				return 0.0;
			}
			return cosineSimilarity;
		} catch (Exception e) {
			LOGGER.error(e);
		}
		return 0;

	}
}
