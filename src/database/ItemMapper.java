package database;

public interface ItemMapper {
	public Item selectItemByUrl(String url);
	public int insertItem(Item item);
	public int updateItem(Item item);
}
