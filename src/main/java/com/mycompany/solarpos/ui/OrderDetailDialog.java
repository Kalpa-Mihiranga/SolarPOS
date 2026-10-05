package com.mycompany.solarpos.ui;

import com.mycompany.solarpos.dao.SalesHistoryDAO;
import java.awt.*;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.*;
import javax.swing.table.*;

public class OrderDetailDialog extends JDialog {

    private static final Logger LOG = Logger.getLogger(OrderDetailDialog.class.getName());

    public OrderDetailDialog(Window owner, SalesHistoryDAO dao, int orderId) {
        super(owner, "Order #" + orderId + " — Line items & Profit",
              Dialog.ModalityType.APPLICATION_MODAL);

        String[] cols = {"Code", "Item", "Category", "Qty",
                         "Unit Price", "Line Total", "Warranty End", "Line Profit"};
        DefaultTableModel model = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

        double totalProfit = 0;
        try {
            List<String[]> lines = dao.findOrderLines(orderId);
            for (String[] l : lines) {
                model.addRow(l);
                try { totalProfit += Double.parseDouble(l[7].replace(",", "")); }
                catch (NumberFormatException ignored) {}
            }
        } catch (Exception ex) {
            LOG.log(Level.SEVERE, "Load order detail failed", ex);
        }

        JTable table = new JTable(model);
        table.setRowHeight(26);
        table.getTableHeader().setReorderingAllowed(false);

        // Right-align money columns
        DefaultTableCellRenderer right = new DefaultTableCellRenderer();
        right.setHorizontalAlignment(SwingConstants.RIGHT);
        for (int c : new int[]{4, 5, 7}) {
            table.getColumnModel().getColumn(c).setCellRenderer(right);
        }

        // Colour profit column
        table.getColumnModel().getColumn(7).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object v, boolean sel,
                                                           boolean focus, int r, int c) {
                super.getTableCellRendererComponent(t, v, sel, focus, r, c);
                setHorizontalAlignment(SwingConstants.RIGHT);
                if (!sel && v != null) {
                    double val = 0;
                    try { val = Double.parseDouble(v.toString().replace(",","")); }
                    catch (NumberFormatException ignored) {}
                    setForeground(val >= 0
                            ? new Color(39, 174, 96) : new Color(192, 57, 43));
                }
                return this;
            }
        });

        JLabel footer = new JLabel(String.format(
                "  Total gross profit on this order: Rs. %,.2f", totalProfit));
        footer.setFont(new Font("Segoe UI", Font.BOLD, 13));
        footer.setForeground(new Color(39, 174, 96));
        footer.setBorder(BorderFactory.createEmptyBorder(8, 6, 8, 6));

        JButton close = new JButton("Close");
        close.addActionListener(e -> dispose());
        JPanel south = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        south.add(footer);
        south.add(close);

        setLayout(new BorderLayout());
        add(new JScrollPane(table), BorderLayout.CENTER);
        add(south, BorderLayout.SOUTH);
        setSize(950, 420);
        setLocationRelativeTo(owner);
        getRootPane().setDefaultButton(close);
    }
}