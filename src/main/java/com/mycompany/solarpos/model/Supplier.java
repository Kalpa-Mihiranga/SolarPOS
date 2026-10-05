package com.mycompany.solarpos.model;

public class Supplier {
    private final int id;
    private final String name;

    public Supplier(int id, String name) {
        this.id = id;
        this.name = name;
    }

    public int getId() { return id; }
    public String getName() { return name; }

    @Override
    public String toString() { return name; }   // what the combo box displays
}