package main;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Set;
import java.util.concurrent.ExecutorService;

import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;

import database.Item;
import database.ItemMapper;

public class Main {
	private static ExecutorService threadPool;
	private static Set<String> commonImageFormats;
	private static SqlSessionFactory sqlSessionFactory;
	
	public static void main(String[] args) {
		if (args.length != 1) {
			System.err.println("ERROR: Invalid parameters. This application takes in a website url");
			return;
		}
		//TODO: Add url verification
		System.out.println("Crawling " + args[0] + "...");
		
		commonImageFormats = Set.of("gif", "jpeg", "jpg", "png", "tiff", "x-icon", "svg+xml");
		
		System.out.println(System.getProperty("java.class.path"));
		
		FileInputStream inputStream = null;
		InputStream inputStreamTest = null;
		try {
//			File config = new File("C:\\Users\\night\\Desktop\\Java Internship\\Price Grabber\\bin\\mybatis-config.xml");
//			inputStream = new FileInputStream(config);
			inputStreamTest = Resources.getResourceAsStream("mybatis-config.xml");
		} catch (IOException e) {
			System.err.println("An error has occured wit MyBatis!: " + e.getMessage());
		}
		sqlSessionFactory = new SqlSessionFactoryBuilder().build(inputStreamTest);
		
		System.out.println("baca");
		
		try (SqlSession sqlSession = sqlSessionFactory.openSession()) {
			ItemMapper mapper = sqlSession.getMapper(ItemMapper.class);
			
			Item item = new Item("https://www.onepiece.store", "ZORO Shirt");
			item = mapper.selectItemByUrlAndName(item);
			
			System.out.println(item.id);
		}
		
	}
}

//;C:\Users\night\Desktop\Java Internship\Price Grabber\bin\mybatis-config.xml;C:\Users\night\Desktop\Java Internship\Price Grabber\bin\ItemMapper.xml;C:\Users\night\Desktop\Java Internship\Price Grabber\bin\jdbc.properties
