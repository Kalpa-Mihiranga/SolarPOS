package com.mycompany.solarpos.ui;

import com.mycompany.solarpos.dao.CustomerAnalyticsDAO;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PiePlot;
import org.jfree.data.general.DefaultPieDataset;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.*;
import javax.swing.table.*;

public class CustomerAnalyticsPanel extends JPanel {

    private static final Logger LOG = Logger.getLogger(CustomerAnalyticsPanel.class.getName());
    private static final Color BLUE   = new Color(0, 120, 212);
    private static final Color GREEN  = new Color(39, 174, 96);
    private static final Color ORANGE = new Color(230, 126, 34);
    private static final Color PURPLE = new Color(142, 68, 173);

    private final CustomerAnalyticsDAO dao = new CustomerAnalyticsDAO();

    private final JLabel lblTotal    = kpi("-");
    private final JLabel lblActive   = kpi("-");
    private final JLabel lblAvgOrder = kpi("-");
    private final JLabel lblPerCust  = kpi("-");

    private final DefaultTableModel spenderModel = new DefaultTableModel(
        new String[]{"Customer","Type","Phone","Orders",
                     "Total Spend","Avg Order","kW Installed","Outstanding"}, 0) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };
    private final DefaultTableModel repeatModel = new DefaultTableModel(
        new String[]{"Customer","Type","Phone","Orders",
                     "First Order","Last Order","Days Between","Lifetime Value"}, 0) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };
    private final DefaultTableModel typeModel = new DefaultTableModel(
        new String[]{"Type","Customers","Orders","Revenue","Avg Order"}, 0) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };

    private final DefaultPieDataset<String> pieDataset = new DefaultPieDataset<>();

    public CustomerAnalyticsPanel() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        JLabel title = new JLabel("\uD83D\uDC65  Customer Analytics");
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
        tabs.addTab("\uD83D\uDCCA  Overview",      buildOverview());
        tabs.addTab("\uD83C\uDFC6  Top Spenders",  buildSpendersTab());
        tabs.addTab("\uD83D\uDD04  Repeat Buyers", buildRepeatTab());
        JPanel p = new JPanel(new BorderLayout());
        p.add(tabs);
        return p;
    }

    private JPanel buildOverview() {
        JPanel p = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.BOTH;
        c.insets = new Insets(6, 6, 6, 6);

        // KPI cards row
        c.gridy = 0; c.weightx = 0.25; c.weighty = 0;
        c.gridx = 0; p.add(card("\uD83D\uDC65 Total Customers",   lblTotal,    "registered", BLUE),   c);
        c.gridx = 1; p.add(card("\uD83D\uDED2 Active Buyers",     lblActive,   "with orders", GREEN),  c);
        c.gridx = 2; p.add(card("\uD83D\uDCB0 Avg Order Value",   lblAvgOrder, "Rs.",         ORANGE), c);
        c.gridx = 3; p.add(card("\uD83D\uDCC8 Revenue / Customer",lblPerCust,  "Rs.",         PURPLE), c);

        // Pie chart + type table
        c.gridy = 1; c.weighty = 1;
        c.gridx = 0; c.gridwidth = 2;
        JFreeChart pie = ChartFactory.createPieChart(
                "Revenue by Customer Type", pieDataset, true, true, false);
        ((PiePlot<?>) pie.getPlot()).setSectionPaint("RESIDENTIAL", GREEN);
        ((PiePlot<?>) pie.getPlot()).setSectionPaint("COMMERCIAL",  BLUE);
        pie.setBackgroundPaint(Color.WHITE);
        ChartPanel cp = new ChartPanel(pie);
        cp.setPopupMenu(null);
        p.add(cp, c);

        c.gridx = 2; c.gridwidth = 2;
        JTable t = new JTable(typeModel);
        t.setRowHeight(26);
        p.add(new JScrollPane(t), c);
        return p;
    }

    private JPanel buildSpendersTab() {
        JTable t = new JTable(spenderModel);
        t.setRowHeight(26);
        rightAlign(t, new int[]{4, 5, 6, 7});
        t.getColumnModel().getColumn(7).setCellRenderer(balanceRenderer());
        JPanel p = new JPanel(new BorderLayout(0, 4));
        JLabel h = new JLabel("  \uD83C\uDFC6 Top 10 customers ranked by total spend");
        h.setForeground(Color.GRAY);
        p.add(h, BorderLayout.NORTH);
        p.add(new JScrollPane(t), BorderLayout.CENTER);
        return p;
    }

    private JPanel buildRepeatTab() {
        JTable t = new JTable(repeatModel);
        t.setRowHeight(26);
        rightAlign(t, new int[]{7});
        JPanel p = new JPanel(new BorderLayout(0, 4));
        JLabel h = new JLabel(
                "  \uD83D\uDD04 Customers with more than one order, ranked by order count");
        h.setForeground(Color.GRAY);
        p.add(h, BorderLayout.NORTH);
        p.add(new JScrollPane(t), BorderLayout.CENTER);
        return p;
    }

    private void load() {
        SwingWorker<Void, Void> w = new SwingWorker<>() {
            double[] kpis;
            List<String[]> spenders, repeat, types;

            @Override protected Void doInBackground() {
                try {
                    kpis     = dao.customerKPIs();
                    spenders = dao.topSpenders();
                    repeat   = dao.repeatBuyers();
                    types    = dao.typeBreakdown();
                } catch (Exception ex) {
                    LOG.log(Level.SEVERE, "Customer analytics failed", ex);
                }
                return null;
            }

            @Override protected void done() {
                if (kpis != null) {
                    lblTotal.setText(String.format("%.0f", kpis[0]));
                    lblActive.setText(String.format("%.0f", kpis[1]));
                    lblAvgOrder.setText(String.format("%,.0f", kpis[3]));
                    lblPerCust.setText(String.format("%,.0f", kpis[4]));
                }
                fill(spenderModel, spenders);
                fill(repeatModel,  repeat);

                pieDataset.clear();
                if (types != null) {
                    for (String[] r : types) {
                        try {
                            pieDataset.setValue(r[0],
                                    Double.parseDouble(r[3].replace(",", "")));
                        } catch (NumberFormatException ignored) {}
                    }
                }
                fill(typeModel, types);
            }
        };
        w.execute();
    }

    private static void fill(DefaultTableModel m, List<String[]> rows) {
        m.setRowCount(0);
        if (rows != null) rows.forEach(m::addRow);
    }

    private static void rightAlign(JTable t, int[] cols) {
        DefaultTableCellRenderer r = new DefaultTableCellRenderer();
        r.setHorizontalAlignment(SwingConstants.RIGHT);
        for (int c : cols) t.getColumnModel().getColumn(c).setCellRenderer(r);
    }

    private static DefaultTableCellRenderer balanceRenderer() {
        return new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object v, boolean sel,
                                                           boolean focus, int r, int c) {
                super.getTableCellRendererComponent(t, v, sel, focus, r, c);
                setHorizontalAlignment(SwingConstants.RIGHT);
                if (!sel && v != null) {
                    try {
                        double val = Double.parseDouble(v.toString().replace(",", ""));
                        setForeground(val > 0.01
                                ? new Color(192, 57, 43) : new Color(39, 174, 96));
                    } catch (NumberFormatException ignored) {}
                }
                return this;
            }
        };
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
        l.setFont(new Font("Segoe UI", Font.BOLD, 26));
        return l;
    }

    private static Color lighten(Color c) {
        return new Color(
            Math.min(255, c.getRed()   + (255 - c.getRed())   * 7 / 10),
            Math.min(255, c.getGreen() + (255 - c.getGreen()) * 7 / 10),
            Math.min(255, c.getBlue()  + (255 - c.getBlue())  * 7 / 10));
    }
}