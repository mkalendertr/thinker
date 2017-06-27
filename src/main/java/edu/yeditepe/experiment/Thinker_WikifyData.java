package edu.yeditepe.experiment;

import it.cnr.isti.hpc.dexter.disambiguation.TurkishEntityDisambiguator;
import it.cnr.isti.hpc.dexter.rest.domain.AnnotatedDocument;
import it.cnr.isti.hpc.dexter.rest.domain.AnnotatedSpot;
import it.cnr.isti.hpc.dexter.util.DexterLocalParams;

import java.io.BufferedReader;
import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.apache.lucene.document.Document;

import edu.yeditepe.controller.AnnotationController;
import edu.yeditepe.lucene.EntitySearchEngine;
import edu.yeditepe.model.EntityScores;
import edu.yeditepe.nlp.TurkishNLP;
import edu.yeditepe.utils.FileUtils;

public class Thinker_WikifyData {
	private static final Logger LOGGER = Logger
			.getLogger(Thinker_WikifyData.class);

	private static Thinker_WikifyData wikifier = new Thinker_WikifyData();

	// Invalid parameter: Usage:
	// GET or POST parameters: {tool}, {input} and {token}
	// token: you can find your token on your login space
	// tool: ner, morphanalyzer, isturkish, morphgenerator, tokenizer,
	// normalize, deasciifier, Vowelizer, depparser, spellcheck, disambiguator,
	// pipeline
	// input: utf-8 string
	// The response is a text/plain encoded in UTF-8

	private Thinker_WikifyData() {

	}

	public static Thinker_WikifyData getInstance() {
		return wikifier;
	}

	public static void main(String[] args) {
		// Babelfly.getInstance().makeRequest(
		// "Yeditepe Üniversitesi, İstanbul'da bulunuyor.");
		HashMap<String, HashSet<String>> annotations = new HashMap<String, HashSet<String>>();
		HashMap<String, String> data = new HashMap<String, String>();

		HashSet<String> hard = new HashSet<String>();
		HashSet<String> easy = new HashSet<String>();
		int ecorrect = 0;
		int eincorrect = 0;

		int hcorrect = 0;
		int hincorrect = 0;

		int undetected = 0;
		int counter = 0;
		BufferedReader in;
		try {
			File file = new File("wiikifier_test");
			// Reading directory contents
			File[] files = file.listFiles();

			for (int i = 0; i < files.length; i++) {
				String fileName = files[i].getPath();
				if (fileName.endsWith("mentions")) {
					HashSet<String> set = new HashSet<String>();
					annotations
							.put(files[i].getName().replaceAll(".mentions", ""),
									set);
					List<String> annoList = FileUtils.readFile(fileName);
					for (String anno : annoList) {
						String[] split = anno.split("\t");
						String entity = split[3];
						Document pagebyURLTitle = EntitySearchEngine
								.getInstance().getPagebyURLTitle(entity);
						if (pagebyURLTitle == null) {
							continue;
						}
						set.add(TurkishNLP.toLowerCase(entity));
						if (split[4].equals("1")) {
							hard.add(TurkishNLP.toLowerCase(entity));
						} else {
							easy.add(TurkishNLP.toLowerCase(entity));
						}
					}
				} else {
					String readFileString = FileUtils.readFileString(fileName);
					data.put(files[i].getName().replaceAll(".txt", ""),
							readFileString);
				}
			}
			int c = 0;
			Set<String> extracted = new HashSet<String>();
			for (String instance : data.keySet()) {
				DexterLocalParams params = new DexterLocalParams();
				params.addParam("text", data.get(instance));
				AnnotatedDocument ad = AnnotationController.annotate(params,
						data.get(instance), "1000000", null, null, null, null,
						"text", "0", "tr");
				if (ad != null) {
					Map<String, EntityScores> entityScoreMap = TurkishEntityDisambiguator.entityScoreMap;
					for (AnnotatedSpot spot : ad.getSpots()) {
						extracted
								.add(TurkishNLP.toLowerCase(spot.getWikiname()));
					}
				}

				HashSet<String> anno = annotations.get(instance);
				String text = TurkishNLP.toLowerCase(data.get(instance));
				for (String a : anno) {
					if (text.contains(a.replaceAll("_", " "))) {

						if (extracted != null && extracted.contains(a)) {
							if (hard.contains(a)) {
								hcorrect++;
							} else {
								ecorrect++;
							}

						} else {
							LOGGER.info(a);
							if (hard.contains(a)) {
								hincorrect++;
							} else {
								eincorrect++;
							}
						}
					}
				}
				c++;
				// if (c > 100) {
				// break;
				// }
			}
			float haccuracy = (float) hcorrect / (hcorrect + hincorrect);
			float eaccuracy = (float) ecorrect / (ecorrect + eincorrect);
			// if (correct + incorrect > 0) {
			// precision = (float) correct / (correct + incorrect);
			// }
			// float recall = 0;
			// if (correct + incorrect + undetected > 0) {
			// recall = (float) correct / (correct + incorrect + undetected);
			// }
			// float fmeasure = 0;
			// if (precision + recall > 0) {
			// fmeasure = 2 * precision * recall / (precision + recall);
			// }
			// System.out
			// .println("\tCorrect\tIncorrect\tUndetected\tPrecison\tRecall\tF-measure");
			// System.out.println("\t" + correct + "\t" + incorrect + "\t"
			// + undetected + "\t" + precision + "\t" + recall + "\t"
			// + fmeasure);
			LOGGER.info(hcorrect + "\t" + hincorrect + "\t" + haccuracy + "\t"
					+ ecorrect + "\t" + eincorrect + "\t" + eaccuracy);

		} catch (Exception e) {
			// TODO: handle exception
		}

	}
}
