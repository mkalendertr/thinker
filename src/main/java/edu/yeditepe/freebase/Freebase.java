package edu.yeditepe.freebase;

import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.jayway.jsonpath.JsonPath;

import edu.yeditepe.repository.MYSQL;
import edu.yeditepe.utils.Property;
import edu.yeditepe.wiki.Wikipedia;

public class Freebase {
	private static final Logger LOGGER = Logger.getLogger(Freebase.class);

	public static void main(String[] args) throws InterruptedException {
		List<String> pages = MYSQL.getEnTitles();
		Multiset<String> domainMultiset = HashMultiset.create();
		Map<String, String> domains = new HashMap<String, String>();
		for (String title : pages) {
			String domain = getFreebaseDomain(title, "en");
			domains.put(title, domain);
			domainMultiset.add(domain);
			LOGGER.info(title + ":" + domain);
			Thread.sleep(100);

		}

		Writer writer;
		try {
			writer = new FileWriter("freebase_types.json");
			Gson gson = new GsonBuilder().create();
			gson.toJson(domains, writer);

			writer.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		for (String string : domainMultiset) {
			System.out
					.print("," + string + "\t" + domainMultiset.count(string));
		}
	}

	public static String getFreebaseDomain(String urlTitle, String language) {
		Object result = search(urlTitle, "notable", language);
		try {
			String type = JsonPath
					.read(result,
							"$.output.notable./common/topic/notable_types[0].id")
					.toString().split("/")[2];

			return type;

		} catch (Exception e) {

		}
		try {
			return JsonPath.read(result, "$.notable.id").toString();

		} catch (Exception e) {

		}
		return getTypefromTitle(urlTitle);
	}

	public static String getTypefromTitle(String title) {
		try {
			return title.split("[\\(\\)]")[1];
		} catch (Exception e) {
			// TODO: handle exception
		}
		return null;
	}

	public static Object search(String urlTitle, String property,
			String language) {
		try {

			String title = Wikipedia.wikiUrlToString(urlTitle);
			// title = getUnicode(title);

			HttpTransport httpTransport = new NetHttpTransport();
			HttpRequestFactory requestFactory = httpTransport
					.createRequestFactory();
			JSONParser parser = new JSONParser();
			GenericUrl url = new GenericUrl(
					"https://www.googleapis.com/freebase/v1/search");
			url.put("query", "\"" + urlTitle + "\"");
			url.put("lang", language);
			url.put("limit", "5");
			url.put("indent", "true");
			url.put("exact", "false");
			url.put("prefixed", "false");
			url.put("scoring", "entity");
			if (property != null) {
				url.put("output", "(" + property + ")");
			} else {
				url.put("output", "(all)");
			}
			// url.put("filter",
			// "(all /common/topic/topic_equivalent_webpage:http://en.wikipedia.org/wiki/"
			// + title + ")");
			// url.put("filter", "(all name{full}:" + getUnicode(title) + ")");

			url.put("key", Property.getInstance().get("google.key"));
			HttpRequest request = requestFactory.buildGetRequest(url);
			HttpResponse httpResponse = request.execute();
			JSONObject response = (JSONObject) parser.parse(httpResponse
					.parseAsString());
			JSONArray results = (JSONArray) response.get("result");
			for (Object result : results) {
				String name = JsonPath.read(result, "$.name").toString();
				if (StringUtils.startsWithIgnoreCase(name, title)) {
					return result;
				}

			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return null;
	}

	public static StringBuffer removeUTFCharacters(String data) {
		Pattern p = Pattern.compile("\\$(\\p{XDigit}{4})");
		Matcher m = p.matcher(data);
		StringBuffer buf = new StringBuffer(data.length());
		while (m.find()) {
			String ch = String.valueOf((char) Integer.parseInt(m.group(1), 16));
			m.appendReplacement(buf, Matcher.quoteReplacement(ch));
		}
		m.appendTail(buf);
		return buf;
	}

	public static String getUnicode(String str_code) {
		String result = "";
		for (int i = 0; i < str_code.length(); i++) {
			String c = str_code.substring(i, i + 1);
			if (!isAlphaNumericRegEx(c)) {

				String hexCode = Integer.toHexString(c.codePointAt(0))
						.toUpperCase();
				String hexCodeWithAllLeadingZeros = "0000" + hexCode;
				String hexCodeWithLeadingZeros = hexCodeWithAllLeadingZeros
						.substring(hexCodeWithAllLeadingZeros.length() - 4);
				hexCodeWithLeadingZeros = "$" + hexCodeWithLeadingZeros;
				result += hexCodeWithLeadingZeros;
			} else {
				result += c;
			}
		}
		return result;
	}

	private static boolean isAlphaNumericRegEx(String s) {
		if (s.equals("(") || s.equals(")")) {
			return false;
		}
		return true;
		// return Pattern.matches("[\\dA-Za-z]+", s);
	}

	public String getFreebaseDomain(String title) {
		// TODO Auto-generated method stub
		return null;
	}

}