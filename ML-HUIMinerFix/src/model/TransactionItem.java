package model;

public class TransactionItem {
    private final Item item;
    private final int quantity; // internal utility

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

    public int getQuantity() {
        return quantity;
    }
}
