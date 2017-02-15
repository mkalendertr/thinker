package it.cnr.isti.hpc.dexter.spot;

import it.cnr.isti.hpc.dexter.entity.Entity;
import it.cnr.isti.hpc.dexter.shingle.Shingle;
import it.cnr.isti.hpc.dexter.spot.repo.SpotRepository;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import edu.yeditepe.lucene.EntitySearchEngine;
import edu.yeditepe.model.EntityPage;
import edu.yeditepe.nlp.TurkishNLP;
import edu.yeditepe.nlp.Zemberek;
import edu.yeditepe.utils.Property;

public class TurkishSpotRepository implements SpotRepository {
	private static final Logger LOGGER = Logger
			.getLogger(TurkishSpotRepository.class);

	private static Set<String> typeBlackList;

	public static String knowledgebase = Property.getInstance().get(
			"knowledgebase");

	public static boolean production = Boolean.parseBoolean(Property
			.getInstance().get("production"));

	public static int candidateNum = 0;

	public TurkishSpotRepository() {
		typeBlackList = new HashSet<String>();
		// typeBlackList.add("film");
		// typeBlackList.add("albüm");
		// typeBlackList.add("kitap");
		// typeBlackList.add("şarkı");
		// typeBlackList.add("dergi");
		// typeBlackList.add("dizi");
		// typeBlackList.add("gazete");
		// typeBlackList.add("karakter");

	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

	@Override
	public Spot getSpot(String spot, String text, String inputText,
			Shingle shingle) {

		// if (text.equals("Eşkıya")) {
		// LOGGER.debug(text);
		// }
		// if (spot.equals("Eşkıya")) {
		// LOGGER.debug(spot);
		// }
		// text = TurkishNLP.toLowerCase(text);
		if (StringUtils.countMatches(text, " ") == 0
				&& (shingle.getPos().equals("Num")
						|| TurkishNLP.isStopWord(spot)
						|| TurkishNLP.isStopWord(text)
						|| (spot.length() <= 1 || (spot.length() == 2 && !StringUtils
								.isAllUpperCase(text))) || (!shingle.getPos()
						.equals("Noun") && StringUtils.isAllLowerCase(text)))

		) {
			// System.out.println(shingle.getPos());
			// LOGGER.info("Eleminate spot: " + spot + " " + shingle.getPos());
			return null;
		}
		try {
			candidateNum++;
			// LOGGER.info("Searching spot: " + spot + " " + shingle.getPos());
			EntitySearchEngine instance = EntitySearchEngine.getInstance();
			List<EntityPage> pages = null;
			pages = instance.performExactEntitySearch(
					spotToLuceneString(Zemberek.normalize(text)),
					spotToLuceneString(spot));

			// pages = instance.performExactEntitySearch(
			// spotToLuceneString(Zemberek.normalize(text)), null);
			// if (pages == null || pages.isEmpty()) {
			// pages = instance.performExactEntitySearch(
			//
			// spotToLuceneString(spot), null);
			// }

			if (!pages.isEmpty()) {

				List<Entity> entities = new ArrayList<Entity>();
				for (EntityPage wikiPage : pages) {
					// double stringDistamce = 1 - LevenshteinDistanceCalculator
					// .calculateDistance(wikiPage.getTitle(), text);
					if (!StringUtils.isNumeric(spot)
							&&

							!(StringUtils.countMatches(text, " ") == 0
									&& StringUtils.isAllLowerCase(text) && wikiPage
									.getLetterCase() != 0)
							&& (StringUtils.countMatches(text, " ") > 0
									|| StringUtils.containsIgnoreCase(spot,
											wikiPage.getTitle()) || StringUtils
										.containsIgnoreCase(
												wikiPage.getAlias(), spot)) &&
							// && (
							// // text.charAt(0) != wikiPage.getTitleMorph()
							// // .charAt(0)
							// // ||
							// StringUtils.containsIgnoreCase(wikiPage.getAlias(),
							// text) || (text.split(" ").length > 1))
							// && !(StringUtils.isAllLowerCase(spot.replace(" ",
							// "")) && (wikiPage.getLetterCase() != 0))
							!(typeBlackList.contains(wikiPage.getType()) && !StringUtils
									.containsIgnoreCase(inputText,
											wikiPage.getType())) &&
							// // !StringUtils.isNumeric(spot)
							// // &
							// !(StringUtils.isAllLowerCase(text) && wikiPag
							// .getLetterCase() != 0
							// &
							text.length() >= 2
					// && (wikiPage.getTitle().contains(" ") || wikiPage
					// .getTitle().length() <= text.length())
					// && stringDistamce >= 0.0
					//
					//

					) {
						if ((knowledgebase.equals("tdk") && wikiPage.getId()
								.startsWith("t"))
								|| knowledgebase.equals("all")
								|| (knowledgebase.equals("wiki") && wikiPage
										.getId().startsWith("w"))) {

							Entity e = new Entity(wikiPage);
							e.setShingle(shingle);
							entities.add(e);
						}
					}
				}
				return new Spot(text, entities, 1, 1);
			}
		} catch (Exception e) {
			LOGGER.error(e);
		}
		return null;
	}

	public static String spotToLuceneString(String url) {
		url = url.toLowerCase(new Locale("tr", "TR"));
		url = url.replace(" ", "_");

		return url;
	}
}
