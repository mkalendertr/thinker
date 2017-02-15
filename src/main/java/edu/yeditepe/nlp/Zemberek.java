package edu.yeditepe.nlp;

import it.cnr.isti.hpc.text.Token;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

import opennlp.tools.tokenize.Tokenizer;
import opennlp.tools.tokenize.TokenizerME;
import opennlp.tools.tokenize.TokenizerModel;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import zemberek.morphology.ambiguity.Z3MarkovModelDisambiguator;
import zemberek.morphology.apps.TurkishMorphParser;
import zemberek.morphology.apps.TurkishSentenceParser;
import zemberek.morphology.lexicon.Suffix;
import zemberek.morphology.parser.MorphParse;
import zemberek.morphology.parser.SentenceMorphParse;
import zemberek.tokenizer.SentenceBoundaryDetector;
import zemberek.tokenizer.SimpleSentenceBoundaryDetector;
import edu.yeditepe.component.ApplicationContextHolder;
import edu.yeditepe.discovery.Features;
import edu.yeditepe.model.ZemberekAPIModel;
import edu.yeditepe.repository.MongoZemberekRepository;
import edu.yeditepe.service.MongoService;
import edu.yeditepe.utils.FileUtils;
import edu.yeditepe.utils.Property;

public class Zemberek {
	private static final Logger LOGGER = Logger.getLogger(Zemberek.class);

	private HashMap<String, String> morphCache = new HashMap<String, String>();

	private static Zemberek zemberek = new Zemberek();

	private static TurkishMorphParser morphParser;

	private static Z3MarkovModelDisambiguator disambiguator;

	private static TurkishSentenceParser sentenceParser;;

	private static String strategy = Property.getInstance().get(
			"morphology.strategy");

	private static Tokenizer tokenizer;
	public static Set<String> tdk;

	private Zemberek() {
		try {
			morphParser = TurkishMorphParser.createWithDefaults();
			disambiguator = new Z3MarkovModelDisambiguator();
			sentenceParser = new TurkishSentenceParser(morphParser,
					disambiguator);
			InputStream modelIn = new FileInputStream("nlp//en-token.bin");
			TokenizerModel model = new TokenizerModel(modelIn);
			tokenizer = new TokenizerME(model);
			tdk = FileUtils.readFileSet("tdk.txt");
		} catch (IOException e) {
			LOGGER.error(e);
		}

	}

	public static Zemberek getInstance() {
		return zemberek;
	}

	public static boolean isTurkish(String input) {
		return input
				.matches("[XWQxwqAaBbCcÇçDdEeFfGgĞğHhIiıİJjKkLlMmNnOoÖöPpRrSsŞşTtUuÜüVvYyZz1234567890 \t\n,.-:;'?!\"'(){}\\[\\]<>%]+");

	}

	public static boolean isOnePhrase(String input) {
		return input
				.matches("[XWQxwqAaBbCcÇçDdEeFfGgĞğHhIiıİJjKkLlMmNnOoÖöPpRrSsŞşTtUuÜüVvYyZz1234567890 ]+");

	}

	public String removeNonTurkishChars(String input) {
		return input
				.replaceAll(
						"[^AaBbCcÇçDdEeFfGgĞğHhIiıİJjKkLlMmNnOoÖöPpRrSsŞşTtUuÜüVvYyZz1234567890 ]+",
						"");

	}

	public String removeNonChars(String input) {
		return input
				.replaceAll(
						"[^AaBbCcÇçDdEeFfGgĞğHhIiıİJjKkLlMmNnOoÖöPpRrSsŞşTtUuÜüVvYyZzXxWwQq1234567890 ]+",
						"");

	}

	public static String removeAfterSpostrophe(String input) {
		return input.replaceAll("'[^ ]*|’[^ ]*", "");
	}

	public static List<String> splitSentences(String text) {

		SentenceBoundaryDetector detector = new SimpleSentenceBoundaryDetector();
		List<String> sentences = detector.getSentences(text);
		return sentences;

	}

	public static String normalize(String text) {
		if (!isTurkish(text)) {
			text = StringUtils.stripAccents(text);
		}

		text = removeAfterSpostrophe(text);

		Pattern p = Pattern.compile("[-\"=|,.;:!?(){}\\[\\]<>%]");
		text = p.matcher(text).replaceAll(" ");
		text = text.replaceAll("\\s+", " ").trim();
		return text;
	}

