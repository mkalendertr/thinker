package edu.yeditepe.utils;

import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.TreeMap;

import org.apache.log4j.Logger;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import edu.yeditepe.repository.MYSQL;

public class DisambiguationPages {
	private static final Logger LOGGER = Logger
			.getLogger(DisambiguationPages.class);

	public static void main(String[] args) throws InterruptedException {
		TreeMap<String, List<String>> dispages = new TreeMap<String, List<String>>();
		List<String> pages = MYSQL.getPageTitles();
		for (String s : pages) {
			String title = s;

			if (s.contains("(anlam_ayrımı)")) {
				continue;
			}
			if (s.contains("_(")) {
				title = s.substring(0, s.indexOf("_(")).trim();
			}
			if (dispages.containsKey(title)) {
				dispages.get(title).add(s);
			} else {
				List<String> l = new ArrayList<String>();
				l.add(s);
				dispages.put(title, l);
			}
		}
		HashMap<String, Long> searchResults = new HashMap<String, Long>();
		boolean flag = false;
		for (String s : dispages.keySet()) {
			if (s.equals("Soldere_Suyu")) {
				flag = true;
				continue;
			}
			List<String> l = dispages.get(s);
			if (l.size() >= 1 && flag) {
				long num = PI3.search(s);
				// System.out.println(l + "\t" + num);
				LOGGER.info("\t" + l + "\t" + num);
				searchResults.put(s, num);
				// Thread.sleep(1000);
			}
		}
		Writer writer;
		try {
			writer = new FileWriter("haberturk.json");
			Gson gson = new GsonBuilder().create();
			gson.toJson(searchResults, writer);

			writer.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
