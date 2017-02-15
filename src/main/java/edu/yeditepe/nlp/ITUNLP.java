package edu.yeditepe.nlp;

import it.cnr.isti.hpc.text.Token;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.params.ConnRoutePNames;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;

import zemberek.tokenizer.SentenceBoundaryDetector;
import zemberek.tokenizer.SimpleSentenceBoundaryDetector;
import edu.yeditepe.component.ApplicationContextHolder;
import edu.yeditepe.model.ITUAPIModel;
import edu.yeditepe.model.Sentence;
import edu.yeditepe.repository.MongoITURepository;
import edu.yeditepe.service.MongoService;
import edu.yeditepe.utils.Property;

public class ITUNLP {
	private static final Logger LOGGER = Logger.getLogger(ITUNLP.class);

	private static ITUNLP ituNLP = new ITUNLP();

	// Invalid parameter: Usage:
	// GET or POST parameters: {tool}, {input} and {token}
	// token: you can find your token on your login space
	// tool: ner, morphanalyzer, isturkish, morphgenerator, tokenizer,
	// normalize, deasciifier, Vowelizer, depparser, spellcheck, disambiguator,
	// pipeline
	// input: utf-8 string
	// The response is a text/plain encoded in UTF-8

	private ITUNLP() {

	}

	public static ITUNLP getInstance() {
		return ituNLP;
	}

	public List<Sentence> processText(String input) {
		LOGGER.info("Input: " + input);
		List<String> sentences = splitSentences(input);
		LOGGER.info("Sentences:\n");
		List<Sentence> resultList = new ArrayList<Sentence>();
		for (String sentence : sentences) {
			LOGGER.info(sentence);
			// Tokenization
			String tokenizerResult = tokenizer(sentence);

			// Morphological Analysis
			String morf = "";
			String[] tokens = StringUtils
					.split(tokenizerResult, StringUtils.LF);
			for (String token : tokens) {
				String morphanalyzerResult = morphanalyzer(token).replace(
						StringUtils.LF, StringUtils.SPACE);
				morf += token + " " + morphanalyzerResult + StringUtils.LF;
			}
			morf += "</S> </S>+ESTag";

			// Morphological Disambiguation
			String disambiguatorResult = disambiguator(morf);
			LOGGER.info("Disambiguator result\n" + disambiguatorResult);

			// Ner
			String nerResult = ner(disambiguatorResult);
			LOGGER.info("Ner result\n" + nerResult);

			String pipelineResult = pipeline(sentence);
			LOGGER.info("Pipeline result\n" + pipelineResult);

			Sentence s = new Sentence(sentence, nerResult, pipelineResult,
					getTotalResult(nerResult, pipelineResult));
			resultList.add(s);

			// LOGGER.info("Turkish LP result\n" + pipelineList);
		}

		return resultList;
	}

	public static String getTotalResult(String nerResult, String pipelineResult) {

		String[] pipelineList = StringUtils.split(pipelineResult,
				StringUtils.LF);
		String[] nerList = StringUtils.split(nerResult, StringUtils.CR
				+ StringUtils.LF);
		HashSet<String> set = new HashSet<String>();
		for (String word : nerList) {
			try {
				String label = word.split("\t\t")[1];
				word = word.substring(0, word.indexOf(" "));
				if (label.startsWith("B-")) {
					label = label.replace("B-", "");
					for (int i = 0; i < pipelineList.length; i++) {
						if (pipelineList[i].contains(word)
								&& !set.contains(pipelineList[i])) {
							pipelineList[i] += "\t" + label;
							set.add(pipelineList[i]);
							break;
						}
					}
				}
			} catch (Exception e) {
				LOGGER.error(e);
			}

		}
		String totalResult = "";
		for (int i = 0; i < pipelineList.length; i++) {
			if (!set.contains(pipelineList[i])) {
				pipelineList[i] += "\tO";
			}
			totalResult += pipelineList[i] + StringUtils.LF;
		}
		return totalResult;
	}

	// Sentence detection
	public static List<String> splitSentences(String text) {

		SentenceBoundaryDetector detector = new SimpleSentenceBoundaryDetector();
		List<String> sentences = detector.getSentences(text);
		List<String> merged = new ArrayList<String>();
		String prevText = "";
		for (String sentence : sentences) {
			if (sentence.contains(".")) {
				merged.add((prevText + sentence).replace("  ", " "));
				prevText = "";
			} else {
				prevText += sentence + StringUtils.SPACE;
			}
		}
		// String[] input = new String[1];
		// input[0] = text;
		// SentenceSplitter splitter = new SentenceSplitter(input);
		//
		// List<LBJ2.nlp.Sentence> s = Arrays.asList(splitter.splitAll());

		return merged;
	}

	public static String pipeline(String text) {
		return makeRequest(text, "pipelineFormal");
	}

	public static String spellcheck(String text) {
		return makeRequest(text, "spellcheck");
	}

	public static String deasciifier(String text) {
		return makeRequest(text, "deasciifier");
	}

	public static String vowelizer(String text) {
		return makeRequest(text, "Vowelizer");
	}

