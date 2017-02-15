package edu.yeditepe.controller;

import it.cnr.isti.hpc.dexter.StandardTagger;
import it.cnr.isti.hpc.dexter.Tagger;
import it.cnr.isti.hpc.dexter.common.ArticleDescription;
import it.cnr.isti.hpc.dexter.common.Field;
import it.cnr.isti.hpc.dexter.common.FlatDocument;
import it.cnr.isti.hpc.dexter.common.MultifieldDocument;
import it.cnr.isti.hpc.dexter.disambiguation.Disambiguator;
import it.cnr.isti.hpc.dexter.entity.EntityMatch;
import it.cnr.isti.hpc.dexter.entity.EntityMatchList;
import it.cnr.isti.hpc.dexter.label.IdHelper;
import it.cnr.isti.hpc.dexter.label.IdHelperFactory;
import it.cnr.isti.hpc.dexter.rest.domain.AnnotatedDocument;
import it.cnr.isti.hpc.dexter.rest.domain.AnnotatedSpot;
import it.cnr.isti.hpc.dexter.rest.domain.Tagmeta;
import it.cnr.isti.hpc.dexter.shingle.ShingleExtractor;
import it.cnr.isti.hpc.dexter.spotter.Spotter;
import it.cnr.isti.hpc.dexter.util.DexterLocalParams;
import it.cnr.isti.hpc.dexter.util.DexterParams;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import edu.yeditepe.lucene.EntitySearchEngine;
import edu.yeditepe.utils.Constants;
import edu.yeditepe.utils.Property;

@RequestMapping("/api/rest")
@RestController
public class AnnotationController {
	private static final Logger LOGGER = Logger
			.getLogger(AnnotationController.class);

	public static final DexterParams params = DexterParams.getInstance();

	public static final IdHelper helper = IdHelperFactory.getStdIdHelper();

	private static Gson gson = new GsonBuilder()
			.serializeSpecialFloatingPointValues().create();

	@RequestMapping(value = "/embedding", method = RequestMethod.GET, produces = "application/json; charset=utf-8")
	public @ResponseBody ArticleDescription embedding(
			@RequestParam(value = "text") String text) {
		ArticleDescription an = new ArticleDescription();

		TreeMap<Double, String> similarEntities = EntitySearchEngine
				.getInstance().getSimilarEntities(text);
		if (similarEntities.isEmpty()) {
			an.setDescription("No result");
			;
		} else {
			String output = "";

			for (Double sim : similarEntities.keySet()) {
				output += similarEntities.get(sim) + ": " + sim + "<br>";
			}
			an.setDescription(output);
			;
		}
		return an;
	}

	@RequestMapping(value = "/annotate", method = RequestMethod.GET, produces = "application/json; charset=utf-8")
	public @ResponseBody AnnotatedDocument annotate(
			@RequestParam(value = "text") String text,
			@RequestParam(value = "myspotter", required = false, defaultValue = "true") String myspotter,
			@RequestParam(value = "n", required = false, defaultValue = "50") String n,
			@RequestParam(value = "spt", required = false) String spotter,
			@RequestParam(value = "dsb", required = false) String disambiguator,
			@RequestParam(value = "wn", required = false, defaultValue = "false") String wikiNames,
			@RequestParam(value = "debug", required = false, defaultValue = "false") String dbg,
			@RequestParam(value = "format", required = false, defaultValue = "text") String format,
			@RequestParam(value = "min-conf", required = false, defaultValue = "0.5") String minConfidence,
			@RequestParam(value = "lang", required = false, defaultValue = "tr") String lang)
			throws ClassNotFoundException, InstantiationException,
			IllegalAccessException {
		if (myspotter != null) {
			try {
				ShingleExtractor.myspotter = Boolean.parseBoolean(myspotter);
			} catch (Exception e) {
				// TODO: handle exception
			}
		}

		DexterLocalParams params = new DexterLocalParams();

		return annotate(params, text, n, spotter, disambiguator, wikiNames,
				dbg, format, minConfidence, lang);
	}

