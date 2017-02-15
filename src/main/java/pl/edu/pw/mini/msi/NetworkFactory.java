package pl.edu.pw.mini.msi;

import org.encog.engine.network.activation.ActivationTANH;
import org.encog.neural.networks.BasicNetwork;
import org.encog.neural.networks.layers.BasicLayer;

import java.util.logging.Logger;

public class NetworkFactory {

    private static final Logger LOG = Logger.getLogger(AutoEncoder2.class.getName());

    private int seed;

    public BasicNetwork getSimpleFeedForwardNetwork(int inputLayerSize, int hiddenLayerSize, int outputLayerSize) {
        BasicNetwork basicNetwork = new BasicNetwork();
        basicNetwork.addLayer(getBasicLayer(inputLayerSize));
        if (hiddenLayerSize > 0) {
            basicNetwork.addLayer(getBasicLayer(hiddenLayerSize));
        }
        basicNetwork.addLayer(getBasicLayer(outputLayerSize));
        basicNetwork.getStructure().finalizeStructure();
        if (seed < 0) {
            basicNetwork.reset();
        } else {
            basicNetwork.reset(seed);
        }
        LOG.info("Created:\t" + basicNetwork.getFactoryArchitecture());
        return basicNetwork;
    }

    public BasicNetwork getSimpleFeedForwardNetwork(int inputLayerSize, int outputLayerSize) {
        return getSimpleFeedForwardNetwork(inputLayerSize, 0, outputLayerSize);
    }

    public BasicLayer getBasicLayer(int neuronsCount) {
        return new BasicLayer(new ActivationTANH(), false, neuronsCount);
    }
}
