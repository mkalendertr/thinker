package edu.yeditepe.discovery;

import info.bliki.wiki.dump.IArticleFilter;
import info.bliki.wiki.dump.Siteinfo;
import info.bliki.wiki.dump.WikiArticle;
import info.bliki.wiki.dump.WikiXMLParser;
import info.bliki.wiki.filter.PlainTextConverter;
import info.bliki.wiki.model.WikiModel;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
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
import org.encog.engine.network.activation.ActivationLinear;
import org.encog.engine.network.activation.ActivationSoftMax;
import org.encog.ml.data.MLDataSet;
import org.encog.ml.data.basic.BasicMLData;
import org.encog.ml.data.basic.BasicMLDataSet;
import org.encog.neural.networks.BasicNetwork;
import org.encog.neural.networks.layers.BasicLayer;
import org.encog.neural.networks.training.propagation.resilient.ResilientPropagation;
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
import edu.yeditepe.deep.autoencoder.AutoEncoder;
import edu.yeditepe.nlp.TurkishNLP;
import edu.yeditepe.nlp.Zemberek;
import edu.yeditepe.utils.FileUtils;
import edu.yeditepe.utils.Property;

public class PrepareTypeClassifierTrainingData {
	private static final Logger LOGGER = Logger
			.getLogger(PrepareTypeClassifierTrainingData.class);

	private static BufferedWriter bw;
	private static Map<String, String> typesMap;
	private static Set<String> filteredTypes;
	private static Map<String, Features> featuresMap = new HashMap<String, Features>();
	private static Map<String, Integer> typesIndex = new HashMap<String, Integer>();
	private static Map<Integer, String> typesIndexInverse = new HashMap<Integer, String>();

	private static List<String> suffixes;

	private static Map<Integer, List<MyFeature[]>> data = new HashMap<Integer, List<MyFeature[]>>();
	private static Map<Integer, List<MyFeature[]>> trainingData = new HashMap<Integer, List<MyFeature[]>>();
	private static Map<Integer, List<MyFeature[]>> testingData = new HashMap<Integer, List<MyFeature[]>>();

	private static Map<Integer, List<double[]>> dataAutoEncoder = new HashMap<Integer, List<double[]>>();

	private static LinkedTreeMap<String, double[]> vectors;

	private static int vectorSize = 50;
	private static int suffixSize = 64;
	private static int featuresSize = 214;

	private static int classNum = 100;

	public static boolean autoencoder = true;

	public static boolean softmax = false;

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

	public static void main(String[] args) throws Exception {
		Reader reader = new FileReader("type.json");
		Gson gson = new GsonBuilder().create();
		typesMap = gson.fromJson(reader, Map.class);
		loadTypes();
		// HashMap<String, String> typesMapTitle = new HashMap<String,
		// String>();
		// for (String id : typesMap.keySet()) {
		// String title = MYSQL.getTRTitleById(id);
		// typesMapTitle.put(title, typesMap.get(id));
		// }
		// Writer writer;
		// try {
		// writer = new FileWriter("disfeatures.json");
		// gson = new GsonBuilder().create();
		// gson.toJson(typesMapTitle, writer);
		//
		// writer.close();
		// } catch (IOException e) {
		// // TODO Auto-generated catch block
		// e.printStackTrace();
		// }

		suffixes = FileUtils.readFile("suffix.txt");
		// prepareTypeClassifierData();
		// saveFeatures();
		reader = new FileReader("discovery_model.json");
		gson = new GsonBuilder().create();
		vectors = new Gson().fromJson(reader,
				new TypeToken<LinkedTreeMap<String, double[]>>() {
				}.getType());

		createTrainingFile();
	}

