package com.mycompany.solarpos.dao;

import com.mycompany.solarpos.db.DBConnection;
import com.mycompany.solarpos.util.ValidationException;
import java.sql.*;
import java.util.*;

public class BalancePaymentDAO {

    private Connection con() throws SQLException {
        return DBConnection.getInstance().getConnection();
    }

    /** Orders that still have an outstanding balance. */
    public List<String[]> findOutstandingOrders(String customerSearch) throws SQLException {
        String sql =
            "SELECT o.order_id, c.name, c.phone, o.order_date, "
          + "o.grand_total, o.down_payment, o.balance_due "
          + "FROM sales_orders o "
          + "JOIN customers c ON c.customer_id = o.customer_id "
          + "WHERE o.balance_due > 0.01 ";
        if (customerSearch != null && !customerSearch.isEmpty()) {
            sql += "AND c.name LIKE '%" + customerSearch.replace("'","''") + "%' ";
        }
        sql += "ORDER BY o.balance_due DESC, o.order_date";
        List<String[]> rows = new ArrayList<>();
        try (PreparedStatement ps = con().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                rows.add(new String[]{
                    rs.getString("order_id"),
                    rs.getString("name"),
                    rs.getString("phone"),
                    rs.getString("order_date") == null ? "" :
                            rs.getString("order_date").substring(0, 10),
                    String.format("%,.2f", rs.getDouble("grand_total")),
                    String.format("%,.2f", rs.getDouble("down_payment")),
                    String.format("%,.2f", rs.getDouble("balance_due"))
                });
            }
        }
        return rows;
    }

    /** Payment history for one order. */
    public List<String[]> findPaymentsForOrder(int orderId) throws SQLException {
        String sql =
            "SELECT p.payment_id, p.amount, p.payment_date, "
          + "u.username AS collected_by, p.notes "
          + "FROM balance_payments p "
          + "JOIN users u ON u.user_id = p.collected_by "
          + "WHERE p.order_id = ? ORDER BY p.payment_date";
        List<String[]> rows = new ArrayList<>();
        try (PreparedStatement ps = con().prepareStatement(sql)) {
            ps.setInt(1, orderId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    rows.add(new String[]{
                        rs.getString("payment_id"),
                        String.format("%,.2f", rs.getDouble("amount")),
                        rs.getString("payment_date") == null ? "" :
                                rs.getString("payment_date").substring(0, 10),
                        rs.getString("collected_by"),
                        rs.getString("notes") == null ? "" : rs.getString("notes")
                    });
                }
            }
        }
        return rows;
    }

    /**
     * Records a payment and reduces the balance on the order.
     * Runs as one transaction.
     */
    public void recordPayment(int orderId, double amount,
                              int collectedBy, String notes)
            throws SQLException, ValidationException {

        // Get current balance
        double currentBalance;
        try (PreparedStatement ps = con().prepareStatement(
                "SELECT balance_due FROM sales_orders WHERE order_id=?")) {
            ps.setInt(1, orderId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) throw new ValidationException("Order not found.");
                currentBalance = rs.getDouble("balance_due");
            }
        }
        if (amount <= 0) {
            throw new ValidationException("Payment amount must be greater than 0.");
        }
        if (amount > currentBalance + 0.005) {
            throw new ValidationException(String.format(
                    "Payment (Rs. %,.2f) exceeds the balance due (Rs. %,.2f).",
                    amount, currentBalance));
        }

        Connection c = con();
        boolean prev = c.getAutoCommit();
        try {
            c.setAutoCommit(false);

            // Insert payment record
            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO balance_payments "
                  + "(order_id, amount, collected_by, notes) VALUES (?,?,?,?)")) {
                ps.setInt(1, orderId);
                ps.setDouble(2, amount);
                ps.setInt(3, collectedBy);
                ps.setString(4, notes);
                ps.executeUpdate();
            }

            // Reduce balance on order
            try (PreparedStatement ps = c.prepareStatement(
                    "UPDATE sales_orders SET balance_due = balance_due - ?, "
                  + "down_payment = down_payment + ? WHERE order_id = ?")) {
                ps.setDouble(1, amount);
                ps.setDouble(2, amount);
                ps.setInt(3, orderId);
                ps.executeUpdate();
            }

            c.commit();
        } catch (Exception ex) {
            c.rollback();
            throw ex;
        } finally {
            c.setAutoCommit(prev);
        }
    }
}