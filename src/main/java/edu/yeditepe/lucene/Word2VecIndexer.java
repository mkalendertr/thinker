package edu.yeditepe.lucene;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.DoubleField;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.IntField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.internal.LinkedTreeMap;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;

import edu.yeditepe.model.Entity;
import edu.yeditepe.model.EntityPage;
import edu.yeditepe.nlp.TurkishNLP;
import edu.yeditepe.repository.MYSQL;
import edu.yeditepe.utils.Property;
import edu.yeditepe.wiki.Wikipedia;

public class Word2VecIndexer {
	private static final Logger LOGGER = Logger
			.getLogger(Word2VecIndexer.class);
	private IndexWriter pageIndexWriter = null;
	private static LinkedTreeMap<String, double[]> vectors;
	private static int vectorSize = 100;

	/** Creates a new instance of Indexer */
	public Word2VecIndexer() {
	}

	public static void main(String[] args) throws IOException {
		Word2VecIndexer indexer = new Word2VecIndexer();
		indexer.indexWordVecs();
		// indexer.rebuildIndexes();
		// addEmbeddings(indexer);
	}

	public IndexWriter getPageIndexWriter() throws IOException {
		if (pageIndexWriter == null) {
			Directory indexDir = FSDirectory.open(new File(Property
					.getInstance().get("lucene.word2vec")));
			IndexWriterConfig config = new IndexWriterConfig(
					Version.LUCENE_4_10_3, new WhitespaceAnalyzer());
			pageIndexWriter = new IndexWriter(indexDir, config);
		}
		return pageIndexWriter;
	}

	public void closeIndexWriter() throws IOException {

		if (pageIndexWriter != null) {
			pageIndexWriter.close();
		}
	}

