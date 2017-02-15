package edu.yeditepe.utils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.TreeMap;

import org.apache.log4j.Logger;

import com.google.common.collect.Multiset.Entry;
import com.google.common.collect.TreeMultiset;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import edu.yeditepe.repository.MYSQL;

public class DisambiguationPages2 {
	private static final Logger LOGGER = Logger
			.getLogger(DisambiguationPages2.class);
	private static String featuresDirectory = "C:\\data\\vector\\";

	public static void main(String[] args) {
		getDisPages();
		LOGGER.info("");
	}

	public static TreeMap<String, List<String>> getDisPages() {
		HashMap<String, Double> searchResults;
		Reader reader;
		try {
			reader = new FileReader("haberturk.json");
			Gson gson = new GsonBuilder().create();
			searchResults = gson.fromJson(reader, HashMap.class);

			File folder = new File(featuresDirectory);
			File[] listOfFiles = folder.listFiles();
			HashSet<Integer> entities = new HashSet<Integer>();
			for (File file : listOfFiles) {
				if (file.isFile()) {
					String key = file.getName().replace(".dat", "");
					entities.add(Integer.parseInt(key));
				}
			}
			TreeMultiset<String> types = TreeMultiset.create();
			TreeMap<String, List<String>> dispages = new TreeMap<String, List<String>>();
			List<String> pages = MYSQL.getPageTitles();
			for (String s : pages) {
				String title = s;
				if (!entities.contains(MYSQL.getId(title))) {
					continue;
				}
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
			for (String s : dispages.keySet()) {

				List<String> l = dispages.get(s);
				double num = 0;
				if (searchResults.containsKey(s)) {
					num = searchResults.get(s);
				}
				if (l.size() > 1) {
					// System.out.println(l + "\t" + num);
					// LOGGER.info("\t" + l + "\t" + num);
					// Thread.sleep(1000);
					for (String title : l) {
						if (title.contains("_(")) {
							String t = title.substring(title.indexOf("(") + 1,
									title.indexOf(")"));
							types.add(t);
						}
					}

				}
			}
			for (Entry<String> t : types.entrySet()) {
				LOGGER.info(t.getElement() + "\t" + t.getCount());
			}
			return dispages;
		} catch (FileNotFoundException e) {
			LOGGER.error(e);
		}
		return null;

	}

}
