package pl.edu.pw.mini.msi;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.encog.engine.network.activation.ActivationTANH;
import org.encog.ml.MLRegression;
import org.encog.ml.data.MLData;
import org.encog.ml.data.MLDataPair;
import org.encog.ml.data.basic.BasicMLData;
import org.encog.ml.data.basic.BasicMLDataPair;
import org.encog.ml.data.basic.BasicMLDataSet;
import org.encog.neural.data.basic.BasicNeuralDataSet;
import org.encog.neural.flat.FlatNetwork;
import org.encog.neural.networks.BasicNetwork;
import org.encog.neural.networks.layers.BasicLayer;
import org.encog.persist.EncogDirectoryPersistence;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.common.primitives.Doubles;

public class AutoEncoder2 implements MLRegression {

	private static final Logger LOG = Logger.getLogger(AutoEncoder2.class
			.getName());

	private final NetworkFactory networkFactory;
	private final Trainer trainer;

	private final List<Integer> encoderHiddenLayersSizes;
	private final BasicMLDataSet trainingDataSet;
	private final BasicMLDataSet validationDataSet;
	private final Visualizer visualizer;
	private final FileManager fileManager;

	private final String dataFileName;
	private BasicNetwork network;
	private BasicNetwork predictor;

	@Deprecated
	public AutoEncoder2(List<Integer> encoderHiddenLayersSizes,
			double[][] trainingData, double[][] validationData,
			NetworkFactory networkFactory, Trainer trainer,
			Visualizer visualizer, FileManager fileManager, String dataFileName) {
		this(encoderHiddenLayersSizes, new BasicNeuralDataSet(trainingData,
				trainingData), new BasicNeuralDataSet(validationData,
				validationData), networkFactory, trainer, visualizer,
				fileManager, dataFileName);
	}

	public AutoEncoder2(List<Integer> encoderHiddenLayersSizes,
			BasicMLDataSet trainingDataSet, BasicMLDataSet validationDataSet,
			NetworkFactory networkFactory, Trainer trainer,
			Visualizer visualizer, FileManager fileManager, String dataFileName) {
		this.encoderHiddenLayersSizes = encoderHiddenLayersSizes;
		this.networkFactory = networkFactory;
		this.trainer = trainer;
		this.visualizer = visualizer;
		this.trainingDataSet = getAutoencoderDataSet(trainingDataSet);
		this.validationDataSet = getAutoencoderDataSet(validationDataSet);
		this.fileManager = fileManager;
		this.dataFileName = dataFileName;
		buildAutoencoderStructure();
	}

	private BasicMLDataSet getAutoencoderDataSet(BasicMLDataSet dataSet) {
		return new BasicMLDataSet(dataSet
				.getData()
				.stream()
				.map(data -> new BasicMLDataPair(data.getInput(), data
						.getInput())).collect(Collectors.toList()));
	}

	public BasicNetwork getEncoder() {
		BasicNetwork encoder = new BasicNetwork();
		for (int i = 0; i < network.getLayerCount() / 2 + 1; i++) {
			encoder.addLayer(networkFactory.getBasicLayer(network
					.getLayerNeuronCount(i)));
		}
		encoder.getStructure().finalizeStructure();
		for (int layerId = 0; layerId < encoder.getLayerCount() - 1; layerId++) {
			for (int fromNeuron = 0; fromNeuron < encoder
					.getLayerNeuronCount(layerId); fromNeuron++) {
				for (int toNeuron = 0; toNeuron < encoder
						.getLayerNeuronCount(layerId + 1); toNeuron++) {
					encoder.setWeight(layerId, fromNeuron, toNeuron,
							network.getWeight(layerId, fromNeuron, toNeuron));
				}
			}
		}
		return encoder;
	}

	public BasicNetwork getPredictor() {
		if (predictor == null) {
			throw new IllegalStateException("You must train predictor before");
		}
		return predictor;
	}

