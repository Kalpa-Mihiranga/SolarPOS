package com.mycompany.solarpos.dao;

import com.mycompany.solarpos.db.DBConnection;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class SalesHistoryDAO {

    private Connection con() throws SQLException {
    try {
        return DBConnection.getInstance().getConnection();
    } catch (java.io.IOException e) {
        throw new SQLException("Cannot load config.properties: " + e.getMessage(), e);
    }
}

    /** All orders with customer and cashier name, filterable by date range. */
    public List<String[]> findOrders(String fromDate, String toDate,
                                     String customerSearch) throws SQLException {
        StringBuilder sql = new StringBuilder(
            "SELECT o.order_id, c.name AS customer, c.type, c.grid_phase, "
          + "u.username AS cashier, o.order_date, o.subtotal, o.discount, "
          + "o.warranty_fees, o.tax, o.grand_total, o.down_payment, "
          + "o.balance_due, o.total_kw "
          + "FROM sales_orders o "
          + "JOIN customers c ON c.customer_id = o.customer_id "
          + "JOIN users u ON u.user_id = o.user_id "
          + "WHERE 1=1 ");
        if (fromDate != null && !fromDate.isEmpty()) {
            sql.append("AND DATE(o.order_date) >= '").append(fromDate).append("' ");
        }
        if (toDate != null && !toDate.isEmpty()) {
            sql.append("AND DATE(o.order_date) <= '").append(toDate).append("' ");
        }
        if (customerSearch != null && !customerSearch.isEmpty()) {
            sql.append("AND c.name LIKE '%").append(
                    customerSearch.replace("'", "''")).append("%' ");
        }
        sql.append("ORDER BY o.order_date DESC");

        List<String[]> rows = new ArrayList<>();
        try (PreparedStatement ps = con().prepareStatement(sql.toString());
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                rows.add(new String[]{
                    rs.getString("order_id"),
                    rs.getString("customer"),
                    rs.getString("type"),
                    rs.getString("cashier"),
                    rs.getString("order_date") == null ? "" :
                            rs.getString("order_date").substring(0, 10),
                    String.format("%,.2f", rs.getDouble("subtotal")),
                    String.format("%,.2f", rs.getDouble("discount")),
                    String.format("%,.2f", rs.getDouble("warranty_fees")),
                    String.format("%,.2f", rs.getDouble("tax")),
                    String.format("%,.2f", rs.getDouble("grand_total")),
                    String.format("%,.2f", rs.getDouble("down_payment")),
                    String.format("%,.2f", rs.getDouble("balance_due")),
                    String.format("%.2f", rs.getDouble("total_kw")),
                    rs.getString("grid_phase")
                });
            }
        }
        return rows;
    }

    /** Line items for a single order. */
    public List<String[]> findOrderLines(int orderId) throws SQLException {
        String sql = "SELECT h.item_code, h.brand, h.model, h.category, "
                   + "d.qty, d.unit_price, d.line_total, d.warranty_end, "
                   + "h.cost_price, (d.unit_price - h.cost_price) * d.qty AS line_profit "
                   + "FROM sale_details d "
                   + "JOIN inventory_hardware h ON h.item_id = d.item_id "
                   + "WHERE d.order_id = ? "
                   + "ORDER BY h.category, h.brand";
        List<String[]> rows = new ArrayList<>();
        try (PreparedStatement ps = con().prepareStatement(sql)) {
            ps.setInt(1, orderId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    rows.add(new String[]{
                        rs.getString("item_code"),
                        rs.getString("brand") + " " + rs.getString("model"),
                        rs.getString("category"),
                        rs.getString("qty"),
                        String.format("%,.2f", rs.getDouble("unit_price")),
                        String.format("%,.2f", rs.getDouble("line_total")),
                        rs.getString("warranty_end"),
                        String.format("%,.2f", rs.getDouble("line_profit"))
                    });
                }
            }
        }
        return rows;
    }

    /** Summary totals for a filtered period. */
    public double[] periodTotals(String fromDate, String toDate) throws SQLException {
        StringBuilder sql = new StringBuilder(
            "SELECT COUNT(*) AS cnt, "
          + "COALESCE(SUM(grand_total),0) AS revenue, "
          + "COALESCE(SUM(discount),0) AS discounts, "
          + "COALESCE(SUM(tax),0) AS tax, "
          + "COALESCE(SUM(balance_due),0) AS outstanding, "
          + "COALESCE(SUM(total_kw),0) AS kw "
          + "FROM sales_orders WHERE 1=1 ");
        if (fromDate != null && !fromDate.isEmpty())
            sql.append("AND DATE(order_date) >= '").append(fromDate).append("' ");
        if (toDate != null && !toDate.isEmpty())
            sql.append("AND DATE(order_date) <= '").append(toDate).append("' ");
        try (PreparedStatement ps = con().prepareStatement(sql.toString());
             ResultSet rs = ps.executeQuery()) {
            rs.next();
            return new double[]{
                rs.getInt("cnt"),
                rs.getDouble("revenue"),
                rs.getDouble("discounts"),
                rs.getDouble("tax"),
                rs.getDouble("outstanding"),
                rs.getDouble("kw")
            };
        }
    }
}