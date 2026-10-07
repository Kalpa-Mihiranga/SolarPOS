package com.mycompany.solarpos.ui;

import com.mycompany.solarpos.db.DBConnection;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.sql.*;
import javax.swing.*;
import javax.swing.border.*;

public class SetupDialog extends JDialog {

    private static final Color BLUE    = new Color(0, 120, 212);
    private static final Color GREEN   = new Color(39, 174, 96);
    private static final Color RED     = new Color(192, 57, 43);
    private static final Color ORANGE  = new Color(230, 126, 34);
    private static final Color BG      = new Color(245, 247, 250);

    private final JTextField     txtHost    = new JTextField("localhost", 18);
    private final JTextField     txtPort    = new JTextField("3306", 6);
    private final JTextField     txtDB      = new JTextField("solar_pos", 18);
    private final JTextField     txtUser    = new JTextField("root", 18);
    private final JPasswordField txtPass    = new JPasswordField(18);
    private final JLabel         lblStatus  = new JLabel(" ");
    private final JButton        btnTest    = new JButton("Test Connection");
    private final JButton        btnSave    = new JButton("Save & Continue");
    private final JProgressBar   progress   = new JProgressBar();

    private boolean saved = false;

    public SetupDialog(Window owner) {
        super(owner, "Solar POS — Database Setup",
              Dialog.ModalityType.APPLICATION_MODAL);
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                int ok = JOptionPane.showConfirmDialog(SetupDialog.this,
                        "The application cannot start without a database connection.\nExit?",
                        "Exit", JOptionPane.YES_NO_OPTION);
                if (ok == JOptionPane.YES_OPTION) System.exit(0);
            }
        });
        buildUI();
        pack();
        setResizable(false);
        setLocationRelativeTo(owner);
    }

    private void buildUI() {
        // Left panel - branding
        JPanel left = new JPanel(new BorderLayout());
        left.setBackground(BLUE);
        left.setPreferredSize(new Dimension(220, 0));
        left.setBorder(BorderFactory.createEmptyBorder(30, 20, 30, 20));

        JLabel icon = new JLabel("\u2600", SwingConstants.CENTER);
        icon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 60));
        icon.setForeground(Color.WHITE);

        JLabel appName = new JLabel("SOLAR POS", SwingConstants.CENTER);
        appName.setFont(new Font("Segoe UI", Font.BOLD, 22));
        appName.setForeground(Color.WHITE);

        JLabel tagline = new JLabel(
                "<html><center>First-time<br>Database Setup</center></html>",
                SwingConstants.CENTER);
        tagline.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        tagline.setForeground(new Color(200, 220, 255));

        JPanel leftContent = new JPanel();
        leftContent.setLayout(new BoxLayout(leftContent, BoxLayout.Y_AXIS));
        leftContent.setOpaque(false);
        leftContent.add(Box.createVerticalGlue());
        icon.setAlignmentX(Component.CENTER_ALIGNMENT);
        appName.setAlignmentX(Component.CENTER_ALIGNMENT);
        tagline.setAlignmentX(Component.CENTER_ALIGNMENT);
        leftContent.add(icon);
        leftContent.add(Box.createVerticalStrut(10));
        leftContent.add(appName);
        leftContent.add(Box.createVerticalStrut(8));
        leftContent.add(tagline);
        leftContent.add(Box.createVerticalGlue());

        // Steps hint
        JLabel steps = new JLabel(
            "<html><font color='#aaccff'>"
          + "Steps:<br><br>"
          + "1. Enter your MySQL<br>&nbsp;&nbsp;&nbsp;password<br><br>"
          + "2. Click Test Connection<br><br>"
          + "3. Click Save &amp; Continue"
          + "</font></html>");
        steps.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        steps.setAlignmentX(Component.CENTER_ALIGNMENT);
        leftContent.add(steps);
        leftContent.add(Box.createVerticalStrut(20));
        left.add(leftContent, BorderLayout.CENTER);

        // Right panel - form
        JPanel right = new JPanel(new GridBagLayout());
        right.setBackground(BG);
        right.setBorder(BorderFactory.createEmptyBorder(30, 30, 20, 30));

        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(6, 4, 6, 4);
        c.anchor = GridBagConstraints.WEST;
        c.fill   = GridBagConstraints.HORIZONTAL;

        // Title
        JLabel title = new JLabel("Database Configuration");
        title.setFont(new Font("Segoe UI", Font.BOLD, 20));
        title.setForeground(new Color(30, 50, 80));
        c.gridx = 0; c.gridy = 0; c.gridwidth = 2; c.weightx = 1;
        right.add(title, c);

        JLabel subtitle = new JLabel(
                "Enter your MySQL connection details below.");
        subtitle.setForeground(Color.GRAY);
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        c.gridy = 1;
        right.add(subtitle, c);

        // Separator
        c.gridy = 2;
        right.add(new JSeparator(), c);

        // Host + Port on one row
        c.gridwidth = 1; c.weightx = 0;
        c.gridy = 3; c.gridx = 0;
        right.add(label("Host"), c);
        c.gridx = 1; c.weightx = 1;
        JPanel hostPort = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        hostPort.setOpaque(false);
        hostPort.add(txtHost);
        hostPort.add(new JLabel("Port:"));
        hostPort.add(txtPort);
        right.add(hostPort, c);

        // Database name
        c.gridy = 4; c.gridx = 0; c.weightx = 0;
        right.add(label("Database"), c);
        c.gridx = 1; c.weightx = 1;
        right.add(txtDB, c);

        // Username
        c.gridy = 5; c.gridx = 0; c.weightx = 0;
        right.add(label("Username"), c);
        c.gridx = 1; c.weightx = 1;
        right.add(txtUser, c);

        // Password  ← the main field
        c.gridy = 6; c.gridx = 0; c.weightx = 0;
        right.add(label("Password *"), c);
        c.gridx = 1; c.weightx = 1;

        JPanel passPanel = new JPanel(new BorderLayout(6, 0));
        passPanel.setOpaque(false);
        JCheckBox showPass = new JCheckBox("Show");
        showPass.setOpaque(false);
        showPass.addActionListener(e -> {
            txtPass.setEchoChar(showPass.isSelected() ? (char) 0 : '•');
        });
        passPanel.add(txtPass,   BorderLayout.CENTER);
        passPanel.add(showPass,  BorderLayout.EAST);
        right.add(passPanel, c);

        // Hint about empty password
        c.gridy = 7; c.gridx = 1; c.weightx = 1;
        JLabel passHint = new JLabel(
                "Leave blank if MySQL has no root password.");
        passHint.setFont(new Font("Segoe UI", Font.ITALIC, 11));
        passHint.setForeground(ORANGE);
        right.add(passHint, c);

        // Progress bar (hidden until testing)
        progress.setIndeterminate(true);
        progress.setVisible(false);
        c.gridy = 8; c.gridx = 0; c.gridwidth = 2;
        right.add(progress, c);

        // Status label
        lblStatus.setFont(new Font("Segoe UI", Font.BOLD, 12));
        c.gridy = 9;
        right.add(lblStatus, c);

        // Buttons
        styleButton(btnTest, BLUE);
        styleButton(btnSave, GREEN);
        btnSave.setEnabled(false);

        btnTest.addActionListener(e -> testConnection());
        btnSave.addActionListener(e -> saveAndContinue());

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        buttons.setOpaque(false);
        buttons.add(btnTest);
        buttons.add(btnSave);

        c.gridy = 10; c.insets = new Insets(14, 4, 6, 4);
        right.add(buttons, c);

        // MySQL install hint
        JLabel mysqlHint = new JLabel(
            "<html><font color='gray' size='2'>"
          + "Don't have MySQL? Download from "
          + "<a href=''>dev.mysql.com/downloads</a>"
          + "</font></html>");
        mysqlHint.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        c.gridy = 11; c.insets = new Insets(4, 4, 4, 4);
        right.add(mysqlHint, c);

        // Assemble
        JPanel root = new JPanel(new BorderLayout());
        root.add(left,  BorderLayout.WEST);
        root.add(right, BorderLayout.CENTER);
        setContentPane(root);

        // Enter key on password field triggers test
        txtPass.addActionListener(e -> testConnection());
    }

    // -------------------------------------------------------- actions

    private void testConnection() {
        setStatus("Testing connection...", Color.GRAY);
        btnTest.setEnabled(false);
        btnSave.setEnabled(false);
        progress.setVisible(true);

        String host = txtHost.getText().trim();
        String port = txtPort.getText().trim();
        String db   = txtDB.getText().trim();
        String user = txtUser.getText().trim();
        String pass = new String(txtPass.getPassword());

        String url  = "jdbc:mysql://" + host + ":" + port + "/" + db
                    + "?useSSL=false&allowPublicKeyRetrieval=true"
                    + "&serverTimezone=Asia/Colombo";

        SwingWorker<String, Void> worker = new SwingWorker<>() {
            @Override
            protected String doInBackground() {
                try (Connection con = java.sql.DriverManager.getConnection(
                        url, user, pass)) {
                    return "OK:" + con.getCatalog();
                } catch (SQLException ex) {
                    return "ERR:" + ex.getMessage();
                }
            }

            @Override
            protected void done() {
                progress.setVisible(false);
                btnTest.setEnabled(true);
                try {
                    String result = get();
                    if (result.startsWith("OK:")) {
                        setStatus(
                            "\u2714  Connected to database: " + result.substring(3),
                            GREEN);
                        btnSave.setEnabled(true);
                    } else {
                        String msg = result.substring(4);
                        if (msg.contains("using password: NO")) {
                            setStatus(
                                "\u2716  Wrong password or no password set."
                              + " Try leaving the password blank.", RED);
                        } else if (msg.contains("Unknown database")) {
                            setStatus(
                                "\u2716  Database 'solar_pos' not found. "
                              + "Run database/solar_pos_full.sql in MySQL Workbench first.",
                                RED);
                        } else if (msg.contains("Connection refused")) {
                            setStatus(
                                "\u2716  Cannot reach MySQL. "
                              + "Make sure MySQL is running (check Services).",
                                RED);
                        } else {
                            setStatus("\u2716  " + msg, RED);
                        }
                    }
                } catch (Exception ex) {
                    setStatus("\u2716  " + ex.getMessage(), RED);
                    progress.setVisible(false);
                    btnTest.setEnabled(true);
                }
            }
        };
        worker.execute();
    }

    private void saveAndContinue() {
        String host = txtHost.getText().trim();
        String port = txtPort.getText().trim();
        String db   = txtDB.getText().trim();
        String user = txtUser.getText().trim();
        String pass = new String(txtPass.getPassword());

        try {
            // Write config.properties next to JAR or in working directory
            File configFile = DBConnection.findConfigFile();

            java.util.Properties p = new java.util.Properties();
            p.setProperty("db.url",
                    "jdbc:mysql://" + host + ":" + port + "/" + db
                  + "?useSSL=false&allowPublicKeyRetrieval=true"
                  + "&serverTimezone=Asia/Colombo");
            p.setProperty("db.user",     user);
            p.setProperty("db.password", pass);

            try (OutputStream out = new FileOutputStream(configFile)) {
                p.store(out, "Solar POS - saved by setup dialog");
            }

            // Reset singleton so it reloads the new config
            DBConnection.reset();

            setStatus("\u2714  Settings saved. Starting application...", GREEN);
            saved = true;

            // Small delay so the user sees the success message
            Timer timer = new Timer(800, e -> dispose());
            timer.setRepeats(false);
            timer.start();

        } catch (Exception ex) {
            setStatus("\u2716  Could not save: " + ex.getMessage(), RED);
        }
    }

    // -------------------------------------------------------- helpers

    private void setStatus(String text, Color color) {
        lblStatus.setText(text);
        lblStatus.setForeground(color);
    }

    private static JLabel label(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("Segoe UI", Font.BOLD, 12));
        return l;
    }

    private static void styleButton(JButton b, Color bg) {
        b.setBackground(bg);
        b.setForeground(Color.WHITE);
        b.setFocusPainted(false);
        b.setFont(new Font("Segoe UI", Font.BOLD, 13));
        b.setBorder(BorderFactory.createEmptyBorder(8, 18, 8, 18));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    public boolean isSaved() { return saved; }
}