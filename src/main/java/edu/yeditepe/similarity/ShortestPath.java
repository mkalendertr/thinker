package edu.yeditepe.similarity;

import org.apache.log4j.Logger;
import org.json.simple.parser.JSONParser;

import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;

import edu.yeditepe.utils.Property;

public class ShortestPath {
	private static final Logger LOGGER = Logger.getLogger(ShortestPath.class);

	public static long getPathDistance(int page1Id, int page2Id) {
		try {
			// LOGGER.info("Request for : " + page1Id + " " + page2Id);
			HttpTransport httpTransport = new NetHttpTransport();
			HttpRequestFactory requestFactory = httpTransport
					.createRequestFactory();
			JSONParser parser = new JSONParser();
			GenericUrl url = new GenericUrl(Property.getInstance().get(
					"neo4j.shortestpath"));
			url.put("page1", page1Id);
			url.put("page2", page2Id);

			HttpRequest request = requestFactory.buildGetRequest(url);
			HttpResponse httpResponse = request.execute();
			Long distance = (Long) parser.parse(httpResponse.parseAsString());
			// LOGGER.info("distance: " + distance);
			return distance;
		} catch (Exception e) {
			LOGGER.error(e);
		}

		return 0;

	}

	// public static Map<String, Long> getPathDistance(List<String> ids) {
	// try {
	//
	// HttpTransport httpTransport = new NetHttpTransport();
	// HttpRequestFactory requestFactory = httpTransport
	// .createRequestFactory();
	// JSONParser parser = new JSONParser();
	// GenericUrl url = new GenericUrl(Property.getInstance().get(
	// "neo4j.shortestpath"));
	// url.put("pageIds", ids);
	//
	// HttpRequest request = requestFactory.buildGetRequest(url);
	// HttpResponse httpResponse = request.execute();
	// return (Map<String, Long>) parser.parse(httpResponse
	// .parseAsString());
	// } catch (Exception e) {
	// LOGGER.error(e);
	// }
	//
	// return null;
	//
	// }

	public static void main(String[] args) {
		LOGGER.info(getPathDistance(10, 2245));
	}
}
