package edu.yeditepe.deep;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.Reader;
import java.io.Writer;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.apache.lucene.document.Document;

import vectorspace.CosineSimilarity;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import com.google.common.collect.Multisets;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import edu.yeditepe.lucene.EntitySearchEngine;
import edu.yeditepe.nlp.TurkishNLP;
import edu.yeditepe.repository.MYSQL;
import edu.yeditepe.utils.DisambiguationPages2;
import edu.yeditepe.utils.FileUtils;
import edu.yeditepe.utils.Property;

public class PrepareData {
	private static final Logger LOGGER = Logger.getLogger(PrepareData.class);
	private static int hashsize = 2;
	private static int hashVectorSize = 20000;

	public static void prepareProperty() throws FileNotFoundException {
		List<String> et = MYSQL.getPageTitles();
		Set<String> entityTitles = new HashSet<String>();
		entityTitles.addAll(et);
		HashMap<String, String> trtitles = MYSQL.getTRTitles();
		Map<String, HashSet<String>> entityWords = new HashMap<String, HashSet<String>>();
		HashSet<String> allWords = new HashSet<String>();
		// add title information
		for (String title : entityTitles) {
			HashSet<String> titleWords = extractDistinctWords(title);
			entityWords.put(title, titleWords);
			allWords.addAll(titleWords);

		}
		// add metadata information
		List<String> lines = FileUtils
				.readFile("C:\\wikipedia_turkish\\dbpedia\\mappingbased_properties_en_uris_tr.nt");

		for (String line : lines) {
			try {
				String t[] = line.split(" ");
				String title = t[0].substring(t[0].lastIndexOf('/') + 1)
						.replaceAll(">", "");
				if (!entityTitles.contains(title)) {
					continue;
				}
				String p = t[1];
				p = TurkishNLP.toLowerCase(p);
				p = p.substring(p.lastIndexOf('/') + 1, p.length() - 1)
						.replace("_", " ");
				HashSet<String> metadataWords = extractDistinctWords(p);
				if (line.contains("\"")) {
					String o = line.substring(line.indexOf('"') + 1,
							line.lastIndexOf('"'));
					byte[] utf8 = o.getBytes("UTF-8");
					String o2 = new String(utf8, "UTF-8");
					o2 = TurkishNLP.toLowerCase(o2);
					if (!o2.contains("\\u") && !o2.contains("http")) {
						metadataWords.addAll(extractDistinctWords(o2));
					}
				} else {
					p = TurkishNLP.toLowerCase(line.split(" ")[2]);
					p = p.substring(p.lastIndexOf('/') + 1, p.length() - 1)
							.replace("_", " ");
					if (!p.contains("\\u")) {
						metadataWords.addAll(extractDistinctWords(p));
					}
				}
				if (entityWords.containsKey(title)) {
					entityWords.get(title).addAll(metadataWords);
				} else {
					entityWords.put(title, metadataWords);
				}
				allWords.addAll(metadataWords);

			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		// add type information
		lines = FileUtils
				.readFile("C:\\wikipedia_turkish\\dbpedia\\instance_types_en_uris_tr.nt");
		for (String line : lines) {
			String t[] = line.split(" ");
			String title = t[0].substring(t[0].lastIndexOf('/') + 1)
					.replaceAll(">", "");
			title = trtitles.get(title);
			if (title == null || !entityTitles.contains(title)) {
				continue;
			}

			String p = t[2];
			p = TurkishNLP.toLowerCase(p);
			p = p.substring(p.lastIndexOf('/') + 1, p.length() - 1).replace(
					"_", " ");
			if (!p.contains("\\u")) {
				HashSet<String> typeWords = extractDistinctWords(p);
				allWords.addAll(typeWords);

				if (entityWords.containsKey(title)) {
					entityWords.get(title).addAll(typeWords);
				} else {
					entityWords.put(title, typeWords);
				}
			}
		}

		List<String> list = MYSQL.getCategories();
		for (String cat : list) {
			String[] category = cat.split("\t");
			String title = category[0];
			if (title == null || !entityTitles.contains(title)) {
				continue;
			}
			HashSet<String> categoryWords = extractDistinctWords(category[1]);
			allWords.addAll(categoryWords);

			if (entityWords.containsKey(title)) {
				entityWords.get(title).addAll(categoryWords);
			} else {
				entityWords.put(title, categoryWords);
			}
		}

		try {
			Multiset<String> multiset = HashMultiset.create();
			HashMap<String, Integer> gramSet = new HashMap<String, Integer>();
			HashMap<String, HashSet<String>> wordHash = new HashMap<String, HashSet<String>>();
			int counter = 0;
			LOGGER.info("Distinct words size: " + allWords.size());
			for (String word : allWords) {
				HashSet<String> wordGramList = new HashSet<String>();
				wordHash.put(word, wordGramList);
				for (int i = 0; i < word.length() - hashsize + 1; i++) {
					String e = word.substring(i, i + hashsize);
					if (!gramSet.containsKey(e)) {
						gramSet.put(e, counter++);
						// LOGGER.info(e);
					}
					multiset.add(e);
					wordGramList.add(e);
				}
			}
			LOGGER.info("size after hashing " + gramSet.size());

			Map<String, Integer> topGrams = new HashMap<String, Integer>();
			int gcounter = 0;
			HashSet<Character> charset = new HashSet<Character>();
			for (String gram : Multisets.copyHighestCountFirst(multiset)
					.elementSet()) {
				for (Character character : gram.toCharArray()) {
					charset.add(character);
				}
				if (topGrams.size() < hashVectorSize - 100) {
					topGrams.put(gram, gcounter);
					gcounter++;
				} else {
					for (char c : gram.toCharArray()) {
						if (!topGrams.containsKey(String.valueOf(c))) {
							topGrams.put(String.valueOf(c), gcounter);
							gcounter++;
						}
					}
				}

			}
			for (Character character : charset) {
				System.out.println(character);
			}

			LOGGER.info("size after optimization " + topGrams.size());
			HashMap<String, String> collisionSet = new HashMap<String, String>();
			HashMap<String, HashSet<Integer>> wordVectors = new HashMap<String, HashSet<Integer>>();
			int collisionNum = 0;
			for (String word : allWords) {
				HashSet<Integer> vector = new HashSet<Integer>();
				// if (title.contains("1412") || title.contains("1142")) {
				// LOGGER.info(title);
				// }
				char[] sb = new char[hashVectorSize];
				for (int i = 0; i < hashVectorSize; i++) {
					sb[i] = '0';
				}
				HashSet<String> wordGrams = wordHash.get(word);
				for (String s : wordGrams) {
					if (topGrams.containsKey(s)) {
						sb[(int) topGrams.get(s)] = '1';
						vector.add((int) topGrams.get(s));
					} else {
						for (char c : s.toCharArray()) {
							sb[(int) topGrams.get(String.valueOf(c))] = '1';
							vector.add((int) topGrams.get(String.valueOf(c)));
						}
					}
					// sb[(int) titleSet.get(s)] = '1';
				}
				String vectorS = String.valueOf(sb);

				if (collisionSet.containsKey(vectorS)) {
					collisionNum++;
					LOGGER.info("word collison: " + word + "\t"
							+ collisionSet.get(vectorS));

				} else {
					collisionSet.put(vectorS, word);
				}
				wordVectors.put(word, vector);

			}
			LOGGER.info("total word collison " + collisionNum);

			HashMap<String, HashSet<Integer>> entityVectors = new HashMap<String, HashSet<Integer>>();
			for (String entityTitle : entityWords.keySet()) {

				HashSet<Integer> entityvector = new HashSet<Integer>();
				HashSet<String> words = entityWords.get(entityTitle);
				// if (entityTitle.equals("Dehgolan_County")
				// || entityTitle.equals("Malekan_County")) {
				// LOGGER.info(words);
				// }
				for (String word : words) {
					entityvector.addAll(wordVectors.get(word));
				}
				Document page = EntitySearchEngine.getInstance()
						.getPagebyURLTitle(entityTitle);
				String id = "";
				if (page == null) {
					continue;
				} else {
					id = page.get("id");
				}
				if (entityvector.size() > 5) {
					entityVectors.put(entityTitle, entityvector);
				}
			}

			collisionNum = 0;
			collisionSet.clear();
			for (String entity : entityVectors.keySet()) {

				// if (title.contains("1412") || title.contains("1142")) {
				// LOGGER.info(title);
				// }
				char[] sb = new char[hashVectorSize];
				for (int i = 0; i < hashVectorSize; i++) {
					sb[i] = '0';
				}
				HashSet<Integer> wordGrams = entityVectors.get(entity);
				// HashSet<Integer> wordGrams2 =
				// entityVectors.get("Malekan_County");
				for (Integer i : wordGrams) {
					sb[i] = '1';
				}
				String vectorS = String.valueOf(sb);
				if (collisionSet.containsKey(vectorS)) {
					String entity2 = TurkishNLP.toLowerCase(collisionSet
							.get(vectorS));
					if (TurkishNLP.toLowerCase(entity).equals(entity2)
							|| entity.length() < 3 || entity2.length() < 3
							|| entity.contains("(anlam_ayrımı)")
							|| entity2.contains("(anlam_ayrımı)")) {
						continue;
					}
					collisionNum++;
					LOGGER.info("entity collison: " + entity + "\t"
							+ collisionSet.get(vectorS));

				} else {
					collisionSet.put(vectorS, entity);
				}

			}
			LOGGER.info("total entity collison " + collisionNum);

			Writer writer = new FileWriter("hashvectors.json");
			Gson gson = new GsonBuilder().create();
			gson.toJson(entityVectors, writer);
			writer.close();

		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	private static HashSet<String> extractDistinctWords(String label) {
		HashSet<String> distinctWords = new HashSet<String>();
		label = label.replaceAll("_", " ");
		label = TurkishNLP.toLowerCase(label);
		// label = Zemberek.getInstance().removeNonChars(label);
		String[] words = label.split(" ");
		for (String word : words) {
			if (word.length() >= 1) {
				// try {
				// double parseDouble = Double.parseDouble(word);
				// continue;
				// } catch (Exception e) {
				// // TODO: handle exception
				// }
				word = TurkishNLP.toLowerCase(word);
				word = "#" + word + "#";
				distinctWords.add(word);
			}
		}
		return distinctWords;
	}

	public static void prepareTitle() {
		List<String> titles = MYSQL.getPageTitles();
		TreeSet<String> titleSet = new TreeSet<String>();
		for (String title : titles) {
			title = title.replaceAll("\"|\\?|'|“|”", "");
			title = TurkishNLP.toLowerCase(title);
			title = "#" + title + "#";
			for (int i = 0; i < title.length() - 2; i++) {
				titleSet.add(title.substring(i, i + 3));
			}
			LOGGER.info(title);
		}
		LOGGER.info("titles size = " + titleSet.size());
		try {
			File file = new File("title_index.txt");

			// if file doesnt exists, then create it
			if (!file.exists()) {
				file.createNewFile();
			}

			FileWriter fw = new FileWriter(file.getAbsoluteFile());
			BufferedWriter bw = new BufferedWriter(fw);
			int counter = 0;
			for (String s : titleSet) {
				bw.write(s + "\t" + counter++ + "\n");
			}

			bw.close();
		} catch (Exception e) {
			// TODO: handle exception
		}

	}

	public static void main(String[] args) throws FileNotFoundException {
		prepareProperty();
		// prepareTitle();
		// prepareVector();
		// preparePositives();
		// prepareNegatives();
		// prepareNegativesLinks();
		// prepareNegativesDisPages();
		// preparePositivesDisPages();
		// measureDataSimilarity();
		// prepareLibLinear();
		// prepareTSNEDis();
		// prepareTSNEAll();
		// prepareTSNEExperiment();
	}

	public static void prepareVector() throws FileNotFoundException {

		HashMap<String, String> trtitles = MYSQL.getTRTitles();
		try {
			List<String> linesHash = FileUtils
					.readFile("property_index_hash.txt");
			HashMap<String, String> propertyHashMap = new HashMap<String, String>();
			for (String line : linesHash) {
				String[] t = line.split("\t");
				// LOGGER.info(line);
				if (t[1].contains("1")) {
					System.out.println(line);

				}
				propertyHashMap.put(t[0], t[1]);
			}
			linesHash.clear();
			linesHash = null;
			Map<String, HashSet<Integer>> propertyIndexes = new HashMap<String, HashSet<Integer>>();
			Map<String, HashSet<String>> propertyIndexesHash = new HashMap<String, HashSet<String>>();

			List<String> titleList = MYSQL.getPageTitles();
			for (String url : titleList) {
				String title = url.replaceAll("\"|\\?|'|“|”", "").replaceAll(
						"_", " ");
				title = TurkishNLP.toLowerCase(title);
				String[] split = title.split(" ");

				for (int i = 0; i < split.length; i++) {
					String word = "#" + split[i] + "#";
					String s = propertyHashMap
							.get(TurkishNLP.toLowerCase(word));
					if (s != null) {
						if (propertyIndexesHash.containsKey(url)) {
							propertyIndexesHash.get(url).add(s);
						} else {
							HashSet<String> l = new HashSet<String>();
							l.add(s);
							propertyIndexesHash.put(url, l);
						}

					}

				}
				// LOGGER.info(title);
			}
			titleList.clear();
			titleList = null;

			List<String> lines = FileUtils
					.readFile("C:\\wikipedia_turkish\\dbpedia\\mappingbased_properties_en_uris_tr.nt");
			for (String line : lines) {
				try {
					String t[] = line.split(" ");
					String title = t[0].substring(t[0].lastIndexOf('/') + 1)
							.replaceAll(">", "");
					title = trtitles.get(title);
					if (title == null) {
						continue;
					}

					String s = propertyHashMap
							.get(TurkishNLP.toLowerCase(t[1]));
					if (s != null) {
						if (propertyIndexesHash.containsKey(title)) {
							propertyIndexesHash.get(title).add(s);
						} else {
							HashSet<String> l = new HashSet<String>();
							l.add(s);
							propertyIndexesHash.put(title, l);
						}

					} else {
						LOGGER.info(s);
					}

				} catch (Exception e) {
					// TODO: handle exception
				}

			}
			lines.clear();
			lines = null;

			// lines = FileUtilZZZs
			// .readFile("C:\\wikipedia_turkish\\dbpedia\\article_categories_en_uris_tr.nt");
			List<String> list = MYSQL.getCategories();
			for (String cat : list) {
				String[] category = cat.split("\t");
				String title = category[0];
				String catName = TurkishNLP.toLowerCase(category[1]);
				try {
					// if (!title.equals("Atakule")) {
					// continue;
					// }
					String s = propertyHashMap.get(catName);
					if (s != null) {
						if (propertyIndexesHash.containsKey(title)) {
							propertyIndexesHash.get(title).add(s);
						} else {
							HashSet<String> l = new HashSet<String>();
							l.add(s);
							propertyIndexesHash.put(title, l);
						}
					} else {
						// LOGGER.info(s);
					}
				} catch (Exception e) {
					// TODO: handle exception
				}

			}
			list.clear();
			list = null;
			lines = FileUtils
					.readFile("C:\\wikipedia_turkish\\dbpedia\\instance_types_en_uris_tr.nt");
			for (String line : lines) {
				try {
					String t[] = line.split(" ");
					String title = t[0].substring(t[0].lastIndexOf('/') + 1)
							.replaceAll(">", "");
					title = trtitles.get(title);
					if (title == null) {
						continue;
					}

					String s = propertyHashMap
							.get(TurkishNLP.toLowerCase(t[2]));
					if (s != null) {

						if (propertyIndexesHash.containsKey(title)) {
							propertyIndexesHash.get(title).add(s);
						} else {
							HashSet<String> l = new HashSet<String>();
							l.add(s);
							propertyIndexesHash.put(title, l);
						}
					} else {
						LOGGER.info(s);
					}
				} catch (Exception e) {
					// TODO: handle exception
				}

			}

			Reader reader = new FileReader("domain.json");
			Gson gson = new GsonBuilder().create();
			Map<String, String> domains = gson.fromJson(reader, Map.class);
			List<String> titles = MYSQL.getPageTitles();
			for (String title : titles) {
				try {
					// if (!title.equals("Mehmet_Tomanbay")) {
					// continue;
					// }
					Set<Integer> titleIndexes = new HashSet<Integer>();
					TreeSet<String> titleSet = new TreeSet<String>();
					int id = MYSQL.getId(title);
					// if (EntitySearchEngine.getInstance().getPage(
					// String.valueOf(id)) == null) {
					// continue;
					// }

					String enTitle = MYSQL.getEnTitle(title);
					String domain = domains.get(enTitle);

					try {
						if (domain != null) {

							String s = propertyHashMap.get(domain);
							if (s != null) {

								if (propertyIndexesHash.containsKey(title)) {
									propertyIndexesHash.get(title).add(s);
								} else {
									HashSet<String> l = new HashSet<String>();
									l.add(s);
									propertyIndexesHash.put(title, l);
								}
							} else {
								LOGGER.info(s);
							}
						}
					} catch (Exception e) {
						LOGGER.error(e);
					}

					try {
						// if (title.equals("Adı_Yok_(dizi)")) {
						// LOGGER.info("");
						// }
						if (title.contains("_(")) {
							String urlType = TurkishNLP.toLowerCase(
									title.substring(title.indexOf("_(") + 2,
											title.indexOf(")"))).replaceAll(
									"_", " ");
							String s = propertyHashMap.get(urlType);
							if (s != null) {

								if (propertyIndexesHash.containsKey(title)) {
									propertyIndexesHash.get(title).add(s);
								} else {
									HashSet<String> l = new HashSet<String>();
									l.add(s);
									propertyIndexesHash.put(title, l);
								}
							}
						}
					} catch (Exception e) {
						LOGGER.error(e);
					}

					int titleSize = 0;
					// int propertySize = 8169;
					HashSet<Integer> pIndexes = propertyIndexes.get(title);
					HashSet<String> pIndexesHash = propertyIndexesHash
							.get(title);
					byte[] bytesHash = new byte[hashVectorSize];
					if (pIndexesHash != null && !pIndexesHash.isEmpty()) {
						for (String s : pIndexesHash) {
							for (int i = 0; i < hashVectorSize; i++) {
								if (s.charAt(i) == '1') {
									bytesHash[i] = 1;
								}
								// else {
								// bytesHash[i] = 0;
								// }
							}
						}

						FileUtils.write(bytesHash, "C:\\data\\vector\\"
								+ File.separator + id + ".dat");

					} else {
						LOGGER.info(title);
					}

				} catch (Exception e) {
					LOGGER.error(e);
				}

			}
		} catch (Exception e) {
			LOGGER.error(e);
		}

	}

	public static StringBuffer getFeatureVectorLibLinear(String key,
			StringBuffer sb) {
		byte[] values = FileUtils.read(Property.getInstance().get(
				"featuresDirectory")
				+ File.separator + key + ".dat");
		int index = 0;
		if (sb == null) {
			sb = new StringBuffer("");
		} else {
			index = values.length;
		}
		for (int i = 0; i < values.length; i++) {
			if (values[i] == 1) {
				sb.append((i + index) + " ");
			}

		}
		return sb;
	}

	public static double[] getFeatureVector(String key) {
		byte[] values = FileUtils.read(Property.getInstance().get(
				"featuresDirectory")
				+ File.separator + key + ".dat");
		return toDoubleArray(values);
	}

	public static double[] toDoubleArray(byte[] byteArray) {
		double[] doubles = new double[hashVectorSize];
		for (int i = 0; i < hashVectorSize; i++) {
			doubles[i] = byteArray[i];
			// if (doubles[i] == 1) {
			// LOGGER.info(doubles[i]);
			// }
		}
		return doubles;
	}

	private static void prepareLibLinear() {
		List<String> posSamples = FileUtils.readFile("pos.txt");
		List<String> negSamples = FileUtils.readFile("neg.txt");
		int size = negSamples.size();
		if (negSamples.size() < posSamples.size()) {
			size = posSamples.size();
		}
		ArrayList<String> list = new ArrayList<String>();
		for (int i = 0; i < size; i++) {
			if (i < negSamples.size()) {
				String[] p = negSamples.get(i).split("\t");
				StringBuffer sb = getFeatureVectorLibLinear(p[0], null);
				sb = getFeatureVectorLibLinear(p[1], sb);
				sb.insert(0, "0 ");
				sb.setCharAt(sb.length() - 1, '\n');
				list.add(sb.toString());
				// LOGGER.info(sb);
			}

			if (i < posSamples.size()) {
				String[] p = posSamples.get(i).split("\t");
				StringBuffer sb = getFeatureVectorLibLinear(p[0], null);
				sb = getFeatureVectorLibLinear(p[1], sb);
				sb.insert(0, "1 ");
				sb.setCharAt(sb.length() - 1, '\n');
				list.add(sb.toString());
			}
			// LOGGER.info(sb);
			if (i % 10000 == 0) {
				Collections.shuffle(list);
				StringBuffer sb2 = new StringBuffer();
				for (String s : list) {
					sb2.append(s);
				}
				FileUtils.writeFile(sb2.toString(), "liblinear_big2.txt");
				list.clear();
				LOGGER.info(i);
			}

		}
	}

	public static void measureDataSimilarity() {
		CosineSimilarity cs = new CosineSimilarity();

		// List<String> posSamples = FileUtils.readFile("pos.txt");
		List<String> negSamples = FileUtils.readFile("neg.txt");

		File folder = new File(Property.getInstance().get("featuresDirectory"));
		File[] listOfFiles = folder.listFiles();
		HashSet<Integer> entities = new HashSet<Integer>();
		for (File file : listOfFiles) {
			if (file.isFile()) {
				String key = file.getName().replace(".dat", "");
				entities.add(Integer.parseInt(key));
			}
		}

		double totalValue = 0;
		// for (int i = 0; i < posSamples.size(); i++) {
		// String[] p = posSamples.get(i).split("\t");
		// double[] p1 = getFeatureVector(p[0]);
		// double[] p2 = getFeatureVector(p[1]);
		// double value = cs.cosineSimilarity(p1, p2);
		// totalValue += value;
		// // LOGGER.info("P " + value);
		// }
		// LOGGER.info("Positives similarity " + totalValue /
		// posSamples.size());
		totalValue = 0;
		StringBuffer c = new StringBuffer("");
		long counter = 0;
		for (int i = 0; i < negSamples.size(); i++) {
			String[] p = negSamples.get(i).split("\t");
			if (entities.contains(Integer.parseInt(p[0]))
					&& entities.contains(Integer.parseInt(p[1]))) {
				double[] p1 = getFeatureVector(p[0]);
				double[] p2 = getFeatureVector(p[1]);
				double value = cs.cosineSimilarity(p1, p2);
				totalValue += value;
				c.append(negSamples.get(i) + "\n");

			}
			counter++;
			if (counter % 10000 == 0) {
				LOGGER.info(counter);
			}
		}
		FileUtils.writeFile(c.toString(), "neg_filter.txt");
		LOGGER.info("Negatives similarity " + totalValue / negSamples.size());

	}

	public static Set<Integer> getPageLinks(int id1) {
		Set<Integer> in = MYSQL.getIncomingLinksList(id1);
		Set<Integer> out = MYSQL.getOutgoingLinksList(id1);
		for (Integer id : out) {
			in.addAll(MYSQL.getOutgoingLinksList(id));
		}
		out.addAll(in);
		return out;
	}

	public static void prepareTSNEAll() {
		HashMap<String, List<Double>> embeddings;
		try {
			HashMap<String, String> trtitles = MYSQL.getTitles();

			FileReader reader = new FileReader(
					"C:\\Development\\videolization\\dexter-core\\embeddings.json");
			Gson gson = new GsonBuilder().create();
			embeddings = gson.fromJson(reader, HashMap.class);
			StringBuffer sb = new StringBuffer("");
			StringBuffer sb2 = new StringBuffer("");
			HashSet<String> titles = new HashSet<String>();
			for (String y : embeddings.keySet()) {
				if (EntitySearchEngine.getInstance().getPage(y) == null) {
					continue;
				}

				try {
					List<Double> ev = embeddings.get(y);
					String title = stripAccents(trtitles.get(y));
					if (titles.contains(title)) {
						continue;
					} else {
						titles.add(title);
					}
					sb.append(title);
					sb2.append(title);
					for (int z = 0; z < 150; z++) {
						sb.append("," + String.format("%.12f", ev.get(z)));
					}
					double[] rv = getFeatureVector(y);

					for (int z = 0; z < hashVectorSize; z++) {
						sb2.append("," + rv[z]);
					}
					sb.append("\r\n");
					sb2.append("\r\n");

				} catch (Exception e) {
					LOGGER.info(e);
				}
			}

			FileUtils.writeFile(sb.toString(), "embeddings_all.csv");
			FileUtils.writeFile(sb2.toString(), "embeddings_all_raw.csv");
		} catch (Exception e) {
			LOGGER.info(e);
		}
	}

	public static void prepareTSNEDis() {
		HashMap<String, List<Double>> embeddings;
		HashMap<String, List<Double>> autoencoder;
		try {

			TreeMap<String, List<String>> dispages = DisambiguationPages2
					.getDisPages();
			FileReader reader = new FileReader("word2vec150.json");
			Gson gson = new GsonBuilder().create();
			embeddings = gson.fromJson(reader, HashMap.class);

			reader = new FileReader("autoencoder300.json");
			gson = new GsonBuilder().create();
			autoencoder = gson.fromJson(reader, HashMap.class);
			int size = dispages.size();
			// size = 5000;
			int counter = 0;
			StringBuffer sb = new StringBuffer("");
			StringBuffer sb2 = new StringBuffer("");
			HashSet<String> titles = new HashSet<String>();
			for (String s : dispages.keySet()) {

				List<String> l = dispages.get(s);
				if (l.size() > 1) {
					for (int i = 0; i < l.size(); i++) {
						try {
							int pid = MYSQL.getId(l.get(i));
							List<Double> e1 = embeddings.get(String
									.valueOf(pid));
							List<Double> a1 = autoencoder.get(String
									.valueOf(pid));
							if (e1 != null && a1 != null) {
								List<Double> v1 = new ArrayList<Double>();
								v1.addAll(a1);
								v1.addAll(e1);

								String title = stripAccents(l.get(i));
								if (titles.contains(title)) {
									continue;
								} else {
									titles.add(title);
								}
								sb.append(title);
								// sb2.append(title);
								for (int z = 0; z < v1.size(); z++) {
									sb.append(","
											+ String.format("%.12f", v1.get(z)));
								}
								// double[] rv =
								// getFeatureVector(String.valueOf(pid));
								//
								// for (int z = 0; z < propertySizeHash; z++) {
								// sb2.append("," + rv[z]);
								// }
								sb.append("\r\n");
								// sb2.append("\r\n");
								if (++counter == size) {
									break;
								}
							}
						} catch (Exception e) {
							LOGGER.info(e);
						}
					}
				}
			}
			FileUtils.writeFile(sb.toString(), "embeddings_dis.csv");
			// FileUtils.writeFile(sb2.toString(), "embeddings_dis_raw.csv");
		} catch (Exception e) {
			LOGGER.info(e);
		}
	}

	private static void prepareTSNEExperiment() {
		HashMap<String, List<Double>> embeddings;
		try {
			HashMap<String, String> trtitles = MYSQL.getTitles();

			FileReader reader = new FileReader(
					"C:\\Development\\videolization\\dexter-core\\embeddings.json");
			Gson gson = new GsonBuilder().create();
			embeddings = gson.fromJson(reader, HashMap.class);

			File file = new File("tsne" + File.separator + "input2");

			// Reading directory contents
			File[] files = file.listFiles();

			for (int i = 0; i < files.length; i++) {
				String ftitle = replaceTurkish(files[i].getName().replaceAll(
						".entities", ""));
				List<String> entities = FileUtils.readFile(files[i]
						.getAbsolutePath());
				StringBuffer sb = new StringBuffer("");
				StringBuffer sb2 = new StringBuffer("");
				HashSet<String> titles = new HashSet<String>();

				for (String y : entities) {

					try {
						List<Double> ev = embeddings.get(y);
						String title = stripAccents(trtitles.get(y));
						if (title.contains("(")) {
							String temp = "-"
									+ title.substring(0, title.indexOf("(") - 1)
											.replaceAll(" ", "_");
							if (StringUtils.containsIgnoreCase(ftitle, temp)) {
								title += "_XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX";
							}
						} else {
							String temp = "-" + title.replaceAll(" ", "_");
							if (StringUtils.containsIgnoreCase(ftitle, temp)) {
								title += "_XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX";

							}
						}
						if (titles.contains(title)) {
							continue;
						} else {
							titles.add(title);
						}
						sb.append(title);
						sb2.append(title);
						for (int z = 0; z < 150; z++) {
							sb.append("," + String.format("%.15f", ev.get(z)));
						}
						double[] rv = getFeatureVector(y);

						for (int z = 0; z < hashVectorSize; z++) {
							sb2.append("," + rv[z]);
						}
						sb.append("\r\n");
						sb2.append("\r\n");

					} catch (Exception e) {
						LOGGER.info(e);
					}
				}

				FileUtils.writeFile(sb.toString(), "tsne\\embedding\\" + ftitle
						+ ".csv");
				FileUtils.writeFile(sb2.toString(), "tsne\\raw\\" + ftitle
						+ ".csv");
			}
		} catch (Exception e) {
			LOGGER.info(e);
		}

	}

	public static String stripAccents(String input) {
		input = replaceTurkish(input).replaceAll("[,'.]", "").replace("_", " ");
		if (input == null) {
			return null;
		}
		final Pattern pattern = Pattern
				.compile("\\p{InCombiningDiacriticalMarks}+");//$NON-NLS-1$
		final String decomposed = Normalizer.normalize(input,
				Normalizer.Form.NFD);
		// Note that this doesn't correctly remove ligatures...
		return pattern.matcher(decomposed).replaceAll("");//$NON-NLS-1$
	}

	public static String replaceTurkish(String input) {

		input = input.replaceAll("ü", "u");
		input = input.replaceAll("ı", "i");
		input = input.replaceAll("ö", "o");
		input = input.replaceAll("ü", "u");
		input = input.replaceAll("ş", "s");
		input = input.replaceAll("ğ", "g");
		input = input.replaceAll("ç", "c");
		input = input.replaceAll("Ü", "U");
		input = input.replaceAll("İ", "I");
		input = input.replaceAll("Ö", "O");
		input = input.replaceAll("Ü", "U");
		input = input.replaceAll("Ş", "S");
		input = input.replaceAll("Ğ", "G");
		input = input.replaceAll("Ç", "C");

		return input;
	}

}
