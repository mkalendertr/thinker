package edu.yeditepe.utils;

import org.json.simple.parser.JSONParser;

import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;

public class PI3 {
	public static void main(String[] args) {
		search("Barack Obama");
	}

	public static long search(String query) {
		try {
			query = query.replace("_", " ");
			HttpTransport httpTransport = new NetHttpTransport();
			HttpRequestFactory requestFactory = httpTransport
					.createRequestFactory();
			JSONParser parser = new JSONParser();
			GenericUrl url = new GenericUrl(
					"http://12.133.183.155:8081/PI-3/api/restful/search/haberturk/"
							+ "\"" + query + "\"");
			url.put("format", "json");

			HttpRequest request = requestFactory.buildGetRequest(url);
			HttpResponse httpResponse = request.execute();
			String s = httpResponse.parseAsString();
			String temp[] = s.substring(s.indexOf("value"),
					s.indexOf(" haberde")).split(" ");
			// if (temp.length == 1) {
			// s = temp[0].substring(temp[0].lastIndexOf("\"") + 1).replace(
			// ".", "");
			// } else if (temp.length == 2) {
			// s = temp[1].replace(".", "");
			// }
			return Long.parseLong(temp[temp.length - 1]);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return 0;
	}
}
