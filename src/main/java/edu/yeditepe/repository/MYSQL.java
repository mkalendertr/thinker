package edu.yeditepe.repository;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.math.BigDecimal;
import java.sql.Blob;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;

import edu.yeditepe.model.Entity;
import edu.yeditepe.model.EntityPage;
import edu.yeditepe.model.Link;
import edu.yeditepe.nlp.TurkishNLP;
import edu.yeditepe.nlp.Zemberek;
import edu.yeditepe.utils.Utils;
import edu.yeditepe.wiki.Wikipedia;

public class MYSQL {
	private static final Logger LOGGER = Logger.getLogger(MYSQL.class);

	private static String dbUrl = "jdbc:mysql://localhost/videolization?useUnicode=true&characterEncoding=UTF-8";

	private static String dbClass = "com.mysql.jdbc.Driver";

	private static String username = "root";

	private static String password = "123456";

	private static MYSQL instance = new MYSQL();

	public static MYSQL getInstance() {
		return instance;
	}

	public static void setInstance(MYSQL instance) {
		MYSQL.instance = instance;
	}

	private static Connection connection;

	private MYSQL() {
		try {
			Class.forName(dbClass);
			connection = DriverManager.getConnection(dbUrl, username, password);

		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static void main(String[] args) throws FileNotFoundException {
		// getPages();
		// getTDKTitles();
		// populateTDK();
		// populateWiki();
		updateTDKId();
	}

	public static String getIncomingLinks(int pageId) {
		String query = "Select pl_from from my_links where pl_to=" + pageId;
		StringBuffer sb = new StringBuffer();
		try {

			Class.forName(dbClass);
			Connection connection = DriverManager.getConnection(dbUrl,
					username, password);
			Statement statement = connection.createStatement();
			ResultSet resultSet = statement.executeQuery(query);
			while (resultSet.next()) {

				sb.append(resultSet.getInt(1) + " ");

			}
			connection.close();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (SQLException e) {
			e.printStackTrace();
		}

		return sb.toString().trim();

	}

	public static Set<Integer> getIncomingLinksList(int pageId) {
		String query = "Select pl_from from my_links where pl_to=" + pageId;
		Set<Integer> list = new HashSet<Integer>();
		try {

			Class.forName(dbClass);
			Connection connection = DriverManager.getConnection(dbUrl,
					username, password);
			Statement statement = connection.createStatement();
			ResultSet resultSet = statement.executeQuery(query);
			while (resultSet.next()) {

				list.add(resultSet.getInt(1));

			}
			connection.close();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (SQLException e) {
			e.printStackTrace();
		}

		return list;

	}

	public static TreeSet<String> getCategoryNames() {
		String query = "SELECT cl_to from (SELECT cl_to, count(cl_to) as num FROM videolization.categorylinks where cl_type!='file' group by cl_to) as c where num>=1;";
		TreeSet<String> list = new TreeSet<String>();
		try {

			Class.forName(dbClass);
			Connection connection = DriverManager.getConnection(dbUrl,
					username, password);
			Statement statement = connection.createStatement();
			ResultSet resultSet = statement.executeQuery(query);
			while (resultSet.next()) {

				list.add(TurkishNLP.toLowerCase(blobToString(resultSet
						.getBlob(1))));
			}
			connection.close();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (SQLException e) {
			e.printStackTrace();
		}

		return list;

	}

	public static List<String> getCategories() {
		String query = "SELECT page_title, cl_to FROM videolization.categorylinks join videolization.my_pages on my_pages.page_id=categorylinks.cl_from where cl_type!='file';";
		List<String> list = new ArrayList<String>();
		try {
			Class.forName(dbClass);
			Connection connection = DriverManager.getConnection(dbUrl,
					username, password);
			Statement statement = connection.createStatement();
			ResultSet resultSet = statement.executeQuery(query);
			while (resultSet.next()) {
				list.add(blobToString(resultSet.getBlob(1)) + "\t"
						+ blobToString(resultSet.getBlob(2)));
			}
			connection.close();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (SQLException e) {
			e.printStackTrace();
		}

		return list;

	}

	public static Set<Integer> getOutgoingLinksList(int pageId) {
		String query = "Select pl_to from my_links where pl_from=" + pageId;
		Set<Integer> list = new HashSet<Integer>();
		try {

			Class.forName(dbClass);
			Connection connection = DriverManager.getConnection(dbUrl,
					username, password);
			Statement statement = connection.createStatement();
			ResultSet resultSet = statement.executeQuery(query);
			while (resultSet.next()) {

				list.add(resultSet.getInt(1));

			}
			connection.close();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (SQLException e) {
			e.printStackTrace();
		}

		return list;

	}

	public static String getOutgoingLinks(int pageId) {
		String query = "Select pl_to from my_links where pl_from=" + pageId;
		StringBuffer sb = new StringBuffer();
		try {

			Class.forName(dbClass);
			Connection connection = DriverManager.getConnection(dbUrl,
					username, password);
			Statement statement = connection.createStatement();
			ResultSet resultSet = statement.executeQuery(query);
			while (resultSet.next()) {

				sb.append(resultSet.getInt(1) + " ");

			}
			connection.close();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (SQLException e) {
			e.printStackTrace();
		}

		return sb.toString().trim();

	}

	public static String getLinks(int pageId) {
		return getIncomingLinks(pageId) + " " + getOutgoingLinks(pageId);

	}

	public static Collection<EntityPage> getPages(Wikipedia wikipedia) {
		String query = "Select a.page_id, a.page_title, p.en_title, a.redirect_title, r.link_count as rank "
				+ "from my_alias as a "
				+ "left join my_pages as p on p.page_id = a.page_id "
				+ "left join my_wikirank as r on r.page_id = a.page_id";

		Map<String, EntityPage> map = new HashMap<String, EntityPage>();

		try {

			Class.forName(dbClass);
			Connection connection = DriverManager.getConnection(dbUrl,
					username, password);
			Statement statement = connection.createStatement();
			ResultSet resultSet = statement.executeQuery(query);
			while (resultSet.next()) {

				int id = resultSet.getInt(1);
				// if (id == 1671518) {
				// System.out.println();
				// }
				String idS = Integer.toString(id);
				String urlTitle = blobToString(resultSet.getBlob(2));
				String enTitle = blobToString(resultSet.getBlob(3));
				String alias = Wikipedia.wikiUrlToString(blobToString(resultSet
						.getBlob(4)));
				BigDecimal linkCount = resultSet.getBigDecimal(5);
				String title = Wikipedia.wikiUrlToString(urlTitle);
				if (map.containsKey(idS)) {
					EntityPage p = map.get(idS);
					String content = null;
					try {
						content = wikipedia.getPageContent().get(p.getId())
								.toString();
					} catch (Exception e) {
					}

					if (!alias.equalsIgnoreCase(p.getTitle())
							&& (content == null || StringUtils
									.containsIgnoreCase(content,
											Zemberek.getInstance()
													.morphPageContent(alias)))) {
						// double distance = GoogleDistanceCalculator
						// .calculateDistance(title, alias);
						// LOGGER.info("Title:" + title + " alias:" + alias
						// + " distance:" + 0);

						p.addAlias(alias);
					}

				} else {
					double rank = 0;
					if (linkCount != null) {
						rank = linkCount.doubleValue();
					}
					// double webCount = 0;
					// try {
					// webCount = GoogleDistanceCalculator
					// .numResultsFromWeb(title);
					// } catch (JSONException | IOException e) {
					// // TODO Auto-generated catch block
					// e.printStackTrace();
					// }
					EntityPage p = new EntityPage(String.valueOf(id), title,
							urlTitle, enTitle, alias, rank);
					map.put(idS, p);
				}
				// if (map.size() > 10) {
				// break;
				// }
			}
			connection.close();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (SQLException e) {
			e.printStackTrace();
		}

		return map.values();
	}

	public static List<Link> getLinks() {
		String query = "Select * from videolization.my_links";

		List<Link> list = new ArrayList<Link>();

		try {

			Class.forName(dbClass);
			Connection connection = DriverManager.getConnection(dbUrl,
					username, password);
			Statement statement = connection.createStatement();
			ResultSet resultSet = statement.executeQuery(query);
			while (resultSet.next()) {

				int from = resultSet.getInt(1);
				int to = resultSet.getInt(2);
				list.add(new Link(String.valueOf(from), String.valueOf(to)));
			}

			connection.close();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (SQLException e) {
			e.printStackTrace();
		}

		return list;
	}

	public static List<Integer> getIds() {
		String query = "Select page_id from videolization.my_pages";

		List<Integer> list = new ArrayList<Integer>();

		try {

			Class.forName(dbClass);
			Connection connection = DriverManager.getConnection(dbUrl,
					username, password);
			Statement statement = connection.createStatement();
			ResultSet resultSet = statement.executeQuery(query);
			while (resultSet.next()) {

				int id = resultSet.getInt(1);
				list.add(id);
			}

			connection.close();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (SQLException e) {
			e.printStackTrace();
		}

		return list;
	}

	public static String blobToString(Blob blob) {
		byte[] bdata;
		try {
			bdata = blob.getBytes(1, (int) blob.length());
			return new String(bdata, "utf-8");
		} catch (Exception e) {

		}
		return "";
	}

	public static List<String> getPageTitles() {
		String query = "Select page_title from videolization.my_pages";

		List<String> list = new ArrayList<String>();

		try {

			Class.forName(dbClass);
			Connection connection = DriverManager.getConnection(dbUrl,
					username, password);
			Statement statement = connection.createStatement();
			ResultSet resultSet = statement.executeQuery(query);
			while (resultSet.next()) {

				list.add(blobToString(resultSet.getBlob(1)));
			}

			connection.close();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (SQLException e) {
			e.printStackTrace();
		}

		return list;
	}

	public static Set<String> getAliases(int page_id) {
		String query = "Select redirect_title from videolization.my_alias where page_id="
				+ page_id;

		Set<String> set = new HashSet<String>();

		try {

			Class.forName(dbClass);
			Connection connection = DriverManager.getConnection(dbUrl,
					username, password);
			Statement statement = connection.createStatement();
			ResultSet resultSet = statement.executeQuery(query);
			while (resultSet.next()) {

				set.add(Wikipedia.wikiUrlToString((blobToString(resultSet
						.getBlob(1)))));
			}

			connection.close();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (SQLException e) {
			e.printStackTrace();
		}

		return set;
	}

	public static Set<String> getExperimentPages() {
		String query = "SELECT page_id FROM videolization.my_wikirank order by link_count desc limit 1000";
		// String query =
		// "SELECT page_id FROM videolization.my_wikirank limit 10000";

		Set<String> list = new HashSet<String>();

		try {

			Class.forName(dbClass);
			Connection connection = DriverManager.getConnection(dbUrl,
					username, password);
			Statement statement = connection.createStatement();
			ResultSet resultSet = statement.executeQuery(query);
			while (resultSet.next()) {

				list.add(String.valueOf(resultSet.getInt(1)));
			}

			connection.close();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (SQLException e) {
			e.printStackTrace();
		}

		return list;
	}

	public static List<String> getEnTitles() {
		String query = "Select en_title from videolization.my_pages as p join my_wikirank as r on r.page_id = p.page_id and r.link_count>=50 where en_title is not null";

		List<String> list = new ArrayList<String>();

		try {

			Class.forName(dbClass);
			Connection connection = DriverManager.getConnection(dbUrl,
					username, password);
			Statement statement = connection.createStatement();
			ResultSet resultSet = statement.executeQuery(query);
			while (resultSet.next()) {

				list.add(blobToString(resultSet.getBlob(1)).toLowerCase()
						.replace(" ", "_"));
			}

			connection.close();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (SQLException e) {
			e.printStackTrace();
		}

		return list;

	}

	public static int getIdFromRedirect(String string) {
		string = string.replace("'", "\\'");

		String query = "Select page_id from videolization.my_alias where redirect_title = '"
				+ string + "'";

		try {

			Statement statement = connection.createStatement();
			ResultSet resultSet = statement.executeQuery(query);
			while (resultSet.next()) {
				int id = resultSet.getInt(1);
				return id;
			}

		} catch (SQLException e) {
			e.printStackTrace();
		}

		return 0;
	}

	public static String getEnTitle(String string) {
		string = string.replace("'", "\\'");
		String query = "Select en_title from videolization.my_pages where page_title ='"
				+ string + "'";

		try {

			Statement statement = connection.createStatement();
			ResultSet resultSet = statement.executeQuery(query);
			while (resultSet.next()) {

				return blobToString(resultSet.getBlob(1)).toLowerCase()
						.replace(" ", "_");
			}

		} catch (SQLException e) {
			e.printStackTrace();
		}
		return null;

	}

	public static int getId(String title) {
		title = title.replace("'", "\\'");
		String query = "Select page_id from videolization.my_pages where page_title ='"
				+ title + "'";

		try {

			Statement statement = connection.createStatement();
			ResultSet resultSet = statement.executeQuery(query);
			while (resultSet.next()) {
				int id = resultSet.getInt(1);
				return id;
			}

		} catch (SQLException e) {
			e.printStackTrace();
		}
		return 0;

	}

	public static String getTRTitleById(String id) {
		id = id.replace("'", "\\'").replaceAll("_", " ");
		String query = "Select  page_title from videolization.my_pages where page_id ='"
				+ id + "'";

		try {

			Statement statement = connection.createStatement();
			ResultSet resultSet = statement.executeQuery(query);
			while (resultSet.next()) {

				return blobToString(resultSet.getBlob(1));
			}

		} catch (SQLException e) {
			e.printStackTrace();
		}
		return null;
	}

	public static String getTRTitleByEnName(String enname) {
		enname = enname.replace("'", "\\'").replaceAll("_", " ");
		String query = "Select  page_title from videolization.my_pages where en_title ='"
				+ enname + "'";

		try {

			Statement statement = connection.createStatement();
			ResultSet resultSet = statement.executeQuery(query);
			while (resultSet.next()) {

				return blobToString(resultSet.getBlob(1));
			}

		} catch (SQLException e) {
			e.printStackTrace();
		}
		return null;
	}

	public static HashMap<String, String> getTRTitles() {
		String query = "Select  page_title, en_title from videolization.my_pages";

		HashMap<String, String> map = new HashMap<String, String>();
		try {
			Statement statement = connection.createStatement();
			ResultSet resultSet = statement.executeQuery(query);
			while (resultSet.next()) {

				map.put(blobToString(resultSet.getBlob(2)).replaceAll(" ", "_"),
						blobToString(resultSet.getBlob(1)));
			}

		} catch (SQLException e) {
			e.printStackTrace();
		}
		return map;
	}

	public static HashMap<String, String> getTitles() {
		String query = "Select  page_id, page_title from videolization.my_pages";

		HashMap<String, String> map = new HashMap<String, String>();
		try {
			Statement statement = connection.createStatement();
			ResultSet resultSet = statement.executeQuery(query);
			while (resultSet.next()) {

				map.put(String.valueOf(resultSet.getInt(1)),
						blobToString(resultSet.getBlob(2)));
			}

		} catch (SQLException e) {
			e.printStackTrace();
		}
		return map;
	}

	public static HashSet<String> getTDKTitles() {
		HashSet<String> tdk = new HashSet<String>();
		HashSet<String> isimler = new HashSet<String>();
		try {
			String query = "SELECT maddebas FROM sozluk.maddeler where ozel=0";
			Statement statement = connection.createStatement();
			ResultSet resultSet = statement.executeQuery(query);
			while (resultSet.next()) {

				tdk.add(String.valueOf(resultSet.getString(1)));
			}

		} catch (SQLException e) {
			e.printStackTrace();
		}

		try {
			String query = "SELECT isimler FROM sozluk.isimler";
			Statement statement = connection.createStatement();
			ResultSet resultSet = statement.executeQuery(query);
			while (resultSet.next()) {

				isimler.add(TurkishNLP.toLowerCase(String.valueOf(resultSet
						.getString(1))));
			}

		} catch (SQLException e) {
			e.printStackTrace();
		}

		Writer writer;
		try {
			writer = new FileWriter("tdk.json");
			Gson gson = new GsonBuilder().create();
			gson.toJson(tdk, writer);

			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

		try {
			writer = new FileWriter("isimler.json");
			Gson gson = new GsonBuilder().create();
			gson.toJson(isimler, writer);

			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return tdk;
	}

	public void insertWikidataTypes(String query) {

		try {
			// Class.forName(dbClass);
			// Connection connection = DriverManager.getConnection(dbUrl,
			// username, password);
			Statement statement = connection.createStatement();
			statement.executeUpdate(query);
			// connection.close();

		} catch (Exception e) {
			e.printStackTrace();
			// System.out.println(query);
		}

	}

	public static String getTDKId(String description) {

		try {
			if (description != null) {

				description = description.replace("'", "\\'");
				String query = "Select id, rank2 from videolization.tdk where description ='"
						+ description + "'";

				Statement statement = connection.createStatement();
				ResultSet resultSet = statement.executeQuery(query);
				while (resultSet.next()) {
					int id = resultSet.getInt(1);
					int rank = resultSet.getInt(2);
					return String.valueOf(id) + String.valueOf(rank);
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return null;

	}

	public static String getTDKId(String title, int rank) {

		try {
			if (title != null) {

				String query = "Select id from videolization.tdk where title ='"
						+ title + "' and rank2=" + rank;

				Statement statement = connection.createStatement();
				ResultSet resultSet = statement.executeQuery(query);
				while (resultSet.next()) {
					int id = resultSet.getInt(1);
					return "t" + String.valueOf(id) + String.valueOf(rank);
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return null;

	}

	public static void updateTDKId() {
		DBCollection entitiesDB = MONGODB.getCollection(Entity.COLLECTION_NAME);
		DBCursor cursor = entitiesDB.find();
		while (cursor.hasNext()) {
			try {
				DBObject object = cursor.next();
				String ido = (String) object.get("id");
				double rank = (Double) object.get("rank");
				if (ido.startsWith("t")) {
					String desc = (String) object.get("description");
					String id = getTDKId(desc);
					if (id == null) {

						int r = (int) Math.round(1 / rank);
						ido += r;
						object.put("id", ido);
					} else {
						object.put("id", "t" + id);

					}
					entitiesDB.save(object);
				}
			} catch (Exception e) {
				// TODO: handle exception
			}

		}
	}

	public static void populateTDK() {
		String query = "Select * from tdk";
		StringBuffer sb = new StringBuffer();
		try {
			DBCollection entitiesDB = MONGODB
					.getCollection(Entity.COLLECTION_NAME);
			Class.forName(dbClass);
			Connection connection = DriverManager.getConnection(dbUrl,
					username, password);
			Statement statement = connection.createStatement();
			ResultSet resultSet = statement.executeQuery(query);
			while (resultSet.next()) {
				int id = resultSet.getInt("id");
				int rank1 = resultSet.getInt("rank1");
				int rank2 = resultSet.getInt("rank2");
				double rank = ((double) 1 / (1 + rank1) + (double) 1 / (rank2)) / 2;
				String title = resultSet.getNString("title");
				String description = resultSet.getNString("description");
				String example = resultSet.getNString("example");

				Entity e = new Entity();
				e.setId("t" + id + rank2);
				e.setSource("tdk");
				e.setTitle(title);
				e.setRank(rank);
				e.setDescription(description);
				e.setLetterCase(Utils.getLetterCase(title));
				if (example != null) {
					e.setSentences(new ArrayList<String>());
					e.getSentences().add(example);
				}
				e.setAlias(new HashSet<String>());
				e.getAlias().add(title);
				try {
					e.setSuffixes(Zemberek.getInstance().getSuffix(example,
							title));
				} catch (Exception e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				entitiesDB.save(e);
			}
			connection.close();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (SQLException e) {
			e.printStackTrace();
		}

	}

	public static void populateWiki() throws FileNotFoundException {
		Reader reader = new FileReader("domain.json");
		Gson gson = new GsonBuilder().create();
		Map<String, String> domains = gson.fromJson(reader, Map.class);

		reader = new FileReader("type.json");
		Map<String, String> types = gson.fromJson(reader, Map.class);

		String query = "Select distinct p.page_id, p.page_title, p.en_title, c.content, r.link_count as rank "
				+ "from my_pages as p "
				+ "left join my_content as c on p.page_id = c.page_id "
				+ "left join my_wikirank as r on r.page_id = p.page_id";
		try {
			DBCollection entitiesDB = MONGODB
					.getCollection(Entity.COLLECTION_NAME);

			Class.forName(dbClass);
			Connection connection = DriverManager.getConnection(dbUrl,
					username, password);
			Statement statement = connection.createStatement();
			ResultSet resultSet = statement.executeQuery(query);
			while (resultSet.next()) {
				int id = resultSet.getInt(1);
				String urlTitle = blobToString(resultSet.getBlob(2));
				if (urlTitle.contains("_(anlam_ayrımı)")) {
					continue;
				}
				String enTitle = blobToString(resultSet.getBlob(3));
				String content = resultSet.getString(4);
				if (content == null) {
					continue;
				}
				BigDecimal linkCount = resultSet.getBigDecimal(5);
				String title = Wikipedia.wikiUrlToString(urlTitle);
				Set<Integer> links = getOutgoingLinksList(id);
				Set<String> linksString = new HashSet<String>();
				for (Integer integer : links) {
					linksString.add("w" + integer);
				}
				linksString.add("w" + id);

				double rank = 0;
				if (linkCount != null) {
					rank = linkCount.doubleValue();
				}

				Set<String> aliases = getAliases(id);
				aliases.add(title);

				String type = types.get(String.valueOf(id));
				String domain = domains.get(enTitle);

				if (type != null && type.length() <= 1) {
					type = null;
				}
				if (domain != null && domain.length() <= 1) {
					domain = null;
				}
				List<String> sentences = getExampleSentence(title, content);
				if (sentences == null || sentences.isEmpty()) {
					continue;
				}

				Entity e = new Entity();
				e.setAlias(aliases);
				e.setId("w" + id);
				e.setSource("vikipedi");
				e.setTitle(title);
				e.setRank(rank);
				e.setDomain(domain);
				e.setType(type);
				e.setLinks(linksString);
				e.setDescription(sentences.get(0));

				if (content.contains(title.toLowerCase())) {
					e.setLetterCase(0);
				} else if (content.contains(title.toUpperCase())) {
					e.setLetterCase(1);
				} else {
					e.setLetterCase(2);
				}
				e.setSentences(sentences);
				Set<String> suffixes = new HashSet<String>();
				for (String sentence : sentences) {
					try {
						suffixes.addAll(Zemberek.getInstance().getSuffix(
								sentence, title));
					} catch (Exception e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
				}
				e.setSuffixes(suffixes);
				entitiesDB.save(e);

			}
			connection.close();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (SQLException e) {
			e.printStackTrace();
		}

	}

	public static void insertWikiContent(int id, String content) {

		try {
			// Class.forName(dbClass);
			// Connection connection = DriverManager.getConnection(dbUrl,
			// username, password);
			Statement statement = connection.createStatement();
			String query = "insert into videolization.my_content values (" + id
					+ ",'" + StringEscapeUtils.escapeSql(content) + "')";
			statement.executeUpdate(query);

			// connection.close();

		} catch (Exception e) {
			e.printStackTrace();
			// System.out.println(query);
		}

	}

	private static List<String> getExampleSentence(String title, String content) {
		try {
			content = content.replaceAll("\n", " ");
			String re = "\\([^()]*\\)";
			Pattern p = Pattern.compile(re);
			Matcher m = p.matcher(content);
			while (m.find()) {
				content = m.replaceAll(" ");
				m = p.matcher(content);
			}

			String re2 = "\\{\\{[A-Za-z+\\s-]+\\}\\}";
			Pattern p2 = Pattern.compile(re2);
			Matcher m2 = p2.matcher(content);
			while (m2.find()) {
				content = m2.replaceAll(" ");
				m2 = p2.matcher(content);
			}
			content = content.replaceAll("\\.", ". ");
			content = content.replaceAll("\\s+", " ").trim();

			content = content.substring(content.toLowerCase().indexOf(
					title.split(" ")[0].toLowerCase()));

			List<String> sentences = new ArrayList<String>();
			List<String> sentencesAll = Zemberek.getInstance().splitSentences(
					content);
			// String[] lines = content.split("\n");
			for (String line : sentencesAll) {
				if (StringUtils.containsIgnoreCase(line, title)
				// && line.contains(".")
						&& StringUtils.countMatches(line, " ") > 4) {
					sentences.add(line);
					if (sentences.size() >= 10) {
						return sentences;
					}
				}
			}
			return sentences;
		} catch (Exception e) {
			// TODO: handle exception
		}
		return null;
	}
}
