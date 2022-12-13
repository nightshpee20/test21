package main;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import database.Image;
import database.ImageMapper;
import database.Item;
import database.ItemMapper;

public class Main {
	private static final Pattern COLLECTIONS_PATTERN = Pattern.compile("https://onepiece\\.store/collections/");
	private static final Pattern SHOP_PATTERN = Pattern.compile("https://onepiece\\.store/shop/");
	
	private static SqlSession sqlSession;
	private static HttpClient client;
	private static File targetDirectory;
	private static String entryUrl;
	
	private static Logger logger;
	
	public static void main(String[] args) {
		logger = LogManager.getLogger(Main.class);
		
		if (args.length != 1 || !validateUrl(args[0])) {
//			System.err.println("ERROR: Invalid parameter(s)! This application takes in a single website url.");
			logger.debug("Invalid parameter(s)! This application takes in a single website url.");
			return;
		}
		
		client = HttpClient.newHttpClient();
		entryUrl = args[0];
		
		try {
			targetDirectory = Resources.getResourceAsFile("img");
		} catch (IOException e) {
//			System.out.println("ERROR: IOException: " + e.getMessage());
			logger.error("Could not find resource: " + e.getMessage());
			return;
		}
		
//		System.out.println(String.format("Crawling %s...", entryUrl));
		logger.info(String.format("Crawling %s...", entryUrl));
		
		InputStream inputStream = null;
		try {
			inputStream = Resources.getResourceAsStream("mybatis-config.xml");
		} catch (IOException e) {
//			System.out.println("An error has occured wit MyBatis!: " + e.getMessage());
			logger.error("An error has occured wit MyBatis!: " + e.getMessage());
			return;
		}
		sqlSession = new SqlSessionFactoryBuilder().build(inputStream).openSession();
		
		crawlPages(entryUrl);
		
		sqlSession.close();
//		System.out.println("## DONE ##");
		logger.info("## DONE ##");
	}
	


	private static void crawlPages(String entryUrl) {
		Matcher collectionsMatcher = COLLECTIONS_PATTERN.matcher(entryUrl);
		Matcher shopMatcher = SHOP_PATTERN.matcher(entryUrl);
		
		if (!shopMatcher.find() && (collectionsMatcher.matches() || !collectionsMatcher.find())) {
			logger.error("Invalid entry URL!");
			return;
		}
		
		Document document = null;
		try {
			document = Jsoup.connect(entryUrl).get();
		} catch (IOException e) {
			logger.error("Could not download page!: " + e.getMessage());
			return;
		}
		
		Elements pageElements = document.select(".page-numbers");		
		int size = pageElements.size();
		Element pageElement = size <= 1 ? null : pageElements.get(size - 2);
		
		entryUrl = entryUrl.replaceAll("/page/\\d/", "/");
		logger.info("Crawling: " + entryUrl);
//		System.out.println(entryUrl);
		
		crawlItems(entryUrl);
		
		if (pageElement == null)
			return;
		
		int maxPage = Integer.parseInt(pageElement.text());
		for (int i = 2; i <= maxPage; i++) {
			String nextPage = String.format("%s/page/%d", entryUrl, i);
			crawlItems(nextPage);
		}
	}
	
	private static void crawlItems(String pageUrl) {
		Document document = null;
		try {
			document = Jsoup.connect(pageUrl).get();
		} catch (IOException e) {
			logger.error("Could not download: " + e.getMessage());
			return;
		}
		
		Elements itemTitleElements = document.select(".woocommerce-loop-product__title");
//		System.out.println("@@@@@@@@@@@@@@@ Items in " + pageUrl + ": " + itemTitleElements.size());
		
		for (Element itemTitleElement : itemTitleElements) {
			String itemUrl = itemTitleElement.child(0).attr("abs:href");
			logger.info("Downloading: " + itemUrl);
//			System.out.println(itemUrl);
			getItemData(itemUrl);
		}
		
		sqlSession.commit();
	}

