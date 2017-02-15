package edu.yeditepe.discovery;

import info.bliki.wiki.dump.IArticleFilter;
import info.bliki.wiki.dump.Siteinfo;
import info.bliki.wiki.dump.WikiArticle;
import info.bliki.wiki.dump.WikiXMLParser;
import info.bliki.wiki.filter.PlainTextConverter;
import info.bliki.wiki.model.WikiModel;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import edu.yeditepe.nlp.TurkishNLP;
import edu.yeditepe.nlp.Zemberek;
import edu.yeditepe.utils.Property;

public class PrepareWord2VecTrainingData {
	private static final Logger LOGGER = Logger
			.getLogger(PrepareWord2VecTrainingData.class);

	private static BufferedWriter bw;

	public static void main(String[] args) throws Exception {

		LOGGER.info(Zemberek.getInstance().disambiguate(
				"Istanbul'da bayram var."));
		// prepareWordtoVec();
		tolowercase();
	}

	private static void tolowercase() throws IOException {
		File outFile = new File("discovery_word2vec_training_lower.txt");

		// if file doesnt exists, then create it
		if (!outFile.exists()) {
			outFile.createNewFile();
		}

		FileWriter fw = new FileWriter(outFile.getAbsoluteFile(), false);
		bw = new BufferedWriter(fw);

		FileInputStream fstream = new FileInputStream(
				"discovery_word2vec_training.txt");
		BufferedReader br = new BufferedReader(new InputStreamReader(fstream));

		String strLine;
		while ((strLine = br.readLine()) != null) {
			bw.write(TurkishNLP.toLowerCase(strLine) + "\n");
		}
		br.close();
		bw.close();
	}

	public static void prepareWordtoVec() {
		try {

			File outFile = new File("discovery_word2vec_training.txt");

			// if file doesnt exists, then create it
			if (!outFile.exists()) {
				outFile.createNewFile();
			}

			FileWriter fw = new FileWriter(outFile.getAbsoluteFile(), false);
			bw = new BufferedWriter(fw);

			IArticleFilter handler = new DemoArticleFilter();
			WikiXMLParser wxp = new WikiXMLParser(Property.getInstance().get(
					"wiki.dump"), handler);
			wxp.parse();

			File all = new File("C:\\Milliyet\\docs");
			File[] listOfFiles = all.listFiles();
			for (File file : listOfFiles) {
				if (file.isFile()) {
					String text = parseDoc(file).replaceAll("\n", "")
							.replaceAll("  ", " ");
					bw.write(Zemberek.getInstance().disambiguate(text) + "\n");
				}
			}

			bw.close();
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	public static String parseDoc(File file) {
		try {
			DocumentBuilderFactory docFactory = DocumentBuilderFactory
					.newInstance();
			DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
			Document xmlDoc = docBuilder.parse(file);

			// Get the staff element by tag name directly
			Node doc = xmlDoc.getElementsByTagName("DOC").item(0);

			// loop the staff child node
			NodeList list = doc.getChildNodes();

			for (int i = 0; i < list.getLength(); i++) {

				Node node = list.item(i);

				// get the salary element, and update the value
				if ("TEXT".equals(node.getNodeName())) {
					return node.getTextContent();
				}

			}

		} catch (Exception pce) {
			pce.printStackTrace();
		}
		return null;

	}

	static class DemoArticleFilter implements IArticleFilter {
		final static Pattern regex = Pattern.compile(
				"[A-Z][\\p{L}\\w\\p{Blank},\\\"\\';\\[\\]\\(\\)-]+[\\.!]",
				Pattern.CANON_EQ);

		// Convert to plain text
		WikiModel wikiModel = new WikiModel("${image}", "${title}");

		public void process(WikiArticle page, Siteinfo siteinfo)
				throws SAXException {

			if (page != null && page.getText() != null
					&& !page.getText().startsWith("#REDIRECT ")) {

				PrintStream out = null;

				try {
					out = new PrintStream(System.out, true, "UTF-8");
				} catch (UnsupportedEncodingException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

				String wikiText = page
						.getText()
						.replaceAll("[=]+[A-Za-z+\\s-]+[=]+", " ")
						.replaceAll("\\{\\{[A-Za-z0-9+\\s-]+\\}\\}", " ")
						.replaceAll("(?m)<ref>.+</ref>", " ")
						.replaceAll(
								"(?m)<ref name=\"[A-Za-z0-9\\s-]+\">.+</ref>",
								" ").replaceAll("<ref>", " <ref>");

				// Remove text inside {{ }}
				String plainStr = wikiModel.render(new PlainTextConverter(),
						wikiText).replaceAll("\\{\\{[A-Za-z+\\s-]+\\}\\}", " ");

				Matcher regexMatcher = regex.matcher(plainStr);
				while (regexMatcher.find()) {
					// Get sentences with 6 or more words
					String sentence = regexMatcher.group();

					if (matchSpaces(sentence, 5)) {
						try {
							bw.write(Zemberek.getInstance().disambiguate(
									sentence)
									+ "\n");
						} catch (Exception e) {
							// TODO Auto-generated catch block
							// e.printStackTrace();
						}
					}
				}

			}
		}

		private boolean matchSpaces(String sentence, int matches) {

			int c = 0;
			for (int i = 0; i < sentence.length(); i++) {
				if (sentence.charAt(i) == ' ')
					c++;
				if (c == matches)
					return true;
			}
			return false;
		}

	}
}
