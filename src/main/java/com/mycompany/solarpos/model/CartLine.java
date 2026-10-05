package com.mycompany.solarpos.model;

/** One row of the POS cart: an inventory item and a quantity. */
public class CartLine {
    private final HardwareItem item;
    private int qty;

    public CartLine(HardwareItem item, int qty) {
        this.item = item;
        this.qty = qty;
    }

    public HardwareItem getItem() { return item; }
    public int getQty() { return qty; }
    public void setQty(int qty) { this.qty = qty; }

    public double getLineTotal() {
        return Math.round(item.getUnitPrice() * qty * 100.0) / 100.0;
    }
}