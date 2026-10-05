package com.mycompany.solarpos.util;

import com.mycompany.solarpos.dao.InventoryDAO;
import com.mycompany.solarpos.dao.OrderDAO;
import com.mycompany.solarpos.model.CartLine;
import com.mycompany.solarpos.model.HardwareItem;
import com.mycompany.solarpos.model.OrderSummary;
import com.mycompany.solarpos.service.PackagePreset;
import com.mycompany.solarpos.service.PricingService;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BackendCheck {

    // Set to true ONLY if you want to save a real order (it reduces stock).
    private static final boolean SAVE_REAL_ORDER = false;

    public static void main(String[] args) throws Exception {
        InventoryDAO inv = new InventoryDAO();
        OrderDAO orders = new OrderDAO();
        PricingService pricing = new PricingService();

        Map<String, HardwareItem> byCode = load(inv);

        // ---- Test 1: pricing of the 5 kWh package
        PackagePreset preset = PackagePreset.all().get(0);
        List<CartLine> cart = preset.build(byCode);
        OrderSummary s = pricing.calculate(cart);
        System.out.println("=== TEST 1: pricing for " + preset + " ===");
        System.out.printf("Subtotal      : %,.2f%n", s.getSubtotal());
        System.out.printf("Discount      : %,.2f  (%s)%n", s.getDiscount(), s.getBundleLabel());
        System.out.printf("Warranty fees : %,.2f%n", s.getWarrantyFees());
        System.out.printf("Tax (18%%)     : %,.2f%n", s.getTax());
        System.out.printf("Grand total   : %,.2f%n", s.getGrandTotal());
        System.out.printf("Min down pay  : %,.2f%n", s.getMinDownPayment());
        System.out.printf("Total kW      : %.2f%n", s.getTotalKw());

        // ---- Test 2: rollback when one line has too little stock
        System.out.println();
        System.out.println("=== TEST 2: rollback on insufficient stock ===");
        int stockBefore = byCode.get("KIT-DC01").getStockQty();
        int ordersBefore = orders.countOrders();

        List<CartLine> bad = List.of(
                new CartLine(byCode.get("KIT-DC01"), 1),      // valid line
                new CartLine(byCode.get("PNL-JK550"), 1000)); // far more than in stock
        double total = pricing.calculate(bad).getGrandTotal();
        try {
            orders.saveOrder(1, 1, bad, total);
            System.out.println("ERROR: the sale should have been rejected!");
        } catch (InsufficientStockException ex) {
            System.out.println("Rejected as expected: " + ex.getMessage());
        }

        int stockAfter = load(inv).get("KIT-DC01").getStockQty();
        System.out.println("KIT-DC01 stock before/after : " + stockBefore + " / " + stockAfter);
        System.out.println("Orders before/after         : " + ordersBefore + " / " + orders.countOrders());
        System.out.println(stockBefore == stockAfter && ordersBefore == orders.countOrders()
                ? "ROLLBACK OK" : "ROLLBACK FAILED");

        // ---- Test 3: down payment rule
        System.out.println();
        System.out.println("=== TEST 3: down payment below 30% ===");
        try {
            orders.saveOrder(1, 1, cart, 1000);
        } catch (ValidationException ex) {
            System.out.println("Rejected as expected: " + ex.getMessage());
        }

        // ---- Optional: real save
        if (SAVE_REAL_ORDER) {
            int id = orders.saveOrder(1, 1, cart, s.getMinDownPayment());
            System.out.println();
            System.out.println("Saved real order #" + id);
        }
    }

    private static Map<String, HardwareItem> load(InventoryDAO inv) throws Exception {
        Map<String, HardwareItem> map = new HashMap<>();
        for (HardwareItem it : inv.findAll()) {
            map.put(it.getItemCode(), it);
        }
        return map;
    }
}