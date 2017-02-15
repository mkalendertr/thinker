package edu.yeditepe.nounphrase;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import de.bwaldvogel.liblinear.Feature;
import de.bwaldvogel.liblinear.FeatureNode;
import de.bwaldvogel.liblinear.Linear;
import de.bwaldvogel.liblinear.Model;
import de.bwaldvogel.liblinear.Parameter;
import de.bwaldvogel.liblinear.Problem;
import de.bwaldvogel.liblinear.SolverType;

public class NounPhraseTrain {
	public static void main(String[] args) throws IOException {

		Reader reader = new FileReader("noun_neg_train.json");
		Gson gson = new GsonBuilder().create();
		List<List<Double>> neg = new Gson().fromJson(reader,
				new TypeToken<List<List<Double>>>() {
				}.getType());

		reader = new FileReader("noun_pos_train.json");
		gson = new GsonBuilder().create();
		List<List<Double>> pos = new Gson().fromJson(reader,
				new TypeToken<List<List<Double>>>() {
				}.getType());
		int trainingSize = pos.size() + neg.size();
		double[] yTraining = new double[trainingSize];
		Feature[][] xTraining = new Feature[trainingSize][];

		int i = 0;
		for (List<Double> list : pos) {
			if (list != null && list.size() > 0) {
				FeatureNode[] features = new FeatureNode[list.size()];
				for (int j = 0; j < features.length; j++) {
					features[j] = new FeatureNode(j + 1, list.get(j));
				}
				xTraining[i] = features;
				yTraining[i] = 1;
				// if (++i >= pos.size()) {
				// break;
				// }
				i++;
			}
		}
		pos.clear();
		pos = null;
		for (List<Double> list : neg) {
			if (list != null && list.size() > 0) {
				FeatureNode[] features = new FeatureNode[list.size()];
				for (int j = 0; j < features.length; j++) {
					features[j] = new FeatureNode(j + 1, list.get(j));
				}
				xTraining[i] = features;
				yTraining[i] = 0;
				// if (++i >= trainingSize * 2) {
				// break;
				// }
				i++;
			}
		}
		neg.clear();
		neg = null;

		Problem problem = new Problem();
		problem.l = xTraining.length;
		problem.n = xTraining[0].length;
		problem.x = xTraining;
		problem.y = yTraining;
		SolverType solver = SolverType.L2R_LR_DUAL; // -s 0
		double C = 1.0; // cost of constraints violation
		double eps = 0.01; // stopping criteria

		Parameter parameter = new Parameter(solver, C, eps);
		Model model = Linear.train(problem, parameter);
		File modelFile = new File("spotter_lr.model");
		model.save(modelFile);

	}
}
