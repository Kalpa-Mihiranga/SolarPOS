package com.mycompany.solarpos.dao;

import com.mycompany.solarpos.db.DBConnection;
import com.mycompany.solarpos.factory.HardwareFactory;
import com.mycompany.solarpos.model.*;
import com.mycompany.solarpos.util.ValidationException;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class InventoryDAO {

    private static final String COLS =
        "item_code, category, brand, model, wattage_w, inverter_type, capacity_kw, capacity_kwh, "
      + "cycle_life, unit_price, stock_qty, warranty_months, installer_warranty_fee, supplier_id, date_received";

    private Connection con() throws SQLException {
        return DBConnection.getInstance().getConnection();
    }

    public List<HardwareItem> findAll() throws SQLException {
        String sql = "SELECT * FROM inventory_hardware ORDER BY category, brand, model";
        List<HardwareItem> list = new ArrayList<>();
        try (PreparedStatement ps = con().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        }
        return list;
    }

    public List<Supplier> findSuppliers() throws SQLException {
        List<Supplier> list = new ArrayList<>();
        try (PreparedStatement ps = con().prepareStatement("SELECT supplier_id, name FROM suppliers ORDER BY name");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(new Supplier(rs.getInt("supplier_id"), rs.getString("name")));
            }
        }
        return list;
    }

    public void insert(HardwareItem item) throws SQLException, ValidationException {
        String sql = "INSERT INTO inventory_hardware (" + COLS + ") VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
        try (PreparedStatement ps = con().prepareStatement(sql)) {
            bind(ps, item);
            ps.executeUpdate();
        } catch (SQLIntegrityConstraintViolationException e) {
            throw new ValidationException("Item code '" + item.getItemCode()
                    + "' already exists. Please use a different code.");
        }
    }

    public void update(HardwareItem item) throws SQLException, ValidationException {
        String sql = "UPDATE inventory_hardware SET item_code=?, category=?, brand=?, model=?, wattage_w=?, "
                   + "inverter_type=?, capacity_kw=?, capacity_kwh=?, cycle_life=?, unit_price=?, stock_qty=?, "
                   + "warranty_months=?, installer_warranty_fee=?, supplier_id=?, date_received=? WHERE item_id=?";
        try (PreparedStatement ps = con().prepareStatement(sql)) {
            bind(ps, item);
            ps.setInt(16, item.getId());
            ps.executeUpdate();
        } catch (SQLIntegrityConstraintViolationException e) {
            throw new ValidationException("Item code '" + item.getItemCode()
                    + "' is already used by another item.");
        }
    }

    public void delete(int itemId) throws SQLException, ValidationException {
        try (PreparedStatement ps = con().prepareStatement("DELETE FROM inventory_hardware WHERE item_id=?")) {
            ps.setInt(1, itemId);
            ps.executeUpdate();
        } catch (SQLIntegrityConstraintViolationException e) {
            throw new ValidationException("This item has sales history and cannot be deleted. "
                    + "Set its stock to 0 instead.");
        }
    }

    /** Factory pattern in action: the category column decides which subclass is created. */
    private HardwareItem mapRow(ResultSet rs) throws SQLException {
        HardwareItem item = HardwareFactory.create(rs.getString("category"));
        item.setId(rs.getInt("item_id"));
        item.setItemCode(rs.getString("item_code"));
        item.setBrand(rs.getString("brand"));
        item.setModel(rs.getString("model"));
        item.setUnitPrice(rs.getDouble("unit_price"));
        item.setStockQty(rs.getInt("stock_qty"));
        item.setWarrantyMonths(rs.getInt("warranty_months"));
        item.setInstallerWarrantyFee(rs.getDouble("installer_warranty_fee"));
        item.setSupplierId(rs.getInt("supplier_id"));
        Date received = rs.getDate("date_received");
        if (received != null) {
            item.setDateReceived(received.toLocalDate());
        }

        if (item instanceof SolarPanel p) {
            p.setWattage(rs.getInt("wattage_w"));
        } else if (item instanceof Inverter i) {
            i.setInverterType(rs.getString("inverter_type"));
            i.setCapacityKw(rs.getDouble("capacity_kw"));
        } else if (item instanceof Battery b) {
            b.setCapacityKwh(rs.getDouble("capacity_kwh"));
            b.setCycleLife(rs.getInt("cycle_life"));
        }
        return item;
    }

    private void bind(PreparedStatement ps, HardwareItem item) throws SQLException {
        Integer wattage = null;
        String invType = null;
        Double capKw = null, capKwh = null;
        Integer cycles = null;

        if (item instanceof SolarPanel p) {
            wattage = p.getWattage();
        } else if (item instanceof Inverter i) {
            invType = i.getInverterType();
            capKw = i.getCapacityKw();
        } else if (item instanceof Battery b) {
            capKwh = b.getCapacityKwh();
            cycles = b.getCycleLife();
        }

        ps.setString(1, item.getItemCode());
        ps.setString(2, item.getCategory());
        ps.setString(3, item.getBrand());
        ps.setString(4, item.getModel());
        setInt(ps, 5, wattage);
        setString(ps, 6, invType);
        setDouble(ps, 7, capKw);
        setDouble(ps, 8, capKwh);
        setInt(ps, 9, cycles);
        ps.setDouble(10, item.getUnitPrice());
        ps.setInt(11, item.getStockQty());
        ps.setInt(12, item.getWarrantyMonths());
        ps.setDouble(13, item.getInstallerWarrantyFee());
        ps.setInt(14, item.getSupplierId());
        ps.setDate(15, Date.valueOf(item.getDateReceived()));
    }

    private void setInt(PreparedStatement ps, int idx, Integer v) throws SQLException {
        if (v == null) ps.setNull(idx, Types.INTEGER); else ps.setInt(idx, v);
    }

    private void setDouble(PreparedStatement ps, int idx, Double v) throws SQLException {
        if (v == null) ps.setNull(idx, Types.DECIMAL); else ps.setDouble(idx, v);
    }

    private void setString(PreparedStatement ps, int idx, String v) throws SQLException {
        if (v == null) ps.setNull(idx, Types.VARCHAR); else ps.setString(idx, v);
    }
}