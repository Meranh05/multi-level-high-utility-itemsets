package model;

public class TransactionItem {
    private Item item;
    private int quantity;

    public TransactionItem(Item item, int quantity) {
        this.item = item;
        this.quantity = quantity;
    }

    public int getUtility() {
        return item.getProfit() * quantity;
    }

    public Item getItem() {
        return item;
    }
}
