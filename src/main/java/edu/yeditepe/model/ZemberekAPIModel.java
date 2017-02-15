package edu.yeditepe.model;

public class ZemberekAPIModel {
	private String function;
	private String input;
	private String output;

	public ZemberekAPIModel(String function, String input, String output) {
		super();
		this.function = function;
		this.input = input;
		this.output = output;
	}

	public String getFunction() {
		return function;
	}

	public void setFunction(String function) {
		this.function = function;
	}

	public String getInput() {
		return input;
	}

	public void setInput(String input) {
		this.input = input;
	}

	public String getOutput() {
		return output;
	}

	public void setOutput(String output) {
		this.output = output;
	}

}
