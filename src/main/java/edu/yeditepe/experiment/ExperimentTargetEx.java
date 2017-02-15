package edu.yeditepe.experiment;

import it.cnr.isti.hpc.dexter.disambiguation.TurkishEntityDisambiguator;
import it.cnr.isti.hpc.dexter.rest.domain.AnnotatedDocument;
import it.cnr.isti.hpc.dexter.rest.domain.AnnotatedSpot;
import it.cnr.isti.hpc.dexter.util.DexterLocalParams;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import edu.yeditepe.controller.AnnotationController;
import edu.yeditepe.repository.MYSQL;
import edu.yeditepe.utils.Property;

public class ExperimentTargetEx {
	private static final Logger LOGGER = Logger
			.getLogger(ExperimentTarget.class);

	private static int popularityWeight = Integer.parseInt(Property
			.getInstance().get("disambiguation.popularityWeight"));

	private static int nameWeight = Integer.parseInt(Property.getInstance()
			.get("disambiguation.nameWeight"));

	private static int letterCaseWeight = Integer.parseInt(Property
			.getInstance().get("disambiguation.letterCaseWeight"));

	private static int suffixWeight = Integer.parseInt(Property.getInstance()
			.get("disambiguation.suffixWeight"));

	private static int typeContentWeight = Integer.parseInt(Property
			.getInstance().get("disambiguation.typeContentWeight"));

	private static int typeWeight = Integer.parseInt(Property.getInstance()
			.get("disambiguation.typeWeight"));

	private static int domainWeight = Integer.parseInt(Property.getInstance()
			.get("disambiguation.domainWeight"));

	private static int wordvecDescriptionWeight = Integer.parseInt(Property
			.getInstance().get("disambiguation.wordvecDescriptionWeight"));

	private static int wordvecDescriptionLocalWeight = Integer
			.parseInt(Property.getInstance().get(
					"disambiguation.wordvecDescriptionLocalWeight"));

	private static int word2vecLinksWeight = Integer.parseInt(Property
			.getInstance().get("disambiguation.word2vecLinksWeight"));

	private static int hashDescriptionWeight = Integer.parseInt(Property
			.getInstance().get("disambiguation.hashDescription"));

	private static int hashInfoboxWeight = Integer.parseInt(Property
			.getInstance().get("disambiguation.hashInfoboxWeight"));

	private static int linkWeight = Integer.parseInt(Property.getInstance()
			.get("disambiguation.linkWeight"));

	private static int wikiWeight = Integer.parseInt(Property.getInstance()
			.get("disambiguation.wikiWeight"));

	private static int leskWeight = Integer.parseInt(Property.getInstance()
			.get("disambiguation.leskWeight"));

	private int window = Integer.parseInt(Property.getInstance().get(
			"disambiguation.window"));

	public static void main(String[] args) {
		long correct = 0;
		long incorrect = 0;
		long totalLinks = 0;
		List<String> incorrectList = new ArrayList<String>();
		List<String> correctList = new ArrayList<String>();
		List<String> nomatch = new ArrayList<String>();

		BufferedReader in;
		try {
			File file = new File(Property.getInstance()
					.get("experiment.target"));

			// Reading directory contents
			File[] files = file.listFiles();

			for (int i = 0; i < files.length; i++) {
				String urltitle = files[i].getName();

				System.out.println(urltitle);
				int correctId = MYSQL.getInstance().getIdFromRedirect(urltitle);
				String title = "";
				if (urltitle.contains("_(")) {
					title = urltitle.substring(0, urltitle.indexOf("_("))
							.replace("_", " ").trim()
							.toLowerCase(new Locale("tr", "TR"));
				} else {
					title = urltitle;
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
					totalLinks++;
					DexterLocalParams params = new DexterLocalParams();

					params.addParam("text", text);
					TurkishEntityDisambiguator.printCandidateEntities = true;
					TurkishEntityDisambiguator.candidateEntitiesFileName = "tsne"
							+ File.separator
							+ "input"
							+ File.separator
							+ totalLinks + "-" + urltitle + ".entities";
					TurkishEntityDisambiguator.candidateEntitiyId = correctId;
					AnnotatedDocument ad = AnnotationController.annotate(
							params, text, "1000000", null, null, null, null,
							"text", "0", "tr");
					if (ad != null) {
						List<AnnotatedSpot> annotatedSpots = ad.getSpots();
						HashSet<Integer> entities = new HashSet<Integer>();
						HashSet<String> spots = new HashSet<String>();

						for (AnnotatedSpot annotatedSpot : annotatedSpots) {
							int id = annotatedSpot.getEntity();
							entities.add(id);
							spots.add(annotatedSpot.getMention().toLowerCase(
									new Locale("tr", "TR")));
						}
						if (entities.contains(correctId)) {
							correct++;
							LOGGER.info("Correct annotation: " + urltitle);
							correctList.add("Correct annotation: " + urltitle);
						} else if (StringUtils.containsIgnoreCase(
								spots.toString(), title)) {
							incorrect++;
							LOGGER.info("Incorrect annotation: c=" + urltitle);
							incorrectList.add("Incorrect annotation: "
									+ urltitle);

						} else {
							nomatch.add("no match: " + file2.getAbsolutePath());
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

		LOGGER.info("\tpopularityWeight\tnameWeight\tletterCaseWeight\tsuffixWeight\twordvecDescriptionWeight\t"
				+ "typeContentWeight\ttypeWeight\tdomainWeight\thashDescriptionWeight\twordvecDescriptionLocalWeight\thashInfoboxWeightt\t"
				+ "linkWeight\tword2vecLinksWeight\tleskWeight\tthreshold\tCorrect\tIncorrect\tUndetected\tPrecison\tRecall\tF-measure");
		float precision = 0;
		if (correct + incorrect > 0) {
			precision = (float) correct / (correct + incorrect);
		}
		float recall = 0;
		if (totalLinks > 0) {
			recall = (float) correct / (totalLinks);
		}
		float fmeasure = 0;
		if (precision + recall > 0) {
			fmeasure = 2 * precision * recall / (precision + recall);
		}
		String r = "\t" + popularityWeight + "\t" + nameWeight + "\t"
				+ letterCaseWeight + "\t" + suffixWeight + "\t"
				+ wordvecDescriptionWeight + "\t" + typeContentWeight + "\t"
				+ typeWeight + "\t" + domainWeight + "\t"
				+ hashDescriptionWeight + "\t" + wordvecDescriptionLocalWeight
				+ "\t" + hashInfoboxWeight + "\t" + linkWeight + "\t"
				+ word2vecLinksWeight + "\t" + leskWeight + "\t" + 0 + "\t"
				+ correct + "\t" + incorrect + "\t"
				+ (totalLinks - correct - incorrect) + "\t" + precision + "\t"
				+ recall + "\t" + fmeasure;
		LOGGER.info(r);
		// LOGGER.info("Total correct annotations: " + correct);
		// LOGGER.info("Total incorrect annotations: " + incorrect);
		// LOGGER.info("Precison: " + (float) correct * 100
		// / (correct + incorrect));
		// LOGGER.info("Recall: " + (float) correct * 100 / (totalLinks));

	}
}
