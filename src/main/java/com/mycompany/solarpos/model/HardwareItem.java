package com.mycompany.solarpos.model;

import java.time.LocalDate;

/** Base class for every hardware item sold by the shop. */
public abstract class HardwareItem {
    protected int id;
    protected String itemCode;
    protected String brand;
    protected String model;
    protected double unitPrice;
    protected int stockQty;
    protected int warrantyMonths;
    protected double installerWarrantyFee;
    protected int supplierId;
    protected LocalDate dateReceived;

    /** Category name as stored in the database: PANEL, INVERTER, BATTERY, KIT. */
    public abstract String getCategory();

    /** Testing each item type must pass before installation. */
    public abstract String getTestingRequirement();

    /** kW of peak power this item adds when sold in the given quantity. */
    public abstract double getKwContribution(int qty);

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getItemCode() { return itemCode; }
    public void setItemCode(String itemCode) { this.itemCode = itemCode; }
    public String getBrand() { return brand; }
    public void setBrand(String brand) { this.brand = brand; }
    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
    public double getUnitPrice() { return unitPrice; }
    public void setUnitPrice(double unitPrice) { this.unitPrice = unitPrice; }
    public int getStockQty() { return stockQty; }
    public void setStockQty(int stockQty) { this.stockQty = stockQty; }
    public int getWarrantyMonths() { return warrantyMonths; }
    public void setWarrantyMonths(int warrantyMonths) { this.warrantyMonths = warrantyMonths; }
    public double getInstallerWarrantyFee() { return installerWarrantyFee; }
    public void setInstallerWarrantyFee(double fee) { this.installerWarrantyFee = fee; }
    public int getSupplierId() { return supplierId; }
    public void setSupplierId(int supplierId) { this.supplierId = supplierId; }
    public LocalDate getDateReceived() { return dateReceived; }
    public void setDateReceived(LocalDate dateReceived) { this.dateReceived = dateReceived; }

    @Override
    public String toString() { return brand + " " + model + " (" + itemCode + ")"; }
}