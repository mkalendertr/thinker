package edu.yeditepe.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.mongodb.BasicDBObject;

public class Entity extends BasicDBObject {
	/**
	 * 
	 */
	private static final long serialVersionUID = -6456168667110404518L;
	public static final String COLLECTION_NAME = "Entity";

	// private String id;
	// private String source;
	// private String title;
	// private Set<String> alias = new HashSet<String>();
	// private String description;
	// private List<String> sentences = new ArrayList<String>();
	// private Set<String> suffixes = new HashSet<String>();
	// private int letterCase;
	// private double rank;
	// private String type;
	// private String domain;
	// private Set<String> inLinks;
	// private Set<String> outLinks;
	// private List<Double> semanticEmbedding;
	// private List<Double> descriptionEmbeddingHash;
	// private List<Double> descriptionEmbeddingAverage;

	public static final String ID = "id";
	public static final String URL = "url";
	public static final String TITLE = "title";
	public static final String SOURCE = "source";
	public static final String ALIAS = "alias";
	public static final String DESCRIPTION = "description";
	public static final String SENTENCES = "sentences";
	public static final String SUFFIXES = "suffixes";
	public static final String LETTERCASE = "letterCase";
	public static final String RANK = "rank";
	public static final String TYPE = "type";
	public static final String DOMAIN = "domain";
	public static final String LINKS = "links";
	// public static final String OUTLINKS = "outLinks";
	public static final String SEMANTICEMBEDDINGAUTOENCODER = "semanticembeddingautoencoder";
	public static final String SEMANTICEMBEDDINGAUTOENCODER3GRAM = "semanticembeddingautoencoder3gram";
	public static final String SEMANTICEMBEDDINGWORD2VEC = "semanticembeddingword2vec";

	public static final String DESCRIPTIONEMBEDDINGHASH = "descriptionembeddinghash";
	public static final String DESCRIPTIONEMBEDDINGAVERAGE = "descriptionembeddingaverage";
	public static final String DESCRIPTIONGHASHVECTOR = "descriptionghashvector";

	public static final String DESCRIPTIONEMBEDDINGHASH2 = "descriptionembeddinghash2";

	public String getId() {
		return getString(ID);
	}

	public void setId(String id) {
		put(ID, id);
	}

	public String getUrl() {
		return getString(URL);
	}

	public void setUrl(String url) {
		put(URL, url);
	}

	public String getSource() {
		return getString(SOURCE);
	}

	public void setSource(String source) {
		put(SOURCE, source);
	}

	public String getTitle() {
		return getString(TITLE);
	}

	public void setTitle(String title) {
		put(TITLE, title);
	}

	public List<String> getAlias() {
		return (List<String>) get(ALIAS);
	}

	public void setAlias(Set<String> alias) {
		put(ALIAS, alias);
	}

	public String getDescription() {
		return getString(DESCRIPTION);
	}

	public void setDescription(String description) {
		put(DESCRIPTION, description);
	}

	public List<String> getSentences() {
		return (List<String>) get(SENTENCES);
	}

	public void setSentences(List<String> sentences) {
		put(SENTENCES, sentences);
	}

	public List<String> getSuffixes() {
		return (List<String>) get(SUFFIXES);
	}

	public void setSuffixes(Set<String> suffixes) {
		put(SUFFIXES, suffixes);
	}

	public int getLetterCase() {
		return getInt(LETTERCASE);
	}

	public void setLetterCase(int letterCase) {
		put(LETTERCASE, letterCase);
	}

	public double getRank() {
		return getDouble(RANK);
	}

	public void setRank(double rank) {
		put(RANK, rank);
	}

	public String getType() {
		return getString(TYPE);
	}

	public void setType(String type) {
		put(TYPE, type);
	}

	public String getDomain() {
		return getString(DOMAIN);
	}

	public void setDomain(String domain) {
		put(DOMAIN, domain);
	}

	public List<String> getLinks() {
		return (List<String>) get(LINKS);
	}

	public void setLinks(Set<String> links) {
		put(LINKS, links);
	}

	// public Set<String> getOutLinks() {
	// return (Set<String>) get(OUTLINKS);
	// }
	//
	// public void setOutLinks(Set<String> outLinks) {
	// put(OUTLINKS, outLinks);
	// }

	public List<Double> getSemanticEmbeddingAutoencoder() {
		return (ArrayList<Double>) get(SEMANTICEMBEDDINGAUTOENCODER);
	}

	public void setSemanticEmbeddingAutoencoder(double[] semanticEmbedding) {
		put(SEMANTICEMBEDDINGAUTOENCODER, semanticEmbedding);
	}

	public List<Double> getSemanticEmbeddingAutoencoder3gram() {
		return (ArrayList<Double>) get(SEMANTICEMBEDDINGAUTOENCODER3GRAM);
	}

	public void setSemanticEmbeddingAutoencoder3gram(
			double[] semanticEmbedding3gram) {
		put(SEMANTICEMBEDDINGAUTOENCODER3GRAM, semanticEmbedding3gram);
	}

	public List<Double> getSemanticEmbeddingWord2Vec() {
		return (ArrayList<Double>) get(SEMANTICEMBEDDINGWORD2VEC);
	}

	public void setSemanticEmbeddinggWord2Vec(double[] semanticEmbedding) {
		put(SEMANTICEMBEDDINGWORD2VEC, semanticEmbedding);
	}

	public List<Double> getDescriptionEmbeddingHash() {
		return (ArrayList<Double>) get(DESCRIPTIONEMBEDDINGHASH);
	}

	public void setDescriptionEmbeddingHash(double[] descriptionEmbeddingHash) {
		put(DESCRIPTIONEMBEDDINGHASH, descriptionEmbeddingHash);
	}

	public List<Double> getDescriptionEmbeddingHash2() {
		return (ArrayList<Double>) get(DESCRIPTIONEMBEDDINGHASH2);
	}

	public void setDescriptionEmbeddingHash2(double[] descriptionEmbeddingHash2) {
		put(DESCRIPTIONEMBEDDINGHASH2, descriptionEmbeddingHash2);
	}

	public List<Double> getDescriptionEmbeddingAverage() {
		return (ArrayList<Double>) get(DESCRIPTIONEMBEDDINGAVERAGE);
	}

	public void setDescriptionEmbeddingAverage(
			double[] descriptionEmbeddingAverage) {
		put(DESCRIPTIONEMBEDDINGAVERAGE, descriptionEmbeddingAverage);
	}

	public List<Double> getDescriptionHashVector() {
		return (ArrayList<Double>) get(DESCRIPTIONGHASHVECTOR);
	}

	public void setDescriptionHashVector(double[] descriptionHashVector) {
		put(DESCRIPTIONGHASHVECTOR, descriptionHashVector);
	}

}
