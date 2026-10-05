package com.mycompany.solarpos.ui;

import com.mycompany.solarpos.dao.CustomerDAO;
import com.mycompany.solarpos.model.Contractor;
import com.mycompany.solarpos.util.ValidationException;
import com.mycompany.solarpos.util.Validator;
import java.awt.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.*;

public class ContractorDialog extends JDialog {

    private static final Logger LOG = Logger.getLogger(ContractorDialog.class.getName());

    private final CustomerDAO dao;
    private final JTextField txtName = new JTextField(20);
    private final JTextField txtPhone = new JTextField(20);
    private final JTextField txtLicense = new JTextField(20);
    private Contractor created;

    public ContractorDialog(Window owner, CustomerDAO dao) {
        super(owner, "New Contractor", Dialog.ModalityType.APPLICATION_MODAL);
        this.dao = dao;

        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createEmptyBorder(15, 15, 5, 15));
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(5, 5, 5, 5);
        c.anchor = GridBagConstraints.WEST;

        c.gridx = 0; c.gridy = 0; form.add(new JLabel("Company / name *"), c);
        c.gridx = 1; form.add(txtName, c);
        c.gridx = 0; c.gridy = 1; form.add(new JLabel("Phone *"), c);
        c.gridx = 1; form.add(txtPhone, c);
        c.gridx = 0; c.gridy = 2; form.add(new JLabel("License no. *"), c);
        c.gridx = 1; form.add(txtLicense, c);

        JButton save = new JButton("Save");
        save.setBackground(new Color(39, 174, 96));
        save.setForeground(Color.WHITE);
        JButton cancel = new JButton("Cancel");
        save.addActionListener(e -> onSave());
        cancel.addActionListener(e -> dispose());

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttons.add(cancel);
        buttons.add(save);

        setLayout(new BorderLayout());
        add(form, BorderLayout.CENTER);
        add(buttons, BorderLayout.SOUTH);
        getRootPane().setDefaultButton(save);
        pack();
        setLocationRelativeTo(owner);
    }

    private void onSave() {
        try {
            String name = txtName.getText().trim();
            String phone = txtPhone.getText().trim();
            String license = txtLicense.getText().trim();

            Validator.requireText("Contractor name", name);
            if (name.length() > 100) throw new ValidationException("Name must be 100 characters or fewer.");
            Validator.requirePhone(phone);
            Validator.requireText("License number", license);
            if (license.length() > 50) throw new ValidationException("License number must be 50 characters or fewer.");

            created = dao.insertContractor(name, phone, license);
            dispose();
        } catch (ValidationException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Validation", JOptionPane.WARNING_MESSAGE);
        } catch (Exception ex) {
            LOG.log(Level.SEVERE, "Add contractor failed", ex);
            JOptionPane.showMessageDialog(this, "Could not save contractor:\n" + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    /** The saved contractor, or null if the dialog was cancelled. */
    public Contractor getCreated() { return created; }
}