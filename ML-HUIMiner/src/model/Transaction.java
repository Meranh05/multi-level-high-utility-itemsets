package model;

import java.util.ArrayList;
import java.util.List;

public class Transaction {
    private int tid;
    private List<TransactionItem> items = new ArrayList<>();

    public Transaction(int tid) {
        this.tid = tid;
    }

    public void addItem(TransactionItem ti) {
        items.add(ti);
    }

    public List<TransactionItem> getItems() {
        return items;
    }
}
