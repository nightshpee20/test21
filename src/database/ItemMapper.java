package database;

public interface ItemMapper {
	public Item selectItemByUrlAndName(Item item);
	public int insertItem(Item item);
}
