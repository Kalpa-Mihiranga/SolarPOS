package com.mycompany.solarpos.util;

public class InsufficientStockException extends Exception {
    public InsufficientStockException(String itemName, int requested) {
        super("Insufficient stock for " + itemName + " (requested " + requested + ").");
    }
}