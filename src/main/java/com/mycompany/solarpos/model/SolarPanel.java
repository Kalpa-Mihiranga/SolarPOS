package com.mycompany.solarpos.model;

public class SolarPanel extends HardwareItem {
    private int wattage;

    public int getWattage() { return wattage; }
    public void setWattage(int wattage) { this.wattage = wattage; }

    @Override public String getCategory() { return "PANEL"; }
    @Override public String getTestingRequirement() { return "Flash test & EL imaging"; }
    @Override public double getKwContribution(int qty) { return wattage * qty / 1000.0; }
}