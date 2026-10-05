package com.mycompany.solarpos.ui;

import com.mycompany.solarpos.dao.BalancePaymentDAO;
import com.mycompany.solarpos.util.Session;
import com.mycompany.solarpos.util.ValidationException;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;

public class BalanceCollectionPanel extends JPanel {

    private static final Logger LOG = Logger.getLogger(BalanceCollectionPanel.class.getName());
    private static final Color BLUE  = new Color(0, 120, 212);
    private static final Color GREEN = new Color(39, 174, 96);
    private static final Color RED   = new Color(192, 57, 43);

    private final BalancePaymentDAO dao = new BalancePaymentDAO();

    private final JTextField txtSearch  = new JTextField(16);
    private final JLabel     lblBalance = new JLabel("Select an order to collect payment");
    private final JTextField txtAmount  = new JTextField(12);
    private final JTextField txtNotes   = new JTextField(20);
    private final JButton    btnPay     = new JButton("Record Payment");
    private int selectedOrderId = -1;

    private final DefaultTableModel orderModel = new DefaultTableModel(
        new String[]{"Order #","Customer","Phone","Date",
                     "Total","Paid","Balance Due"}, 0) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };
    private final DefaultTableModel histModel = new DefaultTableModel(
        new String[]{"Payment #","Amount","Date","Collected By","Notes"}, 0) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };

    private final JTable orderTable = new JTable(orderModel);
    private List<String[]> orderRows;

    public BalanceCollectionPanel() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        JLabel title = new JLabel("\uD83D\uDCB3  Balance Collection");
        title.setFont(new Font("Segoe UI", Font.BOLD, 24));
        add(title, BorderLayout.NORTH);
        add(buildBody(), BorderLayout.CENTER);

        addComponentListener(new ComponentAdapter() {
            @Override public void componentShown(ComponentEvent e) { loadOrders(); }
        });
        loadOrders();
    }

    private JPanel buildBody() {
        JPanel p = new JPanel(new BorderLayout(10, 10));
        p.add(buildOrderPanel(),  BorderLayout.CENTER);
        p.add(buildPaymentPanel(), BorderLayout.EAST);
        return p;
    }

    private JPanel buildOrderPanel() {
        // Search bar
        JPanel search = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        search.add(new JLabel("Search customer:"));
        search.add(txtSearch);
        JButton btnSearch = new JButton("Search");
        btnSearch.setBackground(BLUE);
        btnSearch.setForeground(Color.WHITE);
        btnSearch.addActionListener(e -> loadOrders());
        JButton btnAll = new JButton("Show all");
        btnAll.addActionListener(e -> {
            txtSearch.setText("");
            loadOrders();
        });
        search.add(btnSearch);
        search.add(btnAll);

        // Table
        orderTable.setRowHeight(26);
        orderTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        orderTable.getTableHeader().setReorderingAllowed(false);
        rightAlign(orderTable, new int[]{4, 5, 6});

        orderTable.getColumnModel().getColumn(6).setCellRenderer(
                new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object v, boolean sel,
                                                           boolean focus, int r, int c) {
                super.getTableCellRendererComponent(t, v, sel, focus, r, c);
                setHorizontalAlignment(SwingConstants.RIGHT);
                if (!sel) setForeground(RED);
                return this;
            }
        });

        orderTable.getSelectionModel().addListSelectionListener(
                e -> { if (!e.getValueIsAdjusting()) onOrderSelected(); });

        JPanel p = new JPanel(new BorderLayout(0, 6));
        p.setBorder(BorderFactory.createTitledBorder(
                "\uD83D\uDCCB Outstanding orders (balance > 0)"));
        p.add(search, BorderLayout.NORTH);
        p.add(new JScrollPane(orderTable), BorderLayout.CENTER);

        // Payment history sub-panel
        JTable histTable = new JTable(histModel);
        histTable.setRowHeight(24);
        rightAlign(histTable, new int[]{1});
        JPanel hist = new JPanel(new BorderLayout());
        hist.setBorder(BorderFactory.createTitledBorder("Payment history for selected order"));
        hist.setPreferredSize(new Dimension(0, 160));
        hist.add(new JScrollPane(histTable), BorderLayout.CENTER);
        p.add(hist, BorderLayout.SOUTH);
        return p;
    }

    private JPanel buildPaymentPanel() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBorder(BorderFactory.createTitledBorder("Collect payment"));
        p.setPreferredSize(new Dimension(260, 0));

        GridBagConstraints c = new GridBagConstraints();
        c.insets  = new Insets(6, 8, 6, 8);
        c.anchor  = GridBagConstraints.WEST;
        c.fill    = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;

        lblBalance.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblBalance.setForeground(RED);

        btnPay.setBackground(GREEN);
        btnPay.setForeground(Color.WHITE);
        btnPay.setFocusPainted(false);
        btnPay.setEnabled(false);
        btnPay.addActionListener(e -> collectPayment());

        JButton btnFull = new JButton("Pay full balance");
        btnFull.addActionListener(e -> {
            if (selectedOrderId > 0 && orderRows != null) {
                for (String[] r : orderRows) {
                    if (Integer.parseInt(r[0]) == selectedOrderId) {
                        txtAmount.setText(r[6].replace(",", ""));
                        break;
                    }
                }
            }
        });

        c.gridy = 0; p.add(new JLabel("Selected order balance:"), c);
        c.gridy = 1; p.add(lblBalance, c);
        c.gridy = 2; p.add(new JSeparator(), c);
        c.gridy = 3; p.add(new JLabel("Payment amount (Rs.):"), c);
        c.gridy = 4; p.add(txtAmount, c);
        c.gridy = 5; p.add(btnFull, c);
        c.gridy = 6; p.add(new JLabel("Notes (optional):"), c);
        c.gridy = 7; p.add(txtNotes, c);
        c.gridy = 8; c.insets = new Insets(14, 8, 6, 8);
        p.add(btnPay, c);
        c.gridy = 9; c.weighty = 1;
        p.add(Box.createGlue(), c);
        return p;
    }

    private void loadOrders() {
        try {
            orderRows = dao.findOutstandingOrders(txtSearch.getText().trim());
            orderModel.setRowCount(0);
            orderRows.forEach(orderModel::addRow);
            selectedOrderId = -1;
            lblBalance.setText("Select an order");
            btnPay.setEnabled(false);
            histModel.setRowCount(0);
        } catch (Exception ex) {
            LOG.log(Level.SEVERE, "Load orders failed", ex);
            error("Could not load orders: " + ex.getMessage());
        }
    }

    private void onOrderSelected() {
        int row = orderTable.getSelectedRow();
        if (row < 0) return;
        String[] r = orderRows.get(row);
        selectedOrderId = Integer.parseInt(r[0]);
        lblBalance.setText("Rs. " + r[6] + " outstanding");
        txtAmount.setText(r[6].replace(",", ""));
        btnPay.setEnabled(true);
        loadHistory();
    }

    private void loadHistory() {
        try {
            List<String[]> hist = dao.findPaymentsForOrder(selectedOrderId);
            histModel.setRowCount(0);
            hist.forEach(histModel::addRow);
        } catch (Exception ex) {
            LOG.log(Level.SEVERE, "Load payment history failed", ex);
        }
    }

    private void collectPayment() {
        if (selectedOrderId < 0) { warn("Select an order first."); return; }
        double amount;
        try {
            amount = Double.parseDouble(txtAmount.getText().replace(",", "").trim());
        } catch (NumberFormatException e) {
            warn("Enter a valid payment amount.");
            return;
        }
        int ok = JOptionPane.showConfirmDialog(this,
                String.format("Record payment of Rs. %,.2f for order #%d?",
                        amount, selectedOrderId),
                "Confirm payment", JOptionPane.YES_NO_OPTION);
        if (ok != JOptionPane.YES_OPTION) return;
        try {
            dao.recordPayment(selectedOrderId, amount,
                    Session.getCurrentUser().getId(), txtNotes.getText().trim());
            info(String.format("Payment of Rs. %,.2f recorded.", amount));
            txtAmount.setText("");
            txtNotes.setText("");
            loadOrders();
        } catch (ValidationException ex) {
            warn(ex.getMessage());
        } catch (Exception ex) {
            LOG.log(Level.SEVERE, "Collect payment failed", ex);
            error("Could not record payment: " + ex.getMessage());
        }
    }

    private static void rightAlign(JTable t, int[] cols) {
        DefaultTableCellRenderer r = new DefaultTableCellRenderer();
        r.setHorizontalAlignment(SwingConstants.RIGHT);
        for (int c : cols) t.getColumnModel().getColumn(c).setCellRenderer(r);
    }

    private void info(String m)  { JOptionPane.showMessageDialog(this, m, "Success", JOptionPane.INFORMATION_MESSAGE); }
    private void warn(String m)  { JOptionPane.showMessageDialog(this, m, "Validation", JOptionPane.WARNING_MESSAGE); }
    private void error(String m) { JOptionPane.showMessageDialog(this, m, "Error", JOptionPane.ERROR_MESSAGE); }
}