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
package it.cnr.isti.hpc.dexter.spotter;

import it.cnr.isti.hpc.dexter.common.Document;
import it.cnr.isti.hpc.dexter.common.Field;
import it.cnr.isti.hpc.dexter.entity.EntityMatchList;
import it.cnr.isti.hpc.dexter.entity.EntityRanker;
import it.cnr.isti.hpc.dexter.shingle.Shingle;
import it.cnr.isti.hpc.dexter.shingle.ShingleExtractor;
import it.cnr.isti.hpc.dexter.spot.Spot;
import it.cnr.isti.hpc.dexter.spot.SpotMatch;
import it.cnr.isti.hpc.dexter.spot.SpotMatchList;
import it.cnr.isti.hpc.dexter.spot.TurkishSpotRepository;
import it.cnr.isti.hpc.dexter.spot.repo.SpotRepository;
import it.cnr.isti.hpc.dexter.spot.repo.SpotRepositoryFactory;
import it.cnr.isti.hpc.dexter.util.DexterLocalParams;
import it.cnr.isti.hpc.dexter.util.DexterParams;
import it.cnr.isti.hpc.structure.LRUCache;

import java.util.Iterator;

import org.apache.log4j.Logger;

import edu.yeditepe.utils.Constants;

/**
 * Spotter
 * 
 * @author Diego Ceccarelli, diego.ceccarelli@isti.cnr.it created on 01/ago/2012
 */
public class DictionarySpotter extends AbstractSpotter implements Spotter {
	/**
	 * Logger for this class
	 */
	private static final Logger LOGGER = Logger
			.getLogger(DictionarySpotter.class);

	private static LRUCache<String, Spot> cache;

	DexterParams params = DexterParams.getInstance();

	SpotRepository spotRepo;
	private final boolean usePriorProbability = false;

	public DictionarySpotter() {
		int cachesize = params.getCacheSize("spotter");
		cache = new LRUCache<String, Spot>(cachesize);

		// spotRepo = factory.getStdInstance();
	}

	@Override
	public SpotMatchList match(DexterLocalParams localParams, Document document) {
		String lang = localParams.getParams().get("lang");
		String inputText = localParams.getParams().get("text");
		if (lang == null) {
			lang = "tr";
			localParams.addParam("lang", lang);
		}
		if (lang.equals(Constants.TURKISH)) {
			spotRepo = new TurkishSpotRepository();
		} else if (spotRepo == null) {
			SpotRepositoryFactory factory = new SpotRepositoryFactory();
			spotRepo = factory.getStdInstance();
		}
		SpotMatchList matches = new SpotMatchList();

		Iterator<Field> fields = document.getFields();
		while (fields.hasNext()) {

			Field field = fields.next();
			EntityRanker er = new EntityRanker(field);
			ShingleExtractor shingler = new ShingleExtractor(field.getValue(),
					lang);
			Spot s = null;
			String text;
			for (Shingle shingle : shingler) {
				LOGGER.debug("SHINGLE: [{}] " + shingle);
				text = shingle.getText();
				// if (cache.containsKey(text)) {
				// // hit in cache
				// s = cache.get(text);
				// if (s != null) {
				// s = s.clone();
				// }
				// } else {
				try {
					s = spotRepo.getSpot(
							shingle.getText(),
							inputText.substring(shingle.getStart(),
									shingle.getEnd()), inputText, shingle);
					// cache.put(text, s);
				} catch (Exception e) {
					LOGGER.error(e);
				}

				// }

				if (s == null) {
					LOGGER.debug("no shingle for [{}] " + shingle);
					continue;
				}

				// s.setStart(shingle.getStart());
				// s.setEnd(shingle.getEnd());

				// int pos = matches.index(s);
				// if (pos >= 0) {
				// // the spot is yet in the list, increment its occurrences
				// matches.get(pos).incrementOccurrences();
				// continue;
				// }
				SpotMatch match = new SpotMatch(s, field);
				LOGGER.debug("adding {} to matchset " + s);

				EntityMatchList entities = er.rank(match);
				match.setEntities(entities);
				match.setStart(shingle.getStart());
				match.setEnd(shingle.getEnd());
				matches.add(match);

			}
		}
		matches = filter(localParams, matches);
		return matches;
	}

	@Override
	public void init(DexterParams dexterParams,
			DexterLocalParams defaultModuleParams) {

	}

}
