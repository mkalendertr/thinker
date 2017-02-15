package edu.yeditepe.discovery;

import java.util.HashSet;
import java.util.Set;

public class Features {
	public Features(String id, String title) {
		this.id = id;
		this.title = title;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public Set<String> getVerbs() {
		return verbs;
	}

	public void setVerbs(Set<String> verbs) {
		this.verbs = verbs;
	}

	public Set<String> getNouns() {
		return nouns;
	}

	public void setNouns(Set<String> nouns) {
		this.nouns = nouns;
	}

	public Set<String> getAdjs() {
		return adjs;
	}

	public void setAdjs(Set<String> adjs) {
		this.adjs = adjs;
	}

	public Set<String> getSuffixes() {
		return suffixes;
	}

	public void setSuffixes(Set<String> suffixes) {
		this.suffixes = suffixes;
	}

	public boolean isUppercase() {
		return uppercase;
	}

	public void setUppercase(boolean uppercase) {
		this.uppercase = uppercase;
	}

	private Set<String> verbs = new HashSet<String>();
	private Set<String> nouns = new HashSet<String>();
	private Set<String> adjs = new HashSet<String>();
	private Set<String> suffixes = new HashSet<String>();
	private boolean uppercase = true;
	private String title;
	private String id;

}
