package edu.yeditepe.wiki;

import info.bliki.wiki.dump.IArticleFilter;
import info.bliki.wiki.dump.Siteinfo;
import info.bliki.wiki.dump.WikiArticle;
import info.bliki.wiki.dump.WikiXMLParser;
import info.bliki.wiki.filter.PlainTextConverter;
import info.bliki.wiki.model.WikiModel;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.apache.lucene.queryparser.classic.ParseException;
import org.xml.sax.SAXException;

import edu.yeditepe.lucene.EntitySearchEngine;
import edu.yeditepe.model.EntityPage;
import edu.yeditepe.nlp.Zemberek;
import edu.yeditepe.similarity.GoogleDistanceCalculator;
import edu.yeditepe.utils.Property;

public class WikiParserSuffix {
	private static final Logger LOGGER = Logger
			.getLogger(WikiParserSuffix.class);

	private static BufferedWriter bw;
	public static long correct = 0;
	public static long incorrect = 0;
	public static long undetected = 0;
	public static float totalTypes = 0;
	public static float totalCount = 0;
	public static double threshold = 0.01;
	public static double precision = 0.0;
	public static double recall = 0.0;
	public static double fmeasure = 0.0;
	public static GoogleDistanceCalculator gd = new GoogleDistanceCalculator();

	public static void main(String[] args) throws Exception {
		String test = "kanunda assd dsd kanunu. asd";
		String query = "kanun";
		while (test.contains(query)) {
			int start = test.indexOf(query);
			int end = test.indexOf(" ", start);
			if (end > start) {
				LOGGER.info(test.substring(start, end).replaceAll("\\W", ""));
				test = test.substring(end);
			} else {
				break;
			}
		}

		parse();
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
		float averageSenses = totalTypes / totalCount;
		LOGGER.info("AverageSenses" + averageSenses);
	}

