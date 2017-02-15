package edu.yeditepe.similarity;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Writer;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.GZIPInputStream;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class PageViews {
	public static void main(String[] args) {
		Map<String, Long> map = new HashMap<String, Long>();

		BufferedReader in;
		try {
			File file = new File("C:\\wikipedia_turkish\\viewcounts");

			// Reading directory contents
			File[] files = file.listFiles();

			for (int i = 0; i < files.length; i++) {
				System.out.println(files[i]);
				in = new BufferedReader(new InputStreamReader(
						new GZIPInputStream(new FileInputStream(files[i]))));

				String content;

				while ((content = in.readLine()) != null) {
					try {

						String[] data = content.split(" ");
						if (data[0].equals("tr")) {
							String title = URLDecoder.decode(data[1]);
							long count = Long.parseLong(data[3]);
							// System.out.println(title + " " + count);
							if (map.containsKey(title)) {
								map.put(title, map.get(title) + count);
							} else {
								map.put(title, count);
							}
						}
					} catch (Exception e) {
						// TODO: handle exception
					}
				}
			}

		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		Writer writer;
		try {
			writer = new FileWriter("viewcounts.json");
			Gson gson = new GsonBuilder().create();
			gson.toJson(map, writer);

			writer.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
