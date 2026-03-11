import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.plaf.basic.BasicScrollBarUI;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.util.ArrayList;
import java.util.List;

public class MLHUI extends JFrame {

    private JTextArea txtTransactions, txtExternalUtility, txtTaxonomy, txtOutput;
    private JTextField txtMinUtil;
    private JTextField txtTransactionsPath, txtExternalUtilityPath, txtTaxonomyPath;
    private JLabel lblStatus, lblTime, lblMemory;
    private JButton btnRun, btnExport, btnAbout;
    private final List<String> outputLines = new ArrayList<>();
    private int runCount = 0;
    private final java.util.Map<Integer, Integer> huiIndexByLevel = new java.util.HashMap<>();
    private Integer currentLevel = null;

    private final Color colorBg = new Color(0xF2F4F8);
    private final Color colorCard = Color.WHITE;
    private final Color colorBorder = new Color(0xDADDE3);
    private final Color colorAccent = new Color(0x2563EB);
    private final Color colorMuted = new Color(0x667085);

    private final MLHUIMiner miner = new MLHUIMiner();

    public MLHUI() {
        applyModernUi();
        initComponents();
        setTitle("ML-HUI Miner Paper Correct Version");
        setSize(1100, 720);
        setMinimumSize(new Dimension(980, 640));
        setLocationRelativeTo(null);
        buildUi();
        wireEvents();
    }

    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        northPanel = new javax.swing.JPanel();
        splitPane = new javax.swing.JSplitPane();
        inputScroll = new javax.swing.JScrollPane();
        outputPanel = new javax.swing.JPanel();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(northPanel, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(splitPane, javax.swing.GroupLayout.Alignment.TRAILING, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(northPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, 0)
                .addComponent(splitPane, 0, 300, Short.MAX_VALUE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void wireEvents() {
        btnRun.addActionListener(e -> runMLHUI());
        btnExport.addActionListener(e -> exportOutputToTxt());
        btnAbout.addActionListener(e -> showAbout());
    }

    private void buildUi() {
        getContentPane().setBackground(colorBg);

        // ===== INPUT AREAS =====
        JPanel inputColumn = new JPanel();
        inputColumn.setLayout(new BoxLayout(inputColumn, BoxLayout.Y_AXIS));
        inputColumn.setBorder(new EmptyBorder(10, 12, 12, 12));
        inputColumn.setOpaque(false);

        txtTransactions = new JTextArea(
                "T1: Coke 2, Bread 2, Steak 1\n" +
                "T2: Water 3, Pasta 2, Steak 1\n" +
                "T3: Water 2, Bread 2\n" +
                "T4: Coke 1, Bread 2"
        );
        styleInputArea(txtTransactions);
        txtTransactionsPath = new JTextField();
        inputColumn.add(createInputPanel("Transactions", txtTransactions, txtTransactionsPath));
        inputColumn.add(Box.createVerticalStrut(10));

        txtExternalUtility = new JTextArea(
                "Water: 1\nCoke: 5\nBread: 1\nPasta: 2\nSteak: 10"
        );
        styleInputArea(txtExternalUtility);
        txtExternalUtilityPath = new JTextField();
        inputColumn.add(createInputPanel("External Utility", txtExternalUtility, txtExternalUtilityPath));
        inputColumn.add(Box.createVerticalStrut(10));

        txtTaxonomy = new JTextArea(
                "Beverage: Coke, Water\nFood: Bread, Pasta, Steak"
        );
        styleInputArea(txtTaxonomy);
        txtTaxonomyPath = new JTextField();
        inputColumn.add(createInputPanel("Taxonomy", txtTaxonomy, txtTaxonomyPath));

        inputScroll.setViewportView(inputColumn);
        inputScroll.setBorder(BorderFactory.createEmptyBorder());
        inputScroll.getViewport().setBackground(colorBg);

        // ===== OUTPUT =====
        txtOutput = new JTextArea();
        txtOutput.setFont(new Font("Consolas", Font.PLAIN, 13));
        txtOutput.setEditable(false);
        txtOutput.setBackground(colorCard);
        txtOutput.setBorder(new EmptyBorder(6, 6, 6, 6));
        JScrollPane outputScroll = scroll(txtOutput, "Result Log");

        JPanel outputHeader = new JPanel(new BorderLayout(8, 0));
        outputHeader.setBorder(new EmptyBorder(10, 12, 10, 12));
        outputHeader.setBackground(colorCard);

        JLabel outputTitle = new JLabel("Output");
        outputTitle.setFont(outputTitle.getFont().deriveFont(Font.BOLD, 14f));
        outputHeader.add(outputTitle, BorderLayout.WEST);

        JPanel stats = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        stats.setOpaque(false);
        lblStatus = new JLabel("Status: Idle");
        lblTime = new JLabel("Time: -");
        lblMemory = new JLabel("Memory: -");
        lblStatus.setForeground(colorMuted);
        lblTime.setForeground(colorMuted);
        lblMemory.setForeground(colorMuted);
        stats.add(lblStatus);
        stats.add(lblTime);
        stats.add(lblMemory);
        outputHeader.add(stats, BorderLayout.EAST);

        outputPanel.setLayout(new BorderLayout(0, 0));
        outputPanel.setBackground(colorCard);
        outputPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(colorBorder),
                new EmptyBorder(2, 2, 2, 2)
        ));
        outputPanel.add(outputHeader, BorderLayout.NORTH);
        outputPanel.add(outputScroll, BorderLayout.CENTER);

        // ===== SPLIT PANE =====
        splitPane.setOrientation(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setLeftComponent(inputScroll);
        splitPane.setRightComponent(outputPanel);
        splitPane.setResizeWeight(0.45);
        splitPane.setDividerLocation(460);
        splitPane.setBorder(new EmptyBorder(0, 0, 0, 0));
        splitPane.setDividerSize(8);
        splitPane.setBackground(colorBg);

        // ===== NORTH BAR =====
        northPanel.setLayout(new BorderLayout(12, 0));
        northPanel.setBorder(new EmptyBorder(10, 12, 8, 12));
        northPanel.setBackground(colorBg);

        JLabel title = new JLabel("ML-HUI Miner");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 18f));
        JLabel subtitle = new JLabel("Multi-level high-utility itemset mining");
        subtitle.setForeground(colorMuted);

        JPanel titleBox = new JPanel();
        titleBox.setLayout(new BoxLayout(titleBox, BoxLayout.Y_AXIS));
        titleBox.setOpaque(false);
        titleBox.add(title);
        titleBox.add(subtitle);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.setOpaque(false);
        JLabel minUtilLabel = new JLabel("MinUtil");
        minUtilLabel.setForeground(colorMuted);
        actions.add(minUtilLabel);
        txtMinUtil = new JTextField("20", 6);
        actions.add(txtMinUtil);

        btnRun = new JButton("Run");
        btnExport = new JButton("Export TXT");
        btnAbout = new JButton("About");
        styleButton(btnRun, true);
        styleButton(btnExport, false);
        styleButton(btnAbout, false);
        actions.add(btnRun);
        actions.add(btnExport);
        actions.add(btnAbout);

        northPanel.removeAll();
        northPanel.add(titleBox, BorderLayout.WEST);
        northPanel.add(actions, BorderLayout.EAST);
        northPanel.revalidate();
        northPanel.repaint();
    }

