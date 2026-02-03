package view;

import miner.BasicHUIMiner;
import miner.GeneralizedHUIMiner;
import model.SampleDataProvider;
import model.Taxonomy;
import model.Transaction;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainFrame extends JFrame {

    private final JTextField minUtilField = new JTextField("20");
    private final JTextField alphaStepField = new JTextField("0.5"); // alpha(level)=1+alphaStep*level
    private JTable table;
    private DefaultTableModel tableModel;

    private static final Pattern ITEMSET_PATTERN = Pattern.compile("^\\{.*?\\}");
    private static final Pattern UTIL_PATTERN = Pattern.compile("Utility\\s*=\\s*(\\d+)");

    public MainFrame() {
        setTitle("ML-HUIM Demo (HUI + GHUI, multi-level)");
        setSize(820, 420);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        JPanel top = new JPanel();
        top.setLayout(new BoxLayout(top, BoxLayout.X_AXIS));

        top.add(new JLabel("minUtil: "));
        top.add(minUtilField);
        top.add(Box.createHorizontalStrut(12));

        top.add(new JLabel("alphaStep: "));
        top.add(alphaStepField);
        top.add(Box.createHorizontalStrut(12));

        JButton runBtn = new JButton("Run");
        top.add(runBtn);

        add(top, BorderLayout.NORTH);

        tableModel = new DefaultTableModel(
                new Object[]{"Algorithm", "Itemset", "Utility", "Meta"}, 0
        );
        table = new JTable(tableModel);
        add(new JScrollPane(table), BorderLayout.CENTER);

        runBtn.addActionListener(e -> runAlgorithm());
    }

    private void runAlgorithm() {
        tableModel.setRowCount(0);

        int minUtil;
        double alphaStep;
        try {
            minUtil = Integer.parseInt(minUtilField.getText().trim());
            alphaStep = Double.parseDouble(alphaStepField.getText().trim());
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Invalid minUtil/alphaStep");
            return;
        }

        List<Transaction> db = SampleDataProvider.getTransactions();
        Taxonomy tax = SampleDataProvider.getTaxonomy();

        // --- Basic HUIM (level 0 only)
        BasicHUIMiner basic = new BasicHUIMiner();
        for (String s : basic.mine(db, minUtil)) {
            addRow("Basic (Level 0)", s);
        }

        // --- ML-HUI Miner (HUI + GHUI across levels)
        GeneralizedHUIMiner ml = new GeneralizedHUIMiner(tax, alphaStep);
        for (String s : ml.mine(db, minUtil)) {
            addRow("ML-HUI Miner", s);
        }
    }

    private void addRow(String algo, String line) {
        String itemset = extractItemset(line);
        String util = extractUtility(line);
        String meta = line.replace(itemset, "").replace("->", "").trim();

        tableModel.addRow(new Object[]{algo, itemset, util, meta});
    }

    private String extractItemset(String s) {
        Matcher m = ITEMSET_PATTERN.matcher(s);
        return m.find() ? m.group() : "";
    }

    private String extractUtility(String s) {
        Matcher m = UTIL_PATTERN.matcher(s);
        return m.find() ? m.group(1) : "";
    }
}
