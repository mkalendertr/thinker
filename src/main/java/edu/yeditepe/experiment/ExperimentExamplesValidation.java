package edu.yeditepe.experiment;

import it.cnr.isti.hpc.dexter.disambiguation.TurkishEntityDisambiguator;
import it.cnr.isti.hpc.dexter.entity.EntityMatch;
import it.cnr.isti.hpc.dexter.entity.EntityMatchList;
import it.cnr.isti.hpc.dexter.rest.domain.AnnotatedDocument;
import it.cnr.isti.hpc.dexter.util.DexterLocalParams;
import it.cnr.isti.hpc.text.Token;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.moeaframework.Executor;
import org.moeaframework.core.NondominatedPopulation;
import org.moeaframework.core.Solution;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.internal.LinkedTreeMap;
import com.google.gson.reflect.TypeToken;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;

import edu.yeditepe.controller.AnnotationController;
import edu.yeditepe.lucene.EntitySearchEngine;
import edu.yeditepe.model.Entity;
import edu.yeditepe.model.EntityPage;
import edu.yeditepe.model.EntityScores;
import edu.yeditepe.nlp.TurkishNLP;
import edu.yeditepe.nlp.Zemberek;
import edu.yeditepe.repository.MONGODB;
import edu.yeditepe.utils.FileUtils;
import edu.yeditepe.utils.Property;

public class ExperimentExamplesValidation {
	private static final Logger LOGGER = Logger
			.getLogger(ExperimentExamplesValidation.class);

	private static TreeMap<Integer, String> fmeasuremax = new TreeMap<Integer, String>();

	private static float popularityWeight = Float.parseFloat(Property
			.getInstance().get("disambiguation.popularityWeight"));

	private static float nameWeight = Float.parseFloat(Property.getInstance()
			.get("disambiguation.nameWeight"));

	private static float letterCaseWeight = Float.parseFloat(Property
			.getInstance().get("disambiguation.letterCaseWeight"));

	private static float suffixWeight = Float.parseFloat(Property.getInstance()
			.get("disambiguation.suffixWeight"));

	private static float typeContentWeight = Float.parseFloat(Property
			.getInstance().get("disambiguation.typeContentWeight"));

	private static float typeWeight = Float.parseFloat(Property.getInstance()
			.get("disambiguation.typeWeight"));

	private static float domainWeight = Float.parseFloat(Property.getInstance()
			.get("disambiguation.domainWeight"));

	private static float wordvecDescriptionWeight = Float.parseFloat(Property
			.getInstance().get("disambiguation.wordvecDescriptionWeight"));

	private static float wordvecDescriptionLocalWeight = Float
			.parseFloat(Property.getInstance().get(
					"disambiguation.wordvecDescriptionLocalWeight"));

	private static float word2vecLinksWeight = Float.parseFloat(Property
			.getInstance().get("disambiguation.word2vecLinksWeight"));

	private static float hashDescriptionWeight = Float.parseFloat(Property
			.getInstance().get("disambiguation.hashDescription"));

	private static float hashInfoboxWeight = Float.parseFloat(Property
			.getInstance().get("disambiguation.hashInfoboxWeight"));

	private static float linkWeight = Float.parseFloat(Property.getInstance()
			.get("disambiguation.linkWeight"));

	private static float wikiWeight = Float.parseFloat(Property.getInstance()
			.get("disambiguation.wikiWeight"));

	private static float leskWeight = Float.parseFloat(Property.getInstance()
			.get("disambiguation.leskWeight"));

	private static float simpleLeskWeight = Float.parseFloat(Property
			.getInstance().get("disambiguation.simpleLeskWeight"));

	private static float typeClassifierkWeight = Float.parseFloat(Property
			.getInstance().get("disambiguation.typeClassifierkWeight"));

	private static float minConfidence = Float.parseFloat(Property
			.getInstance().get("disambiguation.minConfidence"));

	private static int maxSense = Integer.parseInt(Property.getInstance().get(
			"tdk.maxsense"));

	private static float fmaxNum = 20;

	private static int sampleSize = 2000;

	private static float wordnetrate = 0.25f;

	private static String fmaxString;

	private static float fmax = 0f;

	private static String dataset = Property.getInstance().get("dataset");

