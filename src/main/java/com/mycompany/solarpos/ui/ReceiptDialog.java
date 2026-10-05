package com.mycompany.solarpos.ui;

import com.mycompany.solarpos.model.CartLine;
import com.mycompany.solarpos.model.Customer;
import com.mycompany.solarpos.model.HardwareItem;
import com.mycompany.solarpos.model.OrderSummary;
import java.awt.*;
import java.awt.print.PrinterException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import javax.swing.*;

public class ReceiptDialog extends JDialog {

    private static final int W = 62;   // receipt width in characters
    private final JTextArea area = new JTextArea();

    public ReceiptDialog(Window owner, int orderId, Customer customer, String cashier,
                         List<CartLine> lines, OrderSummary s, double downPayment) {
        super(owner, "Receipt - Order #" + orderId, Dialog.ModalityType.APPLICATION_MODAL);

        area.setText(buildText(orderId, customer, cashier, lines, s, downPayment));
        area.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        area.setEditable(false);
        area.setMargin(new Insets(12, 14, 12, 14));
        area.setCaretPosition(0);

        JButton print = new JButton("Print");
        JButton close = new JButton("Close");
        print.addActionListener(e -> onPrint());
        close.addActionListener(e -> dispose());
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttons.add(print);
        buttons.add(close);

        setLayout(new BorderLayout());
        add(new JScrollPane(area), BorderLayout.CENTER);
        add(buttons, BorderLayout.SOUTH);
        setSize(640, 700);
        setLocationRelativeTo(owner);
        getRootPane().setDefaultButton(close);
    }

    private void onPrint() {
        try {
            area.print();
        } catch (PrinterException ex) {
            JOptionPane.showMessageDialog(this, "Could not print: " + ex.getMessage(),
                    "Print error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private static String buildText(int orderId, Customer c, String cashier,
                                    List<CartLine> lines, OrderSummary s, double down) {
        StringBuilder sb = new StringBuilder();
        sb.append(center("SOLAR POS")).append('\n');
        sb.append(center("SALES RECEIPT")).append('\n');
        sb.append(rule('=')).append('\n');
        sb.append(String.format("%-11s: %d\n", "Order #", orderId));
        sb.append(String.format("%-11s: %s\n", "Date",
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))));
        sb.append(String.format("%-11s: %s\n", "Cashier", cashier));
        sb.append(String.format("%-11s: %s (%s)\n", "Customer", c.getName(), c.getPhone()));
        sb.append(String.format("%-11s: %s\n", "Address", c.getAddress()));
        sb.append(String.format("%-11s: %s\n", "Grid",
                "SINGLE".equals(c.getGridPhase()) ? "Single phase" : "Three phase"));
        if (c.getContractorName() != null) {
            sb.append(String.format("%-11s: %s\n", "Contractor", c.getContractorName()));
        }
        sb.append(rule('-')).append('\n');
        sb.append(String.format("%-28s %4s %13s %14s\n", "Item", "Qty", "Unit Price", "Line Total"));
        sb.append(rule('-')).append('\n');

        for (CartLine l : lines) {
            HardwareItem it = l.getItem();
            sb.append(String.format(Locale.US, "%-28s %4d %,13.2f %,14.2f\n",
                    trunc(it.getBrand() + " " + it.getModel(), 28),
                    l.getQty(), it.getUnitPrice(), l.getLineTotal()));
        }

        sb.append(rule('-')).append('\n');
        sb.append(row("Subtotal", money(s.getSubtotal())));
        sb.append(row("Bundle discount", "- " + money(s.getDiscount())));
        if (s.getDiscountRate() > 0) {
            sb.append("  (").append(s.getBundleLabel()).append(")\n");
        }
        sb.append(row("Installer warranty fees", money(s.getWarrantyFees())));
        sb.append(row("Tax (18%)", money(s.getTax())));
        sb.append(rule('-')).append('\n');
        sb.append(row("TOTAL", money(s.getGrandTotal())));
        sb.append(row("Down payment received", money(down)));
        sb.append(row("BALANCE DUE", money(s.getGrandTotal() - down)));
        sb.append(rule('-')).append('\n');
        sb.append(String.format(Locale.US, "System size (panels): %.2f kW\n\n", s.getTotalKw()));

        sb.append("WARRANTY COVERAGE (from date of sale)\n");
        LocalDate today = LocalDate.now();
        for (CartLine l : lines) {
            HardwareItem it = l.getItem();
            sb.append(String.format("  %-40s until %s\n",
                    trunc(it.getBrand() + " " + it.getModel(), 40),
                    today.plusMonths(it.getWarrantyMonths())));
        }
        sb.append(rule('=')).append('\n');
        sb.append(center("Thank you for choosing solar energy!")).append('\n');
        return sb.toString();
    }

    private static String money(double v) { return String.format(Locale.US, "Rs. %,.2f", v); }
    private static String row(String label, String value) { return String.format("%-38s%24s\n", label, value); }
    private static String rule(char ch) { return String.valueOf(ch).repeat(W); }
    private static String center(String s) { return " ".repeat(Math.max(0, (W - s.length()) / 2)) + s; }
    private static String trunc(String s, int max) { return s.length() <= max ? s : s.substring(0, max - 1) + "."; }
}