package model;

import java.util.Arrays;
import java.util.List;

public class SampleDataProvider {

    public static List<Transaction> getTransactions() {

        Item water =new Item("Water", 1);
        Item coke = new Item("Coke", 5);
        Item bread = new Item("Bread", 1);
        Item pasta = new Item("Pasta", 2);
        Item steak = new Item("Steak", 10);

        Transaction t1 = new Transaction(1);
        t1.addItem(new TransactionItem(coke, 2));
        t1.addItem(new TransactionItem(bread, 2));
        t1.addItem(new TransactionItem(steak, 1));

        Transaction t2 = new Transaction(2);
        t2.addItem(new TransactionItem(water, 3));
        t2.addItem(new TransactionItem(pasta, 2));
        t2.addItem(new TransactionItem(steak, 1));

        Transaction t3 = new Transaction(3);
        t3.addItem(new TransactionItem(water, 2));
        t3.addItem(new TransactionItem(bread, 2));

        Transaction t4 = new Transaction(4);
        t4.addItem(new TransactionItem(coke, 1));
        t4.addItem(new TransactionItem(bread, 2));
        return Arrays.asList(t1, t2, t3,t4);
    }

    public static Taxonomy getTaxonomy() {
        Taxonomy tax = new Taxonomy();

        tax.addRelation("Coke", "Beverage");
        tax.addRelation("Water", "Beverage");

        tax.addRelation("Bread", "Food");
        tax.addRelation("Pasta", "Food");
        tax.addRelation("Steak", "Food");

        return tax;
    }
}
