package edu.yeditepe.experiment;

import it.cnr.isti.hpc.dexter.disambiguation.TurkishEntityDisambiguator;
import it.cnr.isti.hpc.dexter.rest.domain.AnnotatedDocument;
import it.cnr.isti.hpc.dexter.util.DexterLocalParams;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;

import edu.yeditepe.controller.AnnotationController;
import edu.yeditepe.model.EntityScores;
import edu.yeditepe.utils.FileUtils;
import edu.yeditepe.utils.Property;

public class ExperimentTarget {
	private static final Logger LOGGER = Logger
			.getLogger(ExperimentTarget.class);

	public static void main(String[] args) {
		long correct = 0;
		long incorrect = 0;
		long undetected = 0;
		List<String> incorrectList = new ArrayList<String>();
		List<String> correctList = new ArrayList<String>();
		List<String> nomatch = new ArrayList<String>();
		HashMap<String, HashSet<String>> annotations = new HashMap<String, HashSet<String>>();
		BufferedReader in;
		try {
			File file = new File(Property.getInstance()
					.get("experiment.target"));
			List<String> readFile = FileUtils.readFile(Property.getInstance()
					.get("experiment.targetAnnotation"));
			for (String line : readFile) {
				String[] split = line.split("=");
				String folder = split[0];
				String[] ids = split[1].split(",");
				HashSet<String> set = new HashSet<String>();
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
				if (correctIds == null) {
					correctIds = new HashSet<String>();
					System.out.println("no annotation");
				} else {
					System.out.println("");
				}
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
					DexterLocalParams params = new DexterLocalParams();
					params.addParam("text", text);
					AnnotatedDocument ad = AnnotationController.annotate(
							params, text, "1000000", null, null, null, null,
							"text", "0", "tr");
					if (ad != null) {
						Set<String> selectedEntities = TurkishEntityDisambiguator.selectedEntities;
						Map<String, EntityScores> entityScoreMap = TurkishEntityDisambiguator.entityScoreMap;
						boolean correctMapping = false;
						boolean entityDetected = false;
						for (String id : correctIds) {
							if (!correctMapping
									&& selectedEntities.contains(id)) {
								correct++;
								// LOGGER.info("Correct annotation: " + folder);
								correctList
										.add("Correct annotation: " + folder);
								correctMapping = true;
								entityDetected = true;
								break;
							} else if (entityScoreMap.containsKey(id)) {
								entityDetected = true;
							}
						}
						if (!correctMapping && entityDetected) {
							incorrect++;
							// LOGGER.info("Incorrect annotation: c=" + folder);
							incorrectList
									.add("Incorrect annotation: " + folder);
						} else if (!entityDetected) {
							nomatch.add("no match: " + file2.getAbsolutePath());
							undetected++;
						}
					}
				}
			}
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		for (String string : correctList) {
			LOGGER.info(string);
		}
		for (String string : incorrectList) {
			LOGGER.info(string);
		}
		for (String string : nomatch) {
			LOGGER.info(string);
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
		LOGGER.info("\tCorrect\tİncorrect\tUndetected\tPrecison\tRecall\tF-measure");
		LOGGER.info("\t" + correct + "\t" + incorrect + "\t" + undetected
				+ "\t" + precision + "\t" + recall + "\t" + fmeasure);

	}
}
