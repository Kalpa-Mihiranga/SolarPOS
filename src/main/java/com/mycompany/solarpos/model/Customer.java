package com.mycompany.solarpos.model;

public class Customer {
    private int id;
    private String name;
    private String type;          // RESIDENTIAL or COMMERCIAL
    private String phone;
    private String email;
    private String address;
    private String gridPhase;     // SINGLE or THREE
    private int contractorId;     // 0 = no contractor
    private String contractorName;

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public String getGridPhase() { return gridPhase; }
    public void setGridPhase(String gridPhase) { this.gridPhase = gridPhase; }
    public int getContractorId() { return contractorId; }
    public void setContractorId(int contractorId) { this.contractorId = contractorId; }
    public String getContractorName() { return contractorName; }
    public void setContractorName(String contractorName) { this.contractorName = contractorName; }

    @Override
    public String toString() { return name + " (" + phone + ")"; }   // used by the POS customer picker later
}