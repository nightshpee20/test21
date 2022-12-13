package database;

public class Image {
	public int itemId;
	public String url;
	public String location;
	
	public Image(int itemId, String url, String location) {
		this.itemId = itemId;
		this.url = url;
		this.location = location;
	}
}