	private static HashMap<String, Set<String>> annotationIndex = new HashMap<String, Set<String>>();

	public static void main(String[] args) {

		TreeMap<String, List<String>> examples = new TreeMap<String, List<String>>();
		// examples.put("w13855", "İstanbul'da hava çok güzel");
		// examples.put("w1702250", "İstanbul dizisi yayından kaldırıldı");
		if (dataset.equals("target")) {
			HashMap<String, HashSet<String>> annotations = new HashMap<String, HashSet<String>>();
			BufferedReader in;
			try {
				File file = new File(Property.getInstance().get(
						"experiment.target"));
				List<String> readFile = FileUtils.readFile(Property
						.getInstance().get("experiment.targetAnnotation"));
				for (String line : readFile) {
					String[] split = line.split("=");
					String folder = split[0];
					String[] ids = split[1].split(",");
					LinkedHashSet<String> set = new LinkedHashSet<String>();
					for (String id : ids) {
						set.add(id);
					}
					annotations.put(folder, set);
				}
				// Reading directory contents
				File[] files = file.listFiles();

				for (int i = 0; i < files.length; i++) {
					String folder = files[i].getName();
					HashSet<String> correctIds = annotations.get(folder);
					// if (correctIds == null) {
					// correctIds = new HashSet<String>();
					// System.out.println("no annotation");
					// } else {
					// System.out.println("");
					// }
					File[] files2 = files[i].listFiles();
					for (File file2 : files2) {
						in = new BufferedReader(new InputStreamReader(
								(new FileInputStream(file2))));

						String text = "";
						String line;
						String prevline = "";
						while ((line = in.readLine()) != null) {
							try {
								text += line + " ";
								prevline = line;
							} catch (Exception e) {
								// TODO: handle exception
							}
						}
						text = text.replace(prevline, "");
						String id = (String) correctIds.toArray()[0];
						annotationIndex.put(id, correctIds);
						if (examples.containsKey(correctIds.toArray()[0])) {

							examples.get(id).add(text);
						} else {
							List<String> l = new ArrayList<String>();
							l.add(text);
							examples.put(id, l);
						}
					}
				}
			} catch (Exception e) {
				// TODO: handle exception
			}
		} else if (dataset.equals("itu")) {
			try {
				Gson gson = new GsonBuilder().create();
				Reader reader = new FileReader("itu_dataset.json");
				examples = new Gson().fromJson(reader,
						new TypeToken<LinkedTreeMap<String, List<String>>>() {
						}.getType());
			} catch (Exception e) {
				examples = ExperimentITU.readDataset();

			}

		}

		else {

			try {
				Gson gson = new GsonBuilder().create();
				Reader reader = new FileReader(Property.getInstance().get(
						"dataset.file"));
				examples = new Gson().fromJson(reader,
						new TypeToken<TreeMap<String, List<String>>>() {
						}.getType());
			} catch (Exception e) {
				examples = createTraining();
				TreeMap<String, List<String>> val = new TreeMap<String, List<String>>();
				TreeMap<String, List<String>> test = new TreeMap<String, List<String>>();
				for (String string : examples.keySet()) {
					if (val.size() < sampleSize / 2) {
						val.put(string, examples.get(string));
					} else {
						test.put(string, examples.get(string));

					}

				}
				Writer writer;
				try {
					writer = new FileWriter("validation_"
							+ Property.getInstance().get("dataset.file"));
					Gson gson = new GsonBuilder().create();
					gson.toJson(val, writer);
					writer.close();
				} catch (IOException e2) {
					// TODO Auto-generated catch block
					e2.printStackTrace();
				}
				try {
					writer = new FileWriter("test_"
							+ Property.getInstance().get("dataset.file"));
					Gson gson = new GsonBuilder().create();
					gson.toJson(test, writer);
					writer.close();
				} catch (IOException e2) {
					// TODO Auto-generated catch block
					e2.printStackTrace();
				}
			}
		}
		performExperiment(examples);
	}

