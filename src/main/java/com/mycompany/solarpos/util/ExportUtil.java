package com.mycompany.solarpos.util;

import java.awt.Desktop;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JTable;
import javax.swing.table.TableModel;

public class ExportUtil {

    private ExportUtil() {}

    public static void exportToCSV(JTable table, String defaultName) {
        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new File(defaultName + ".csv"));
        if (chooser.showSaveDialog(table) != JFileChooser.APPROVE_OPTION) {
            return;
        }

        File file = chooser.getSelectedFile();
        if (!file.getName().toLowerCase().endsWith(".csv")) {
            file = new File(file.getPath() + ".csv");
        }

        try (PrintWriter pw = new PrintWriter(
                new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8))) {
            
            TableModel m = table.getModel();
            
            // Escribir encabezados
            StringBuilder header = new StringBuilder();
            for (int c = 0; c < table.getColumnCount(); c++) {
                if (c > 0) {
                    header.append(',');
                }
                header.append('"').append(table.getColumnName(c)).append('"');
            }
            pw.println(header);

            // Escribir filas respetando el orden y filtrado visual de la tabla
            for (int r = 0; r < table.getRowCount(); r++) {
                StringBuilder row = new StringBuilder();
                for (int c = 0; c < table.getColumnCount(); c++) {
                    if (c > 0) {
                        row.append(',');
                    }
                    Object val = table.getValueAt(r, c);
                    String s = (val == null) ? "" : val.toString().replace("\"", "\"\"");
                    row.append('"').append(s).append('"');
                }
                pw.println(row);
            }

            JOptionPane.showMessageDialog(table,
                    "CSV guardado exitosamente:\n" + file.getAbsolutePath(),
                    "Exportación completa", JOptionPane.INFORMATION_MESSAGE);

            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
                Desktop.getDesktop().open(file);
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(table,
                    "Error al exportar: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}