	private static void saveFeatures() {
		Writer writer;
		try {
			writer = new FileWriter("disfeatures.json");
			Gson gson = new GsonBuilder().create();
			gson.toJson(featuresMap, writer);

			writer.close();
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
		if (autoencoder) {
			AutoEncoder encoder = new AutoEncoder();
			for (String key : featuresMap.keySet()) {
				Features f = featuresMap.get(key);
				double[] v1 = getAverageVector(f.getTitle().split(" "));
				double[] v2 = getAverageVector(f.getNouns().toArray());
				double[] v3 = getAverageVector(f.getVerbs().toArray());
				double[] v4 = getSuffixVector(f.getSuffixes());
				if (v1 != null && v2 != null && v3 != null && v4 != null) {
					double[] vector = merge(v1, v2, v3);
					int y = typesIndex.get(typesMap.get(f.getId()));
					encoder.addData(vector);
					if (dataAutoEncoder.containsKey(y)) {
						dataAutoEncoder.get(y).add(vector);
					} else {
						List<double[]> list = new ArrayList<double[]>();
						list.add(vector);
						dataAutoEncoder.put(y, list);
					}
				}
			}
			featuresSize = 150;
			encoder.addLayer(new ActivationLinear(), featuresSize);
			// encoder.addLayer(new ActivationTANH(), 50);
			BasicNetwork network = encoder.getNetwork(1);
			for (Integer type : dataAutoEncoder.keySet()) {
				List<MyFeature[]> list = new ArrayList<MyFeature[]>();
				data.put(type, list);
				List<double[]> x = dataAutoEncoder.get(type);
				for (double[] vector : x) {
					double[] output = new double[featuresSize];
					network.compute(vector, output);
					MyFeature[] features = new MyFeature[featuresSize];
					for (int i = 0; i < features.length; i++) {
						features[i] = new MyFeature(i + 1, output[i], "");
					}
					list.add(features);
				}

			}
			// dataAutoEncoder.clear();
		} else {
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
						features[index - 1] = new MyFeature(index, d,
								f.getTitle());
						index++;
					}
					for (double d : v2) {
						features[index - 1] = new MyFeature(index, d,
								f.getTitle());
						index++;
					}
					for (double d : v3) {
						features[index - 1] = new MyFeature(index, d,
								f.getTitle());
						index++;
					}
					for (double d : v4) {
						features[index - 1] = new MyFeature(index, d,
								f.getTitle());
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

		if (softmax) {
			BasicNetwork network = new BasicNetwork();
			network.addLayer(new BasicLayer(new ActivationLinear(), true,
					featuresSize));
			// network.addLayer(new BasicLayer(new ActivationLinear(), true,
			// 150));
			network.addLayer(new BasicLayer(new ActivationSoftMax(), true, 100));
			// network.addLayer(new BasicLayer(new ActivationTANH(), true, 75));
			network.getStructure().finalizeStructure();
			network.setBiasActivation(0.1);
			network.reset();

			double[][] input = new double[trainingSize][featuresSize];
			double[][] output = new double[trainingSize][classNum];
			int i = 0;
			for (Integer type : trainingData.keySet()) {
				List<MyFeature[]> td = trainingData.get(type);
				for (MyFeature[] features2 : td) {
					for (int j = 0; j < features2.length; j++) {
						input[i][j] = features2[j].getValue();
					}
					output[i] = getSoftMaxOutput(type);
					i++;
				}
			}
			MLDataSet trainingSet = new BasicMLDataSet(input, output);
			ResilientPropagation train = new ResilientPropagation(network,
					trainingSet);

			train.iteration(1000);
			int trueNum = 0;
			int falseNum = 0;
			for (Integer type : testingData.keySet()) {
				List<MyFeature[]> td = testingData.get(type);
				for (MyFeature[] features2 : td) {
					double[] tinput = new double[featuresSize];
					for (int j = 0; j < features2.length; j++) {
						tinput[j] = features2[j].getValue();
					}
					BasicMLData d1 = new BasicMLData(tinput);
					double[] e1 = network.compute(d1).getData();
					int result = getMaxIndex(e1);
					if (result == type) {
						trueNum++;
					} else {
						falseNum++;
					}
				}
			}
			float precision = (float) trueNum * 100 / (trueNum + falseNum);
			float recall = (float) trueNum * 100 / (trainingSize);
			float fmeasure = 2 * precision * recall / (precision + recall);
			String s = "Total  correct\t" + trueNum + " Incorrect\t" + falseNum
					+ "\tPrecision\t" + precision + "\tRecall\t" + recall
					+ "\tF-measure\t" + fmeasure;
			LOGGER.info(s);

		} else {

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
							double prediction = Linear.predictProbability(
									model, features2, probablity);
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
							double prediction = Linear
									.predict(model, features2);
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

	public static void prepareTypeClassifierData() {
		try {

			File outFile = new File("discovery_classifier_training.txt");

			// if file doesnt exists, then create it
			if (!outFile.exists()) {
				outFile.createNewFile();
			}

			FileWriter fw = new FileWriter(outFile.getAbsoluteFile(), false);
			bw = new BufferedWriter(fw);

			IArticleFilter handler = new DemoArticleFilter();
			WikiXMLParser wxp = new WikiXMLParser(Property.getInstance().get(
					"wiki.dump"), handler);
			wxp.parse();

			bw.close();
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
			Features features = new Features(id, page.getTitle());
			try {
				if (typesMap.get(id) == null
						|| !filteredTypes.contains(typesMap.get(id))) {
					return;
				}
			} catch (Exception e) {
				// TODO: handle exception
			}

			if (page != null && page.getText() != null
					&& !page.getText().startsWith("#REDIRECT ")) {

				PrintStream out = null;

				try {
					out = new PrintStream(System.out, true, "UTF-8");
				} catch (UnsupportedEncodingException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

				String wikiText = page
						.getText()
						.replaceAll("[=]+[A-Za-z+\\s-]+[=]+", " ")
						.replaceAll("\\{\\{[A-Za-z0-9+\\s-]+\\}\\}", " ")
						.replaceAll("(?m)<ref>.+</ref>", " ")
						.replaceAll(
								"(?m)<ref name=\"[A-Za-z0-9\\s-]+\">.+</ref>",
								" ").replaceAll("<ref>", " <ref>");

				// Remove text inside {{ }}
				String plainStr = wikiModel.render(new PlainTextConverter(),
						wikiText).replaceAll("\\{\\{[A-Za-z+\\s-]+\\}\\}", " ");
				Matcher regexMatcher = regex.matcher(plainStr);
				String title = TurkishNLP.toLowerCase(page.getTitle());
				features.setUppercase(isUpperCase(page.getTitle(), wikiText));
				while (regexMatcher.find()) {
					// Get sentences with 6 or more words
					String sentence = regexMatcher.group();

					if (matchSpaces(sentence, 5)
							&& StringUtils.contains(
									TurkishNLP.toLowerCase(sentence),
									title.split(" ")[0] + " ")) {
						try {
							Zemberek.getInstance().disambiguate(sentence,
									title, features);
							// bw.write(dis);
						} catch (Exception e) {
							// TODO Auto-generated catch block
							// e.printStackTrace();
						}
					}
				}
				featuresMap.put(id, features);

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

	final static public double[] merge(final double[]... arrays) {
		int size = 0;
		for (double[] a : arrays)
			size += a.length;

		double[] res = new double[size];

		int destPos = 0;
		for (int i = 0; i < arrays.length; i++) {
			if (i > 0)
				destPos += arrays[i - 1].length;
			int length = arrays[i].length;
			System.arraycopy(arrays[i], 0, res, destPos, length);
		}

		return res;
	}

	private static double[] getSoftMaxOutput(int index) {
		double[] vector = new double[classNum];
		vector[index] = 1;

		return vector;
	}

	public static int getMaxIndex(double[] input) {
		int maxIndex = 0;
		double max = Double.MIN_VALUE;
		for (int i = 0; i < input.length; i++) {
			if (input[i] > max) {
				max = input[i];
				maxIndex = i;
			}
		}
		return maxIndex;
	}
}
