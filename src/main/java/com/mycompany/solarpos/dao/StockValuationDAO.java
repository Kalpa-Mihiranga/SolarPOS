package com.mycompany.solarpos.dao;

import com.mycompany.solarpos.db.DBConnection;
import java.sql.*;
import java.util.*;

public class StockValuationDAO {

    private Connection con() throws SQLException {
    try {
        return DBConnection.getInstance().getConnection();
    } catch (java.io.IOException e) {
        throw new SQLException("Cannot load config.properties: " + e.getMessage(), e);
    }
}

    /** Full stock valuation with slow-moving flag. */
    public List<String[]> fullValuation() throws SQLException {
        String sql =
            "SELECT h.item_code, h.brand, h.model, h.category, "
          + "h.stock_qty, h.cost_price, h.unit_price, "
          + "h.stock_qty * h.cost_price  AS stock_cost_value, "
          + "h.stock_qty * h.unit_price  AS stock_sale_value, "
          + "(h.unit_price - h.cost_price) AS unit_margin, "
          + "COALESCE(s.units_sold, 0)   AS units_sold, "
          + "COALESCE(ROUND(s.units_sold / "
          + "   NULLIF(h.stock_qty + s.units_sold, 0), 2), 0) AS turnover_ratio, "
          + "h.warranty_months, "
          + "DATE_ADD(h.date_received, INTERVAL h.warranty_months MONTH) AS warranty_end "
          + "FROM inventory_hardware h "
          + "LEFT JOIN ( "
          + "    SELECT item_id, SUM(qty) AS units_sold "
          + "    FROM sale_details GROUP BY item_id "
          + ") s ON s.item_id = h.item_id "
          + "ORDER BY h.category, turnover_ratio ASC";
        List<String[]> rows = new ArrayList<>();
        try (PreparedStatement ps = con().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                double ratio = rs.getDouble("turnover_ratio");
                String warrantyEnd = rs.getString("warranty_end");
                boolean slowMoving = ratio < 0.25;
                boolean warrantyRisk = warrantyEnd != null &&
                        rs.getDate("warranty_end").toLocalDate()
                        .isBefore(java.time.LocalDate.now().plusMonths(6));
                String flag = (slowMoving || warrantyRisk) ? "⚠ SLOW-MOVING" : "✓ OK";

                rows.add(new String[]{
                    rs.getString("item_code"),
                    rs.getString("brand") + " " + rs.getString("model"),
                    rs.getString("category"),
                    rs.getString("stock_qty"),
                    String.format("%,.2f", rs.getDouble("cost_price")),
                    String.format("%,.2f", rs.getDouble("unit_price")),
                    String.format("%,.2f", rs.getDouble("stock_cost_value")),
                    String.format("%,.2f", rs.getDouble("stock_sale_value")),
                    String.format("%,.2f", rs.getDouble("unit_margin")),
                    rs.getString("units_sold"),
                    String.format("%.0f%%", ratio * 100),
                    warrantyEnd == null ? "-" : warrantyEnd,
                    flag
                });
            }
        }
        return rows;
    }

    /** Category summary for the valuation cards. */
    public List<String[]> categorySummary() throws SQLException {
        String sql =
            "SELECT h.category, "
          + "COUNT(*)                          AS items, "
          + "SUM(h.stock_qty)                  AS total_units, "
          + "SUM(h.stock_qty * h.cost_price)   AS cost_value, "
          + "SUM(h.stock_qty * h.unit_price)   AS sale_value, "
          + "SUM(CASE WHEN h.stock_qty = 0 THEN 1 ELSE 0 END) AS out_of_stock "
          + "FROM inventory_hardware h "
          + "GROUP BY h.category ORDER BY cost_value DESC";
        List<String[]> rows = new ArrayList<>();
        try (PreparedStatement ps = con().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                rows.add(new String[]{
                    rs.getString("category"),
                    rs.getString("items"),
                    rs.getString("total_units"),
                    String.format("%,.2f", rs.getDouble("cost_value")),
                    String.format("%,.2f", rs.getDouble("sale_value")),
                    rs.getString("out_of_stock")
                });
            }
        }
        return rows;
    }

    /** Overall totals. */
    public double[] totals() throws SQLException {
        String sql =
            "SELECT SUM(stock_qty * cost_price)  AS total_cost, "
          + "SUM(stock_qty * unit_price)         AS total_sale, "
          + "SUM(CASE WHEN stock_qty <= 5 THEN stock_qty * cost_price ELSE 0 END) AS low_stock_value, "
          + "COUNT(CASE WHEN stock_qty = 0 THEN 1 END) AS out_of_stock "
          + "FROM inventory_hardware";
        try (PreparedStatement ps = con().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            rs.next();
            return new double[]{
                rs.getDouble("total_cost"),
                rs.getDouble("total_sale"),
                rs.getDouble("low_stock_value"),
                rs.getDouble("out_of_stock")
            };
        }
    }
}