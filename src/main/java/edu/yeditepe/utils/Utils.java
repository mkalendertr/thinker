package edu.yeditepe.utils;

import org.apache.commons.lang3.StringUtils;

public class Utils {
	public static int getLetterCase(String title) {
		if (StringUtils.isAllLowerCase(title)) {
			return 0;
		} else if (StringUtils.isAllUpperCase(title)) {
			return 1;
		} else {
			return 2;
		}
	}
}
