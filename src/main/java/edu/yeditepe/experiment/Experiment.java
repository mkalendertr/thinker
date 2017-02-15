package edu.yeditepe.experiment;

import it.cnr.isti.hpc.dexter.label.IdHelper;
import it.cnr.isti.hpc.dexter.label.IdHelperFactory;
import it.cnr.isti.hpc.dexter.rest.domain.AnnotatedDocument;
import it.cnr.isti.hpc.dexter.rest.domain.AnnotatedSpot;
import it.cnr.isti.hpc.dexter.util.DexterLocalParams;
import it.cnr.isti.hpc.dexter.util.DexterParams;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import org.apache.log4j.Logger;

import scala.collection.mutable.HashSet;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import edu.yeditepe.controller.AnnotationController;
import edu.yeditepe.wiki.Wikipedia;

public class Experiment {
	private static final Logger LOGGER = Logger.getLogger(Experiment.class);

	public static final DexterParams params = DexterParams.getInstance();
	public static final IdHelper helper = IdHelperFactory.getStdIdHelper();

	private static Gson gson = new GsonBuilder()
			.serializeSpecialFloatingPointValues().create();

	public static void main(String[] args) throws IOException {
		Wikipedia wiki = new Wikipedia();
		wiki.experiment();
		HashMap<String, HashMap<String, Integer>> pageLinks = wiki
				.getPageLinks();
		HashMap<String, String> pagePlainText = wiki.getPagePlainText();
		List<String> incorrectList = new ArrayList<String>();
		long correct = 0;
		long incorrect = 0;
		long totalLinks = 0;
		for (String page : pagePlainText.keySet()) {
			LOGGER.info("Annotating page: " + page);
			HashMap<String, Integer> links = pageLinks.get(page);
			Collection<Integer> linkIds = links.values();
			totalLinks += links.size();

			String text = pagePlainText.get(page);
			LOGGER.info(text);

			DexterLocalParams params = new DexterLocalParams();

			params.addParam("text", text);
			AnnotatedDocument ad = AnnotationController.annotate(params, text,
					"1000000", null, null, null, null, "text", "0", "tr");
			if (ad != null) {
				List<AnnotatedSpot> annotatedSpots = ad.getSpots();
				HashSet<Integer> controlled = new HashSet<Integer>();
				for (AnnotatedSpot annotatedSpot : annotatedSpots) {
					String title = annotatedSpot.getMention();
					String url = annotatedSpot.getWikiname();
					int id = annotatedSpot.getEntity();

					if (links.containsKey(title) && !controlled.contains(id)) {
						int manuelId = links.get(title);
						controlled.add(id);
						if (manuelId != 0) {

							if (manuelId == id) {
								correct++;
								LOGGER.info("Correct annotation: " + title
										+ "," + url + ","
										+ annotatedSpot.getScore());

							} else {
								incorrect++;
								LOGGER.info("Incorrect annotation: c=" + title
										+ ",ours=" + url + ","
										+ annotatedSpot.getScore());
								incorrectList.add("Incorrect annotation: c="
										+ title + ",ours=" + url + ","
										+ annotatedSpot.getScore());
							}
						}
					} else if (linkIds.contains(id) && !controlled.contains(id)) {
						controlled.add(id);
						correct++;
						LOGGER.info("Correct annotation: " + title + "," + url
								+ "," + annotatedSpot.getScore());
					}
				}
			}
			for (String l : links.keySet()) {
				LOGGER.info("Key:" + l + " Value:" + links.get(l));
			}
			// }
		}

		for (String string : incorrectList) {
			LOGGER.info(string);
		}

		LOGGER.info("Total correct annotations: " + correct);
		LOGGER.info("Total incorrect annotations: " + incorrect);
		LOGGER.info("Precison: " + correct * 100 / (correct + incorrect));
		LOGGER.info("Recall: " + correct * 100 / (totalLinks));

	}
}
