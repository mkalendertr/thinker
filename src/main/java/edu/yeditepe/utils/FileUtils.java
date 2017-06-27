package edu.yeditepe.utils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

public class FileUtils {

	public static List<String> readFile(String filename) {
		try {
			List<String> lines = new ArrayList<String>();
			FileInputStream fstream = new FileInputStream(filename);
			BufferedReader br = new BufferedReader(new InputStreamReader(
					fstream));

			String strLine;
			while ((strLine = br.readLine()) != null) {
				// // Print the content on the console
				// System.out.println(strLine);
				lines.add(strLine);
			}

			// Close the input stream
			br.close();
			return lines;
		} catch (Exception e) {
			// TODO: handle exception
		}
		return null;
	}

	public static String readFileString(String filename) {
		String text = "";

		try {
			FileInputStream fstream = new FileInputStream(filename);
			BufferedReader br = new BufferedReader(new InputStreamReader(
					fstream));
			String strLine;
			while ((strLine = br.readLine()) != null) {
				// // Print the content on the console
				// System.out.println(strLine);
				text += strLine + "\n";
			}

			// Close the input stream
			br.close();
			return text;
		} catch (Exception e) {
			// TODO: handle exception
		}
		return text;
	}

	public static Set<String> readFileSet(String filename) {
		try {
			Set<String> lines = new TreeSet<String>();
			FileInputStream fstream = new FileInputStream(filename);
			BufferedReader br = new BufferedReader(new InputStreamReader(
					fstream));

			String strLine;
			while ((strLine = br.readLine()) != null) {
				// // Print the content on the console
				// System.out.println(strLine);
				lines.add(strLine);
			}

			// Close the input stream
			br.close();
			return lines;
		} catch (Exception e) {
			// TODO: handle exception
		}
		return null;
	}

	public synchronized static void writeFile(String content, String fileName) {
		try {

			File file = new File(fileName);

			// if file doesnt exists, then create it
			if (!file.exists()) {
				file.createNewFile();
			}

			FileWriter fw = new FileWriter(file.getAbsoluteFile(), true);
			BufferedWriter bw = new BufferedWriter(fw);
			bw.write(content);
			bw.close();

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void writeFileOverWrite(String content, String fileName) {
		try {

			File file = new File(fileName);

			// if file doesnt exists, then create it
			if (!file.exists()) {
				file.createNewFile();
			}

			FileWriter fw = new FileWriter(file.getAbsoluteFile(), false);
			BufferedWriter bw = new BufferedWriter(fw);
			bw.write(content);
			bw.close();

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void write(byte[] aInput, String aOutputFileName) {
		try {
			OutputStream output = null;
			try {
				output = new BufferedOutputStream(new FileOutputStream(
						aOutputFileName));
				output.write(aInput);
			} finally {
				output.close();
			}
		} catch (FileNotFoundException ex) {
		} catch (IOException ex) {
		}
	}

	public static byte[] read(String aInputFileName) {
		File file = new File(aInputFileName);
		byte[] result = new byte[(int) file.length()];
		try {
			InputStream input = null;
			try {
				int totalBytesRead = 0;
				input = new BufferedInputStream(new FileInputStream(file));
				while (totalBytesRead < result.length) {
					int bytesRemaining = result.length - totalBytesRead;
					// input.read() returns -1, 0, or more :
					int bytesRead = input.read(result, totalBytesRead,
							bytesRemaining);
					if (bytesRead > 0) {
						totalBytesRead = totalBytesRead + bytesRead;
					}
				}
				/*
				 * the above style is a bit tricky: it places bytes into the
				 * 'result' array; 'result' is an output parameter; the while
				 * loop usually has a single iteration only.
				 */
			} finally {
				input.close();
			}
		} catch (FileNotFoundException ex) {
		} catch (IOException ex) {
		}
		return result;
	}

	public static void main(String[] args) {
		byte[] bytes = read("vector" + File.separator + "968.bat");
		int a = 5;
		a++;
	}
}
