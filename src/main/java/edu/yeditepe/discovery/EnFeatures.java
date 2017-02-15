package edu.yeditepe.discovery;

public class EnFeatures {
	private String title;
	private String type;
	private double[] titleVector;
	private double[] nounVector;
	private double[] verbVector;

	public EnFeatures(String title, String type) {
		this.title = title;
		this.type = type;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public double[] getTitleVector() {
		return titleVector;
	}

	public void setTitleVector(double[] titleVector) {
		this.titleVector = titleVector;
	}

	public double[] getNounVector() {
		return nounVector;
	}

	public void setNounVector(double[] nounVector) {
		this.nounVector = nounVector;
	}

	public double[] getVerbVector() {
		return verbVector;
	}

	public void setVerbVector(double[] verbVector) {
		this.verbVector = verbVector;
	}
}
