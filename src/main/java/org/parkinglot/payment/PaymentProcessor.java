package org.parkinglot.payment;

public interface PaymentProcessor {
    boolean processPayment(double amount);
}