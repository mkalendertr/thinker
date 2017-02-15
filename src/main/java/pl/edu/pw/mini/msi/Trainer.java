package pl.edu.pw.mini.msi;

import java.util.Map;
import java.util.logging.Logger;

import org.encog.ml.data.MLData;
import org.encog.ml.data.MLDataPair;
import org.encog.ml.data.MLDataSet;
import org.encog.neural.networks.BasicNetwork;
import org.encog.neural.networks.training.propagation.resilient.ResilientPropagation;
import org.encog.util.Format;

import com.google.common.collect.ImmutableMap;

public class Trainer {

	private static final Logger LOG = Logger.getLogger(Trainer.class.getName());

	private final double initialUpdate;
	private final double maxStep;
	private final int iterationStep;
	private final int maxIterations;

	public Trainer(double initialUpdate, double maxStep, int iterationStep,
			int maxIterations) {
		this.initialUpdate = initialUpdate;
		this.maxStep = maxStep;
		this.iterationStep = iterationStep;
		this.maxIterations = maxIterations;
	}

	public String trainToError(BasicNetwork network, MLDataSet trainingData,
			MLDataSet validationData) {
		ResilientPropagation train = new ResilientPropagation(network,
				trainingData, initialUpdate, maxStep);
		train.setThreadCount(2);
		LOG.info("Beginning training...");
		StringBuilder errorString = new StringBuilder();
		double oldTestError;
		double testError = Double.MAX_VALUE;
		do {
			oldTestError = testError;
			train.iteration(iterationStep);
			testError = getTestError(network, validationData);
			errorString.append(String.format("%d, %f, %f\n",
					train.getIteration(), train.getError(), testError));

		} while (oldTestError > testError
				&& train.getIteration() < maxIterations);
		train.finishTraining();
		LOG.info("Finish training after " + train.getIteration()
				+ " iterations and error " + train.getError());

		return errorString.toString();
	}

	public double computeErrorOnTestSet(BasicNetwork network, MLDataSet testData) {
		double error = getTestError(network, testData);
		LOG.info(String.format("Test set error %s", Format.formatPercent(error)));

		return error;
	}

	private double getTestError(BasicNetwork network, MLDataSet validationData) {
		double testError = 0.0;
		for (MLDataPair pair : validationData) {
			MLData output = network.compute(pair.getInput());
			testError += distance(pair.getIdeal().getData(), output.getData());
		}
		return testError / validationData.size();
	}

	static public double squaredEuclidean(double[] x, double[] y) {
		if (x.length != y.length)
			throw new RuntimeException(
					"Arguments must have same number of dimensions.");
		double cumssq = 0.0;
		for (int i = 0; i < x.length; i++)
			cumssq += (x[i] - y[i]) * (x[i] - y[i]);
		return cumssq;
	}

	static public double distance(double[] x, double[] y) {
		return Math.sqrt(squaredEuclidean(x, y));
	}

	public Map<String, Number> toMap() {
		return ImmutableMap.of("initialUpdate", initialUpdate, "maxStep",
				maxStep, "iterationStep", iterationStep, "maxIterations",
				maxIterations);
	}
}
