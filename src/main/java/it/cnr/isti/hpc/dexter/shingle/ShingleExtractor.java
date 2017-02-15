/**
s *  Copyright 2012 Salvatore Trani
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

import it.cnr.isti.hpc.dexter.spot.clean.SpotManager;
import it.cnr.isti.hpc.text.Sentence;
import it.cnr.isti.hpc.text.SentenceSegmenter;
import it.cnr.isti.hpc.text.Token;
import it.cnr.isti.hpc.text.TokenSegmenter;
import it.cnr.isti.hpc.wikipedia.article.Article;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

import org.apache.log4j.Logger;

import edu.yeditepe.nlp.TurkishNLP;
import edu.yeditepe.nounphrase.MySpotter;
import edu.yeditepe.utils.Constants;
import edu.yeditepe.utils.Property;

/**
 * ShingleExtractor extracts all the ngrams (of fixed length) from the text of
 * an article. Please observe that the ShingleExtractor perform a cleaning step
 * over each shingle using the {@link SpotManager standard spot cleaner}. Each
 * shingle produced could be different but the 'original' fragment, but it
 * contains the original start/end position in the original test.
 * 
 * 
 * @see Shingle
 * @see SpotManager
 * @author Salvatore Trani, salvatore.trani@isti.cnr.it created on 02/aug/2012
 * @author Diego Ceccarelli modified to make it thread safe on 11/july/2014
 */
public class ShingleExtractor implements Iterable<Shingle> {
	private static final Logger LOGGER = Logger
			.getLogger(ShingleExtractor.class);

	private int maxShingleSize;

	private static final int DEFAULT_MAX_SHINGLE_SIZE = Property.getInstance()
			.getInt("maxShingleSize");

	public static boolean myspotter = Boolean.parseBoolean(Property
			.getInstance().get("myspotter"));

	private static final float confidence = Float.parseFloat(Property
			.getInstance().get("myspotter.confidence"));

	private final List<List<Token>> cleanedSentences;

	private final SpotManager sm;

	private final TokenSegmenter ts;

	private final SentenceSegmenter ss;

	private String language = "tr";

	private Set<String> mergeSet = new HashSet<String>();

	private ShingleExtractor() {
		sm = SpotManager.getStandardSpotCleaner();
		ts = new TokenSegmenter();
		ss = SentenceSegmenter.getInstance();
		cleanedSentences = new ArrayList<List<Token>>();
		maxShingleSize = DEFAULT_MAX_SHINGLE_SIZE;
	}

	public ShingleExtractor(Article a) {
		this();
		addText(a.getTitle());
		for (String p : a.getParagraphs()) {
			addText(p);
		}
	}

	// public ShingleExtractor(Document doc) {
	// this(doc.getMention());
	// }

	public ShingleExtractor(String text, String language) {
		this();
		this.language = language;
		addText(text);
	}

	// FIXME
	public void process(String text) {
		addText(text);
	}

	private void addText(String text) {
		if (text == null || text.isEmpty())
			return;
		List<Sentence> sentences = ss.splitPos(text);

		// int start = 0;
		for (Sentence sentence : sentences) {
			String currSentence = sentence.getText();
			int startSentence = sentence.getStart();

			// System.out.println("SENTENCE [" + currSentence + "]");
			// //List<Token> textShingles = new LinkedList<Token>();
			// FIXME CLEAN SHOULD NO CHANGE THE OFFSETS OF THE TOKENS
			// currSentence = sm.clean(currSentence);
			List<Token> tokens = null;
			List<Token> cleanTokens = new LinkedList<Token>();

			if (language.equals(Constants.TURKISH)) {
				tokens = TurkishNLP.disambiguate(currSentence);
			} else {
				tokens = ts.tokenizePos(currSentence);

			}

			// experimental
			for (Token t : tokens) {

				t.setStart(t.getStart() + startSentence);
				t.setEnd(t.getEnd() + startSentence);

				if (language.equals(Constants.TURKISH)) {
					String cleanToken = sm.clean(t.getMorphText());
					// if (cleanToken.isEmpty())
					// continue;
					cleanTokens.add(t);
				} else {
					String token = text.substring(t.getStart(), t.getEnd());
					String cleanToken = sm.clean(token);
					if (cleanToken.isEmpty())
						continue;

					// System.out.println(token + "-> " + cleanToken);
					// System.out.println("token in text: " + token);

					// Skip empty token (or tokens made only of chars cleaned
					// above)

					t.setText(cleanToken);
					cleanTokens.add(t);
				}

			}
			cleanedSentences.add(cleanTokens);
		}

	}

