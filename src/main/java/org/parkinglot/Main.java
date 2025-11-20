package org.parkinglot;

import org.parkinglot.coststrategy.StandardCostComputationStrategy;
import org.parkinglot.parkingFloor.ParkingFloor;
import org.parkinglot.parkingStrategy.NearestAvailableSpotStrategy;
import org.parkinglot.parkingStrategy.RandomSpotStrategy;
import org.parkinglot.parkingticket.ParkingTicket;
import org.parkinglot.payment.CardPaymentProcessor;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;


public class Main {
    public static void main(String[] args) throws InterruptedException {
        // Test 1: Sequential parking (original behavior)
        System.out.println("=== TEST 1: Sequential Vehicle Parking ===\n");
        testSequentialParking();

        System.out.println("\n\n=== TEST 2: Concurrent Vehicle Parking (Multiple Threads) ===\n");
        testConcurrentParking();
    }

    /**
     * Demonstrates sequential vehicle parking (original behavior)
     */
    private static void testSequentialParking() throws InterruptedException {
        ParkingLot lot = new ParkingLot(new NearestAvailableSpotStrategy(), new CardPaymentProcessor(), new StandardCostComputationStrategy());

        // Setup parking lot with limited spots
        ParkingFloor floor1 = new ParkingFloor("F1");
        floor1.addSpot(new ParkingSpot("F1-S1", SpotType.SMALL));
        floor1.addSpot(new ParkingSpot("F1-S2", SpotType.MEDIUM));
        floor1.addSpot(new ParkingSpot("F1-S3", SpotType.LARGE));
        lot.addFloor(floor1);

        ParkingFloor floor2 = new ParkingFloor("F2");
        floor2.addSpot(new ParkingSpot("F2-S1", SpotType.SMALL));
        floor2.addSpot(new ParkingSpot("F2-S2", SpotType.LARGE));
        lot.addFloor(floor2);

        for (ParkingFloor floor : lot.getFloors()) {
            floor.showFloorDisplay();
        }

        // Create vehicles
        Vehicle car1 = new Vehicle("KA-01-1234", VehicleType.CAR);
        Vehicle truck1 = new Vehicle("KA-99-8888", VehicleType.TRUCK);
        Vehicle car2 = new Vehicle("KA-05-5678", VehicleType.CAR);
        Vehicle bus1 = new Vehicle("KA-09-0001", VehicleType.BUS);

        // Park vehicles sequentially
        System.out.println("Parking vehicles sequentially...\n");
        ParkingTicket t1 = lot.getEntryPanel().parkVehicle(car1, lot);
        ParkingTicket t2 = lot.getEntryPanel().parkVehicle(truck1, lot);
        ParkingTicket t3 = lot.getEntryPanel().parkVehicle(car2, lot);
        ParkingTicket t4 = lot.getEntryPanel().parkVehicle(bus1, lot);

        // Simulate time passage
        Thread.sleep(5000);

        // Unpark vehicles
        System.out.println("\nReleasing vehicles...\n");
        if (t1 != null) lot.getExitPanel().unparkVehicle(t1, lot);
        if (t2 != null) lot.getExitPanel().unparkVehicle(t2, lot);
        if (t3 != null) lot.getExitPanel().unparkVehicle(t3, lot);
        if (t4 != null) lot.getExitPanel().unparkVehicle(t4, lot);
    }

    /**
     * Demonstrates concurrent vehicle parking with multiple threads.
     * This tests the thread-safety of the parking system when multiple vehicles
     * request spots simultaneously.
     */
    private static void testConcurrentParking() throws InterruptedException {
        ParkingLot lot = new ParkingLot(new NearestAvailableSpotStrategy(), new CardPaymentProcessor(), new StandardCostComputationStrategy());

        // Setup parking lot with limited spots (fewer than vehicles trying to park)
        ParkingFloor floor1 = new ParkingFloor("F1");
        floor1.addSpot(new ParkingSpot("F1-S1", SpotType.SMALL));
        floor1.addSpot(new ParkingSpot("F1-S2", SpotType.MEDIUM));
        floor1.addSpot(new ParkingSpot("F1-S3", SpotType.LARGE));
        lot.addFloor(floor1);

        ParkingFloor floor2 = new ParkingFloor("F2");
        floor2.addSpot(new ParkingSpot("F2-S1", SpotType.SMALL));
        floor2.addSpot(new ParkingSpot("F2-S2", SpotType.LARGE));
        lot.addFloor(floor2);

        System.out.println("\nInitial Parking Lot Status:");
        for (ParkingFloor floor : lot.getFloors()) {
            floor.showFloorDisplay();
        }

        System.out.println("Starting concurrent parking test with 10 vehicles competing for 5 spots...\n");

        // Create multiple threads to simulate concurrent vehicle parking requests
        ExecutorService executor = Executors.newFixedThreadPool(10);
        List<ParkingTicket> tickets = new ArrayList<>();

        // Create 10 vehicles trying to park concurrently
        for (int i = 0; i < 10; i++) {
            final int vehicleId = i;
            executor.submit(() -> {
                String plateNumber = String.format("KA-%02d-%04d", vehicleId / 2, vehicleId * 100);
                VehicleType type = vehicleId % 3 == 0 ? VehicleType.TRUCK : VehicleType.CAR;
                Vehicle vehicle = new Vehicle(plateNumber, type);

                System.out.println("[Thread-" + Thread.currentThread().getId() + "] Vehicle " + plateNumber + " requesting parking...");
                ParkingTicket ticket = lot.getEntryPanel().parkVehicle(vehicle, lot);

                if (ticket != null) {
                    synchronized (tickets) {
                        tickets.add(ticket);
                    }
                } else {
                    System.out.println("[Thread-" + Thread.currentThread().getId() + "] Vehicle " + plateNumber + " parking FAILED");
                }
            });
        }

        // Wait for all threads to complete
        executor.shutdown();
        boolean finished = executor.awaitTermination(30, TimeUnit.SECONDS);

        if (!finished) {
            System.out.println("Timeout waiting for parking operations to complete");
            executor.shutdownNow();
        }

        System.out.println("\n\n=== Parking Result Summary ===");
        System.out.println("Total vehicles parked successfully: " + tickets.size() + " out of 10");

        // Display final parking lot status
        System.out.println("\nFinal Parking Lot Status:");
        for (ParkingFloor floor : lot.getFloors()) {
            floor.showFloorDisplay();
        }

        // Cleanup: unpark all vehicles
        System.out.println("\nReleasing parked vehicles...\n");
        for (ParkingTicket ticket : tickets) {
            if (ticket != null) {
                lot.getExitPanel().unparkVehicle(ticket, lot);
            }
        }

        System.out.println("\nConcurrent parking test completed!");
    }
}