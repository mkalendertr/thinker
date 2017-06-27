package edu.yeditepe.experiment;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

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
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import edu.yeditepe.nlp.TurkishNLP;
import edu.yeditepe.utils.FileUtils;
import edu.yeditepe.utils.Property;

public class Babelfly {
	private static final Logger LOGGER = Logger.getLogger(Babelfly.class);

	private static Babelfly wikifier = new Babelfly();

	// Invalid parameter: Usage:
	// GET or POST parameters: {tool}, {input} and {token}
	// token: you can find your token on your login space
	// tool: ner, morphanalyzer, isturkish, morphgenerator, tokenizer,
	// normalize, deasciifier, Vowelizer, depparser, spellcheck, disambiguator,
	// pipeline
	// input: utf-8 string
	// The response is a text/plain encoded in UTF-8

	private Babelfly() {

	}

	public static Babelfly getInstance() {
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
		int correct = 0;
		int incorrect = 0;
		int undetected = 0;
		int counter = 0;
		BufferedReader in;
		try {
			List<String> readFile = FileUtils.readFile(Property.getInstance()
					.get("experiment.targetAnnotation"));
			for (String line : readFile) {
				String[] split = line.split("=");
				String folder = split[0];
				String[] ids = split[1].split(",");
				LinkedHashSet<String> set = new LinkedHashSet<String>();
				for (String id : ids) {
					set.add(id);
				}
				annotations.put(folder, set);
			}
			File file = new File(Property.getInstance()
					.get("experiment.target"));

			// Reading directory contents
			File[] files = file.listFiles();

			for (int i = 0; i < files.length; i++) {
				String folder = files[i].getName();
				HashSet<String> correctIds = annotations.get(folder);
				File[] files2 = files[i].listFiles();
				for (File file2 : files2) {
					in = new BufferedReader(new InputStreamReader(
							(new FileInputStream(file2))));

					String text = "";
					String line;
					String prevline = "";
					while ((line = in.readLine()) != null) {
						try {
							text += line + " ";
							prevline = line;
						} catch (Exception e) {
							// TODO: handle exception
						}
					}
					text = text.replace(prevline, "");
					File f = new File("babel\\" + file2.getPath().hashCode()
							+ "_babel.json");
					String output = "";
					if (f.exists() && !f.isDirectory()) {
						output = FileUtils.readFileString(f.getPath());
					} else {
						output = makeRequest(text,
								String.valueOf(file2.getPath().hashCode()));
					}
					JSONParser parser = new JSONParser();
					JSONArray response = (JSONArray) parser.parse(output);
					Set<String> names = new HashSet<String>();
					Set<String> ids = new HashSet<String>();
					String namesText = "";
					for (int y = 0; y < response.size(); y++) {
						JSONObject entity = (JSONObject) response.get(y);
						String babelId = (String) entity.get("babelSynsetID");
						JSONObject charFragment = (JSONObject) entity
								.get("charFragment");
						String start = String
								.valueOf(charFragment.get("start"));
						String end = String.valueOf(charFragment.get("end"));
						String name = text.substring(Integer.parseInt(start),
								Integer.parseInt(end) + 1);
						names.add(TurkishNLP.toLowerCase(name));
						namesText += TurkishNLP.toLowerCase(name) + " ";
						ids.add(babelId.toString());
						LOGGER.info(name + "\t" + entity.get("babelSynsetID"));
					}
					String target = TurkishNLP.toLowerCase(folder);
					boolean detected = false;
					for (String id : ids) {
						if (correctIds.contains(id)) {
							correct++;
							detected = true;
							LOGGER.info("True\t" + target + "\t" + id);
							break;
						}
					}

					if (detected == false && namesText.contains(target)) {
						incorrect++;
						LOGGER.info("False\t" + target + "\t");
					} else {
						undetected++;
						LOGGER.info("Undetected\t" + target + "\t");
					}

				}
			}
			float precision = 0;
			if (correct + incorrect > 0) {
				precision = (float) correct / (correct + incorrect);
			}
			float recall = 0;
			if (correct + incorrect + undetected > 0) {
				recall = (float) correct / (correct + incorrect + undetected);
			}
			float fmeasure = 0;
			if (precision + recall > 0) {
				fmeasure = 2 * precision * recall / (precision + recall);
			}
			System.out
					.println("\tCorrect\tIncorrect\tUndetected\tPrecison\tRecall\tF-measure");
			System.out.println("\t" + correct + "\t" + incorrect + "\t"
					+ undetected + "\t" + precision + "\t" + recall + "\t"
					+ fmeasure);

		} catch (Exception e) {
			// TODO: handle exception
		}

	}
}
