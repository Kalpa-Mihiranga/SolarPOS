package com.mycompany.solarpos.factory;

import com.mycompany.solarpos.model.*;

/** Factory pattern: creates the right HardwareItem subclass from a category name. */
public class HardwareFactory {

    private HardwareFactory() { }

    public static HardwareItem create(String category) {
        if (category == null) {
            throw new IllegalArgumentException("Category is required");
        }
        switch (category.toUpperCase()) {
            case "PANEL":    return new SolarPanel();
            case "INVERTER": return new Inverter();
            case "BATTERY":  return new Battery();
            case "KIT":      return new InstallKit();
            default: throw new IllegalArgumentException("Unknown category: " + category);
        }
    }
}