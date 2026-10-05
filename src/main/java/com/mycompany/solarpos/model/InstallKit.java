package com.mycompany.solarpos.model;

public class InstallKit extends HardwareItem {
    @Override public String getCategory() { return "KIT"; }
    @Override public String getTestingRequirement() { return "Visual inspection"; }
    @Override public double getKwContribution(int qty) { return 0; }
}