package edu.yeditepe.discovery;

import info.bliki.wiki.dump.IArticleFilter;
import info.bliki.wiki.dump.Siteinfo;
import info.bliki.wiki.dump.WikiArticle;
import info.bliki.wiki.dump.WikiXMLParser;
import info.bliki.wiki.filter.PlainTextConverter;
import info.bliki.wiki.model.WikiModel;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.xml.sax.SAXException;

import com.google.common.collect.TreeMultiset;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.internal.LinkedTreeMap;
import com.google.gson.reflect.TypeToken;

import de.bwaldvogel.liblinear.Feature;
import de.bwaldvogel.liblinear.Linear;
import de.bwaldvogel.liblinear.Model;
import de.bwaldvogel.liblinear.Parameter;
import de.bwaldvogel.liblinear.Problem;
import de.bwaldvogel.liblinear.SolverType;
import edu.yeditepe.nlp.OpenNLP;
import edu.yeditepe.nlp.TurkishNLP;
import edu.yeditepe.nlp.Zemberek;
import edu.yeditepe.utils.FileUtils;

public class PrepareTypeClassifierTrainingDataEn {
	private static final Logger LOGGER = Logger
			.getLogger(PrepareTypeClassifierTrainingDataEn.class);

	private static BufferedWriter bw;
	private static Map<String, String> typesMap;
	private static Set<String> filteredTypes;
	private static Map<String, Features> featuresMap = new HashMap<String, Features>();
	private static Map<String, EnFeatures> enfeaturesMap = new HashMap<String, EnFeatures>();

	private static Map<String, Integer> typesIndex = new HashMap<String, Integer>();
	private static Map<Integer, String> typesIndexInverse = new HashMap<Integer, String>();

	private static List<String> suffixes;

	private static Map<Integer, List<MyFeature[]>> data = new HashMap<Integer, List<MyFeature[]>>();
	private static Map<Integer, List<MyFeature[]>> trainingData = new HashMap<Integer, List<MyFeature[]>>();
	private static Map<Integer, List<MyFeature[]>> testingData = new HashMap<Integer, List<MyFeature[]>>();

	private static LinkedTreeMap<String, double[]> vectors;

	public static int vectorSize = 300;
	private static int suffixSize = 64;
	private static int featuresSize = 214;

	private static int fileCounter = 0;

	public static void main(String[] args) throws Exception {
		vectors = loadGloveVector("C:\\wikipedia\\glove.6B.300d.txt");
		prepareTypeClassifierData("C:\\wikipedia\\enwiki-latest-pages-articles.xml.bz2");
		saveFeatures();
		// createTrainingFile();
	}

	public static void getTypes() throws Exception {
		Reader reader = new FileReader("type.json");
		Gson gson = new GsonBuilder().create();
		Map<String, String> t = gson.fromJson(reader, Map.class);

		TreeSet<String> typestr = new TreeSet<String>();
		TreeMultiset<String> all = TreeMultiset.create();
		HashSet<String> printed = new HashSet<String>();
		TreeMultiset<String> trSet = TreeMultiset.create();
		for (String p : t.values()) {
			p = TurkishNLP.toLowerCase(p);
			typestr.add(p);
			trSet.add(p);

		}
		for (String s : trSet) {
			int count = trSet.count(s);
			if (count < 40) {
				trSet.remove(s);
				typestr.remove(s);
			} else {
				all.add(s);
			}
		}
		for (String s : all) {
			if (!printed.contains(s)) {
				System.out.println(s + "\t" + all.count(s));
				printed.add(s);
			}
		}
		LOGGER.info(Zemberek.getInstance().disambiguate(
				"Ali topu at. Murat kizlari kovala."));

	}

	public static void loadTypes() {
		filteredTypes = FileUtils.readFileSet("filtered_types.txt");
		int index = 0;
		for (String t : filteredTypes) {
			typesIndex.put(t, index);
			typesIndexInverse.put(index, t);
			index++;
		}
	}

