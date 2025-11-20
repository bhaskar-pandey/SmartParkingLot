package org.parkinglot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.parkinglot.panels.EntryPanel;
import org.parkinglot.panels.ExitPanel;
import org.parkinglot.parkingFloor.ParkingFloor;
import org.parkinglot.parkingStrategy.ParkingStrategy;
import org.parkinglot.parkingticket.ParkingTicket;
import org.parkinglot.payment.CardPaymentProcessor;
import org.parkinglot.payment.PaymentProcessor;
import org.parkinglot.coststrategy.CostComputationStrategy;
import org.parkinglot.coststrategy.StandardCostComputationStrategy;


public class ParkingLot {
    private final List<ParkingFloor> floors;
    private final EntryPanel entryPanel;
    private ExitPanel exitPanel;
    // Thread-safe map for managing active tickets
    private final Map<String, ParkingTicket> activeTickets = Collections.synchronizedMap(new HashMap<>());

    public ParkingLot(ParkingStrategy strategy, PaymentProcessor paymentProcessor, CostComputationStrategy costStrategy) {
        this.floors = new ArrayList<>();
        this.entryPanel = new EntryPanel(strategy);
        this.exitPanel = new ExitPanel(paymentProcessor, costStrategy);
    }

    public void addFloor(ParkingFloor floor) {
        floors.add(floor);
    }

    public List<ParkingFloor> getFloors() {
        return floors;
    }

    public EntryPanel getEntryPanel() {
        return entryPanel;
    }

    public ExitPanel getExitPanel() {
        return exitPanel;
    }

    public void changeStrategy(ParkingStrategy strategy) {
        entryPanel.changeStrategy(strategy);
    }

    public void issueTicket(ParkingTicket ticket) {
        activeTickets.put(ticket.getTicketId(), ticket);
    }

    public ParkingTicket getTicket(String ticketId) {
        return activeTickets.get(ticketId);
    }

    public void setExitPanel(ExitPanel exitPanel) {
        this.exitPanel = exitPanel;
    }

    public ParkingSpot getSpotById(String spotId) {
        for (ParkingFloor floor : floors) {
            ParkingSpot spot = floor.getSpotById(spotId);
            if (spot != null) {
                return spot;
            }
        }
        return null;
    }



    public void isParkingLotFull() {
        for (ParkingFloor floor : floors) {
            if (!floor.isFull()) {
                System.out.println("Parking lot is not full.");
                return;
            }
        }
        System.out.println("Parking lot is full.");
    }

}
