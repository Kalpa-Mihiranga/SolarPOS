package com.mycompany.solarpos.ui;

import com.mycompany.solarpos.dao.StockValuationDAO;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;

public class StockValuationPanel extends JPanel {

    private static final Logger LOG = Logger.getLogger(StockValuationPanel.class.getName());
    private static final Color BLUE   = new Color(0, 120, 212);
    private static final Color GREEN  = new Color(39, 174, 96);
    private static final Color ORANGE = new Color(230, 126, 34);
    private static final Color RED    = new Color(192, 57, 43);

    private final StockValuationDAO dao = new StockValuationDAO();

    private final JLabel lblCostValue  = kpi("-");
    private final JLabel lblSaleValue  = kpi("-");
    private final JLabel lblLowStock   = kpi("-");
    private final JLabel lblOutOfStock = kpi("-");

    private final DefaultTableModel mainModel = new DefaultTableModel(
        new String[]{"Code","Item","Category","Stock","Cost Price",
                     "Sale Price","Stock Cost","Stock Value",
                     "Unit Margin","Sold","Turnover","Warranty End","Status"}, 0) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };
    private final DefaultTableModel catModel = new DefaultTableModel(
        new String[]{"Category","Items","Units","Cost Value","Sale Value","Out of Stock"}, 0) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };

    private final JTable mainTable = new JTable(mainModel);
    private final TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(mainModel);
    private final JTextField txtSearch = new JTextField(16);
    private final JComboBox<String> cmbFilter = new JComboBox<>(
            new String[]{"All","⚠ Slow-Moving Only","✓ OK Only","Out of Stock"});

    public StockValuationPanel() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        JLabel title = new JLabel("\uD83D\uDCE6  Stock Valuation");
        title.setFont(new Font("Segoe UI", Font.BOLD, 24));

        JButton refresh = new JButton("\u21BA  Refresh");
        refresh.setBackground(BLUE);
        refresh.setForeground(Color.WHITE);
        refresh.setFocusPainted(false);
        refresh.addActionListener(e -> load());

        JPanel header = new JPanel(new BorderLayout());
        header.add(title, BorderLayout.WEST);
        header.add(refresh, BorderLayout.EAST);
        header.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
        add(header, BorderLayout.NORTH);
        add(buildBody(), BorderLayout.CENTER);

        addComponentListener(new ComponentAdapter() {
            @Override public void componentShown(ComponentEvent e) { load(); }
        });
        load();
    }

    private JPanel buildBody() {
        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("\uD83D\uDCCB  All Stock", buildMainTab());
        tabs.addTab("\uD83D\uDCC8  By Category", buildCategoryTab());
        JPanel p = new JPanel(new BorderLayout());
        p.add(tabs);
        return p;
    }

    private JPanel buildMainTab() {
        JPanel p = new JPanel(new BorderLayout(0, 8));

        // KPI cards
        JPanel cards = new JPanel(new GridLayout(1, 4, 8, 0));
        cards.add(card("📦 Total Stock (Cost)",  lblCostValue,  "Rs.", BLUE));
        cards.add(card("💰 Total Stock (Value)", lblSaleValue,  "Rs.", GREEN));
        cards.add(card("⚠ Low Stock Value",      lblLowStock,   "Rs.", ORANGE));
        cards.add(card("❌ Out of Stock Items",   lblOutOfStock, "items", RED));
        p.add(cards, BorderLayout.NORTH);

        // Filter bar
        JPanel filterBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        filterBar.add(new JLabel("Search:"));
        filterBar.add(txtSearch);
        filterBar.add(new JLabel("Show:"));
        filterBar.add(cmbFilter);
        txtSearch.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e)  { applyFilter(); }
            @Override public void removeUpdate(DocumentEvent e)  { applyFilter(); }
            @Override public void changedUpdate(DocumentEvent e) { applyFilter(); }
        });
        cmbFilter.addActionListener(e -> applyFilter());

        // Table
        mainTable.setRowSorter(sorter);
        mainTable.setRowHeight(26);
        mainTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        rightAlign(mainTable, new int[]{3, 4, 5, 6, 7, 8, 9});

        // Status column renderer
        mainTable.getColumnModel().getColumn(12).setCellRenderer(
                new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object v, boolean sel,
                                                           boolean focus, int r, int c) {
                super.getTableCellRendererComponent(t, v, sel, focus, r, c);
                if (!sel && v != null) {
                    setForeground(v.toString().contains("SLOW")
                            ? RED : GREEN);
                    setFont(getFont().deriveFont(Font.BOLD));
                }
                return this;
            }
        });

        JPanel tablePanel = new JPanel(new BorderLayout(0, 4));
        tablePanel.add(filterBar, BorderLayout.NORTH);
        tablePanel.add(new JScrollPane(mainTable), BorderLayout.CENTER);

        JLabel hint = new JLabel(
                "  \uD83D\uDCA1 Slow-Moving = turnover < 25% OR supplier warranty ends within 6 months");
        hint.setForeground(Color.GRAY);
        hint.setFont(new Font("Segoe UI", Font.ITALIC, 11));
        tablePanel.add(hint, BorderLayout.SOUTH);

        p.add(tablePanel, BorderLayout.CENTER);
        return p;
    }

    private JPanel buildCategoryTab() {
        JTable t = new JTable(catModel);
        t.setRowHeight(26);
        rightAlign(t, new int[]{2, 3, 4, 5});
        JPanel p = new JPanel(new BorderLayout(0, 4));
        JLabel h = new JLabel("  Category-level inventory worth summary");
        h.setForeground(Color.GRAY);
        p.add(h, BorderLayout.NORTH);
        p.add(new JScrollPane(t), BorderLayout.CENTER);
        return p;
    }

    private void applyFilter() {
        String text    = txtSearch.getText().trim();
        String filter  = (String) cmbFilter.getSelectedItem();
        RowFilter<DefaultTableModel, Object> textFilter = text.isEmpty()
                ? null : RowFilter.regexFilter("(?i)" + Pattern.quote(text));
        RowFilter<DefaultTableModel, Object> statusFilter = null;
        if (filter != null) {
            if (filter.contains("Slow")) {
                statusFilter = RowFilter.regexFilter("SLOW", 12);
            } else if (filter.contains("OK")) {
                statusFilter = RowFilter.regexFilter("✓ OK", 12);
            } else if (filter.contains("Out")) {
                statusFilter = RowFilter.numberFilter(
                        RowFilter.ComparisonType.EQUAL, 0, 3);
            }
        }
        if (textFilter != null && statusFilter != null) {
            sorter.setRowFilter(RowFilter.andFilter(
                    java.util.List.of(textFilter, statusFilter)));
        } else if (textFilter != null) {
            sorter.setRowFilter(textFilter);
        } else {
            sorter.setRowFilter(statusFilter);
        }
    }

    private void load() {
        SwingWorker<Void, Void> w = new SwingWorker<>() {
            double[] totals;
            List<String[]> rows, cats;

            @Override protected Void doInBackground() {
                try {
                    totals = dao.totals();
                    rows   = dao.fullValuation();
                    cats   = dao.categorySummary();
                } catch (Exception ex) {
                    LOG.log(Level.SEVERE, "Stock valuation failed", ex);
                }
                return null;
            }

            @Override protected void done() {
                if (totals != null) {
                    lblCostValue.setText(String.format("Rs. %,.0f", totals[0]));
                    lblSaleValue.setText(String.format("Rs. %,.0f", totals[1]));
                    lblLowStock.setText(String.format("Rs. %,.0f", totals[2]));
                    lblOutOfStock.setText(String.format("%.0f", totals[3]));
                    lblLowStock.setForeground(totals[2] > 0 ? ORANGE : GREEN);
                    lblOutOfStock.setForeground(totals[3] > 0 ? RED : GREEN);
                }
                mainModel.setRowCount(0);
                if (rows != null) rows.forEach(mainModel::addRow);
                catModel.setRowCount(0);
                if (cats != null) cats.forEach(catModel::addRow);
            }
        };
        w.execute();
    }

    private static void rightAlign(JTable t, int[] cols) {
        DefaultTableCellRenderer r = new DefaultTableCellRenderer();
        r.setHorizontalAlignment(SwingConstants.RIGHT);
        for (int c : cols) t.getColumnModel().getColumn(c).setCellRenderer(r);
    }

    private static JPanel card(String title, JLabel num, String sub, Color accent) {
        JPanel card = new JPanel(new BorderLayout(0, 4));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(accent, 2, true),
                BorderFactory.createEmptyBorder(10, 14, 10, 14)));
        card.setBackground(lighten(accent));
        JLabel t = new JLabel(title);
        t.setFont(new Font("Segoe UI", Font.BOLD, 12));
        t.setForeground(accent.darker());
        num.setForeground(accent.darker().darker());
        JLabel s = new JLabel(sub);
        s.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        s.setForeground(accent.darker());
        card.add(t, BorderLayout.NORTH);
        card.add(num, BorderLayout.CENTER);
        card.add(s, BorderLayout.SOUTH);
        return card;
    }

    private static JLabel kpi(String t) {
        JLabel l = new JLabel(t);
        l.setFont(new Font("Segoe UI", Font.BOLD, 22));
        return l;
    }

    private static Color lighten(Color c) {
        return new Color(
            Math.min(255, c.getRed()   + (255 - c.getRed())   * 7 / 10),
            Math.min(255, c.getGreen() + (255 - c.getGreen()) * 7 / 10),
            Math.min(255, c.getBlue()  + (255 - c.getBlue())  * 7 / 10));
    }
}