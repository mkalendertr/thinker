package edu.yeditepe.experiment;

import it.cnr.isti.hpc.dexter.disambiguation.TurkishEntityDisambiguator;
import it.cnr.isti.hpc.dexter.entity.EntityMatch;
import it.cnr.isti.hpc.dexter.entity.EntityMatchList;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
/* Copyright 2009-2015 David Hadka
 *
 * This file is part of the MOEA Framework.
 *
 * The MOEA Framework is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at your
 * option) any later version.
 *
 * The MOEA Framework is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with the MOEA Framework.  If not, see <http://www.gnu.org/licenses/>.
 */
import org.moeaframework.Executor;
import org.moeaframework.core.NondominatedPopulation;
import org.moeaframework.core.Solution;
import org.moeaframework.core.variable.EncodingUtils;
import org.moeaframework.core.variable.RealVariable;
import org.moeaframework.problem.AbstractProblem;

import edu.yeditepe.model.EntityScores;
import edu.yeditepe.utils.Property;

/**
 * Demonstrates how a new problem is defined and used within the MOEA Framework.
 */
public class ParameterOptimization extends AbstractProblem {
	/**
	 * Implementation of the DTLZ2 function.
	 */
	private static final Logger LOGGER = Logger
			.getLogger(ParameterOptimization.class);
	private static Map<String, List<Map<String, EntityScores>>> training = new HashMap<String, List<Map<String, EntityScores>>>();

	public static Map<String, List<Map<String, EntityScores>>> getTraining() {
		return training;
	}

	public static void setTraining(
			Map<String, List<Map<String, EntityScores>>> training) {
		ParameterOptimization.training = training;
	}

	public static Map<String, List<Map<String, EntityScores>>> getTesting() {
		return testing;
	}

	public static void setTesting(
			Map<String, List<Map<String, EntityScores>>> testing) {
		ParameterOptimization.testing = testing;
	}

	private static Map<String, List<Map<String, EntityScores>>> testing = new HashMap<String, List<Map<String, EntityScores>>>();

	// public static Map<String, List<Map<String, EntityScores>>> getAllScores()
	// {
	// return allScores;
	// }

	// public static void setAllScores(
	// Map<String, List<Map<String, EntityScores>>> allScores) {
	// ParameterOptimization.allScores = allScores;
	// }

	private static HashMap<String, Set<String>> annotationIndex = new HashMap<String, Set<String>>();

	public static HashMap<String, Set<String>> getAnnotationIndex() {
		return annotationIndex;
	}

	public static void setAnnotationIndex(
			HashMap<String, Set<String>> annotationIndex) {
		ParameterOptimization.annotationIndex = annotationIndex;

	}

	private static String dataset = Property.getInstance().get("dataset");
	public static double fmax = 0;
	public static String fmaxS;
	public static int iterationNum = 1;

	/**
	 * Constructs a new instance of the DTLZ2 function, defining it to include
	 * 11 decision variables and 2 objectives.
	 */
	public ParameterOptimization() {
		super(17, 1);
	}

	/**
	 * Constructs a new solution and defines the bounds of the decision
	 * variables.
	 */
	@Override
	public Solution newSolution() {
		Solution solution = new Solution(getNumberOfVariables(),
				getNumberOfObjectives());

		for (int i = 0; i < getNumberOfVariables(); i++) {
			solution.setVariable(i, new RealVariable(0.0, 1.0));
		}

		return solution;
	}

	/**
	 * Extracts the decision variables from the solution, evaluates the
	 * Rosenbrock function, and saves the resulting objective value back to the
	 * solution.
	 */
	@Override
	public void evaluate(Solution solution) {
		evaluate(solution, training, false);
		evaluate(solution, testing, true);
	}

