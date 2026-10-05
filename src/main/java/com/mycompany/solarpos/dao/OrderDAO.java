package com.mycompany.solarpos.dao;

import com.mycompany.solarpos.db.DBConnection;
import com.mycompany.solarpos.model.CartLine;
import com.mycompany.solarpos.model.OrderSummary;
import com.mycompany.solarpos.service.PricingService;
import com.mycompany.solarpos.util.InsufficientStockException;
import com.mycompany.solarpos.util.ValidationException;
import java.sql.*;
import java.time.LocalDate;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class OrderDAO {

    private static final Logger LOG = Logger.getLogger(OrderDAO.class.getName());
    private final PricingService pricing = new PricingService();

    /**
     * Saves a sale as ONE transaction: order header, order lines and stock reduction.
     * Prices are recalculated here, so the saved totals never depend on the screen.
     * @return the new order id
     */
    public int saveOrder(int customerId, int userId, List<CartLine> lines, double downPayment)
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

        Connection con = DBConnection.getInstance().getConnection();
        boolean previousAutoCommit = con.getAutoCommit();
        try {
            con.setAutoCommit(false);                      // start transaction

            int orderId = insertOrder(con, customerId, userId, summary, down);
            for (CartLine line : lines) {
                reduceStock(con, line);
                insertDetail(con, orderId, line);
            }

            con.commit();                                  // everything succeeded
            return orderId;

        } catch (SQLException | InsufficientStockException | RuntimeException e) {
            rollbackQuietly(con);                          // undo everything
            throw e;
        } finally {
            con.setAutoCommit(previousAutoCommit);
        }
    }

    private int insertOrder(Connection con, int customerId, int userId,
                            OrderSummary s, double down) throws SQLException {
        String sql = "INSERT INTO sales_orders (customer_id, user_id, subtotal, discount, tax, "
                   + "warranty_fees, grand_total, down_payment, balance_due, total_kw) "
                   + "VALUES (?,?,?,?,?,?,?,?,?,?)";
        try (PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
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
                keys.next();
                return keys.getInt(1);
            }
        }
    }

    /** Reduces stock only if enough is available; 0 rows updated means not enough stock. */
    private void reduceStock(Connection con, CartLine line) throws SQLException, InsufficientStockException {
        String sql = "UPDATE inventory_hardware SET stock_qty = stock_qty - ? "
                   + "WHERE item_id = ? AND stock_qty >= ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, line.getQty());
            ps.setInt(2, line.getItem().getId());
            ps.setInt(3, line.getQty());
            if (ps.executeUpdate() == 0) {
                throw new InsufficientStockException(line.getItem().toString(), line.getQty());
            }
        }
    }

    private void insertDetail(Connection con, int orderId, CartLine line) throws SQLException {
        String sql = "INSERT INTO sale_details (order_id, item_id, qty, unit_price, line_total, warranty_end) "
                   + "VALUES (?,?,?,?,?,?)";
        LocalDate warrantyEnd = LocalDate.now().plusMonths(line.getItem().getWarrantyMonths());
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

    public int countOrders() throws SQLException {
        try (PreparedStatement ps = DBConnection.getInstance().getConnection()
                .prepareStatement("SELECT COUNT(*) FROM sales_orders");
             ResultSet rs = ps.executeQuery()) {
            rs.next();
            return rs.getInt(1);
        }
    }

    private void rollbackQuietly(Connection con) {
        try {
            con.rollback();
        } catch (SQLException ex) {
            LOG.log(Level.SEVERE, "Rollback failed", ex);
        }
    }
}