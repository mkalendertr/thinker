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
package it.cnr.isti.hpc.text;

/**
 * @author Diego Ceccarelli <diego.ceccarelli@isti.cnr.it>
 * 
 *         Created on Sep 4, 2012
 */
public class Token {

	public Token(String originalText, String morphText, String suffix,
			String pos, int start, int end, String sentence) {
		super();
		this.text = originalText;
		this.morphText = morphText;
		this.suffix = suffix;
		this.pos = pos;
		this.start = start;
		this.end = end;
		this.sentence = sentence;
	}

	public Token(String originalText, String morphText, String suffix,
			String pos, int start, int end, String sentence, String dictionary) {
		super();
		this.text = originalText;
		this.morphText = morphText;
		this.suffix = suffix;
		this.pos = pos;
		this.start = start;
		this.end = end;
		this.sentence = sentence;
		this.dictionary = dictionary;
	}

	public Token(String text, int start, int end) {
		super();
		this.text = text;
		this.start = start;
		this.end = end;
	}

	private String text;
	private String morphText;
	private String suffix;
	private String pos;
	private int start;
	private int end;
	private String sentence;
	private String dictionary;

	public String getDictionary() {
		return dictionary;
	}

	public void setDictionary(String dictionary) {
		this.dictionary = dictionary;
	}

	public String getText() {
		return text;
	}

	public String getMorphText() {
		return morphText;
	}

	public void setMorphText(String morphText) {
		this.morphText = morphText;
	}

	public Token(int start, int end) {
		super();
		this.start = start;
		this.end = end;
	}

	public boolean isEmpty() {
		return text.isEmpty();
	}

	public String getPos() {
		return pos;
	}

	public void setPos(String pos) {
		this.pos = pos;
	}

	/**
	 * @return the text
	 */
	// public String getText() {
	// return text;
	// }

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

	public String getSuffix() {
		return suffix;
	}

	public void setSuffix(String suffix) {
		this.suffix = suffix;
	}

	public String getSentence() {
		return sentence;
	}

	public void setSentence(String sentence) {
		this.sentence = sentence;
	}

	public String toString() {
		return "Token[" + text + "] \t(" + start + "," + end + ")";
	}

}
