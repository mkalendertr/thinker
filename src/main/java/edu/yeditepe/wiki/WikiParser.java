package edu.yeditepe.wiki;

import info.bliki.wiki.dump.IArticleFilter;
import info.bliki.wiki.dump.Siteinfo;
import info.bliki.wiki.dump.WikiArticle;
import info.bliki.wiki.dump.WikiXMLParser;
import info.bliki.wiki.filter.PlainTextConverter;
import info.bliki.wiki.model.WikiModel;

import java.io.BufferedWriter;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.xml.sax.SAXException;

import edu.yeditepe.repository.MYSQL;
import edu.yeditepe.utils.Property;

public class WikiParser {
	private static final Logger LOGGER = Logger.getLogger(WikiParser.class);

	private static BufferedWriter bw;

	public static void main(String[] args) throws Exception {
		parse();
	}

	public static void parse() {
		try {

			IArticleFilter handler = new DemoArticleFilter();
			WikiXMLParser wxp = new WikiXMLParser(Property.getInstance().get(
					"wiki.dump"), handler);
			wxp.parse();

		} catch (Exception e) {
			e.printStackTrace();
		}

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

				String wikiText = page.getText();
				// .replaceAll("[=]+[A-Za-z+\\s-]+[=]+", " ")
				// .replaceAll("\\{\\{[A-Za-z0-9+\\s-]+\\}\\}", " ")
				// .replaceAll("(?m)<ref>.+</ref>", " ")
				// .replaceAll(
				// "(?m)<ref name=\"[A-Za-z0-9\\s-]+\">.+</ref>",
				// " ").replaceAll("<ref>", " <ref>");

				// Remove text inside {{ }}
				String plainStr = wikiModel.render(new PlainTextConverter(),
						wikiText);
				// .replaceAll("\\{\\{[A-Za-z+\\s-]+\\}\\}", " ");

				MYSQL.insertWikiContent(Integer.parseInt(page.getId()),
						plainStr);
				// Matcher regexMatcher = regex.matcher(plainStr);
				// while (regexMatcher.find()) {
				// // Get sentences with 6 or more words
				// String sentence = regexMatcher.group();
				//
				// if (matchSpaces(sentence, 5)) {
				// try {
				// bw.write(Zemberek.getInstance().disambiguate(
				// sentence)
				// + "\n");
				// } catch (Exception e) {
				// // TODO Auto-generated catch block
				// // e.printStackTrace();
				// }
				// }
				// }

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