	private static void saveFeatures() {
		Writer writer;
		try {
			writer = new FileWriter(fileCounter++ + "disfeatures_en.json");
			Gson gson = new GsonBuilder().create();
			gson.toJson(enfeaturesMap, writer);

			writer.close();
			enfeaturesMap.clear();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private static void createTrainingFile() throws IOException {
		Reader reader = new FileReader("disfeatures.json");
		Gson gson = new GsonBuilder().create();
		featuresMap = new Gson().fromJson(reader,
				new TypeToken<Map<String, Features>>() {
				}.getType());

		// double[] y = new double[sampleSize];
		// Feature[][] x = new Feature[sampleSize][];

		for (String key : featuresMap.keySet()) {
			Features f = featuresMap.get(key);
			MyFeature[] features = new MyFeature[featuresSize];
			double[] v1 = getAverageVector(f.getTitle().split(" "));
			double[] v2 = getAverageVector(f.getNouns().toArray());
			double[] v3 = getAverageVector(f.getVerbs().toArray());
			double[] v4 = getSuffixVector(f.getSuffixes());
			if (v1 != null && v2 != null && v3 != null && v4 != null) {
				int y = typesIndex.get(typesMap.get(f.getId()));
				int index = 1;
				for (double d : v1) {
					features[index - 1] = new MyFeature(index, d, f.getTitle());
					index++;
				}
				for (double d : v2) {
					features[index - 1] = new MyFeature(index, d, f.getTitle());
					index++;
				}
				for (double d : v3) {
					features[index - 1] = new MyFeature(index, d, f.getTitle());
					index++;
				}
				for (double d : v4) {
					features[index - 1] = new MyFeature(index, d, f.getTitle());
					index++;
				}

				if (data.containsKey(y)) {
					data.get(y).add(features);
				} else {
					List<MyFeature[]> list = new ArrayList<MyFeature[]>();
					list.add(features);
					data.put(y, list);
				}
			}
		}
		int trainingSize = 0;
		int testingSize = 0;
		int counter = 0;
		for (Integer type : data.keySet()) {
			List<MyFeature[]> x = data.get(type);
			Collections.shuffle(x);
			int split = x.size() * 7 / 10;
			if (x.size() > 0) {
				List<MyFeature[]> train = x.subList(0, split);
				List<MyFeature[]> test = x.subList(split, x.size());
				trainingData.put(type, train);
				testingData.put(type, test);
				trainingSize += train.size();
				testingSize += test.size();
				System.out.println(typesIndexInverse.get(counter) + " "
						+ x.size() + " " + train.size() + " " + test.size());
				counter++;
			}
		}

		double[] yTraining = new double[trainingSize];
		Feature[][] xTraining = new Feature[trainingSize][];
		int index = 0;
		for (Integer type : trainingData.keySet()) {
			List<MyFeature[]> td = trainingData.get(type);
			for (MyFeature[] features2 : td) {
				xTraining[index] = features2;
				yTraining[index] = type;
				index++;
			}
		}
		Problem problem = new Problem();
		problem.l = trainingSize;
		problem.n = featuresSize;
		problem.x = xTraining;
		problem.y = yTraining;

		SolverType solver = SolverType.L2R_LR; // -s 0
		double C = 1.0; // cost of constraints violation
		double eps = 0.01; // stopping criteria

		Parameter parameter = new Parameter(solver, C, eps);
		Model model = Linear.train(problem, parameter);
		File modelFile = new File("discoveryLiblinear.model");
		model.save(modelFile);

		boolean predictProb = true;
		for (float threshold = 0; threshold <= 0; threshold += 0.1) {
			counter = 0;
			int trueNum = 0;
			int falseNum = 0;
			double[] probablity = new double[100];

			for (Integer type : testingData.keySet()) {
				List<MyFeature[]> td = testingData.get(type);
				for (MyFeature[] features2 : td) {
					if (predictProb) {
						double prediction = Linear.predictProbability(model,
								features2, probablity);
						if (probablity[(int) prediction] >= threshold) {

							if (prediction == type) {
								trueNum++;
							} else {
								falseNum++;
								LOGGER.info(features2[0].getTitle()
										+ "\t Correct\t"
										+ typesIndexInverse.get(type)
										+ "\tPrediction\t"
										+ typesIndexInverse
												.get((int) prediction));
							}
						}
					} else {
						double prediction = Linear.predict(model, features2);
						if (prediction == type) {
							trueNum++;
						} else {
							falseNum++;
						}
					}
					counter++;

				}
			}
			float precision = (float) trueNum * 100 / (trueNum + falseNum);
			float recall = (float) trueNum * 100 / (counter);
			float fmeasure = 2 * precision * recall / (precision + recall);
			String s = "Threshold\t" + threshold + " Total  correct\t"
					+ trueNum + " Incorrect\t" + falseNum + "\tPrecision\t"
					+ precision + "\tRecall\t" + recall + "\tF-measure\t"
					+ fmeasure;
			LOGGER.info(s);
		}
	}

	private static double[] getSuffixVector(Set<String> s) {
		double[] vector = new double[suffixSize];
		int i = 0;
		for (String suffix : suffixes) {
			if (s.contains(suffix)) {
				vector[i++] = 1;
			} else {
				vector[i++] = 0;
			}
		}
		return vector;
	}

	private static double[] getAverageVector(Object[] objects) {
		double[] avg = new double[vectorSize];
		List<double[]> vectors = new ArrayList<double[]>();
		for (int i = 0; i < objects.length; i++) {
			double[] wordVector = getWordVector((String) objects[i]);
			if (wordVector != null) {
				vectors.add(wordVector);
			}
		}
		if (!vectors.isEmpty()) {
			for (int i = 0; i < avg.length; i++) {
				for (int j = 0; j < vectors.size(); j++) {
					avg[i] += (double) vectors.get(j)[i];
				}
				avg[i] /= vectors.size();
			}
			return avg;
		} else {
			return null;
		}

	}

	private static double[] getWordVector(String word) {
		double[] rawVector = null;
		rawVector = vectors.get(word);

		if (rawVector == null) {
			rawVector = vectors.get(TurkishNLP.toLowerCase(word));
		}
		if (rawVector != null) {
			return rawVector;
		}
		return null;
		// TODO Auto-generated method stub

	}

	public static void prepareTypeClassifierData(String bz2Filename) {
		try {

			try {
				IArticleFilter handler = new DemoArticleFilter();
				WikiXMLParser wxp = new WikiXMLParser(bz2Filename, handler);
				wxp.parse();
			} catch (Exception e) {
				e.printStackTrace();
			}
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

		public void process(WikiArticle page, Siteinfo siteinfo)
				throws SAXException {

			String id = page.getId();
			String url = page.getTitle();

			if (page != null && page.getText() != null
					&& !page.getText().startsWith("#REDIRECT ")
					&& page.getText().contains("{{Infobox ")) {

				String wikiText = page
						.getText()
						.replaceAll("[=]+[A-Za-z+\\s-]+[=]+", " ")
						.replaceAll("\\{\\{[A-Za-z0-9+\\s-]+\\}\\}", " ")
						.replaceAll("(?m)<ref>.+</ref>", " ")
						.replaceAll(
								"(?m)<ref name=\"[A-Za-z0-9\\s-]+\">.+</ref>",
								" ").replaceAll("<ref>", " <ref>");
				int start = wikiText.indexOf("{{Infobox ");
				String infotext = wikiText.substring(start + 10);
				int end = infotext.indexOf('\n');
				String type = infotext.substring(0, end).toLowerCase();
				type = type.split("<|\\|")[0];
				if (type == null) {
					LOGGER.info(infotext);
				} else {
					EnFeatures features = new EnFeatures(url, type);

					// LOGGER.info(type);
					// Remove text inside {{ }}
					String plainStr = wikiModel.render(
							new PlainTextConverter(), wikiText).replaceAll(
							"\\{\\{[A-Za-z+\\s-]+\\}\\}", " ");
					Matcher regexMatcher = regex.matcher(plainStr);
					String title = TurkishNLP.toLowerCase(page.getTitle());
					// features.setUppercase(isUpperCase(page.getTitle(),
					// wikiText));
					while (regexMatcher.find()) {
						// Get sentences with 6 or more words
						String sentence = regexMatcher.group();

						if (matchSpaces(sentence, 4)
								&& StringUtils.contains(
										TurkishNLP.toLowerCase(sentence),
										title.split(" ")[0] + " ")) {
							try {
								OpenNLP.getInstance().parse(sentence, features,
										vectors);
								enfeaturesMap.put(title, features);
								if (enfeaturesMap.size() > 100000) {
									saveFeatures();
								}
							} catch (Exception e) {
								// TODO Auto-generated catch block
								// e.printStackTrace();
							}
						}
					}
					// featuresMap.put(id, features);
				}
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

	public static String wikiUrlToStringLowerCase(String url) {
		url = url.toLowerCase(new Locale("tr", "TR"));
		url = url.replaceAll("_", " ");
		if (url.contains("(")) {
			url = url.substring(0, url.indexOf("(")).trim();
		}

		return url;
	}

	public static LinkedTreeMap<String, double[]> loadGloveVector(
			String fileName) {
		LinkedTreeMap<String, double[]> vectors = new LinkedTreeMap<String, double[]>();

		try {
			FileInputStream fstream = new FileInputStream(fileName);
			BufferedReader br = new BufferedReader(new InputStreamReader(
					fstream));

			String strLine;
			while ((strLine = br.readLine()) != null) {
				String[] line = strLine.split(" ");
				String word = line[0];
				double[] vector = new double[line.length - 1];
				for (int i = 0; i < vector.length; i++) {
					vector[i] = Double.parseDouble(line[i + 1]);
				}
				vectors.put(word, vector);
			}

			// Close the input stream
			br.close();

		} catch (Exception e) {
			// TODO: handle exception
		}
		return vectors;
	}
}
