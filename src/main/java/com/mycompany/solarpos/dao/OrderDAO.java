package com.mycompany.solarpos.dao;

import com.mycompany.solarpos.db.DBConnection;
import com.mycompany.solarpos.model.CartLine;
import com.mycompany.solarpos.model.OrderSummary;
import com.mycompany.solarpos.service.PricingService;
import com.mycompany.solarpos.util.InsufficientStockException;
import com.mycompany.solarpos.util.ValidationException;
import java.io.IOException;
import java.sql.*;
import java.time.LocalDate;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class OrderDAO {

    private static final Logger LOG = Logger.getLogger(OrderDAO.class.getName());
    private final PricingService pricing = new PricingService();

    // ---------------------------------------------------------------- connection helper

    private Connection con() throws SQLException {
        try {
            return DBConnection.getInstance().getConnection();
        } catch (IOException e) {
            throw new SQLException(
                    "Cannot load config.properties: " + e.getMessage(), e);
        }
    }

    // ---------------------------------------------------------------- save order

    /**
     * Saves a sale as ONE transaction: order header, order lines and stock
     * reduction. Prices are recalculated here so the saved totals never
     * depend on what the screen calculated.
     *
     * @return the new order_id
     */
    public int saveOrder(int customerId, int userId,
                         List<CartLine> lines, double downPayment)
            throws SQLException, ValidationException, InsufficientStockException {

        if (customerId <= 0) {
            throw new ValidationException("Please select a customer.");
        }
        if (lines == null || lines.isEmpty()) {
            throw new ValidationException("The cart is empty.");
        }

        double down = PricingService.round2(downPayment);
        OrderSummary summary = pricing.calculate(lines);
        pricing.validateDownPayment(summary, down);

        Connection con = con();                        // uses the helper above
        boolean previousAutoCommit = con.getAutoCommit();
        try {
            con.setAutoCommit(false);                  // start transaction

            int orderId = insertOrder(con, customerId, userId, summary, down);
            for (CartLine line : lines) {
                reduceStock(con, line);
                insertDetail(con, orderId, line);
            }

            con.commit();                              // everything succeeded
            return orderId;

        } catch (SQLException
                | InsufficientStockException
                | RuntimeException e) {
            rollbackQuietly(con);                      // undo everything
            throw e;
        } finally {
            try {
                con.setAutoCommit(previousAutoCommit);
            } catch (SQLException ex) {
                LOG.log(Level.WARNING, "Could not restore auto-commit", ex);
            }
        }
    }

    // ---------------------------------------------------------------- private helpers

    private int insertOrder(Connection con, int customerId, int userId,
                            OrderSummary s, double down) throws SQLException {
        String sql =
            "INSERT INTO sales_orders "
          + "(customer_id, user_id, subtotal, discount, tax, "
          + " warranty_fees, grand_total, down_payment, balance_due, total_kw) "
          + "VALUES (?,?,?,?,?,?,?,?,?,?)";
        try (PreparedStatement ps =
                con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, customerId);
            ps.setInt(2, userId);
            ps.setDouble(3, s.getSubtotal());
            ps.setDouble(4, s.getDiscount());
            ps.setDouble(5, s.getTax());
            ps.setDouble(6, s.getWarrantyFees());
            ps.setDouble(7, s.getGrandTotal());
            ps.setDouble(8, down);
            ps.setDouble(9, PricingService.round2(s.getGrandTotal() - down));
            ps.setDouble(10, s.getTotalKw());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new SQLException("No generated key returned for new order.");
                }
                return keys.getInt(1);
            }
        }
    }

    /**
     * Reduces stock atomically.
     * The WHERE clause checks stock_qty >= qty so that if stock drops to zero
     * between the cart check and the save, 0 rows are updated and we throw
     * InsufficientStockException instead of going negative.
     */
    private void reduceStock(Connection con,
                             CartLine line)
            throws SQLException, InsufficientStockException {
        String sql =
            "UPDATE inventory_hardware "
          + "SET stock_qty = stock_qty - ? "
          + "WHERE item_id = ? AND stock_qty >= ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, line.getQty());
            ps.setInt(2, line.getItem().getId());
            ps.setInt(3, line.getQty());
            if (ps.executeUpdate() == 0) {
                throw new InsufficientStockException(
                        line.getItem().toString(), line.getQty());
            }
        }
    }

    private void insertDetail(Connection con,
                              int orderId,
                              CartLine line) throws SQLException {
        String sql =
            "INSERT INTO sale_details "
          + "(order_id, item_id, qty, unit_price, line_total, warranty_end) "
          + "VALUES (?,?,?,?,?,?)";
        LocalDate warrantyEnd =
                LocalDate.now().plusMonths(line.getItem().getWarrantyMonths());
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, orderId);
            ps.setInt(2, line.getItem().getId());
            ps.setInt(3, line.getQty());
            ps.setDouble(4, line.getItem().getUnitPrice());
            ps.setDouble(5, line.getLineTotal());
            ps.setDate(6, Date.valueOf(warrantyEnd));
            ps.executeUpdate();
        }
    }

    // ---------------------------------------------------------------- utility queries

    /** Used by the backend test to verify rollback behaviour. */
    public int countOrders() throws SQLException {
        String sql = "SELECT COUNT(*) FROM sales_orders";
        try (PreparedStatement ps = con().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            rs.next();
            return rs.getInt(1);
        }
    }

    // ---------------------------------------------------------------- rollback

    private void rollbackQuietly(Connection con) {
        try {
            con.rollback();
            LOG.info("[OrderDAO] Transaction rolled back.");
        } catch (SQLException ex) {
            LOG.log(Level.SEVERE, "Rollback failed", ex);
        }
    }
}