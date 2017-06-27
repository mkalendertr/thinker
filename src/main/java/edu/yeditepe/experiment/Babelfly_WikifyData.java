package edu.yeditepe.experiment;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.ParseException;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;
import org.apache.lucene.document.Document;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import edu.yeditepe.lucene.EntitySearchEngine;
import edu.yeditepe.nlp.TurkishNLP;
import edu.yeditepe.utils.FileUtils;

public class Babelfly_WikifyData {
	private static final Logger LOGGER = Logger
			.getLogger(Babelfly_WikifyData.class);

	private static Babelfly_WikifyData wikifier = new Babelfly_WikifyData();

	// Invalid parameter: Usage:
	// GET or POST parameters: {tool}, {input} and {token}
	// token: you can find your token on your login space
	// tool: ner, morphanalyzer, isturkish, morphgenerator, tokenizer,
	// normalize, deasciifier, Vowelizer, depparser, spellcheck, disambiguator,
	// pipeline
	// input: utf-8 string
	// The response is a text/plain encoded in UTF-8

	private Babelfly_WikifyData() {

	}

	public static Babelfly_WikifyData getInstance() {
		return wikifier;
	}

	public static String makeRequest(String text, String filename) {
		try {

			DefaultHttpClient httpclient = new DefaultHttpClient();

			HttpPost post = new HttpPost("http://babelfy.io/v1/disambiguate");
			List<NameValuePair> parameters = new ArrayList<NameValuePair>(3);

			parameters.add(new BasicNameValuePair("lang", "TR"));
			parameters.add(new BasicNameValuePair("text", text));
			parameters.add(new BasicNameValuePair("key",
					"c645c0e7-4d9b-46d8-b9a2-403ed0a68187"));
			post.setEntity(new UrlEncodedFormEntity(parameters, "UTF-8"));
			HttpResponse resp = httpclient.execute(post);
			String output = EntityUtils.toString(resp.getEntity());
			// JSONArray response = (JSONArray) parser.parse(output);

			Writer writer = null;

			try {
				writer = new BufferedWriter(new OutputStreamWriter(
						new FileOutputStream("babel\\" + filename
								+ "_babel.json"), "utf-8"));
				writer.write(output);
			} catch (IOException ex) {
				// report
			} finally {
				try {
					writer.close();
				} catch (Exception ex) {/* ignore */
				}
			}

			// for (int i = 0; i < response.size(); i++) {
			// JSONObject entity = (JSONObject) response.get(i);
			// LOGGER.info(entity.get("babelSynsetID"));
			// }

			return output;
		} catch (UnsupportedEncodingException e) {
			LOGGER.error(e);
		} catch (ClientProtocolException e) {
			LOGGER.error(e);
		} catch (IOException e) {
			LOGGER.error(e);
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

	public static void main(String[] args) {
		// Babelfly.getInstance().makeRequest(
		// "Yeditepe Üniversitesi, İstanbul'da bulunuyor.");
		HashMap<String, HashSet<String>> annotations = new HashMap<String, HashSet<String>>();
		HashMap<String, String> data = new HashMap<String, String>();
		HashSet<String> hard = new HashSet<String>();
		HashSet<String> easy = new HashSet<String>();
		int ecorrect = 0;
		int eincorrect = 0;

		int hcorrect = 0;
		int hincorrect = 0;
		int undetected = 0;
		int counter = 0;
		BufferedReader in;
		try {
			File file = new File("wiikifier_test");
			// Reading directory contents
			File[] files = file.listFiles();

			for (int i = 0; i < files.length; i++) {
				String fileName = files[i].getPath();
				if (fileName.endsWith("mentions")) {
					HashSet<String> set = new HashSet<String>();
					annotations
							.put(files[i].getName().replaceAll(".mentions", ""),
									set);
					List<String> annoList = FileUtils.readFile(fileName);
					for (String anno : annoList) {
						String[] split = anno.split("\t");
						String entity = split[2];
						Document pagebyURLTitle = EntitySearchEngine
								.getInstance().getPagebyURLTitle(split[3]);
						if (pagebyURLTitle == null) {
							continue;
						}
						if (split[4].equals("1")) {
							hard.add(entity.toLowerCase());
						} else {
							easy.add(entity.toLowerCase());
						}
						set.add(entity.toLowerCase());
					}
				} else {
					String readFileString = FileUtils.readFileString(fileName);
					data.put(files[i].getName().replaceAll(".txt", ""),
							readFileString);
				}
			}
			int c = 0;
			for (String instance : data.keySet()) {
				try {

					File f = new File("babel\\" + instance.hashCode()
							+ "_babel.json");
					String output = "";
					if (f.exists() && !f.isDirectory()) {
						output = FileUtils.readFileString(f.getPath());
					} else {
						output = makeRequest(data.get(instance),
								String.valueOf(instance.hashCode()));

					}
					JSONParser parser = new JSONParser();
					JSONArray response = (JSONArray) parser.parse(output);
					HashSet<String> extracted = new HashSet<String>();
					for (int y = 0; y < response.size(); y++) {
						try {
							JSONObject entity = (JSONObject) response.get(y);
							String url = (String) entity.get("DBpediaURL");
							if (url.length() > 0) {
								String name = url.substring(url
										.lastIndexOf('/') + 1);

								extracted.add(name.toLowerCase());
								// LOGGER.info(url);
							}
						} catch (Exception e) {
							// TODO: handle exception
						}

					}
					String text = TurkishNLP.toLowerCase(data.get(instance));

					HashSet<String> anno = annotations.get(instance);
					for (String a : anno) {
						if (text.contains(a.replaceAll("_", " "))) {
							if (extracted.contains(a)) {
								if (hard.contains(a)) {
									hcorrect++;
								} else {
									ecorrect++;
								}

							} else {
								LOGGER.info(a);
								if (hard.contains(a)) {
									hincorrect++;
								} else {
									eincorrect++;
								}
							}
						}
					}
					c++;
					// if (c > 6850) {
					// break;
					// }
				} catch (Exception e) {
					// TODO: handle exception
				}
			}

			float haccuracy = (float) hcorrect / (hcorrect + hincorrect);
			float eaccuracy = (float) ecorrect / (ecorrect + eincorrect);
			// if (correct + incorrect > 0) {
			// precision = (float) correct / (correct + incorrect);
			// }
			// float recall = 0;
			// if (correct + incorrect + undetected > 0) {
			// recall = (float) correct / (correct + incorrect + undetected);
			// }
			// float fmeasure = 0;
			// if (precision + recall > 0) {
			// fmeasure = 2 * precision * recall / (precision + recall);
			// }
			// System.out
			// .println("\tCorrect\tIncorrect\tUndetected\tPrecison\tRecall\tF-measure");
			// System.out.println("\t" + correct + "\t" + incorrect + "\t"
			// + undetected + "\t" + precision + "\t" + recall + "\t"
			// + fmeasure);
			LOGGER.info(hcorrect + "\t" + hincorrect + "\t" + haccuracy + "\t"
					+ ecorrect + "\t" + eincorrect + "\t" + eaccuracy);

		} catch (Exception e) {
			e.printStackTrace();
		}

	}
}
