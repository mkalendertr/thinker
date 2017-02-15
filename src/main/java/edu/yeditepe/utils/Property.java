package edu.yeditepe.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class Property {
	private static Property instance = new Property();
	private Properties prop = new Properties();

	public static Property getInstance() {
		return instance;
	}

	private Property() {
		try {

			// InputStream inputStream = this.getClass().getClassLoader()
			// .getResourceAsStream("application.properties");
			InputStream inputStream = new FileInputStream(new File(
					"application.properties"));

			prop.load(inputStream);

		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	public String get(String key) {
		return prop.getProperty(key);
	}

	public int getInt(String key) {
		return Integer.parseInt(prop.getProperty(key));
	}

}