	public List<Token> disambiguateFindTokens(String sentence, boolean merge,
			boolean normalize) {
		List<Token> list = new ArrayList<Token>();
		try {
			SentenceMorphParse sentenceParse;
			if (normalize) {
				sentenceParse = sentenceParser
						.parse(removeNonTurkishChars(sentence));

			} else {
				sentenceParse = sentenceParser.parse(sentence);

			}
			sentenceParser.disambiguate(sentenceParse);
			int start = 0;
			for (SentenceMorphParse.Entry entry : sentenceParse) {
				try {
					start = sentence.indexOf(entry.input, start);
					int end = sentence.indexOf(" ", start);
					// if (end <= 0) {
					end = start + entry.input.length();
					// }
					// if (entry.input.length() > 1) {
					String lemma = entry.parses.get(0).getLemma();
					if (!lemma.equals("UNK")) {
						int i = 0, maxParse = 0;
						if (strategy.equals("longest")) {
							String max = "";
							for (MorphParse parse : entry.parses) {
								if (max.length() < parse.getLemma().length()
										&& parse.dictionaryItem.primaryPos.shortForm
												.equals("Noun")
										&& (tdk.contains(parse.getLemma()) || !StringUtils
												.isAllLowerCase(parse
														.getLemma()))) {
									max = parse.getLemma();
									maxParse = i;
								}
								i++;
							}
						}
						List<String> lemmas = entry.parses.get(maxParse)
								.getLemmas();
						String dictionary = entry.parses.get(maxParse).dictionaryItem.lemma;
						lemma = lemmas.get(lemmas.size() - 1);
						String pos = entry.parses.get(maxParse).getPos()
								.toString();
						// if (entry.parses.get(0).getPos().toString()
						// .equals("Noun")) {
						//
						// }
						List<Suffix> suffixes = entry.parses.get(maxParse)
								.getSuffixes();
						String suffix = "";
						for (Suffix s : suffixes) {
							suffix += s.toString() + " ";
						}
						String ppos = entry.parses.get(maxParse).dictionaryItem.primaryPos.shortForm;
						// if (!(StringUtils.isAllLowerCase(entry.input) &&
						// ppos
						// .equals("Verb"))) {
						if (dictionary != null
								&& dictionary.length() >= lemma.length()) {
							lemma = dictionary;
						}

						Token t = new Token(sentence.substring(start, end),
								lemma, suffix.trim(), ppos, start, end,
								sentence, dictionary

						);
						list.add(t);
						// }

					} else {
						Token t = new Token(sentence.substring(start, end),
								entry.input, "", "", start, end, sentence);
						list.add(t);
					}

					// }
					// if (entry.input.length() > 1
					// && StringUtils.containsIgnoreCase(tokens[i],
					// entry.input)) {
					// }

				} catch (Exception e) {
					// TODO: handle exception
				}
			}
			if (!merge) {
				return list;
			}
			List<Token> mergedList = new ArrayList<Token>();
			if (list.size() > 0) {
				mergedList.add(list.get(0));
				for (int i = 1; i < list.size(); i++) {
					try {
						if (Character
								.isUpperCase(mergedList
										.get(mergedList.size() - 1).getText()
										.charAt(0))
								&& Character.isUpperCase(list.get(i).getText()
										.charAt(0))) {
							String text = sentence.substring(
									mergedList.get(mergedList.size() - 1)
											.getStart(), list.get(i).getEnd());
							if (list.get(i).getStart()
									- mergedList.get(mergedList.size() - 1)
											.getEnd() == 1
									&& isOnePhrase(mergedList.get(
											mergedList.size() - 1).getText())) {
								mergedList.get(mergedList.size() - 1).setEnd(
										list.get(i).getEnd());
								mergedList
										.get(mergedList.size() - 1)
										.setMorphText(
												list.get(i - 1).getText()
														+ " "
														+ list.get(i)
																.getMorphText());
								mergedList.get(mergedList.size() - 1).setText(
										text);
								mergedList.get(mergedList.size() - 1)
										.setSuffix(list.get(i).getSuffix());
								mergedList.get(mergedList.size() - 1).setPos(
										list.get(i).getPos());
							} else {
								mergedList.add(list.get(i));
							}
						} else {
							mergedList.add(list.get(i));
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
				}

			}

			return mergedList;
		} catch (Exception e) {
			// TODO: handle exception
		}
		return list;
	}

	public boolean hasMorph(String input) {
		List<MorphParse> parses = morphParser.parse(input);
		if (parses.size() > 0) {
			return true;
		}
		return false;
	}

	public String morphTokenToAny(String input) {
		// input = removeAfterSpostrophe(input);
		String lower = TurkishNLP.toLowerCase(input);
		if (lower.length() > 1) {

			List<MorphParse> parses = morphParser.parse(lower);
			if (!parses.isEmpty()) {
				String current = "";
				String currentMorph = "? ?";
				if (strategy.equals("shortest")) {
					current = lower;
				}
				for (MorphParse parse : parses) {

					String pos = parse.dictionaryItem.primaryPos.toString();
					String oflazer = parse.formatOflazer();
					String lemma = parse.getLemma();
					String morph = oflazer.substring(oflazer.indexOf('+') + 1);
					// if (pos.equalsIgnoreCase("Verb")
					// && (lemma.endsWith("mak") || lemma.endsWith("mek"))) {
					// lemma = lemma.substring(0, lemma.length() - 3);
					// }
					if (morph.startsWith("Unk") || lemma.length() <= 1) {
						return input + " ? ?";
					} else {
						if (strategy.equals("shortest")) {
							if (lemma.length() < current.length()
									|| (!pos.equalsIgnoreCase("Verb") && lemma
											.length() == current.length())) {
								current = lemma;
								currentMorph = pos
										+ " "
										+ oflazer.substring(oflazer
												.indexOf('+') + 1);

							}
						} else {
							if (lemma.length() > current.length()
									|| (!pos.equalsIgnoreCase("Verb") && lemma
											.length() == current.length())) {
								current = lemma;
								currentMorph = pos
										+ " "
										+ oflazer.substring(oflazer
												.indexOf('+') + 1);

							}
						}

					}
				}
				if (!isTurkish(current)) {
					current = StringUtils.stripAccents(current);
				}

				current = input.charAt(0) + current.substring(1);
				return current + " " + currentMorph;
			}

		}
		if (!isTurkish(input)) {
			input = StringUtils.stripAccents(input);
		}
		input = StringUtils.stripAccents(input);
		return input + " ? ?";
	}

	public String morphTokenToNoun(String input) {
		// input = removeAfterSpostrophe(input);
		String lower = TurkishNLP.toLowerCase(input);
		if (lower.length() > 1) {

			List<MorphParse> parses = morphParser.parse(lower);
			if (!parses.isEmpty()) {
				String current = "";
				String currentMorph = "? ?";
				if (strategy.equals("shortest")) {
					current = lower;
				}
				for (MorphParse parse : parses) {

					String pos = parse.dictionaryItem.primaryPos.toString();
					String oflazer = parse.formatOflazer();
					String lemma = parse.getLemma();
					String morph = oflazer.substring(oflazer.indexOf('+') + 1);
					if (pos.equalsIgnoreCase("Verb")
							&& (lemma.endsWith("mak") || lemma.endsWith("mek"))) {
						lemma = lemma.substring(0, lemma.length() - 3);
					}
					if (morph.startsWith("Unk")) {
						return input + " ? ?";
					} else {
						if (strategy.equals("shortest")) {
							if (lemma.length() <= current.length()
									&& pos.equalsIgnoreCase("Noun")) {
								current = lemma;
								currentMorph = pos
										+ " "
										+ oflazer.substring(oflazer
												.indexOf('+') + 1);

							}
						} else {
							if (lemma.length() >= current.length()
									&& pos.equalsIgnoreCase("Noun")) {
								current = lemma;
								currentMorph = pos
										+ " "
										+ oflazer.substring(oflazer
												.indexOf('+') + 1);

							}
						}
					}

				}
				if (current.equals("")) {
					return input + " ? ?";
				}
				if (!isTurkish(current)) {
					current = StringUtils.stripAccents(current);
				}
				current = input.charAt(0) + current.substring(1);
				return current + " " + currentMorph;
			}

		}
		if (!isTurkish(input)) {
			input = StringUtils.stripAccents(input);
		}
		input = StringUtils.stripAccents(input);
		return input + " ? ?";
	}

	public String morphEntityName(String name) {
		Pattern p = Pattern.compile("[\"\\-,;:!?(){}\\[\\]<>%‘]");
		name = p.matcher(name).replaceAll(" ");
		name = name.replaceAll("\\s+", " ").trim();
		name = name.replaceAll("\\.", " ").trim();
		if (isTurkish(name)) {
			// name = name.split(",")[0];
			String[] tokens = name.split(" ");
			tokens[tokens.length - 1] = removeAfterSpostrophe(tokens[tokens.length - 1]);
			String[] morph;

			morph = morphTokenToAny(tokens[tokens.length - 1]).split(" ");

			// if (tokens.length == 1) {
			// morph = morphTokenToNoun(tokens[tokens.length - 1]).split(" ");
			// } else {
			// morph = morphTokenToAny(tokens[tokens.length - 1]).split(" ");
			// }

			tokens[tokens.length - 1] = morph[0];
			String result = "";
			for (String token : tokens) {
				result += token + " ";
			}
			result = TurkishNLP.toLowerCase(result);
			// LOGGER.info("normalizeEntityName result: " + result);
			return result.trim();
		} else {
			name = normalize(name);
			name = TurkishNLP.toLowerCase(name);
			// LOGGER.info("normalizeEntityName result: " + name);

			return name;
		}

	}

	public String morphPageContent(String content) {
		if (morphCache.containsKey(content)) {
			return morphCache.get(content);

		}
		// List<Token> tokens = morphSentenceToShortestNoun(content);
		// String result = "";
		// for (Token token : tokens) {
		// if (!TurkishNLP.isStopWord(token.getMorphText())) {
		// result += token.getMorphText() + " ";
		// }
		// }
		// LOGGER.info("normalizeEntityName result: " + result);
		// content = morphTokenToShortestNoun(content).split(" ")[0];
		String[] words = content.split(" ");
		String t = "";
		for (int i = 0; i < words.length; i++) {
			String result = words[i].replaceAll("&#39|\\*|\t|/|\"", "");
			result = normalize(result);
			if (result.length() > 1) {
				result = morphTokenToAny(result).split(" ")[0] + " ";
				result = result.toLowerCase(new Locale("tr", "TR"));
				// morphCache.put(content, result);
				t += result.trim() + " ";

			}
		}
		return t.trim();

		// if (content.matches(".*\\d.*")) {
		// return "";
		// }
		//
		// return normalize(content);

	}

	public static void saveRequest(String function, String input, String output) {
		try {
			MongoService mongo = ApplicationContextHolder.getContext().getBean(
					MongoService.class);
			MongoZemberekRepository zemberekRepo = (MongoZemberekRepository) mongo
					.getRepository("Zemberek");
			ZemberekAPIModel zemberek = new ZemberekAPIModel(function, input,
					output);
			zemberekRepo.save(zemberek);
			// itu = ituRepo.findByFunctionAndInput("tokenizer", "test");
			LOGGER.info("zemberek Request is saved");
		} catch (Exception e) {
			LOGGER.error("zemberek Request is not saved");
		}

	}

	public static String getRequest(String function, String input) {
		try {
			MongoService mongo = ApplicationContextHolder.getContext().getBean(
					MongoService.class);
			MongoZemberekRepository zemberekRepo = (MongoZemberekRepository) mongo
					.getRepository("Zemberek");

			ZemberekAPIModel zemberek = zemberekRepo.findByFunctionAndInput(
					function, input);
			if (zemberek != null) {
				return zemberek.getOutput();

			}
		} catch (Exception e) {
			LOGGER.error("zemberek mongo repo can not be searched");
		}
		return null;
	}

	public static void main(String[] args) {
		// shortest olmasi gerektigine ornek longest olunca derin oluyor deri
		// yerine
		Zemberek.getInstance().hasMorph("dilmektedir");
		List<Token> disambiguateFindTokens = Zemberek.getInstance()
				.disambiguateFindTokens("Ceylan'ın", false, false);
		LOGGER.info(Zemberek.getInstance().morphTokenToAny("Ceylan'ın"));

		try {
			Zemberek.getInstance().disambiguateOnly("terk etmek.");
		} catch (Exception e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		try {
			Zemberek.getInstance().disambiguate("hızlı yapmak", "hızlı", null);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		LOGGER.info(Zemberek.getInstance().morphEntityName(
				"vakıf üniversitesidir"));

		LOGGER.info(Zemberek.getInstance().morphEntityName("çalışan"));
		LOGGER.info(Zemberek.getInstance().morphEntityName("fenerbahçemize"));

		LOGGER.info(Zemberek.getInstance().morphEntityName(
				"Döllük, Mustafakemalpaşa"));
		LOGGER.info(Zemberek.getInstance().morphEntityName("istek vakfı"));
		isTurkish("Ara");

		LOGGER.info(Zemberek.getInstance().morphTokenToNoun("derinin"));

		LOGGER.info(Zemberek.getInstance().morphEntityName("sildim"));
		Zemberek.getInstance().morphTokenToNoun("dericilik");

		// Zemberek.getInstance().morphSentenceToLongest("Güney Slav halkıdır.");
		Zemberek.getInstance().morphEntityName("Türkiye Kupasını");
		Zemberek.getInstance().morphTokenToNoun("Ardıçalanı'nın");
		Zemberek.getInstance().morphTokenToNoun("Slovence");
		String text = removeAfterSpostrophe("Afrika'ya");

		Zemberek.getInstance().morphEntityName("verem");

		Zemberek.getInstance()
				.morphTokenToNoun(
						"Yeditepe Üniversitesi, İstanbul'da eğitim veren, İstanbul Eğitim ve Kültür Vakfı (İSTEK Vakfı) tarafından 4142 sayılı yasa ile 1996 yılında kurulan, Yüksek Öğretim Yasası çerçevesinde kamu tüzel kişiliği, mali ve idari özerkliği olan bir vakıf üniversitesidir.");
		Zemberek.getInstance().morphEntityName(
				" Sovyetler Birliği'nin dağılmasının");
		Zemberek.getInstance().morphEntityName("Yeditepe Üniversitesi");

		Zemberek.getInstance().morphEntityName("Реингольд");
		Zemberek.getInstance().morphEntityName("Ben, Kendim ve Sevgilim");
		Zemberek.getInstance().morphEntityName("Bouvard ile Pécuchet");
		Zemberek.getInstance().morphEntityName(
				"İstanbul Eğitim ve Kültür Vakfı (İstek Vakfı)");
		Zemberek.getInstance().morphEntityName("Doğan Holding");
		Zemberek.getInstance().morphEntityName(
				"(Agen) - La Garenne Hava Meydanı");
		Zemberek.getInstance().morphEntityName("vakıf üniversitesidir");
	}

	public String disambiguate(String sentence) throws Exception {
		SentenceMorphParse sentenceParse = sentenceParser.parse(sentence);
		sentenceParser.disambiguate(sentenceParse);
		StringBuffer sb = new StringBuffer("");
		for (SentenceMorphParse.Entry entry : sentenceParse) {
			try {
				if (entry.input.length() > 1) {
					String lemma = entry.parses.get(0).getLemma();
					if (!lemma.equals("UNK")) {
						sb.append(lemma + " ");
					} else {
						sb.append(entry.input + " ");
					}
				}
			} catch (Exception e) {
				// TODO: handle exception
			}
		}
		return normalize(sb.toString());

	}

	public void disambiguate(String sentence, String title, Features features)
			throws Exception {
		SentenceMorphParse sentenceParse = sentenceParser.parse(sentence);
		sentenceParser.disambiguate(sentenceParse);
		for (SentenceMorphParse.Entry entry : sentenceParse) {
			try {
				if (entry.input.length() > 1) {
					String lemma = entry.parses.get(0).getLemma();
					if (!lemma.equals("UNK")) {

						if (StringUtils.containsIgnoreCase(title, entry.input)) {
							List<Suffix> suffixes = entry.parses.get(0)
									.getSuffixes();
							for (Suffix suffix : suffixes) {
								features.getSuffixes().add(suffix.toString());
							}

						} else {

							if (entry.parses.get(0).getPos().toString()
									.equals("Noun")) {
								features.getNouns().add(lemma);
							} else if (entry.parses.get(0).getPos().toString()
									.equals("Verb")) {
								features.getVerbs().add(lemma);
							} else if (entry.parses.get(0).getPos().toString()
									.equals("Adjective")) {

								features.getAdjs().add(lemma);
							} else {
								// LOGGER.info(lemma
								// + "-"
								// + entry.parses.get(0).getPos()
								// .toString());
							}
						}
					}
				}
			} catch (Exception e) {
				// TODO: handle exception
			}
		}

	}

	public Set<String> getSuffix(String sentence, String title)
			throws Exception {
		// if (title.equals("üyelik")) {
		// LOGGER.info("");
		// }
		Set<String> suffixSet = new HashSet<String>();
		try {

			String[] t = title.split(" ");
			title = t[t.length - 1];
			SentenceMorphParse sentenceParse = sentenceParser.parse(sentence);
			sentenceParser.disambiguate(sentenceParse);
			for (SentenceMorphParse.Entry entry : sentenceParse) {
				try {
					if (entry.input.length() > 1) {
						String lemma = entry.parses.get(0).getLemma();
						if (!lemma.equals("UNK")) {

							if (StringUtils.containsIgnoreCase(title,
									entry.input)
									|| StringUtils.containsIgnoreCase(
											entry.input, title)) {
								List<Suffix> suffixes = entry.parses.get(0)
										.getSuffixes();
								for (Suffix suffix : suffixes) {
									suffixSet.add(suffix.toString());
								}
								return suffixSet;

							}
						}
					}
				} catch (Exception e) {
					// TODO: handle exception
				}
			}

		} catch (Exception e) {
			// TODO: handle exception
		}
		return suffixSet;
	}

	public void disambiguateOnly(String sentence) throws Exception {
		SentenceMorphParse sentenceParse = sentenceParser.parse(sentence);
		sentenceParser.disambiguate(sentenceParse);
		writeParseResult(sentenceParse);
	}

	public String disambiguateNormalize(String sentence) throws Exception {
		sentence = normalize(sentence);
		SentenceMorphParse sentenceParse = sentenceParser.parse(sentence);
		sentenceParser.disambiguate(sentenceParse);
		StringBuffer sb = new StringBuffer("");
		for (SentenceMorphParse.Entry entry : sentenceParse) {
			try {
				if (entry.input.length() > 1) {
					String lemma = entry.parses.get(0).getLemma();
					if (!lemma.equals("UNK")) {
						sb.append(lemma + " ");
					} else {
						sb.append(entry.input + " ");
					}
				}
			} catch (Exception e) {
				// TODO: handle exception
			}
		}
		return sb.toString().trim();
	}

	public String getEntityLemma(String sentence) throws Exception {
		try {

			String tokens[] = sentence.split(" ");
			SentenceMorphParse sentenceParse = sentenceParser
					.parse(tokens[tokens.length - 1]);
			sentenceParser.disambiguate(sentenceParse);
			StringBuffer sb = new StringBuffer("");
			for (int i = 0; i < tokens.length - 1; i++) {
				sb.append(tokens[i] + " ");
			}
			for (SentenceMorphParse.Entry entry : sentenceParse) {
				try {
					if (entry.input.length() > 1) {
						String lemma = entry.parses.get(0).getLemma();
						if (!lemma.equals("UNK")) {
							sb.append(lemma + " ");
						} else {
							sb.append(entry.input + " ");
						}
					}
				} catch (Exception e) {
					// TODO: handle exception
				}
			}
			return sb.toString().trim();
		} catch (Exception e) {
			// TODO: handle exception
		}
		return sentence;
	}

	private void writeParseResult(SentenceMorphParse sentenceParse) {
		for (SentenceMorphParse.Entry entry : sentenceParse) {
			System.out.println("Word = " + entry.input);
			for (MorphParse parse : entry.parses) {
				System.out.println(parse.formatLong());
				// parse.dictionaryItem.primaryPos
				break;
			}
		}
	}

	public List<Set<String>> disambiguateForEmbedding(String sentence) {
		List<Set<String>> words = new ArrayList<Set<String>>();
		Set<String> nouns = new HashSet();
		Set<String> verbs = new HashSet();
		Set<String> others = new HashSet();
		words.add(nouns);
		words.add(verbs);
		words.add(others);
		try {
			sentence = normalize(sentence);
			SentenceMorphParse sentenceParse = sentenceParser.parse(sentence);
			sentenceParser.disambiguate(sentenceParse);
			for (SentenceMorphParse.Entry entry : sentenceParse) {

				if (entry.input.length() > 1) {
					String lemma = entry.parses.get(0).getLemma();
					if (!lemma.equals("UNK")) {
						if (entry.parses.get(0).getPos().toString()
								.equals("Noun")) {
							nouns.add(lemma);
						} else if (entry.parses.get(0).getPos().toString()
								.equals("Verb")) {
							verbs.add(lemma);
						} else if (entry.parses.get(0).getPos().toString()
								.equals("Adjective")) {

							others.add(lemma);
						}
					} else if (entry.parses.get(0).getPos().toString()
							.equals("Adverb")) {
						others.add(lemma);
					}

					else {
						// LOGGER.info(lemma + "-"
						// + entry.parses.get(0).getPos().toString());
					}
				}

			}
		} catch (Exception e) {
			// LOGGER.info(e);
		}
		return words;
	}

	public String[] tokenize(String input) {
		try {

			String tokens[] = tokenizer.tokenize(input);
			return tokens;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
}
