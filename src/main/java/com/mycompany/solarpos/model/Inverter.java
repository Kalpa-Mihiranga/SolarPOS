package com.mycompany.solarpos.model;

public class Inverter extends HardwareItem {
    private String inverterType;   // HYBRID, OFF_GRID, ON_GRID
    private double capacityKw;

    public String getInverterType() { return inverterType; }
    public void setInverterType(String inverterType) { this.inverterType = inverterType; }
    public double getCapacityKw() { return capacityKw; }
    public void setCapacityKw(double capacityKw) { this.capacityKw = capacityKw; }

    @Override public String getCategory() { return "INVERTER"; }
    @Override public String getTestingRequirement() { return "Grid synchronisation test"; }
    @Override public double getKwContribution(int qty) { return 0; } // panels define peak power
}