package view;

import miner.BasicHUIMiner;
import miner.GeneralizedHUIMiner;
import model.*;

import javax.swing.*;
import java.awt.BorderLayout;
import java.util.Arrays;
import java.util.List;
import javax.swing.table.DefaultTableModel;

public class MainFrame extends JFrame {

    private JTextField minUtilField = new JTextField("20");
    private JTextArea output = new JTextArea();
    private JTable table;
    private DefaultTableModel tableModel;


    public MainFrame() {
        setTitle("ML-HUIM Demo");
        setSize(600, 400);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        JPanel top = new JPanel(new BorderLayout());
        top.add(new JLabel("Minimum Utility:"), BorderLayout.WEST);
        top.add(minUtilField, BorderLayout.CENTER);

        JButton runBtn = new JButton("Run");
        top.add(runBtn, BorderLayout.EAST);

        add(top, BorderLayout.NORTH);
        tableModel = new DefaultTableModel(
        new Object[]{"Algorithm", "Itemset", "Utility"}, 0);
        table = new JTable(tableModel);

        add(new JScrollPane(table), BorderLayout.CENTER);

        runBtn.addActionListener(e -> runAlgorithm());
    }

    private void runAlgorithm() {
    tableModel.setRowCount(0); // clear bảng

    int minUtil = Integer.parseInt(minUtilField.getText());

    List<Transaction> db = SampleDataProvider.getTransactions();
    Taxonomy tax = SampleDataProvider.getTaxonomy();

    output.append("=== BASIC HUIM ===\n");
    BasicHUIMiner basic = new BasicHUIMiner();
    for (String s : basic.mine(db, minUtil)) {

        String itemset = s.substring(
            s.indexOf("{"), s.indexOf("}") + 1
        );
        String util = s.substring(s.lastIndexOf("=") + 1).trim();

        tableModel.addRow(new Object[]{
            "Basic",
            itemset,
            util
        });
}


    output.append("\n=== GENERALIZED HUIM ===\n");
    GeneralizedHUIMiner gen = new GeneralizedHUIMiner(tax);
    for (String s : gen.mine(db, minUtil)) {

        String itemset = s.substring(
            s.indexOf("{"), s.indexOf("}") + 1
        );
        String util = s.substring(s.lastIndexOf("=") + 1).trim();

        tableModel.addRow(new Object[]{
            "Generalized",
            itemset,
            util
        });
}

}

}