	private BasicNetwork getEncoderWithCustomOutputLayer(int sizeOfOutputLayer) {
		BasicNetwork encoder = new BasicNetwork();
		for (int i = 0; i < network.getLayerCount() / 2 + 1; i++) {
			encoder.addLayer(networkFactory.getBasicLayer(network
					.getLayerNeuronCount(i)));
		}
		encoder.addLayer(networkFactory.getBasicLayer(sizeOfOutputLayer));
		encoder.getStructure().finalizeStructure();
		for (int layerId = 0; layerId < encoder.getLayerCount() - 2; layerId++) {
			for (int fromNeuron = 0; fromNeuron < encoder
					.getLayerNeuronCount(layerId); fromNeuron++) {
				for (int toNeuron = 0; toNeuron < encoder
						.getLayerNeuronCount(layerId + 1); toNeuron++) {
					encoder.setWeight(layerId, fromNeuron, toNeuron,
							network.getWeight(layerId, fromNeuron, toNeuron));
				}
			}
		}
		return encoder;
	}

	public void pretrainPredictor(BasicMLDataSet trainingDataSet,
			BasicMLDataSet validationDataSet) {
		predictor = getEncoderWithCustomOutputLayer(trainingDataSet
				.getIdealSize());
		BasicMLDataSet layerTrainingData = getPredictorLastLayerTrainingDataData(trainingDataSet);
		BasicMLDataSet layerValidationData = getPredictorLastLayerTrainingDataData(validationDataSet);
		BasicNetwork lastLayer = networkFactory.getSimpleFeedForwardNetwork(
				layerTrainingData.getInputSize(),
				layerTrainingData.getIdealSize());
		trainer.trainToError(lastLayer, layerTrainingData, layerValidationData);
		LOG.info("Last layer:" + lastLayer.getFactoryArchitecture());
		int layerId = predictor.getLayerCount() - 2;
		for (int fromNeuron = 0; fromNeuron < predictor
				.getLayerNeuronCount(layerId); fromNeuron++) {
			for (int toNeuron = 0; toNeuron < predictor
					.getLayerNeuronCount(layerId + 1); toNeuron++) {
				predictor.setWeight(layerId, fromNeuron, toNeuron,
						lastLayer.getWeight(0, fromNeuron, toNeuron));
			}
		}
		LOG.info("Predictor:" + predictor.getFactoryArchitecture());
		fileManager.saveVisualisationToFile(
				visualizer.dumpNetworkToDot(predictor),
				predictor.getFactoryArchitecture());
	}

	private BasicMLDataSet getPredictorLastLayerTrainingDataData(
			BasicMLDataSet trainingData) {
		BasicMLDataSet outputs = new BasicMLDataSet();
		for (MLDataPair dataPair : trainingData.getData()) {
			predictor.compute(new BasicMLData(dataPair.getInputArray()));
			int lastLayer = predictor.getLayerCount() - 2;
			BasicMLData data = new BasicMLData(
					predictor.getLayerNeuronCount(lastLayer));
			for (int i = 0; i < predictor.getLayerNeuronCount(lastLayer); i++) {
				data.setData(i, predictor.getLayerOutput(lastLayer, i));
			}
			outputs.add(new BasicMLDataPair(data, dataPair.getIdeal()));
		}
		return outputs;
	}

	public void train() {
		// TODO: Move here training and validation data set
		buildAutoEncoder();
		String errorString = trainer.trainToError(network, trainingDataSet,
				validationDataSet);

		fileManager.saveErrorDataToFile(errorString, dataFileName);
	}

	private void buildAutoEncoder() {
		buildAutoencoderStructure();
		List<Double> decoderWeights = new LinkedList<>();
		List<Double> coderWeights = new LinkedList<>();
		BasicMLDataSet trainingData = trainingDataSet;
		BasicMLDataSet validationData = validationDataSet;

		for (int layerId = 0; layerId < encoderHiddenLayersSizes.size(); layerId++) {
			BasicNetwork layerNetwork = trainLayerNetwork(layerId,
					encoderHiddenLayersSizes.get(layerId), trainingData,
					validationData);
			trainingData = getNextTrainingData(trainingData, layerNetwork);
			validationData = getNextTrainingData(validationData, layerNetwork);

			FlatNetwork layerNetworkFlat = layerNetwork.getFlat();
			decoderWeights.addAll(Doubles.asList(layerNetworkFlat.getWeights())
					.subList(0, layerNetworkFlat.getWeightIndex()[1]));
			coderWeights = Stream.concat(
					Doubles.asList(layerNetworkFlat.getWeights())
							.subList(layerNetworkFlat.getWeightIndex()[1],
									layerNetworkFlat.getWeights().length)
							.stream(), coderWeights.stream()).collect(
					Collectors.<Double> toList());
		}

		network.decodeFromArray(Doubles.toArray(Stream.concat(
				decoderWeights.stream(), coderWeights.stream()).collect(
				Collectors.<Double> toList())));
		LOG.info("Network:" + network.getFactoryArchitecture());
		fileManager.saveVisualisationToFile(
				visualizer.dumpNetworkToDot(network),
				network.getFactoryArchitecture());
	}

