package com.mycompany.solarpos.model;

public class Battery extends HardwareItem {
    private double capacityKwh;
    private int cycleLife;

    public double getCapacityKwh() { return capacityKwh; }
    public void setCapacityKwh(double capacityKwh) { this.capacityKwh = capacityKwh; }
    public int getCycleLife() { return cycleLife; }
    public void setCycleLife(int cycleLife) { this.cycleLife = cycleLife; }

    @Override public String getCategory() { return "BATTERY"; }
    @Override public String getTestingRequirement() { return "Capacity discharge test"; }
    @Override public double getKwContribution(int qty) { return 0; }
}