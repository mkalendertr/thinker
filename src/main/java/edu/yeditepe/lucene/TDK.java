package edu.yeditepe.lucene;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.Reader;
import java.util.HashSet;

import org.apache.log4j.Logger;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import edu.yeditepe.nlp.TurkishNLP;

public class TDK {
	private static final Logger LOGGER = Logger.getLogger(TDK.class);

	private static TDK tdk = new TDK();
	private HashSet<String> sozluk = new HashSet<String>();
	private HashSet<String> isimler = new HashSet<String>();

	private TDK() {
		Reader reader1;
		try {
			reader1 = new FileReader("tdk.json");
			Gson gson1 = new GsonBuilder().create();
			sozluk = gson1.fromJson(reader1, HashSet.class);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		try {
			reader1 = new FileReader("isimler.json");
			Gson gson1 = new GsonBuilder().create();
			isimler = gson1.fromJson(reader1, HashSet.class);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}

	public static TDK getInstance() {
		return tdk;
	}

	public boolean isTurkishWord(String text) {
		text = TurkishNLP.disambiguateEntityName(text);
		return sozluk.contains(text);
	}

	public boolean isName(String text) {
		text = TurkishNLP.toLowerCase(text);
		return isimler.contains(text);
	}
}
