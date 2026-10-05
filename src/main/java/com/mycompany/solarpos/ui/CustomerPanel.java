package com.mycompany.solarpos.ui;

import com.mycompany.solarpos.dao.CustomerDAO;
import com.mycompany.solarpos.model.Contractor;
import com.mycompany.solarpos.model.Customer;
import com.mycompany.solarpos.util.ValidationException;
import com.mycompany.solarpos.util.Validator;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;

public class CustomerPanel extends JPanel {

    private static final Logger LOG = Logger.getLogger(CustomerPanel.class.getName());
    private static final Contractor NO_CONTRACTOR = new Contractor(0, "-- None --", "", "");

    private final CustomerDAO dao = new CustomerDAO();
    private List<Customer> customers = new ArrayList<>();
    private Customer selected;

    private final JTextField txtName = new JTextField(16);
    private final JComboBox<String> cmbType = new JComboBox<>(new String[]{"RESIDENTIAL", "COMMERCIAL"});
    private final JTextField txtPhone = new JTextField(16);
    private final JTextField txtEmail = new JTextField(16);
    private final JTextField txtAddress = new JTextField(16);
    private final JRadioButton rdoSingle = new JRadioButton("Single phase", true);
    private final JRadioButton rdoThree = new JRadioButton("Three phase");
    private final DefaultComboBoxModel<Contractor> contractorModel = new DefaultComboBoxModel<>();
    private final JComboBox<Contractor> cmbContractor = new JComboBox<>(contractorModel);
    private final JTextField txtSearch = new JTextField(20);

