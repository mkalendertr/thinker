package edu.yeditepe.experiment;

import it.cnr.isti.hpc.dexter.disambiguation.TurkishEntityDisambiguator;
import it.cnr.isti.hpc.dexter.rest.domain.AnnotatedDocument;
import it.cnr.isti.hpc.dexter.rest.domain.AnnotatedSpot;
import it.cnr.isti.hpc.dexter.util.DexterLocalParams;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.log4j.Logger;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import edu.yeditepe.controller.AnnotationController;
import edu.yeditepe.repository.MYSQL;
import edu.yeditepe.utils.Property;

public class ExperimentAll {
	private static final Logger LOGGER = Logger.getLogger(ExperimentAll.class);

	public static void main(String[] args) {
		long correct = 0;
		long incorrect = 0;
		long totalLinks = 0;
		long unknown = 0;
		List<String> incorrectList = new ArrayList<String>();
		List<String> correctList = new ArrayList<String>();
		List<String> nomatch = new ArrayList<String>();

		BufferedReader in;
		try {
			File file = new File(Property.getInstance().get("experiment.all")
					+ File.separator + "input");

			// Reading directory contents
			File[] files = file.listFiles();

			for (int i = 0; i < files.length; i++) {
				String urltitle = files[i].getName();

				System.out.println(urltitle);
				in = new BufferedReader(new InputStreamReader(
						(new FileInputStream(files[i]))));

				String text = "";
				String line;
				while ((line = in.readLine()) != null) {
					try {
						text += line + " ";
					} catch (Exception e) {
						// TODO: handle exception
					}
				}
				DexterLocalParams params = new DexterLocalParams();

				params.addParam("text", text);
				TurkishEntityDisambiguator.annotateEntities = Boolean
						.parseBoolean(Property.getInstance().get(
								"experiment.annotateEntities"));
				TurkishEntityDisambiguator.candidateEntitiesFileName = Property
						.getInstance().get("experiment.all")
						+ File.separator
						+ "annotation" + File.separator + urltitle + ".json";
				AnnotatedDocument ad = AnnotationController.annotate(params,
						text, "1000000", null, null, null, null, "text", "0",
						"tr");
				if (ad != null) {
					List<AnnotatedSpot> annotatedSpots = ad.getSpots();
					HashMap<String, String> entities = new HashMap<String, String>();
					Reader reader = new FileReader(
							TurkishEntityDisambiguator.candidateEntitiesFileName);
					Gson gson = new GsonBuilder().create();
					entities = gson.fromJson(reader, HashMap.class);

					for (AnnotatedSpot annotatedSpot : annotatedSpots) {
						int id = annotatedSpot.getEntity();
						String title = MYSQL.getInstance().getTRTitleById(
								String.valueOf(id));
						if (entities.get(annotatedSpot.getMention()) != null
								&& !entities.get(annotatedSpot.getMention())
										.equals("-1")) {

							if (entities.get(annotatedSpot.getMention())
									.equalsIgnoreCase(title)) {
								correct++;
								LOGGER.info("Correct annotation: "
										+ annotatedSpot.getMention());
								correctList.add("Correct annotation: "
										+ annotatedSpot.getMention());
							} else {
								incorrect++;
								LOGGER.info("Incorrect annotation: c="
										+ annotatedSpot.getMention());
								incorrectList.add("Incorrect annotation: "
										+ annotatedSpot.getMention());

							}
						} else if (entities.get(annotatedSpot.getMention()) != null
								&& entities.get(annotatedSpot.getMention())
										.equals("-1")) {
							unknown++;
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
		LOGGER.info("Total correct annotations: " + correct);
		LOGGER.info("Total incorrect annotations: " + incorrect);
		LOGGER.info("Total unknown annotations: " + unknown + " =%" + unknown
				* 100 / (correct + incorrect + unknown));
		LOGGER.info("Precison: " + (float) correct * 100
				/ (correct + incorrect));
		// LOGGER.info("Recall: " + (float) correct * 100 / (correct +
		// incorrect));

	}
}
