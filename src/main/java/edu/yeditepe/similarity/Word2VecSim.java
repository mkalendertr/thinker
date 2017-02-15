package edu.yeditepe.similarity;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.Reader;
import java.util.Map;

import org.apache.log4j.Logger;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * Loading word2vec and running similarity
 */
public class Word2VecSim {
	private static final Logger LOGGER = Logger.getLogger(Word2VecSim.class);

	private static Word2VecSim instance = new Word2VecSim();
	Map<String, Double> vectors;

	private Word2VecSim() {
		Reader reader;
		try {
			reader = new FileReader("vector.json");
			Gson gson = new GsonBuilder().create();
			vectors = gson.fromJson(reader, Map.class);
			// for (String key : vectors.keySet()) {
			// LOGGER.info(key);
			// }
		} catch (FileNotFoundException e) {
			LOGGER.error(e);
		}

	}

	public static Word2VecSim getInstance() {
		return instance;
	}

	public double getSimilarity(String p1, String p2) {
		p1 = p1.toLowerCase().replace(" ", "_");
		p2 = p2.toLowerCase().replace(" ", "_");
		if (vectors.containsKey(p1 + "||" + p2)) {
			return vectors.get(p1 + "||" + p2);
		} else if (vectors.containsKey(p2 + "||" + p1)) {
			return vectors.get(p2 + "||" + p1);
		}

		return 0;
	}

	public static void main(String[] args) throws FileNotFoundException {

		// List<String> titles = MYSQL.getEnTitles();
		// for (int i = 0; i < titles.size(); i++) {
		// String t1 = titles.get(i);
		// for (int j = i + 1; j < titles.size(); j++) {
		// String t2 = titles.get(j);
		// double sim = instance.vec.similarity("/en/" + t1, "/en/" + t2);
		// // log.info(t1 + " " + t2 + " " + sim);
		// }
		// }

		// while (true) {
		// try {
		// BufferedReader bufferRead = new BufferedReader(
		// new InputStreamReader(System.in));
		// System.out.print("Enter: ");
		// String s = bufferRead.readLine();
		// String words[] = s.split(" ");
		// if (words.length == 1) {
		// Map<String, Double> map = similarWordsInVocabTo(vec,
		// words[0], 0.5);
		// // Collections.sort(list);
		// for (String word : map.keySet()) {
		// System.out.println(word + " " + map.get(word));
		// }
		// } else if (words.length == 2) {
		// System.out.println("Similarity of "
		// + vec.similarity(words[0], words[1]));
		// } else if (words.length == 3) {
		// List<String> list = vec.analogyWords(words[0], words[1],
		// words[2]);
		//
		// for (String word : list) {
		// System.out.println(word);
		// }
		//
		// }
		//
		// } catch (Exception e) {
		// e.printStackTrace();
		// }
		// }

	}

}
