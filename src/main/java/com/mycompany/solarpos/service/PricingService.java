package com.mycompany.solarpos.service;

import com.mycompany.solarpos.model.CartLine;
import com.mycompany.solarpos.model.HardwareItem;
import com.mycompany.solarpos.model.OrderSummary;
import com.mycompany.solarpos.util.ValidationException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

public class PricingService {

    public static final double TAX_RATE = 0.18;
    public static final double DISCOUNT_FULL_BUNDLE = 0.05;      // panels + inverter + battery
    public static final double DISCOUNT_PANEL_INVERTER = 0.03;   // panels + inverter
    public static final double MIN_DOWN_PAYMENT_RATE = 0.30;

    public OrderSummary calculate(List<CartLine> lines) {
        double subtotal = 0;
        double warrantyFees = 0;
        double kw = 0;
        boolean hasPanel = false, hasInverter = false, hasBattery = false;

        for (CartLine line : lines) {
            HardwareItem item = line.getItem();
            subtotal += line.getLineTotal();
            warrantyFees += item.getInstallerWarrantyFee() * line.getQty();
            kw += item.getKwContribution(line.getQty());
            switch (item.getCategory()) {
                case "PANEL"    -> hasPanel = true;
                case "INVERTER" -> hasInverter = true;
                case "BATTERY"  -> hasBattery = true;
                default -> { }
            }
        }

        double rate;
        String label;
        if (hasPanel && hasInverter && hasBattery) {
            rate = DISCOUNT_FULL_BUNDLE;
            label = "Full system bundle (5%)";
        } else if (hasPanel && hasInverter) {
            rate = DISCOUNT_PANEL_INVERTER;
            label = "Panels + inverter bundle (3%)";
        } else {
            rate = 0;
            label = "No bundle discount";
        }

        subtotal = round2(subtotal);
        warrantyFees = round2(warrantyFees);
        double discount = round2(subtotal * rate);
        double taxable = subtotal - discount + warrantyFees;
        double tax = round2(taxable * TAX_RATE);
        double grandTotal = round2(taxable + tax);
        double minDown = round2(grandTotal * MIN_DOWN_PAYMENT_RATE);

        return new OrderSummary(subtotal, rate, label, discount, warrantyFees,
                tax, grandTotal, minDown, round2(kw));
    }

    /** The down payment must be at least 30% of the total and no more than the total. */
    public void validateDownPayment(OrderSummary summary, double downPayment) throws ValidationException {
        if (summary.getGrandTotal() <= 0) {
            throw new ValidationException("The cart is empty.");
        }
        if (downPayment + 0.005 < summary.getMinDownPayment()) {
            throw new ValidationException(String.format(
                    "Down payment must be at least Rs. %,.2f (30%% of the total).",
                    summary.getMinDownPayment()));
        }
        if (downPayment > summary.getGrandTotal() + 0.005) {
            throw new ValidationException("Down payment cannot be more than the total amount.");
        }
    }

    public static double round2(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }
}