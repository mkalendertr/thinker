package edu.yeditepe.lucene;

import java.io.IOException;
import java.text.DecimalFormat;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.DocsEnum;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopScoreDocCollector;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.Version;

public class TFIDF {
	public static void main(String[] args) throws IOException, ParseException {

		// some variables
		int N = 0; // count of all documents
		int Nt = 0; // the documents, that contain the searched term t
		int TF = 0; // TF = Term Frequency
		double IDF = 1; // IDF(t) = log(N/Nt)
		double TF_IDF = 0; // TF-IDF(t, doc) = TF(t, doc) * IDF(t)
		String searchCriteria = "art";
		DecimalFormat df = new DecimalFormat("0.000"); // used to round the
														// TF_IDF in the result.

		// 0. Specify the analyzer for tokenizing text.
		// The same analyzer should be used for indexing and searching
		StandardAnalyzer analyzer = new StandardAnalyzer(Version.LUCENE_45);

		// 1. create the index
		Directory index = new RAMDirectory();

		IndexWriterConfig config = new IndexWriterConfig(Version.LUCENE_45,
				analyzer);

		IndexWriter w = new IndexWriter(index, config);
		addDoc(w, "Lucene in Action", "193398817");
		addDoc(w, "Lucene for Dummies", "55320055Z");
		addDoc(w, "Managing Gigabytes", "55063554A");
		addDoc(w, "The Art of Computer Art Science", "9900333X1");
		addDoc(w, "Greatest sports - Rugby", "666666ABC");
		addDoc(w, "History of Medieval period", "7777777A3");
		addDoc(w, "The Art of Lucene", "AGENT_007");
		addDoc(w, "The Art of Lucene art, Art Edition 2", "999999XXX");
		N = w.numDocs(); // get the count of all documents
		w.close();

		// 2. query
		String querystr = args.length > 0 ? args[0] : searchCriteria;

		// the "title" arg specifies the default field to use when no field is
		// explicitly specified in the query.
		Query q = new QueryParser(Version.LUCENE_45, "title", analyzer)
				.parse(querystr);

		// 3. search
		int hitsPerPage = 10;
		IndexReader reader = DirectoryReader.open(index);
		IndexSearcher searcher = new IndexSearcher(reader);
		TopScoreDocCollector collector = TopScoreDocCollector.create(
				hitsPerPage, true);
		searcher.search(q, collector);
		ScoreDoc[] hits = collector.topDocs().scoreDocs;

		Nt = hits.length;
		double fraction = N / Nt;
		IDF = Math.log(fraction);

		// 4. display results
		System.out.println("Works great - found " + hits.length + " hits.");
		for (int i = 0; i < hits.length; ++i) {
			int docId = hits[i].doc;
			Document d = searcher.doc(docId);
			System.out.println((i + 1) + ". ISBN: " + d.get("isbn") + "\t"
					+ "Title: " + d.get("title"));

			// 5. calculate TF-IDF(t,doc) = TF * IDF
			Terms terms = reader.getTermVector(docId, "title");
			if (terms != null && terms.size() > 0) {
				TermsEnum termsEnum = terms.iterator(null); // access the terms
															// for this field
				BytesRef term = null;
				while ((term = termsEnum.next()) != null) {// explore the terms
															// for this field
					DocsEnum docsEnum = termsEnum.docs(null, null); // enumerate
																	// through
																	// documents
					while (docsEnum.nextDoc() != DocIdSetIterator.NO_MORE_DOCS) {
						if (term.utf8ToString().equalsIgnoreCase(querystr)) {
							TF = docsEnum.freq(); // get the term frequency in
													// the document
							TF_IDF = TF * IDF;
							System.out.println("TF-IDF(Term:" + querystr
									+ ", Doc:" + d.get("title") + ") = "
									+ df.format(TF_IDF));
						}
					}
				}
			}
		}

		// reader can only be closed when there is no need to access the
		// documents any more.
		reader.close();
	}

	private static void addDoc(IndexWriter w, String title, String isbn)
			throws IOException {

		Document doc = new Document();

		// create a custom field for title, we want it tokenized
		FieldType type = new FieldType();
		type.setIndexed(true);
		type.setStored(true);
		type.setStoreTermVectors(true);
		Field field = new Field("title", title, type);
		doc.add(field);

		// use a string field for isbn because we don't want it tokenized
		doc.add(new StringField("isbn", isbn, Field.Store.YES));
		w.addDocument(doc);
	}
}