	@RequestMapping(value = "/annotate", method = RequestMethod.POST, produces = "application/json; charset=utf-8")
	public @ResponseBody AnnotatedDocument annotatePost(
			@RequestParam(value = "text") String text,
			@RequestParam(value = "myspotter", required = false, defaultValue = "true") String myspotter,
			@RequestParam(value = "n", required = false, defaultValue = "1500") String n,
			@RequestParam(value = "spt", required = false) String spotter,
			@RequestParam(value = "dsb", required = false) String disambiguator,
			@RequestParam(value = "wn", required = false, defaultValue = "false") String wikiNames,
			@RequestParam(value = "debug", required = false, defaultValue = "false") String dbg,
			@RequestParam(value = "format", required = false, defaultValue = "text") String format,
			@RequestParam(value = "min-conf", required = false, defaultValue = "0.5") String minConfidence,
			@RequestParam(value = "lang", required = false, defaultValue = "tr") String lang)
			throws ClassNotFoundException, InstantiationException,
			IllegalAccessException {
		if (myspotter != null) {
			try {
				ShingleExtractor.myspotter = Boolean.parseBoolean(myspotter);
			} catch (Exception e) {
				// TODO: handle exception
			}
		}
		DexterLocalParams params = new DexterLocalParams();
		return annotate(params, text, n, spotter, disambiguator, wikiNames,
				dbg, format, minConfidence, lang);
	}

	@RequestMapping(value = "/get-desc", method = RequestMethod.GET, produces = "application/json; charset=utf-8")
	public @ResponseBody ArticleDescription getDescription(
			@RequestParam(value = "id") String id,
			@RequestParam(value = "title-only", required = false, defaultValue = "false") String titleonly,
			@RequestParam(value = "lang", required = false, defaultValue = "tr") String lang) {
		ArticleDescription desc = null;
		// int i = Integer.parseInt(id);
		if (lang.equals(Constants.TURKISH)) {
			desc = EntitySearchEngine.getInstance().getPage(id);
		} else {

			// boolean titleOnly = new Boolean(titleonly);
			// if (titleOnly) {
			// desc = server.getOnlyEntityLabel(i);
			// desc.setDescription(null);
			// desc.setImage(null);
			// return desc.toJson();
			//
			// }
			// desc = server.get(i);
		}

		if (desc == null) {
			LOGGER.warn("description for id {} is null " + id);
			desc = ArticleDescription.EMPTY;
		}
		// desc.setImage("");
		// desc.setInfobox(new HashMap<String, String>());
		desc.setId(0);

		LOGGER.info("getDescription: {}" + desc.getUri());
		return desc;

	}

	public static AnnotatedDocument annotate(DexterLocalParams requestParams,
			String text, String n, String spotter, String disambiguator,
			String wikiNames, String dbg, String format,
			String minConfidenceStr, String lang) {
		text = text.replaceAll("’", "'");
		text = text.replaceAll("\"", "");
		text = text.replaceAll("-", " ");
		requestParams.addParam("originalText", text);
		// Pattern p = Pattern.compile("[\"\\-,;:!?(){}\\[\\]<>%‘]");
		// text = p.matcher(text).replaceAll(" ");
		text = text.replaceAll("\n", " ");
		text = text.replaceAll("\r", " ");
		text = text.replaceAll("\\s+", " ").trim();
		requestParams.addParam("text", text);
		if (text == null) {
			return null;
		}

		Spotter s = params.getSpotter(spotter);

		Disambiguator d = params.getDisambiguator(disambiguator);
		Tagger tagger = new StandardTagger("std", s, d);

		Boolean debug = new Boolean(dbg);
		boolean addWikinames = new Boolean(wikiNames);

		Integer entitiesToAnnotate = Integer.parseInt(n);
		// double minConfidence = Double.parseDouble(minConfidenceStr);
		double minConfidence = Double.parseDouble(Property.getInstance().get(
				"disambiguation.minConfidence"));

		MultifieldDocument doc = null;
		try {
			doc = parseDocument(text, format);
		} catch (IllegalArgumentException e) {
			LOGGER.error(e);
			return null;
		}

		EntityMatchList eml = tagger.tag(requestParams, doc);

		AnnotatedDocument adoc = new AnnotatedDocument(doc);

		if (debug) {
			Tagmeta meta = new Tagmeta();
			meta.setDisambiguator(d.getClass().toString());
			meta.setSpotter(s.getClass().toString());
			meta.setFormat(format);
			meta.setRequestParams(requestParams.getParams());

			adoc.setMeta(meta);

		}

		annotate(adoc, eml, entitiesToAnnotate, addWikinames, minConfidence);

		// logger.info("annotate: {}", annotated);
		return adoc;
	}

	private static MultifieldDocument parseDocument(String text, String format) {
		Tagmeta.DocumentFormat df = Tagmeta.getDocumentFormat(format);
		MultifieldDocument doc = null;
		if (df == Tagmeta.DocumentFormat.TEXT) {
			doc = new FlatDocument(text);
		}
		if (df == Tagmeta.DocumentFormat.JSON) {
			doc = gson.fromJson(text, MultifieldDocument.class);

		}
		return doc;

	}

