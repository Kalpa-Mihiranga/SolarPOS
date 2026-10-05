package com.mycompany.solarpos.ui;

import com.mycompany.solarpos.dao.CustomerDAO;
import com.mycompany.solarpos.dao.InventoryDAO;
import com.mycompany.solarpos.dao.OrderDAO;
import com.mycompany.solarpos.model.*;
import com.mycompany.solarpos.service.PackagePreset;
import com.mycompany.solarpos.service.PricingService;
import com.mycompany.solarpos.util.InsufficientStockException;
import com.mycompany.solarpos.util.Session;
import com.mycompany.solarpos.util.ValidationException;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.text.ParseException;
import java.util.*;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;

public class PosPanel extends JPanel {

    private static final Logger LOG = Logger.getLogger(PosPanel.class.getName());

    private static final String[][] CATEGORIES = {
        {"PANEL",    "Solar panels (count)"},
        {"INVERTER", "Inverter"},
        {"BATTERY",  "Battery units"},
        {"KIT",      "Installation kit"}
    };

    // ---- services
    private final InventoryDAO inventoryDao = new InventoryDAO();
    private final CustomerDAO customerDao = new CustomerDAO();
    private final OrderDAO orderDao = new OrderDAO();
    private final PricingService pricing = new PricingService();

    // ---- state
    private List<HardwareItem> inventory = new ArrayList<>();
    private final Map<String, HardwareItem> byCode = new HashMap<>();
    private final List<CartLine> cart = new ArrayList<>();
    private List<Customer> allCustomers = new ArrayList<>();
    private OrderSummary current = pricing.calculate(new ArrayList<>());
    private boolean userEditedDown = false;
    private boolean settingDown = false;

    // ---- customer widgets
    private final DefaultComboBoxModel<Customer> customerModel = new DefaultComboBoxModel<>();
    private final JComboBox<Customer> cmbCustomer = new JComboBox<>(customerModel);
    private final JTextField txtCustomerSearch = new JTextField(14);
    private final JLabel lblCustomerInfo = new JLabel(" ");

    // ---- package / custom widgets
    private final JComboBox<PackagePreset> cmbPackage =
            new JComboBox<>(PackagePreset.all().toArray(new PackagePreset[0]));
    private final JTextArea txtPackageInfo = new JTextArea(7, 40);
    private final Map<String, JComboBox<HardwareItem>> pickers = new LinkedHashMap<>();
    private final Map<String, JSpinner> spinners = new LinkedHashMap<>();

    // ---- cart widgets
    private final CartTableModel cartModel = new CartTableModel();
    private final JTable cartTable = new JTable(cartModel);

    // ---- summary widgets
    private final JLabel lblSubtotal = valueLabel();
    private final JLabel lblDiscount = valueLabel();
    private final JLabel lblBundle = new JLabel(" ");
    private final JLabel lblWarranty = valueLabel();
    private final JLabel lblTax = valueLabel();
    private final JLabel lblGrand = valueLabel();
    private final JLabel lblKw = valueLabel();
    private final JTextField txtDown = new JTextField(10);
    private final JLabel lblDownHint = new JLabel(" ");
    private final JLabel lblBalance = valueLabel();

    public PosPanel() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        JLabel title = new JLabel("New Sale");
        title.setFont(new Font("Segoe UI", Font.BOLD, 24));
        JPanel north = new JPanel(new BorderLayout(0, 8));
        north.add(title, BorderLayout.NORTH);
        north.add(buildCustomerBar(), BorderLayout.CENTER);
        add(north, BorderLayout.NORTH);

        JPanel left = new JPanel(new BorderLayout(0, 10));
        left.add(buildTabs(), BorderLayout.NORTH);
        left.add(buildCartPanel(), BorderLayout.CENTER);
        add(left, BorderLayout.CENTER);
        add(buildSummaryPanel(), BorderLayout.EAST);

