/**
 *  Copyright 2013 Diego Ceccarelli
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

/**
 *  Copyright 2013 Diego Ceccarelli
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
package it.cnr.isti.hpc.dexter.disambiguation;

import it.cnr.isti.hpc.dexter.entity.Entity;
import it.cnr.isti.hpc.dexter.entity.EntityMatch;
import it.cnr.isti.hpc.dexter.entity.EntityMatchList;
import it.cnr.isti.hpc.dexter.shingle.Shingle;
import it.cnr.isti.hpc.dexter.spot.SpotMatch;
import it.cnr.isti.hpc.dexter.spot.SpotMatchList;
import it.cnr.isti.hpc.dexter.util.DexterLocalParams;
import it.cnr.isti.hpc.dexter.util.DexterParams;
import it.cnr.isti.hpc.text.Token;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Writer;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import com.google.common.collect.Multisets;
import com.google.common.collect.Sets;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import edu.yeditepe.deep.AveragePooling;
import edu.yeditepe.deep.DescriptionEmbeddingAverage;
import edu.yeditepe.experiment.RankLib;
import edu.yeditepe.model.EntityPage;
import edu.yeditepe.model.EntityScores;
import edu.yeditepe.nlp.TurkishNLP;
import edu.yeditepe.nlp.Zemberek;
import edu.yeditepe.similarity.EntityEmbeddingSimilarity;
import edu.yeditepe.similarity.GoogleDistanceCalculator;
import edu.yeditepe.similarity.JaccardCalculator;
import edu.yeditepe.similarity.LevenshteinDistanceCalculator;
import edu.yeditepe.typeclassifier.TypeClassifier;
import edu.yeditepe.utils.BlackListEntities;
import edu.yeditepe.utils.FileUtils;
import edu.yeditepe.utils.Property;

/**
 * Implements the Okkam's Razor principle, resolving the ambiguity for a spot
 * using the entity with the largest probability to be represented by the spot
 * (this probability is called <i>commonness</i>, and it is computed as the
 * ratio between the links that point to the entity (using the spot as anchor)
 * and the total number of links that have the spot as anchor.
 * 
 * @author Diego Ceccarelli <diego.ceccarelli@isti.cnr.it>
 * 
 *         Created on Sep 30, 2013
 */
public class TurkishEntityDisambiguator implements Disambiguator {
	private static final Logger LOGGER = Logger
			.getLogger(TurkishEntityDisambiguator.class);

	public static GoogleDistanceCalculator gd = new GoogleDistanceCalculator();

	public static boolean printCandidateEntities = false;

	public static boolean annotateEntities = false;

	public static boolean ranklib = false;

	public static String candidateEntitiesFileName = "";

	public static int candidateEntitiyId = 0;

	private static Set<String> typeBlackList;

	public static Map<String, EntityScores> entityScoreMap;

	public static Set<String> selectedEntities;

	private static float popularityWeight = Float.parseFloat(Property
			.getInstance().get("disambiguation.popularityWeight"));

	private static float nameWeight = Float.parseFloat(Property.getInstance()
			.get("disambiguation.nameWeight"));

	private static float letterCaseWeight = Float.parseFloat(Property
			.getInstance().get("disambiguation.letterCaseWeight"));

	private static float suffixWeight = Float.parseFloat(Property.getInstance()
			.get("disambiguation.suffixWeight"));

	private static float typeContentWeight = Float.parseFloat(Property
			.getInstance().get("disambiguation.typeContentWeight"));

	private static float typeWeight = Float.parseFloat(Property.getInstance()
			.get("disambiguation.typeWeight"));

	private static float domainWeight = Float.parseFloat(Property.getInstance()
			.get("disambiguation.domainWeight"));

	private static float wordvecDescriptionWeight = Float.parseFloat(Property
			.getInstance().get("disambiguation.wordvecDescriptionWeight"));

	private static float wordvecDescriptionLocalWeight = Float
			.parseFloat(Property.getInstance().get(
					"disambiguation.wordvecDescriptionLocalWeight"));

	private static float word2vecLinksWeight = Float.parseFloat(Property
			.getInstance().get("disambiguation.word2vecLinksWeight"));

	private static float hashDescriptionWeight = Float.parseFloat(Property
			.getInstance().get("disambiguation.hashDescription"));

