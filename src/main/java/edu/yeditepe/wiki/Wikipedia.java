package edu.yeditepe.wiki;

import info.bliki.wiki.dump.IArticleFilter;
import info.bliki.wiki.dump.Siteinfo;
import info.bliki.wiki.dump.WikiArticle;
import info.bliki.wiki.dump.WikiXMLParser;
import info.bliki.wiki.filter.PlainTextConverter;
import info.bliki.wiki.model.WikiModel;
import it.cnr.isti.hpc.dexter.util.URLConnectionReader;
import it.cnr.isti.hpc.text.SentenceSegmenter;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.math3.linear.RealVector;
import org.apache.log4j.Logger;
import org.xml.sax.SAXException;

import com.google.common.collect.TreeMultiset;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import edu.yeditepe.lucene.EntitySearchEngine;
import edu.yeditepe.nlp.ITUNLP;
import edu.yeditepe.nlp.TurkishNLP;
import edu.yeditepe.nlp.Zemberek;
import edu.yeditepe.repository.MYSQL;
import edu.yeditepe.repository.MongoSentenceRepository;
import edu.yeditepe.utils.FileUtils;
import edu.yeditepe.utils.Property;

public class Wikipedia {
	private static final Logger LOGGER = Logger.getLogger(Wikipedia.class);

	public static final String DISAMBIGUATION = "(anlam ayrımı)";

	public static final String DISAMBIGUATION2 = "Diğer anlamı|";

	public static final String DISAMBIGUATION3 = "{{anlam ayrımı}}";

	public static final String REDIRECT = "#YÖNLENDİR";

	public static final String REDIRECT2 = "#redirect";

	private static MongoSentenceRepository repository;

	private static ITUNLP nlp;

	private static int counter = 0;

	private static int windowSize = 20;

	private static Map<String, String> domains;

	public static HashSet<String> getUpperCasePages() {
		return upperCasePages;
	}

	public Map<String, RealVector> getTextVectors() {
		return textVectors;
	}

	public static void setUpperCasePages(HashSet<String> upperCasePages) {
		Wikipedia.upperCasePages = upperCasePages;
	}

	public static HashSet<String> getDisambiguation() {
		return disambiguation;
	}

	public static void setDisambiguation(HashSet<String> disambiguation) {
		Wikipedia.disambiguation = disambiguation;
	}

	private static HashMap<String, Integer> pageLength = new HashMap<String, Integer>();

	private static HashSet<String> upperCasePages = new HashSet<String>();

	private static HashSet<String> disambiguation = new HashSet<String>();

	private static HashMap<String, String> disambiguationPages = new HashMap<String, String>();

	private static HashMap<String, String> types = new HashMap<String, String>();

	private static HashMap<String, Integer> typesCount = new HashMap<String, Integer>();

	public static HashMap<String, Integer> getTypesCount() {
		return typesCount;
	}

	public static void setTypesCount(HashMap<String, Integer> typesCount) {
		Wikipedia.typesCount = typesCount;
	}

	private static HashMap<String, String> pageContent = new HashMap<String, String>();

	private static HashMap<String, ArrayList<String>> pageReferenceContent2 = new HashMap<String, ArrayList<String>>();

	private static HashMap<String, String> pageReferenceContent = new HashMap<String, String>();

	private static HashMap<String, StringBuffer> pageMorphReferenceContent = new HashMap<String, StringBuffer>();

	public static HashMap<String, String> getPageReferenceContent() {
		return pageReferenceContent;
	}

	public static void setPageReferenceContent(
			HashMap<String, String> pageReferenceContent) {
		Wikipedia.pageReferenceContent = pageReferenceContent;
	}

	public static HashMap<String, String> getPageContent() {
		return pageContent;
	}

	public static HashMap<String, String> getTypes() {
		return types;
	}

	public static void setTypes(HashMap<String, String> types) {
		Wikipedia.types = types;
	}

	private static Map<String, RealVector> textVectors;