	public void evaluate(Solution solution,
			Map<String, List<Map<String, EntityScores>>> dataset,
			boolean testing) {

		double[] x = EncodingUtils.getReal(solution);
		double[] f = new double[numberOfObjectives];

		double threshold = 0;
		double nameWeight = x[1];
		double suffixWeight = x[2];
		double letterCaseWeight = x[3];
		double wordvecDescriptionWeight = x[4];
		double typeContentWeight = x[5];
		double typeWeight = x[6];
		double domainWeight = x[7];
		double hashDescriptionWeight = x[8];
		double wordvecDescriptionLocalWeight = x[9];
		double hashInfoboxWeight = x[10];
		double linkWeight = x[11];
		double word2vecLinksWeight = x[12];
		double leskWeight = x[13];
		double simpleLeskWeight = x[14];
		double typeClassifierkWeight = x[15];
		double wikiWeight = x[16] * 5;

		int correct = 0;
		int incorrect = 0;
		int undetected = 0;

		for (String correctId : dataset.keySet()) {
			List<Map<String, EntityScores>> entityScoreMapL = dataset
					.get(correctId);
			for (Map<String, EntityScores> entityScoreMap : entityScoreMapL) {
				Set<String> set = annotationIndex.get(correctId);
				EntityScores entityScores = null;
				if (dataset.equals("target")) {
					if (set != null) {
						for (String s : set) {
							if (entityScoreMap.containsKey(s)) {
								entityScores = entityScoreMap.get(s);
								// correctId = s;
								break;
							}

						}
					}
				} else {
					entityScores = entityScoreMap.get(correctId);
				}
				if (entityScores != null) {
					try {

						double maxScore = -1;
						double maxSecond = -2;
						String maxId = "";
						EntityMatchList entities = entityScores
								.getEntityMatch().getSpot().getEntities();
						for (EntityMatch em : entities) {
							String id = em.getId();
							EntityScores e = entityScoreMap.get(id);

							double totalScore = /*
												 * e.getPopularityScore() *
												 * popularityWeight +
												 */e.getNameScore()
									* nameWeight + e.getLeskScore()
									* leskWeight + e.getSimpleLeskScore()
									* simpleLeskWeight + e.getLetterCaseScore()
									* letterCaseWeight

									+ e.getSuffixScore() * suffixWeight
									+ e.getTypeContentScore()
									* typeContentWeight + e.getTypeScore()
									* typeWeight + e.getDomainScore()
									* domainWeight
									+ e.getWordvecDescriptionScore()
									* wordvecDescriptionWeight
									+ e.getWordvecDescriptionLocalScore()
									* wordvecDescriptionLocalWeight
									+ e.getHashDescriptionScore()
									* hashDescriptionWeight
									+ e.getHashInfoboxScore()
									* hashInfoboxWeight
									+ e.getWordvecLinksScore()
									* word2vecLinksWeight + e.getLinkScore()
									* linkWeight + e.getTypeClassifierkScore()
									* typeClassifierkWeight + wikiWeight;
							if (TurkishEntityDisambiguator.ranklib) {
								totalScore = RankLib.getInstance().score(e);
							}

							if (totalScore >= maxScore) {

								maxSecond = maxScore;
								maxScore = totalScore;
								maxId = id;
							} else if (totalScore > maxSecond) {
								maxSecond = totalScore;
							}
						}

						if (maxId.equals("") || maxScore == maxSecond
								|| maxScore <= threshold) {
							// nomatch.add("no match: "
							// +
							// correctId);
							undetected++;
						} else if (maxId.equals(correctId)) {
							correct++;
							// System.out.println("Correct annotation: "
							// +
							// correctId);
							// correctList
							// .add("Correct annotation: "
							// +
							// correctId);
						} else if (dataset.equals("target")
								&& annotationIndex.get(correctId).contains(
										maxId)) {

							correct++;

						} else {
							incorrect++;
							// System.out.println("Incorrect annotation: c="
							// +
							// correctId);
							// incorrectList
							// .add("Incorrect annotation: "
							// +
							// correctId);
						}
					} catch (Exception e) {
						undetected++;
					}
				}

				else {
					// nomatch.add("no match: "
					// +
					// correctId);
					undetected++;
				}
			}
		}
		float precision = 0;
		if (correct + incorrect > 0) {
			precision = (float) correct / (correct + incorrect);
		}
		float recall = 0;
		if (correct + incorrect + undetected > 0) {
			recall = (float) correct / (correct + incorrect + undetected);
		}
		float fmeasure = 0;
		if (precision + recall > 0) {
			fmeasure = 2 * precision * recall / (precision + recall);
		}

		if (testing) {
			if (fmeasure > fmax) {
				fmax = fmeasure;
				fmaxS = ("\t" + 0 + "\t" + nameWeight + "\t" + letterCaseWeight
						+ "\t" + suffixWeight + "\t" + wordvecDescriptionWeight
						+ "\t" + typeContentWeight + "\t" + typeWeight + "\t"
						+ domainWeight + "\t" + hashDescriptionWeight + "\t"
						+ wordvecDescriptionLocalWeight + "\t"
						+ hashInfoboxWeight + "\t" + linkWeight + "\t"
						+ word2vecLinksWeight + "\t" + leskWeight + "\t"
						+ simpleLeskWeight + "\t" + typeClassifierkWeight
						+ "\t" + threshold + "\t" + wikiWeight + "\t" + correct
						+ "\t" + incorrect + "\t" + undetected + "\t"
						+ precision + "\t" + recall + "\t" + fmeasure);
				// LOGGER.info("\t" + iterationNum + "\t" + fmax);
			}
			if (iterationNum % 200 == 1) {
				// LOGGER.info("\t" + iterationNum + "\t" + fmax);
			}
			iterationNum++;
		} else {
			// System.out.println(fmeasure);
			f[0] = 1 - fmeasure;
			solution.setObjectives(f);
		}
	}

	public static void main(String[] args) {
		// configure and run the DTLZ2 function
		NondominatedPopulation result = new Executor()
				.withProblemClass(ParameterOptimization.class)
				.withAlgorithm("NSGAII").withMaxEvaluations(100000).run();

		// display the results
		System.out.format("Objective1  Objective2%n");

		for (Solution solution : result) {
			double[] x = EncodingUtils.getReal(solution);
			for (int i = 0; i < x.length; i++) {
				System.out.format("%d\t%.4f%n", i, x[i]);

			}

			System.out.println();
			System.out.format("%.4f%n", solution.getObjective(0));
			System.out.println(fmaxS);
			System.out.println("FMAX = " + fmax);
		}
	}

}
