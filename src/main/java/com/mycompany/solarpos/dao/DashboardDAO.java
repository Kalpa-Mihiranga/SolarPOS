package com.mycompany.solarpos.dao;

import com.mycompany.solarpos.db.DBConnection;
import java.sql.*;
import java.util.*;

public class DashboardDAO {

    private Connection con() throws SQLException {
    try {
        return DBConnection.getInstance().getConnection();
    } catch (java.io.IOException e) {
        throw new SQLException("Cannot load config.properties: " + e.getMessage(), e);
    }
}

    public double totalKwThisMonth() throws SQLException {
        String sql = "SELECT COALESCE(SUM(total_kw),0) FROM sales_orders "
                   + "WHERE MONTH(order_date)=MONTH(CURDATE()) AND YEAR(order_date)=YEAR(CURDATE())";
        try (PreparedStatement ps = con().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            rs.next();
            return rs.getDouble(1);
        }
    }

    public double grossRevenueThisMonth() throws SQLException {
        String sql = "SELECT COALESCE(SUM(grand_total),0) FROM sales_orders "
                   + "WHERE MONTH(order_date)=MONTH(CURDATE()) AND YEAR(order_date)=YEAR(CURDATE())";
        try (PreparedStatement ps = con().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            rs.next();
            return rs.getDouble(1);
        }
    }

    public int ordersThisMonth() throws SQLException {
        String sql = "SELECT COUNT(*) FROM sales_orders "
                   + "WHERE MONTH(order_date)=MONTH(CURDATE()) AND YEAR(order_date)=YEAR(CURDATE())";
        try (PreparedStatement ps = con().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            rs.next();
            return rs.getInt(1);
        }
    }

    public int lowStockCount() throws SQLException {
        String sql = "SELECT COUNT(*) FROM inventory_hardware WHERE stock_qty <= 5";
        try (PreparedStatement ps = con().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            rs.next();
            return rs.getInt(1);
        }
    }

    /** Returns brand → units sold, top 5, current month only. */
    public Map<String, Integer> topInverterBrands() throws SQLException {
        String sql = "SELECT h.brand, SUM(d.qty) AS units "
                   + "FROM sale_details d "
                   + "JOIN inventory_hardware h ON d.item_id = h.item_id "
                   + "JOIN sales_orders o ON o.order_id = d.order_id "
                   + "WHERE h.category = 'INVERTER' "
                   + "AND MONTH(o.order_date) = MONTH(CURDATE()) "
                   + "AND YEAR(o.order_date) = YEAR(CURDATE()) "
                   + "GROUP BY h.brand ORDER BY units DESC LIMIT 5";
        Map<String, Integer> map = new LinkedHashMap<>();
        try (PreparedStatement ps = con().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                map.put(rs.getString("brand"), rs.getInt("units"));
            }
        }
        return map;
    }

    /** All-time top inverter brands (for the chart when no monthly data). */
    public Map<String, Integer> topInverterBrandsAllTime() throws SQLException {
        String sql = "SELECT h.brand, SUM(d.qty) AS units "
                   + "FROM sale_details d "
                   + "JOIN inventory_hardware h ON d.item_id = h.item_id "
                   + "WHERE h.category = 'INVERTER' "
                   + "GROUP BY h.brand ORDER BY units DESC LIMIT 5";
        Map<String, Integer> map = new LinkedHashMap<>();
        try (PreparedStatement ps = con().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                map.put(rs.getString("brand"), rs.getInt("units"));
            }
        }
        return map;
    }

    /** Warranties expiring within the next 60 days. */
    public List<String[]> expiringWarranties() throws SQLException {
        String sql = "SELECT c.name, h.brand, h.model, d.warranty_end, "
                   + "DATEDIFF(d.warranty_end, CURDATE()) AS days_left "
                   + "FROM sale_details d "
                   + "JOIN sales_orders o ON o.order_id = d.order_id "
                   + "JOIN customers c ON c.customer_id = o.customer_id "
                   + "JOIN inventory_hardware h ON h.item_id = d.item_id "
                   + "WHERE d.warranty_end BETWEEN CURDATE() "
                   + "AND DATE_ADD(CURDATE(), INTERVAL 60 DAY) "
                   + "ORDER BY d.warranty_end";
        List<String[]> rows = new ArrayList<>();
        try (PreparedStatement ps = con().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                rows.add(new String[]{
                    rs.getString("name"),
                    rs.getString("brand") + " " + rs.getString("model"),
                    rs.getString("warranty_end"),
                    rs.getString("days_left") + " days"
                });
            }
        }
        return rows;
    }

    /** Recent 8 orders for the activity feed. */
    public List<String[]> recentOrders() throws SQLException {
        String sql = "SELECT o.order_id, c.name, o.grand_total, o.total_kw, o.order_date "
                   + "FROM sales_orders o "
                   + "JOIN customers c ON c.customer_id = o.customer_id "
                   + "ORDER BY o.order_date DESC LIMIT 8";
        List<String[]> rows = new ArrayList<>();
        try (PreparedStatement ps = con().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                rows.add(new String[]{
                    "#" + rs.getString("order_id"),
                    rs.getString("name"),
                    String.format("Rs. %,.0f", rs.getDouble("grand_total")),
                    String.format("%.2f kW", rs.getDouble("total_kw")),
                    rs.getString("order_date").substring(0, 10)
                });
            }
        }
        return rows;
    }
}