	public static void parse() {
		try {

			IArticleFilter handler = new DemoArticleFilter();
			WikiXMLParser wxp = new WikiXMLParser(Property.getInstance().get(
					"wiki.dump"), handler);
			wxp.parse();

		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	static class DemoArticleFilter implements IArticleFilter {
		final static Pattern regex = Pattern.compile(
				"[A-Z][\\p{L}\\w\\p{Blank},\\\"\\';\\[\\]\\(\\)-]+[\\.!]",
				Pattern.CANON_EQ);

		// Convert to plain text
		WikiModel wikiModel = new WikiModel("${image}", "${title}");
		EntitySearchEngine instance = EntitySearchEngine.getInstance();
		List<EntityPage> pages = null;

		public void process(WikiArticle page, Siteinfo siteinfo)
				throws SAXException {

			if (page != null && page.getText() != null
					&& page.getTitle().contains("(")
					&& !page.getText().startsWith("#REDIRECT ")
					&& !page.getTitle().contains("(anlam ayrımı)")

			) {

				try {
					String title = page.getTitle();
					if (title.contains("(")) {
						title = title.substring(0, title.indexOf("(")).trim();
					}
					pages = instance.performExactEntitySearch(
							spotToLuceneString(title),
							spotToLuceneString(title));
					Set<String> types = new HashSet<String>();
					Set<String> suffixes = new HashSet<String>();
					String pagetype = "";
					for (EntityPage entityPage : pages) {
						if (entityPage.getId().equals("w" + page.getId())) {
							pagetype = entityPage.getType()
									.replaceAll("_", " ");
							types.add(pagetype);
							// LOGGER.info(entityPage.getTitle() + " - "
							// + entityPage.getType());
						} else if (entityPage.getType() != null
								&& entityPage.getType().length() > 2) {
							types.add(entityPage.getType().replaceAll("_", " "));
						}
					}
					if (types.size() > 1 && pagetype.length() > 0) {

						String wikiText = page.getText();
						String plainStr = wikiModel.render(
								new PlainTextConverter(), wikiText);
						plainStr = plainStr.toLowerCase(new Locale("tr", "TR"));
						title = title.toLowerCase(new Locale("tr", "TR"));
						while (plainStr.contains(title)) {
							int start = plainStr.indexOf(title);
							int end = plainStr.indexOf(" ",
									start + title.length());
							if (end > start) {
								String suffix = plainStr.substring(start, end)
										.replaceAll("\\W", "");
								if (// suffix.contains("'")||
								(Zemberek.getInstance().hasMorph(suffix)
										&& !suffix.equals(title) && suffix
										.length() > 4)) {
									suffixes.add(suffix);
								}
								plainStr = plainStr.substring(end);
							} else {
								break;
							}
						}
						Map<String, Double> similarityScores = new HashMap<String, Double>();
						if (suffixes.size() < 10 || suffixes.size() > 15) {
							return;
						}
						for (String suffix : suffixes) {
							for (String type : types) {
								double sim = gd.calculateSimilarity(suffix,
										type);
								if (similarityScores.containsKey(type)) {
									similarityScores.put(type,
											similarityScores.get(type) + sim);
								} else {
									similarityScores.put(type, sim);
								}
							}
						}
						String maxscoreType = "";
						double maxScore = -1;
						for (String type : types) {
							try {

								double score = similarityScores.get(type)
										/ suffixes.size();
								if (score > maxScore) {
									maxScore = score;
									maxscoreType = type;
								}
							} catch (Exception e) {
								LOGGER.info(e);
							}
							// LOGGER.info(type + " - "
							// + similarityScores.get(type)
							// / suffixes.size());
						}
						if (maxScore == 0) {
							return;
						}
						if (maxScore < threshold) {
							undetected++;
							LOGGER.info("Below Threshold " + page.getTitle()
									+ " Score: " + maxScore + " for "
									+ maxscoreType);
						}
						if (maxscoreType.equals(pagetype)) {
							correct++;
							LOGGER.info("Correct " + page.getTitle()
									+ " Score: " + maxScore + " for "
									+ maxscoreType);
						} else {
							incorrect++;
							LOGGER.info("Incorrect " + page.getTitle()
									+ " Score: " + maxScore + " for "
									+ maxscoreType);
						}
						totalTypes += types.size();
						totalCount++;

					}

				} catch (IOException e) {
					// TODO Auto-generated catch block
					// e.printStackTrace();
				} catch (ParseException e) {
					// TODO Auto-generated catch block
					// e.printStackTrace();
				}

				// .replaceAll("[=]+[A-Za-z+\\s-]+[=]+", " ")
				// .replaceAll("\\{\\{[A-Za-z0-9+\\s-]+\\}\\}", " ")
				// .replaceAll("(?m)<ref>.+</ref>", " ")
				// .replaceAll(
				// "(?m)<ref name=\"[A-Za-z0-9\\s-]+\">.+</ref>",
				// " ").replaceAll("<ref>", " <ref>");

				// Remove text inside {{ }}

				// .replaceAll("\\{\\{[A-Za-z+\\s-]+\\}\\}", " ");

				// MYSQL.insertWikiContent(Integer.parseInt(page.getId()),
				// plainStr);
				// Matcher regexMatcher = regex.matcher(plainStr);
				// while (regexMatcher.find()) {
				// // Get sentences with 6 or more words
				// String sentence = regexMatcher.group();
				//
				// if (matchSpaces(sentence, 5)) {
				// try {
				// bw.write(Zemberek.getInstance().disambiguate(
				// sentence)
				// + "\n");
				// } catch (Exception e) {
				// // TODO Auto-generated catch block
				// // e.printStackTrace();
				// }
				// }
				// }

			}
		}

		private boolean matchSpaces(String sentence, int matches) {

			int c = 0;
			for (int i = 0; i < sentence.length(); i++) {
				if (sentence.charAt(i) == ' ')
					c++;
				if (c == matches)
					return true;
			}
			return false;
		}

	}

	public static String spotToLuceneString(String url) {
		url = url.toLowerCase(new Locale("tr", "TR"));
		url = url.replace(" ", "_");

		return url;
	}
}
