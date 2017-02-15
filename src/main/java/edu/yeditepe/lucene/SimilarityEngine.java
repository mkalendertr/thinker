package edu.yeditepe.lucene;

import java.io.File;
import java.io.IOException;

import org.apache.log4j.Logger;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.DoubleField;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;

import edu.yeditepe.utils.Property;

public class SimilarityEngine {
    private static final Logger LOGGER = Logger.getLogger(SimilarityEngine.class);

    private IndexSearcher similaritySearcher = null;

    private QueryParser similarityParser = null;

    private IndexWriter similarityIndexWriter = null;

    private static SimilarityEngine instance = new SimilarityEngine();

    public static SimilarityEngine getInstance() {
        return instance;
    }

    /** Creates a new instance of SearchEngine */
    private SimilarityEngine() {
        try {
            Directory indexDir = FSDirectory.open(new File(Property.getInstance().get("lucene.similarity")));
            IndexWriterConfig config = new IndexWriterConfig(Version.LUCENE_4_10_3, new WhitespaceAnalyzer());
            similarityIndexWriter = new IndexWriter(indexDir, config);
            similaritySearcher =
                new IndexSearcher(DirectoryReader.open(FSDirectory.open(new File(Property.getInstance().get("lucene.similarity")))));
            similarityParser = new QueryParser("id", new WhitespaceAnalyzer());

        } catch (IOException e) {
            LOGGER.error(e);
        }
    }

    public IndexWriter getSimilarityIndexWriter() throws IOException {

        return similarityIndexWriter;
    }

    public void closeIndexWriter() throws IOException {
        if (similarityIndexWriter != null) {
            similarityIndexWriter.close();
        }

    }

    public void indexWikiPageSimilarity(int page1, int page2, Double link) {
        try {
            IndexWriter writer = getSimilarityIndexWriter();
            Document doc = new Document();
            doc.add(new TextField("id", page1 + "_" + page2 + " " + page2 + "_" + page1, Field.Store.NO));
            doc.add(new DoubleField("link", link, Field.Store.YES));

            writer.addDocument(doc);
            // LOGGER.info("Similarty index " + page1 + "_" + page2 + " = " + link);
        } catch (Exception e) {
            LOGGER.error(e);
        }

    }

    public double performSimilaritySearch(String page1, String page2) {
        // Query query = parser.parse(queryString + "*");
        LOGGER.info("Searching similarity " + page1 + " " + page2);
        try {
            Query query = similarityParser.parse(QueryParser.escape(page1 + "_" + page2));
            // SortField field = new SortField("link", SortField.Type.DOUBLE, true);
            //
            // Sort sort = new Sort(field);
            TopDocs topDocs = similaritySearcher.search(query, 1);
            ScoreDoc[] hits = topDocs.scoreDocs;

            // retrieve each matching document from the ScoreDoc arry
            for (int i = 0; i < hits.length; i++) {
                Document doc = similaritySearcher.doc(hits[i].doc);
                String sim = doc.get("link");
                LOGGER.info("Similarity= " + sim);
                if (sim != null) {
                    return Double.parseDouble(sim);
                }
            }
        } catch (Exception e) {
            LOGGER.error(e);
        }

        return -1;
    }

    public static void main(String[] args) throws IOException, ParseException {

        SimilarityEngine instance = new SimilarityEngine();

        instance.performSimilaritySearch("35", "22");
    }
}