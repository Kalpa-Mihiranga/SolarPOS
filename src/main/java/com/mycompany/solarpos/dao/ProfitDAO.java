package com.mycompany.solarpos.dao;

import com.mycompany.solarpos.db.DBConnection;
import java.sql.*;
import java.util.*;

public class ProfitDAO {

    private Connection con() throws SQLException {
        return DBConnection.getInstance().getConnection();
    }

    /** Monthly P&L for the last N months. */
    public List<String[]> monthlyPL(int months) throws SQLException {
        String sql =
            "SELECT DATE_FORMAT(o.order_date,'%Y-%m') AS period, "
          + "COUNT(DISTINCT o.order_id)          AS orders, "
          + "SUM(d.line_total)                   AS revenue, "
          + "SUM(h.cost_price * d.qty)           AS cogs, "
          + "SUM(d.line_total) - SUM(h.cost_price * d.qty) AS gross_profit, "
          + "SUM(o.tax)                          AS tax_collected, "
          + "SUM(o.discount)                     AS discounts, "
          + "SUM(o.warranty_fees)                AS warranty_income "
          + "FROM sales_orders o "
          + "JOIN sale_details d ON d.order_id = o.order_id "
          + "JOIN inventory_hardware h ON h.item_id = d.item_id "
          + "WHERE o.order_date >= DATE_SUB(CURDATE(), INTERVAL ? MONTH) "
          + "GROUP BY period ORDER BY period DESC";
        List<String[]> rows = new ArrayList<>();
        try (PreparedStatement ps = con().prepareStatement(sql)) {
            ps.setInt(1, months);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    double revenue = rs.getDouble("revenue");
                    double cogs    = rs.getDouble("cogs");
                    double gross   = rs.getDouble("gross_profit");
                    double margin  = revenue > 0 ? gross / revenue * 100 : 0;
                    rows.add(new String[]{
                        rs.getString("period"),
                        rs.getString("orders"),
                        String.format("%,.2f", revenue),
                        String.format("%,.2f", cogs),
                        String.format("%,.2f", gross),
                        String.format("%.1f%%", margin),
                        String.format("%,.2f", rs.getDouble("tax_collected")),
                        String.format("%,.2f", rs.getDouble("discounts")),
                        String.format("%,.2f", rs.getDouble("warranty_income"))
                    });
                }
            }
        }
        return rows;
    }

    /** Category-level profit breakdown. */
    public List<String[]> categoryPL() throws SQLException {
        String sql =
            "SELECT h.category, "
          + "COUNT(DISTINCT o.order_id)          AS orders, "
          + "SUM(d.qty)                          AS units, "
          + "SUM(d.line_total)                   AS revenue, "
          + "SUM(h.cost_price * d.qty)           AS cogs, "
          + "SUM(d.line_total - h.cost_price * d.qty) AS profit "
          + "FROM sale_details d "
          + "JOIN inventory_hardware h ON h.item_id = d.item_id "
          + "JOIN sales_orders o ON o.order_id = d.order_id "
          + "GROUP BY h.category ORDER BY profit DESC";
        List<String[]> rows = new ArrayList<>();
        try (PreparedStatement ps = con().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                double rev    = rs.getDouble("revenue");
                double profit = rs.getDouble("profit");
                rows.add(new String[]{
                    rs.getString("category"),
                    rs.getString("orders"),
                    rs.getString("units"),
                    String.format("%,.2f", rev),
                    String.format("%,.2f", rs.getDouble("cogs")),
                    String.format("%,.2f", profit),
                    String.format("%.1f%%", rev > 0 ? profit / rev * 100 : 0)
                });
            }
        }
        return rows;
    }

    /** Top 10 most profitable products. */
    public List<String[]> topProducts() throws SQLException {
        String sql =
            "SELECT h.brand, h.model, h.category, "
          + "SUM(d.qty) AS units, "
          + "SUM(d.line_total) AS revenue, "
          + "SUM((d.unit_price - h.cost_price) * d.qty) AS profit "
          + "FROM sale_details d "
          + "JOIN inventory_hardware h ON h.item_id = d.item_id "
          + "GROUP BY h.item_id ORDER BY profit DESC LIMIT 10";
        List<String[]> rows = new ArrayList<>();
        try (PreparedStatement ps = con().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                rows.add(new String[]{
                    rs.getString("brand") + " " + rs.getString("model"),
                    rs.getString("category"),
                    rs.getString("units"),
                    String.format("%,.2f", rs.getDouble("revenue")),
                    String.format("%,.2f", rs.getDouble("profit"))
                });
            }
        }
        return rows;
    }

    /** Overall KPI summary. */
    public double[] overallKPIs() throws SQLException {
        String sql =
            "SELECT SUM(d.line_total)                        AS total_revenue, "
          + "SUM(h.cost_price * d.qty)                      AS total_cogs, "
          + "SUM(d.line_total - h.cost_price * d.qty)       AS total_profit, "
          + "SUM(o.tax)                                     AS total_tax, "
          + "SUM(o.discount)                                AS total_discount, "
          + "SUM(o.balance_due)                             AS total_outstanding, "
          + "COUNT(DISTINCT o.order_id)                     AS total_orders "
          + "FROM sales_orders o "
          + "JOIN sale_details d ON d.order_id = o.order_id "
          + "JOIN inventory_hardware h ON h.item_id = d.item_id";
        try (PreparedStatement ps = con().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            rs.next();
            return new double[]{
                rs.getDouble("total_revenue"),
                rs.getDouble("total_cogs"),
                rs.getDouble("total_profit"),
                rs.getDouble("total_tax"),
                rs.getDouble("total_discount"),
                rs.getDouble("total_outstanding"),
                rs.getDouble("total_orders")
            };
        }
    }
}