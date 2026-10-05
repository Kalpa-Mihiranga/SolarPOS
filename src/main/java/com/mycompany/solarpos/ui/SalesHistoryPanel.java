package com.mycompany.solarpos.ui;

import com.mycompany.solarpos.dao.SalesHistoryDAO;
import com.mycompany.solarpos.db.DBConnection;
import com.mycompany.solarpos.util.ExportUtil;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.FlowLayout;
import java.awt.Font;
import java.io.InputStream;
import java.sql.Connection;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;

import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.view.JasperViewer;

public class SalesHistoryPanel extends JPanel {

    private static final Logger LOG = Logger.getLogger(SalesHistoryPanel.class.getName());
    private static final Color ACCENT = new Color(0, 120, 212);

    private final SalesHistoryDAO dao = new SalesHistoryDAO();

    private final JTextField txtFrom    = new JTextField(10);
    private final JTextField txtTo      = new JTextField(10);
    private final JTextField txtSearch  = new JTextField(16);
    private final JLabel     lblSummary = new JLabel(" ");

    private final String[] COLS = {
        "Order #", "Customer", "Type", "Cashier", "Date",
        "Subtotal", "Discount", "Warranty", "Tax",
        "Total", "Down Paid", "Balance", "kW", "Phase"
    };

    private final DefaultTableModel model = new DefaultTableModel(COLS, 0) {
        @Override 
        public boolean isCellEditable(int r, int c) { 
            return false; 
        }
    };

    private final JTable table = new JTable(model);
    private final TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(model);

    private List<String[]> allRows;

    public SalesHistoryPanel() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        JLabel title = new JLabel("\uD83D\uDDC3  Sales History");
        title.setFont(new Font("Segoe UI", Font.BOLD, 24));

        JPanel top = new JPanel(new BorderLayout(0, 8));
        top.add(title, BorderLayout.NORTH);
        top.add(buildFilter(), BorderLayout.CENTER);
        top.add(lblSummary, BorderLayout.SOUTH);
        
        add(top, BorderLayout.NORTH);
        add(buildTable(), BorderLayout.CENTER);

