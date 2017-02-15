package edu.yeditepe.deep.autoencoder;

import java.util.ArrayList;
import java.util.List;

import org.encog.engine.network.activation.ActivationFunction;
import org.encog.engine.network.activation.ActivationLinear;
import org.encog.engine.network.activation.ActivationSoftMax;
import org.encog.engine.network.activation.ActivationTANH;
import org.encog.ml.data.MLData;
import org.encog.ml.data.MLDataPair;
import org.encog.ml.data.MLDataSet;
import org.encog.ml.data.basic.BasicMLData;
import org.encog.ml.data.basic.BasicMLDataPair;
import org.encog.ml.data.basic.BasicMLDataSet;
import org.encog.neural.networks.BasicNetwork;
import org.encog.neural.networks.layers.BasicLayer;
import org.encog.neural.networks.training.propagation.Propagation;
import org.encog.neural.networks.training.propagation.resilient.ResilientPropagation;

public class AutoEncoder {

	public class MLParams {
		public double[] weights;
		public ActivationFunction func;
		public int nodes;

		public MLParams(double[] weights, ActivationFunction func, int nodes) {
			this.weights = weights;
			this.func = func;
			this.nodes = nodes;
		}
	}

	private List<MLParams> params;
	private MLDataSet dataset;
	private ActivationFunction func;

	private BasicNetwork network;
	private BasicNetwork hiddenNet;
	private MLDataSet intermediateDataset;

	public AutoEncoder() {
		params = new ArrayList<MLParams>();
		dataset = new BasicMLDataSet();
	}

	public void setData(MLDataSet dataset) {
		this.dataset = dataset;
	}

	public void setData(double[][] p) {
		for (int i = 0; i < p.length; i++) {
			double[] input = p[i];

			MLDataPair pair = new BasicMLDataPair(new BasicMLData(input),
					new BasicMLData(input));

			dataset.add(pair);
		}
	}

	public void addData(double[] p) {
		MLDataPair pair = new BasicMLDataPair(new BasicMLData(p),
				new BasicMLData(p));
		dataset.add(pair);
	}

	public void setFunc(ActivationFunction func) {
		this.func = func;
	}

	public void addLayer(ActivationFunction func, int nodes) {

		if (params.size() > 0) {
			buildNetwork();

			transformData();
		} else {
			intermediateDataset = this.dataset;
		}

		network = new BasicNetwork();

		network.addLayer(new BasicLayer(new ActivationLinear(), true,
				intermediateDataset.getInputSize()));

		network.addLayer(new BasicLayer(func, true, nodes));

		network.addLayer(new BasicLayer(new ActivationTANH(), false,
				intermediateDataset.getIdealSize()));

		network.getStructure().finalizeStructure();

		network.reset();

		train(nodes);
	}

	public void train(int nodes) {
		Propagation propagation;

		// propagation= new QuickPropagation(network,
		// intermediateDataset, 0.01);
		propagation = new ResilientPropagation(network, intermediateDataset);

		propagation.setThreadCount(2);

		// for(int i = 0 ; i < 100; i ++) {
		propagation.iteration();

		System.out.println("In deep layer: " + params.size()
				+ " Training error " + propagation.getError());
		// }

		int fromNodes = network.getInputCount() + 1;
		int toNodes = network.getLayerNeuronCount(1); // the next layer

		int numWeight = fromNodes * toNodes;

		double[] weights = new double[numWeight];
		int k = 0;
		for (int i = 0; i < fromNodes; i++) {
			for (int j = 0; j < toNodes; j++) {
				// FIXME, bug
				weights[k++] = network.getWeight(0, i, j);
			}
		}

		ActivationFunction func = network.getActivation(1);

		MLParams param = new MLParams(weights, func, nodes);

		params.add(param);

		System.out.println("Add weight: " + weights.length
				+ "\n and the activation function: " + func.toString());
	}

	private void transformData() {
		intermediateDataset = new BasicMLDataSet();
		for (int i = 0; i < this.dataset.getRecordCount(); i++) {
			MLData input = hiddenNet.compute(dataset.get(i).getInput());
			intermediateDataset.add(input, input);
		}
	}

