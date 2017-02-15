package edu.yeditepe.deep;

import java.util.List;

import vectorspace.CosineSimilarity;

public class AveragePooling {
	private static AveragePooling instance = new AveragePooling();
	private static CosineSimilarity cs = new CosineSimilarity();

	private AveragePooling() {

	}

	public double getSimilarity(List<Double> e1, List<Double> e2) {

		if (e1 != null && e2 != null) {
			double value = cs.cosineSimilarity(e1, e2);
			if (value > 0) {
				return value;
			}
			return 0;
		} else {
			return 0;
		}
	}

	public double getSimilarity(double[] e1, double[] e2) {

		if (e1 != null && e2 != null) {
			double value = cs.cosineSimilarity(e1, e2);
			if (value > 0) {
				return value;
			}
			return 0;
		} else {
			return 0;
		}
	}

	public static AveragePooling getInstance() {
		return instance;
	}
}
