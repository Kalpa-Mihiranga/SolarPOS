package com.mycompany.solarpos.model;

/** The result of pricing a cart. Immutable. */
public class OrderSummary {
    private final double subtotal;
    private final double discountRate;
    private final String bundleLabel;
    private final double discount;
    private final double warrantyFees;
    private final double tax;
    private final double grandTotal;
    private final double minDownPayment;
    private final double totalKw;

    public OrderSummary(double subtotal, double discountRate, String bundleLabel, double discount,
                        double warrantyFees, double tax, double grandTotal,
                        double minDownPayment, double totalKw) {
        this.subtotal = subtotal;
        this.discountRate = discountRate;
        this.bundleLabel = bundleLabel;
        this.discount = discount;
        this.warrantyFees = warrantyFees;
        this.tax = tax;
        this.grandTotal = grandTotal;
        this.minDownPayment = minDownPayment;
        this.totalKw = totalKw;
    }

    public double getSubtotal() { return subtotal; }
    public double getDiscountRate() { return discountRate; }
    public String getBundleLabel() { return bundleLabel; }
    public double getDiscount() { return discount; }
    public double getWarrantyFees() { return warrantyFees; }
    public double getTax() { return tax; }
    public double getGrandTotal() { return grandTotal; }
    public double getMinDownPayment() { return minDownPayment; }
    public double getTotalKw() { return totalKw; }

    public double getTaxableAmount() {
        return Math.round((subtotal - discount + warrantyFees) * 100.0) / 100.0;
    }
}