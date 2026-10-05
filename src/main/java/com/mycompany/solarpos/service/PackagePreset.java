package com.mycompany.solarpos.service;

import com.mycompany.solarpos.model.CartLine;
import com.mycompany.solarpos.model.HardwareItem;
import com.mycompany.solarpos.util.ValidationException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** A ready-made system bundle: a target kWh size mapped to inventory item codes. */
public class PackagePreset {

    private final String name;
    private final double targetKwh;
    private final Map<String, Integer> components = new LinkedHashMap<>();

    private PackagePreset(String name, double targetKwh) {
        this.name = name;
        this.targetKwh = targetKwh;
    }

    private PackagePreset with(String itemCode, int qty) {
        components.put(itemCode, qty);
        return this;
    }

    public static List<PackagePreset> all() {
        return List.of(
            new PackagePreset("5 kWh Home Starter", 5)
                .with("PNL-JK550", 8).with("INV-HW5", 1).with("BAT-HW5", 1)
                .with("KIT-RF01", 2).with("KIT-DC01", 1).with("KIT-AC01", 1),
            new PackagePreset("10 kWh Home Plus", 10)
                .with("PNL-JK550", 18).with("INV-HW8", 1).with("BAT-HW5", 2)
                .with("KIT-RF01", 3).with("KIT-DC01", 1).with("KIT-AC01", 1),
            new PackagePreset("15 kWh Home Premium", 15)
                .with("PNL-JK600", 24).with("INV-GW10", 1).with("BAT-HW5", 3)
                .with("KIT-RF01", 4).with("KIT-DC01", 2).with("KIT-AC01", 1)
        );
    }

    /** Turns the preset into cart lines using live inventory (keyed by item code). */
    public List<CartLine> build(Map<String, HardwareItem> byCode) throws ValidationException {
        List<CartLine> lines = new ArrayList<>();
        for (Map.Entry<String, Integer> e : components.entrySet()) {
            HardwareItem item = byCode.get(e.getKey());
            if (item == null) {
                throw new ValidationException("Package item " + e.getKey() + " is missing from inventory.");
            }
            if (item.getStockQty() < e.getValue()) {
                throw new ValidationException("Not enough stock for " + item + ": need "
                        + e.getValue() + ", available " + item.getStockQty() + ".");
            }
            lines.add(new CartLine(item, e.getValue()));
        }
        return lines;
    }

    public String getName() { return name; }
    public double getTargetKwh() { return targetKwh; }
    public Map<String, Integer> getComponents() { return Collections.unmodifiableMap(components); }

    @Override
    public String toString() { return name; }
}