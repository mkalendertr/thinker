package edu.yeditepe.utils;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.zip.GZIPInputStream;

import edu.yeditepe.repository.MYSQL;

public class Wikidata {
	public static void main(String[] args) {
		try {
			BufferedReader in = new BufferedReader(new InputStreamReader(
					new GZIPInputStream(new FileInputStream(
							"C:\\wikidata\\wikidata-terms.nt.gz"))));

			String strLine;
			StringBuffer sb = new StringBuffer(
					"INSERT INTO videolization.wikidata_labels VALUES ");
			long counter = 1;
			while ((strLine = in.readLine()) != null) {
				// Print the content on the console
				try {
					// System.out.println(strLine);
					if (strLine.contains("rdf-schema#label")
							&& strLine.contains("@tr")) {
						String[] triple = strLine.split(" ");
						triple[0] = triple[0].substring(32,
								triple[0].length() - 1);
						triple[2] = triple[2].substring(1,
								triple[2].length() - 4).replace("\"", "");
						if (triple[0].startsWith("Q")) {
							sb.append("(\"" + triple[0] + "\",\"" + triple[2]
									+ "\"),");
							counter++;
						}

					}
				} catch (Exception e) {
					// TODO: handle exception
				}
				if (counter % 500 == 0) {
					MYSQL.getInstance().insertWikidataTypes(
							sb.substring(0, sb.length() - 1));
					System.out.println(counter);
					sb = new StringBuffer(
							"INSERT INTO videolization.wikidata_labels VALUES ");
					counter = 1;

				}

			}
			MYSQL.getInstance().insertWikidataTypes(
					sb.substring(0, sb.length() - 1));
			sb = new StringBuffer(
					"INSERT INTO videolization.wikidata_labels VALUES ");
			System.out.println(counter);
			// Close the input stream
			in.close();
		} catch (Exception e) {
			// TODO: handle exception
		}
	}
}
