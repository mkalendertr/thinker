package edu.yeditepe.model;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;

import edu.yeditepe.nlp.Zemberek;
import edu.yeditepe.wiki.Wikipedia;

public class EntityPage {
	private static final Logger LOGGER = Logger.getLogger(EntityPage.class);

	private String titleMorph;

	private String urlTitle;

	private String enTitle;
	private String id;

	private String title;

	private String type;

	private String predictedType = "";

	private double viewcount;

	private double pagelength;

	private double rank;

	private String links;
	private String alias;

	private String domain;
	private List<Double> word2vec;
	private List<Double> autoencoder;
	private List<Double> dword2vec;
	private List<Double> dautoencoder;
	private String description;
	private String suffix;
	private int letterCase;

	public String getPredictedType() {
		return predictedType;
	}

	public void setPredictedType(String predictedType) {
		this.predictedType = predictedType;
	}

	public double getRank() {
		return rank;
	}

	public void setRank(double wikiRank) {
		this.rank = wikiRank;
	}

	// public String getAliasM() {

	// }

	// public void setAlias(String alias) {
	// this.alias = alias;
	// }

	public List<Double> getDword2vec() {
		return dword2vec;
	}

	public void setDword2vec(List<Double> dword2vec) {
		this.dword2vec = dword2vec;
	}

	public List<Double> getDautoencoder() {
		return dautoencoder;
	}

	public void setDautoencoder(List<Double> dautoencoder) {
		this.dautoencoder = dautoencoder;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getSuffix() {
		return suffix;
	}

	public void setSuffix(String suffix) {
		this.suffix = suffix;
	}

	public int getLetterCase() {
		return letterCase;
	}

	public void setLetterCase(int letterCase) {
		this.letterCase = letterCase;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getUrlTitle() {
		return urlTitle;
	}

	public void setUrlTitle(String urlTitle) {
		this.urlTitle = urlTitle;
	}

	public String getLinks() {
		return links;
	}

	public void setLinks(String links) {
		this.links = links;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getTitleMorph() {
		return titleMorph;
	}

	public void setTitleMorph(String titleMorph) {
		this.titleMorph = titleMorph;
	}

	public double getViewcount() {
		return viewcount;
	}

	public void setViewcount(double viewcount) {
		this.viewcount = viewcount;
	}

	public double getPagelength() {
		return pagelength;
	}

	public void setPagelength(double pagelength) {
		this.pagelength = pagelength;
	}

	public Set<String> getAliasSet() {
		return aliasSet;
	}

	public void setAliasSet(Set<String> aliasSet) {
		this.aliasSet = aliasSet;
	}

	public String getEnTitle() {
		return enTitle;
	}

	public void setEnTitle(String enTitle) {
		this.enTitle = enTitle;
	}

	public String getDomain() {
		return domain;
	}

	public void setDomain(String domain) {
		this.domain = domain;
	}

	public String getAliasMorph() {

		StringBuffer sb = new StringBuffer();

		for (String string : aliasSet) {
			sb.append(string + " ");
		}
		return sb.toString().trim();

	}

	public String getAlias() {
		return alias;
	}

	public void setAlias(String alias) {
		this.alias = alias;
	}

	private Set<String> aliasSet = new HashSet<String>();

	public List<Double> getWord2vec() {
		return word2vec;
	}

	public void setWord2vec(List<Double> word2vec) {
		this.word2vec = word2vec;
	}

	public List<Double> getAutoencoder() {
		return autoencoder;
	}

	public void setAutoencoder(List<Double> autoencoder) {
		this.autoencoder = autoencoder;
	}

	public EntityPage(String id, String title, String type, String titleMorph,
			String urlTitle, String enTitle, String description, String suffix,
			double rank, int letterCase, String links, String alias,
			String domain, List<Double> word2vec, List<Double> autoencoder,
			List<Double> dword2vec, List<Double> dautoencoder) {
		super();
		this.id = id;
		this.title = title;
		this.type = type;
		this.titleMorph = titleMorph;
		this.urlTitle = urlTitle;
		this.enTitle = enTitle;
		this.description = description;
		this.suffix = suffix;
		this.rank = rank;
		this.letterCase = letterCase;
		this.links = links;
		this.alias = alias + ",";
		this.domain = domain;
		this.word2vec = word2vec;
		this.autoencoder = autoencoder;
		this.dword2vec = dword2vec;
		this.dautoencoder = dautoencoder;
	}

	public EntityPage(String id, String title, String urlTitle, String enTitle,
			String alias, double wikiRank) {
		super();
		this.id = id;
		this.title = title;
		this.urlTitle = urlTitle;
		this.enTitle = enTitle;
		this.rank = wikiRank;
		this.alias = "";
		addAlias(title);
		addAlias(alias);
	}

	public EntityPage() {
		// TODO Auto-generated constructor stub
	}

	public void addAlias(String alias) {
		// if (alias.equals("Afrika")) {
		// LOGGER.info(alias);
		// }
		if (alias != null && alias.length() > 1) {
			this.alias += "," + alias;
			String s = Wikipedia.stringToLuceneString(alias);
			// if (!aliasSet.contains(s)) {
			aliasSet.add(s);
			String label = Zemberek.getInstance().morphEntityName(alias);
			if (label != null && label.length() > 1) {
				s = Wikipedia.stringToLuceneString(label);
				aliasSet.add(s);
			}
		}
		// }

	}

	public void clearAlias() {
		aliasSet.clear();
	}

}