    private JScrollPane scroll(JTextArea ta, String title) {
        JScrollPane sp = new JScrollPane(ta);
        TitledBorder border = BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(colorBorder),
                title
        );
        border.setTitleColor(colorMuted);
        sp.setBorder(border);
        sp.getViewport().setBackground(colorCard);
        styleScrollBar(sp);
        return sp;
    }

    private JPanel createInputPanel(String title, JTextArea area, JTextField pathField) {
        JPanel panel = new JPanel(new BorderLayout(6, 6));
        panel.setBackground(colorCard);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(colorBorder),
                new EmptyBorder(8, 8, 8, 8)
        ));
        JPanel top = new JPanel(new BorderLayout(6, 0));
        top.setBorder(new EmptyBorder(0, 0, 6, 0));
        top.setOpaque(false);

        JLabel fileLabel = new JLabel("Input file");
        fileLabel.setForeground(colorMuted);
        top.add(fileLabel, BorderLayout.WEST);

        pathField.setEditable(false);
        pathField.setToolTipText("Drop a file here or click Browse");
        pathField.setBackground(new Color(0xF8FAFC));
        pathField.setBorder(BorderFactory.createLineBorder(colorBorder));
        JButton browse = new JButton("Browse");
        styleButton(browse, false);
        browse.addActionListener(e -> chooseFileInto(area, pathField));
        top.add(pathField, BorderLayout.CENTER);
        top.add(browse, BorderLayout.EAST);
        panel.add(top, BorderLayout.NORTH);
        panel.add(scroll(area, title), BorderLayout.CENTER);

        installFileDrop(panel, area, pathField);
        installFileDrop(area, area, pathField);

        return panel;
    }

    private void styleInputArea(JTextArea area) {
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setBorder(new EmptyBorder(6, 6, 6, 6));
        area.setBackground(Color.WHITE);
    }

    private void applyModernUi() {
        try {
            UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel");
        } catch (Exception ignored) {
        }
        Font base = new Font("Segoe UI", Font.PLAIN, 13);
        UIManager.put("Label.font", base);
        UIManager.put("Button.font", base);
        UIManager.put("TextField.font", base);
        UIManager.put("TextArea.font", base);
        UIManager.put("TitledBorder.font", base.deriveFont(Font.BOLD, 12f));
        UIManager.put("ScrollBar.width", 12);
    }

    private void styleScrollBar(JScrollPane sp) {
        JScrollBar vBar = sp.getVerticalScrollBar();
        JScrollBar hBar = sp.getHorizontalScrollBar();
        vBar.setUI(new ModernScrollBarUI());
        hBar.setUI(new ModernScrollBarUI());
        vBar.setUnitIncrement(16);
        hBar.setUnitIncrement(16);
        vBar.setOpaque(false);
        hBar.setOpaque(false);
        sp.setBorder(BorderFactory.createCompoundBorder(
                sp.getBorder(),
                new EmptyBorder(2, 2, 2, 2)
        ));
    }

    private class ModernScrollBarUI extends BasicScrollBarUI {
        @Override
        protected void configureScrollBarColors() {
            this.thumbColor = new Color(0xC9CED6);
            this.thumbDarkShadowColor = new Color(0xC9CED6);
            this.thumbHighlightColor = new Color(0xC9CED6);
            this.thumbLightShadowColor = new Color(0xC9CED6);
            this.trackColor = new Color(0xF2F4F8);
        }

        @Override
        protected JButton createDecreaseButton(int orientation) {
            return createZeroButton();
        }

        @Override
        protected JButton createIncreaseButton(int orientation) {
            return createZeroButton();
        }

        private JButton createZeroButton() {
            JButton btn = new JButton();
            btn.setPreferredSize(new Dimension(0, 0));
            btn.setMinimumSize(new Dimension(0, 0));
            btn.setMaximumSize(new Dimension(0, 0));
            return btn;
        }

        @Override
        protected void paintTrack(Graphics g, JComponent c, Rectangle trackBounds) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setColor(new Color(0xF2F4F8));
            g2.fillRect(trackBounds.x, trackBounds.y, trackBounds.width, trackBounds.height);
            g2.dispose();
        }

        @Override
        protected void paintThumb(Graphics g, JComponent c, Rectangle thumbBounds) {
            if (thumbBounds.isEmpty() || !scrollbar.isEnabled()) return;
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(new Color(0xC9CED6));
            int arc = 10;
            g2.fillRoundRect(thumbBounds.x, thumbBounds.y, thumbBounds.width, thumbBounds.height, arc, arc);
            g2.dispose();
        }
    }

    private void styleButton(JButton button, boolean primary) {
        button.setFocusPainted(false);
        button.setOpaque(true);
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(primary ? colorAccent : colorBorder),
                new EmptyBorder(6, 14, 6, 14)
        ));
        if (primary) {
            button.setBackground(colorAccent);
            button.setForeground(Color.WHITE);
        } else {
            button.setBackground(Color.WHITE);
            button.setForeground(new Color(0x344054));
        }
    }

    private void chooseFileInto(JTextArea area, JTextField pathField) {
        JFileChooser chooser = createSystemFileChooser();
        int res = chooser.showOpenDialog(this);
        if (res == JFileChooser.APPROVE_OPTION) {
            loadFileIntoArea(chooser.getSelectedFile(), area, pathField);
        }
    }

    private void installFileDrop(JComponent component, JTextArea area, JTextField pathField) {
        component.setTransferHandler(new TransferHandler() {
            @Override
            public boolean canImport(TransferSupport support) {
                return support.isDataFlavorSupported(DataFlavor.javaFileListFlavor);
            }

            @Override
            public boolean importData(TransferSupport support) {
                if (!canImport(support)) return false;
                try {
                    @SuppressWarnings("unchecked")
                    List<java.io.File> files = (List<java.io.File>) support.getTransferable()
                            .getTransferData(DataFlavor.javaFileListFlavor);
                    if (files.isEmpty()) return false;
                    loadFileIntoArea(files.get(0), area, pathField);
                    return true;
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(MLHUI.this,
                            "Cannot read file: " + ex.getMessage(),
                            "File Error", JOptionPane.ERROR_MESSAGE);
                    return false;
                }
            }
        });
    }

    private void loadFileIntoArea(java.io.File file, JTextArea area, JTextField pathField) {
        try {
            String content = java.nio.file.Files.readString(file.toPath());
            area.setText(content);
            pathField.setText(file.getAbsolutePath());
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "Cannot read file: " + ex.getMessage(),
                    "File Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void runMLHUI() {

        txtOutput.setText("");
        outputLines.clear();
        lblStatus.setText("Status: Running...");
        lblTime.setText("Time: -");
        lblMemory.setText("Memory: -");
        huiIndexByLevel.clear();
        currentLevel = null;

        int minUtil = Integer.parseInt(txtMinUtil.getText().trim());
        String minUtilLine = "--- minUtil = " + minUtil+" ----";
        outputLines.add(minUtilLine);
        txtOutput.append(minUtilLine + "\n");

        MLHUIMiner.Result result = miner.run(
                txtTransactions.getText(),
                txtExternalUtility.getText(),
                txtTaxonomy.getText(),
                minUtil,
                this::handleLog
        );
        runCount++;

        lblStatus.setText("Status: Done");
        lblTime.setText("Time: " + result.timeMs + " ms");
        lblMemory.setText("Memory: " + result.memoryKb + " KB");
        appendUiOnly("\n[4] Performance");
        appendUiOnly("    Time   : " + result.timeMs + " ms");
        appendUiOnly("    Memory : " + result.memoryKb + " KB");
    }

    private void handleLog(String s) {
        String uiLine = formatLogLine(s);
        String exportLine = formatExportLine(s);
        if (exportLine != null && !exportLine.isEmpty()) {
            for (String line : exportLine.split("\\R", -1)) {
                if (!line.isEmpty()) outputLines.add(line);
            }
        }
        txtOutput.append(uiLine + "\n");
    }

    private void appendUiOnly(String s) {
        txtOutput.append(s + "\n");
    }

    private String formatLogLine(String s) {
        if (s == null || s.isEmpty()) return s;
        if (s.startsWith("=== ML-HUI START")) {
            String minutil = s.replace("=== ML-HUI START (minutil=", "")
                    .replace(") ===", "").trim();
            return "ML-HUI START | minutil=" + minutil + "\n" +
                    "----------------------------------------\n" +
                    "[1] Items kept by TWU";
        }
        if (s.startsWith("[KEEP")) {
            String clean = s.replace("[", "").replace("]", "");
            String[] parts = clean.split(" ");
            String level = parts.length > 1 ? parts[1] : "";
            String item = parts.length > 2 ? parts[2] : "";
            String twu = clean.contains("TWU=")
                    ? clean.substring(clean.indexOf("TWU=") + 4)
                    : "";
            return "    - " + level + ": " + item + "  (TWU=" + twu + ")";
        }
        if (s.startsWith("--- DFS LEVEL")) {
            String levelText = s.replace("--- DFS LEVEL", "").replace("---", "").trim();
            try {
                currentLevel = Integer.parseInt(levelText);
            } catch (NumberFormatException ignored) {
                currentLevel = null;
            }
            int titleNo = (currentLevel == null) ? 2 : (currentLevel + 2);
            return "\n[" + titleNo + "] HUI Results - Level " +
                    (currentLevel == null ? levelText : currentLevel);
        }
        if (s.startsWith("HUI ")) {
            int levelKey = (currentLevel == null) ? -1 : currentLevel;
            int next = huiIndexByLevel.getOrDefault(levelKey, 0) + 1;
            huiIndexByLevel.put(levelKey, next);
            String content = s.replace("HUI ", "").replace(" = ", "  ->  ");
            return "    " + next + ") " + content;
        }
        return s;
    }

    private String formatExportLine(String s) {
        String formatted = formatLogLine(s);
        if (formatted == null || formatted.isEmpty()) return formatted;
        if (isResultHeader(formatted)) return formatted.trim();
        if (isResultLine(formatted)) return stripStepPrefix(formatted);
        return "";
    }

    private String stripStepPrefix(String line) {
        String cleaned = line;
        cleaned = cleaned.replaceFirst("^\\s*Bước\\s+\\d+(?:-\\d+)?\\s*:\\s*", "");
        cleaned = cleaned.replaceFirst("^\\s*\\[\\d+\\]\\s*", "");
        return cleaned;
    }

    private boolean isResultLine(String line) {
        String trimmed = line == null ? "" : line.trim();
        if (trimmed.isEmpty()) return false;
        if (trimmed.matches("^\\d+\\)\\s+.*")) return true;
        return trimmed.contains("Không tìm thấy");
    }

    private boolean isResultHeader(String line) {
        String trimmed = line == null ? "" : line.trim();
        return trimmed.startsWith("--- KẾT QUẢ TẠI LEVEL");
    }

    private void exportOutputToTxt() {
        if (outputLines.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "No output to export. Run the miner first.",
                    "Export", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        JFileChooser chooser = createSystemFileChooser();
        chooser.setSelectedFile(new java.io.File(buildDefaultExportName()));
        int res = chooser.showSaveDialog(this);
        if (res == JFileChooser.APPROVE_OPTION) {
            java.io.File file = ensureTxtExtension(chooser.getSelectedFile());
            try {
                java.nio.file.Files.write(file.toPath(), outputLines);
                JOptionPane.showMessageDialog(this,
                        "Exported to: " + file.getAbsolutePath(),
                        "Export", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this,
                        "Cannot write file: " + ex.getMessage(),
                        "Export Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private JFileChooser createSystemFileChooser() {
        String current = UIManager.getLookAndFeel().getClass().getName();
        try {
            String system = UIManager.getSystemLookAndFeelClassName();
            if (!current.equals(system)) {
                UIManager.setLookAndFeel(system);
            }
        } catch (Exception ignored) {
        }
        JFileChooser chooser = new JFileChooser();
        SwingUtilities.updateComponentTreeUI(chooser);
        try {
            UIManager.setLookAndFeel(current);
        } catch (Exception ignored) {
        }
        return chooser;
    }

    private String buildDefaultExportName() {
        String minUtilText = txtMinUtil.getText().trim();
        if (minUtilText.isEmpty()) minUtilText = "0";
        String time = new java.text.SimpleDateFormat("dd-MM-yyyy")
                .format(new java.util.Date());
        int runNo = Math.max(1, runCount);
        return "Output_Run" + runNo + "_Util_" + minUtilText + "_Date_" + time + ".txt";
    }

    private java.io.File ensureTxtExtension(java.io.File file) {
        String name = file.getName();
        if (!name.toLowerCase().endsWith(".txt")) {
            return new java.io.File(file.getParentFile(), name + ".txt");
        }
        return file;
    }

    private void showAbout() {
        String message =
                "ML-HUI Miner - Hướng dẫn sử dụng\n\n" +
                "1) Tải file đầu vào:\n" +
                "   - Transactions: danh sách giao dịch (T1: Item qty, ...)\n" +
                "   - External Utility: giá trị utility (Item: value)\n" +
                "   - Taxonomy: nhóm item (Group: item1, item2)\n" +
                "   Có thể kéo thả file hoặc bấm Browse.\n\n" +
                "2) Nhập MinUtil và bấm Run để chạy.\n\n" +
                "3) Kết quả hiển thị ở Output (có Time/Memory).\n\n" +
                "4) Export TXT: xuất log kết quả (không bao gồm Time/Memory).\n\n" +
                "Thuật ngữ trong chương trình:\n" +
                "- Transaction (T): danh sách các giao dịch, một giao dịch gồm nhiều item và số lượng.\n" +
                "- Item: sản phẩm/đối tượng trong giao dịch.\n" +
                "- External Utility: giá trị (lợi ích) của mỗi item.\n" +
                "- Utility: giá trị của item trong giao dịch (= số lượng * external utility).\n" +
                "- MinUtil: ngưỡng lợi ích tối thiểu để xét tập HUI.\n" +
                "- HUI: tập item có utility >= MinUtil.\n" +
                "- TWU: Tổng Utility trong tất cả giao dịch có chứa item.\n" +
                "- EUCS: thông tin đồng xuất hiện để cắt tỉa (pruning).\n" +
                "- DFS Level: quá trình tìm kiếm theo từng cấp (level).\n" +
                "- Time/Memory: thời gian xử lý và bộ nhớ sử dụng.";

        JOptionPane.showMessageDialog(this, message, "About", JOptionPane.INFORMATION_MESSAGE);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new MLHUI().setVisible(true));
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel northPanel;
    private javax.swing.JScrollPane inputScroll;
    private javax.swing.JPanel outputPanel;
    private javax.swing.JSplitPane splitPane;
    // End of variables declaration//GEN-END:variables
}
