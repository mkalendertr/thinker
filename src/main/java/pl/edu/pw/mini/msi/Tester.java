package pl.edu.pw.mini.msi;

import org.encog.ml.MLRegression;
import org.encog.ml.data.MLData;
import org.encog.ml.data.MLDataPair;
import org.encog.ml.data.MLDataSet;
import org.encog.util.arrayutil.NormalizedField;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class Tester {

    private final List<NormalizedField> outputNormalizers;

    public Tester(List<NormalizedField> outputNormalizers) {
        this.outputNormalizers = outputNormalizers;
    }

    public Collection<List<Double>> testNetwork(MLRegression network, MLDataSet input) {
        Collection<List<Double>> result = new ArrayList<>(input.size());
        input.forEach(expected -> {
            MLData actual = network.compute(expected.getInput());
            result.add(computeDifference(deNormalize(expected.getIdeal()), deNormalize(actual)));
        });
        return result;
    }

    private List<Double> computeDifference(List<Double> expectedDeNormalized, List<Double> actualDeNormalized) {
        List<Double> difference = new ArrayList<>();
        for (int i = 0; i < actualDeNormalized.size(); i++) {
            difference.add(expectedDeNormalized.get(i) - actualDeNormalized.get(i));
        }
        return difference;
    }

    public Collection<List<Double>> getComputedResults(MLRegression network, MLDataSet input) {
        Collection<List<Double>> result = new ArrayList<>(input.size());
        input.forEach(expected -> {
            MLData actual = network.compute(expected.getInput());
            result.add(computeResults(deNormalize(expected.getIdeal()), deNormalize(actual)));
        });
        return result;
    }

    private List<Double> computeResults(List<Double> expectedDeNormalized, List<Double> actualDeNormalized) {
        // computed results should contain ideal x,y,z and then computed x,y,z in each row
        // for easier processing
        List<Double> computedResults = new ArrayList<>();
        computedResults.addAll(expectedDeNormalized.stream().collect(Collectors.toList()));
        computedResults.addAll(actualDeNormalized.stream().collect(Collectors.toList()));
        return computedResults;
    }

    private List<Double> deNormalize(MLData actual) {
        List<Double> actualDeNormalized = new ArrayList<>();
        for (int i = 0; i < actual.size(); i++) {
            actualDeNormalized.add(outputNormalizers.get(i).deNormalize(actual.getData(i)));
        }
        return actualDeNormalized;
    }
}
