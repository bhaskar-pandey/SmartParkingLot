package org.parkinglot;

import java.util.concurrent.locks.ReentrantReadWriteLock;

// has a vehicle
public class ParkingSpot {
    private final String id;
    private final SpotType spotType;
    private boolean isOccupied;
    private Vehicle parkedVehicle;
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    public ParkingSpot(String id, SpotType spotType) {
        this.id = id;
        this.spotType = spotType;
        this.isOccupied = false;

    }

    /**
     * Checks if the vehicle can fit without acquiring the spot.
     * Used for availability checking.
     */
    public boolean canFitVehicle(Vehicle vehicle) {
      lock.readLock().lock();
      try {
        if (isOccupied) {
          return false;
        }

        return switch (vehicle.getType()) {
            case CAR -> spotType == SpotType.SMALL || spotType == SpotType.MEDIUM;
            case BUS, TRUCK -> spotType == SpotType.LARGE;
        };
      } finally {
        lock.readLock().unlock();
      }
    }

    /**
     * Attempts to park a vehicle in this spot atomically.
     * Ensures that the spot can fit the vehicle and is still unoccupied.
     *
     * @param vehicle the vehicle to park
     * @return true if the vehicle was successfully parked, false if the spot was already taken
     */
    public boolean tryParkVehicle(Vehicle vehicle) {
        // Double-check: ensure spot is still available and can fit the vehicle
        if(canFitVehicle(vehicle)){
            try {
                lock.writeLock().lock();
                // Spot is available and suitable, park the vehicle
                this.parkedVehicle = vehicle;
                this.isOccupied = true;
                return true;
            }
            finally {
                lock.writeLock().unlock();
            }
        }
        return false;
    }

    /**
     * Removes the vehicle from the spot.
     * Should be called when a vehicle exits the parking lot.
     */
    public void removeVehicle() {
        lock.writeLock().lock();
        try {
            this.parkedVehicle = null;
            this.isOccupied = false;
        } finally {
            lock.writeLock().unlock();
        }
    }

    public boolean isOccupied() {
        lock.readLock().lock();
        try {
            return isOccupied;
        } finally {
            lock.readLock().unlock();
        }
    }

    public String getId() {
        return id;
    }

    public SpotType getSpotType() {
        return spotType;
    }

    public Vehicle getParkedVehicle() {
        lock.readLock().lock();
        try {
            return parkedVehicle;
        } finally {
            lock.readLock().unlock();
        }
    }
}
