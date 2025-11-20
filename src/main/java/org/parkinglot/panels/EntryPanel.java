package org.parkinglot.panels;

import org.parkinglot.ParkingLot;
import org.parkinglot.ParkingSpot;
import org.parkinglot.Vehicle;
import org.parkinglot.displayPanel.EntryDisplayPanel;
import org.parkinglot.parkingStrategy.ParkingStrategy;
import org.parkinglot.parkingticket.ParkingTicket;
import org.parkinglot.parkingticket.ParkingTicketGenerator;


public class EntryPanel {
    private ParkingStrategy strategy;
    private final EntryDisplayPanel displayPanel;
    private final ParkingTicketGenerator ticketGenerator;
    private static final int MAX_RETRIES = 3;

    public EntryPanel(ParkingStrategy strategy) {
        this(strategy, new ParkingTicketGenerator());
    }

    public EntryPanel(ParkingStrategy strategy, ParkingTicketGenerator ticketGenerator) {
        this.strategy = strategy;
        this.displayPanel = new EntryDisplayPanel();
        this.ticketGenerator = ticketGenerator;
    }

    public void changeStrategy(ParkingStrategy strategy) {
        this.strategy = strategy;
    }

    /**
     * Parks a vehicle with retry logic to handle concurrent spot requests.
     * If the found spot is claimed by another vehicle, tries to find an alternative spot.
     *
     * @param vehicle the vehicle to park
     * @param parkingLot the parking lot
     * @return the ParkingTicket if successful, null if no spot could be procured
     */
    public ParkingTicket parkVehicle(Vehicle vehicle, ParkingLot parkingLot) {
        ParkingTicket ticket = null;
        int retryCount = 0;

        while (retryCount < MAX_RETRIES && ticket == null) {
            // Find an available spot
            ParkingSpot spot = strategy.findSpot(vehicle, parkingLot);

            if (spot != null) {
                // Attempt to atomically reserve the spot
                if (spot.tryParkVehicle(vehicle)) {
                    // Successfully reserved the spot
                    ticket = ticketGenerator.generateTicket(vehicle, spot);
                    parkingLot.issueTicket(ticket);
                    displayPanel.displayTicketIssued(ticket);
                    return ticket;
                } else {
                    // Spot was claimed by another vehicle, retry
                    retryCount++;
                    System.out.println("[RETRY " + retryCount + "] Spot " + spot.getId() + " was taken by another vehicle. Trying alternative spot for " + vehicle.getLicenseNumber());

                    // Small delay before retrying to reduce contention
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            } else {
                // No spots available
                System.out.println("[FAILED] No available spots for vehicle " + vehicle.getLicenseNumber());
                System.out.println("[FAILED] Vehicle " + vehicle.getLicenseNumber() + " could not find a parking spot after " + MAX_RETRIES + " attempts");
                return null;
            }
        }
        return ticket;
    }
}