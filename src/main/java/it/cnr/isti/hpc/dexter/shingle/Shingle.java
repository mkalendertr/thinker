/**
 *  Copyright 2012 Diego Ceccarelli
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package it.cnr.isti.hpc.dexter.shingle;

import it.cnr.isti.hpc.text.Token;

import java.util.List;

/**
 * A Shingle represents a fragment of text in a document to annotate.
 * 
 * 
 * @author Diego Ceccarelli <diego.ceccarelli@isti.cnr.it>
 * 
 *         Created on Sep 4, 2012
 */
public class Shingle {

	public List<Token> tokens;

	/** cleaned text */
	public String text;

	public String originalText;

	public String sentence;

	/** start position in the original text */
	public int start;

	/** end position in the original text */
	public int end;

	public String suffix;

	public String pos;

	public Shingle(List<Token> tokens) {
		this.tokens = tokens;
		StringBuilder sb = new StringBuilder();
		start = tokens.get(0).getStart();
		end = tokens.get(tokens.size() - 1).getEnd();
		for (int i = 0; i < tokens.size() - 1; i++) {
			sb.append(tokens.get(i).getText()).append(" ");
		}
		originalText = sb.toString() + tokens.get(tokens.size() - 1).getText();
		sb.append(tokens.get(tokens.size() - 1).getMorphText());
		text = sb.toString();
		suffix = tokens.get(tokens.size() - 1).getSuffix();
		pos = tokens.get(tokens.size() - 1).getPos();
		this.sentence = tokens.get(0).getSentence();
	}

	public Shingle(List<Token> tokens, String text, int start, int end) {
		super();
		this.tokens = tokens;
		this.text = text;
		this.start = start;
		this.end = end;
	}

	public Shingle(List<Token> tokens, String text) {
		this(tokens, text, -1, -1);
	}

	public boolean isEmpty() {
		return text.isEmpty();
	}

	/**
	 * @return the text
	 */
	public String getText() {
		return text;
	}

	/**
	 * @param text
	 *            the text to set
	 */
	public void setText(String text) {
		this.text = text;
	}

	/**
	 * @return the start
	 */
	public int getStart() {
		return start;
	}

	/**
	 * @param start
	 *            the start to set
	 */
	public void setStart(int start) {
		this.start = start;
	}

	public String getOriginalText() {
		return originalText;
	}

	public void setOriginalText(String originalText) {
		this.originalText = originalText;
	}

	/**
	 * @return the end
	 */
	public int getEnd() {
		return end;
	}

	/**
	 * @param end
	 *            the end to set
	 */
	public void setEnd(int end) {
		this.end = end;
	}

	@Override
	public String toString() {
		return "<" + text + "> [" + start + "," + end + "]";
	}

	public String getSuffix() {
		return suffix;
	}

	public void setSuffix(String suffix) {
		this.suffix = suffix;
	}

	public String getPos() {
		return pos;
	}

	public void setPos(String pos) {
		this.pos = pos;
	}

	public String getSentence() {
		return sentence;
	}

	public void setSentence(String sentence) {
		this.sentence = sentence;
	}

	public String originalShingle(String originalText) {
		return originalText.substring(start, end);
	}

}
