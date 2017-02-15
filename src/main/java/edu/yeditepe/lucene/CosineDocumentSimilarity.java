package edu.yeditepe.lucene;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.RealVector;
import org.apache.log4j.Logger;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.Version;

import com.google.gson.Gson;

public class CosineDocumentSimilarity {
	private static final Logger LOGGER = Logger
			.getLogger(CosineDocumentSimilarity.class);

	public static final String CONTENT = "Content";

	private final Set<String> terms = new HashSet<String>();

	private final Map<String, RealVector> vectors = new HashMap<String, RealVector>();

	private final Map<String, Integer> documentsMap = new HashMap<String, Integer>();

	private final List<String> documents = new ArrayList<String>();

	private Directory directory;

	private Analyzer analyzer;

	private IndexWriterConfig iwc;

	private IndexWriter writer;

	private int docCounter = 0;

	public CosineDocumentSimilarity() throws IOException {
		directory = new RAMDirectory();
		Analyzer analyzer = new WhitespaceAnalyzer();
		iwc = new IndexWriterConfig(Version.LATEST, analyzer);
		writer = new IndexWriter(directory, iwc);

	}

	public void addDocument(String id, String content) throws IOException {
		Document doc = new Document();
		doc.add(new VecTextField(CONTENT, content, Store.YES));
		writer.addDocument(doc);
		documentsMap.put(id, docCounter++);

	}

	public RealVector calculateVector(String id) throws IOException {
		writer.close();
		IndexReader reader = DirectoryReader.open(directory);

		try {
			Map<String, Integer> f1 = getTermFrequencies(reader,
					documentsMap.get(id));
			reader.close();
			return toRealVector(f1);
		} catch (Exception e) {
			LOGGER.error(e);
		}

		return null;
	}

	// public Map<String, RealVector> calculateVectors() throws IOException {
	// writer.close();
	// IndexReader reader = DirectoryReader.open(directory);
	// for (int i = 0; i < documents.size(); i++) {
	// try {
	// Map<String, Integer> f1 = getTermFrequencies(reader, i);
	// vectors.put(documents.get(i), toRealVector(f1));
	// } catch (Exception e) {
	// LOGGER.error(e);
	// }
	//
	// }
	// reader.close();
	// return vectors;
	// }

	public static double getCosineSimilarity(RealVector v1, RealVector v2) {
		try {
			return (v1.dotProduct(v2)) / (v1.getNorm() * v2.getNorm());
		} catch (Exception e) {
			LOGGER.error(e);
		}
		return 0;
	}

	Map<String, Integer> getTermFrequencies(IndexReader reader, int docId)
			throws IOException {
		Terms vector = reader.getTermVector(docId, CONTENT);
		TermsEnum termsEnum = null;
		termsEnum = vector.iterator(termsEnum);
		Map<String, Integer> frequencies = new HashMap<String, Integer>();
		BytesRef text = null;
		while ((text = termsEnum.next()) != null) {
			String term = text.utf8ToString();
			int freq = (int) termsEnum.totalTermFreq();
			frequencies.put(term, freq);
			terms.add(term);
		}
		return frequencies;
	}

	RealVector toRealVector(Map<String, Integer> map) {
		RealVector vector = new ArrayRealVector(terms.size());
		int i = 0;
		for (String term : terms) {
			int value = map.containsKey(term) ? map.get(term) : 0;
			vector.setEntry(i++, value);
		}
		return (RealVector) vector.mapDivide(vector.getL1Norm());
	}

	public Map<String, RealVector> getVectors() {
		return vectors;
	}

	public static void main(String[] args) {
		try {
			CosineDocumentSimilarity s = new CosineDocumentSimilarity();
			s.addDocument("0", "22");
			s.addDocument("1", "22 22");
			s.addDocument(
					"2",
					" 222195 623824 687091 770989 773284 836902 1075390 1096888 1414166 1436046 1468627 1490038 1490351 1711155 1756678 22 30 676 701 816 2260 2272 2278 2279 2315 2323 3112 3334 3490 3637 4138 4275 6851 8207 9189 16711 21984 25808 27391 45293 63922 66289 70967 79840 92070 113301 120772 154353 191415 215219 404564 549590 550324 1423101 1436046");

			// Map<String, RealVector> vectors = s.calculateVectors();
			// LOGGER.info(vectors.get("0"));
			System.out.println(getCosineSimilarity(s.calculateVector("0"),
					s.calculateVector("1")));
			// System.out.println(getCosineSimilarity(vectors.get("0"),
			// vectors.get("2")));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static String vectorSerialize(RealVector o) throws IOException {
		Gson gson = new Gson();
		return gson.toJson(o);

	}

	public static ArrayRealVector vectorDeserialize(String s)
			throws IOException, ClassNotFoundException {
		Gson gson = new Gson();
		return gson.fromJson(s, ArrayRealVector.class);

	}
}