	public static String disambiguator(String text) {
		return makeRequest(text, "disambiguator");
	}

	public static String tokenizer(String text) {
		return makeRequest(text, "tokenizer");
	}

	public static String morphanalyzer(String text) {
		return makeRequest(text, "morphanalyzer");
	}

	public static String isturkish(String text) {
		return makeRequest(text, "isturkish");
	}

	public static String morphgenerator(String text) {
		return makeRequest(text, "morphgenerator");
	}

	public static String normalize(String text) {
		return makeRequest(text, "normalize");
	}

	public static String depparser(String text) {
		return makeRequest(text, "depparser");
	}

	public static String ner(String text) {
		return makeRequest(text, "ner");
	}

	public static String makeRequest(String input, String tool) {
		try {
			String output = getRequest(tool, input);
			if (output != null) {
				return output;
			}
			DefaultHttpClient httpclient = new DefaultHttpClient();
			if (Property.getInstance().get("proxy.enable").equals("true")) {
				httpclient.getCredentialsProvider().setCredentials(
						new AuthScope(Property.getInstance().get("proxy.host"),
								Integer.parseInt(Property.getInstance().get(
										"proxy.port"))),
						new UsernamePasswordCredentials(Property.getInstance()
								.get("proxy.name"), Property.getInstance().get(
								"proxy.pass")));
				HttpHost proxy = new HttpHost(Property.getInstance().get(
						"proxy.host"), Integer.parseInt(Property.getInstance()
						.get("proxy.port")));
				httpclient.getParams().setParameter(
						ConnRoutePNames.DEFAULT_PROXY, proxy);
			}

			HttpPost post = new HttpPost(
					"http://tools.nlp.itu.edu.tr/SimpleApi");
			List<NameValuePair> parameters = new ArrayList<NameValuePair>(3);

			parameters.add(new BasicNameValuePair("tool", tool));
			parameters.add(new BasicNameValuePair("input", input));
			parameters.add(new BasicNameValuePair("token", Property
					.getInstance().get("turkish.itu.key")));
			post.setEntity(new UrlEncodedFormEntity(parameters, "UTF-8"));
			HttpResponse resp = httpclient.execute(post);
			output = EntityUtils.toString(resp.getEntity());
			saveRequest(tool, input, output);
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

	public List<Token> disambiguate(String sentence) {
		try {

			// Tokenization
			String disambiguatorResult = "";

			String tokenizerResult = tokenizer(sentence);

			// Morphological Analysis
			String morf = "";
			String[] tokens = StringUtils
					.split(tokenizerResult, StringUtils.LF);
			for (String token : tokens) {
				String morphanalyzerResult = morphanalyzer(token).replace(
						StringUtils.LF, StringUtils.SPACE);
				morf += token + " " + morphanalyzerResult + StringUtils.LF;
			}
			morf += "</S> </S>+ESTag";

			// Morphological Disambiguation
			disambiguatorResult = disambiguator(morf);

			String[] words = disambiguatorResult.split(StringUtils.LF);
			List<Token> tokensList = new ArrayList<Token>();
			int start = 0;
			int end = 0;
			for (String word : words) {
				String[] temp = word.split(" ");
				String originalText = temp[0];
				String text = temp[1].substring(0, temp[1].indexOf('+'));
				text = originalText.charAt(0) + text.substring(1);
				String morph = temp[1].substring(temp[1].indexOf('+') + 1);
				start = sentence.indexOf(originalText, start);
				Token t = new Token(text, originalText, morph, "",
						sentence.indexOf(originalText, start),
						start += originalText.length(), null);
				tokensList.add(t);
			}
			LOGGER.info("Disambiguator result\n" + disambiguatorResult);

			return tokensList;

		} catch (Exception e) {

		}
		return null;
	}

	public static void saveRequest(String function, String input, String output) {
		try {
			MongoService mongo = ApplicationContextHolder.getContext().getBean(
					MongoService.class);
			MongoITURepository ituRepo = (MongoITURepository) mongo
					.getRepository("ITU");
			ITUAPIModel itu = new ITUAPIModel(function, input, output);
			ituRepo.save(itu);
			// itu = ituRepo.findByFunctionAndInput("tokenizer", "test");
			LOGGER.info("ITU Request is saved");
		} catch (Exception e) {
			LOGGER.error("ITU Request is not saved");
		}

	}

	public static String getRequest(String function, String input) {
		try {
			MongoService mongo = ApplicationContextHolder.getContext().getBean(
					MongoService.class);
			MongoITURepository ituRepo = (MongoITURepository) mongo
					.getRepository("ITU");
			ITUAPIModel itu = ituRepo.findByFunctionAndInput(function, input);
			if (itu != null) {
				return itu.getOutput();

			}
		} catch (Exception e) {
			LOGGER.error("mongo repo can not be searched");
		}
		return null;
	}

	public static void main(String[] args) {
		ITUNLP.getInstance().pipeline(
				"Yeditepe Üniversitesi, İstanbul'da bulunuyor.");
	}
}