	public static void performExperiment(Map<String, List<String>> examples) {
		String loop = Property.getInstance().get("experiment.loop");
		int correct = 0;
		int incorrect = 0;
		int undetected = 0;
		int counter = 0;
		List<String> incorrectList = new ArrayList<String>();
		List<String> correctList = new ArrayList<String>();
		List<String> nomatch = new ArrayList<String>();

		HashMap<String, List<Map<String, EntityScores>>> allScores = new HashMap<String, List<Map<String, EntityScores>>>();
		for (String correctId : examples.keySet()) {
			List<String> l = examples.get(correctId);
			for (String text : l) {
				System.out.println("Experiment num " + counter++);
				LOGGER.info("Experiment num " + counter);
				try {
					DexterLocalParams params = new DexterLocalParams();
					params.addParam("text", text);
					AnnotatedDocument ad = AnnotationController.annotate(
							params, text, "1000000", null, null, null, null,
							"text", "0", "tr");
					if (ad != null) {
						Map<String, EntityScores> entityScoreMap = TurkishEntityDisambiguator.entityScoreMap;
						if (allScores.containsKey(correctId)) {

							allScores.get(correctId).add(entityScoreMap);
						} else {
							List<Map<String, EntityScores>> l2 = new ArrayList<Map<String, EntityScores>>();
							l2.add(entityScoreMap);
							allScores.put(correctId, l2);
						}
						EntityScores entityScores = null;
						if (dataset.equals("target")) {
							Set<String> set = annotationIndex.get(correctId);
							for (String s : set) {
								if (entityScoreMap.containsKey(s)) {
									entityScores = entityScoreMap.get(s);
									break;
								}

							}
						} else {
							entityScores = entityScoreMap.get(correctId);
						}
						if (entityScores == null) {
							nomatch.add("no match: " + correctId);
							undetected++;
							System.out.println(correctId + "\t" + text);
						} else {
							String selected = entityScores.getEntityMatch()
									.getSpot().getEntities().get(0).getId();
							if (entityScores.getEntityMatch().getSpot()
									.getEntities().get(0).getScore() <= minConfidence) {
								nomatch.add("threshold nomatch: " + correctId);
								undetected++;
							} else if (selected.equals(correctId)) {
								correct++;
								// System.out.println("Correct annotation: "
								// +
								// folder);
								correctList.add("Correct annotation: "
										+ correctId);
							} else if ((dataset.equals("target") && annotationIndex
									.get(correctId).contains(selected))) {
								correct++;
								// System.out.println("Correct annotation: "
								// +
								// folder);
								correctList.add("Correct annotation: "
										+ correctId);
							} else {
								incorrect++;
								// System.out.println("Incorrect annotation: c="
								// +
								// folder);
								incorrectList.add("Incorrect annotation: "
										+ correctId);
							}

						}

					}
				} catch (Exception e) {
					nomatch.add("no match: " + correctId);
					undetected++;
					e.printStackTrace();
				}
			}

		}
		for (String string : correctList) {
			System.out.println(string);
			LOGGER.info(string);
		}
		for (String string : incorrectList) {
			System.out.println(string);
			LOGGER.info(string);
		}
		for (String string : nomatch) {
			System.out.println(string);
			LOGGER.info(string);
		}
		String genetic = Property.getInstance().get("experiment.genetic");
		// if (loop.equals("false") && genetic.equals("false")) {

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
		// System.out.println("\tCorrect\tİncorrect\tUndetected\tPrecison\tRecall\tF-measure");
		// System.out.println("\t" + correct + "\t" + incorrect + "\t" +
		// undetected
		// + "\t" + precision + "\t" + recall + "\t" + fmeasure);

		System.out
				.println("\tpopularity\tname\tletterCase\tsuffix\twordvecDescription\t"
						+ "typeContent\ttype\tdomain\thashDescription\twordvecDescriptionLocal\thashInfoboxt\t"
						+ "link\tword2vecLinks\tlesk\tsimpleLesk\ttypeClassifier\tthreshold\twikiweight\tCorrect\tIncorrect\tUndetected\tPrecison\tRecall\tF-measure");
		String r = ("\t" + popularityWeight + "\t" + nameWeight + "\t"
				+ letterCaseWeight + "\t" + suffixWeight + "\t"
				+ wordvecDescriptionWeight + "\t" + typeContentWeight + "\t"
				+ typeWeight + "\t" + domainWeight + "\t"
				+ hashDescriptionWeight + "\t" + wordvecDescriptionLocalWeight
				+ "\t" + hashInfoboxWeight + "\t" + linkWeight + "\t"
				+ word2vecLinksWeight + "\t" + leskWeight + "\t"
				+ simpleLeskWeight + "\t" + typeClassifierkWeight + "\t"
				+ minConfidence + "\t" + wikiWeight + "\t" + correct + "\t"
				+ incorrect + "\t" + undetected + "\t" + precision + "\t"
				+ recall + "\t" + fmeasure);
		// System.out.println(r);
		// LOGGER.info(r);
		// return;
		// }

		float maxWeight = Float.parseFloat(Property.getInstance().get(
				"experiment.maxWeight"));
		float wikiWeight = Float.parseFloat(Property.getInstance().get(
				"disambiguation.wikiWeight"));

		if (loop.equals("true")) {

			for (float threshold = 0; threshold < 1; threshold++) {
				for (float domainWeight = 0; domainWeight < 1; domainWeight++) {
					for (float popularityWeight = 0; popularityWeight <= 1; popularityWeight++) {
						for (float leskWeight = 0; leskWeight <= 1; leskWeight++) {
							for (float wordvecDescriptionLocalWeight = 0; wordvecDescriptionLocalWeight <= 1; wordvecDescriptionLocalWeight++) {
								for (float wordvecDescriptionWeight = 0; wordvecDescriptionWeight <= 1; wordvecDescriptionWeight++) {
									for (float simpleLeskWeight = 0; simpleLeskWeight <= 1; simpleLeskWeight++) {
										for (float hashInfoboxWeight = 0; hashInfoboxWeight <= 1; hashInfoboxWeight++) {
											for (float hashDescriptionWeight = 0; hashDescriptionWeight <= 1; hashDescriptionWeight++) {
												for (float typeContentWeight = 0; typeContentWeight <= maxWeight; typeContentWeight++) {
													for (float typeWeight = 0; typeWeight <= maxWeight; typeWeight++) {
														for (float nameWeight = 0; nameWeight <= maxWeight; nameWeight++) {
															for (float word2vecLinksWeight = 0; word2vecLinksWeight <= maxWeight; word2vecLinksWeight++) {
																for (float linkWeight = 0; linkWeight <= maxWeight; linkWeight++) {
																	for (float suffixWeight = 0; suffixWeight <= maxWeight; suffixWeight++) {
																		for (float letterCaseWeight = 0; letterCaseWeight <= maxWeight; letterCaseWeight++) {
																			for (float typeClassifierkWeight = 0; typeClassifierkWeight <= maxWeight; typeClassifierkWeight++) {

																				float sum = popularityWeight
																						+ nameWeight
																						+ letterCaseWeight
																						+ suffixWeight
																						+ wordvecDescriptionWeight
																						+ typeContentWeight
																						+ typeWeight
																						+ domainWeight
																						+ hashDescriptionWeight
																						+ wordvecDescriptionLocalWeight
																						+ hashInfoboxWeight
																						+ linkWeight
																						+ word2vecLinksWeight
																						+ leskWeight
																						+ simpleLeskWeight
																						+ typeClassifierkWeight;
																				if (sum > 1
																						&& maxWeight == 1
																						&& sum != 15) {
																					continue;
																				}

																				correct = 0;
																				incorrect = 0;
																				undetected = 0;
																				incorrectList = new ArrayList<String>();
																				correctList = new ArrayList<String>();
																				nomatch = new ArrayList<String>();

																				for (String correctId : allScores
																						.keySet()) {
																					List<Map<String, EntityScores>> entityScoreMapL = allScores
																							.get(correctId);
																					for (Map<String, EntityScores> entityScoreMap : entityScoreMapL) {

																						if (entityScoreMap
																								.containsKey(correctId)) {
																							double maxScore = -1;
																							double maxSecond = -2;
																							String maxId = "";
																							EntityScores entityScores = entityScoreMap
																									.get(correctId);
																							EntityMatchList entities = entityScores
																									.getEntityMatch()
																									.getSpot()
																									.getEntities();
																							for (EntityMatch em : entities) {
																								String id = em
																										.getId();
																								EntityScores e = entityScoreMap
																										.get(id);

																								double totalScore = wikiWeight
																										+ e.getPopularityScore()
																										* popularityWeight
																										+ e.getNameScore()
																										* nameWeight
																										+ e.getLeskScore()
																										* leskWeight
																										+ e.getSimpleLeskScore()
																										* simpleLeskWeight
																										+ e.getLetterCaseScore()
																										* letterCaseWeight

																										+ e.getSuffixScore()
																										* suffixWeight
																										+ e.getTypeContentScore()
																										* typeContentWeight
																										+ e.getTypeScore()
																										* typeWeight
																										+ e.getDomainScore()
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
																										* word2vecLinksWeight
																										+ e.getLinkScore()
																										* linkWeight
																										+ e.getTypeClassifierkScore()
																										* typeClassifierkWeight;
																								if (totalScore >= maxScore
																										&& totalScore > threshold) {

																									maxSecond = maxScore;
																									maxScore = totalScore;
																									maxId = id;
																								} else if (totalScore > maxSecond) {
																									maxSecond = totalScore;
																								}
																							}
																							if (maxId
																									.equals("")
																									|| maxScore == maxSecond) {
																								// nomatch.add("no
																								// match:
																								// "
																								// +
																								// correctId);
																								undetected++;
																							} else if (maxId
																									.equals(correctId)) {
																								correct++;
																								// System.out.println("Correct
																								// annotation:
																								// "
																								// +
																								// correctId);
																								// correctList
																								// .add("Correct
																								// annotation:
																								// "
																								// +
																								// correctId);
																							} else if (dataset
																									.equals("target")
																									&& annotationIndex
																											.get(correctId)
																											.contains(
																													maxId)) {
																								correct++;
																							} else {
																								incorrect++;
																								// System.out.println("Incorrect
																								// annotation:
																								// c="
																								// +
																								// correctId);
																								// incorrectList
																								// .add("Incorrect
																								// annotation:
																								// "
																								// +
																								// correctId);
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
																				precision = 0;
																				if (correct
																						+ incorrect > 0) {
																					precision = (float) correct
																							/ (correct + incorrect);
																				}
																				recall = 0;
																				if (correct
																						+ incorrect
																						+ undetected > 0) {
																					recall = (float) correct
																							/ (correct
																									+ incorrect + undetected);
																				}
																				fmeasure = 0;
																				if (precision
																						+ recall > 0) {
																					fmeasure = 2
																							* precision
																							* recall
																							/ (precision + recall);
																				}

																				r = ("\t"
																						+ popularityWeight
																						+ "\t"
																						+ nameWeight
																						+ "\t"
																						+ letterCaseWeight
																						+ "\t"
																						+ suffixWeight
																						+ "\t"
																						+ wordvecDescriptionWeight
																						+ "\t"
																						+ typeContentWeight
																						+ "\t"
																						+ typeWeight
																						+ "\t"
																						+ domainWeight
																						+ "\t"
																						+ hashDescriptionWeight
																						+ "\t"
																						+ wordvecDescriptionLocalWeight
																						+ "\t"
																						+ hashInfoboxWeight
																						+ "\t"
																						+ linkWeight
																						+ "\t"
																						+ word2vecLinksWeight
																						+ "\t"
																						+ leskWeight
																						+ "\t"
																						+ simpleLeskWeight
																						+ "\t"
																						+ typeClassifierkWeight
																						+ "\t"
																						+ threshold
																						+ "\t"
																						+ wikiWeight
																						+ "\t"
																						+ correct
																						+ "\t"
																						+ incorrect
																						+ "\t"
																						+ undetected
																						+ "\t"
																						+ precision
																						+ "\t"
																						+ recall
																						+ "\t" + fmeasure);
																				if (fmeasure > fmax) {
																					fmax = fmeasure;
																					fmaxString = r;
																				}

																				if ((fmeasuremax
																						.isEmpty() || fmeasuremax
																						.size() < fmaxNum)
																						&& !fmeasuremax
																								.containsKey(correct)) {
																					fmeasuremax
																							.put(correct,
																									r);
																					// System.out.println(r);

																				} else if (correct > (fmeasuremax
																						.descendingKeySet()
																						.last())
																						&& !fmeasuremax
																								.containsKey(correct)) {

																					fmeasuremax
																							.remove(fmeasuremax
																									.descendingKeySet()
																									.last());
																					fmeasuremax
																							.put(correct,
																									r);
																					// System.out.println(r);
																				}

																				// if
																				// (sum
																				// ==
																				// 1)
																				// {
																				//
																				// System.out
																				// .println(r);
																				LOGGER.info(r);
																				// }
																			}
																		}
																	}
																}
															}
														}
													}
												}
											}
										}
									}
								}
							}
						}
					}
				}

			}

			// System.out
			// .println("\threshold\tname\tletterCase\tsuffix\twordvecDescription\t"
			// +
			// "typeContent\ttype\tdomain\thashDescription\twordvecDescriptionLocal\thashInfoboxt\t"
			// +
			// "link\tword2vecLinks\tlesk\tsimpleLesk\ttypeClassifier\tCorrect\tIncorrect\tUndetected\tPrecison\tRecall\tF-measure");
			// System.out.println(fmaxString);
			// LOGGER.info(fmaxString);
			// for (int i : fmeasuremax.keySet()) {
			// System.out.println(fmeasuremax.get(i));
			// LOGGER.info(fmeasuremax.get(i));
			// }
		}

		if (genetic.equals("true")) {
			LOGGER.info("NSGAII");
			for (int it = 1; it < 21; it++) {
				// LOGGER.info("Iteration " + it);

				NondominatedPopulation result = null;
				int kfold = 10;
				int foldSize = allScores.size() / kfold;
				for (int i = 0; i < kfold; i++) {
					int y = 0;
					HashMap<String, List<Map<String, EntityScores>>> training = new HashMap<String, List<Map<String, EntityScores>>>();
					HashMap<String, List<Map<String, EntityScores>>> test = new HashMap<String, List<Map<String, EntityScores>>>();

					for (String correctId : allScores.keySet()) {
						if (y >= i * foldSize && test.size() < foldSize) {
							test.put(correctId, allScores.get(correctId));
						} else {
							training.put(correctId, allScores.get(correctId));
						}
						y++;
					}
					ParameterOptimization.setAnnotationIndex(annotationIndex);
					ParameterOptimization.setTraining(training);
					ParameterOptimization.setTesting(test);
					result = new Executor()
							.withProblemClass(ParameterOptimization.class)
							.withAlgorithm("NSGAII").withMaxEvaluations(2001)
							.run();

					for (Solution solution : result) {
						// double[] x = EncodingUtils.getReal(solution);
						// for (int i = 0; i < x.length; i++) {
						// System.out.format("%d\t%.4f%n", i, x[i]);
						// }
						// System.out.format("%.4f%n",
						// solution.getObjective(0));
						// System.out.println(ParameterOptimization.fmaxS);
						// System.out.println("NSGAII FMAX = "
						// + ParameterOptimization.fmax);
						LOGGER.info(ParameterOptimization.fmaxS);
						// LOGGER.info("NSGAII FMAX = " +
						// ParameterOptimization.fmax);
						ParameterOptimization.iterationNum = 0;
						ParameterOptimization.fmax = 0;
					}
				}
			}
			// result = new Executor()
			// .withProblemClass(ParameterOptimization.class)
			// .withAlgorithm("GDE3").withMaxEvaluations(10000).run();
			//
			// for (Solution solution : result) {
			// // double[] x = EncodingUtils.getReal(solution);
			// // for (int i = 0; i < x.length; i++) {
			// // System.out.format("%d\t%.4f%n", i, x[i]);
			// // }
			// System.out.format("%.4f%n", solution.getObjective(0));
			// System.out.println(ParameterOptimization.fmaxS);
			// System.out.println("GDE3 FMAX = " + ParameterOptimization.fmax);
			// LOGGER.info(ParameterOptimization.fmaxS);
			// LOGGER.info("GDE3 FMAX = " + ParameterOptimization.fmax);
			//
			// }
			//
			// Analyzer analyzer = new Analyzer()
			// .withProblemClass(ParameterOptimization.class)
			// .includeAllMetrics().showStatisticalSignificance();
			//
			// Executor executor = new Executor().withProblemClass(
			// ParameterOptimization.class).withMaxEvaluations(10000);
			// analyzer.addAll("NSGAII", executor.withAlgorithm("NSGAII")
			// .runSeeds(50));
			// LOGGER.info(ParameterOptimization.fmaxS);
			//
			// analyzer.addAll("GDE3",
			// executor.withAlgorithm("GDE3").runSeeds(50));
			// LOGGER.info(ParameterOptimization.fmaxS);
			//
			// analyzer.printAnalysis();

		}
	}

	public static TreeMap<String, List<String>> createTraining() {
		TreeMap<String, List<String>> examples = new TreeMap<String, List<String>>();
		Set<String> selectedIds = new HashSet<String>();
		int wordnetNum = 0, wikiNum = 0;
		int maxWordnet = (int) (sampleSize * wordnetrate);
		int maxWiki = (int) (sampleSize - maxWordnet) + 10;
		DBCollection entitiesDB = MONGODB.getCollection(Entity.COLLECTION_NAME);

		DBCursor cursor = entitiesDB.find();
		while (cursor.hasNext() && selectedIds.size() < sampleSize * 2) {
			try {
				DBObject object = cursor.next();
				String ido = (String) object.get("id");
				String title = (String) object.get("title");
				if (StringUtils.countMatches(title, " ") > 3) {
					continue;
				}
				List<Token> disambiguateFindTokens = Zemberek.getInstance()
						.disambiguateFindTokens(title, false, true);
				String lemma = "";
				for (Token token : disambiguateFindTokens) {
					lemma += token.getMorphText() + " ";
				}
				lemma = lemma.trim();
				if (!Zemberek.isOnePhrase(lemma)
						|| TurkishNLP.isStopWord(lemma)) {
					continue;
				}
				EntitySearchEngine instance = EntitySearchEngine.getInstance();
				List<EntityPage> pages = instance.performExactEntitySearch(
						spotToLuceneString((Zemberek.normalize(lemma))), null);

				if (pages.isEmpty() || pages.size() < 2 || pages.size() >= 10) {
					continue;
				} else {
					for (EntityPage entityPage : pages) {
						selectedIds.add(entityPage.getId());
					}
				}

			} catch (Exception e) {
				// TODO: handle exception
			}
		}
		cursor = entitiesDB.find();
		while (cursor.hasNext()) {
			try {
				DBObject object = cursor.next();
				String ido = (String) object.get("id");
				int sense = Character
						.getNumericValue(ido.charAt(ido.length() - 1));
				if (!selectedIds.contains(ido)) {
					continue;
				}
				String title = (String) object.get("title");

				double rank = (Double) object.get("rank");
				ArrayList<String> sentences = (ArrayList<String>) object
						.get("sentences");
				if (ido.startsWith("t") /* && wordnetNum < maxWordnet */
						&& sense <= maxSense) {
					if (sentences != null && sentences.size() > 0) {
						String text = sentences.get(0);
						if (StringUtils.countMatches(text, " ") > 5
								&& text.contains(".")) {
							if (examples.containsKey(ido)) {

								examples.get(ido).add(text);
							} else {
								List<String> l = new ArrayList<String>();
								l.add(text);
								examples.put(ido, l);
								LOGGER.info(title);
							}
							wordnetNum++;
						}
					}
				} // else if (maxWiki > wikiNum) {
				if (sentences != null && sentences.size() > 1) {
					StringBuffer sb = new StringBuffer("");
					for (int i = 0; i < sentences.size(); i++) {
						sb.append(sentences.get(i) + " \n ");
					}
					String text = sb.toString();
					rank = Math.log(1 + rank) / Math.log(58641);

					// if (rank >= Math.random() + 0.2) {
					if (!text.contains("{{")
							&& StringUtils.countMatches(text, " ") > 5
							&& text.contains(".")) {
						if (examples.containsKey(ido)) {

							examples.get(ido).add(text);
						} else {
							List<String> l = new ArrayList<String>();
							l.add(text);
							examples.put(ido, l);
							LOGGER.info(title);
						}
						wikiNum++;

					}
					// }
					// }

				}
			} catch (Exception e) {
				// TODO: handle exception
			}

			if (examples.size() >= sampleSize) {
				System.out.println(wordnetNum);
				return examples;
			}
		}
		return examples;

	}

	public static String spotToLuceneString(String url) {
		url = url.toLowerCase(new Locale("tr", "TR"));
		url = url.replace(" ", "_");

		return url;
	}
}