    private final DefaultTableModel tableModel = new DefaultTableModel(
            new String[]{"ID", "Name", "Type", "Phone", "Email", "Address", "Grid Phase", "Contractor"}, 0) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };
    private final JTable table = new JTable(tableModel);
    private final TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(tableModel);

    public CustomerPanel() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        JLabel title = new JLabel("Customer & Contractor Registration");
        title.setFont(new Font("Segoe UI", Font.BOLD, 24));

        JPanel top = new JPanel(new BorderLayout(0, 8));
        top.add(title, BorderLayout.NORTH);
        top.add(buildForm(), BorderLayout.CENTER);
        top.add(buildButtons(), BorderLayout.SOUTH);
        add(top, BorderLayout.NORTH);
        add(buildTablePanel(), BorderLayout.CENTER);

        loadContractors();
        refresh();
        clearForm();
    }

    // ---------------------------------------------------------------- layout

    private JPanel buildForm() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBorder(BorderFactory.createTitledBorder("Customer details"));

        ButtonGroup group = new ButtonGroup();
        group.add(rdoSingle);
        group.add(rdoThree);
        JPanel phasePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        phasePanel.add(rdoSingle);
        phasePanel.add(rdoThree);

        JButton btnNewContractor = new JButton("New contractor");
        btnNewContractor.addActionListener(e -> onNewContractor());
        JPanel contractorPanel = new JPanel(new BorderLayout(6, 0));
        contractorPanel.add(cmbContractor, BorderLayout.CENTER);
        contractorPanel.add(btnNewContractor, BorderLayout.EAST);

        addField(p, "Name *",                txtName,         0, 0);
        addField(p, "Customer type *",       cmbType,         1, 0);
        addField(p, "Phone *",               txtPhone,        0, 1);
        addField(p, "Email",                 txtEmail,        1, 1);
        addField(p, "Address *",             txtAddress,      0, 2);
        addField(p, "Grid connection *",     phasePanel,      1, 2);
        addField(p, "Installation contractor", contractorPanel, 0, 3);
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
        JLabel hint = new JLabel("   * required");
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
        p.add(new JScrollPane(table), BorderLayout.CENTER);
        return p;
    }

    // ---------------------------------------------------------------- data

    private void loadContractors() {
        try {
            contractorModel.removeAllElements();
            contractorModel.addElement(NO_CONTRACTOR);
            for (Contractor c : dao.findContractors()) {
                contractorModel.addElement(c);
            }
        } catch (Exception ex) {
            LOG.log(Level.SEVERE, "Could not load contractors", ex);
            error("Could not load contractors:\n" + ex.getMessage());
        }
    }

    public void refresh() {
        try {
            customers = dao.findAll();
            tableModel.setRowCount(0);
            for (Customer c : customers) {
                tableModel.addRow(new Object[]{
                    c.getId(), c.getName(), c.getType(), c.getPhone(),
                    c.getEmail() == null ? "" : c.getEmail(), c.getAddress(),
                    "SINGLE".equals(c.getGridPhase()) ? "Single phase" : "Three phase",
                    c.getContractorName() == null ? "-" : c.getContractorName()
                });
            }
        } catch (Exception ex) {
            LOG.log(Level.SEVERE, "Could not load customers", ex);
            error("Could not load customers:\n" + ex.getMessage());
        }
    }

    private void applyFilter() {
        String text = txtSearch.getText().trim();
        sorter.setRowFilter(text.isEmpty() ? null : RowFilter.regexFilter("(?i)" + Pattern.quote(text)));
    }

    // ---------------------------------------------------------------- actions

    private void onNewContractor() {
        ContractorDialog dialog = new ContractorDialog(SwingUtilities.getWindowAncestor(this), dao);
        dialog.setVisible(true);
        Contractor created = dialog.getCreated();
        if (created != null) {
            loadContractors();
            selectContractor(created.getId());
        }
    }

    private void onAdd() {
        try {
            dao.insert(buildCustomerFromForm());
            info("Customer registered successfully.");
            refresh();
            clearForm();
        } catch (ValidationException ex) {
            warn(ex.getMessage());
        } catch (Exception ex) {
            LOG.log(Level.SEVERE, "Add customer failed", ex);
            error("Could not add customer:\n" + ex.getMessage());
        }
    }

    private void onUpdate() {
        if (selected == null) {
            warn("Select a customer from the table first.");
            return;
        }
        try {
            Customer c = buildCustomerFromForm();
            c.setId(selected.getId());
            dao.update(c);
            info("Customer updated successfully.");
            refresh();
            clearForm();
        } catch (ValidationException ex) {
            warn(ex.getMessage());
        } catch (Exception ex) {
            LOG.log(Level.SEVERE, "Update customer failed", ex);
            error("Could not update customer:\n" + ex.getMessage());
        }
    }

    private void onDelete() {
        if (selected == null) {
            warn("Select a customer from the table first.");
            return;
        }
        int ok = JOptionPane.showConfirmDialog(this,
                "Delete customer \"" + selected.getName() + "\"?\nThis cannot be undone.",
                "Confirm delete", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (ok != JOptionPane.YES_OPTION) {
            return;
        }
        try {
            dao.delete(selected.getId());
            info("Customer deleted.");
            refresh();
            clearForm();
        } catch (ValidationException ex) {
            warn(ex.getMessage());
        } catch (Exception ex) {
            LOG.log(Level.SEVERE, "Delete customer failed", ex);
            error("Could not delete customer:\n" + ex.getMessage());
        }
    }

    private void onRowSelected() {
        int viewRow = table.getSelectedRow();
        if (viewRow < 0) {
            return;
        }
        selected = customers.get(table.convertRowIndexToModel(viewRow));
        txtName.setText(selected.getName());
        cmbType.setSelectedItem(selected.getType());
        txtPhone.setText(selected.getPhone());
        txtEmail.setText(selected.getEmail() == null ? "" : selected.getEmail());
        txtAddress.setText(selected.getAddress());
        if ("SINGLE".equals(selected.getGridPhase())) {
            rdoSingle.setSelected(true);
        } else {
            rdoThree.setSelected(true);
        }
        selectContractor(selected.getContractorId());
    }

    // ---------------------------------------------------------------- form logic

    private void selectContractor(int contractorId) {
        for (int i = 0; i < contractorModel.getSize(); i++) {
            if (contractorModel.getElementAt(i).getId() == contractorId) {
                cmbContractor.setSelectedIndex(i);
                return;
            }
        }
        cmbContractor.setSelectedIndex(0);
    }

    private void clearForm() {
        selected = null;
        table.clearSelection();
        txtName.setText("");
        txtPhone.setText("");
        txtEmail.setText("");
        txtAddress.setText("");
        cmbType.setSelectedIndex(0);
        rdoSingle.setSelected(true);
        cmbContractor.setSelectedIndex(0);
        txtName.requestFocus();
    }

    private Customer buildCustomerFromForm() throws ValidationException {
        Customer c = new Customer();

        String name = txtName.getText().trim();
        Validator.requireText("Name", name);
        if (name.length() > 100) throw new ValidationException("Name must be 100 characters or fewer.");
        c.setName(name);

        c.setType((String) cmbType.getSelectedItem());

        String phone = txtPhone.getText().trim();
        Validator.requirePhone(phone);
        c.setPhone(phone);

        String email = txtEmail.getText().trim();
        Validator.requireOptionalEmail(email);
        if (email.length() > 100) throw new ValidationException("Email must be 100 characters or fewer.");
        c.setEmail(email);

        String address = txtAddress.getText().trim();
        Validator.requireText("Address", address);
        if (address.length() > 255) throw new ValidationException("Address must be 255 characters or fewer.");
        c.setAddress(address);

        c.setGridPhase(rdoSingle.isSelected() ? "SINGLE" : "THREE");

        Contractor k = (Contractor) cmbContractor.getSelectedItem();
        c.setContractorId(k == null ? 0 : k.getId());
        return c;
    }

    // ---------------------------------------------------------------- dialogs

    private void info(String msg)  { JOptionPane.showMessageDialog(this, msg, "Success", JOptionPane.INFORMATION_MESSAGE); }
    private void warn(String msg)  { JOptionPane.showMessageDialog(this, msg, "Validation", JOptionPane.WARNING_MESSAGE); }
    private void error(String msg) { JOptionPane.showMessageDialog(this, msg, "Error", JOptionPane.ERROR_MESSAGE); }
}