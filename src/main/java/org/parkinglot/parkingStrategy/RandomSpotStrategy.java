package org.parkinglot.parkingStrategy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.parkinglot.ParkingLot;
import org.parkinglot.parkingFloor.ParkingFloor;
import org.parkinglot.ParkingSpot;
import org.parkinglot.Vehicle;


public class RandomSpotStrategy implements ParkingStrategy {
  @Override
  public ParkingSpot findSpot(Vehicle vehicle, ParkingLot parkingLot) {
    List<ParkingFloor> floors = new ArrayList<>(parkingLot.getFloors());
    Collections.shuffle(floors);
    for (ParkingFloor floor : floors) {
      if (floor.isUnderMaintenance()) continue;

      ParkingSpot spot = floor.getAvailableSpot(vehicle);
      if (spot != null) return spot;
    }
    return null;
  }
}