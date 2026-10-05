package com.mycompany.solarpos.ui;

import com.mycompany.solarpos.dao.InventoryDAO;
import com.mycompany.solarpos.factory.HardwareFactory;
import com.mycompany.solarpos.model.*;
import com.mycompany.solarpos.util.ValidationException;
import com.mycompany.solarpos.util.Validator;
import java.awt.*;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;

public class InventoryPanel extends JPanel {

    private static final Logger LOG = Logger.getLogger(InventoryPanel.class.getName());

    private final InventoryDAO dao = new InventoryDAO();
    private List<HardwareItem> items = new ArrayList<>();
    private HardwareItem selected;

    // form fields
    private final JComboBox<String> cmbCategory = new JComboBox<>(new String[]{"PANEL", "INVERTER", "BATTERY", "KIT"});
    private final JTextField txtCode = new JTextField(12);
    private final JTextField txtBrand = new JTextField(12);
    private final JTextField txtModel = new JTextField(12);
    private final JComboBox<Supplier> cmbSupplier = new JComboBox<>();
    private final JTextField txtDate = new JTextField(12);
    private final JTextField txtPrice = new JTextField(12);
    private final JTextField txtStock = new JTextField(12);
    private final JTextField txtWarranty = new JTextField(12);
    private final JTextField txtFee = new JTextField(12);
    private final JTextField txtWattage = new JTextField(12);
    private final JComboBox<String> cmbInvType = new JComboBox<>(new String[]{"HYBRID", "OFF_GRID", "ON_GRID"});
    private final JTextField txtCapKw = new JTextField(12);
    private final JTextField txtCapKwh = new JTextField(12);
    private final JTextField txtCycles = new JTextField(12);
    private final JTextField txtSearch = new JTextField(20);

