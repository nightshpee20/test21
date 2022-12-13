package database;

import java.sql.Date;
 
public class Item {
	public int id;
	public String title;
	public String url;
	public double price;
	public String description;
	public Date lastUpdate;
	public char presentOrMissing;
	
	public Item(String title, String url, double price, String description) {
		this.title = title;
		this.url = url;
		this.price = price;
		this.description = description;
	}
	
	public Item(int id, String title, String url, double price, String description, Date lastUpdate, char presentOrMissing) {
		this.id = id;
		this.title = title;
		this.url = url;
		this.price = price;
		this.description = description;
		this.lastUpdate = lastUpdate;
		this.presentOrMissing = presentOrMissing;
	}
	
	public boolean equals(Item item) {
		if (!title.equals(item.title))
			return false;
		if (price != item.price)
			return false;
		if (!description.equals(item.description))
			return false;
		return true;
	}
	
	@Override
	public String toString() {
		return String.format("title: %s, url: %s, price: %f, description: %s", title, url, price, description);
	}
}
