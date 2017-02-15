package edu.yeditepe.similarity;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.hornetq.utils.json.JSONException;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import yandex.Yandex;

/**
 * This implements the Normalized Google Distance (NGD) as described in R.L.
 * Cilibrasi and P.M.B. Vitanyi, "The Google Similarity Distance", IEEE Trans.
 * Knowledge and Data Engineering, 19:3(2007), 370 - 383
 */

public class GoogleDistanceCalculator {

	private static final Logger LOGGER = Logger
			.getLogger(GoogleDistanceCalculator.class);

	/** A Google URL that will return the number of matches, among other things. */
	private static final String GOOGLE_SEARCH_SITE_PREFIX = "http://ajax.googleapis.com/ajax/services/search/web?v=1.0&"
	/* + "key=AIzaSyCGqX6ZO5xBmfcim0rq6BYFV0DUikSdNyk&" */
	;

	/** A Yahoo URL that will return the number of matches, among other things. */
	private static final String YAHOO_SEARCH_SITE_PREFIX = "https://yboss.yahooapis.com/ysearch/web?q=";

	// + theQueryTerm?appid=YOUR_API_KEY&format=json"
	// see http://developer.yahoo.com/search/boss/

	/**
	 * The file in the eclipse install directory containing a textual rep. of
	 * the cache.
	 */
	protected static final String CACHE_FILE_NAME = "google.cache";

	static int counter = 0;

	/**
	 * The logarithm of a number that is (hopefully) greater than or equal to
	 * the (unpublished) indexed number of Google documents.
	 * http://googleblog.blogspot.com/2008/07/we-knew-web-was-big.html puts this
	 * at a trillion or more.
	 */
	protected final static double logN = Math.log(1.0e12);

	private static Map<String, Double> cache = new HashMap<String, Double>();

	/** Holds the new terms we entered (these are also in the cache) */
	private static Map<String, Double> newCache = new HashMap<String, Double>();

	/**
	 * The key to use for querying Yahoo. This is read in via the system
	 * property "yahooApiKey".
	 */
	private static String yahooApiKey = "TIAIXlPV34Fz97nSdICWUmrxsysM4YTj6wlomOc6ycZTKDbbLXU.dOtlhdvE5Mh0tZQ-";

