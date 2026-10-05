package com.mycompany.solarpos.ui;

import com.mycompany.solarpos.dao.UserDAO;
import com.mycompany.solarpos.model.User;
import com.mycompany.solarpos.util.Session;
import com.mycompany.solarpos.util.Validator;
import com.mycompany.solarpos.util.ValidationException;
import java.awt.*;
import java.awt.geom.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.*;

public class LoginForm extends JFrame {

    private static final Logger LOG = Logger.getLogger(LoginForm.class.getName());

    private static final Color INK    = new Color(30, 41, 59);
    private static final Color MUTED  = new Color(100, 116, 139);
    private static final Color ORANGE = new Color(255, 140, 0);
    private static final Color AMBER  = new Color(255, 190, 40);

    private final JTextField txtUser = new JTextField();
    private final JPasswordField txtPass = new JPasswordField();
    private final GradientButton btnLogin = new GradientButton("Sign In");

    public LoginForm() {
        setTitle("Solar POS - Login");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setResizable(false);

        SolarArtPanel art = new SolarArtPanel();
        art.setPreferredSize(new Dimension(470, 580));

        JPanel root = new JPanel(new BorderLayout());
        root.add(art, BorderLayout.WEST);
        root.add(buildFormPanel(), BorderLayout.CENTER);
        setContentPane(root);

        getRootPane().setDefaultButton(btnLogin);   // Enter key = Login
        btnLogin.addActionListener(e -> doLogin());

        pack();
        setLocationRelativeTo(null);
    }

    // ============================================================ form side

    private JPanel buildFormPanel() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBackground(Color.WHITE);
        p.setPreferredSize(new Dimension(430, 580));
        p.setBorder(BorderFactory.createEmptyBorder(20, 50, 20, 50));

        JLabel welcome = new JLabel("Welcome back");
        welcome.setFont(new Font("Segoe UI", Font.BOLD, 30));
        welcome.setForeground(INK);

        JPanel accent = new JPanel();
        accent.setBackground(ORANGE);
        accent.setPreferredSize(new Dimension(48, 4));

        JLabel sub = new JLabel("Sign in to your Solar POS account");
        sub.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        sub.setForeground(MUTED);

        styleField(txtUser, "Enter your username");
        styleField(txtPass, "Enter your password");
        txtPass.putClientProperty("JPasswordField.showRevealButton", true);

        JLabel footer = new JLabel("Solar POS  \u2022  Sales & Quoting System");
        footer.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        footer.setForeground(new Color(148, 163, 184));

        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.WEST;
        int row = 0;

        // top spacer (centres the form vertically)
        c.gridy = row++; c.weighty = 1; c.fill = GridBagConstraints.BOTH;
        p.add(spacer(), c);
        c.weighty = 0; c.fill = GridBagConstraints.HORIZONTAL;

        c.gridy = row++; c.insets = new Insets(0, 0, 8, 0);  p.add(welcome, c);
        c.gridy = row++; c.insets = new Insets(0, 0, 12, 0);
        c.fill = GridBagConstraints.NONE; p.add(accent, c);
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridy = row++; c.insets = new Insets(0, 0, 30, 0); p.add(sub, c);

        c.gridy = row++; c.insets = new Insets(0, 0, 6, 0);  p.add(fieldLabel("USERNAME"), c);
        c.gridy = row++; c.insets = new Insets(0, 0, 18, 0); p.add(txtUser, c);
        c.gridy = row++; c.insets = new Insets(0, 0, 6, 0);  p.add(fieldLabel("PASSWORD"), c);
        c.gridy = row++; c.insets = new Insets(0, 0, 28, 0); p.add(txtPass, c);
        c.gridy = row++; c.insets = new Insets(0, 0, 0, 0);  p.add(btnLogin, c);

        // bottom spacer
        c.gridy = row++; c.weighty = 1; c.fill = GridBagConstraints.BOTH;
        p.add(spacer(), c);
        c.weighty = 0; c.fill = GridBagConstraints.HORIZONTAL;

