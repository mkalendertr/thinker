package edu.yeditepe.lucene;

import it.cnr.isti.hpc.dexter.common.ArticleDescription;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.FSDirectory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import edu.yeditepe.model.EntityPage;
import edu.yeditepe.similarity.EntityEmbeddingSimilarity;
import edu.yeditepe.utils.Property;
import edu.yeditepe.wiki.Wikipedia;

public class EntitySearchEngine {
	private static final Logger LOGGER = Logger
			.getLogger(EntitySearchEngine.class);

	private IndexSearcher pageSearcher = null;

	public IndexSearcher getPageSearcher() {
		return pageSearcher;
	}

	public void setPageSearcher(IndexSearcher pageSearcher) {
		this.pageSearcher = pageSearcher;
	}

	private QueryParser pageParser = null;

	private String fuzzyRate = Property.getInstance().get("lucene.fuzzy");

	private int searchNum = Integer.parseInt(Property.getInstance().get(
			"lucene.searchNum"));
	private static int maxSense = Integer.parseInt(Property.getInstance().get(
			"tdk.maxsense"));
	private static EntitySearchEngine instance = new EntitySearchEngine();

	public static EntitySearchEngine getInstance() {
		return instance;
	}

	/** Creates a new instance of SearchEngine */
	private EntitySearchEngine() {
		try {
			pageSearcher = new IndexSearcher(
					DirectoryReader.open(FSDirectory.open(new File(Property
							.getInstance().get("lucene.index")))));
			pageParser = new QueryParser(Property.getInstance().get(
					"lucene.searchField"), new WhitespaceAnalyzer());
			// File folder = new File(
			// (Property.getInstance().get("featuresDirectory")));
			// File[] listOfFiles = folder.listFiles();
			// entities = new HashSet<Integer>();
			// for (File file : listOfFiles) {
			// if (file.isFile()) {
			// String key = file.getName().replace(".bat", "");
			// entities.add(Integer.parseInt(key));
			// }
			// }
		} catch (IOException e) {
			LOGGER.error(e);
		}
	}

	public TopDocs performExactSearch(String queryString, String queryString2)
			throws IOException, ParseException {
		// Query query = parser.parse(queryString + "*");
		LOGGER.debug("Searching index for spot " + queryString + " // "
				+ queryString2);
		String s = QueryParser.escape(queryString);
		if (queryString2 != null) {
			s += " " + QueryParser.escape(queryString2);
		}
		Query query = pageParser.parse(s);
		SortField field = new SortField("id", SortField.Type.STRING);
		// SortField field = new SortField("url_title", SortField.Type.STRING,
		// true);
		Sort sort = new Sort(field);
		// LOGGER.info(pageSearcher.explain(query, n));
		return pageSearcher.search(query, searchNum, sort);

	}

	public List<EntityPage> performExactEntitySearch(String queryString,
			String queryString2) throws IOException, ParseException {
		Set<String> tdkSet = new HashSet<String>();
		Gson gson = new GsonBuilder().create();
		List<EntityPage> pages = new ArrayList<EntityPage>();
		TopDocs topDocs = performExactSearch(queryString, queryString2);
		ScoreDoc[] hits = topDocs.scoreDocs;
		for (int i = 0; i < hits.length; i++) {
			Document doc = instance.getPageSearcher().doc(hits[i].doc);
			String id = doc.get("id");
			String title = doc.get("title");
			String description = doc.get("description");
			String suffix = doc.get("suffix");
			String type = doc.get("type");
			String urlTitle = doc.get("url_title");
			String enTitle = doc.get("en_title");
			double rank = Double.parseDouble(doc.get("rank"));
			String alias = doc.get("alias").replace("_", " ");

			int lettercase = Integer.parseInt(doc.get("case"));
			String links = doc.get("links");
			String titleMorph = doc.get("title_morph");
			String domain = doc.get("domain");
			// String content = doc.get("content");
			// String referenceContent = doc.get("reference_content");
			String w = doc.get("sword2vec");
			List<Double> e1 = null, e2 = null, e3 = null, e4 = null;
			if (w != null) {
				e1 = gson.fromJson(w, List.class);

			}
			String a = doc.get("sautoencoder");
			if (a != null) {
				e2 = gson.fromJson(a, List.class);

			}
			a = doc.get("daverage");
			if (a != null) {
				e3 = gson.fromJson(a, List.class);

			}
			a = doc.get("dhash");
			if (a != null) {
				e4 = gson.fromJson(a, List.class);

			}

			int sense = Character.getNumericValue(id.charAt(id.length() - 1));
			String tdkId = id.substring(0, id.length() - 1);
			if (id.startsWith("w") || !tdkSet.contains(tdkId)) {
				EntityPage page = new EntityPage(id, title, type, titleMorph,
						urlTitle, enTitle, description, suffix, rank,
						lettercase, links, alias, domain, e1, e2, e3, e4);
				tdkSet.add(tdkId);
				if (id.startsWith("t")
						&& Character.isUpperCase(title.charAt(0))) {
					continue;
				}
				pages.add(page);
			}

		}
		return pages;
	}

	public TopDocs performFuzzySearch(String queryString, int n, boolean prefix)
			throws IOException, ParseException {
		// Query query = parser.parse(queryString + "*");
		LOGGER.debug("Searching index for spot " + queryString);
		Query query = pageParser.parse(QueryParser.escape(queryString) + "~"
				+ fuzzyRate);
		SortField field = new SortField("rank", SortField.Type.DOUBLE, true);
		// SortField field = new SortField("url_title", SortField.Type.STRING,
		// true);
		Sort sort = new Sort(field);
		return pageSearcher.search(query, n, sort);

	}

