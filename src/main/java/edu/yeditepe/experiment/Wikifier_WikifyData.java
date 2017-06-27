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
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;

import edu.yeditepe.repository.MYSQL;
import edu.yeditepe.utils.FileUtils;

public class Wikifier_WikifyData {
	private static final Logger LOGGER = Logger
			.getLogger(Wikifier_WikifyData.class);

	private static Wikifier_WikifyData wikifier = new Wikifier_WikifyData();

	// Invalid parameter: Usage:
	// GET or POST parameters: {tool}, {input} and {token}
	// token: you can find your token on your login space
	// tool: ner, morphanalyzer, isturkish, morphgenerator, tokenizer,
	// normalize, deasciifier, Vowelizer, depparser, spellcheck, disambiguator,
	// pipeline
	// input: utf-8 string
	// The response is a text/plain encoded in UTF-8

	private Wikifier_WikifyData() {

	}

	public static Wikifier_WikifyData getInstance() {
		return wikifier;
	}

	public static String makeRequest(String text, String filename) {
		try {

			DefaultHttpClient httpclient = new DefaultHttpClient();

			HttpPost post = new HttpPost(
					"http://shelley.cs.illinois.edu:8080/xlwikifier");
			List<NameValuePair> parameters = new ArrayList<NameValuePair>(3);

			parameters.add(new BasicNameValuePair("lang", "English"));
			parameters.add(new BasicNameValuePair("text", text));
			post.setEntity(new UrlEncodedFormEntity(parameters, "UTF-8"));
			HttpResponse resp = httpclient.execute(post);
			String output = EntityUtils.toString(resp.getEntity());
			Writer writer = null;

			try {
				writer = new BufferedWriter(new OutputStreamWriter(
						new FileOutputStream("wikifier\\" + filename
								+ "_wikifier.txt"), "utf-8"));
				writer.write(output);
			} catch (IOException ex) {
				// report
			} finally {
				try {
					writer.close();
				} catch (Exception ex) {/* ignore */
				}
			}

			String pattern1 = "English Wiki: ";
			String pattern2 = " <br>";

			Pattern p = Pattern.compile(Pattern.quote(pattern1) + "(.*?)"
					+ Pattern.quote(pattern2));
			Matcher m = p.matcher(output);
			while (m.find()) {
				System.out.println(m.group(1));
				String entitle = m.group(1);
				String trTitleByEnName = MYSQL.getTRTitleByEnName(entitle);
				System.out.println(trTitleByEnName);
				int id = MYSQL.getId(trTitleByEnName);
				LOGGER.info("w" + id);
			}

			return output;
		} catch (UnsupportedEncodingException e) {
			LOGGER.error(e);
		} catch (ClientProtocolException e) {
			LOGGER.error(e);
		} catch (IOException e) {
			LOGGER.error(e);
		}
		return null;
	}

	public static void main(String[] args) {
		// Babelfly.getInstance().makeRequest(
		// "Yeditepe Üniversitesi, İstanbul'da bulunuyor.");
		HashMap<String, HashSet<String>> annotations = new HashMap<String, HashSet<String>>();
		HashMap<String, String> data = new HashMap<String, String>();
		int correct = 0;
		int incorrect = 0;
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

					File f = new File("wikifier\\" + instance.hashCode()
							+ "_wikifier.txt");
					String output = "";
					if (f.exists() && !f.isDirectory()) {
						output = FileUtils.readFileString(f.getPath());
					} else {
						output = makeRequest(data.get(instance),
								String.valueOf(instance.hashCode()));

					}
					String pattern1 = "English Wiki: ";
					String pattern2 = " <br>";
					Set<String> ids = new HashSet<String>();
					Set<String> names = new HashSet<String>();
					String namesText = "";
					Pattern p = Pattern.compile(Pattern.quote(pattern1)
							+ "(.*?)" + Pattern.quote(pattern2));
					Matcher m = p.matcher(output);
					while (m.find()) {
						try {
							// System.out.println(m.group(1));
							String entitle = m.group(1);

							names.add(entitle.toLowerCase());

						} catch (Exception e) {
							// TODO: handle exception
						}
					}

					HashSet<String> anno = annotations.get(instance);
					for (String a : anno) {
						if (names.contains(a)) {
							correct++;
						} else {
							incorrect++;
						}
					}
					c++;
					// if (c > 2000) {
					// break;
					// }
				} catch (Exception e) {
					// TODO: handle exception
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
			e.printStackTrace();
		}

	}
}
