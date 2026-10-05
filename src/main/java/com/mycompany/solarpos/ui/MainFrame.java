package com.mycompany.solarpos.ui;

import com.mycompany.solarpos.model.User;
import com.mycompany.solarpos.util.Session;
import java.awt.*;
import java.util.function.Supplier;
import javax.swing.*;

public class MainFrame extends JFrame {

    private static final Color SIDEBAR = new Color(28, 40, 51);

    // Icon code points (safe: no manual surrogate pairs)
    private static final int ICON_DASHBOARD = 0x2600;   // sun
    private static final int ICON_SALE      = 0x1F6D2;  // shopping cart
    private static final int ICON_CUSTOMERS = 0x1F465;  // busts in silhouette
    private static final int ICON_INVENTORY = 0x1F4E6;  // package
    private static final int ICON_BALANCE   = 0x1F4B3;  // credit card
    private static final int ICON_HISTORY   = 0x1F9FE;  // receipt
    private static final int ICON_PROFIT    = 0x1F4B9;  // chart with yen/upward trend
    private static final int ICON_ANALYTICS = 0x1F4CA;  // bar chart
    private static final int ICON_STOCK     = 0x1F4B0;  // money bag
    private static final int ICON_USERS     = 0x1F464;  // bust in silhouette
    private static final int ICON_REPORTS   = 0x1F4C4;  // page facing up
    private static final int ICON_LOGOUT    = 0x1F6AA;  // door

    private final CardLayout cards = new CardLayout();
    private final JPanel content = new JPanel(cards);
    private final JPanel sidebar = new JPanel();

    public MainFrame() {
        User user = Session.getCurrentUser();
        setTitle("Solar POS - " + user.getUsername() + " (" + user.getRole() + ")");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1250, 780);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        buildSidebar(user);
        add(sidebar, BorderLayout.WEST);
        add(content, BorderLayout.CENTER);

        showScreen("DASHBOARD");
    }

    /** Builds "icon  label" from a Unicode code point. */
    private static String icon(int codePoint, String label) {
        return new String(Character.toChars(codePoint)) + "  " + label;
    }

    private void buildSidebar(User user) {
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setBackground(SIDEBAR);
        sidebar.setPreferredSize(new Dimension(210, 0));
        sidebar.setBorder(BorderFactory.createEmptyBorder(15, 10, 15, 10));

        JLabel logo = new JLabel("SOLAR POS");
        logo.setForeground(Color.WHITE);
        logo.setFont(new Font("Segoe UI", Font.BOLD, 20));
        logo.setAlignmentX(Component.LEFT_ALIGNMENT);
        sidebar.add(logo);
        sidebar.add(Box.createVerticalStrut(25));

        // ---- All users
        addScreen(icon(ICON_DASHBOARD, "Dashboard"),         "DASHBOARD", DashboardPanel::new,          false, user);
        addScreen(icon(ICON_SALE,      "New Sale"),          "POS",       PosPanel::new,                false, user);
        addScreen(icon(ICON_CUSTOMERS, "Customers"),         "CUSTOMERS", CustomerPanel::new,           false, user);
        addScreen(icon(ICON_INVENTORY, "Inventory"),         "INVENTORY", InventoryPanel::new,          false, user);
        addScreen(icon(ICON_BALANCE,   "Collect Balance"),   "BALANCE",   BalanceCollectionPanel::new,  false, user);

        // ---- Admin section
        if (user.isAdmin()) {
            JLabel sep = new JLabel("  \u2015\u2015 ADMIN \u2015\u2015");
            sep.setForeground(new Color(127, 140, 141));
            sep.setFont(new Font("Segoe UI", Font.BOLD, 10));
            sep.setAlignmentX(Component.LEFT_ALIGNMENT);
            sep.setBorder(BorderFactory.createEmptyBorder(12, 0, 4, 0));
            sidebar.add(sep);
            sidebar.add(Box.createVerticalStrut(2));
        }

        addScreen(icon(ICON_HISTORY,   "Sales History"),     "SALES",     SalesHistoryPanel::new,       true, user);
        addScreen(icon(ICON_PROFIT,    "Profit & Loss"),     "PROFIT",    ProfitPanel::new,             true, user);
        addScreen(icon(ICON_ANALYTICS, "Analytics"),         "ANALYTICS", CustomerAnalyticsPanel::new,  true, user);
        addScreen(icon(ICON_STOCK,     "Stock Valuation"),   "STOCK",     StockValuationPanel::new,     true, user);
        addScreen(icon(ICON_USERS,     "Users"),             "USERS",     UserManagementPanel::new,     true, user);
        addScreen(icon(ICON_REPORTS,   "Reports"),           "REPORTS",   ReportPanel::new,             true, user);

        sidebar.add(Box.createVerticalGlue());

        JLabel who = new JLabel(user.getUsername() + " (" + user.getRole() + ")");
        who.setForeground(new Color(170, 183, 191));
        who.setAlignmentX(Component.LEFT_ALIGNMENT);
        sidebar.add(who);
        sidebar.add(Box.createVerticalStrut(8));

        JButton logout = navButton(icon(ICON_LOGOUT, "Logout"));
        logout.setBackground(new Color(192, 57, 43));
        logout.addActionListener(e -> logout());
        sidebar.add(logout);
    }

    /**
     * Adds a sidebar button + its screen. Admin-only screens are skipped for
     * cashiers, and their panels are never even created.
     */
    private void addScreen(String label, String key, Supplier<? extends JPanel> panelFactory,
                           boolean adminOnly, User user) {
        if (adminOnly && !user.isAdmin()) {
            return;
        }
        content.add(panelFactory.get(), key);
        JButton b = navButton(label);
        b.addActionListener(e -> showScreen(key));
        sidebar.add(b);
        sidebar.add(Box.createVerticalStrut(6));
    }

    private JButton navButton(String text) {
        JButton b = new JButton(text);
        b.setAlignmentX(Component.LEFT_ALIGNMENT);
        b.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        b.setHorizontalAlignment(SwingConstants.LEFT);
        b.setFocusPainted(false);
        b.setForeground(Color.WHITE);
        b.setBackground(new Color(44, 62, 80));
        return b;
    }

    public void showScreen(String key) {
        cards.show(content, key);
    }

    private void logout() {
        int ok = JOptionPane.showConfirmDialog(this, "Log out now?", "Logout", JOptionPane.YES_NO_OPTION);
        if (ok == JOptionPane.YES_OPTION) {
            Session.clear();
            dispose();
            new LoginForm().setVisible(true);
        }
    }
}