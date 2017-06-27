package edu.yeditepe.experiment;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
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

public class Wikifier {
	private static final Logger LOGGER = Logger.getLogger(Wikifier.class);

	private static Wikifier wikifier = new Wikifier();

	// Invalid parameter: Usage:
	// GET or POST parameters: {tool}, {input} and {token}
	// token: you can find your token on your login space
	// tool: ner, morphanalyzer, isturkish, morphgenerator, tokenizer,
	// normalize, deasciifier, Vowelizer, depparser, spellcheck, disambiguator,
	// pipeline
	// input: utf-8 string
	// The response is a text/plain encoded in UTF-8

	private Wikifier() {

	}

	public static Wikifier getInstance() {
		return wikifier;
	}

	public static String makeRequest(String text, String filename) {
		try {

			DefaultHttpClient httpclient = new DefaultHttpClient();

			HttpPost post = new HttpPost(
					"http://shelley.cs.illinois.edu:8080/xlwikifier");
			List<NameValuePair> parameters = new ArrayList<NameValuePair>(3);

			parameters.add(new BasicNameValuePair("lang", "Turkish"));
			parameters.add(new BasicNameValuePair("text", text));
			post.setEntity(new UrlEncodedFormEntity(parameters, "UTF-8"));
			HttpResponse resp = httpclient.execute(post);
			String output = EntityUtils.toString(resp.getEntity());
			Writer writer = null;

			try {
				writer = new BufferedWriter(new OutputStreamWriter(
						new FileOutputStream("wikify\\" + filename
								+ "_wikify.txt"), "utf-8"));
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
		Wikifier.getInstance().makeRequest(
				"Yeditepe Üniversitesi, İstanbul'da bulunuyor.", "test");
	}
}