	private static float hashInfoboxWeight = Float.parseFloat(Property
			.getInstance().get("disambiguation.hashInfoboxWeight"));

	private static float linkWeight = Float.parseFloat(Property.getInstance()
			.get("disambiguation.linkWeight"));

	private static float wikiWeight = Float.parseFloat(Property.getInstance()
			.get("disambiguation.wikiWeight"));

	private static float leskWeight = Float.parseFloat(Property.getInstance()
			.get("disambiguation.leskWeight"));

	private static float simpleLeskWeight = Float.parseFloat(Property
			.getInstance().get("disambiguation.simpleLeskWeight"));

	private static float typeClassifierkWeight = Float.parseFloat(Property
			.getInstance().get("disambiguation.typeClassifierkWeight"));

	private int window = Integer.parseInt(Property.getInstance().get(
			"disambiguation.window"));

	private int minSuffix = Integer.parseInt(Property.getInstance().get(
			"disambiguation.minSuffix"));

	public TurkishEntityDisambiguator() {
		typeBlackList = new HashSet<String>();
		typeBlackList.add("film");
		typeBlackList.add("albüm");
		typeBlackList.add("kitap");
		typeBlackList.add("şarkı");
		typeBlackList.add("dergi");
		typeBlackList.add("dizi");
		typeBlackList.add("gazete");
		typeBlackList.add("karakter");
		typeBlackList.add("ilçe");
	}

	// public static void updateWeights(int p, int n, int l, int s, int w, int
	// t, int tw, int d, int a, int w2, int e, int l2) {
	// popularityWeight = p;
	// nameWeight = n;
	// letterCaseWeight = l;
	// suffixWeight = s;
	// wordvectorAverageDocumentWeight = w;
	// typeContentWeight = t;
	// typeWeight = tw;
	// domainWeight = d;
	// autoVecWeight = a;
	// word2vecWeight = w2;
	// eeWeight = e;
	// linkWeight = l2;
	//
	// }

