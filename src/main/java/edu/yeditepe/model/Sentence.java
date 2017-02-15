package edu.yeditepe.model;

import org.springframework.data.annotation.Id;

public class Sentence {

    @Id
    private String orignalText;

    private String nerResult;

    private String pipelineResult;

    private String totalResult;

    public Sentence() {
    }

    public Sentence(String orignalText, String nerResult,
	    String pipelineResult, String totalResult) {
	super();
	this.orignalText = orignalText;
	this.nerResult = nerResult;
	this.pipelineResult = pipelineResult;
	this.totalResult = totalResult;
    }

    public String getOrignalText() {
	return orignalText;
    }

    public void setOrignalText(String orignalText) {
	this.orignalText = orignalText;
    }

    public String getNerResult() {
	return nerResult;
    }

    public void setNerResult(String nerResult) {
	this.nerResult = nerResult;
    }

    public String getPipelineResult() {
	return pipelineResult;
    }

    public void setPipelineResult(String pipelineResult) {
	this.pipelineResult = pipelineResult;
    }

    public String getTotalResult() {
	return totalResult;
    }

    public void setTotalResult(String totalResult) {
	this.totalResult = totalResult;
    }

    @Override
    public String toString() {
	return String
		.format("Sentence[id=%s, pipelineResult='%s', nerResult='%s', totalResult='%s']",
			orignalText, pipelineResult, nerResult, totalResult);
    }

}
