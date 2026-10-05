package com.mycompany.solarpos.dao;

import com.mycompany.solarpos.db.DBConnection;
import com.mycompany.solarpos.model.User;
import com.mycompany.solarpos.util.PasswordUtil;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class UserDAO {

    /** Returns the User if the credentials are correct, otherwise null. */
   public User login(String username, String password) throws SQLException {
    String sql = "SELECT user_id, username, role, is_active FROM users "
               + "WHERE username = ? AND password_hash = ?";
    try (PreparedStatement ps = DBConnection.getInstance()
            .getConnection().prepareStatement(sql)) {
        ps.setString(1, username);
        ps.setString(2, PasswordUtil.sha256(password));
        try (ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                if (rs.getInt("is_active") == 0) {
                    return null; // treat inactive same as wrong password
                }
                return new User(rs.getInt("user_id"),
                        rs.getString("username"), rs.getString("role"));
            }
        }
    }
    return null;
    }
}