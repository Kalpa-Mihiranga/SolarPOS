package com.mycompany.solarpos.model;

public class Contractor {
    private final int id;
    private final String name;
    private final String phone;
    private final String licenseNo;

    public Contractor(int id, String name, String phone, String licenseNo) {
        this.id = id;
        this.name = name;
        this.phone = phone;
        this.licenseNo = licenseNo;
    }

    public int getId() { return id; }
    public String getName() { return name; }
    public String getPhone() { return phone; }
    public String getLicenseNo() { return licenseNo; }

    @Override
    public String toString() { return name; }   // what the combo box displays
}