	public ArticleDescription getPage(String id) {
		try {
			// IndexSearcher searcher = new IndexSearcher(
			// DirectoryReader.open(FSDirectory.open(new File(Property
			// .getInstance().get("lucene.index")
			// + "_without_embedding"))));

			IndexSearcher searcher = instance.getPageSearcher();
			QueryParser pageParser = new QueryParser("id",
					new WhitespaceAnalyzer());
			Query query = pageParser.parse(QueryParser.escape(id));
			TopDocs topDocs = pageSearcher.search(query, 1);

			ScoreDoc[] hits = topDocs.scoreDocs;
			Document doc = searcher.doc(hits[0].doc);
			String pageTitle = doc.get("title");
			String url_title = doc.get("url_title");
			String description = doc.get("description");
			ArticleDescription desc = new ArticleDescription();
			desc.setId(0);
			desc.setTitle(pageTitle);
			desc.setDescription(description);
			if (url_title != null && url_title.length() > 0) {
				desc.setUri("http://tr.wikipedia.org/wiki/" + url_title);// +
																			// URLEncoder.encode(url_title,
																			// "UTF-8")
			} else {
				// desc.setUri(pageTitle + " : " + description);
				desc.setUri("TDK");
			}

			return desc;
		} catch (Exception e) {
			// TODO: handle exception
		}

		return null;
	}

	public Document getPagebyURLTitle(String title) {
		try {
			// IndexSearcher searcher = new IndexSearcher(
			// DirectoryReader.open(FSDirectory.open(new File(Property
			// .getInstance().get("lucene.index")
			// + "_without_embedding"))));

			IndexSearcher searcher = instance.getPageSearcher();
			QueryParser pageParser = new QueryParser("url_title",
					new WhitespaceAnalyzer());
			Query query = pageParser.parse(QueryParser.escape(title));
			TopDocs topDocs = pageSearcher.search(query, 1);

			ScoreDoc[] hits = topDocs.scoreDocs;
			Document doc = searcher.doc(hits[0].doc);
			return doc;

		} catch (Exception e) {
			// TODO: handle exception
		}

		return null;
	}

	public TreeMap<Double, String> getSimilarEntities(String urlTitle) {
		Query query = new MatchAllDocsQuery();
		TreeMap<Double, String> map = new TreeMap<Double, String>();
		try {

			Gson gson = new GsonBuilder().create();

			Document doc1 = getPagebyURLTitle(urlTitle.replaceAll(" ", "_"));
			String id1 = doc1.get("id");
			String urlTitle1 = doc1.get("url_title");
			String w = doc1.get("sword2vec");
			List<Double> e1 = null, e2 = null, e3 = null, e4 = null;
			if (w != null) {
				e1 = gson.fromJson(w, List.class);

			} else {
				return null;
			}
			String a = doc1.get("sautoencoder");
			if (a != null) {
				e2 = gson.fromJson(a, List.class);

			} else {
				return null;
			}
			EntityPage p1 = new EntityPage();
			p1.setId(id1);
			p1.setAutoencoder(e2);
			p1.setWord2vec(e1);
			TopDocs topDocs = instance.getPageSearcher().search(query, 1000000);
			ScoreDoc[] hits = topDocs.scoreDocs;
			for (int i = 0; i < hits.length; i++) {
				Document doc = instance.getPageSearcher().doc(hits[i].doc);
				String id2 = doc.get("id");
				String urlTitle2 = doc.get("url_title");
				w = doc.get("sword2vec");
				if (w != null) {
					e3 = gson.fromJson(w, List.class);

				}
				a = doc.get("sautoencoder");
				if (a != null) {
					e4 = gson.fromJson(a, List.class);

				}
				EntityPage p2 = new EntityPage();
				p2.setId(id2);
				p2.setAutoencoder(e4);
				p2.setWord2vec(e3);
				double sim = EntityEmbeddingSimilarity.getInstance()
						.getSimilarity(p1, p2);
				if (sim > 0) {
					if (map.size() < 20) {
						map.put(sim, urlTitle2);
					} else {
						if (map.firstKey() < sim) {
							map.remove(map.firstKey());
							map.put(sim, urlTitle2);
						}
					}
				}
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return map;

	}

	public String getPagebyId(String id) {
		try {
			// IndexSearcher searcher = new IndexSearcher(
			// DirectoryReader.open(FSDirectory.open(new File(Property
			// .getInstance().get("lucene.index")
			// + "_without_embedding"))));

			IndexSearcher searcher = instance.getPageSearcher();
			QueryParser pageParser = new QueryParser("id",
					new WhitespaceAnalyzer());
			Query query = pageParser.parse(QueryParser.escape(id));
			TopDocs topDocs = pageSearcher.search(query, 1);

			ScoreDoc[] hits = topDocs.scoreDocs;
			Document doc = searcher.doc(hits[0].doc);
			return doc.get("url_title");

		} catch (Exception e) {
			// TODO: handle exception
		}

		return null;
	}

	public static void main(String[] args) throws IOException, ParseException {
		String query = "vakıf_üniversite";
		// query = "ABD";
		// query = "ada";
		// query = "ara";
		instance.performExactEntitySearch(
				Wikipedia.stringToLuceneString(query), null);
		ArticleDescription ad = instance.getPage("w111757");

	}
}