package pl.edu.pw.mini.msi;

import org.encog.neural.networks.BasicNetwork;

public class Visualizer {
    String dumpNetworkToDot(BasicNetwork basicNetwork) {
        StringBuilder codeBuilder = new StringBuilder()
                .append("\n\ndigraph g{\n" + "label = \"")
                .append(basicNetwork.getFactoryArchitecture())
                .append("\"")
                .append("node [fixedsize=true, label=\"\", shape=circle, color=black];\n")
                .append("rankdir=LR\n")
                .append("\n");
        for (int layerId = 0; layerId < basicNetwork.getLayerCount() - 1; layerId++) {
            codeBuilder.append(String.format("\nsubgraph layer_%d {\n", layerId));
            for (int fromNeuron = 0; fromNeuron < basicNetwork.getLayerNeuronCount(layerId); fromNeuron++) {
                codeBuilder.append(String.format("layer_%d_%d\n", layerId, fromNeuron));
            }
            codeBuilder.append("}\n");
            for (int fromNeuron = 0; fromNeuron < basicNetwork.getLayerNeuronCount(layerId); fromNeuron++) {
                for (int toNeuron = 0; toNeuron < basicNetwork.getLayerNeuronCount(layerId + 1); toNeuron++) {
                    codeBuilder.append(String.format("layer_%d_%d", layerId, fromNeuron));
                    codeBuilder.append(" -> ");
                    codeBuilder.append(String.format("layer_%d_%d", layerId + 1, toNeuron));
                    codeBuilder.append(String.format(" [label=%f]", basicNetwork.getWeight(layerId, fromNeuron, toNeuron)));
                    codeBuilder.append(";\n");
                }
            }
        }
        codeBuilder.append("}\n\n");
        return codeBuilder.toString();
    }
}