	public void indexWordVecs() throws IOException {
		Gson gson = new GsonBuilder().create();
		vectors = new LinkedTreeMap<String, double[]>();
		IndexWriter writer = getPageIndexWriter();

		try {
			JsonReader reader = new JsonReader(new FileReader(Property
					.getInstance().get("word2vec.modelfile")));

			reader.beginObject();

			while (reader.hasNext()) {

				String word = reader.nextName();
				Document doc = new Document();
				doc.add(new StringField("word", word, Field.Store.YES));

				// read array
				reader.beginArray();
				ArrayList<Double> vector = new ArrayList<Double>(vectorSize);
				while (reader.hasNext()) {
					vector.add(reader.nextDouble());
				}
				doc.add(new StringField("vector", gson.toJson(vector),
						Field.Store.YES));
				writer.addDocument(doc);
				reader.endArray();

			}
			closeIndexWriter();

			reader.endObject();
			reader.close();

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	public void indexWikiPage(EntityPage page, boolean upperCase, int length,
			String links, double viewcount, String type, String domain,
			String content, String referenceContent) throws IOException {

		// LOGGER.info("Indexing wikipage: " + page.getTitle());
		try {
			IndexWriter writer = getPageIndexWriter();
			Document doc = new Document();
			doc.add(new StringField("id", page.getId(), Field.Store.YES));
			doc.add(new StringField("title", page.getTitle(), Field.Store.YES));

			String title_morph = TurkishNLP.disambiguateEntityName(page
					.getTitle());
			if (title_morph == null) {
				title_morph = page.getTitle();
			}
			title_morph = Wikipedia.stringToLuceneString(title_morph);
			doc.add(new StringField("title_morph", title_morph, Field.Store.YES));
			doc.add(new StringField("type", toLuceneString(type),
					Field.Store.YES));
			doc.add(new StringField("domain", toLuceneString(domain),
					Field.Store.YES));
			doc.add(new StringField("url_title", page.getUrlTitle(),
					Field.Store.YES));
			doc.add(new StringField("en_title", page.getEnTitle(),
					Field.Store.YES));
			doc.add(new DoubleField("pagerank", page.getRank(), Field.Store.YES));
			doc.add(new DoubleField("viewcount", viewcount, Field.Store.YES));
			doc.add(new TextField("alias", page.getAlias(), Field.Store.YES));
			doc.add(new TextField("alias_morph", page.getAliasMorph(),
					Field.Store.YES));
			doc.add(new StringField("upper", String.valueOf(upperCase),
					Field.Store.YES));
			doc.add(new IntField("length", length, Field.Store.YES));
			if (links.length() >= 32766) {
				links = links.substring(0, 32000);
			}
			doc.add(new StringField("links", links, Field.Store.YES));
			// doc.add(new StringField("content", toLuceneString(content),
			// Field.Store.YES));
			// doc.add(new StringField("reference_content",
			// toLuceneString(referenceContent), Field.Store.YES));
			writer.addDocument(doc);
		} catch (Exception e) {
			LOGGER.error(e);
		}

	}

	public void rebuildIndexes() throws IOException {

		HashMap<String, String> types = null;
		Map<String, Double> viewcounts;
		HashMap<String, String> contents = null;
		HashMap<String, String> references = null;
		HashMap<Integer, String> linksMap = null;
		Wikipedia wikipedia = new Wikipedia();
		//
		// Reader reader1 = new FileReader("content.json");
		// Gson gson1 = new GsonBuilder().create();
		// contents = gson1.fromJson(reader1, HashMap.class);
		// for (String i : contents.keySet()) {
		// LOGGER.info(i + " : " + contents.get(i));
		// }
		//
		// reader1 = new FileReader("reference.json");
		// gson1 = new GsonBuilder().create();
		// references = gson1.fromJson(reader1, HashMap.class);
		// for (String i : references.keySet()) {
		// LOGGER.info(i + " : " + references.get(i));
		// }

		boolean processText = false;
		wikipedia.process(processText);

		Collection<EntityPage> pages = MYSQL.getPages(wikipedia);

		// linksMap = new HashMap<Integer, String>();
		// for (WikiPage page : pages) {
		// String links = MYSQL.getLinks(page.getId());
		// linksMap.put(page.getId(), links);
		//
		// }
		// Writer writer;
		// try {
		// writer = new FileWriter("link.json");
		// Gson gson = new GsonBuilder().create();
		// gson.toJson(linksMap, writer);
		//
		// writer.close();
		// } catch (IOException e) {
		// // TODO Auto-generated catch block
		// e.printStackTrace();
		// }

		if (processText) {
			// types = Wikipedia.getTypes();
			// linksMap = new HashMap<Integer, String>();
			// for (WikiPage page : pages) {
			// String links = MYSQL.getLinks(page.getId());
			// linksMap.put(page.getId(), links);
			//
			// }
			// Writer writer;
			// try {
			// writer = new FileWriter("link.json");
			// Gson gson = new GsonBuilder().create();
			// gson.toJson(linksMap, writer);
			//
			// writer.close();
			// } catch (IOException e) {
			// // TODO Auto-generated catch block
			// e.printStackTrace();
			// }
			contents = wikipedia.getPageContent();
			references = wikipedia.getPageReferenceContent();
		} else {

			Reader reader = new FileReader("reference.json");
			Gson gson = new GsonBuilder().create();
			references = gson.fromJson(reader, HashMap.class);

			reader = new FileReader("content.json");
			gson = new GsonBuilder().create();
			contents = gson.fromJson(reader, HashMap.class);

		}

		Reader reader = new FileReader("domain.json");
		Gson gson = new GsonBuilder().create();
		Map<String, String> domains = gson.fromJson(reader, Map.class);

		// reader = new FileReader("link.json");
		// gson = new GsonBuilder().create();
		// linksMap = gson.fromJson(reader, HashMap.class);

		reader = new FileReader("viewcounts.json");
		gson = new GsonBuilder().create();
		viewcounts = gson.fromJson(reader, Map.class);

		reader = new FileReader("type.json");
		gson = new GsonBuilder().create();
		types = gson.fromJson(reader, HashMap.class);

		HashSet<String> ambiguity = Wikipedia.getDisambiguation();
		HashSet<String> uppercase = Wikipedia.getUpperCasePages();
		HashMap<String, Integer> pageLength = Wikipedia.getPageLength();

		for (EntityPage page : pages) {
			String id = String.valueOf(page.getId());
			// if (id.equals("691538")) {
			// System.out.println();
			// }
			if (!ambiguity.contains(id)) {

				// String links = linksMap.get(id);
				String links = null; // MYSQL.getOutgoingLinks(page.getId());
				if (links == null) {
					links = "";
				}
				page.setLinks(links);
				int length = 0;
				if (pageLength.containsKey(id)) {
					length = pageLength.get(id);
				}
				boolean upper = uppercase.contains(id);
				double viewcount = 0;
				if (viewcounts.containsKey(page.getUrlTitle())) {
					viewcount = viewcounts.get(page.getUrlTitle());
				}
				String type = "";
				if (types.containsKey(id)) {
					type = types.get(id);
				}

				String domain = domains.get(page.getEnTitle().toLowerCase()
						.replace(" ", "_"));
				if (domain == null) {
					domain = "";
				}
				String content = "";
				try {
					if (contents.containsKey(id)) {
						content = contents.get(id);
						// LOGGER.info("content : " + content);

					}

				} catch (Exception e) {
					LOGGER.error(e);
				}
				String referenceContent = "";
				try {
					String title = page.getTitle();
					if (references.containsKey(title)) {
						referenceContent = references.get(page.getUrlTitle());
						// LOGGER.info("reference : " + referenceContent);

					}

				} catch (Exception e) {
					LOGGER.error(e);
				}

				indexWikiPage(page, upper, length, links, viewcount, type,
						domain, content, referenceContent);

			}

		}

		closeIndexWriter();
	}

	public static String toLuceneString(String text) {
		if (text == null) {
			return "";
		}
		text = text.toLowerCase(new Locale("tr", "TR"));
		text = text.replace(" ", "_");

		return text;
	}

	public static void addEmbeddings(Word2VecIndexer indexer) {
		try {

			Reader freader = new FileReader("word2vec150.json");
			Gson gson = new GsonBuilder().create();
			HashMap<String, List<Double>> word2vec = gson.fromJson(freader,
					HashMap.class);

			freader = new FileReader("autoencoder300.json");
			gson = new GsonBuilder().create();
			HashMap<String, List<Double>> autoencoder = gson.fromJson(freader,
					HashMap.class);
			LOGGER.info("embeddings are loaded");

			IndexWriter writer = indexer.getPageIndexWriter();
			Directory indexDir = FSDirectory.open(new File(Property
					.getInstance().get("lucene.index") + "_embedding"));
			IndexWriterConfig config = new IndexWriterConfig(
					Version.LUCENE_4_10_3, new WhitespaceAnalyzer());
			IndexWriter pageIndexWriter = new IndexWriter(indexDir, config);

			IndexSearcher reader = new IndexSearcher(
					DirectoryReader.open(FSDirectory.open(new File(Property
							.getInstance().get("lucene.index")))));
			int maxDoc = writer.maxDoc();
			for (int i = 0; i < maxDoc; i++) {
				Document d = reader.doc(i);
				String id = d.get("id");
				LOGGER.info(d.get("id"));
				List<Double> e1 = word2vec.get(id);
				List<Double> e2 = autoencoder.get(id);
				if (e1 != null && e2 != null) {
					d.add(new StringField("word2vec", gson.toJson(e1),
							Field.Store.YES));
					d.add(new StringField("autoencoder", gson.toJson(e2),
							Field.Store.YES));
					pageIndexWriter.addDocument(d);
				}
				// break;
			}
			writer.close();
			pageIndexWriter.close();
		} catch (Exception e) {
			// TODO: handle exception
		}
	}

	static <K, V extends Comparable<? super V>> List<Entry<K, V>> entriesSortedByValues(
			Map<K, V> map) {

		List<Entry<K, V>> sortedEntries = new ArrayList<Entry<K, V>>(
				map.entrySet());

		Collections.sort(sortedEntries, new Comparator<Entry<K, V>>() {
			@Override
			public int compare(Entry<K, V> e1, Entry<K, V> e2) {
				return e2.getValue().compareTo(e1.getValue());
			}
		});

		return sortedEntries;
	}

	private static Entity convertJSONToPojo(String json) {

		Type type = new TypeToken<Entity>() {
		}.getType();

		return new Gson().fromJson(json, type);

	}
}