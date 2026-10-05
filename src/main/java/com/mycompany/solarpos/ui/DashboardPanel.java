package com.mycompany.solarpos.ui;

import com.mycompany.solarpos.dao.DashboardDAO;
import com.mycompany.solarpos.util.Session;
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
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

public class DashboardPanel extends JPanel {

    private static final Logger LOG = Logger.getLogger(DashboardPanel.class.getName());
    private static final Color ACCENT_BLUE   = new Color(0, 120, 212);
    private static final Color ACCENT_GREEN  = new Color(39, 174, 96);
    private static final Color ACCENT_ORANGE = new Color(230, 126, 34);
    private static final Color ACCENT_RED    = new Color(192, 57, 43);

    private final DashboardDAO dao = new DashboardDAO();

    // stat cards
    private final JLabel lblKw       = bigNumber("-");
    private final JLabel lblRevenue  = bigNumber("-");
    private final JLabel lblOrders   = bigNumber("-");
    private final JLabel lblLowStock = bigNumber("-");

    // chart + tables
    private DefaultCategoryDataset chartDataset = new DefaultCategoryDataset();
    private ChartPanel chartPanel;
    private final DefaultTableModel warrantyModel = new DefaultTableModel(
            new String[]{"Customer", "Item", "Expires", "Days left"}, 0) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };
    private final DefaultTableModel orderModel = new DefaultTableModel(
            new String[]{"Order", "Customer", "Total", "kW", "Date"}, 0) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };

    public DashboardPanel() {
        setLayout(new BorderLayout(12, 12));
        setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        add(buildHeader(), BorderLayout.NORTH);
        add(buildBody(), BorderLayout.CENTER);

        addComponentListener(new ComponentAdapter() {
            @Override public void componentShown(ComponentEvent e) { refresh(); }
        });
        refresh();
    }

    // ================================================================= layout

    private JPanel buildHeader() {
        JLabel title = new JLabel("\u2600 Solar POS Dashboard");
        title.setFont(new Font("Segoe UI", Font.BOLD, 26));

        JButton btnRefresh = iconButton("\u21BA  Refresh");
        btnRefresh.setBackground(ACCENT_BLUE);
        btnRefresh.setForeground(Color.WHITE);
        btnRefresh.addActionListener(e -> refresh());

        JLabel lblUser = new JLabel(
                "\uD83D\uDC64  " + Session.getCurrentUser().getUsername()
                + "  (" + Session.getCurrentUser().getRole() + ")");
        lblUser.setForeground(Color.GRAY);

        JLabel lblDate = new JLabel(
                "\uD83D\uDCC5  " + LocalDate.now().format(DateTimeFormatter.ofPattern("MMMM d, yyyy")));
        lblDate.setForeground(Color.GRAY);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        right.add(lblDate);
        right.add(lblUser);
        right.add(btnRefresh);

        JPanel p = new JPanel(new BorderLayout());
        p.add(title, BorderLayout.WEST);
        p.add(right, BorderLayout.EAST);
        p.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
        return p;
    }

    private JPanel buildBody() {
        JPanel p = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.BOTH;
        c.insets = new Insets(6, 6, 6, 6);

        // Row 0: four stat cards
        c.gridy = 0; c.weightx = 0.25; c.weighty = 0;
        c.gridx = 0; p.add(statCard("\u26A1 Peak Power Sold",  lblKw,       "kW this month",  ACCENT_BLUE),   c);
        c.gridx = 1; p.add(statCard("\uD83D\uDCB0 Gross Revenue",   lblRevenue,  "Rs. this month", ACCENT_GREEN),  c);
        c.gridx = 2; p.add(statCard("\uD83D\uDCCB Orders",          lblOrders,   "this month",     ACCENT_ORANGE), c);
        c.gridx = 3; p.add(statCard("\u26A0 Low Stock Items",  lblLowStock, "5 units or less", ACCENT_RED),   c);

        // Row 1: bar chart (left) + warranty table (right)
        c.gridy = 1; c.weighty = 0.45;
        c.gridx = 0; c.gridwidth = 2; p.add(buildChartPanel(), c);
        c.gridx = 2; c.gridwidth = 2; p.add(buildWarrantyPanel(), c);

        // Row 2: recent orders
        c.gridy = 2; c.weighty = 0.35;
        c.gridx = 0; c.gridwidth = 4; p.add(buildRecentOrdersPanel(), c);

        return p;
    }

    private JPanel statCard(String title, JLabel number, String sub, Color accent) {
        JPanel card = new JPanel(new BorderLayout(0, 6));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(accent, 2, true),
                BorderFactory.createEmptyBorder(14, 18, 14, 18)));
        card.setBackground(lighten(accent));

        JLabel t = new JLabel(title);
        t.setFont(new Font("Segoe UI", Font.BOLD, 13));
        t.setForeground(accent.darker());

        number.setForeground(accent.darker().darker());
        number.setHorizontalAlignment(SwingConstants.LEFT);

        JLabel s = new JLabel(sub);
        s.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        s.setForeground(accent.darker());

        card.add(t, BorderLayout.NORTH);
        card.add(number, BorderLayout.CENTER);
        card.add(s, BorderLayout.SOUTH);
        return card;
    }

    private JPanel buildChartPanel() {
        JFreeChart chart = ChartFactory.createBarChart(
                "\uD83D\uDCC8 Top Inverter Brands (all time)",
                "Brand", "Units Sold",
                chartDataset, PlotOrientation.VERTICAL, false, true, false);

        chart.setBackgroundPaint(Color.WHITE);
        CategoryPlot plot = chart.getCategoryPlot();
        plot.setBackgroundPaint(new Color(250, 250, 250));
        plot.setRangeGridlinePaint(Color.LIGHT_GRAY);
        plot.getRangeAxis().setStandardTickUnits(
                org.jfree.chart.axis.NumberAxis.createIntegerTickUnits());

        BarRenderer renderer = (BarRenderer) plot.getRenderer();
        renderer.setBarPainter(new StandardBarPainter());
        renderer.setSeriesPaint(0, ACCENT_BLUE);
        renderer.setShadowVisible(false);
        renderer.setDefaultToolTipGenerator(
                (dataset, r, c) -> dataset.getColumnKey(c) + ": " + dataset.getValue(r, c) + " units");

        chartPanel = new ChartPanel(chart);
        chartPanel.setPopupMenu(null);

        JPanel p = new JPanel(new BorderLayout());
        p.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(ACCENT_BLUE, 1, true),
                "\uD83D\uDCC8 Inverter Brand Performance"));
        p.add(chartPanel, BorderLayout.CENTER);
        return p;
    }

    private JPanel buildWarrantyPanel() {
        JTable table = new JTable(warrantyModel);
        table.setRowHeight(26);
        table.getTableHeader().setReorderingAllowed(false);

        // Colour code days-left column
        DefaultTableCellRenderer daysRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object v, boolean sel,
                                                           boolean focus, int r, int c) {
                super.getTableCellRendererComponent(t, v, sel, focus, r, c);
                if (!sel && v != null) {
                    String txt = v.toString();
                    int days = 0;
                    try { days = Integer.parseInt(txt.replace(" days", "").trim()); }
                    catch (NumberFormatException ignored) { }
                    if (days <= 14) setForeground(ACCENT_RED);
                    else if (days <= 30) setForeground(ACCENT_ORANGE);
                    else setForeground(new Color(39, 127, 49));
                }
                return this;
            }
        };
        table.getColumnModel().getColumn(3).setCellRenderer(daysRenderer);

        JPanel p = new JPanel(new BorderLayout());
        p.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(ACCENT_ORANGE, 1, true),
                "\u23F0 Warranties Expiring in 60 Days"));
        JScrollPane scroll = new JScrollPane(table);
        scroll.setPreferredSize(new Dimension(0, 0));
        p.add(scroll, BorderLayout.CENTER);

        JLabel hint = new JLabel("  \uD83D\uDD34 red = ≤14 days   \uD83D\uDFE0 orange = ≤30 days   \uD83D\uDFE2 green = >30 days");
        hint.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        hint.setForeground(Color.GRAY);
        p.add(hint, BorderLayout.SOUTH);
        return p;
    }

    private JPanel buildRecentOrdersPanel() {
        JTable table = new JTable(orderModel);
        table.setRowHeight(26);
        table.getTableHeader().setReorderingAllowed(false);
        table.getColumnModel().getColumn(0).setPreferredWidth(60);
        table.getColumnModel().getColumn(1).setPreferredWidth(200);
        table.getColumnModel().getColumn(2).setPreferredWidth(180);

        DefaultTableCellRenderer right = new DefaultTableCellRenderer();
        right.setHorizontalAlignment(SwingConstants.RIGHT);
        table.getColumnModel().getColumn(2).setCellRenderer(right);
        table.getColumnModel().getColumn(3).setCellRenderer(right);

        JPanel p = new JPanel(new BorderLayout());
        p.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(ACCENT_GREEN, 1, true),
                "\uD83D\uDDD3 Recent Orders"));
        p.add(new JScrollPane(table), BorderLayout.CENTER);
        return p;
    }

    // ================================================================= refresh

    public void refresh() {
        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            double kw, rev;
            int orders, lowStock;
            Map<String, Integer> brands, allBrands;
            List<String[]> warranties, recentOrders;

            @Override
            protected Void doInBackground() {
                try {
                    kw = dao.totalKwThisMonth();
                    rev = dao.grossRevenueThisMonth();
                    orders = dao.ordersThisMonth();
                    lowStock = dao.lowStockCount();
                    brands = dao.topInverterBrands();
                    allBrands = dao.topInverterBrandsAllTime();
                    warranties = dao.expiringWarranties();
                    recentOrders = dao.recentOrders();
                } catch (Exception ex) {
                    LOG.log(Level.SEVERE, "Dashboard refresh failed", ex);
                }
                return null;
            }

            @Override
            protected void done() {
                lblKw.setText(String.format(Locale.US, "%.2f", kw));
                lblRevenue.setText(String.format(Locale.US, "%,.0f", rev));
                lblOrders.setText(String.valueOf(orders));

                lblLowStock.setText(String.valueOf(lowStock));
                lblLowStock.setForeground(lowStock > 0 ? ACCENT_RED : ACCENT_GREEN);

                // Chart: monthly data if available, else all-time
                Map<String, Integer> display = (brands != null && !brands.isEmpty()) ? brands : allBrands;
                chartDataset.clear();
                if (display != null) {
                    display.forEach((b, u) -> chartDataset.addValue(u, "Units", b));
                }

                warrantyModel.setRowCount(0);
                if (warranties != null) {
                    warranties.forEach(warrantyModel::addRow);
                }

                orderModel.setRowCount(0);
                if (recentOrders != null) {
                    recentOrders.forEach(orderModel::addRow);
                }
            }
        };
        worker.execute();
    }

    // ================================================================= helpers

    private static JLabel bigNumber(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("Segoe UI", Font.BOLD, 34));
        return l;
    }

    private static JButton iconButton(String text) {
        JButton b = new JButton(text);
        b.setFocusPainted(false);
        return b;
    }

    private static Color lighten(Color c) {
        int r = Math.min(255, c.getRed()   + (255 - c.getRed())   * 7 / 10);
        int g = Math.min(255, c.getGreen() + (255 - c.getGreen()) * 7 / 10);
        int b = Math.min(255, c.getBlue()  + (255 - c.getBlue())  * 7 / 10);
        return new Color(r, g, b);
    }
}