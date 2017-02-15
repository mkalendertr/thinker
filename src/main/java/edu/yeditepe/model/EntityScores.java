package edu.yeditepe.model;

import it.cnr.isti.hpc.dexter.entity.EntityMatch;

public class EntityScores {
	String id;

	double popularityScore;

	double nameScore = 0;

	double letterCaseScore = 0;

	double suffixScore = 0;

	double typeContentScore = 0;

	double typeScore = 0;

	double domainScore = 0;

	double wordvecDescriptionScore = 0;
	double wordvecDescriptionLocalScore = 0;
	double wordvecLinksScore = 0;

	double hashDescriptionScore = 0;
	double hashInfoboxScore = 0;

	double linkScore = 0;

	double leskScore = 0;

	double simpleLeskScore = 0;

	double typeClassifierkScore = 0;

	double score = 0;

	EntityMatch entityMatch;

	public EntityScores(EntityMatch entityMatch, String id,
			double popularityScore, double nameScore, double letterCaseScore,
			double suffixScore, double wordvecDescriptionScore,
			double typeContentScore, double typeScore, double domainScore,
			double hashDescriptionScore, double wordvecDescriptionLocalScore,
			double hashInfoboxScore, double linkScore,
			double wordvecLinksScore, double leskScore, double simpleLeskScore,
			double typeClassifierkScore) {
		super();
		this.entityMatch = entityMatch;
		this.id = id;
		this.popularityScore = popularityScore;
		this.nameScore = nameScore;
		this.letterCaseScore = letterCaseScore;
		this.suffixScore = suffixScore;
		this.wordvecDescriptionScore = wordvecDescriptionScore;
		this.typeContentScore = typeContentScore;
		this.typeScore = typeScore;
		this.domainScore = domainScore;
		this.hashDescriptionScore = hashDescriptionScore;
		this.wordvecDescriptionLocalScore = wordvecDescriptionLocalScore;
		this.hashInfoboxScore = hashInfoboxScore;
		this.linkScore = linkScore;
		this.wordvecLinksScore = wordvecLinksScore;
		this.leskScore = leskScore;
		this.simpleLeskScore = simpleLeskScore;
		this.typeClassifierkScore = typeClassifierkScore;
	}

	public EntityScores(EntityMatch entityMatch, String id, double score) {
		super();
		this.entityMatch = entityMatch;
		this.id = id;
		this.score = score;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public double getPopularityScore() {
		return popularityScore;
	}

	public void setPopularityScore(double popularityScore) {
		this.popularityScore = popularityScore;
	}

	public double getNameScore() {
		return nameScore;
	}

	public void setNameScore(double nameScore) {
		this.nameScore = nameScore;
	}

	public double getLetterCaseScore() {
		return letterCaseScore;
	}

	public void setLetterCaseScore(double letterCaseScore) {
		this.letterCaseScore = letterCaseScore;
	}

	public double getSuffixScore() {
		return suffixScore;
	}

	public void setSuffixScore(double suffixScore) {
		this.suffixScore = suffixScore;
	}

	public double getTypeContentScore() {
		return typeContentScore;
	}

	public void setTypeContentScore(double typeContentScore) {
		this.typeContentScore = typeContentScore;
	}

	public double getTypeScore() {
		return typeScore;
	}

	public void setTypeScore(double typeScore) {
		this.typeScore = typeScore;
	}

	public double getDomainScore() {
		return domainScore;
	}

	public void setDomainScore(double domainScore) {
		this.domainScore = domainScore;
	}

	public double getLinkScore() {
		return linkScore;
	}

	public void setLinkScore(double linkScore) {
		this.linkScore = linkScore;
	}

	public double getScore() {
		return score;
	}

	public void setScore(double score) {
		this.score = score;
	}

	public EntityMatch getEntityMatch() {
		return entityMatch;
	}

	public void setEntityMatch(EntityMatch entityMatch) {
		this.entityMatch = entityMatch;
	}

	public double getWordvecDescriptionScore() {
		return wordvecDescriptionScore;
	}

	public void setWordvecDescriptionScore(double wordvecDescriptionScore) {
		this.wordvecDescriptionScore = wordvecDescriptionScore;
	}

	public double getHashDescriptionScore() {
		return hashDescriptionScore;
	}

	public void setHashDescriptionScore(double hashDescriptionScore) {
		this.hashDescriptionScore = hashDescriptionScore;
	}

	public double getHashInfoboxScore() {
		return hashInfoboxScore;
	}

	public void setHashInfoboxScore(double hashInfoboxScore) {
		this.hashInfoboxScore = hashInfoboxScore;
	}

	public double getWordvecDescriptionLocalScore() {
		return wordvecDescriptionLocalScore;
	}

	public void setWordvecDescriptionLocalScore(
			double wordvecDescriptionLocalScore) {
		this.wordvecDescriptionLocalScore = wordvecDescriptionLocalScore;
	}

	public double getWordvecLinksScore() {
		return wordvecLinksScore;
	}

	public void setWordvecLinksScore(double wordvecLinksScore) {
		this.wordvecLinksScore = wordvecLinksScore;
	}

	public double getLeskScore() {
		return leskScore;
	}

	public void setLeskScore(double leskScore) {
		this.leskScore = leskScore;
	}

	public double getSimpleLeskScore() {
		return simpleLeskScore;
	}

	public void setSimpleLeskScore(double simpleLeskScore) {
		this.simpleLeskScore = simpleLeskScore;
	}

	public double getTypeClassifierkScore() {
		return typeClassifierkScore;
	}

	public void setTypeClassifierkScore(double typeClassifierkScore) {
		this.typeClassifierkScore = typeClassifierkScore;
	}

}
