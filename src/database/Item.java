package database;

import java.sql.Date;

public class Item {
	public int id;
	String url;
	String name;
	String description;
	String location;
	Date lastUpdate;
	
	public Item(String url, String name) {
		this.url = url;
		this.name = name;
	}
}
