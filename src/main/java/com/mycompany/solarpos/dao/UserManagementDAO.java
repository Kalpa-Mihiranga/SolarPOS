package com.mycompany.solarpos.dao;

import com.mycompany.solarpos.db.DBConnection;
import com.mycompany.solarpos.util.PasswordUtil;
import com.mycompany.solarpos.util.ValidationException;
import java.sql.*;
import java.util.*;

public class UserManagementDAO {

    private Connection con() throws SQLException {
    try {
        return DBConnection.getInstance().getConnection();
    } catch (java.io.IOException e) {
        throw new SQLException("Cannot load config.properties: " + e.getMessage(), e);
    }
}

    public List<String[]> findAll() throws SQLException {
        String sql =
            "SELECT u.user_id, u.username, u.role, u.is_active, "
          + "COUNT(DISTINCT o.order_id) AS total_orders, "
          + "MAX(o.order_date)          AS last_sale "
          + "FROM users u "
          + "LEFT JOIN sales_orders o ON o.user_id = u.user_id "
          + "GROUP BY u.user_id ORDER BY u.role, u.username";
        List<String[]> rows = new ArrayList<>();
        try (PreparedStatement ps = con().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                rows.add(new String[]{
                    rs.getString("user_id"),
                    rs.getString("username"),
                    rs.getString("role"),
                    rs.getInt("is_active") == 1 ? "Active" : "Inactive",
                    rs.getString("total_orders"),
                    rs.getString("last_sale") == null ? "Never" :
                            rs.getString("last_sale").substring(0, 10)
                });
            }
        }
        return rows;
    }

    public void createUser(String username, String password,
                           String role) throws SQLException, ValidationException {
        if (username.length() < 3) {
            throw new ValidationException("Username must be at least 3 characters.");
        }
        if (password.length() < 6) {
            throw new ValidationException("Password must be at least 6 characters.");
        }
        try (PreparedStatement ps = con().prepareStatement(
                "INSERT INTO users (username, password_hash, role, is_active) VALUES (?,?,?,1)")) {
            ps.setString(1, username.trim().toLowerCase());
            ps.setString(2, PasswordUtil.sha256(password));
            ps.setString(3, role);
            ps.executeUpdate();
        } catch (SQLIntegrityConstraintViolationException e) {
            throw new ValidationException("Username '" + username + "' is already taken.");
        }
    }

    public void resetPassword(int userId,
                              String newPassword) throws SQLException, ValidationException {
        if (newPassword.length() < 6) {
            throw new ValidationException("Password must be at least 6 characters.");
        }
        try (PreparedStatement ps = con().prepareStatement(
                "UPDATE users SET password_hash=? WHERE user_id=?")) {
            ps.setString(1, PasswordUtil.sha256(newPassword));
            ps.setInt(2, userId);
            ps.executeUpdate();
        }
    }

    public void setActive(int userId, boolean active) throws SQLException {
        try (PreparedStatement ps = con().prepareStatement(
                "UPDATE users SET is_active=? WHERE user_id=?")) {
            ps.setInt(1, active ? 1 : 0);
            ps.setInt(2, userId);
            ps.executeUpdate();
        }
    }
}