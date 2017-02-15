package edu.yeditepe.lucene;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.apache.log4j.Logger;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.FSDirectory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import edu.yeditepe.nlp.TurkishNLP;
import edu.yeditepe.utils.Property;

public class Word2VecSearchEngine {
	private static final Logger LOGGER = Logger
			.getLogger(Word2VecSearchEngine.class);

	private IndexSearcher pageSearcher = null;

	public IndexSearcher getPageSearcher() {
		return pageSearcher;
	}

	public void setPageSearcher(IndexSearcher pageSearcher) {
		this.pageSearcher = pageSearcher;
	}

	private QueryParser pageParser = null;

	private String fuzzyRate = Property.getInstance().get("lucene.fuzzy");

	private static Word2VecSearchEngine instance = new Word2VecSearchEngine();

	public static Word2VecSearchEngine getInstance() {
		return instance;
	}

	/** Creates a new instance of SearchEngine */
	private Word2VecSearchEngine() {
		try {
			pageSearcher = new IndexSearcher(DirectoryReader.open(FSDirectory
					.open(new File(Property.getInstance()
							.get("lucene.word2vec")))));
			pageParser = new QueryParser("word", new StandardAnalyzer());
		} catch (IOException e) {
			LOGGER.error(e);
		}
	}

	public List<Double> getWordVector(String name) throws IOException,
			ParseException {
		try {
			Gson gson = new GsonBuilder().create();
			Query query = pageParser.parse(QueryParser.escape(name));
			TopDocs search = pageSearcher.search(query, 1);
			ScoreDoc[] hits = search.scoreDocs;

			if (hits.length == 1) {
				Document doc = pageSearcher.doc(hits[0].doc);
				String v = doc.get("vector");

				if (v != null) {
					return gson.fromJson(v, List.class);
				}
			} else {
				query = pageParser.parse(QueryParser.escape(TurkishNLP
						.toLowerCase(name)));
				search = pageSearcher.search(query, 1);
				hits = search.scoreDocs;

				if (hits.length == 1) {
					Document doc = pageSearcher.doc(hits[0].doc);
					String v = doc.get("vector");

					if (v != null) {
						return gson.fromJson(v, List.class);
					}
				}
			}
		} catch (Exception e) {
		}
		return null;

	}

	public static void main(String[] args) throws IOException, ParseException {
		LOGGER.info(instance.getWordVector("Murat"));

	}
}