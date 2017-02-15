package edu.yeditepe.experiment;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import edu.yeditepe.repository.MYSQL;

public class ExperimentITU {
	public static void main(String[] args) {
		readDataset();
	}

	public static TreeMap<String, List<String>> readDataset() {
		TreeMap<String, List<String>> examples = new TreeMap<String, List<String>>();
		BufferedReader in;
		try {

			File file = new File("experiment_itu\\isim çekimli");

			// Reading directory contents
			File[] files = file.listFiles();
			for (File file2 : files) {
				FileInputStream fr = new FileInputStream(file2);
				InputStreamReader isr = new InputStreamReader(fr,
						Charset.forName("Windows-1254"));
				BufferedReader read = new BufferedReader(isr);
				String text = "";

				try {
					while (read.ready()) {
						text += read.readLine();
					}
					read.close();
				} catch (Exception e) {
					e.printStackTrace();
				}

				// artik metnin icerigi temp'de
				String regexForWord2 = "(?<=<WORD>).*(?=</WORD>)"; // "(?<=>).*(?=</HEAD>)";
				String word2 = regexFunc(regexForWord2, text);
				System.out.println(word2);

				String regexForContext = "(?<=<TEXT>)(.*?)(?=</TEXT>)";
				String[] context = regexFuncForContext(regexForContext, text);
				// her bir <text>'i oku
				String[] meaningNo = new String[context.length];
				String[] word = new String[context.length];
				for (int i = 0; i < context.length; i++) {
					if (context[i] != null) {
						String regexForMeaning = "(?<=SENSE_TDK_NO=\")(\\d+)*(?=\")";
						meaningNo[i] = regexFunc(regexForMeaning, context[i]);
						// SENSE_TDK_NO meaningNo'da
						// System.out.println(meaningNo[i]);

						String regexForWord = "<WORD>.*</WORD>"; // "(?<=>).*(?=</HEAD>)";
						word[i] = word2;// regexFunc(regexForWord, context[i]);
						// System.out.println(word[i] + " " + meaningNo[i]);
						String ido = MYSQL.getTDKId(word[i],
								Integer.parseInt(meaningNo[i]));
						if (ido != null) {
							if (examples.containsKey(ido)) {
								examples.get(ido).add(context[i]);
							} else {
								List<String> l = new ArrayList<String>();
								l.add(context[i]);
								examples.put(ido, l);
								// break;
							}

						}
					}
				}
			}

		} catch (Exception e) {
			// TODO: handle exception
		}
		Writer writer;
		try {
			writer = new FileWriter("itu_dataset.json");
			Gson gson = new GsonBuilder().create();
			gson.toJson(examples, writer);
			writer.close();
		} catch (IOException e2) {
			// TODO Auto-generated catch block
			e2.printStackTrace();
		}
		return examples;

	}

	// regex ifadeleri isleyen fonksiyon
	public static String regexFunc(String regex, String content) {
		String temp = null;
		// String regex = "(?<=</WORD>).*(?=</WORD>)";
		Pattern pattern = Pattern.compile(regex);
		String targetString = content;
		Matcher matcher = pattern.matcher(targetString);
		while (matcher.find()) {
			temp = matcher.group();
		}
		return temp;
	}

	public static String[] regexFuncForContext(String regex, String content) {
		String[] temp = new String[100];
		for (int i = 0; i < 100; i++) {
			temp[i] = null;
		}
		// String regex = "(?<=</WORD>).*(?=</WORD>)";
		Pattern pattern = Pattern.compile(regex);
		String targetString = content;
		Matcher matcher = pattern.matcher(targetString);
		int i = 0;
		while (matcher.find()) {
			temp[i] = matcher.group();
			i++;
		}
		return temp;
	}
}
