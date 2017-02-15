package edu.yeditepe.similarity;

import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import org.apache.log4j.Logger;

import com.google.common.collect.Sets;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import edu.yeditepe.repository.MYSQL;

public class WikiLinkSimilarity {
	private static final Logger LOGGER = Logger
			.getLogger(WikiLinkSimilarity.class);

	public static void main(String[] args) {
		List<Integer> ids = MYSQL.getIds();
		HashMap<Integer, String> linksMap = new HashMap<Integer, String>();
		HashMap<String, Double> similarity = new HashMap<String, Double>();

		for (Integer id : ids) {
			String links = MYSQL.getLinks(id);
			linksMap.put(id, links);
		}
		for (int i = 0; i < ids.size(); i++) {
			LOGGER.info(i);
			HashSet<String> set1 = Sets.newHashSet(linksMap.get(ids.get(i))
					.split(" "));
			for (int j = i + 1; j < ids.size(); j++) {
				HashSet<String> set2 = Sets.newHashSet(linksMap.get(ids.get(j))
						.split(" "));
				double linkSim = JaccardCalculator.calculateSimilarity(set1,
						set2);
				similarity.put(ids.get(i) + "_" + ids.get(j), linkSim);
			}
		}

		Writer writer;
		try {
			writer = new FileWriter("linksimilarity.json");
			Gson gson = new GsonBuilder().create();
			gson.toJson(similarity, writer);

			writer.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
}