	@Override
	public EntityMatchList disambiguate(DexterLocalParams localParams,
			SpotMatchList sml) {
		entityScoreMap = new HashMap<String, EntityScores>();
		selectedEntities = new HashSet<String>();
		Multiset<String> entityFrequencyMultiset = HashMultiset.create();

		EntityMatchList entities = sml.getEntities();
		String inputText = localParams.getParams().get("text");
		String algorithm = Property.getInstance().get("algorithm");

		String ambigious = Property.getInstance().get("algorithm.ambigious");

		List<Token> inputTokens = Zemberek.getInstance()
				.disambiguateFindTokens(inputText, false, true);
		List<Double> documentVector = DescriptionEmbeddingAverage
				.getAverageVectorList(inputText);
		Multiset<String> inputTokensMultiset = HashMultiset.create();
		for (Token token : inputTokens) {
			inputTokensMultiset.add(token.getMorphText());
		}

		Multiset<String> domainMultiset = HashMultiset.create();
		Multiset<String> typeMultiset = HashMultiset.create();
		HashMap<String, Double> entitySimMap = new HashMap<String, Double>();
		// if (printCandidateEntities) {
		// printEntities(entities);
		// }
		HashSet<String> words = new HashSet<String>();
		Multiset<String> leskWords = HashMultiset.create();

		// first pass for finding number of types and domains
		for (int i = 0; i < entities.size(); i++) {
			EntityMatch em = entities.get(i);
			String id = em.getId();
			if (!entityFrequencyMultiset.contains(id)) {
				entityFrequencyMultiset.add(id);
				Entity entity = em.getEntity();
				words.add(entity.getShingle().getText());
				String type = entity.getPage().getType();
				if (type != null && type.length() > 0) {
					typeMultiset.add(type);
				}
				String domain = entity.getPage().getDomain();
				if (domain != null && domain.length() > 0) {
					domainMultiset.add(domain);
				}

				String desc = entity.getPage().getDescription();
				List<Token> tokens = Zemberek.getInstance()
						.disambiguateFindTokens(desc, false, true);
				for (Token token : tokens) {
					leskWords.add(token.getMorphText());
				}

			} else {
				entityFrequencyMultiset.add(id);
			}
		}

		int maxDomainCount = 0;
		for (String domain : Multisets.copyHighestCountFirst(domainMultiset)
				.elementSet()) {
			maxDomainCount = domainMultiset.count(domain);
			break;
		}
		int maxTypeCount = 0;
		for (String type : Multisets.copyHighestCountFirst(typeMultiset)
				.elementSet()) {
			maxTypeCount = typeMultiset.count(type);
			break;
		}

		double maxSuffixScore = 0, maxLeskScore = 0, maxSimpleLeskScore = 0, maxLinkScore = 0, maxHashInfoboxScore = 0, maxwordvecDescriptionLocalScore = 0, maxHashDescriptionScore = 0, maxPopularityScore = 0, maxWordvectorAverage = 0, maxWordvecLinksScore = 0;
		// second pass compute similarities between entities in a window
		int currentSpotIndex = -1;
		SpotMatch currentSpot = null;
		for (int i = 0; i < entities.size(); i++) {
			EntityMatch em = entities.get(i);
			SpotMatch spot = em.getSpot();
			if (currentSpot == null || spot != currentSpot) {
				currentSpotIndex++;
				currentSpot = spot;
			}

			String id = em.getId();
			Entity entity = entities.get(i).getEntity();
			EntityPage page = entities.get(i).getEntity().getPage();
			String domain = page.getDomain();
			String type = page.getType();
			Shingle shingle = entity.getShingle();

			/* windowing algorithms stars */
			int left = currentSpotIndex - window;
			int right = currentSpotIndex + window;
			if (left < 0) {
				right -= left;
				left = 0;
			}
			if (right > sml.size()) {
				left += (sml.size()) - right;
				right = sml.size();
				if (left < 0) {
					left = 0;
				}
			}

			double linkScore = 0, hashInfoboxScore = 0, wordvecDescriptionLocalScore = 0, hashDescriptionScore = 0, wordvecLinksScore = 0;
			for (int j = left; j < right; j++) {
				SpotMatch sm2 = sml.get(j);
				EntityMatchList entities2 = sm2.getEntities();
				for (EntityMatch em2 : entities2) {
					String id2 = em2.getId();
					EntityPage page2 = em2.getEntity().getPage();
					int counter = 0;
					if (!ambigious.equals("true")) {
						for (EntityMatch entityMatch : entities2) {
							if (entityMatch.getId().startsWith("w")) {
								counter++;
							}
						}
					}

					if ((ambigious.equals("true") || counter == 1)
							&& em.getSpot() != em2.getSpot() && !id.equals(id2)) {
						// Link Similarity calculation starts
						double linkSim = 0;
						if (id.startsWith("w") && id2.startsWith("w")) {
							if (entitySimMap.containsKey("link" + id + id2)) {
								linkSim = entitySimMap.get("link" + id + id2);
							} else {
								HashSet<String> set1 = Sets.newHashSet(page
										.getLinks().split(" "));
								HashSet<String> set2 = Sets.newHashSet(page2
										.getLinks().split(" "));
								linkSim = JaccardCalculator
										.calculateSimilarity(set1, set2);
								entitySimMap.put("link" + id + id2, linkSim);
							}
							linkScore += linkSim;
							// Link Similarity calculation ends
						}
						// Entity embedding similarity calculation starts
						double eeSim = 0;
						if (id.startsWith("w") && id2.startsWith("w")) {
							if (entitySimMap.containsKey("ee" + id + id2)) {
								eeSim = entitySimMap.get("ee" + id + id2);
							} else {
								eeSim = EntityEmbeddingSimilarity.getInstance()
										.getSimilarity(page, page2);
								entitySimMap.put("ee" + id + id2, eeSim);
							}
							hashInfoboxScore += eeSim;
						}
						double w2veclinksSim = 0;
						if (id.startsWith("w") && id2.startsWith("w")) {
							if (entitySimMap.containsKey("wl" + id + id2)) {
								w2veclinksSim = entitySimMap.get("wl" + id
										+ id2);
							} else {
								w2veclinksSim = AveragePooling.getInstance()
										.getSimilarity(page.getWord2vec(),
												page2.getWord2vec());
								entitySimMap
										.put("wl" + id + id2, w2veclinksSim);
							}
							wordvecLinksScore += w2veclinksSim;
						}

						// Entity embedding similarity calculation ends

						// Description word2vec similarity calculation
						// starts
						double word2vecSim = 0;

						if (entitySimMap.containsKey("w2v" + id + id2)) {
							word2vecSim = entitySimMap.get("w2v" + id + id2);
						} else {
							word2vecSim = AveragePooling.getInstance()
									.getSimilarity(page2.getDword2vec(),
											page.getDword2vec());
							entitySimMap.put("w2v" + id + id2, word2vecSim);
						}
						wordvecDescriptionLocalScore += word2vecSim;
						// Description word2vec similarity calculation ends

						// Description autoencoder similarity calculation
						// starts
						double autoVecSim = 0;

						if (entitySimMap.containsKey("a2v" + id + id2)) {
							autoVecSim = entitySimMap.get("a2v" + id + id2);
						} else {
							autoVecSim = AveragePooling.getInstance()
									.getSimilarity(page2.getDautoencoder(),
											page.getDautoencoder());
							entitySimMap.put("a2v" + id + id2, autoVecSim);
						}
						hashDescriptionScore += autoVecSim;
						// Description autoencoder similarity calculation
						// ends

					}
				}
			}
			if (linkScore > maxLinkScore) {
				maxLinkScore = linkScore;
			}
			if (hashInfoboxScore > maxHashInfoboxScore) {
				maxHashInfoboxScore = hashInfoboxScore;
			}
			if (wordvecDescriptionLocalScore > maxwordvecDescriptionLocalScore) {
				maxwordvecDescriptionLocalScore = wordvecDescriptionLocalScore;
			}
			if (hashDescriptionScore > maxHashDescriptionScore) {
				maxHashDescriptionScore = hashDescriptionScore;
			}
			if (wordvecLinksScore > maxWordvecLinksScore) {
				maxWordvecLinksScore = wordvecLinksScore;
			}

			/* windowing algorithms ends */

			double domainScore = 0;
			if (domainMultiset.size() > 0 && maxDomainCount > 1
					&& domainMultiset.count(domain) > 1) {
				domainScore = (double) domainMultiset.count(domain)
						/ maxDomainCount;
			}
			double typeScore = 0;
			if (typeMultiset.size() > 0 && maxTypeCount > 1
					&& typeMultiset.count(type) > 1) {
				typeScore = (double) typeMultiset.count(type) / maxTypeCount;
			}
			if (typeBlackList.contains(type)) {
				typeScore /= 10;
			}

			double typeContentScore = 0;
			if (type.length() > 0
					&& StringUtils.containsIgnoreCase(words.toString(), type)) {
				typeContentScore = 1;
			}

			double typeClassifierScore = TypeClassifier.getInstance().predict(
					page, page.getTitle(), page.getType(),
					entity.getShingle().getSentence());

			double wordvecDescriptionScore = AveragePooling.getInstance()
					.getSimilarity(documentVector, page.getDword2vec());
			if (wordvecDescriptionScore > maxWordvectorAverage) {
				maxWordvectorAverage = wordvecDescriptionScore;
			}

			double suffixScore = 0;

			if (type != null && type.length() > 0) {
				Set<String> suffixes = new HashSet<String>();
				String t = entity.getTitle()
						.toLowerCase(new Locale("tr", "TR"));

				for (int x = 0; x < entities.size(); x++) {
					EntityMatch e2 = entities.get(x);
					if (e2.getId().equals(entity.getId())) {
						suffixes.add(e2.getMention());
					}
				}
				suffixes.remove(t);
				suffixes.remove(entity.getTitle());
				// String inputTextLower = inputText.toLowerCase(new
				// Locale("tr",
				// "TR"));
				// while (inputTextLower.contains(t)) {
				// int start = inputTextLower.indexOf(t);
				// int end = inputTextLower.indexOf(" ", start + t.length());
				// if (end > start) {
				// String suffix = inputTextLower.substring(start, end);
				// // .replaceAll("\\W", "");
				// if (suffix.contains("'")
				// || (Zemberek.getInstance().hasMorph(suffix)
				// && !suffix.equals(t) && suffix.length() > 4)) {
				// suffixes.add(suffix);
				// }
				// inputTextLower = inputTextLower.substring(end);
				// } else {
				// break;
				// }
				// }
				if (suffixes.size() >= minSuffix) {
					for (String suffix : suffixes) {
						double sim = gd.calculateSimilarity(suffix, type);
						suffixScore += sim;
					}
				}
			}

			// String entitySuffix = page.getSuffix();
			// String[] inputSuffix = shingle.getSuffix().split(" ");
			// for (int j = 0; j < inputSuffix.length; j++) {
			// if (entitySuffix.contains(inputSuffix[j])) {
			// suffixScore += 0.25f;
			// }
			// }

			if (suffixScore > maxSuffixScore) {
				maxSuffixScore = suffixScore;
			}
			// if (id.equals("w691538")) {
			// LOGGER.info("");
			// }
			double letterCaseScore = 0;
			int lc = page.getLetterCase();
			if (StringUtils.isAllLowerCase(em.getMention()) && lc == 0
					&& id.startsWith("t")) {
				letterCaseScore = 1;
			} else if (StringUtils.isAllUpperCase(em.getMention()) && lc == 1
					&& id.startsWith("w")) {
				letterCaseScore = 1;
			} else if (Character.isUpperCase(em.getMention().charAt(0))
					&& lc == 2 && id.startsWith("w")) {
				letterCaseScore = 1;
			} else if (StringUtils.isAllLowerCase(em.getMention())
					&& id.startsWith("t")) {
				letterCaseScore = 1;
			}

			double nameScore = 1 - LevenshteinDistanceCalculator
					.calculateDistance(page.getTitle(),
							Zemberek.removeAfterSpostrophe(em.getMention()));

			double popularityScore = page.getRank();
			if (id.startsWith("w")) {
				popularityScore = Math.log10(popularityScore + 1);
				if (popularityScore > maxPopularityScore) {
					maxPopularityScore = popularityScore;
				}
			}

			double leskScore = 0, simpleLeskScore = 0;

			String desc = em.getEntity().getPage().getDescription();
			if (desc != null) {
				List<Token> tokens = Zemberek.getInstance()
						.disambiguateFindTokens(desc, false, true);
				for (Token token : tokens) {
					if (inputTokensMultiset.contains(token.getMorphText())
							&& !TurkishNLP.isStopWord(token.getMorphText())) {
						simpleLeskScore += inputTokensMultiset.count(token
								.getMorphText());
					}
					if (leskWords.contains(token.getMorphText())
							&& !TurkishNLP.isStopWord(token.getMorphText())) {
						leskScore += leskWords.count(token.getMorphText());
					}

				}
				leskScore /= Math.log(tokens.size() + 1);
				simpleLeskScore /= Math.log(tokens.size() + 1);
				if (leskScore > maxLeskScore) {
					maxLeskScore = leskScore;
				}
				if (simpleLeskScore > maxSimpleLeskScore) {
					maxSimpleLeskScore = simpleLeskScore;
				}

				if (!entityScoreMap.containsKey(id)) {
					EntityScores scores = new EntityScores(em, id,
							popularityScore, nameScore, letterCaseScore,
							suffixScore, wordvecDescriptionScore,
							typeContentScore, typeScore, domainScore,
							hashDescriptionScore, wordvecDescriptionLocalScore,
							hashInfoboxScore, linkScore, wordvecLinksScore,
							leskScore, simpleLeskScore, typeClassifierScore);
					entityScoreMap.put(id, scores);
				} else {
					EntityScores entityScores = entityScoreMap.get(id);
					entityScores.setHashInfoboxScore((entityScores
							.getHashInfoboxScore() + hashInfoboxScore) / 2);
					entityScores.setHashDescriptionScore((entityScores
							.getHashInfoboxScore() + hashDescriptionScore) / 2);
					entityScores
							.setLinkScore((entityScores.getLinkScore() + linkScore) / 2);
					entityScores
							.setWordvecDescriptionLocalScore((entityScores
									.getWordvecDescriptionLocalScore() + wordvecDescriptionLocalScore) / 2);
					entityScores.setWordvecLinksScore((entityScores
							.getWordvecLinksScore() + wordvecLinksScore) / 2);
					entityScores
							.setLeskScore((entityScores.getLeskScore() + leskScore) / 2);

				}

			}
		}
		/* normalization and total score calculation starts */
		Set<String> set = new HashSet<String>();
		for (int i = 0; i < entities.size(); i++) {
			EntityMatch em = entities.get(i);
			String id = em.getId();
			EntityScores entityScores = entityScoreMap.get(id);
			if (set.contains(id)) {
				continue;
			}
			if (id.startsWith("w")) {
				if (maxLinkScore > 0 && entityScores.getLinkScore() > 0) {
					entityScores.setLinkScore(entityScores.getLinkScore()
							/ maxLinkScore);
				}
				if (maxHashInfoboxScore > 0
						&& entityScores.getHashInfoboxScore() > 0) {
					entityScores.setHashInfoboxScore(entityScores
							.getHashInfoboxScore() / maxHashInfoboxScore);
				}
				if (maxWordvecLinksScore > 0
						&& entityScores.getWordvecLinksScore() > 0) {
					entityScores.setWordvecLinksScore(entityScores
							.getWordvecLinksScore() / maxWordvecLinksScore);
				}
				if (maxPopularityScore > 0
						&& entityScores.getPopularityScore() > 0) {
					entityScores.setPopularityScore(entityScores
							.getPopularityScore() / maxPopularityScore);
				}
			}
			if (maxwordvecDescriptionLocalScore > 0
					&& entityScores.getWordvecDescriptionLocalScore() > 0) {
				entityScores.setWordvecDescriptionLocalScore(entityScores
						.getWordvecDescriptionLocalScore()
						/ maxwordvecDescriptionLocalScore);
			}
			if (maxHashDescriptionScore > 0
					&& entityScores.getHashDescriptionScore() > 0) {
				entityScores.setHashDescriptionScore(entityScores
						.getHashDescriptionScore() / maxHashDescriptionScore);
			}
			if (maxWordvectorAverage > 0
					&& entityScores.getWordvecDescriptionScore() > 0) {
				entityScores.setWordvecDescriptionScore(entityScores
						.getWordvecDescriptionScore() / maxWordvectorAverage);
			}
			if (maxLeskScore > 0 && entityScores.getLeskScore() > 0) {
				entityScores.setLeskScore(entityScores.getLeskScore()
						/ maxLeskScore);
			}
			if (maxSimpleLeskScore > 0 && entityScores.getSimpleLeskScore() > 0) {
				entityScores.setSimpleLeskScore(entityScores
						.getSimpleLeskScore() / maxSimpleLeskScore);
			}
			if (maxSuffixScore > 0 && entityScores.getSuffixScore() > 0) {
				entityScores.setSuffixScore(entityScores.getSuffixScore()
						/ maxSuffixScore);
			}
			set.add(id);
		}

		LOGGER.info("\t"
				+ "id\tTitle\tURL\tScore\tPopularity\tName\tLesk\tSimpeLesk\tCase\tNoun\tSuffix\tTypeContent\tType\tDomain\twordvecDescription\twordvecDescriptionLocal\thashDescription\thashInfobox\tword2vecLinks\tLink\t\ttypeClassifier\tDescription");
		for (int i = 0; i < entities.size(); i++) {
			EntityMatch em = entities.get(i);
			String id = em.getId();
			EntityScores e = entityScoreMap.get(id);
			double wikiScore = 0;
			if (id.startsWith("w")
					&& Character.isUpperCase(em.getMention().charAt(0))) {
				wikiScore = wikiWeight;
			} else if (id.startsWith("t")
					&& Character.isLowerCase(em.getMention().charAt(0))) {
				wikiScore = wikiWeight;
			}
			// if(id.equals("w508792")){
			// LOGGER.info("");
			// }
			double totalScore = wikiScore + e.getPopularityScore()
					* popularityWeight + e.getNameScore() * nameWeight
					+ e.getLeskScore() * leskWeight + e.getSimpleLeskScore()
					* simpleLeskWeight + e.getLetterCaseScore()
					* letterCaseWeight + e.getSuffixScore() * suffixWeight
					+ e.getTypeContentScore() * typeContentWeight
					+ e.getTypeScore() * typeWeight + e.getDomainScore()
					* domainWeight + e.getWordvecDescriptionScore()
					* wordvecDescriptionWeight
					+ e.getWordvecDescriptionLocalScore()
					* wordvecDescriptionLocalWeight
					+ e.getHashDescriptionScore() * hashDescriptionWeight
					+ e.getHashInfoboxScore() * hashInfoboxWeight
					+ e.getWordvecLinksScore() * word2vecLinksWeight
					+ e.getLinkScore() * linkWeight
					+ e.getTypeClassifierkScore() * typeClassifierkWeight;
			if (ranklib == true) {
				totalScore = RankLib.getInstance().score(e);
			}

			if (em.getEntity().getPage().getUrlTitle().contains("(")) {
				totalScore /= 2;
			}
			em.setScore(totalScore);
			e.setScore(totalScore);

			LOGGER.info("\t" + id + "\t" + em.getEntity().getPage().getTitle()
					+ "\t" + em.getEntity().getPage().getUrlTitle() + "\t"
					+ em.getScore() + "\t"
					+ e.getPopularityScore() * popularityWeight + "\t"
					+ e.getNameScore() * nameWeight + "\t"
					+ e.getLeskScore() * leskWeight + "\t"
					+ e.getSimpleLeskScore() * simpleLeskWeight + "\t"
					+ e.getLetterCaseScore() * letterCaseWeight + "\t"
					+ e.getSuffixScore() * suffixWeight + "\t"
					+ e.getTypeContentScore() * typeContentWeight + "\t"
					+ e.getTypeScore() * typeWeight + "\t" + e.getDomainScore()
					* domainWeight + "\t" + e.getWordvecDescriptionScore()
					* wordvecDescriptionWeight + "\t"
					+ e.getWordvecDescriptionLocalScore()
					* wordvecDescriptionLocalWeight + "\t"
					+ e.getHashDescriptionScore() * hashDescriptionWeight
					+ "\t" + e.getHashInfoboxScore() * hashInfoboxWeight + "\t"
					+ e.getWordvecLinksScore() * word2vecLinksWeight + "\t"
					+ e.getLinkScore() * linkWeight + "\t"
					+ e.getTypeClassifierkScore() * typeClassifierkWeight
					+ "\t" + em.getEntity().getPage().getDescription());
		}

		// if (annotateEntities) {
		// annotateEntities(localParams.getParams().get("originalText"), sml);
		// }

		EntityMatchList eml = new EntityMatchList();
		for (SpotMatch match : sml) {
			EntityMatchList list = match.getEntities();
			if (!list.isEmpty()) {
				list.sort();
				eml.add(list.get(0));
				selectedEntities.add(list.get(0).getId());
			}
		}
		return eml;
	}

