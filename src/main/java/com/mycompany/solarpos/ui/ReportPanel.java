package com.mycompany.solarpos.ui;

import com.mycompany.solarpos.db.DBConnection;
import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.export.*;
import net.sf.jasperreports.pdf.JRPdfExporter;
import net.sf.jasperreports.pdf.SimplePdfExporterConfiguration;
import java.awt.*;
import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import net.sf.jasperreports.view.JasperViewer;

public class ReportPanel extends JPanel {

    private static final Logger LOG = Logger.getLogger(ReportPanel.class.getName());

    private final JComboBox<Integer> cmbYear    = new JComboBox<>();
    private final JComboBox<Integer> cmbQuarter = new JComboBox<>(new Integer[]{1, 2, 3, 4});
    private final JTextArea          txtLog     = new JTextArea(6, 60);
    private JasperPrint              lastPrint;

    public ReportPanel() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        add(buildHeader(),  BorderLayout.NORTH);
        add(buildCenter(), BorderLayout.CENTER);
        add(buildLog(),    BorderLayout.SOUTH);

        // populate years: last 3 years up to current
        int currentYear = java.time.LocalDate.now().getYear();
        for (int y = currentYear - 2; y <= currentYear; y++) {
            cmbYear.addItem(y);
        }
        cmbYear.setSelectedItem(currentYear);
        cmbQuarter.setSelectedItem(guessCurrentQuarter());
    }

    // ====================================================== layout

    private JPanel buildHeader() {
        JLabel title = new JLabel(
                "\uD83D\uDCC4  Reports");
        title.setFont(new Font("Segoe UI", Font.BOLD, 24));

        JLabel sub = new JLabel(
                "Generate the Quarterly Hardware Turnover & Warranty Liability Report");
        sub.setForeground(Color.GRAY);

        JPanel p = new JPanel(new BorderLayout(0, 4));
        p.add(title, BorderLayout.NORTH);
        p.add(sub,   BorderLayout.CENTER);
        p.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
        return p;
    }

    private JPanel buildCenter() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(0, 120, 212), 1, true),
                "Quarterly Hardware Turnover & Warranty Liability Report"));

        GridBagConstraints c = new GridBagConstraints();
        c.insets  = new Insets(8, 10, 8, 10);
        c.anchor  = GridBagConstraints.WEST;

        // Year and quarter selectors
        c.gridy = 0; c.gridx = 0; p.add(new JLabel("Year:"),    c);
        c.gridx = 1; p.add(cmbYear, c);
        c.gridx = 2; p.add(new JLabel("Quarter:"), c);
        c.gridx = 3; p.add(cmbQuarter, c);

        // Buttons
        JButton btnGenerate = iconButton(
                "\uD83D\uDD0D  Generate & View",
                new Color(0, 120, 212));
        JButton btnPdf = iconButton(
                "\uD83D\uDCC5  Export to PDF",
                new Color(39, 174, 96));
        btnGenerate.addActionListener(e -> onGenerate());
        btnPdf.addActionListener(e -> onExportPdf());

        c.gridy = 1; c.gridx = 0; c.gridwidth = 2; p.add(btnGenerate, c);
        c.gridx = 2; c.gridwidth = 2; p.add(btnPdf, c);

        // Description card
        JTextArea desc = new JTextArea(
                "REPORT PURPOSE\n"
              + "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n\n"
              + "This report joins four tables:\n"
              + "  \u2022 inventory_hardware   — stock levels, prices, warranty terms\n"
              + "  \u2022 sale_details         — what was sold and in what quantity\n"
              + "  \u2022 sales_orders         — when each order was placed\n"
              + "  \u2022 customers            — which buyers took stock off the shelf\n\n"
              + "KEY METRICS\n"
              + "  \u2022 Turnover ratio  = units sold ÷ (stock + units sold)\n"
              + "  \u2022 Capital tied up = remaining stock × unit price\n"
              + "  \u2022 Supplier warranty end date vs. remaining stock\n\n"
              + "MANAGEMENT DECISION\n"
              + "  Items flagged SLOW-MOVING (ratio < 25%) whose supplier warranty\n"
              + "  expires within 6 months should be discounted, bundled or returned\n"
              + "  before the warranty lapses and the capital is permanently tied up.\n\n"
              + "  The summary footer shows the total capital at risk."
        );
        desc.setEditable(false);
        desc.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        desc.setBackground(new Color(248, 248, 255));
        desc.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));

        c.gridy = 2; c.gridx = 0; c.gridwidth = 4;
        c.fill = GridBagConstraints.BOTH;
        c.weightx = 1; c.weighty = 1;
        p.add(new JScrollPane(desc), c);

        return p;
    }

    private JPanel buildLog() {
        txtLog.setEditable(false);
        txtLog.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 11));
        txtLog.setForeground(Color.DARK_GRAY);
        JPanel p = new JPanel(new BorderLayout());
        p.setBorder(BorderFactory.createTitledBorder("Console"));
        p.add(new JScrollPane(txtLog), BorderLayout.CENTER);
        return p;
    }

    // ====================================================== actions

    private void onGenerate() {
        int year    = (Integer) cmbYear.getSelectedItem();
        int quarter = (Integer) cmbQuarter.getSelectedItem();
        log("Generating report for Year=" + year + " Q" + quarter + " ...");

        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        SwingWorker<JasperPrint, Void> worker = new SwingWorker<>() {
            @Override
            protected JasperPrint doInBackground() throws Exception {
                return fillReport(year, quarter);
            }

            @Override
            protected void done() {
                setCursor(Cursor.getDefaultCursor());
                try {
                    lastPrint = get();
                    if (lastPrint.getPages().isEmpty()) {
                        log("Report generated but contains 0 pages. "
                          + "No data for Q" + quarter + " " + year + ".");
                        JOptionPane.showMessageDialog(ReportPanel.this,
                                "No data found for Q" + quarter + " " + year
                              + ".\nTry a different quarter or year.",
                                "No data", JOptionPane.INFORMATION_MESSAGE);
                        return;
                    }
                    log("Done — " + lastPrint.getPages().size() + " page(s). Opening viewer...");
                    JasperViewer viewer = new JasperViewer(lastPrint, false);
                    viewer.setTitle("Quarterly Turnover Report — " + year + " Q" + quarter);
                    viewer.setVisible(true);
                } catch (Exception ex) {
                    LOG.log(Level.SEVERE, "Report generation failed", ex);
                    log("ERROR: " + ex.getMessage());
                    JOptionPane.showMessageDialog(ReportPanel.this,
                            "Report failed:\n" + ex.getMessage(),
                            "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        worker.execute();
    }

    private void onExportPdf() {
        if (lastPrint == null) {
            JOptionPane.showMessageDialog(this,
                    "Generate the report first, then export it.",
                    "No report", JOptionPane.WARNING_MESSAGE);
            return;
        }

        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new File("TurnoverReport_"
                + cmbYear.getSelectedItem() + "_Q" + cmbQuarter.getSelectedItem() + ".pdf"));
        chooser.setFileFilter(new FileNameExtensionFilter("PDF files", "pdf"));
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }

        File file = chooser.getSelectedFile();
        if (!file.getName().toLowerCase().endsWith(".pdf")) {
            file = new File(file.getAbsolutePath() + ".pdf");
        }

        log("Exporting to " + file.getAbsolutePath() + " ...");
        final File finalFile = file;
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() throws Exception {
                JRPdfExporter exporter = new JRPdfExporter();
                exporter.setExporterInput(new SimpleExporterInput(lastPrint));
                exporter.setExporterOutput(
                        new SimpleOutputStreamExporterOutput(finalFile));
                SimplePdfExporterConfiguration cfg = new SimplePdfExporterConfiguration();
                cfg.setMetadataAuthor("Solar POS");
                cfg.setMetadataTitle("Hardware Turnover Report");
                exporter.setConfiguration(cfg);
                exporter.exportReport();
                return null;
            }

            @Override
            protected void done() {
                setCursor(Cursor.getDefaultCursor());
                try {
                    get();
                    log("PDF saved: " + finalFile.getAbsolutePath());
                    int ok = JOptionPane.showConfirmDialog(ReportPanel.this,
                            "PDF saved.\nOpen it now?",
                            "Export complete", JOptionPane.YES_NO_OPTION);
                    if (ok == JOptionPane.YES_OPTION) {
                        Desktop.getDesktop().open(finalFile);
                    }
                } catch (Exception ex) {
                    LOG.log(Level.SEVERE, "PDF export failed", ex);
                    log("ERROR: " + ex.getMessage());
                    JOptionPane.showMessageDialog(ReportPanel.this,
                            "PDF export failed:\n" + ex.getMessage(),
                            "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        worker.execute();
    }

    // ====================================================== Jasper

    private JasperPrint fillReport(int year, int quarter) throws Exception {
        try (InputStream is = getClass().getResourceAsStream("/reports/turnover.jrxml")) {
            if (is == null) {
                throw new FileNotFoundException(
                        "turnover.jrxml not found in /reports/. "
                      + "Copy it to src/main/resources/reports/ and rebuild.");
            }
            JasperReport jr = JasperCompileManager.compileReport(is);
            Map<String, Object> params = new HashMap<>();
            params.put("P_YEAR",    year);
            params.put("P_QUARTER", quarter);
            return JasperFillManager.fillReport(
                    jr,
                    params,
                   DBConnection.getInstance().getConnection()); // already inside try-catch Exception
        }
    }

    // ====================================================== helpers

    private void log(String message) {
        SwingUtilities.invokeLater(() -> {
            txtLog.append("[" + java.time.LocalTime.now()
                    .format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss"))
                    + "] " + message + "\n");
            txtLog.setCaretPosition(txtLog.getDocument().getLength());
        });
    }

    private static int guessCurrentQuarter() {
        return (java.time.LocalDate.now().getMonthValue() - 1) / 3 + 1;
    }

    private static JButton iconButton(String text, Color bg) {
        JButton b = new JButton(text);
        b.setBackground(bg);
        b.setForeground(Color.WHITE);
        b.setFocusPainted(false);
        b.setPreferredSize(new Dimension(200, 38));
        return b;
    }
}