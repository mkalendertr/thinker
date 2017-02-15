package edu.yeditepe.utils;

import java.util.Set;

public class BlackListEntities {
	private static BlackListEntities instance = new BlackListEntities();
	private static Set<String> pages;

	public static BlackListEntities getInstance() {
		return instance;
	}

	public static void setInstance(BlackListEntities instance) {
		BlackListEntities.instance = instance;
	}

	private BlackListEntities() {
		pages = FileUtils.readFileSet("popular_pages.txt");
	}

	public boolean isBlackListEntity(String id) {
		return pages.contains(id);
	}
}