        c.gridy = row;   c.anchor = GridBagConstraints.CENTER; p.add(footer, c);
        return p;
    }

    private static JPanel spacer() {
        JPanel s = new JPanel();
        s.setOpaque(false);
        return s;
    }

    private static JLabel fieldLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("Segoe UI", Font.BOLD, 11));
        l.setForeground(MUTED);
        return l;
    }

    private static void styleField(JTextField f, String placeholder) {
        f.setPreferredSize(new Dimension(300, 44));
        f.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        f.setBackground(new Color(248, 250, 252));
        f.setForeground(INK);
        f.setCaretColor(INK);
        f.putClientProperty("JTextField.placeholderText", placeholder);
        f.putClientProperty("JComponent.roundRect", true);
        f.putClientProperty("JComponent.outline", null);
    }

    // ============================================================ login logic

    private void doLogin() {
        try {
            String user = txtUser.getText().trim();
            String pass = new String(txtPass.getPassword());
            Validator.requireText("Username", user);
            Validator.requireText("Password", pass);

            User u = new UserDAO().login(user, pass);
            if (u == null) {
                JOptionPane.showMessageDialog(this, "Invalid username or password.",
                        "Login failed", JOptionPane.ERROR_MESSAGE);
                txtPass.setText("");
                txtPass.requestFocus();
                return;
            }
            Session.setCurrentUser(u);
            dispose();
            new MainFrame().setVisible(true);

        } catch (ValidationException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Validation", JOptionPane.WARNING_MESSAGE);
        } catch (Exception ex) {
            LOG.log(Level.SEVERE, "Login error", ex);
            JOptionPane.showMessageDialog(this, "Could not log in:\n" + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ============================================================ gradient button

    private static class GradientButton extends JButton {

        GradientButton(String text) {
            super(text);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setForeground(Color.WHITE);
            setFont(new Font("Segoe UI", Font.BOLD, 15));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setPreferredSize(new Dimension(300, 48));
        }

        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            Color top = new Color(255, 170, 30);
            Color bottom = new Color(240, 110, 0);
            if (!isEnabled()) {
                top = new Color(200, 200, 200);
                bottom = new Color(170, 170, 170);
            } else if (getModel().isPressed()) {
                top = new Color(225, 120, 0);
                bottom = new Color(200, 90, 0);
            } else if (getModel().isRollover()) {
                top = new Color(255, 190, 60);
                bottom = new Color(255, 125, 10);
            }

            g.setPaint(new GradientPaint(0, 0, top, 0, getHeight(), bottom));
            g.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 24, 24));
            g.dispose();

            super.paintComponent(g0);   // draws the text
        }
    }

    // ============================================================ animated artwork

    private static class SolarArtPanel extends JPanel {

        private double t = 0;
        private final javax.swing.Timer timer = new javax.swing.Timer(33, e -> {
            t += 0.033;
            repaint();
        });

        SolarArtPanel() {
            setOpaque(true);
        }

        @Override
        public void addNotify() {
            super.addNotify();
            timer.start();
        }

        @Override
        public void removeNotify() {
            timer.stop();
            super.removeNotify();
        }

        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();

            paintSky(g, w, h);
            paintSun(g, w, h);
            paintClouds(g, w, h);
            turbine(g, w * 0.12, h * 0.72, 120, 48, 1.6, 215);
            turbine(g, w * 0.31, h * 0.70, 80, 34, 2.2, 170);
            paintHills(g, w, h);
            paintPanels(g, w, h);
            paintBranding(g, w, h);

            g.dispose();
        }

        // ---------------------------------------------------- sky & sun

        private void paintSky(Graphics2D g, int w, int h) {
            g.setPaint(new LinearGradientPaint(0, 0, 0, h,
                    new float[]{0f, 0.5f, 0.78f},
                    new Color[]{new Color(24, 64, 140), new Color(255, 153, 51), new Color(255, 214, 102)}));
            g.fillRect(0, 0, w, h);
        }

        private void paintSun(Graphics2D g, int w, int h) {
            float sx = w * 0.74f;
            float sy = h * 0.30f;

            g.setPaint(new RadialGradientPaint(new Point2D.Float(sx, sy), 160f,
                    new float[]{0f, 0.35f, 1f},
                    new Color[]{new Color(255, 244, 170, 230), new Color(255, 200, 60, 90), new Color(255, 170, 0, 0)}));
            g.fill(new Ellipse2D.Float(sx - 160, sy - 160, 320, 320));

            g.setStroke(new BasicStroke(3.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.setColor(new Color(255, 236, 150, 210));
            for (int i = 0; i < 16; i++) {
                double a = i * Math.PI / 8 + t * 0.25;
                double r1 = 58;
                double r2 = (i % 2 == 0 ? 98 : 80) + Math.sin(t * 2 + i) * 4;
                g.draw(new Line2D.Double(
                        sx + Math.cos(a) * r1, sy + Math.sin(a) * r1,
                        sx + Math.cos(a) * r2, sy + Math.sin(a) * r2));
            }

            g.setPaint(new GradientPaint(sx, sy - 44, new Color(255, 247, 190), sx, sy + 44, AMBER));
            g.fill(new Ellipse2D.Float(sx - 44, sy - 44, 88, 88));
        }

        private void paintClouds(Graphics2D g, int w, int h) {
            cloud(g, drift(w, 60, 9), h * 0.14, 1.1);
            cloud(g, drift(w, 280, 6), h * 0.40, 0.8);
            cloud(g, drift(w, 400, 12), h * 0.22, 0.6);
        }

        private double drift(int w, double base, double speed) {
            return ((base + t * speed) % (w + 260)) - 130;
        }

        private void cloud(Graphics2D g, double x, double y, double s) {
            g.setColor(new Color(255, 255, 255, 150));
            g.fill(new Ellipse2D.Double(x, y, 70 * s, 28 * s));
            g.fill(new Ellipse2D.Double(x + 18 * s, y - 14 * s, 44 * s, 34 * s));
            g.fill(new Ellipse2D.Double(x + 40 * s, y - 6 * s, 50 * s, 28 * s));
        }

        // ---------------------------------------------------- wind turbines

        private void turbine(Graphics2D g, double bx, double by, double tower,
                             double blade, double speed, int alpha) {
            g.setColor(new Color(255, 255, 255, alpha));
            g.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            double hx = bx;
            double hy = by - tower;
            g.draw(new Line2D.Double(bx, by, hx, hy));
            for (int k = 0; k < 3; k++) {
                double a = t * speed + k * 2 * Math.PI / 3;
                g.draw(new Line2D.Double(hx, hy, hx + Math.cos(a) * blade, hy + Math.sin(a) * blade));
            }
            g.fill(new Ellipse2D.Double(hx - 3.5, hy - 3.5, 7, 7));
        }

        // ---------------------------------------------------- hills

        private void paintHills(Graphics2D g, int w, int h) {
            // back hill
            Path2D back = new Path2D.Double();
            back.moveTo(0, h * 0.70);
            back.curveTo(w * 0.20, h * 0.63, w * 0.45, h * 0.72, w * 0.70, h * 0.66);
            back.curveTo(w * 0.85, h * 0.63, w * 0.95, h * 0.67, w, h * 0.65);
            back.lineTo(w, h);
            back.lineTo(0, h);
            back.closePath();
            g.setPaint(new GradientPaint(0, (float) (h * 0.62), new Color(48, 140, 110),
                    0, h, new Color(20, 80, 75)));
            g.fill(back);

            // front hill
            Path2D front = new Path2D.Double();
            front.moveTo(0, h * 0.80);
            front.curveTo(w * 0.25, h * 0.72, w * 0.55, h * 0.84, w, h * 0.76);
            front.lineTo(w, h);
            front.lineTo(0, h);
            front.closePath();
            g.setPaint(new GradientPaint(0, (float) (h * 0.72), new Color(24, 100, 85),
                    0, h, new Color(8, 42, 48)));
            g.fill(front);
        }

        // ---------------------------------------------------- solar panels

        private void paintPanels(Graphics2D g, int w, int h) {
            panelRow(g, 5, w * 0.08, w * 0.92, h * 0.735, 64, 28, 7);
            panelRow(g, 3, w * 0.07, w * 0.93, h * 0.815, 118, 52, 13);
        }

        private void panelRow(Graphics2D g, int count, double startX, double endX,
                              double y, double pw, double ph, double skew) {
            double step = (endX - startX - pw) / (count - 1);
            for (int i = 0; i < count; i++) {
                panel(g, startX + i * step, y, pw, ph, skew);
            }
        }

        private void panel(Graphics2D g, double x, double y, double w, double h, double skew) {
            Point2D.Double tl = new Point2D.Double(x + skew, y);
            Point2D.Double tr = new Point2D.Double(x + w - skew, y);
            Point2D.Double br = new Point2D.Double(x + w, y + h);
            Point2D.Double bl = new Point2D.Double(x, y + h);

            Path2D quad = new Path2D.Double();
            quad.moveTo(tl.x, tl.y);
            quad.lineTo(tr.x, tr.y);
            quad.lineTo(br.x, br.y);
            quad.lineTo(bl.x, bl.y);
            quad.closePath();

            // stand
            double cx = x + w / 2;
            g.setColor(new Color(25, 35, 45));
            g.setStroke(new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.draw(new Line2D.Double(cx, y + h, cx, y + h + h * 0.35));

            // cells
            g.setPaint(new GradientPaint((float) x, (float) y, new Color(45, 100, 185),
                    (float) x, (float) (y + h), new Color(12, 35, 90)));
            g.fill(quad);

            // grid lines
            g.setColor(new Color(170, 205, 255, 150));
            g.setStroke(new BasicStroke(1f));
            int cols = 6;
            int rows = 3;
            for (int i = 1; i < cols; i++) {
                double u = i / (double) cols;
                Point2D.Double p1 = pt(tl, tr, bl, br, u, 0);
                Point2D.Double p2 = pt(tl, tr, bl, br, u, 1);
                g.draw(new Line2D.Double(p1, p2));
            }
            for (int j = 1; j < rows; j++) {
                double v = j / (double) rows;
                Point2D.Double p1 = pt(tl, tr, bl, br, 0, v);
                Point2D.Double p2 = pt(tl, tr, bl, br, 1, v);
                g.draw(new Line2D.Double(p1, p2));
            }

            // sunlight glare
            Shape old = g.getClip();
            g.clip(quad);
            g.setPaint(new GradientPaint((float) x, (float) y, new Color(255, 255, 255, 110),
                    (float) (x + w * 0.7), (float) (y + h), new Color(255, 255, 255, 0)));
            g.fill(quad);
            g.setClip(old);

            // frame
            g.setColor(new Color(215, 225, 240));
            g.setStroke(new BasicStroke(1.6f));
            g.draw(quad);
        }

        private static Point2D.Double pt(Point2D.Double tl, Point2D.Double tr,
                                         Point2D.Double bl, Point2D.Double br,
                                         double u, double v) {
            double tx = tl.x + (tr.x - tl.x) * u;
            double ty = tl.y + (tr.y - tl.y) * u;
            double bx = bl.x + (br.x - bl.x) * u;
            double by = bl.y + (br.y - bl.y) * u;
            return new Point2D.Double(tx + (bx - tx) * v, ty + (by - ty) * v);
        }

        // ---------------------------------------------------- branding text

        private void paintBranding(Graphics2D g, int w, int h) {
            // little sun logo
            g.setPaint(new GradientPaint(36, 46, new Color(255, 247, 190), 36, 82, AMBER));
            g.fill(new Ellipse2D.Float(30, 50, 30, 30));
            g.setColor(new Color(255, 255, 255, 220));
            g.setStroke(new BasicStroke(2.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            for (int i = 0; i < 8; i++) {
                double a = i * Math.PI / 4;
                g.draw(new Line2D.Double(45 + Math.cos(a) * 19, 65 + Math.sin(a) * 19,
                        45 + Math.cos(a) * 25, 65 + Math.sin(a) * 25));
            }

            shadowText(g, "SOLAR POS", new Font("Segoe UI", Font.BOLD, 30), 76, 76, Color.WHITE);
            shadowText(g, "Power your business with the sun",
                    new Font("Segoe UI", Font.PLAIN, 14), 32, 112, new Color(255, 255, 255, 235));
            shadowText(g, "Sales  \u2022  Quotes  \u2022  Inventory",
                    new Font("Segoe UI", Font.PLAIN, 12), 32, h - 24, new Color(255, 255, 255, 190));
        }

        private void shadowText(Graphics2D g, String s, Font f, int x, int y, Color c) {
            g.setFont(f);
            g.setColor(new Color(0, 0, 0, 80));
            g.drawString(s, x + 2, y + 2);
            g.setColor(c);
            g.drawString(s, x, y);
        }
    }
}