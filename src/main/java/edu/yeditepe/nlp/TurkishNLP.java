package edu.yeditepe.nlp;

import it.cnr.isti.hpc.text.Token;

import java.util.List;
import java.util.Locale;
import java.util.Set;

import edu.yeditepe.utils.FileUtils;
import edu.yeditepe.utils.Property;

public class TurkishNLP {
	private static Set<String> stopWords;
    public static boolean production= Boolean.parseBoolean(Property.getInstance().get(
            "production"));
	public static boolean isStopWord(String word) {
		if (stopWords == null) {
			stopWords = FileUtils.readFileSet(Property.getInstance().get(
					"turkish.stopwords"));
		}
		return stopWords.contains(word);
	}

	public static List<Token> disambiguate(String sentence) {
		String nlp = Property.getInstance().get("turkish.nlp");
		if (nlp.equals("itu")) {
			return ITUNLP.getInstance().disambiguate(sentence);

		} else {
			return Zemberek.getInstance().disambiguateFindTokens(sentence,
			        production, false);
		}
	}

	public static String disambiguateEntityName(String sentence) {
		return Zemberek.getInstance().morphEntityName(sentence);
	}

	public static String normalizeString(String sentence) {
		return Zemberek.normalize(sentence);
	}

	public static String toLowerCase(String text) {

		return text.toLowerCase(new Locale("tr", "TR"));

	}

	public static String morphPageContent(String content) {
		return Zemberek.getInstance().morphPageContent(content);
	}

}