    // table
    private final DefaultTableModel tableModel = new DefaultTableModel(
            new String[]{"ID", "Code", "Category", "Brand", "Model", "Specification",
                         "Testing Requirement", "Unit Price", "Stock", "Warranty (mo)", "Received"}, 0) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
        @Override public Class<?> getColumnClass(int c) {
            return switch (c) {
                case 0, 8, 9 -> Integer.class;
                case 7 -> Double.class;
                default -> String.class;
            };
        }
    };
    private final JTable table = new JTable(tableModel);
    private final TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(tableModel);

    public InventoryPanel() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        JLabel title = new JLabel("Component Inventory");
        title.setFont(new Font("Segoe UI", Font.BOLD, 24));

        JPanel top = new JPanel(new BorderLayout(0, 8));
        top.add(title, BorderLayout.NORTH);
        top.add(buildForm(), BorderLayout.CENTER);
        top.add(buildButtons(), BorderLayout.SOUTH);
        add(top, BorderLayout.NORTH);
        add(buildTablePanel(), BorderLayout.CENTER);

        cmbCategory.addActionListener(e -> updateFieldState());
        loadSuppliers();
        refresh();
        clearForm();
        addComponentListener(new java.awt.event.ComponentAdapter() {
    @Override
    public void componentShown(java.awt.event.ComponentEvent e) {
        refresh();
    }
});
    }

    // ---------------------------------------------------------------- layout

    private JPanel buildForm() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBorder(BorderFactory.createTitledBorder("Item details"));

        addField(p, "Category *",          cmbCategory, 0, 0);
        addField(p, "Item code *",         txtCode,     1, 0);
        addField(p, "Brand *",             txtBrand,    2, 0);
        addField(p, "Model *",             txtModel,    3, 0);

        addField(p, "Supplier *",          cmbSupplier, 0, 1);
        addField(p, "Date received *",     txtDate,     1, 1);
        addField(p, "Unit price (Rs.) *",  txtPrice,    2, 1);
        addField(p, "Stock qty *",         txtStock,    3, 1);

        addField(p, "Warranty (months) *", txtWarranty, 0, 2);
        addField(p, "Installer warranty fee (Rs.) *", txtFee, 1, 2);
        addField(p, "Wattage (W)",         txtWattage,  2, 2);
        addField(p, "Inverter type",       cmbInvType,  3, 2);

        addField(p, "Capacity (kW)",       txtCapKw,    0, 3);
        addField(p, "Capacity (kWh)",      txtCapKwh,   1, 3);
        addField(p, "Cycle life",          txtCycles,   2, 3);
        return p;
    }

    private void addField(JPanel p, String label, JComponent comp, int col, int row) {
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(4, 6, 4, 6);
        c.anchor = GridBagConstraints.WEST;
        c.gridx = col * 2;
        c.gridy = row;
        p.add(new JLabel(label), c);
        c.gridx = col * 2 + 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;
        p.add(comp, c);
    }

    private JPanel buildButtons() {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        JButton add = new JButton("Add");
        JButton update = new JButton("Update");
        JButton delete = new JButton("Delete");
        JButton clear = new JButton("Clear");

        add.setBackground(new Color(39, 174, 96));
        add.setForeground(Color.WHITE);
        update.setBackground(new Color(0, 120, 212));
        update.setForeground(Color.WHITE);
        delete.setBackground(new Color(192, 57, 43));
        delete.setForeground(Color.WHITE);

        add.addActionListener(e -> onAdd());
        update.addActionListener(e -> onUpdate());
        delete.addActionListener(e -> onDelete());
        clear.addActionListener(e -> clearForm());

        p.add(add);
        p.add(update);
        p.add(delete);
        p.add(clear);
        JLabel hint = new JLabel("   * required   |   Red rows = stock of 5 or fewer");
        hint.setForeground(Color.GRAY);
        p.add(hint);
        return p;
    }

    private JPanel buildTablePanel() {
        JPanel p = new JPanel(new BorderLayout(0, 6));

        JPanel searchBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        searchBar.add(new JLabel("Search:"));
        searchBar.add(txtSearch);
        txtSearch.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { applyFilter(); }
            @Override public void removeUpdate(DocumentEvent e) { applyFilter(); }
            @Override public void changedUpdate(DocumentEvent e) { applyFilter(); }
        });
        p.add(searchBar, BorderLayout.NORTH);

        table.setRowSorter(sorter);
        table.setRowHeight(26);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) onRowSelected();
        });

        DefaultTableCellRenderer renderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object value, boolean sel,
                                                           boolean focus, int row, int col) {
                super.getTableCellRendererComponent(t, value, sel, focus, row, col);
                if (value instanceof Double d) {
                    setText(String.format("%,.2f", d));
                }
                setHorizontalAlignment(value instanceof Number ? SwingConstants.RIGHT : SwingConstants.LEFT);
                int stock = (int) tableModel.getValueAt(t.convertRowIndexToModel(row), 8);
                if (!sel) {
                    setForeground(stock <= 5 ? new Color(192, 57, 43) : t.getForeground());
                }
                return this;
            }
        };
        table.setDefaultRenderer(Object.class, renderer);
        table.setDefaultRenderer(Integer.class, renderer);
        table.setDefaultRenderer(Double.class, renderer);

        p.add(new JScrollPane(table), BorderLayout.CENTER);
        return p;
    }

    // ---------------------------------------------------------------- data

    private void loadSuppliers() {
        try {
            for (Supplier s : dao.findSuppliers()) {
                cmbSupplier.addItem(s);
            }
        } catch (Exception ex) {
            LOG.log(Level.SEVERE, "Could not load suppliers", ex);
            error("Could not load suppliers:\n" + ex.getMessage());
        }
    }

    public void refresh() {
        try {
            items = dao.findAll();
            tableModel.setRowCount(0);
            for (HardwareItem it : items) {
                tableModel.addRow(new Object[]{
                    it.getId(), it.getItemCode(), it.getCategory(), it.getBrand(), it.getModel(),
                    describe(it), it.getTestingRequirement(), it.getUnitPrice(),
                    it.getStockQty(), it.getWarrantyMonths(), String.valueOf(it.getDateReceived())
                });
            }
        } catch (Exception ex) {
            LOG.log(Level.SEVERE, "Could not load inventory", ex);
            error("Could not load inventory:\n" + ex.getMessage());
        }
    }

    private String describe(HardwareItem it) {
        if (it instanceof SolarPanel p) return p.getWattage() + " W";
        if (it instanceof Inverter i) return i.getInverterType() + " " + i.getCapacityKw() + " kW";
        if (it instanceof Battery b) return b.getCapacityKwh() + " kWh / " + b.getCycleLife() + " cycles";
        return "-";
    }

    private void applyFilter() {
        String text = txtSearch.getText().trim();
        sorter.setRowFilter(text.isEmpty() ? null : RowFilter.regexFilter("(?i)" + Pattern.quote(text)));
    }

    // ---------------------------------------------------------------- actions

    private void onAdd() {
        try {
            dao.insert(buildItemFromForm());
            info("Item added successfully.");
            refresh();
            clearForm();
            addComponentListener(new java.awt.event.ComponentAdapter() {
    @Override
    public void componentShown(java.awt.event.ComponentEvent e) {
        refresh();
    }
});
        } catch (ValidationException ex) {
            warn(ex.getMessage());
        } catch (Exception ex) {
            LOG.log(Level.SEVERE, "Add item failed", ex);
            error("Could not add item:\n" + ex.getMessage());
        }
    }

    private void onUpdate() {
        if (selected == null) {
            warn("Select an item from the table first.");
            return;
        }
        try {
            HardwareItem item = buildItemFromForm();
            item.setId(selected.getId());
            dao.update(item);
            info("Item updated successfully.");
            refresh();
            clearForm();
            addComponentListener(new java.awt.event.ComponentAdapter() {
    @Override
    public void componentShown(java.awt.event.ComponentEvent e) {
        refresh();
    }
});
        } catch (ValidationException ex) {
            warn(ex.getMessage());
        } catch (Exception ex) {
            LOG.log(Level.SEVERE, "Update item failed", ex);
            error("Could not update item:\n" + ex.getMessage());
        }
    }

    private void onDelete() {
        if (selected == null) {
            warn("Select an item from the table first.");
            return;
        }
        int ok = JOptionPane.showConfirmDialog(this,
                "Delete " + selected + "?\nThis cannot be undone.",
                "Confirm delete", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (ok != JOptionPane.YES_OPTION) {
            return;
        }
        try {
            dao.delete(selected.getId());
            info("Item deleted.");
            refresh();
            clearForm();
            addComponentListener(new java.awt.event.ComponentAdapter() {
    @Override
    public void componentShown(java.awt.event.ComponentEvent e) {
        refresh();
    }
});
        } catch (ValidationException ex) {
            warn(ex.getMessage());
        } catch (Exception ex) {
            LOG.log(Level.SEVERE, "Delete item failed", ex);
            error("Could not delete item:\n" + ex.getMessage());
        }
    }

    private void onRowSelected() {
        int viewRow = table.getSelectedRow();
        if (viewRow < 0) {
            return;
        }
        selected = items.get(table.convertRowIndexToModel(viewRow));
        populateForm(selected);
    }

    // ---------------------------------------------------------------- form logic

    /** Enables only the fields that apply to the chosen category. */
    private void updateFieldState() {
        String cat = (String) cmbCategory.getSelectedItem();
        boolean panel = "PANEL".equals(cat);
        boolean inv = "INVERTER".equals(cat);
        boolean bat = "BATTERY".equals(cat);
        setFieldState(txtWattage, panel);
        cmbInvType.setEnabled(inv);
        setFieldState(txtCapKw, inv);
        setFieldState(txtCapKwh, bat);
        setFieldState(txtCycles, bat);
    }

    private void setFieldState(JTextField f, boolean enabled) {
        f.setEnabled(enabled);
        if (!enabled) {
            f.setText("");
        }
    }

    private void populateForm(HardwareItem it) {
        cmbCategory.setSelectedItem(it.getCategory());   // also triggers updateFieldState()
        txtCode.setText(it.getItemCode());
        txtBrand.setText(it.getBrand());
        txtModel.setText(it.getModel());
        txtDate.setText(String.valueOf(it.getDateReceived()));
        txtPrice.setText(String.format("%.2f", it.getUnitPrice()));
        txtStock.setText(String.valueOf(it.getStockQty()));
        txtWarranty.setText(String.valueOf(it.getWarrantyMonths()));
        txtFee.setText(String.format("%.2f", it.getInstallerWarrantyFee()));

        for (int i = 0; i < cmbSupplier.getItemCount(); i++) {
            if (cmbSupplier.getItemAt(i).getId() == it.getSupplierId()) {
                cmbSupplier.setSelectedIndex(i);
                break;
            }
        }

        if (it instanceof SolarPanel p) {
            txtWattage.setText(String.valueOf(p.getWattage()));
        } else if (it instanceof Inverter i) {
            cmbInvType.setSelectedItem(i.getInverterType());
            txtCapKw.setText(String.valueOf(i.getCapacityKw()));
        } else if (it instanceof Battery b) {
            txtCapKwh.setText(String.valueOf(b.getCapacityKwh()));
            txtCycles.setText(String.valueOf(b.getCycleLife()));
        }
    }

    private void clearForm() {
        selected = null;
        table.clearSelection();
        cmbCategory.setSelectedIndex(0);
        for (JTextField f : new JTextField[]{txtCode, txtBrand, txtModel, txtPrice, txtStock,
                                             txtWarranty, txtWattage, txtCapKw, txtCapKwh, txtCycles}) {
            f.setText("");
        }
        txtFee.setText("0");
        txtDate.setText(LocalDate.now().toString());
        cmbInvType.setSelectedIndex(0);
        if (cmbSupplier.getItemCount() > 0) {
            cmbSupplier.setSelectedIndex(0);
        }
        updateFieldState();
        txtCode.requestFocus();
    }
    
    

    /** Reads and validates the form. The Factory creates the correct subclass. */
    private HardwareItem buildItemFromForm() throws ValidationException {
        HardwareItem item = HardwareFactory.create((String) cmbCategory.getSelectedItem());

        String code = txtCode.getText().trim().toUpperCase();
        Validator.requireText("Item code", code);
        requireMax("Item code", code, 30);
        if (!code.matches("[A-Z0-9-]+")) {
            throw new ValidationException("Item code may contain only letters, digits and hyphens.");
        }
        item.setItemCode(code);

        Validator.requireText("Brand", txtBrand.getText());
        requireMax("Brand", txtBrand.getText().trim(), 50);
        item.setBrand(txtBrand.getText().trim());

        Validator.requireText("Model", txtModel.getText());
        requireMax("Model", txtModel.getText().trim(), 80);
        item.setModel(txtModel.getText().trim());

        Supplier s = (Supplier) cmbSupplier.getSelectedItem();
        if (s == null) {
            throw new ValidationException("Please select a supplier.");
        }
        item.setSupplierId(s.getId());

        try {
            item.setDateReceived(LocalDate.parse(txtDate.getText().trim()));
        } catch (DateTimeParseException e) {
            throw new ValidationException("Date received must be in the format yyyy-MM-dd (e.g. 2026-09-29).");
        }
        if (item.getDateReceived().isAfter(LocalDate.now())) {
            throw new ValidationException("Date received cannot be in the future.");
        }

        item.setUnitPrice(Validator.requirePositiveDouble("Unit price", txtPrice.getText()));
        item.setStockQty(Validator.requireNonNegativeInt("Stock quantity", txtStock.getText()));
        item.setWarrantyMonths(Validator.requirePositiveInt("Warranty months", txtWarranty.getText()));
        item.setInstallerWarrantyFee(Validator.requireNonNegativeDouble("Installer warranty fee", txtFee.getText()));

        if (item instanceof SolarPanel p) {
            p.setWattage(Validator.requirePositiveInt("Wattage", txtWattage.getText()));
        } else if (item instanceof Inverter i) {
            i.setInverterType((String) cmbInvType.getSelectedItem());
            i.setCapacityKw(Validator.requirePositiveDouble("Inverter capacity (kW)", txtCapKw.getText()));
        } else if (item instanceof Battery b) {
            b.setCapacityKwh(Validator.requirePositiveDouble("Battery capacity (kWh)", txtCapKwh.getText()));
            b.setCycleLife(Validator.requirePositiveInt("Cycle life", txtCycles.getText()));
        }
        return item;
    }

    private void requireMax(String field, String value, int max) throws ValidationException {
        if (value.length() > max) {
            throw new ValidationException(field + " must be " + max + " characters or fewer.");
        }
    }

    // ---------------------------------------------------------------- dialogs

    private void info(String msg)  { JOptionPane.showMessageDialog(this, msg, "Success", JOptionPane.INFORMATION_MESSAGE); }
    private void warn(String msg)  { JOptionPane.showMessageDialog(this, msg, "Validation", JOptionPane.WARNING_MESSAGE); }
    private void error(String msg) { JOptionPane.showMessageDialog(this, msg, "Error", JOptionPane.ERROR_MESSAGE); }
}