	public GoogleDistanceCalculator() {
		try {
			cache = setupCache(CACHE_FILE_NAME);

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void clearCache() {
		cache = new HashMap<String, Double>();
		newCache = new HashMap<String, Double>();
		File cacheFile = new File(CACHE_FILE_NAME);
		cacheFile.delete();
	}

	protected Map<String, Double> setupCache(String filename)
			throws NumberFormatException, IOException {

		File cacheFile = new File(filename);

		if (cacheFile.canRead()) {
			BufferedReader reader = new BufferedReader(new FileReader(filename));

			String line;

			while ((line = reader.readLine()) != null) {
				try {

					int lastSpaceIndex = line.lastIndexOf(' ');
					String token = line.substring(0, lastSpaceIndex);
					double count = Double.parseDouble(line
							.substring(lastSpaceIndex + 1));
					cache.put(token, count);
				} catch (Exception e) {
					// TODO: handle exception
				}
			}

			reader.close();
		}
		return cache;
	}

	/**
	 * Adds the contents of newCache to the specified file
	 * 
	 * @param filename
	 */
	protected static void updateCache(String filename) {

		if (counter++ >= 0) {
			BufferedWriter writer = null;
			try {
				writer = new BufferedWriter(new FileWriter(filename, true));

				for (Map.Entry<String, Double> entry : newCache.entrySet()) {
					writer.append(entry.getKey() + " " + entry.getValue()
							+ "\n");
				}
				newCache = new HashMap<String, Double>();
				counter = 0;
			} catch (IOException e) {
				// Things will just take longer
			} finally {
				if (writer != null) {
					try {
						writer.close();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
		}
	}

	public static double numResultsFromWeb(String term) throws JSONException,
			IOException {
		double result = 0;

		if (cache.containsKey(term)) {
			result = cache.get(term);
		} else {
			// URL url = null;
			// InputStream stream = null;
			// try {
			// url = makeQueryURL(term);
			//
			// URLConnection connection = null;
			// if (Property.getInstance().get("proxy.enable").equals("true")) {
			// SocketAddress address = new InetSocketAddress(Property
			// .getInstance().get("proxy.host"),
			// Integer.parseInt(Property.getInstance().get(
			// "proxy.port")));
			// java.net.Proxy proxy = new java.net.Proxy(
			// java.net.Proxy.Type.HTTP, address);
			// connection = url.openConnection(proxy);
			// } else {
			// connection = url.openConnection();
			//
			// }
			// // connection.setConnectTimeout(2000);
			// stream = connection.getInputStream();
			// InputStreamReader inputReader = new InputStreamReader(stream);
			// BufferedReader bufferedReader = new BufferedReader(inputReader);
			// double count = getCountFromQuery(bufferedReader);
			// System.out.println(term + ":\t" + count + " hits");
			double count = Yandex.search(term);
			cache.put(term, count);
			newCache.put(term, count);
			updateCache(CACHE_FILE_NAME);
			result = count;
		}
		// finally {
		// if (stream != null) {
		// try {
		// stream.close();
		// } catch (IOException e) {
		// }
		// }
		// }
		// }
		return result;
	}

	private static double getCountFromQuery(BufferedReader reader)
			throws JSONException, IOException {

		double count = 0;
		try {
			count = getCountFromGoogleQuery(reader);
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return count;
	}

	private double getCountFromYahooQuery(BufferedReader reader)
			throws IOException, JSONException, ParseException {
		// String line;
		// StringBuilder builder = new StringBuilder();
		//
		// while ((line = reader.readLine()) != null) {
		// builder.append(line);
		// }
		// String response = builder.toString();
		// JSONObject json = new JSONObject(response);
		JSONParser parser = new JSONParser();
		JSONObject json = (JSONObject) parser.parse(reader);
		JSONObject cursor = (JSONObject) json.get("ysearchresponse");
		double count = Double.parseDouble((String) cursor.get("totalhits"));
		return count;
	}

	@SuppressWarnings("unused")
	private static double getCountFromGoogleQuery(BufferedReader bufferedReader)
			throws JSONException, IOException, ParseException {

		double count = 0;

		try {
			JSONParser parser = new JSONParser();
			JSONObject json = (JSONObject) parser.parse(bufferedReader);
			JSONObject responseData = (JSONObject) json.get("responseData");
			JSONObject cursor = (JSONObject) responseData.get("cursor");
			count = Double.parseDouble((String) cursor
					.get("estimatedResultCount"));
		} catch (Exception e) {
			LOGGER.error(e);
			count = 0;
		}
		return count;
	}

	protected static URL makeQueryURL(String term)
			throws MalformedURLException, IOException {
		// String searchTerm = term.replaceAll(" ", "+");
		String searchTerm = URLEncoder.encode(term, "UTF-8");
		URL url;
		// String urlStringY = makeYahooQueryString(searchTerm);
		String urlString = makeGoogleQueryString(searchTerm);
		url = new URL(urlString);
		return url;
	}

	/**
	 * Builds a query string suitable for Google
	 * 
	 * @param searchTerm
	 * @return
	 */
	@SuppressWarnings("unused")
	private static String makeGoogleQueryString(String searchTerm) {
		String urlString = GOOGLE_SEARCH_SITE_PREFIX + "q=" + searchTerm + " ";
		/*
		 * Example queries: cassell: q=cassell keith cassell: q=keith+cassell
		 * "keith cassell": q=%22keith+cassell%22 "keith cassell" betweenness:
		 * q=%22keith+cassell%22+betweenness
		 */
		return urlString;
	}

	/**
	 * Builds a query string suitable for Yahoo
	 * 
	 * @param searchTerm
	 * @return
	 */
	private static String makeYahooQueryString(String searchTerm) {
		String urlString = YAHOO_SEARCH_SITE_PREFIX + searchTerm + "&appid="
				+ yahooApiKey + "&count=0&format=json";
		// System.out.println(urlString);
		return urlString;
	}

	public static Double calculateDistance(double min, double max, double both) {
		double distance = 0;

		try {

			// if necessary, swap the min and max
			if (max < min) {
				double temp = max;
				max = min;
				min = temp;
			}

			if (min > 0.0 && both > 0.0) {
				distance = (Math.log(max) - Math.log(both))
						/ (logN - Math.log(min));
			} else {
				distance = 1.0;
			}

			// Counts change and are estimated, so there would be a possibility
			// of a slightly negative distance.
			if (distance < 0.0) {
				distance = 0.0;
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return distance;
	}

	/**
	 * Calculates the normalized Google Distance (NGD) between the two terms
	 * specified. NOTE: this number can change between runs, because it is based
	 * on the number of web pages found by Google, which changes.
	 * 
	 * @return a number from 0 (minimally distant) to 1 (maximally distant),
	 *         unless an exception occurs in which case, it is negative
	 *         (RefactoringConstants.UNKNOWN_DISTANCE)
	 */
	public static Double calculateSimilarity(String term1, String term2) {
		double distance = 0;

		try {
			double min = numResultsFromWeb(term1);
			double max = numResultsFromWeb(term2);
			double both = numResultsFromWeb(term1 + " & " + term2);

			// if necessary, swap the min and max
			if (max < min) {
				double temp = max;
				max = min;
				min = temp;
			}

			if (min > 0.0 && both > 0.0) {
				distance = (Math.log(max) - Math.log(both))
						/ (logN - Math.log(min));
			} else {
				distance = 1.0;
			}

			// Counts change and are estimated, so there would be a possibility
			// of a slightly negative distance.
			if (distance < 0.0) {
				distance = 0.0;
			} else if (distance > 1.0) {
				distance = 1.0;
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return 1 - distance;
	}

	public DistanceCalculatorEnum getType() {
		return DistanceCalculatorEnum.GoogleDistance;
	}

	public static void main(String[] args) throws NumberFormatException,
			IOException, JSONException {
		String searchTerm1 = "kanunda";
		String searchTerm2 = "hukuk";
		String searchTerm3 = "müzik";
		GoogleDistanceCalculator gd = new GoogleDistanceCalculator();
		// double distance2 = gd.calculateDistance(
		// gd.numResultsFromWeb(searchTerm1),
		// gd.numResultsFromWeb(searchTerm2),
		// gd.numResultsFromWeb(searchTerm1 + " & " + searchTerm2));
		Double distance = gd.calculateSimilarity(searchTerm1, searchTerm2);
		Double distance2 = gd.calculateSimilarity(searchTerm1, searchTerm3);
		LOGGER.info(distance);
		LOGGER.info(distance2);
		// LOGGER.info(distance2);
	}
}
