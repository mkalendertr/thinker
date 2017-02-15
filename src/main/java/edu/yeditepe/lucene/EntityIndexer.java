package edu.yeditepe.lucene;

import java.io.File;
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

import org.apache.commons.lang3.StringUtils;
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
import com.google.gson.reflect.TypeToken;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;

import edu.yeditepe.model.Entity;
import edu.yeditepe.model.EntityPage;
import edu.yeditepe.nlp.TurkishNLP;
import edu.yeditepe.nlp.Zemberek;
import edu.yeditepe.repository.MONGODB;
import edu.yeditepe.repository.MYSQL;
import edu.yeditepe.utils.Property;
import edu.yeditepe.wiki.Wikipedia;

public class EntityIndexer {
	private static final Logger LOGGER = Logger.getLogger(EntityIndexer.class);

	/** Creates a new instance of Indexer */
	public EntityIndexer() {
	}

	private IndexWriter pageIndexWriter = null;

	public IndexWriter getPageIndexWriter() throws IOException {
		if (pageIndexWriter == null) {
			Directory indexDir = FSDirectory.open(new File(Property
					.getInstance().get("lucene.index")));
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

	public void indexEntities() throws IOException {
		Gson gson = new GsonBuilder().create();

		DBCollection entitiesDB = MONGODB.getCollection(Entity.COLLECTION_NAME);
		DBCursor cursor = entitiesDB.find();
		long counter = 0;
		IndexWriter writer = getPageIndexWriter();
		while (cursor.hasNext()) {
			DBObject object = cursor.next();
			Entity e = convertJSONToPojo(object.toString());
			try {
				// if (e.getId().equals("w253874")) {
				// LOGGER.info("");
				// } else {
				// continue;
				// }

				Document doc = new Document();
				String title_morph = e.getTitle();
				try {
					title_morph = Zemberek.getInstance().getEntityLemma(
							title_morph);
					if (StringUtils.startsWithIgnoreCase(e.getTitle(),
							title_morph)) {
						title_morph = e.getTitle();
					}

				} catch (Exception e2) {
					LOGGER.info(e2);
				}

				List<String> aliases = e.getAlias();
				String aliasString = "";
				String aliasMorphString = "";
				if (aliases != null) {
					for (String alias : aliases) {
						aliasString += toLuceneString(alias) + " ";

						try {
							String aliasMorph = Zemberek.getInstance()
									.getEntityLemma(alias);
							if (!StringUtils.startsWithIgnoreCase(alias,
									aliasMorph)) {
								aliasMorphString += toLuceneString(aliasMorph)
										+ " ";
							}
							// else {
							// LOGGER.info("");
							// }
						} catch (Exception e2) {
							// TODO: handle exception
						}

					}
				}
				aliasMorphString += aliasString;
				aliasString = aliasString.trim();
				aliasMorphString = aliasMorphString.trim();

				String suffixString = "";
				if (e.getSuffixes() != null) {
					for (String suffix : e.getSuffixes()) {
						suffixString += suffix + " ";
					}
					suffixString = suffixString.trim();
				}
				String linkString = "";
				List<String> links = e.getLinks();
				if (links != null) {
					for (String link : links) {
						linkString += link + " ";
					}
				}
				linkString = linkString.trim();

				List<Double> e1 = e.getSemanticEmbeddingWord2Vec();
				List<Double> e2 = e.getSemanticEmbeddingAutoencoder();
				List<Double> e3 = e.getDescriptionEmbeddingAverage();
				List<Double> e4 = e.getDescriptionEmbeddingHash2();

				doc.add(new StringField("id", e.getId(), Field.Store.YES));
				doc.add(new StringField("title", e.getTitle(), Field.Store.YES));
				if (e.getDescription() == null) {
					doc.add(new StringField("description", "", Field.Store.YES));
				} else {
					doc.add(new StringField("description", e.getDescription(),
							Field.Store.YES));
				}
				doc.add(new StringField("suffix", suffixString, Field.Store.YES));
				doc.add(new StringField("title_morph",
						toLuceneString(title_morph), Field.Store.YES));
				doc.add(new TextField("alias", aliasString, Field.Store.YES));
				doc.add(new TextField("alias_morph", aliasMorphString,
						Field.Store.YES));
				doc.add(new StringField("type", toLuceneString(e.getType()),
						Field.Store.YES));
				doc.add(new StringField("domain",
						toLuceneString(e.getDomain()), Field.Store.YES));
				if (e.getUrl() == null) {
					doc.add(new StringField("url_title", "", Field.Store.YES));
				} else {
					doc.add(new StringField("url_title", e.getUrl(),
							Field.Store.YES));
				}
				doc.add(new DoubleField("rank", e.getRank(), Field.Store.YES));
				doc.add(new IntField("case", e.getLetterCase(), Field.Store.YES));
				doc.add(new StringField("links", linkString, Field.Store.YES));
				if (e1 != null)
					doc.add(new StringField("sword2vec", gson.toJson(e1),
							Field.Store.YES));
				if (e2 != null)
					doc.add(new StringField("sautoencoder", gson.toJson(e2),
							Field.Store.YES));
				if (e3 != null)
					doc.add(new StringField("daverage", gson.toJson(e3),
							Field.Store.YES));
				if (e4 != null)
					doc.add(new StringField("dhash", gson.toJson(e4),
							Field.Store.YES));
				writer.addDocument(doc);

			} catch (Exception ex) {
				LOGGER.info(e.getTitle() + " " + ex);
			}
		}
		try {
			closeIndexWriter();
		} catch (IOException e) {
			// TODO Auto-generated catch block
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

	public static void main(String[] args) throws IOException {
		// FileReader reader = new FileReader("viewcounts.json");
		// Gson gson = new GsonBuilder().create();
		// Map<String, Double> viewcounts = gson.fromJson(reader, Map.class);
		// List<Entry<String, Double>> list = entriesSortedByValues(viewcounts);
		// int counter = 0;
		// for (Map.Entry<String, Double> entry : list) {
		// System.out.println("Key : " + entry.getKey() + " Value : "
		// + String.format("%.2f", entry.getValue()));
		// if (counter++ > 10) {
		// break;
		// }
		// }
		EntityIndexer indexer = new EntityIndexer();
		indexer.indexEntities();
		// indexer.rebuildIndexes();
		// addEmbeddings(indexer);
	}

	public static void addEmbeddings(EntityIndexer indexer) {
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