	private void annotateEntities(String inputText, SpotMatchList sml) {
		File file = new File(candidateEntitiesFileName);
		if (!file.exists()) {

			BufferedReader br = new BufferedReader(new InputStreamReader(
					System.in));
			HashMap<String, String> annotations = new HashMap<String, String>();
			for (SpotMatch match : sml) {
				if (annotations.get(match.getMention()) == null) {

					EntityMatchList list = match.getEntities();
					int i = 0;
					int start = inputText.substring(0, match.getStart())
							.lastIndexOf(".");
					if (start == -1) {
						start = 0;
					}
					int end = inputText.indexOf(".", match.getEnd());
					if (end == -1) {
						end = inputText.length();
					}
					String text = inputText.substring(start, end);
					System.out.println(text);
					System.out.println("None=-1");
					for (EntityMatch entityMatch : list) {
						System.out.println(i++
								+ " "
								+ entityMatch.getMention()
								+ "="
								+ entityMatch.getEntity().getPage()
										.getUrlTitle());
					}
					System.out.println("Input for " + match.getMention());
					try {
						int input = Integer.parseInt(br.readLine());
						if (input == -1) {
							annotations.put(match.getMention(), "-1");
						}

						else {
							annotations.put(match.getMention(), list.get(input)
									.getEntity().getPage().getUrlTitle());
						}
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
						annotations.put(match.getMention(), "-1");
					}
				}
			}
			Writer writer;
			try {
				writer = new FileWriter(candidateEntitiesFileName);
				Gson gson = new GsonBuilder().create();
				gson.toJson(annotations, writer);

				writer.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	private boolean isBlackListEntity(int id) {
		return BlackListEntities.getInstance().isBlackListEntity(
				String.valueOf(id));
	}

	private void printEntities(EntityMatchList entities) {
		StringBuffer sb = new StringBuffer("");
		boolean flag = false;
		for (int i = 0; i < entities.size(); i++) {
			sb.append(entities.get(i).getEntity().getId() + "\n");
			if (entities.get(i).getEntity().getId().equals(candidateEntitiyId)) {
				flag = true;
			}
		}
		if (!flag) {
			LOGGER.info("no match bad experiment data for "
					+ candidateEntitiesFileName);
		}
		FileUtils.writeFileOverWrite(sb.toString(), candidateEntitiesFileName);
	}

	@Override
	public void init(DexterParams dexterParams,
			DexterLocalParams dexterModuleParams) {

	}

}
