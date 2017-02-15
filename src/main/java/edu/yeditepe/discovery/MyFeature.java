package edu.yeditepe.discovery;

import de.bwaldvogel.liblinear.FeatureNode;

public class MyFeature extends FeatureNode {
	private String title;

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public MyFeature(int index, double value, String title) {
		super(index, value);
		this.setTitle(title);
	}

}