	private void buildAutoencoderStructure() {
		network = new BasicNetwork();
		network.addLayer(new BasicLayer(new ActivationTANH(), false,
				trainingDataSet.getInputSize()));
		for (int i = 0; i < encoderHiddenLayersSizes.size() - 1; i++) {
			network.addLayer(networkFactory
					.getBasicLayer(encoderHiddenLayersSizes.get(i)));
		}
		for (int i = encoderHiddenLayersSizes.size() - 1; i >= 0; i--) {
			network.addLayer(networkFactory
					.getBasicLayer(encoderHiddenLayersSizes.get(i)));
		}
		network.addLayer(networkFactory.getBasicLayer(trainingDataSet
				.getIdealSize()));
		network.getStructure().finalizeStructure();
	}

	private BasicMLDataSet getNextTrainingData(BasicMLDataSet trainingData,
			BasicNetwork layerNetwork) {
		List<MLData> outputs = new ArrayList<>(trainingData.size());
		for (MLDataPair dataPair : trainingData.getData()) {
			layerNetwork.compute(new BasicMLData(dataPair.getInputArray()));
			int middleLayer = 1;
			BasicMLData data = new BasicMLData(
					layerNetwork.getLayerNeuronCount(middleLayer));
			for (int i = 0; i < layerNetwork.getLayerNeuronCount(middleLayer); i++) {
				data.setData(i, layerNetwork.getLayerOutput(middleLayer, i));
			}
			outputs.add(data);
		}
		return new BasicMLDataSet(Lists.transform(outputs,
				new Function<MLData, MLDataPair>() {
					@Override
					public MLDataPair apply(MLData input) {
						return new BasicMLDataPair(input, input);
					}
				}));
	}

	private BasicNetwork trainLayerNetwork(int layerId,
			int hiddenLayerNeuronsCount, BasicMLDataSet trainingData,
			BasicMLDataSet validationData) {
		BasicNetwork network = networkFactory.getSimpleFeedForwardNetwork(
				trainingData.getInputSize(), hiddenLayerNeuronsCount,
				trainingData.getIdealSize());
		String errorString = trainer.trainToError(network, trainingData,
				validationData);
		LOG.info("Layer Network:" + network.getFactoryArchitecture());

		fileManager.saveVisualisationToFile(
				visualizer.dumpNetworkToDot(network), dataFileName, layerId);
		fileManager.saveErrorDataToFile(errorString, dataFileName, layerId);

		return network;
	}

	@Override
	public MLData compute(MLData input) {
		return network.compute(input);
	}

	public double[] compute(MLData input, int layer, int size) {
		network.compute(input);
		double[] result = Arrays.copyOfRange(
				network.getFlat().getLayerOutput(), network.getFlat()
						.getLayerIndex()[layer], network.getFlat()
						.getLayerIndex()[layer] + size);
		return result;
	}

	@Override
	public int getInputCount() {
		return network.getInputCount();
	}

	@Override
	public int getOutputCount() {
		return network.getOutputCount();
	}

	public void save(String file) {
		try {

			EncogDirectoryPersistence.saveObject(new File(file), network);
		} catch (Exception e) {
			// TODO: handle exception
		}
	}

	public void load(String file) {
		try {

			network = (BasicNetwork) EncogDirectoryPersistence
					.loadObject(new File(file));
		} catch (Exception e) {
			// TODO: handle exception
		}
	}
}
