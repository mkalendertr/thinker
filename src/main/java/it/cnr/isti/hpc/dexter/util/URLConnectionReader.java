package it.cnr.isti.hpc.dexter.util;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;

public class URLConnectionReader {
    public static String getContent(String address) throws Exception {
	URL url = new URL(address);
	URLConnection c = url.openConnection();
	BufferedReader in = new BufferedReader(new InputStreamReader(
		c.getInputStream()));
	StringBuffer sb = new StringBuffer();
	String inputLine;

	while ((inputLine = in.readLine()) != null) {
	    sb.append(inputLine);
	}
	in.close();
	return sb.toString();
    }
}