	private String fileName;

	private static Set<String> experimentPages;

	private static boolean processText;

	private static boolean experiment;

	private static boolean vsm;

	private static boolean flag = true;

	private static HashMap<String, String> pagePlainText = new HashMap<String, String>();

	private static HashMap<String, HashMap<String, Integer>> pageLinks = new HashMap<String, HashMap<String, Integer>>();

	public static HashMap<String, String> getPagePlainText() {
		return pagePlainText;
	}

	public static void setPagePlainText(HashMap<String, String> pagePlainText) {
		Wikipedia.pagePlainText = pagePlainText;
	}

	public static HashMap<String, HashMap<String, Integer>> getPageLinks() {
		return pageLinks;
	}

	public static void setPageLinks(
			HashMap<String, HashMap<String, Integer>> pageLinks) {
		Wikipedia.pageLinks = pageLinks;
	}

	private static int taxoCounter = 0;

	private static int infoCounter = 0;

	private static Set<String> pageTitles;

	public Wikipedia(String fileName, MongoSentenceRepository repository,
			ITUNLP nlp) throws IOException {
		this.repository = repository;
		this.nlp = nlp;
		this.fileName = fileName;
	}

	public Wikipedia() throws IOException {
		fileName = Property.getInstance().get("wiki.dump");
		// textCosineDocumentSimilarity = new CosineDocumentSimilarity();
	}