	private void buildNetwork() {

		hiddenNet = new BasicNetwork();

		hiddenNet.addLayer(new BasicLayer(new ActivationLinear(), true, dataset
				.getInputSize()));

		for (int i = 0; i < params.size() - 1; i++) {
			hiddenNet.addLayer(new BasicLayer(params.get(i).func, true, params
					.get(i).nodes));
		}

		hiddenNet.addLayer(new BasicLayer(params.get(params.size() - 1).func,
				false, params.get(params.size() - 1).nodes));

		hiddenNet.getStructure().finalizeStructure();

		for (int i = 0; i < params.size(); i++) {
			double[] layer_weights = params.get(i).weights;
			int j = 0;
			int fromCount = network.getLayerTotalNeuronCount(i);
			int toCount = network.getLayerNeuronCount(i + 1);

			for (int fromNeuron = 0; fromNeuron < fromCount; fromNeuron++) {
				for (int toNeuron = 0; toNeuron < toCount; toNeuron++) {
					hiddenNet.setWeight(i, fromNeuron, toNeuron,
							layer_weights[j++]);
				}
			}
		}

	}

	public double[] represent(int layer) {

		assert (layer <= params.size());

		hiddenNet = new BasicNetwork();

		hiddenNet.addLayer(new BasicLayer(new ActivationLinear(), true, dataset
				.getInputSize()));

		for (int i = 0; i < params.size() - 1 && i < layer - 1; i++) {
			hiddenNet.addLayer(new BasicLayer(params.get(i).func, true, params
					.get(i).nodes));
		}

		hiddenNet.addLayer(new BasicLayer(params.get(layer - 1).func, false,
				params.get(layer - 1).nodes));

		hiddenNet.getStructure().finalizeStructure();

		for (int i = 0; i < params.size(); i++) {
			double[] layer_weights = params.get(i).weights;
			int j = 0;
			int fromCount = hiddenNet.getLayerTotalNeuronCount(i);
			int toCount = hiddenNet.getLayerNeuronCount(i + 1);

			for (int fromNeuron = 0; fromNeuron < fromCount; fromNeuron++) {
				for (int toNeuron = 0; toNeuron < toCount; toNeuron++) {
					hiddenNet.setWeight(i, fromNeuron, toNeuron,
							layer_weights[j++]);
				}
			}
		}

		return hiddenNet.compute(this.dataset.get(0).getInput()).getData();
	}

	public BasicNetwork getNetworkSoftmax(int layer) {

		hiddenNet = new BasicNetwork();

		hiddenNet.addLayer(new BasicLayer(new ActivationLinear(), true, dataset
				.getInputSize()));

		for (int i = 0; i < params.size() - 1 && i < layer - 1; i++) {
			hiddenNet.addLayer(new BasicLayer(params.get(i).func, true, params
					.get(i).nodes));
		}

		hiddenNet.addLayer(new BasicLayer(params.get(layer - 1).func, false,
				params.get(layer - 1).nodes));
		hiddenNet.addLayer(new BasicLayer(new ActivationSoftMax(), false, 2));

		hiddenNet.getStructure().finalizeStructure();
		hiddenNet.reset();
		for (int i = 0; i < params.size(); i++) {
			double[] layer_weights = params.get(i).weights;
			int j = 0;
			int fromCount = hiddenNet.getLayerTotalNeuronCount(i);
			int toCount = hiddenNet.getLayerNeuronCount(i + 1);

			for (int fromNeuron = 0; fromNeuron < fromCount; fromNeuron++) {
				for (int toNeuron = 0; toNeuron < toCount; toNeuron++) {
					hiddenNet.setWeight(i, fromNeuron, toNeuron,
							layer_weights[j++]);
				}
			}
		}

		return hiddenNet;
	}

	public BasicNetwork getNetwork(int layer) {

		hiddenNet = new BasicNetwork();

		hiddenNet.addLayer(new BasicLayer(new ActivationLinear(), true, dataset
				.getInputSize()));

		for (int i = 0; i < params.size() - 1 && i < layer - 1; i++) {
			hiddenNet.addLayer(new BasicLayer(params.get(i).func, true, params
					.get(i).nodes));
		}

		hiddenNet.addLayer(new BasicLayer(params.get(layer - 1).func, false,
				params.get(layer - 1).nodes));

		hiddenNet.getStructure().finalizeStructure();
		hiddenNet.reset();
		for (int i = 0; i < params.size(); i++) {
			double[] layer_weights = params.get(i).weights;
			int j = 0;
			int fromCount = hiddenNet.getLayerTotalNeuronCount(i);
			int toCount = hiddenNet.getLayerNeuronCount(i + 1);

			for (int fromNeuron = 0; fromNeuron < fromCount; fromNeuron++) {
				for (int toNeuron = 0; toNeuron < toCount; toNeuron++) {
					hiddenNet.setWeight(i, fromNeuron, toNeuron,
							layer_weights[j++]);
				}
			}
		}

		return hiddenNet;
	}

}