	private static void getItemData(String itemUrl) {
		Document document = null;
		try {
			document = Jsoup.connect(itemUrl).get();
		} catch (IOException e) {
			logger.error("Could not download: " + e.getMessage());
			return;
		}
		
		String title = getTitle(document);
		double price = getPrice(document);
		String desc = getDescription(document);
		
		Item newItem = new Item(title, itemUrl, price, desc);
		
		ItemMapper itemMapper = sqlSession.getMapper(ItemMapper.class);
		Item databaseItem = itemMapper.selectItemByUrl(itemUrl);
		
		if (databaseItem == null) {
			itemMapper.insertItem(newItem);
			insertItem(document, newItem);
		} else if (!newItem.equals(databaseItem)) {
			itemMapper.updateItem(newItem);
		} 
//			System.out.println("SAME!!");
	}
	
	private static void insertItem(Document document, Item newItem) {
		System.out.println("INSERTING ITEM...");
		String imageUrl = getImageUrl(document);
		String newImageLocation = downloadImage(newItem, imageUrl);
		
		if (newImageLocation.equals("FAIL")) {
			sqlSession.rollback();
			return;
		}
		
		Image newImage = new Image(newItem.id, imageUrl, newImageLocation);
		
		ImageMapper imageMapper = sqlSession.getMapper(ImageMapper.class);
		int affectedRows = imageMapper.insertImage(newImage);
		
//		System.out.println(affectedRows != 0 ? "INSERT SUCCESSFUL" : "INSERT FAIL");
		logger.info(affectedRows != 0 ? "INSERT SUCCESSFUL" : "INSERT FAIL");
	}
	
	private static boolean validateUrl(String url) {
		try {
			new URL(url).toURI();
			return true;
		} catch (Exception e) {
			logger.error("Invalid url: " + url);
			return false;
		}
	}

	public static String downloadImage(Item newItem, String imageUrl) {
		URI uri = null;
		try {
			uri = new URI(imageUrl);
		} catch (URISyntaxException e) {
//			System.out.println("Error: URISyntaxException: " + e.getMessage());
			logger.error(e.getMessage());
			return "FAIL";
		}
		
		HttpRequest request = HttpRequest.newBuilder()
	             .uri(uri)
	             .GET()
	             .build();
		
		HttpResponse<byte[]> response = null;
		try {
			response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
		} catch (IOException | InterruptedException e) {
//			System.out.println("ERROR: " + e.getMessage());
			logger.error(e.getMessage());
			return "FAIL";
		}
		
		String location = "\\img\\img" + newItem.id + getFormat(imageUrl);
		String newImagePath = targetDirectory.getParent().replaceAll("%20", " ") + location;
		File newImg = new File(newImagePath);
		
		try {
			newImg.createNewFile();
			Files.write(Path.of(newImagePath), response.body());
		} catch (IOException e) {
//			System.out.println("ERROR: IOException: " + e.getMessage());
			logger.error(e.getMessage());
			return "FAIL";
		}
		
		return location;
	}

	public static String getImageUrl(Document document) {
		Element imageElement = document.selectFirst(".wp-post-image");
		if (imageElement == null)
			imageElement = document.selectFirst(".zoomImg");
		String imgUrl = imageElement.attr("src");
		return imgUrl;
	}

	public static String getDescription(Document document) {
		Element descriptionElement = document.selectFirst("#tab-description");
		if (descriptionElement == null)
			return "No description available";
		String description = descriptionElement.text();
		return description;
	}

	public static String getTitle(Document document) {
		Element titleElement = document.selectFirst(".product_title");
		String title = titleElement.text();
		if (title.indexOf("–") != -1)
			title = title.replaceAll(" – ", " ");
		
		return title;
	}
	
	public static double getPrice(Document document) {
		Element priceElement = document.selectFirst("p.price span bdi");
		return priceElement == null ? 0 : Double.parseDouble(priceElement.text().substring(1));
	}
	
	public static String getFormat(String imageUrl) {
		int lastDot = imageUrl.lastIndexOf(".");
		return imageUrl.substring(lastDot);
	}
}