	public static String getWikiTextOnline(String title, String lang) {
		try {
			return URLConnectionReader.getContent("https://" + lang
					+ ".wikipedia.org/wiki/" + title + "?action=raw");
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

	public static boolean isDisambiguationPage(String title, String wikitext) {
		if (StringUtils.containsIgnoreCase(title, DISAMBIGUATION)
				|| StringUtils.startsWithIgnoreCase(wikitext, DISAMBIGUATION3)
				|| StringUtils.endsWithIgnoreCase(wikitext, DISAMBIGUATION3)) {
			return true;
		}

		return false;
	}

	public static boolean isUpperCase(String title, String wikitext) {
		if (title == null || wikitext == null) {
			return false;
		}
		if (title.contains("(")) {
			title = title.substring(0, title.indexOf("(")).trim();
		}
		String upper = title.replace("_", " ");
		int upperCount = StringUtils.countMatches(wikitext, upper);
		String lower = wikiUrlToStringLowerCase(title);
		int lowerCount = StringUtils.countMatches(wikitext, lower);
		float ratio = (float) lowerCount / (lowerCount + upperCount);
		if (ratio == 0 && upperCount >= 2) {
			return true;
		} else if (lowerCount >= upperCount || lowerCount + upperCount <= 5) {
			return false;
		} else if (ratio < 0.4) {
			return true;
		}
		return false;

	}

	public void experiment() {
		try {
			this.experiment = true;
			this.experimentPages = MYSQL.getExperimentPages();
			IArticleFilter handler = new DemoArticleFilter();
			WikiXMLParser wxp = new WikiXMLParser(fileName, handler);
			wxp.parse();
		} catch (Exception e) {
			// TODO: handle exception
		}

	}

	public void process(boolean processText) {
		try {

			Reader reader = new FileReader("domain.json");
			Gson gson = new GsonBuilder().create();
			domains = gson.fromJson(reader, Map.class);

			this.processText = processText;
			pageTitles = new HashSet<String>(MYSQL.getPageTitles());
			IArticleFilter handler = new DemoArticleFilter();
			WikiXMLParser wxp = new WikiXMLParser(fileName, handler);
			wxp.parse();
			if (processText) {

				for (String s : pageMorphReferenceContent.keySet()) {
					FileUtils
							.writeFile(pageMorphReferenceContent.get(s)
									.toString(), "domain" + File.separator + s
									+ ".txt");
				}

				// for (String s : pageReferenceContent2.keySet()) {
				// TreeMultiset<String> set = TreeMultiset.create();
				// ArrayList<String> sentence = pageReferenceContent2.get(s);
				// set.addAll(pageReferenceContent2.get(s));
				// int counter = 0;
				// String reference = "";
				// for (String r : set.elementSet()) {
				// if (set.count(r) >= 3 && r.length() > 3
				// && r.length() < 15
				// && Character.isUpperCase(r.charAt(0))) {
				// reference += r + " ";
				// if (++counter == windowSize) {
				// break;
				// }
				// }
				// }
				// if (reference.trim().length() > 0) {
				// pageReferenceContent.put(s, reference);
				//
				// }
				// }
				// Writer writer;
				// try {
				// writer = new FileWriter("reference.json");
				// Gson gson = new GsonBuilder().create();
				// gson.toJson(Wikipedia.pageReferenceContent, writer);
				//
				// writer.close();
				// } catch (IOException e) {
				// // TODO Auto-generated catch block
				// e.printStackTrace();
				// }
				//
				// try {
				// writer = new FileWriter("content.json");
				// Gson gson = new GsonBuilder().create();
				// gson.toJson(Wikipedia.pageContent, writer);
				//
				// writer.close();
				// } catch (IOException e) {
				// // TODO Auto-generated catch block
				// e.printStackTrace();
				// }

				// try {
				// writer = new FileWriter("type.json");
				// Gson gson = new GsonBuilder().create();
				// gson.toJson(Wikipedia.types, writer);
				//
				// writer.close();
				// } catch (IOException e) {
				// // TODO Auto-generated catch block
				// e.printStackTrace();
				// }

			}
			// textVectors = textCosineDocumentSimilarity.calculateVectors();
			// for (String string : disambiguation) {
			// System.out.println(string);
			// }
			// for (String string : disambiguationPages.keySet()) {
			// System.out.println(string + " \t "
			// + disambiguationPages.get(string));
			// }
		} catch (Exception e) {
			LOGGER.error(e);
		}
	}

	static class DemoArticleFilter implements IArticleFilter {

		public boolean process(WikiArticle page) {
			System.out.println("----------------------------------------");
			System.out.println(page.getTitle());
			System.out.println("----------------------------------------");
			System.out.println(page.getText());
			return true;
		}

		public void process(WikiArticle page, Siteinfo arg1)
				throws SAXException {
			// LOGGER.debug("Wiki page parsing starts");
			if (flag) {
				if (page.getTitle()
						.equals("2012 Yaz Olimpiyatları'nda atletizm - Erkekler 10.000 metre")) {
					flag = false;

				}
				return;
			}
			try {
				if (counter % 1000 == 0) {
					for (String s : pageMorphReferenceContent.keySet()) {
						// FileUtils.writeFile(pageMorphReferenceContent.get(s).toString(),
						// "C:\\experiment" + File.separator + s + ".txt");
					}
					pageMorphReferenceContent.clear();
				}
				// System.out.println(counter++);
				String wikitext = page.getText();
				String title = page.getTitle();
				String url = StringToWikiUrl(title);

				// if (page.getId().equals("13855")) {
				// System.out.println();
				// }

				int length = 0;
				if (wikitext != null) {
					length = wikitext.length();
				}
				pageLength.put(page.getId(), length);

				// if (StringUtils.containsIgnoreCase(wikitext,
				// DISAMBIGUATION2)) {
				// // disambiguationPages.put(title, Freebase.getType(title,
				// // "tr"));
				// disambiguationPages.put(title, null);
				// }
				if (isUpperCase(title, wikitext)) {
					upperCasePages.add(page.getId());
				}

				if (StringUtils.startsWithIgnoreCase(wikitext, REDIRECT)
						|| StringUtils
								.startsWithIgnoreCase(wikitext, REDIRECT2)) {
					// LOGGER.debug("REDIRECT:" + title);
				} else if (StringUtils.containsIgnoreCase(title, ":")) {
					// LOGGER.debug("Ignore:" + title);
				} else if ((StringUtils.containsIgnoreCase(title,
						DISAMBIGUATION) || StringUtils.endsWithIgnoreCase(
						wikitext, DISAMBIGUATION3))
						|| (StringUtils.containsIgnoreCase(wikitext,
								DISAMBIGUATION) && length < 3000)) {
					// LOGGER.debug("DISAMBIGUATION:" + title + " " + length);
					disambiguation.add(page.getId());
				}

				else {
					if (processText) {
						String urlType = "";
						try {
							if (url.contains("_(")) {
								urlType = TurkishNLP.toLowerCase(
										url.substring(url.indexOf("_(") + 2,
												url.indexOf(")"))).replaceAll(
										"_", " ");
							}
						} catch (Exception e) {
						}

						String infoText = "";
						try {
							int i = wikitext.indexOf(" bilgi kutusu");
							if (i >= 0) {
								infoText = wikitext.substring(0, i);
								infoText = TurkishNLP
										.toLowerCase(infoText
												.substring(infoText
														.lastIndexOf("{{") + 2));
							}
						} catch (Exception e) {

						}
						String taxoText = "";
						try {
							int t = wikitext.indexOf("{{Takso");
							if (t >= 0) {
								taxoText = wikitext.substring(t + 7);
								if (!taxoText.startsWith("nlar}}")
										&& !taxoText.startsWith("nomi")
										&& !taxoText.startsWith("kutu")) {
									taxoText = TurkishNLP
											.toLowerCase(taxoText.substring(0,
													taxoText.indexOf("\n") + 1));

								} else {
									taxoText = "";
								}
							}
						} catch (Exception e) {

						}

						if (taxoText.length() != 0 && infoText.length() == 0) {
							// LOGGER.debug("taxoText:" + taxoText);
							taxoCounter++;
						} else if (taxoText.length() == 0
								&& infoText.length() != 0) {
							// LOGGER.debug("infobox:" + infoText);
							infoCounter++;
						}
						if (taxoText.length() != 0 && infoText.length() != 0
								&& !taxoText.equals(infoText)) {
							LOGGER.debug("not equal taxo:" + taxoText
									+ " info:" + infoText);

						}

						if (taxoText.length() == 0 && urlType.length() != 0) {
							// LOGGER.debug("urlType:" + urlType);

						}
						LOGGER.debug(taxoCounter + " " + infoCounter);
						String type = "";
						if (urlType.length() > 0) {
							type = urlType;
						} else if (infoText.length() > 0) {
							type = infoText;
						} else if (taxoText.length() > 0) {
							type = taxoText;
						} else if (urlType.length() > 0) {
							type = urlType;
						}
						if (type.length() > 50) {
							type = "";
						}
						type = type.replaceAll("\n", "").trim();
						// if (type.length() == 0) {
						// LOGGER.debug(page.getTitle());
						// }
						// // LOGGER.debug(title + " : " + type);
						// types.put(page.getId(), type);
						//
						// // if (typesCount.containsKey(type)) {
						// // typesCount.put(type, typesCount.get(type) + 1);
						// // } else {
						// // typesCount.put(type, 1);
						// // }

						String enTitle = MYSQL.getEnTitle(page.getTitle()
								.replace(" ", "_"));
						String domain = domains.get(enTitle);
						if (wikitext.indexOf("'''") > 0) {
							wikitext = wikitext.substring(wikitext
									.indexOf("'''"));
						}
						SentenceSegmenter ss = SentenceSegmenter.getInstance();
						wikitext = wikitext.replaceAll("\\.", ". ");
						wikitext = wikitext.replaceAll("\\s+", " ").trim();

						Matcher matcher = Pattern.compile("\\[\\[([^\\]\\]]+)")
								.matcher(wikitext);
						HashMap<String, String> tags = new HashMap<String, String>();

						int pos = -1;
						while (matcher.find(pos + 1)) {
							try {
								pos = matcher.start();
								String[] t = matcher.group(1).split("\\|");

								// if (!t[0].contains("Dosya")
								// && t[0].contains("(")) {
								// tags.put(
								// t[1],
								// TurkishNLP
								// .toLowerCase(getInsideParanthesis(t[0])));
								//
								//
								// }
								String uri = t[0].replace(" ", "_");
								int id = MYSQL.getIdFromRedirect(uri);
								if (id > 0) {

									if (t.length == 1) {
										tags.put(t[0], uri);
									} else if (t.length == 2) {
										tags.put(t[1], uri);
									}
								}
							} catch (Exception e) {
								LOGGER.error(e);
							}

						}
						if (tags.size() > 0) {
							String plain = getPlainText(wikitext);
							String[] sentences = ss.split(plain);
							// HashSet<String> pageSet = new HashSet<String>();
							TreeMultiset<String> pageSet = TreeMultiset
									.create();
							for (String s : sentences) {
								HashSet<String> set = new HashSet<String>();
								if (s.length() > 0 && !s.contains("style")
										&& !s.contains("align")
										&& !s.contains("_")) {

									String[] words = s.split(" ");
									ArrayList<String> l = new ArrayList<String>();
									String morphSen = "";
									if (words.length < 5) {
										continue;
									}
									for (String w : words) {
										String morph = Zemberek.getInstance()
												.morphPageContent(w);
										if (!StringUtils.isNumeric(morph)) {
											l.add(morph);
											morphSen += morph + " ";
										}

									}

									morphSen = morphSen.replaceAll("\\s+", " ")
											.trim() + " \n";
									// if (morphSen.contains("yaz idi")) {
									// continue;
									// }
									type = domain;
									// if (type != null && type.length() > 0) {
									// set.add(type);
									// if (pageMorphReferenceContent
									// .containsKey(type)) {
									//
									// pageMorphReferenceContent.get(type)
									// .append(morphSen);
									// } else {
									//
									// pageMorphReferenceContent.put(type,
									// new StringBuffer(morphSen));
									// }
									// }

									// String morphSen = s;
									for (String tag : tags.keySet()) {

										if (StringUtils.containsIgnoreCase(s,
												" " + tag + " ")) {

											// if (pageReferenceContent2
											// .containsKey(tag)) {
											// pageReferenceContent2.get(tag)
											// .addAll(l);
											// } else {
											//
											// pageReferenceContent2.put(tag,
											// l);
											// }
											String tagValue = tags.get(tag);

											// enTitle = MYSQL
											// .getEnTitle(tagValue);
											// tagValue = domains.get(enTitle);
											if (tagValue != null
													&& !set.contains(tagValue)) {
												set.add(tagValue);
												if (pageMorphReferenceContent
														.containsKey(tagValue)) {
													StringBuffer sc = pageMorphReferenceContent
															.get(tagValue);
													if (!StringUtils.contains(
															sc, morphSen)) {
														sc.append(morphSen);
													}

												} else {

													pageMorphReferenceContent
															.put(tagValue,
																	new StringBuffer(
																			morphSen));
												}
											}
										}

									}

									// pageSet.addAll(l);
								}
							}
						}
						// String pcontent = "";
						// int counter = 0;
						//
						// for (String s : pageSet.descendingMultiset()
						// .elementSet()) {
						// if (pageSet.count(s) >= 3 && s.length() > 3
						// && s.length() < 15
						// && Character.isUpperCase(s.charAt(0))) {
						// pcontent += s + " ";
						// if (++counter == windowSize) {
						// break;
						// }
						// }
						// }
						// if (pcontent.trim().length() > 0) {
						// pageContent.put(page.getId(), pcontent.trim());
						//
						// }

						// content.put(page.getId(), text);
					} else if (experiment) {
						try {
							if (experimentPages.contains(page.getId())) {
								// String[] lines =
								// wikitext.split(StringUtils.LF);
								// String content = "";
								// for (String l : lines) {
								// if (l.contains(".") && !l.startsWith("*")
								// && !l.startsWith("|")) {
								// content += l + " ";
								// if (StringUtils.countMatches(content,
								// ". ") >= 200) {
								// break;
								// }
								// ;
								// }
								// }
								// wikitext = content;
								// wikitext = wikitext.replaceAll(
								// "<ref(.+?)</ref>", "");
								String plain = getPlainText(wikitext);
								Matcher matcher = Pattern.compile(
										"\\[\\[([^\\]\\]]+)").matcher(wikitext);
								HashMap<String, Integer> tags = new HashMap<String, Integer>();
								int pos = -1;
								while (matcher.find(pos + 1)) {
									pos = matcher.start();
									try {
										String[] tag = matcher.group(1).split(
												"\\|");
										// duz metinde icerme kosulu eklenebilir
										int id = MYSQL.getInstance()
												.getIdFromRedirect(
														tag[0].replaceAll(" ",
																"_"));

										if (id > 0
												&& !StringUtils
														.isNumeric(tag[0])) {
											if (EntitySearchEngine
													.getInstance().getPage(
															String.valueOf(id)) != null) {

												String s = tag[0];
												if (s.contains("(")) {
													s = s.substring(0,
															s.indexOf("("))
															.trim();
												}
												if (tag.length == 2
														&& !tags.containsKey(tag[0])
														&& StringUtils
																.containsIgnoreCase(
																		tag[1],
																		s)
														&& !StringUtils
																.isNumeric(tag[1])
														&& StringUtils
																.containsIgnoreCase(
																		plain,
																		tag[1])) {
													tags.put(tag[1], id);
												} else if (tag.length == 1
														&& StringUtils
																.containsIgnoreCase(
																		plain,
																		tag[0])) {
													tags.put(tag[0], id);
												}
											}
										}
									} catch (Exception e) {
										// TODO: handle exception
									}

								}

								pagePlainText.put(page.getTitle(), plain);
								pageLinks.put(page.getTitle(), tags);
							}
						} catch (Exception e) {
							LOGGER.error(e);
						}
					} else if (vsm) {

						Matcher matcher = Pattern.compile("\\[\\[([^\\]\\]]+)")
								.matcher(wikitext);
						HashMap<String, Integer> tags = new HashMap<String, Integer>();
						if (title.contains("(")) {
							title = title.substring(0, title.indexOf("("))
									.trim();
						}
						tags.put(
								Zemberek.getInstance().morphPageContent(title),
								Integer.parseInt(page.getId()));
						int pos = -1;
						while (matcher.find(pos + 1)) {
							pos = matcher.start();
							try {
								String t = matcher.group(1);
								String[] tag = t.split("\\|");
								int id = MYSQL.getInstance().getIdFromRedirect(
										tag[0].replaceAll(" ", "_"));
								if (id > 0) {
									wikitext = wikitext.replaceAll("\\[\\[(["
											+ t + "]\\]])",
											"_" + String.valueOf(id) + "_");
									if (EntitySearchEngine.getInstance()
											.getPage(String.valueOf(id)) != null) {

										String s = tag[0];
										if (s.contains("(")) {
											s = s.substring(0, s.indexOf("("))
													.trim();
										}
										if (tag.length == 2) {
											tags.put(Zemberek.getInstance()
													.morphPageContent(tag[1]),
													id);
										} else if (tag.length == 1) {
											tags.put(Zemberek.getInstance()
													.morphPageContent(tag[0]),
													id);
										}
									}
								}

							} catch (Exception e) {
								// TODO: handle exception
							}

						}
						String plain = getPlainText(wikitext);
						String[] words = plain.split(" ");
						ArrayList<String> l = new ArrayList<String>();
						String morphSen = "";

						for (String w : words) {
							String morph = Zemberek.getInstance()
									.morphPageContent(w);
							if (!StringUtils.isNumeric(morph)) {
								l.add(morph);
								morphSen += morph + " ";
							}

						}

						morphSen = morphSen.replaceAll("\\s+", " ").trim()
								+ " \n";

						for (String tag : tags.keySet()) {
							morphSen = morphSen
									.replaceAll(
											" " + tag + " ",
											String.valueOf(" _" + tags.get(tag)
													+ "_ "));
						}
						FileUtils.writeFile(morphSen, "C:\\experiment2\\"
								+ page.getTitle() + ".txt");
					}
				}

			} catch (Exception e) {
				LOGGER.error(e);
			}
		}
	}

	public static String getPlainText(String wiki) {
		WikiModel wikiModel = new WikiModel(
				"http://www.mywiki.com/wiki/${image}",
				"http://www.mywiki.com/wiki/${title}");

		wiki = wiki.replaceAll("\\{\\|(.+?)\\|\\}", "");
		try {
			wiki = wiki.substring(0, wiki.indexOf("== Kaynaklar =="));
		} catch (Exception e) {
			// TODO: handle exception
		}

		String plainStr = wikiModel.render(new PlainTextConverter(), wiki);
		// LOGGER.info(plainStr);

		String text = plainStr;
		// text = text
		// .replaceAll(
		// "[^AaBbCcÇçDdEeFfGgĞğHhIiıİJjKkLlMmNnOoÖöPpRrSsŞşTtUuÜüVvYyZz1234567890, ]",
		// " ");
		text = text.replaceAll("[0-9]+px", "");
		text = text.replaceAll("&nbsp;", " ");
		text = text.replaceAll("\\{\\{(.+?)\\}\\}", "");
		// text = text.replaceAll("\\[(.+?)\\]", "");
		text = text.replaceAll("\\.", ". ");
		text = text.replaceAll("\n", " ");
		text = text.replaceAll("\r", " ");
		text = text.replaceAll("\\s+", " ").trim();
		if (text.contains("]]")) {
			text = text.substring(text.lastIndexOf("]]") + 2);
		}
		text = StringEscapeUtils.unescapeHtml(text);
		text = text.replaceAll("\"", "");
		// LOGGER.info(text);
		return text;
	}

	public static HashMap<String, Integer> getPageLength() {
		return pageLength;
	}

	public static void setPageLength(HashMap<String, Integer> pageLength) {
		Wikipedia.pageLength = pageLength;
	}

	public static String wikiUrlToString(String url) {
		url = url.replaceAll("_", " ");
		if (url.contains("(")) {
			url = url.substring(0, url.indexOf("(")).trim();
		}

		return url;
	}

	public static String StringToWikiUrl(String url) {
		url = url.trim().replaceAll(" ", "_");

		return url;
	}

	public static String wikiUrlToStringLowerCase(String url) {
		url = url.toLowerCase(new Locale("tr", "TR"));
		url = url.replaceAll("_", " ");
		if (url.contains("(")) {
			url = url.substring(0, url.indexOf("(")).trim();
		}

		return url;
	}

	public static String stringToLuceneString(String url) {
		url = url.toLowerCase(new Locale("tr", "TR"));
		url = url.replace(" ", "_");
		if (url.contains("_(")) {
			url = url.substring(0, url.indexOf("_(")).trim();
		}
		return url;
	}

	public static void main(String[] args) {
		Wikipedia wikipedia;
		try {
			wikipedia = new Wikipedia();
			wikipedia.vsm = true;
			wikipedia.process(false);
			HashMap<String, Integer> typesCount = wikipedia.getTypesCount();
			for (String key : typesCount.keySet()) {
				System.out.println(key + " \t " + typesCount.get(key));
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		// String title = "Taraf_(gazete)";
		// String wikitext = getWikiTextOnline(title, "tr");
		// System.out.println("is upper : " + isUpperCase(title, wikitext));
	}

	public static String getInsideParanthesis(String str) {
		try {
			int firstBracket = str.indexOf('(');
			return str.substring(firstBracket + 1,
					str.indexOf(')', firstBracket));
		} catch (Exception e) {
			// TODO: handle exception
		}
		return "";

	}
}