        // Refresh customers and stock every time this screen is opened
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentShown(ComponentEvent e) {
                reloadCustomers();
                reloadInventory();
            }
        });

        reloadCustomers();
        reloadInventory();
        recalc();
    }

    // =================================================================== layout

    private JPanel buildCustomerBar() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBorder(BorderFactory.createTitledBorder("Customer"));

        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(4, 6, 4, 6);
        c.anchor = GridBagConstraints.WEST;
        c.gridy = 0;
        c.gridx = 0; p.add(new JLabel("Find:"), c);
        c.gridx = 1; p.add(txtCustomerSearch, c);
        c.gridx = 2; p.add(new JLabel("Customer *"), c);
        c.gridx = 3; c.fill = GridBagConstraints.HORIZONTAL; c.weightx = 1; p.add(cmbCustomer, c);

        c.gridy = 1; c.gridx = 0; c.gridwidth = 4;
        lblCustomerInfo.setForeground(Color.GRAY);
        p.add(lblCustomerInfo, c);

        txtCustomerSearch.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { filterCustomers(); }
            @Override public void removeUpdate(DocumentEvent e) { filterCustomers(); }
            @Override public void changedUpdate(DocumentEvent e) { filterCustomers(); }
        });
        cmbCustomer.addActionListener(e -> updateCustomerInfo());
        return p;
    }

    private JTabbedPane buildTabs() {
        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("kWh Package", buildPackageTab());
        tabs.addTab("Custom Components", buildCustomTab());
        return tabs;
    }

    private JPanel buildPackageTab() {
        JPanel p = new JPanel(new BorderLayout(0, 8));
        p.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        JButton load = new JButton("Load package into cart");
        load.setBackground(new Color(0, 120, 212));
        load.setForeground(Color.WHITE);
        load.addActionListener(e -> onLoadPackage());

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        top.add(new JLabel("Target package:"));
        top.add(cmbPackage);
        top.add(load);

        txtPackageInfo.setEditable(false);
        txtPackageInfo.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));

        p.add(top, BorderLayout.NORTH);
        p.add(new JScrollPane(txtPackageInfo), BorderLayout.CENTER);
        cmbPackage.addActionListener(e -> updatePackagePreview());
        return p;
    }

    private JPanel buildCustomTab() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        int row = 0;
        for (String[] cat : CATEGORIES) {
            JComboBox<HardwareItem> combo = new JComboBox<>();
            combo.setRenderer(itemRenderer());
            JSpinner spinner = new JSpinner(new SpinnerNumberModel(1, 1, 1000, 1));
            ((JSpinner.DefaultEditor) spinner.getEditor()).getTextField().setColumns(4);
            JButton add = new JButton("Add to cart");
            add.addActionListener(e -> onAddCustom(cat[0]));

            pickers.put(cat[0], combo);
            spinners.put(cat[0], spinner);

            GridBagConstraints c = new GridBagConstraints();
            c.insets = new Insets(4, 6, 4, 6);
            c.gridy = row;
            c.anchor = GridBagConstraints.WEST;
            c.gridx = 0; p.add(new JLabel(cat[1]), c);
            c.gridx = 1; c.fill = GridBagConstraints.HORIZONTAL; c.weightx = 1; p.add(combo, c);
            c.gridx = 2; c.fill = GridBagConstraints.NONE; c.weightx = 0; p.add(new JLabel("Qty"), c);
            c.gridx = 3; p.add(spinner, c);
            c.gridx = 4; p.add(add, c);
            row++;
        }
        return p;
    }

    private JPanel buildCartPanel() {
        JPanel p = new JPanel(new BorderLayout(0, 6));
        p.setBorder(BorderFactory.createTitledBorder("Cart"));

        cartTable.setRowHeight(26);
        cartTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        cartTable.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);
        cartTable.setDefaultRenderer(Double.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object v, boolean sel,
                                                           boolean focus, int r, int c) {
                super.getTableCellRendererComponent(t, v, sel, focus, r, c);
                if (v instanceof Double d) {
                    setText(String.format(Locale.US, "%,.2f", d));
                }
                setHorizontalAlignment(SwingConstants.RIGHT);
                return this;
            }
        });
        cartTable.getColumnModel().getColumn(0).setPreferredWidth(300);

        JButton remove = new JButton("Remove selected");
        JButton clear = new JButton("Clear cart");
        remove.addActionListener(e -> onRemove());
        clear.addActionListener(e -> onClearCart());
        JLabel hint = new JLabel("   Double-click a Qty cell to change it");
        hint.setForeground(Color.GRAY);
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        buttons.add(remove);
        buttons.add(clear);
        buttons.add(hint);

        p.add(new JScrollPane(cartTable), BorderLayout.CENTER);
        p.add(buttons, BorderLayout.SOUTH);
        return p;
    }

    private JPanel buildSummaryPanel() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBorder(BorderFactory.createTitledBorder("Order summary"));
        p.setPreferredSize(new Dimension(340, 0));

        lblBundle.setForeground(new Color(39, 174, 96));
        lblBundle.setFont(lblBundle.getFont().deriveFont(Font.ITALIC));
        lblGrand.setFont(new Font("Segoe UI", Font.BOLD, 22));
        lblGrand.setForeground(new Color(30, 132, 73));
        lblBalance.setFont(lblBalance.getFont().deriveFont(Font.BOLD, 15f));
        lblDownHint.setFont(lblDownHint.getFont().deriveFont(11f));
        txtDown.setHorizontalAlignment(SwingConstants.RIGHT);
        txtDown.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { onDownChanged(); }
            @Override public void removeUpdate(DocumentEvent e) { onDownChanged(); }
            @Override public void changedUpdate(DocumentEvent e) { onDownChanged(); }
        });

        JLabel total = new JLabel("TOTAL");
        total.setFont(new Font("Segoe UI", Font.BOLD, 16));

        JButton btnMin = new JButton("Min 30%");
        JButton btnFull = new JButton("Pay in full");
        btnMin.addActionListener(e -> { userEditedDown = false; recalc(); });
        btnFull.addActionListener(e -> {
            if (!cart.isEmpty()) {
                userEditedDown = true;
                setDown(String.format(Locale.US, "%.2f", current.getGrandTotal()));
            }
        });
        JPanel quick = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        quick.add(btnMin);
        quick.add(btnFull);

        JButton btnSale = new JButton("COMPLETE SALE");
        btnSale.setFont(new Font("Segoe UI", Font.BOLD, 16));
        btnSale.setBackground(new Color(39, 174, 96));
        btnSale.setForeground(Color.WHITE);
        btnSale.setPreferredSize(new Dimension(0, 48));
        btnSale.addActionListener(e -> onCompleteSale());

        JButton btnClearSale = new JButton("Clear sale");
        btnClearSale.addActionListener(e -> onClearSale());

        int r = 0;
        addRow(p, r++, new JLabel("Subtotal"), lblSubtotal);
        addRow(p, r++, new JLabel("Bundle discount"), lblDiscount);
        addSpan(p, r++, lblBundle);
        addRow(p, r++, new JLabel("Installer warranty fees"), lblWarranty);
        addRow(p, r++, new JLabel(String.format("Tax (%.0f%%)", PricingService.TAX_RATE * 100)), lblTax);
        addSpan(p, r++, new JSeparator());
        addRow(p, r++, total, lblGrand);
        addRow(p, r++, new JLabel("Peak power (panels)"), lblKw);
        addSpan(p, r++, new JSeparator());
        addRow(p, r++, new JLabel("Down payment (Rs.)"), txtDown);
        addSpan(p, r++, lblDownHint);
        addSpan(p, r++, quick);
        addRow(p, r++, new JLabel("Balance due"), lblBalance);
        addSpan(p, r++, btnSale);
        addSpan(p, r++, btnClearSale);

        GridBagConstraints filler = new GridBagConstraints();
        filler.gridy = r;
        filler.weighty = 1;
        p.add(Box.createGlue(), filler);
        return p;
    }

    private void addRow(JPanel p, int row, JComponent left, JComponent right) {
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(5, 8, 5, 8);
        c.gridy = row;
        c.gridx = 0;
        c.anchor = GridBagConstraints.WEST;
        p.add(left, c);
        c.gridx = 1;
        c.anchor = GridBagConstraints.EAST;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;
        p.add(right, c);
    }

    private void addSpan(JPanel p, int row, JComponent comp) {
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(3, 8, 3, 8);
        c.gridy = row;
        c.gridx = 0;
        c.gridwidth = 2;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;
        p.add(comp, c);
    }

    private static JLabel valueLabel() {
        return new JLabel("Rs. 0.00", SwingConstants.RIGHT);
    }

    // =================================================================== data loading

    private void reloadCustomers() {
        try {
            allCustomers = customerDao.findAll();
            filterCustomers();
        } catch (Exception ex) {
            LOG.log(Level.SEVERE, "Could not load customers", ex);
            error("Could not load customers:\n" + ex.getMessage());
        }
    }

    public void reloadInventory() {
        try {
            inventory = inventoryDao.findAll();
            byCode.clear();
            for (HardwareItem it : inventory) {
                byCode.put(it.getItemCode(), it);
            }
            for (String[] cat : CATEGORIES) {
                fillCombo(pickers.get(cat[0]), cat[0]);
            }
            updatePackagePreview();
            rebindCart();
        } catch (Exception ex) {
            LOG.log(Level.SEVERE, "Could not load inventory", ex);
            error("Could not load inventory:\n" + ex.getMessage());
        }
    }

    private void fillCombo(JComboBox<HardwareItem> combo, String category) {
        HardwareItem previous = (HardwareItem) combo.getSelectedItem();
        combo.removeAllItems();
        int selectIndex = 0;
        int i = 0;
        for (HardwareItem it : inventory) {
            if (!category.equals(it.getCategory())) {
                continue;
            }
            combo.addItem(it);
            if (previous != null && previous.getId() == it.getId()) {
                selectIndex = i;
            }
            i++;
        }
        if (combo.getItemCount() > 0) {
            combo.setSelectedIndex(selectIndex);
        }
    }

    /** After stock changes, point the cart at fresh inventory objects and cap quantities. */
    private void rebindCart() {
        if (cart.isEmpty()) {
            recalc();
            return;
        }
        Map<Integer, HardwareItem> byId = new HashMap<>();
        for (HardwareItem it : inventory) {
            byId.put(it.getId(), it);
        }
        List<CartLine> fresh = new ArrayList<>();
        boolean changed = false;
        for (CartLine l : cart) {
            HardwareItem it = byId.get(l.getItem().getId());
            if (it == null || it.getStockQty() <= 0) {
                changed = true;
                continue;
            }
            int qty = Math.min(l.getQty(), it.getStockQty());
            if (qty != l.getQty()) {
                changed = true;
            }
            fresh.add(new CartLine(it, qty));
        }
        cart.clear();
        cart.addAll(fresh);
        cartModel.fireTableDataChanged();
        recalc();
        if (changed) {
            info("Some cart lines were adjusted or removed to match the current stock.");
        }
    }

    // =================================================================== customers

    private void filterCustomers() {
        Customer previous = (Customer) cmbCustomer.getSelectedItem();
        String q = txtCustomerSearch.getText().trim().toLowerCase();

        List<Customer> matches = new ArrayList<>();
        for (Customer c : allCustomers) {
            if (q.isEmpty() || c.getName().toLowerCase().contains(q) || c.getPhone().contains(q)) {
                matches.add(c);
            }
        }
        customerModel.removeAllElements();
        for (Customer c : matches) {
            customerModel.addElement(c);
        }

        int idx = -1;
        if (matches.size() == 1) {
            idx = 0;
        } else if (previous != null) {
            for (int i = 0; i < matches.size(); i++) {
                if (matches.get(i).getId() == previous.getId()) {
                    idx = i;
                    break;
                }
            }
        }
        cmbCustomer.setSelectedIndex(idx);
        updateCustomerInfo();
    }

    private void updateCustomerInfo() {
        Customer c = (Customer) cmbCustomer.getSelectedItem();
        if (c == null) {
            lblCustomerInfo.setText(" ");
            return;
        }
        lblCustomerInfo.setText(c.getType() + "  |  "
                + ("SINGLE".equals(c.getGridPhase()) ? "Single phase" : "Three phase")
                + "  |  Contractor: " + (c.getContractorName() == null ? "none" : c.getContractorName())
                + "  |  " + c.getAddress());
    }

    // =================================================================== package mode

    private void updatePackagePreview() {
        PackagePreset preset = (PackagePreset) cmbPackage.getSelectedItem();
        if (preset == null) {
            return;
        }
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("  %s  (target %.0f kWh)\n\n", preset.getName(), preset.getTargetKwh()));

        List<CartLine> lines = new ArrayList<>();
        boolean complete = true;
        for (Map.Entry<String, Integer> e : preset.getComponents().entrySet()) {
            HardwareItem it = byCode.get(e.getKey());
            if (it == null) {
                sb.append(String.format("  %3d x %s  (NOT IN INVENTORY)\n", e.getValue(), e.getKey()));
                complete = false;
                continue;
            }
            lines.add(new CartLine(it, e.getValue()));
            String lowStock = it.getStockQty() < e.getValue() ? "   <-- LOW STOCK (" + it.getStockQty() + ")" : "";
            sb.append(String.format("  %3d x %s %s%s\n", e.getValue(), it.getBrand(), it.getModel(), lowStock));
        }
        if (complete) {
            OrderSummary s = pricing.calculate(lines);
            sb.append(String.format(Locale.US, "\n  Peak panel power       : %.2f kW", s.getTotalKw()));
            sb.append("\n  Estimated total (tax in): ").append(money(s.getGrandTotal()));
        }
        txtPackageInfo.setText(sb.toString());
        txtPackageInfo.setCaretPosition(0);
    }

    private void onLoadPackage() {
        PackagePreset preset = (PackagePreset) cmbPackage.getSelectedItem();
        if (preset == null) {
            return;
        }
        try {
            List<CartLine> lines = preset.build(byCode);   // checks stock too
            if (!cart.isEmpty()) {
                int ok = JOptionPane.showConfirmDialog(this,
                        "Replace the current cart with \"" + preset + "\"?",
                        "Load package", JOptionPane.YES_NO_OPTION);
                if (ok != JOptionPane.YES_OPTION) {
                    return;
                }
            }
            cart.clear();
            cart.addAll(lines);
            userEditedDown = false;
            cartModel.fireTableDataChanged();
            recalc();
        } catch (ValidationException ex) {
            warn(ex.getMessage());
        }
    }

    // =================================================================== custom mode

    private ListCellRenderer<Object> itemRenderer() {
        return new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                          boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof HardwareItem it) {
                    setText(it.getBrand() + " " + it.getModel() + "  |  " + spec(it)
                            + "  |  " + money(it.getUnitPrice()) + "  |  stock " + it.getStockQty());
                    if (it.getStockQty() == 0 && !isSelected) {
                        setForeground(Color.GRAY);
                    }
                }
                return this;
            }
        };
    }

    private static String spec(HardwareItem it) {
        if (it instanceof SolarPanel p) return p.getWattage() + " W";
        if (it instanceof Inverter i) return i.getInverterType() + " " + i.getCapacityKw() + " kW";
        if (it instanceof Battery b) return b.getCapacityKwh() + " kWh";
        return "kit";
    }

    private void onAddCustom(String category) {
        HardwareItem item = (HardwareItem) pickers.get(category).getSelectedItem();
        if (item == null) {
            warn("No items available in this category.");
            return;
        }
        JSpinner spinner = spinners.get(category);
        try {
            spinner.commitEdit();
        } catch (ParseException ex) {
            warn("Enter a valid whole-number quantity.");
            return;
        }
        int qty = (Integer) spinner.getValue();
        addToCart(item, qty);
        spinner.setValue(1);
    }

    // =================================================================== cart

    private void addToCart(HardwareItem item, int qty) {
        if (item.getStockQty() <= 0) {
            warn(item + " is out of stock.");
            return;
        }
        CartLine existing = null;
        for (CartLine l : cart) {
            if (l.getItem().getId() == item.getId()) {
                existing = l;
                break;
            }
        }
        int already = existing == null ? 0 : existing.getQty();
        if (already + qty > item.getStockQty()) {
            warn("Only " + item.getStockQty() + " in stock for " + item
                    + " (" + already + " already in the cart).");
            return;
        }
        if (existing != null) {
            existing.setQty(already + qty);
        } else {
            cart.add(new CartLine(item, qty));
        }
        cartModel.fireTableDataChanged();
        recalc();
    }

    private void stopCellEditing() {
        if (cartTable.isEditing()) {
            cartTable.getCellEditor().stopCellEditing();
        }
    }

    private void onRemove() {
        stopCellEditing();
        int row = cartTable.getSelectedRow();
        if (row < 0) {
            warn("Select a cart line first.");
            return;
        }
        cart.remove(row);
        cartModel.fireTableDataChanged();
        recalc();
    }

    private void onClearCart() {
        stopCellEditing();
        cart.clear();
        userEditedDown = false;
        cartModel.fireTableDataChanged();
        recalc();
    }

    private void onClearSale() {
        if (!cart.isEmpty()) {
            int ok = JOptionPane.showConfirmDialog(this, "Discard the current sale?",
                    "Clear sale", JOptionPane.YES_NO_OPTION);
            if (ok != JOptionPane.YES_OPTION) {
                return;
            }
        }
        clearSale();
    }

    private void clearSale() {
        stopCellEditing();
        cart.clear();
        cartModel.fireTableDataChanged();
        userEditedDown = false;
        txtCustomerSearch.setText("");
        cmbCustomer.setSelectedIndex(-1);
        recalc();
    }

    // =================================================================== totals

    private void recalc() {
        current = pricing.calculate(cart);
        lblSubtotal.setText(money(current.getSubtotal()));
        lblDiscount.setText(current.getDiscount() > 0 ? "- " + money(current.getDiscount()) : money(0));
        lblBundle.setText(cart.isEmpty() ? " " : current.getBundleLabel());
        lblWarranty.setText(money(current.getWarrantyFees()));
        lblTax.setText(money(current.getTax()));
        lblGrand.setText(money(current.getGrandTotal()));
        lblKw.setText(String.format(Locale.US, "%.2f kW", current.getTotalKw()));
        if (!userEditedDown) {
            setDown(cart.isEmpty() ? "" : String.format(Locale.US, "%.2f", current.getMinDownPayment()));
        }
        updateBalance();
    }

    private void onDownChanged() {
        if (!settingDown) {
            userEditedDown = true;
        }
        updateBalance();
    }

    private void setDown(String text) {
        settingDown = true;
        try {
            txtDown.setText(text);
        } finally {
            settingDown = false;
        }
    }

    private void updateBalance() {
        if (cart.isEmpty()) {
            lblBalance.setText("-");
            lblDownHint.setText(" ");
            return;
        }
        double grand = current.getGrandTotal();
        double min = current.getMinDownPayment();
        Double down = parseMoney(txtDown.getText());

        lblDownHint.setForeground(Color.GRAY);
        if (down == null) {
            lblBalance.setText("-");
            lblDownHint.setText("Enter a valid amount");
            lblDownHint.setForeground(new Color(192, 57, 43));
        } else if (down + 0.005 < min) {
            lblBalance.setText(money(grand - down));
            lblDownHint.setText("Below the minimum of " + money(min));
            lblDownHint.setForeground(new Color(192, 57, 43));
        } else if (down > grand + 0.005) {
            lblBalance.setText("-");
            lblDownHint.setText("More than the total amount");
            lblDownHint.setForeground(new Color(192, 57, 43));
        } else {
            lblBalance.setText(money(grand - down));
            lblDownHint.setText("Minimum 30%: " + money(min));
        }
    }

    private Double parseMoney(String text) {
        try {
            String t = text.replace(",", "").replace("Rs.", "").trim();
            return t.isEmpty() ? null : Double.valueOf(t);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static String money(double v) {
        return String.format(Locale.US, "Rs. %,.2f", v);
    }

    // =================================================================== complete sale

    private void onCompleteSale() {
        stopCellEditing();
        try {
            Customer customer = (Customer) cmbCustomer.getSelectedItem();
            if (customer == null) {
                throw new ValidationException("Please select a customer.");
            }
            if (cart.isEmpty()) {
                throw new ValidationException("The cart is empty. Load a package or add components first.");
            }
            Double downValue = parseMoney(txtDown.getText());
            if (downValue == null) {
                throw new ValidationException("Enter a valid down payment amount.");
            }
            double down = PricingService.round2(downValue);
            pricing.validateDownPayment(current, down);

            int ok = JOptionPane.showConfirmDialog(this,
                    "Complete this sale?\n\n"
                    + "Customer      : " + customer.getName() + "\n"
                    + "Total         : " + money(current.getGrandTotal()) + "\n"
                    + "Down payment  : " + money(down) + "\n"
                    + "Balance due   : " + money(current.getGrandTotal() - down),
                    "Confirm sale", JOptionPane.YES_NO_OPTION);
            if (ok != JOptionPane.YES_OPTION) {
                return;
            }

            List<CartLine> soldLines = new ArrayList<>(cart);
            OrderSummary sold = current;
            int orderId = orderDao.saveOrder(customer.getId(), Session.getCurrentUser().getId(),
                    soldLines, down);                      // one DB transaction

            clearSale();
            reloadInventory();                             // stock has changed
            new ReceiptDialog(SwingUtilities.getWindowAncestor(this), orderId, customer,
                    Session.getCurrentUser().getUsername(), soldLines, sold, down).setVisible(true);

        } catch (ValidationException ex) {
            warn(ex.getMessage());
        } catch (InsufficientStockException ex) {
            warn(ex.getMessage() + "\nStock levels have been refreshed.");
            reloadInventory();
        } catch (Exception ex) {
            LOG.log(Level.SEVERE, "Sale failed", ex);
            error("The sale could not be saved (nothing was changed):\n" + ex.getMessage());
        }
    }

    // =================================================================== cart table model

    private class CartTableModel extends AbstractTableModel {
        private final String[] cols = {"Item", "Category", "Qty", "Unit Price", "Line Total", "In Stock"};

        @Override public int getRowCount() { return cart.size(); }
        @Override public int getColumnCount() { return cols.length; }
        @Override public String getColumnName(int c) { return cols[c]; }

        @Override
        public Class<?> getColumnClass(int c) {
            return switch (c) {
                case 2, 5 -> Integer.class;
                case 3, 4 -> Double.class;
                default -> String.class;
            };
        }

        @Override public boolean isCellEditable(int r, int c) { return c == 2; }

        @Override
        public Object getValueAt(int r, int c) {
            CartLine l = cart.get(r);
            HardwareItem it = l.getItem();
            return switch (c) {
                case 0 -> it.getBrand() + " " + it.getModel();
                case 1 -> it.getCategory();
                case 2 -> l.getQty();
                case 3 -> it.getUnitPrice();
                case 4 -> l.getLineTotal();
                default -> it.getStockQty();
            };
        }

        @Override
        public void setValueAt(Object value, int r, int c) {
            if (c != 2) {
                return;
            }
            int q = ((Number) value).intValue();
            CartLine l = cart.get(r);
            if (q <= 0) {
                warn("Quantity must be at least 1. Use \"Remove selected\" to delete a line.");
                return;
            }
            if (q > l.getItem().getStockQty()) {
                warn("Only " + l.getItem().getStockQty() + " in stock for " + l.getItem() + ".");
                return;
            }
            l.setQty(q);
            fireTableRowsUpdated(r, r);
            recalc();
        }
    }

    // =================================================================== dialogs

    private void info(String msg)  { JOptionPane.showMessageDialog(this, msg, "Information", JOptionPane.INFORMATION_MESSAGE); }
    private void warn(String msg)  { JOptionPane.showMessageDialog(this, msg, "Validation", JOptionPane.WARNING_MESSAGE); }
    private void error(String msg) { JOptionPane.showMessageDialog(this, msg, "Error", JOptionPane.ERROR_MESSAGE); }
}