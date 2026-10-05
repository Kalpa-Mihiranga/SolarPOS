package com.mycompany.solarpos.ui;

import com.mycompany.solarpos.dao.ProfitDAO;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.chart.renderer.category.StandardBarPainter;
import org.jfree.data.category.DefaultCategoryDataset;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.*;
import javax.swing.table.*;

public class ProfitPanel extends JPanel {

    private static final Logger LOG   = Logger.getLogger(ProfitPanel.class.getName());
    private static final Color GREEN  = new Color(39, 174, 96);
    private static final Color BLUE   = new Color(0, 120, 212);
    private static final Color ORANGE = new Color(230, 126, 34);
    private static final Color RED    = new Color(192, 57, 43);

    private final ProfitDAO dao = new ProfitDAO();

    // KPI cards
    private final JLabel lblRevenue     = kpiLabel("-");
    private final JLabel lblCogs        = kpiLabel("-");
    private final JLabel lblProfit      = kpiLabel("-");
    private final JLabel lblMargin      = kpiLabel("-");
    private final JLabel lblTax         = kpiLabel("-");
    private final JLabel lblDiscount    = kpiLabel("-");
    private final JLabel lblOutstanding = kpiLabel("-");
    private final JLabel lblOrders      = kpiLabel("-");

    // Tables
    private final DefaultTableModel monthlyModel = new DefaultTableModel(
        new String[]{"Period","Orders","Revenue","COGS","Gross Profit",
                     "Margin","Tax Collected","Discounts","Warranty Income"}, 0) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };
    private final DefaultTableModel categoryModel = new DefaultTableModel(
        new String[]{"Category","Orders","Units","Revenue","COGS","Profit","Margin"}, 0) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };
    private final DefaultTableModel productModel = new DefaultTableModel(
        new String[]{"Product","Category","Units Sold","Revenue","Profit"}, 0) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };

    // Chart
    private final DefaultCategoryDataset chartDataset = new DefaultCategoryDataset();
    private final JComboBox<String> cmbMonths =
            new JComboBox<>(new String[]{"3 months","6 months","12 months","24 months"});

    public ProfitPanel() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        add(buildHeader(), BorderLayout.NORTH);
        add(buildBody(),   BorderLayout.CENTER);

        addComponentListener(new ComponentAdapter() {
            @Override public void componentShown(ComponentEvent e) { refresh(); }
        });
        refresh();
    }

    // ============================================ layout

    private JPanel buildHeader() {
        JLabel title = new JLabel("\uD83D\uDCB9  Profit & Loss");
        title.setFont(new Font("Segoe UI", Font.BOLD, 24));

        cmbMonths.setSelectedIndex(1);
        cmbMonths.addActionListener(e -> refresh());

        JButton btnRefresh = new JButton("\u21BA  Refresh");
        btnRefresh.setBackground(BLUE);
        btnRefresh.setForeground(Color.WHITE);
        btnRefresh.setFocusPainted(false);
        btnRefresh.addActionListener(e -> refresh());

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        right.add(new JLabel("Period:"));
        right.add(cmbMonths);
        right.add(btnRefresh);

        JPanel p = new JPanel(new BorderLayout());
        p.add(title, BorderLayout.WEST);
        p.add(right, BorderLayout.EAST);
        p.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
        return p;
    }

    private JPanel buildBody() {
        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("\uD83D\uDCCA  Overview",         buildOverviewTab());
        tabs.addTab("\uD83D\uDCC5  Monthly Breakdown", buildMonthlyTab());
        tabs.addTab("\uD83D\uDCE6  By Category",       buildCategoryTab());
        tabs.addTab("\uD83C\uDFC6  Top Products",      buildProductTab());
        return wrapInPanel(tabs);
    }

    private JPanel buildOverviewTab() {
        JPanel p = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.BOTH;
        c.insets = new Insets(6, 6, 6, 6);
        c.weighty = 0;

        // KPI cards - row 0
        c.gridy = 0; c.weightx = 0.25;
        c.gridx = 0; p.add(kpiCard("💰 Total Revenue",      lblRevenue,     "all time", BLUE),   c);
        c.gridx = 1; p.add(kpiCard("📦 Cost of Goods",      lblCogs,        "all time", ORANGE), c);
        c.gridx = 2; p.add(kpiCard("📈 Gross Profit",       lblProfit,      "all time", GREEN),  c);
        c.gridx = 3; p.add(kpiCard("📊 Gross Margin",       lblMargin,      "%",        GREEN),  c);

        // KPI cards - row 1
        c.gridy = 1;
        c.gridx = 0; p.add(kpiCard("🏛 Tax Collected",      lblTax,         "all time", BLUE),   c);
        c.gridx = 1; p.add(kpiCard("🏷 Total Discounts",    lblDiscount,    "all time", ORANGE), c);
        c.gridx = 2; p.add(kpiCard("⚠ Outstanding Balance",lblOutstanding, "unpaid",   RED),    c);
        c.gridx = 3; p.add(kpiCard("🛒 Total Orders",       lblOrders,      "all time", BLUE),   c);

        // Chart - row 2
        c.gridy = 2; c.gridx = 0; c.gridwidth = 4;
        c.weighty = 1;
        JFreeChart chart = ChartFactory.createBarChart(
                "Monthly Revenue vs Profit",
                "Month", "Amount (Rs.)",
                chartDataset, PlotOrientation.VERTICAL, true, true, false);
        chart.setBackgroundPaint(Color.WHITE);
        CategoryPlot plot = chart.getCategoryPlot();
        plot.setBackgroundPaint(new Color(250, 250, 250));
        plot.setRangeGridlinePaint(Color.LIGHT_GRAY);
        BarRenderer renderer = (BarRenderer) plot.getRenderer();
        renderer.setBarPainter(new StandardBarPainter());
        renderer.setSeriesPaint(0, BLUE);
        renderer.setSeriesPaint(1, GREEN);
        renderer.setShadowVisible(false);
        ChartPanel cp = new ChartPanel(chart);
        cp.setPopupMenu(null);
        p.add(cp, c);
        return p;
    }

    private JPanel buildMonthlyTab() {
        JTable table = styledTable(monthlyModel,
                new int[]{2, 3, 4, 6, 7, 8});
        table.getColumnModel().getColumn(4).setCellRenderer(profitRenderer());
        return scrollPanel(table, "Monthly revenue, cost of goods and gross profit");
    }

    private JPanel buildCategoryTab() {
        JTable table = styledTable(categoryModel, new int[]{3, 4, 5});
        table.getColumnModel().getColumn(5).setCellRenderer(profitRenderer());
        return scrollPanel(table, "Profit breakdown by hardware category");
    }

    private JPanel buildProductTab() {
        JTable table = styledTable(productModel, new int[]{3, 4});
        table.getColumnModel().getColumn(4).setCellRenderer(profitRenderer());
        return scrollPanel(table, "Top 10 most profitable products (all time)");
    }

    // ============================================ refresh

    public void refresh() {
        int months = switch (cmbMonths.getSelectedIndex()) {
            case 0 -> 3; case 2 -> 12; case 3 -> 24; default -> 6;
        };

        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            double[] kpis;
            List<String[]> monthly, category, products;
            List<String[]> chartRows;

            @Override
            protected Void doInBackground() {
                try {
                    kpis = dao.overallKPIs();
                    monthly = dao.monthlyPL(months);
                    category = dao.categoryPL();
                    products = dao.topProducts();
                    chartRows = dao.monthlyPL(months);
                } catch (Exception ex) {
                    LOG.log(Level.SEVERE, "P&L refresh failed", ex);
                }
                return null;
            }

            @Override
            protected void done() {
                if (kpis != null) {
                    double revenue = kpis[0], cogs = kpis[1], profit = kpis[2];
                    double margin  = revenue > 0 ? profit / revenue * 100 : 0;
                    lblRevenue.setText(String.format("Rs. %,.0f", revenue));
                    lblCogs.setText(String.format("Rs. %,.0f", cogs));
                    lblProfit.setText(String.format("Rs. %,.0f", profit));
                    lblMargin.setText(String.format("%.1f%%", margin));
                    lblTax.setText(String.format("Rs. %,.0f", kpis[3]));
                    lblDiscount.setText(String.format("Rs. %,.0f", kpis[4]));
                    lblOutstanding.setText(String.format("Rs. %,.0f", kpis[5]));
                    lblOrders.setText(String.format("%.0f", kpis[6]));
                    lblProfit.setForeground(profit >= 0 ? GREEN : RED);
                    lblMargin.setForeground(margin >= 20 ? GREEN : margin >= 10 ? ORANGE : RED);
                    lblOutstanding.setForeground(kpis[5] > 0 ? RED : GREEN);
                }

                // Monthly chart
                chartDataset.clear();
                if (chartRows != null) {
                    for (String[] r : chartRows) {
                        try {
                            chartDataset.addValue(
                                    Double.parseDouble(r[2].replace(",","")), "Revenue", r[0]);
                            chartDataset.addValue(
                                    Double.parseDouble(r[4].replace(",","")), "Profit",  r[0]);
                        } catch (NumberFormatException ignored) {}
                    }
                }

                populateTable(monthlyModel,  monthly);
                populateTable(categoryModel, category);
                populateTable(productModel,  products);
            }
        };
        worker.execute();
    }

    // ============================================ helpers

    private static void populateTable(DefaultTableModel m, List<String[]> rows) {
        m.setRowCount(0);
        if (rows != null) rows.forEach(m::addRow);
    }

    private static JTable styledTable(DefaultTableModel m, int[] rightCols) {
        JTable t = new JTable(m);
        t.setRowHeight(26);
        t.getTableHeader().setReorderingAllowed(false);
        DefaultTableCellRenderer right = new DefaultTableCellRenderer();
        right.setHorizontalAlignment(SwingConstants.RIGHT);
        for (int c : rightCols) t.getColumnModel().getColumn(c).setCellRenderer(right);
        return t;
    }

    private static DefaultTableCellRenderer profitRenderer() {
        return new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object v, boolean sel,
                                                           boolean focus, int r, int c) {
                super.getTableCellRendererComponent(t, v, sel, focus, r, c);
                setHorizontalAlignment(SwingConstants.RIGHT);
                if (!sel && v != null) {
                    try {
                        double val = Double.parseDouble(v.toString().replace(",","").replace("%",""));
                        setForeground(val >= 0
                                ? new Color(39, 174, 96) : new Color(192, 57, 43));
                    } catch (NumberFormatException ignored) {}
                }
                return this;
            }
        };
    }

    private static JPanel kpiCard(String title, JLabel number, String sub, Color accent) {
        JPanel card = new JPanel(new BorderLayout(0, 4));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(accent, 2, true),
                BorderFactory.createEmptyBorder(10, 14, 10, 14)));
        card.setBackground(lighten(accent));
        JLabel t = new JLabel(title);
        t.setFont(new Font("Segoe UI", Font.BOLD, 12));
        t.setForeground(accent.darker());
        number.setForeground(accent.darker().darker());
        number.setHorizontalAlignment(SwingConstants.LEFT);
        JLabel s = new JLabel(sub);
        s.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        s.setForeground(accent.darker());
        card.add(t, BorderLayout.NORTH);
        card.add(number, BorderLayout.CENTER);
        card.add(s, BorderLayout.SOUTH);
        return card;
    }

    private static JLabel kpiLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("Segoe UI", Font.BOLD, 22));
        return l;
    }

    private static JPanel scrollPanel(JTable table, String hint) {
        JPanel p = new JPanel(new BorderLayout(0, 4));
        JLabel h = new JLabel("  \uD83D\uDCA1 " + hint);
        h.setForeground(Color.GRAY);
        h.setFont(new Font("Segoe UI", Font.ITALIC, 11));
        p.add(h, BorderLayout.NORTH);
        p.add(new JScrollPane(table), BorderLayout.CENTER);
        return p;
    }

    private static JPanel wrapInPanel(JComponent c) {
        JPanel p = new JPanel(new BorderLayout());
        p.add(c);
        return p;
    }

    private static Color lighten(Color c) {
        int r = Math.min(255, c.getRed()   + (255 - c.getRed())   * 7 / 10);
        int g = Math.min(255, c.getGreen() + (255 - c.getGreen()) * 7 / 10);
        int b = Math.min(255, c.getBlue()  + (255 - c.getBlue())  * 7 / 10);
        return new Color(r, g, b);
    }
}