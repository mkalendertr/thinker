package edu.yeditepe.similarity;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.log4j.Logger;

import vectorspace.CosineSimilarity;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import edu.yeditepe.deep.EntityEmbedding;
import edu.yeditepe.model.EntityPage;

public class EntityEmbeddingSimilarity {
	private static final Logger LOGGER = Logger
			.getLogger(EntityEmbeddingSimilarity.class);

	private static HashMap<String, List<Double>> embeddings = new HashMap<String, List<Double>>();;
	private static HashMap<String, List<Double>> autoencoder = new HashMap<String, List<Double>>();

	private static CosineSimilarity cs = new CosineSimilarity();

	private EntityEmbedding nn;

	private static String modelFile = "embedding_network.model";

	private static boolean threeGram = false;

	private static EntityEmbeddingSimilarity instance = new EntityEmbeddingSimilarity();

	public static EntityEmbeddingSimilarity getInstance() {
		return instance;
	}

	public static void setInstance(EntityEmbeddingSimilarity instance) {
		EntityEmbeddingSimilarity.instance = instance;
	}

	public static void main(String[] args) throws IOException {
		EntityEmbeddingSimilarity es = new EntityEmbeddingSimilarity();
		es.interact();
		LOGGER.info(es.getSimilarity("393217", "958501"));
		List<String> list = new ArrayList<String>();
		list.add("958501");
		// list.add("10");
		List<Double> e1 = es.getEmbedding("393217");
		List<Double> e2 = es.getEmbedding(list);
		LOGGER.info(es.getSimilarity(e1, e2));
	}

	private EntityEmbeddingSimilarity() {
		if (threeGram) {
			Reader reader;
			try {
				reader = new FileReader("hash_embeddings_v2.json");
				Gson gson = new GsonBuilder().create();
				autoencoder = gson.fromJson(reader, HashMap.class);

			} catch (FileNotFoundException e) {
				LOGGER.error(e);
			}
		}

	}

	public double getSimilarity(String p1, String p2) {
		try {

			boolean flag = true;
			if (flag) {
				List<Double> e1 = embeddings.get(p1);
				List<Double> e2 = embeddings.get(p2);
				List<Double> a1 = autoencoder.get(p1);
				List<Double> a2 = autoencoder.get(p2);
				if (e1 != null && e2 != null && a1 != null && a2 != null) {
					List<Double> v1 = new ArrayList<Double>();
					v1.addAll(a1);
					v1.addAll(e1);
					List<Double> v2 = new ArrayList<Double>();
					v2.addAll(a2);
					v2.addAll(e2);
					return getSimilarity(v1, v2);
					// EntityEmbeddingSimilarity..return (getSimilarity(a1, a2)
					// + getSimilarity(e1, e2)) / 2;
				} else if (e1 != null && e2 != null) {
					// return getSimilarity(e1, e2);

				} else if (a1 != null && a2 != null) {
					return getSimilarity(a1, a2);

				}
			} else {
				List<Double> e1 = embeddings.get(p1);
				List<Double> e2 = embeddings.get(p2);

				if (e1 != null && e2 != null) {

					return getSimilarity(e1, e2);
				}
			}
		} catch (Exception e) {
			LOGGER.error(e);
		}
		return 0;

	}

	public double getSimilarity(EntityPage p1, EntityPage p2) {
		try {

			boolean flag = true;
			if (flag) {
				List<Double> e1 = p1.getWord2vec();
				List<Double> e2 = p2.getWord2vec();
				List<Double> a1 = null;
				List<Double> a2 = null;
				if (threeGram == false) {
					a1 = p1.getAutoencoder();
					a2 = p2.getAutoencoder();
				} else {
					a1 = autoencoder.get(p1.getUrlTitle());
					a2 = autoencoder.get(p2.getUrlTitle());
				}

				if (e1 != null && e2 != null && a1 != null && a2 != null) {
					List<Double> v1 = new ArrayList<Double>();
					v1.addAll(a1);
					v1.addAll(e1);
					List<Double> v2 = new ArrayList<Double>();
					v2.addAll(a2);
					v2.addAll(e2);
					// return getSimilarity(a1, a2);
					return (getSimilarity(a1, a2) + 2 * getSimilarity(e1, e2)) / 2;
				} else if (a1 != null && a2 != null) {
					return getSimilarity(a1, a2);

				} else if (e1 != null && e2 != null) {
					return getSimilarity(e1, e2);

				}
			} else {
				List<Double> e1 = embeddings.get(p1);
				List<Double> e2 = embeddings.get(p2);

				if (e1 != null && e2 != null) {

					return getSimilarity(e1, e2);
				}
			}
		} catch (Exception e) {
			LOGGER.error(e);
		}
		return 0;

	}

	public double getSimilarityRaw(String p1, String p2) {
		// try {
		// double[] e1 = PrepareData.getFeatureVector(p1);
		// double[] e2 = PrepareData.getFeatureVector(p2);
		// return cs.cosineSimilarity(e1, e2);
		// } catch (Exception e) {
		// // LOGGER.error(e);
		// }
		return 0;

	}

	public double getSimilarity(List<Double> e1, List<Double> e2) {

		if (e1 != null && e2 != null) {
			double value = cs.cosineSimilarity(e1, e2);
			if (value < 0) {
				return 0;
			}
			return value;
		} else {
			return 0;
		}
	}

	public ArrayList<Double> getEmbedding(List<String> input) {
		double[] e = cs.normalize(nn.evaluate(nn.getFeatureVector(input)));
		ArrayList<Double> list = new ArrayList<Double>(e.length);
		for (Double element : e) {
			list.add(element);
		}
		return list;
	}

	public List<Double> getEmbedding(String input) {
		return embeddings.get(input);
	}

	// public TreeMap<Double, String> neighbours(String title) {
	// String key = EntitySearchEngine.getInstance().getPagebyURLTitle(title);
	// TreeMap<Double, String> map = new TreeMap<Double, String>();
	// for (String e : embeddings.keySet()) {
	// double sim = getSimilarity(e, key);
	// if (sim > 0) {
	// if (map.size() < 20) {
	// map.put(sim, EntitySearchEngine.getInstance()

	// getPagebyId(e));
	// } else {
	// if (map.firstKey() < sim) {
	// map.remove(map.firstKey());
	// map.put(sim, MYSQL.getTRTitleById(e));
	// }
	// }
	// }
	//
	// }
	// return map;
	// }

	public void interact() throws IOException {
		// try (BufferedReader br = new BufferedReader(new InputStreamReader(
		// System.in))) {
		// while (true) {
		// System.out
		// .print("Enter wikipedia page title (EXIT to break): ");
		// String title = br.readLine();
		// if (title.equals("EXIT")) {
		// break;
		// }
		// TreeMap<Double, String> neighbours = neighbours(String
		// .valueOf(MYSQL.getId(title)));
		// for (Double val : neighbours.descendingKeySet()) {
		// System.out.println(neighbours.get(val) + "="
		// + String.format("%.2f", val));
		// }
		//
		// }
		// }
	}
}