        // Default date range: last 90 days
        txtTo.setText(LocalDate.now().toString());
        txtFrom.setText(LocalDate.now().minusDays(90).toString());
        loadData();
    }

    // -------------------------------------------------- UI Layout

    private JPanel buildFilter() {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 4));
        p.setBorder(BorderFactory.createTitledBorder("Filter"));

        JButton btnLoad   = btn("Search", new Color(0, 120, 212));
        JButton btnReset  = btn("Reset", Color.GRAY);
        JButton btnAll    = btn("All time", new Color(127, 140, 141));
        JButton btnCSV    = btn("⬇ Export CSV", new Color(39, 174, 96));
        JButton btnJasper = btn("📄 Print Report", new Color(142, 68, 173));

        btnCSV.addActionListener(e -> ExportUtil.exportToCSV(table, "SalesHistory"));
        btnJasper.addActionListener(e -> openJasperReport());

        btnLoad.addActionListener(e -> loadData());
        btnReset.addActionListener(e -> {
            txtFrom.setText(LocalDate.now().minusDays(90).toString());
            txtTo.setText(LocalDate.now().toString());
            txtSearch.setText("");
            loadData();
        });
        btnAll.addActionListener(e -> {
            txtFrom.setText("");
            txtTo.setText("");
            loadData();
        });

        p.add(new JLabel("From (yyyy-MM-dd):"));
        p.add(txtFrom);
        p.add(new JLabel("To:"));
        p.add(txtTo);
        p.add(new JLabel("Customer name:"));
        p.add(txtSearch);
        p.add(btnLoad);
        p.add(btnReset);
        p.add(btnAll);
        p.add(btnCSV);
        p.add(btnJasper);

        return p;
    }

    private JPanel buildTable() {
        table.setRowSorter(sorter);
        table.setRowHeight(26);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getTableHeader().setReorderingAllowed(false);

        // Right-align numeric columns
        DefaultTableCellRenderer right = new DefaultTableCellRenderer();
        right.setHorizontalAlignment(SwingConstants.RIGHT);
        for (int c : new int[]{5, 6, 7, 8, 9, 10, 11, 12}) {
            table.getColumnModel().getColumn(c).setCellRenderer(right);
        }

        // Color balance-due column red if > 0
        table.getColumnModel().getColumn(11).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object v, boolean sel,
                                                           boolean focus, int r, int c) {
                super.getTableCellRendererComponent(t, v, sel, focus, r, c);
                setHorizontalAlignment(SwingConstants.RIGHT);
                if (!sel && v != null) {
                    double val = 0.0;
                    try { 
                        val = Double.parseDouble(v.toString().replace(",", "").trim()); 
                    } catch (NumberFormatException ignored) {}
                    
                    setForeground(val > 0.01 ? new Color(192, 57, 43) : new Color(39, 174, 96));
                }
                return this;
            }
        });

        table.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2) {
                    openDetail();
                }
            }
        });

        JPanel p = new JPanel(new BorderLayout(0, 4));
        JLabel hint = new JLabel("  \uD83D\uDCA1 Double-click any order to see its line items and profit breakdown");
        hint.setForeground(Color.GRAY);
        hint.setFont(new Font("Segoe UI", Font.ITALIC, 11));
        p.add(hint, BorderLayout.NORTH);
        p.add(new JScrollPane(table), BorderLayout.CENTER);
        return p;
    }

    // -------------------------------------------------- Data & Actions

    private void loadData() {
        try {
            allRows = dao.findOrders(
                    txtFrom.getText().trim(),
                    txtTo.getText().trim(),
                    txtSearch.getText().trim());
            model.setRowCount(0);
            for (String[] r : allRows) { 
                model.addRow(r); 
            }

            double[] totals = dao.periodTotals(
                    txtFrom.getText().trim(),
                    txtTo.getText().trim());

            lblSummary.setText(String.format(
                    "  \uD83D\uDCCA  %d orders  |  Revenue: Rs. %,.2f  |  "
                  + "Discounts: Rs. %,.2f  |  Tax collected: Rs. %,.2f  |  "
                  + "Outstanding balance: Rs. %,.2f  |  Total kW sold: %.2f",
                    (int) totals[0], totals[1], totals[2], totals[3], totals[4], totals[5]));
            lblSummary.setFont(new Font("Segoe UI", Font.BOLD, 12));
            lblSummary.setForeground(ACCENT);

        } catch (Exception ex) {
            LOG.log(Level.SEVERE, "Load sales history failed", ex);
            JOptionPane.showMessageDialog(this,
                    "Could not load sales: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void openDetail() {
        int row = table.getSelectedRow();
        if (row < 0) return;
        int orderId = Integer.parseInt(
                model.getValueAt(table.convertRowIndexToModel(row), 0).toString());
        new OrderDetailDialog(
                SwingUtilities.getWindowAncestor(this), dao, orderId).setVisible(true);
    }

    private void openJasperReport() {
        final String from = txtFrom.getText().trim();
        final String to = txtTo.getText().trim();
        final String custSearch = txtSearch.getText().trim();

        // 1. Verify report template existence
        InputStream is = getClass().getResourceAsStream("/reports/sales_history.jrxml");
        if (is == null) {
            JOptionPane.showMessageDialog(this,
                    "sales_history.jrxml not found.\n"
                  + "Make sure it exists in src/main/resources/reports/sales_history.jrxml",
                    "Report Missing", JOptionPane.ERROR_MESSAGE);
            return;
        }

        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

        // 2. Offload report compilation & generation to background thread
        SwingWorker<JasperPrint, Void> worker = new SwingWorker<>() {
            @Override
            protected JasperPrint doInBackground() throws Exception {
                JasperReport jr = JasperCompileManager.compileReport(is);

                Map<String, Object> params = new HashMap<>();
                params.put("P_FROM_DATE", from.isEmpty() ? null : from);
                params.put("P_TO_DATE", to.isEmpty() ? null : to);
                params.put("P_CUSTOMER", custSearch.isEmpty() ? null : "%" + custSearch + "%");
                params.put("P_REPORT_TITLE", "Sales History Report");

                Connection conn = DBConnection.getInstance().getConnection();
                return JasperFillManager.fillReport(jr, params, conn);
            }

            @Override
            protected void done() {
                setCursor(Cursor.getDefaultCursor());
                try {
                    JasperPrint jp = get();
                    if (jp == null || jp.getPages().isEmpty()) {
                        JOptionPane.showMessageDialog(SalesHistoryPanel.this,
                                "No data found for the selected filter criteria.",
                                "No Data", JOptionPane.INFORMATION_MESSAGE);
                        return;
                    }
                    JasperViewer viewer = new JasperViewer(jp, false);
                    viewer.setTitle("Solar POS - Sales History Report");
                    viewer.setLocationRelativeTo(SalesHistoryPanel.this);
                    viewer.setVisible(true);
                } catch (Exception ex) {
                    LOG.log(Level.SEVERE, "Jasper report execution failed", ex);
                    JOptionPane.showMessageDialog(SalesHistoryPanel.this,
                            "Failed to generate report:\n" + ex.getMessage(),
                            "Report Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        };

        worker.execute();
    }

    private static JButton btn(String text, Color bg) {
        JButton b = new JButton(text);
        b.setBackground(bg);
        b.setForeground(Color.WHITE);
        b.setFocusPainted(false);
        return b;
    }
}