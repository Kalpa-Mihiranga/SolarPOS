package com.mycompany.solarpos.dao;

import com.mycompany.solarpos.db.DBConnection;
import com.mycompany.solarpos.model.Contractor;
import com.mycompany.solarpos.model.Customer;
import com.mycompany.solarpos.util.ValidationException;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CustomerDAO {

    private Connection con() throws SQLException {
    try {
        return DBConnection.getInstance().getConnection();
    } catch (java.io.IOException e) {
        throw new SQLException("Cannot load config.properties: " + e.getMessage(), e);
    }
}

    // ------------------------------------------------------------ customers

    public List<Customer> findAll() throws SQLException {
        String sql = "SELECT c.*, k.name AS contractor_name FROM customers c "
                   + "LEFT JOIN contractors k ON k.contractor_id = c.contractor_id "
                   + "ORDER BY c.name";
        List<Customer> list = new ArrayList<>();
        try (PreparedStatement ps = con().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Customer c = new Customer();
                c.setId(rs.getInt("customer_id"));
                c.setName(rs.getString("name"));
                c.setType(rs.getString("type"));
                c.setPhone(rs.getString("phone"));
                c.setEmail(rs.getString("email"));
                c.setAddress(rs.getString("address"));
                c.setGridPhase(rs.getString("grid_phase"));
                c.setContractorId(rs.getInt("contractor_id"));   // 0 when NULL
                c.setContractorName(rs.getString("contractor_name"));
                list.add(c);
            }
        }
        return list;
    }

    public void insert(Customer c) throws SQLException {
        String sql = "INSERT INTO customers (name, type, phone, email, address, grid_phase, contractor_id) "
                   + "VALUES (?,?,?,?,?,?,?)";
        try (PreparedStatement ps = con().prepareStatement(sql)) {
            bind(ps, c);
            ps.executeUpdate();
        }
    }

    public void update(Customer c) throws SQLException {
        String sql = "UPDATE customers SET name=?, type=?, phone=?, email=?, address=?, grid_phase=?, "
                   + "contractor_id=? WHERE customer_id=?";
        try (PreparedStatement ps = con().prepareStatement(sql)) {
            bind(ps, c);
            ps.setInt(8, c.getId());
            ps.executeUpdate();
        }
    }

    public void delete(int customerId) throws SQLException, ValidationException {
        try (PreparedStatement ps = con().prepareStatement("DELETE FROM customers WHERE customer_id=?")) {
            ps.setInt(1, customerId);
            ps.executeUpdate();
        } catch (SQLIntegrityConstraintViolationException e) {
            throw new ValidationException("This customer has sales orders and cannot be deleted.");
        }
    }

    private void bind(PreparedStatement ps, Customer c) throws SQLException {
        ps.setString(1, c.getName());
        ps.setString(2, c.getType());
        ps.setString(3, c.getPhone());
        if (c.getEmail() == null || c.getEmail().isEmpty()) {
            ps.setNull(4, Types.VARCHAR);
        } else {
            ps.setString(4, c.getEmail());
        }
        ps.setString(5, c.getAddress());
        ps.setString(6, c.getGridPhase());
        if (c.getContractorId() == 0) {
            ps.setNull(7, Types.INTEGER);
        } else {
            ps.setInt(7, c.getContractorId());
        }
    }

    // ------------------------------------------------------------ contractors

    public List<Contractor> findContractors() throws SQLException {
        List<Contractor> list = new ArrayList<>();
        String sql = "SELECT contractor_id, name, phone, license_no FROM contractors ORDER BY name";
        try (PreparedStatement ps = con().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(new Contractor(rs.getInt("contractor_id"), rs.getString("name"),
                        rs.getString("phone"), rs.getString("license_no")));
            }
        }
        return list;
    }

    /** Inserts a contractor and returns it with its generated id. */
    public Contractor insertContractor(String name, String phone, String licenseNo) throws SQLException {
        String sql = "INSERT INTO contractors (name, phone, license_no) VALUES (?,?,?)";
        try (PreparedStatement ps = con().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, name);
            ps.setString(2, phone);
            ps.setString(3, licenseNo);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                return new Contractor(keys.getInt(1), name, phone, licenseNo);
            }
        }
    }
}