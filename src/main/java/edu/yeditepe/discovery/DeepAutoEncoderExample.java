//package edu.yeditepe.discovery;
//
//import java.util.Arrays;
//
//import org.deeplearning4j.datasets.fetchers.MnistDataFetcher;
//import org.deeplearning4j.datasets.iterator.impl.MnistDataSetIterator;
//import org.deeplearning4j.nn.api.OptimizationAlgorithm;
//import org.deeplearning4j.nn.conf.MultiLayerConfiguration;
//import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
//import org.deeplearning4j.nn.conf.layers.OutputLayer;
//import org.deeplearning4j.nn.conf.layers.RBM;
//import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
//import org.deeplearning4j.optimize.api.IterationListener;
//import org.deeplearning4j.optimize.listeners.ScoreIterationListener;
//import org.nd4j.linalg.api.ndarray.INDArray;
//import org.nd4j.linalg.dataset.DataSet;
//import org.nd4j.linalg.dataset.api.iterator.DataSetIterator;
//import org.nd4j.linalg.lossfunctions.LossFunctions;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//
///**
// * @author Adam Gibson
// */
//public class DeepAutoEncoderExample {
//
//	private static Logger log = LoggerFactory
//			.getLogger(DeepAutoEncoderExample.class);
//
//	public static void main(String[] args) throws Exception {
//		final int numRows = 28;
//		final int numColumns = 28;
//		int seed = 123;
//		int numSamples = MnistDataFetcher.NUM_EXAMPLES;
//		int batchSize = 1000;
//		int iterations = 1;
//		int listenerFreq = iterations / 5;
//
//		log.info("Load data....");
//		DataSetIterator iter = new MnistDataSetIterator(batchSize, numSamples,
//				true);
//
//		log.info("Build model....");
//		MultiLayerConfiguration conf = new NeuralNetConfiguration.Builder()
//				.seed(seed)
//				.iterations(iterations)
//				.optimizationAlgo(
//						OptimizationAlgorithm.STOCHASTIC_GRADIENT_DESCENT)
//				.list(4)
//				.layer(0,
//						new RBM.Builder()
//								.nIn(numRows * numColumns)
//								.nOut(1000)
//								.lossFunction(
//										LossFunctions.LossFunction.RMSE_XENT)
//								.build())
//				.layer(1,
//						new RBM.Builder()
//								.nIn(1000)
//								.nOut(500)
//								.lossFunction(
//										LossFunctions.LossFunction.RMSE_XENT)
//								.build())
//
//				.layer(2,
//						new RBM.Builder()
//								.nIn(500)
//								.nOut(1000)
//								.lossFunction(
//										LossFunctions.LossFunction.RMSE_XENT)
//								.build())
//				.layer(3,
//						new OutputLayer.Builder(
//								LossFunctions.LossFunction.RMSE_XENT).nIn(1000)
//								.nOut(numRows * numColumns).build())
//				.pretrain(true).backprop(true).build();
//
//		MultiLayerNetwork model = new MultiLayerNetwork(conf);
//		model.init();
//
//		model.setListeners(Arrays
//				.asList((IterationListener) new ScoreIterationListener(
//						listenerFreq)));
//
//		log.info("Train model....");
//		while (iter.hasNext()) {
//			DataSet next = iter.next();
//			model.fit(new DataSet(next.getFeatureMatrix(), next
//					.getFeatureMatrix()));
//			INDArray output = model.output(next.getFeatureMatrix());
//			log.info(output.toString());
//		}
//
//	}
//
// }
