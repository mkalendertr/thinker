//package edu.yeditepe.experiment;
//
//import it.cnr.isti.hpc.dexter.rest.domain.AnnotatedDocument;
//import it.cnr.isti.hpc.dexter.rest.domain.AnnotatedSpot;
//import it.cnr.isti.hpc.dexter.util.DexterLocalParams;
//
//import java.io.File;
//import java.io.IOException;
//import java.net.MalformedURLException;
//import java.util.HashMap;
//import java.util.List;
//import java.util.Locale;
//
//import javax.xml.parsers.DocumentBuilder;
//import javax.xml.parsers.DocumentBuilderFactory;
//
//import org.apache.log4j.Logger;
//import org.apache.solr.client.solrj.SolrQuery;
//import org.apache.solr.client.solrj.SolrServerException;
//import org.apache.solr.client.solrj.impl.HttpSolrServer;
//import org.apache.solr.client.solrj.response.QueryResponse;
//import org.apache.solr.common.SolrDocument;
//import org.apache.solr.common.SolrDocumentList;
//import org.apache.solr.common.SolrInputDocument;
//import org.w3c.dom.Document;
//import org.w3c.dom.Node;
//import org.w3c.dom.NodeList;
//
//import edu.yeditepe.controller.AnnotationController;
//import edu.yeditepe.nlp.Zemberek;
//import edu.yeditepe.utils.FileUtils;
//
//public class ExperimentSearch {
//	private static final Logger LOGGER = Logger.getLogger(Zemberek.class);
//
//	public static void main(String[] args) throws IOException,
//			SolrServerException {
//		indexDocuments();
//		evaluate();
//	}
//
//	public static void indexDocuments() throws SolrServerException, IOException {
//		HttpSolrServer server = new HttpSolrServer(
//				"http://localhost:8983/solr/milliyet2");
//		File all = new File("C:\\Milliyet\\docs");
//		File[] listOfFiles = all.listFiles();
//		for (File file : listOfFiles) {
//			if (file.isFile()) {
//
//				SolrInputDocument doc = new SolrInputDocument();
//
//				String id = file.getName().replace(".txt", "");
//				String text = parseDoc(file).replaceAll("\n", "").replaceAll(
//						"  ", " ");
//				DexterLocalParams params = new DexterLocalParams();
//				params.addParam("text", text);
//				AnnotatedDocument ad = AnnotationController.annotate(params,
//						text, "1000000", null, null, null, null, "text", "0",
//						"tr");
//				String content = Zemberek.getInstance().morphPageContent(
//						ad.getDocument().getContent()
//								.toLowerCase(new Locale("tr", "TR")));
//				List<AnnotatedSpot> annotatedSpots = ad.getSpots();
//				HashMap<String, Double> spots = new HashMap<String, Double>();
//				for (AnnotatedSpot annotatedSpot : annotatedSpots) {
//					String mention[] = annotatedSpot.getMention().split(" ");
//					for (String s : mention) {
//						s = Zemberek.getInstance().morphTokenToNoun(s)
//								.split(" ")[0];
//						if (!spots.containsKey(s)
//								|| spots.get(s) < annotatedSpot.getScore()) {
//							spots.put(s, annotatedSpot.getScore());
//
//						}
//					}
//					// spots.add(annotatedSpot.getMention().toLowerCase(
//					// new Locale("tr", "TR")));
//				}
//				String payload = "";
//				for (String spot : spots.keySet()) {
//					payload += spot + "|" + spots.get(spot) + " ";
//				}
//				for (String word : content.split(" ")) {
//					if (!spots.containsKey(word)) {
//						payload += word + " ";
//						spots.put(word, 1d);
//					}
//
//				}
//
//				LOGGER.info(content);
//				doc.addField("id", id);
//				doc.addField("name", text);
//				doc.addField("payloads", payload.trim());
//				server.add(doc);
//			}
//		}
//
//		server.commit();
//	}
//
//	public static void evaluate() throws MalformedURLException,
//			SolrServerException {
//		StringBuffer sb = new StringBuffer("");
//		List<String> queries = FileUtils.readFile("C:\\Milliyet\\queries.txt");
//		for (String query : queries) {
//			String qid = query.split("\t")[0];
//			String qName = query.split("\t")[1];
//			// if (!qid.equals("282")) {
//			// continue;
//			// }
//			SolrDocumentList results = search(qName);
//			for (int i = 0; i < results.size(); ++i) {
//				SolrDocument sd = results.get(i);
//				String rid = (String) sd.getFieldValue("id");
//				float score = Float.parseFloat(sd.getFieldValue("score")
//						.toString()) / 10;
//				LOGGER.info(qid + " Q0 " + rid + " " + i + " " + score
//						+ " default");
//				sb.append(qid + " Q0 " + rid + " " + i + " " + score
//						+ " default\n");
//				// 238 Q0 214938 997 0.026120087 default
//			}
//
//		}
//		FileUtils.writeFile(sb.toString(), "payload.out");
//	}
//
//	public static SolrDocumentList search(String queryString)
//			throws MalformedURLException, SolrServerException {
//
//		HttpSolrServer solr = new HttpSolrServer(
//				"http://localhost:8983/solr/milliyet2");
//		String[] q = queryString.toLowerCase(new Locale("tr", "TR")).split(" ");
//		queryString = "";
//		for (String s : q) {
//			queryString += Zemberek.getInstance().morphTokenToNoun(s)
//					.split(" ")[0]
//					+ " ";
//		}
//		queryString = queryString.trim();
//		SolrQuery query = new SolrQuery();
//		query.setQuery("payloads:" + queryString);
//		// query.addFilterQuery("");
//		query.setFields("id", "score");
//		// query.setStart(0);
//		query.set("defType", "myqp");
//		query.set("rows", "1000");
//		QueryResponse response = solr.query(query);
//		return response.getResults();
//
//	}
//
//	public static String parseDoc(File file) {
//		try {
//			DocumentBuilderFactory docFactory = DocumentBuilderFactory
//					.newInstance();
//			DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
//			Document xmlDoc = docBuilder.parse(file);
//
//			// Get the staff element by tag name directly
//			Node doc = xmlDoc.getElementsByTagName("DOC").item(0);
//
//			// loop the staff child node
//			NodeList list = doc.getChildNodes();
//
//			for (int i = 0; i < list.getLength(); i++) {
//
//				Node node = list.item(i);
//
//				// get the salary element, and update the value
//				if ("TEXT".equals(node.getNodeName())) {
//					return node.getTextContent();
//				}
//
//			}
//
//		} catch (Exception pce) {
//			pce.printStackTrace();
//		}
//		return null;
//
//	}
//
// }