package com.mycompany.solarpos;

import com.formdev.flatlaf.FlatLightLaf;
import com.mycompany.solarpos.ui.LoginForm;
import javax.swing.SwingUtilities;

public class SolarPOS {

    public static void main(String[] args) {
        FlatLightLaf.setup();
        SwingUtilities.invokeLater(() -> new LoginForm().setVisible(true));
    }
}