	public static void annotate(AnnotatedDocument adoc, EntityMatchList eml,
			int nEntities, boolean addWikiNames, double minConfidence) {
		String text = adoc.getDocument().getContent();
		eml.sort();
		EntityMatchList emlSub = new EntityMatchList();
		int size = Math.min(nEntities, eml.size());
		List<AnnotatedSpot> spots = adoc.getSpots();
		spots.clear();
		for (int i = 0; i < size; i++) {
			EntityMatch em = eml.get(i);
			try {

				if (em.getScore() < minConfidence
						&& !(Character.isUpperCase(text.charAt(em.getStart()))
								&& em.getStart() != 0 && text.charAt(em
								.getStart() - 2) != '.'
						// || !(TDK
						// .getInstance().isTurkishWord(
						// em.getEntity().getTitle()) || TDK
						// .getInstance().isTurkishWord(em.getMention())))
						// ||
						// (TDK.getInstance().isName(em.getEntity().getTitle())
						// || TDK
						// .getInstance().isName(em.getMention())
						)

				) {
					LOGGER.debug("remove entity {}, confidence {} to low"
							+ em.getId() + em.getScore());
					continue;
				}
			} catch (Exception e) {
				// TODO: handle exception
			}
			emlSub.add(em);
			AnnotatedSpot spot = new AnnotatedSpot(em.getMention(),
					em.getSpotLinkProbability(), em.getStart(), em.getEnd(), em
							.getSpot().getLinkFrequency(), em.getSpot()
							.getFrequency(), Integer.parseInt(em.getId()
							.substring(1)), em.getFrequency(),
					em.getCommonness(), em.getScore());
			spot.setField(em.getSpot().getField().getName());
			spot.setWikiname(em.getEntity().getPage().getUrlTitle());
			if (addWikiNames) {
				spot.setWikiname(helper.getLabel(Integer.parseInt(em.getId()
						.substring(1))));
			}

			spots.add(spot);
		}
		MultifieldDocument annotatedDocument = getAnnotatedDocument(adoc,
				emlSub);
		adoc.setAnnotatedDocument(annotatedDocument);
	}

	private static MultifieldDocument getAnnotatedDocument(
			AnnotatedDocument adoc, EntityMatchList eml) {
		Collections.sort(eml, new EntityMatch.SortByPosition());

		Iterator<Field> iterator = adoc.getDocument().getFields();
		MultifieldDocument annotated = new MultifieldDocument();
		while (iterator.hasNext()) {
			int pos = 0;
			StringBuffer sb = new StringBuffer();
			Field field = iterator.next();
			String currentField = field.getName();
			String currentText = field.getValue();

			for (EntityMatch em : eml) {
				if (!em.getSpot().getField().getName().equals(currentField)) {
					continue;
				}
				assert em.getStart() >= 0;
				assert em.getEnd() >= 0;
				try {
					sb.append(currentText.substring(pos, em.getStart()));
				} catch (java.lang.StringIndexOutOfBoundsException e) {
					LOGGER.debug("error annotating text output of bound for range {} - {} "
							+ pos + em.getStart());
					LOGGER.debug("text: \n\n {}\n\n" + currentText);
				}
				// the spot has been normalized, i want to retrieve the real one
				String realSpot = "none";
				try {
					realSpot = currentText
							.substring(em.getStart(), em.getEnd());
				} catch (java.lang.StringIndexOutOfBoundsException e) {
					LOGGER.debug("error annotating text output of bound for range {} - {} "
							+ pos + em.getStart());
					LOGGER.debug("text: \n\n {}\n\n" + currentText);
				}
				sb.append(
						"<a href=\"#\" onmouseover='manage(\"" + em.getId()
								+ "\", \"" + em.getEntity().getPage().getType()
								+ "\", \""
								+ em.getEntity().getPage().getPredictedType()
								+ "\")' >").append(realSpot).append("</a>");
				pos = em.getEnd();
			}
			if (pos < currentText.length()) {
				try {
					sb.append(currentText.substring(pos));
				} catch (java.lang.StringIndexOutOfBoundsException e) {
					LOGGER.debug("error annotating text output of bound for range {} - end "
							+ pos);
					LOGGER.debug("text: \n\n {}\n\n" + currentText);
				}

			}
			annotated.addField(new Field(field.getName(), sb.toString()));

		}

		return annotated;
	}
}
