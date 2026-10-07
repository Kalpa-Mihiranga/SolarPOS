package com.mycompany.solarpos.dao;

import com.mycompany.solarpos.db.DBConnection;
import java.sql.*;
import java.util.*;

public class CustomerAnalyticsDAO {

    private Connection con() throws SQLException {
    try {
        return DBConnection.getInstance().getConnection();
    } catch (java.io.IOException e) {
        throw new SQLException("Cannot load config.properties: " + e.getMessage(), e);
    }
}

    /** Top 10 customers by total spend. */
    public List<String[]> topSpenders() throws SQLException {
        String sql =
            "SELECT c.name, c.type, c.phone, "
          + "COUNT(DISTINCT o.order_id)    AS orders, "
          + "SUM(o.grand_total)            AS total_spend, "
          + "AVG(o.grand_total)            AS avg_order, "
          + "SUM(o.total_kw)               AS total_kw, "
          + "SUM(o.balance_due)            AS outstanding "
          + "FROM customers c "
          + "JOIN sales_orders o ON o.customer_id = c.customer_id "
          + "GROUP BY c.customer_id "
          + "ORDER BY total_spend DESC LIMIT 10";
        List<String[]> rows = new ArrayList<>();
        try (PreparedStatement ps = con().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                rows.add(new String[]{
                    rs.getString("name"),
                    rs.getString("type"),
                    rs.getString("phone"),
                    rs.getString("orders"),
                    String.format("%,.2f", rs.getDouble("total_spend")),
                    String.format("%,.2f", rs.getDouble("avg_order")),
                    String.format("%.2f", rs.getDouble("total_kw")),
                    String.format("%,.2f", rs.getDouble("outstanding"))
                });
            }
        }
        return rows;
    }

    /** Repeat buyers: customers with more than one order. */
    public List<String[]> repeatBuyers() throws SQLException {
        String sql =
            "SELECT c.name, c.type, c.phone, "
          + "COUNT(DISTINCT o.order_id) AS orders, "
          + "MIN(DATE(o.order_date))    AS first_order, "
          + "MAX(DATE(o.order_date))    AS last_order, "
          + "DATEDIFF(MAX(o.order_date), MIN(o.order_date)) AS days_between, "
          + "SUM(o.grand_total)         AS lifetime_value "
          + "FROM customers c "
          + "JOIN sales_orders o ON o.customer_id = c.customer_id "
          + "GROUP BY c.customer_id "
          + "HAVING orders > 1 "
          + "ORDER BY orders DESC, lifetime_value DESC";
        List<String[]> rows = new ArrayList<>();
        try (PreparedStatement ps = con().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                rows.add(new String[]{
                    rs.getString("name"),
                    rs.getString("type"),
                    rs.getString("phone"),
                    rs.getString("orders"),
                    rs.getString("first_order"),
                    rs.getString("last_order"),
                    rs.getString("days_between") + " days",
                    String.format("%,.2f", rs.getDouble("lifetime_value"))
                });
            }
        }
        return rows;
    }

    /** Overall customer KPIs. */
    public double[] customerKPIs() throws SQLException {
        String sql =
            "SELECT COUNT(DISTINCT c.customer_id)                    AS total_customers, "
          + "COUNT(DISTINCT CASE WHEN o.order_id IS NOT NULL "
          + "     THEN c.customer_id END)                            AS active_customers, "
          + "COUNT(DISTINCT o.order_id)                              AS total_orders, "
          + "COALESCE(AVG(o.grand_total), 0)                        AS avg_order_value, "
          + "COALESCE(SUM(o.grand_total) / "
          + "     NULLIF(COUNT(DISTINCT c.customer_id), 0), 0)      AS revenue_per_customer "
          + "FROM customers c "
          + "LEFT JOIN sales_orders o ON o.customer_id = c.customer_id";
        try (PreparedStatement ps = con().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            rs.next();
            return new double[]{
                rs.getDouble("total_customers"),
                rs.getDouble("active_customers"),
                rs.getDouble("total_orders"),
                rs.getDouble("avg_order_value"),
                rs.getDouble("revenue_per_customer")
            };
        }
    }

    /** Customer type breakdown: residential vs commercial. */
    public List<String[]> typeBreakdown() throws SQLException {
        String sql =
            "SELECT c.type, "
          + "COUNT(DISTINCT c.customer_id) AS customers, "
          + "COUNT(DISTINCT o.order_id)    AS orders, "
          + "COALESCE(SUM(o.grand_total),0)      AS revenue, "
          + "COALESCE(AVG(o.grand_total),0)      AS avg_order "
          + "FROM customers c "
          + "LEFT JOIN sales_orders o ON o.customer_id = c.customer_id "
          + "GROUP BY c.type";
        List<String[]> rows = new ArrayList<>();
        try (PreparedStatement ps = con().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                rows.add(new String[]{
                    rs.getString("type"),
                    rs.getString("customers"),
                    rs.getString("orders"),
                    String.format("%,.2f", rs.getDouble("revenue")),
                    String.format("%,.2f", rs.getDouble("avg_order"))
                });
            }
        }
        return rows;
    }
}