	public int getMaxShingleSize() {
		return maxShingleSize;
	}

	@Override
	public Iterator<Shingle> iterator() {
		return new ShingleIterator();
	}

	public void setMaxShingleSize(int maxShingleSize) {
		this.maxShingleSize = maxShingleSize;
	}

	private class ShingleIterator implements Iterator<Shingle> {
		private int currentPos;

		private int currentSen;

		private final Deque<Shingle> tempContainer;

		public ShingleIterator() {
			currentPos = 0;
			currentSen = 0;
			tempContainer = new ArrayDeque<Shingle>();
		}

		private void generateNextShingles() {
			Deque<Shingle> container = new ArrayDeque<Shingle>();
			LOGGER.debug("Sentence " + currentSen);
			if (currentSen >= cleanedSentences.size())
				return;
			for (List<Token> currentSentence : cleanedSentences) {

				// List<Token> currentSentence =
				// cleanedSentences.get(currentSen);
				// Generate the shingles of size (1 .. maxShingleSize) starting
				// at
				// currentPos
				List<Token> longest = null;

				for (int i = 0; i < currentSentence.size(); i++) {
					for (int j = i; j < currentSentence.size(); j++) {
						List<Token> tokens = currentSentence.subList(i, j + 1);
						if (tokens.get(tokens.size() - 1).getPos()
								.equals("Punc")
								|| tokens.size() > maxShingleSize) {
							break;
						}
						if (!myspotter) {
							Shingle s = new Shingle(tokens);
							container.add(s);
						} else {
							if (tokens.size() == 1) {
								longest = tokens;
							} else {
								double merge = MySpotter.getInstance()
										.merge(tokens.get(tokens.size() - 2)
												.getText(),
												tokens.get(tokens.size() - 1)
														.getText(),
												tokens.size() - 1);
								if (merge < confidence) {
									if (longest != null) {
										Shingle s = new Shingle(longest);
										container.add(s);
										longest = null;
									}
									break;
								} else {
									mergeSet.add(tokens.get(tokens.size() - 1)
											.getText());
									mergeSet.add(tokens.get(tokens.size() - 2)
											.getText());
									LOGGER.info("Merge "
											+ tokens.get(0).getText()
											+ "..."
											+ tokens.get(tokens.size() - 1)
													.getText() + " " + merge);
									longest = tokens;

								}
							}

						}

					}
					if (longest != null) {
						Shingle s = new Shingle(longest);
						container.add(s);
						longest = null;
					}
				}

				currentSen++;
			}
			for (Shingle s : container) {
				if (!mergeSet.contains(s.getOriginalText())) {
					tempContainer.add(s);
				} else {
					LOGGER.info("Eleminate :" + s.getOriginalText());
				}
			}
		}

		@Override
		public boolean hasNext() {
			if (tempContainer.size() == 0)
				generateNextShingles();
			return tempContainer.size() > 0;
		}

		@Override
		public Shingle next() {
			if (hasNext())
				return tempContainer.pop();
			else
				throw new NoSuchElementException();
		}

		@Override
		public void remove() {
			throw new UnsupportedOperationException();
		}
	}

	public List<List<Token>> getCleanedSentences() {
		return cleanedSentences;
	}
}
