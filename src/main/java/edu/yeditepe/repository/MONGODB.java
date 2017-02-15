package edu.yeditepe.repository;

import java.net.UnknownHostException;

import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.Mongo;

public class MONGODB {
	private static MONGODB instance = new MONGODB();
	private static DB db;

	private MONGODB() {
		Mongo mongo;
		try {
			mongo = new Mongo("localhost", 27017);
			db = (mongo.getDB("videolization"));
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public static DBCollection getCollection(String name) {
		return db.getCollection(name);
	}

	public static MONGODB getInstance() {
		return instance;
	}

	public static DB getDb() {
		return db;
	}

}
