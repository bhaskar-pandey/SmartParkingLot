package org.parkinglot.parkingStrategy;

import org.parkinglot.ParkingLot;
import org.parkinglot.ParkingSpot;
import org.parkinglot.Vehicle;


public interface ParkingStrategy {
    ParkingSpot findSpot(Vehicle vehicle, ParkingLot parkingLot);
}