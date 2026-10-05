package com.mycompany.solarpos.ui;

import com.mycompany.solarpos.dao.UserManagementDAO;
import com.mycompany.solarpos.util.Session;
import com.mycompany.solarpos.util.ValidationException;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.*;
import javax.swing.table.*;

public class UserManagementPanel extends JPanel {

    private static final Logger LOG = Logger.getLogger(UserManagementPanel.class.getName());
    private static final Color BLUE  = new Color(0, 120, 212);
    private static final Color GREEN = new Color(39, 174, 96);
    private static final Color RED   = new Color(192, 57, 43);

    private final UserManagementDAO dao = new UserManagementDAO();

    private final DefaultTableModel model = new DefaultTableModel(
        new String[]{"ID","Username","Role","Status","Orders","Last Sale"}, 0) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };
    private final JTable table = new JTable(model);
    private List<String[]> rows;

    // New user form
    private final JTextField     txtNewUser  = new JTextField(14);
    private final JPasswordField txtNewPass  = new JPasswordField(14);
    private final JComboBox<String> cmbRole  = new JComboBox<>(new String[]{"CASHIER","ADMIN"});

    public UserManagementPanel() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        JLabel title = new JLabel("\uD83D\uDC64  User Management");
        title.setFont(new Font("Segoe UI", Font.BOLD, 24));

        add(title, BorderLayout.NORTH);
        add(buildBody(), BorderLayout.CENTER);

        addComponentListener(new ComponentAdapter() {
            @Override public void componentShown(ComponentEvent e) { load(); }
        });
        load();
    }

    private JPanel buildBody() {
        JPanel p = new JPanel(new BorderLayout(10, 10));
        p.add(buildTablePanel(), BorderLayout.CENTER);
        p.add(buildFormPanel(),  BorderLayout.EAST);
        return p;
    }

    private JPanel buildTablePanel() {
        table.setRowHeight(28);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getTableHeader().setReorderingAllowed(false);

        // Status column renderer
        table.getColumnModel().getColumn(3).setCellRenderer(
                new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object v, boolean sel,
                                                           boolean focus, int r, int c) {
                super.getTableCellRendererComponent(t, v, sel, focus, r, c);
                if (!sel && v != null) {
                    boolean active = "Active".equals(v.toString());
                    setForeground(active ? GREEN : RED);
                    setFont(getFont().deriveFont(Font.BOLD));
                    setText(active ? "✓ Active" : "✗ Inactive");
                }
                return this;
            }
        });

        JButton btnActivate   = btn("✓ Activate",    GREEN);
        JButton btnDeactivate = btn("✗ Deactivate",  RED);
        JButton btnReset      = btn("🔑 Reset password", BLUE);
        JButton btnRefresh    = btn("↺ Refresh",     Color.GRAY);

        btnActivate.addActionListener(e -> setActive(true));
        btnDeactivate.addActionListener(e -> setActive(false));
        btnReset.addActionListener(e -> doResetPassword());
        btnRefresh.addActionListener(e -> load());

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        buttons.add(btnActivate);
        buttons.add(btnDeactivate);
        buttons.add(btnReset);
        buttons.add(btnRefresh);

        JLabel hint = new JLabel(
                "  \uD83D\uDCA1 Inactive users cannot log in. "
              + "You cannot deactivate your own account.");
        hint.setForeground(Color.GRAY);
        hint.setFont(new Font("Segoe UI", Font.ITALIC, 11));

        JPanel p = new JPanel(new BorderLayout(0, 6));
        p.setBorder(BorderFactory.createTitledBorder("System users"));
        p.add(new JScrollPane(table), BorderLayout.CENTER);
        p.add(buttons,  BorderLayout.NORTH);
        p.add(hint,     BorderLayout.SOUTH);
        return p;
    }

    private JPanel buildFormPanel() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBorder(BorderFactory.createTitledBorder("Add new user"));
        p.setPreferredSize(new Dimension(240, 0));

        GridBagConstraints c = new GridBagConstraints();
        c.insets  = new Insets(6, 8, 6, 8);
        c.anchor  = GridBagConstraints.WEST;
        c.fill    = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;

        c.gridy = 0; c.gridx = 0; p.add(new JLabel("Username"), c);
        c.gridy = 1; p.add(txtNewUser,  c);
        c.gridy = 2; p.add(new JLabel("Password (min 6 chars)"), c);
        c.gridy = 3; p.add(txtNewPass,  c);
        c.gridy = 4; p.add(new JLabel("Role"), c);
        c.gridy = 5; p.add(cmbRole,     c);

        JButton btnAdd = new JButton("Add user");
        btnAdd.setBackground(GREEN);
        btnAdd.setForeground(Color.WHITE);
        btnAdd.setFocusPainted(false);
        btnAdd.addActionListener(e -> addUser());
        c.gridy = 6;
        c.insets = new Insets(14, 8, 6, 8);
        p.add(btnAdd, c);

        // Fill remaining space
        c.gridy = 7; c.weighty = 1;
        p.add(Box.createGlue(), c);
        return p;
    }

    private void load() {
        try {
            rows = dao.findAll();
            model.setRowCount(0);
            rows.forEach(model::addRow);
        } catch (Exception ex) {
            LOG.log(Level.SEVERE, "Load users failed", ex);
            error("Could not load users: " + ex.getMessage());
        }
    }

    private void setActive(boolean active) {
        int row = table.getSelectedRow();
        if (row < 0) { warn("Select a user first."); return; }
        String[] r = rows.get(row);
        int userId = Integer.parseInt(r[0]);
        if (userId == Session.getCurrentUser().getId()) {
            warn("You cannot change your own account status.");
            return;
        }
        String action = active ? "activate" : "deactivate";
        int ok = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to " + action + " user \"" + r[1] + "\"?",
                "Confirm", JOptionPane.YES_NO_OPTION);
        if (ok != JOptionPane.YES_OPTION) return;
        try {
            dao.setActive(userId, active);
            load();
            info("User " + r[1] + " has been " + action + "d.");
        } catch (Exception ex) {
            LOG.log(Level.SEVERE, "Set active failed", ex);
            error("Could not update user: " + ex.getMessage());
        }
    }

    private void doResetPassword() {
        int row = table.getSelectedRow();
        if (row < 0) { warn("Select a user first."); return; }
        String[] r = rows.get(row);
        JPasswordField pwd = new JPasswordField(16);
        int ok = JOptionPane.showConfirmDialog(this, new Object[]{
                "New password for \"" + r[1] + "\":", pwd},
                "Reset password", JOptionPane.OK_CANCEL_OPTION);
        if (ok != JOptionPane.OK_OPTION) return;
        try {
            dao.resetPassword(Integer.parseInt(r[0]), new String(pwd.getPassword()));
            info("Password reset for " + r[1] + ".");
        } catch (ValidationException ex) {
            warn(ex.getMessage());
        } catch (Exception ex) {
            LOG.log(Level.SEVERE, "Reset password failed", ex);
            error("Could not reset password: " + ex.getMessage());
        }
    }

    private void addUser() {
        try {
            String username = txtNewUser.getText().trim();
            String password = new String(txtNewPass.getPassword());
            String role     = (String) cmbRole.getSelectedItem();
            dao.createUser(username, password, role);
            txtNewUser.setText("");
            txtNewPass.setText("");
            cmbRole.setSelectedIndex(0);
            load();
            info("User \"" + username + "\" created successfully.");
        } catch (ValidationException ex) {
            warn(ex.getMessage());
        } catch (Exception ex) {
            LOG.log(Level.SEVERE, "Create user failed", ex);
            error("Could not create user: " + ex.getMessage());
        }
    }

    private static JButton btn(String t, Color bg) {
        JButton b = new JButton(t);
        b.setBackground(bg);
        b.setForeground(Color.WHITE);
        b.setFocusPainted(false);
        return b;
    }

    private void info(String m)  { JOptionPane.showMessageDialog(this, m, "Success", JOptionPane.INFORMATION_MESSAGE); }
    private void warn(String m)  { JOptionPane.showMessageDialog(this, m, "Validation", JOptionPane.WARNING_MESSAGE); }
    private void error(String m) { JOptionPane.showMessageDialog(this, m, "Error", JOptionPane.ERROR_MESSAGE); }
}