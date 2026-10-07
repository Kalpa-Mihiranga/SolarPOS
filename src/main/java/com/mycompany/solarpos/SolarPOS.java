package com.mycompany.solarpos;

import com.formdev.flatlaf.FlatLightLaf;
import com.mycompany.solarpos.db.DBConnection;
import com.mycompany.solarpos.ui.LoginForm;
import com.mycompany.solarpos.ui.SetupDialog;
import javax.swing.*;

public class SolarPOS {

    public static void main(String[] args) {
        FlatLightLaf.setup();
        SwingUtilities.invokeLater(() -> {
            try {
                DBConnection db = DBConnection.getInstance();

                // If password is empty, show setup dialog first
                if (db.isPasswordEmpty()) {
                    SetupDialog setup = new SetupDialog(null);
                    setup.setVisible(true);
                    if (!setup.isSaved()) {
                        System.exit(0);
                        return;
                    }
                }

                // Test the connection before opening login
                DBConnection.getInstance().getConnection();
                new LoginForm().setVisible(true);

            } catch (Exception ex) {
                // Config file missing entirely - show setup dialog
                SetupDialog setup = new SetupDialog(null);
                setup.setVisible(true);
                if (setup.isSaved()) {
                    try {
                        DBConnection.getInstance().getConnection();
                        new LoginForm().setVisible(true);
                    } catch (Exception e2) {
                        JOptionPane.showMessageDialog(null,
                                "Could not connect after setup:\n" + e2.getMessage(),
                                "Error", JOptionPane.ERROR_MESSAGE);
                        System.exit(1);
                    }
                }
            }
        });
    }
}