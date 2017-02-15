package edu.yeditepe.model;

public class Link {
	public String getFrom() {
		return from;
	}

	public void setFrom(String from) {
		this.from = from;
	}

	public String getTo() {
		return to;
	}

	public void setTo(String to) {
		this.to = to;
	}

	public Link(String from, String to) {
		super();
		this.from = from;
		this.to = to;
	}

	private String from;
	private String to;
}
