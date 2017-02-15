package edu.yeditepe.nounphrase;

import info.bliki.wiki.dump.IArticleFilter;
import info.bliki.wiki.dump.Siteinfo;
import info.bliki.wiki.dump.WikiArticle;
import info.bliki.wiki.dump.WikiXMLParser;
import info.bliki.wiki.model.WikiModel;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.xml.sax.SAXException;

import edu.yeditepe.nlp.Zemberek;
import edu.yeditepe.utils.FileUtils;
import edu.yeditepe.utils.Property;

public class NounPhraseCreateData {
	private static final Logger LOGGER = Logger
			.getLogger(NounPhraseCreateData.class);

	public static void main(String[] args) throws UnsupportedEncodingException,
			FileNotFoundException, IOException, SAXException {

		IArticleFilter handler = new DemoArticleFilter();
		WikiXMLParser wxp = new WikiXMLParser(Property.getInstance().get(
				"wiki.dump"), handler);
		wxp.parse();
	}

	static class DemoArticleFilter implements IArticleFilter {
		final static Pattern regex = Pattern.compile(
				"[A-Z][\\p{L}\\w\\p{Blank},\\\"\\';\\[\\]\\(\\)-]+[\\.!]",
				Pattern.CANON_EQ);

		// Convert to plain text
		WikiModel wikiModel = new WikiModel("${image}", "${title}");

		public void process(WikiArticle page, Siteinfo siteinfo)
				throws SAXException {

			if (page != null && page.getText() != null
					&& !page.getText().startsWith("#REDIRECT ")) {

				PrintStream out = null;

				try {
					out = new PrintStream(System.out, true, "UTF-8");
				} catch (UnsupportedEncodingException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				String wtext = page.getText();
				wtext = wtext.replaceAll("''", "");
				wtext = wtext.replaceAll(" '", "'");
				wtext = wtext.replaceAll("\\[\\[", "XXXXX");
				wtext = wtext.replaceAll("]]", "WWWWW");
				wtext = wtext.replaceAll("\\s+", " ").trim();
				List<String> splitSentences = Zemberek.getInstance()
						.splitSentences(wtext);
				for (String sentence : splitSentences) {
					if (sentence.contains("{{") || !sentence.contains(".")) {
						continue;
					}
					String[] tokens = Zemberek.getInstance().tokenize(sentence);
					if (tokens.length < 5) {
						continue;
					}
					for (int i = 0; i < tokens.length; i++) {

						String link = "";
						String prevWord = "";
						String nextWord = "";
						if (tokens[i].contains("XXXXX")) {
							if (i > 1) {
								prevWord = tokens[i - 1];
								try {
									if (tokens[i - 1].startsWith("'")) {
										prevWord = tokens[i - 2] + prevWord;
									}
								} catch (Exception e) {
									// TODO: handle exception
								}
							}
							if (tokens[i].contains("WWWWW")) {

								link = tokens[i].replaceAll("XXXXX", "");
								link = link.replaceAll("WWWWW", "");
								try {
									if (tokens[i + 1].startsWith("'")) {
										link += tokens[i + 1];
										i++;
									}
								} catch (Exception e) {
									// TODO: handle exception
								}

								link.trim();
								if (i < tokens.length - 1) {
									nextWord = tokens[i + 1];
								}

							} else {

								for (int j = i; j < tokens.length; j++) {
									link += tokens[j] + " ";
									if (tokens[j].contains("WWWWW")) {
										try {
											if (tokens[j + 1].startsWith("'")) {
												link += tokens[j + 1];
												i++;
												j++;
											}
										} catch (Exception e) {
											// TODO: handle exception
										}

										if (j < tokens.length - 1) {
											nextWord = tokens[j + 1];
										}
										break;
									}
									i++;
								}
								link = link.replaceAll("XXXXX", "");
								link = link.replaceAll("WWWWW", "");

							}
							prevWord = cleanPrevNext(prevWord);
							nextWord = cleanPrevNext(nextWord);

							if (link.contains("|")) {
								link = link.substring(link.indexOf("|") + 1);
							}
							// link = link.replaceAll(" .", ".");
							link = link.replaceAll("\\s+", " ").trim();
							Pattern pattern = Pattern
									.compile("[;*{}<>:|(),\"]");
							Matcher matcher = pattern.matcher(link);
							if (!matcher.find()) {
								LOGGER.info("P:" + prevWord + " L:" + link
										+ " N:" + nextWord);
								FileUtils.writeFile(prevWord + "\t" + link
										+ "\t" + nextWord + "\n",
										"nounphrases.txt");

							}

						}
					}
				}

			}

		}
	}

	public static String cleanPrevNext(String in) {
		in = in.replaceAll("XXXXX", "");
		in = in.replaceAll("WWWWW", "");
		if (in.contains("|")) {
			in = in.substring(in.indexOf("|") + 1);
		}
		return in.trim();

	}
}
