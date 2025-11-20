# Smart Parking Lot System

A comprehensive, production-grade parking lot management system built with Java that handles multiple vehicles with **thread-safe concurrent parking operations**, flexible pricing strategies, and support for multiple payment methods.

## Features

### Core Functionality
- ✅ **Multi-floor parking lot management** with customizable spot types (SMALL, MEDIUM, LARGE)
- ✅ **Vehicle type support** (CAR, BUS, TRUCK) with appropriate spot size mapping
- ✅ **Concurrent vehicle parking** with atomic spot reservation and retry logic
- ✅ **Flexible parking strategies** (Nearest Available, Random)
- ✅ **Multiple cost computation strategies** (Standard, Premium)
- ✅ **Multiple payment processors** (Card, Cash)
- ✅ **Maintenance mode** for floors under maintenance
- ✅ **Thread-safe operations** using ReentrantReadWriteLock for spot reservations

### Architecture Patterns
- Strategy Pattern (Parking strategies, Cost computation)
- Factory Pattern (Parking spot creation, Payment processor creation)
- Builder Pattern (Extensible parking lot configuration)

---

## Application Flow

### 1. System Initialization

```
main() 
  ↓
Create ParkingLot(parkingStrategy, paymentProcessor, costStrategy)
  ├─ Initialize EntryPanel with parking strategy
  ├─ Initialize ExitPanel with payment processor & cost strategy
  └─ Create empty parking floors list

Add ParkingFloors to ParkingLot
  ├─ Create Floor (F1, F2, F3, etc.)
  ├─ Add ParkingSpots to each floor
  │  ├─ Spot Type: SMALL, MEDIUM, LARGE
  │  └─ Each spot has unique ID (e.g., F1-S1)
  └─ Set maintenance status (optional)
```

### 2. Vehicle Parking Flow

```
Vehicle Arrives
  ↓
EntryPanel.parkVehicle(vehicle, parkingLot) [MAIN ENTRY POINT]
  ↓
  ├─ Retry Loop (max 3 attempts)
  │  ├─ Call ParkingStrategy.findSpot(vehicle, parkingLot)
  │  │  └─ Search through floors → find first available spot
  │  │     that matches vehicle type requirements
  │  │
  │  ├─ If spot found:
  │  │  └─ Call spot.tryParkVehicle(vehicle) [ATOMIC OPERATION]
  │  │     ├─ Acquire WriteLock
  │  │     ├─ Double-check: spot still available?
  │  │     ├─ Validate: vehicle type matches spot?
  │  │     ├─ If both OK: Mark occupied + Park vehicle
  │  │     ├─ Release WriteLock
  │  │     └─ Return true/false (success/failure)
  │  │
  │  ├─ If parking succeeded:
  │  │  ├─ Generate ParkingTicket with vehicle & spot info
  │  │  ├─ Issue ticket to ParkingLot (thread-safe map)
  │  │  └─ Return ticket
  │  │
  │  └─ If parking failed (spot taken by another thread):
  │     ├─ Sleep 10ms (backoff)
  │     └─ Retry with new spot search
  │
  └─ If no spot available after 3 retries:
     └─ Return null (parking failed)
```

### 3. Vehicle Exit Flow

```
Vehicle Exits (with valid ParkingTicket)
  ↓
ExitPanel.unparkVehicle(ticket, parkingLot) [MAIN EXIT POINT]
  ↓
  ├─ Validate ticket
  ├─ Get spot from ticket ID
  │  └─ Call parkingLot.getSpotById(spotId)
  │
  ├─ Calculate parking cost
  │  └─ costStrategy.computeCost(ticket)
  │     ├─ Calculate duration: (exit_time - entry_time)
  │     ├─ Apply cost formula (depends on strategy)
  │     └─ Return total cost
  │
  ├─ Process payment
  │  └─ paymentProcessor.processPayment(cost)
  │     ├─ Execute payment transaction
  │     └─ Return success/failure
  │
  ├─ Remove vehicle from spot (THREAD-SAFE)
  │  └─ spot.removeVehicle() [ATOMIC]
  │     ├─ Acquire WriteLock
  │     ├─ Clear parkedVehicle reference
  │     ├─ Mark spot as unoccupied
  │     └─ Release WriteLock
  │
  └─ Display exit receipt
```

### 4. Concurrent Operation Flow

```
Multiple Threads Competing for Same Spot:

Time  Thread A (Vehicle 1)          Thread B (Vehicle 2)
────  ──────────────────────────    ──────────────────────────
 T0   findSpot() → Spot X           findSpot() → Spot X
      Both see same available spot

 T1   tryParkVehicle()              tryParkVehicle()
      Acquire WriteLock             [BLOCKED waiting for lock]
      
 T2   Double-check available ✓      
      Mark occupied = true
      Release WriteLock
      Return true
      
 T3                                 [Acquire WriteLock]
                                    Double-check available ✗
                                    [occupied = true from T2]
                                    Release WriteLock
                                    Return false
      
 T4   ticket = issued               Retry with different spot
      PARKING SUCCESS                (or fail after 3 retries)
```

---

## Component Overview

### Core Components

#### 1. **ParkingLot** (Main Coordinator)
- Manages all parking floors
- Coordinates EntryPanel and ExitPanel
- Maintains thread-safe ticket registry
- Methods: `addFloor()`, `getFloors()`, `getSpotById()`, `issueTicket()`, `getTicket()`

#### 2. **ParkingFloor** (Floor Manager)
- Contains multiple ParkingSpots organized by type
- Supports maintenance mode
- Methods: `addSpot()`, `getAvailableSpot()`, `getSpotById()`, `isFull()`, `setUnderMaintenance()`

#### 3. **ParkingSpot** (Thread-Safe Reservation Unit) ⭐
- **Thread-safe atomic spot reservation** using ReentrantReadWriteLock
- Methods:
  - `canFitVehicle(vehicle)` - Non-blocking read check
  - `tryParkVehicle(vehicle)` - Atomic CAS-like operation
  - `removeVehicle()` - Safe cleanup
  - `isOccupied()` - Thread-safe status check

#### 4. **EntryPanel** (Entry Gateway)
- Orchestrates vehicle parking process
- Implements retry logic with exponential backoff
- Methods: `parkVehicle(vehicle, lot)`, `changeStrategy()`

#### 5. **ExitPanel** (Exit Gateway)
- Orchestrates vehicle exit and payment
- Methods: `unparkVehicle(ticket, lot)`

#### 6. **ParkingTicket** (Record)
- Immutable parking record
- Contains: vehicle, spot ID, entry time
- Methods: `getTicketId()`, `getVehicle()`, `getSpotId()`, `getEntryTime()`, `printTicket()`

### Strategy Components

#### Parking Strategies (Interface: `ParkingStrategy`)
- **NearestAvailableSpotStrategy** - First available spot (FIFO search)
- **RandomSpotStrategy** - Random spot selection

#### Cost Strategies (Interface: `CostComputationStrategy`)
- **StandardCostComputationStrategy** - Base rates per vehicle type
- **PremiumCostComputationStrategy** - Higher rates with surcharges

#### Payment Processors (Interface: `PaymentProcessor`)
- **CardPaymentProcessor** - Credit/debit card transactions
- **CashPaymentProcessor** - Cash payment handling

### Data Models

#### Vehicle Types
- **CAR** - Fits SMALL or MEDIUM spots
- **BUS** - Requires LARGE spots
- **TRUCK** - Requires LARGE spots

#### Spot Types
- **SMALL** - Compact parking
- **MEDIUM** - Standard parking
- **LARGE** - Oversized parking (buses, trucks)

---

## Thread Safety Model

### Concurrency Guarantees

| Operation | Thread-Safe | Mechanism |
|-----------|------------|-----------|
| Spot availability check | ✅ | ReadLock |
| Atomic spot reservation | ✅ | WriteLock + Double-check |
| Vehicle removal | ✅ | WriteLock |
| Ticket issuance | ✅ | Synchronized HashMap |
| Ticket retrieval | ✅ | Synchronized HashMap |

### Key Design Decisions

1. **ReentrantReadWriteLock in ParkingSpot**
   - Allows multiple concurrent readers
   - Exclusive write access for reservations
   - Prevents race conditions

2. **tryParkVehicle() Method**
   - Atomic check-and-set operation
   - Returns boolean (success/failure)
   - Never blocks; allows retry logic

3. **Retry with Backoff**
   - Max 3 retry attempts
   - 10ms sleep between retries
   - Reduces lock contention

4. **Synchronized Collections**
   - Thread-safe ticket registry
   - No manual synchronization needed

---

## Usage Examples

### Basic Setup
```java
// Create parking lot
ParkingLot lot = new ParkingLot(
    new NearestAvailableSpotStrategy(),
    new CardPaymentProcessor(),
    new StandardCostComputationStrategy()
);

// Add floors and spots
ParkingFloor floor1 = new ParkingFloor("F1");
floor1.addSpot(new ParkingSpot("F1-S1", SpotType.SMALL));
floor1.addSpot(new ParkingSpot("F1-S2", SpotType.MEDIUM));
floor1.addSpot(new ParkingSpot("F1-S3", SpotType.LARGE));
lot.addFloor(floor1);
```

### Vehicle Parking
```java
Vehicle car = new Vehicle("KA-01-1234", VehicleType.CAR);
ParkingTicket ticket = lot.getEntryPanel().parkVehicle(car, lot);

if (ticket != null) {
    System.out.println("Parked in: " + ticket.getSpotId());
} else {
    System.out.println("No spots available");
}
```

### Vehicle Exit
```java
lot.getExitPanel().unparkVehicle(ticket, lot);
```

### Concurrent Scenario
```java
ExecutorService executor = Executors.newFixedThreadPool(10);
for (int i = 0; i < 10; i++) {
    executor.submit(() -> {
        Vehicle vehicle = new Vehicle("KA-" + UUID.randomUUID(), VehicleType.CAR);
        ParkingTicket ticket = lot.getEntryPanel().parkVehicle(vehicle, lot);
        // Only 5 will succeed if 5 spots available
    });
}
executor.shutdown();
executor.awaitTermination(30, TimeUnit.SECONDS);
```

---

## Running the Application

### Build
```bash
./gradlew clean build
```

### Run Tests
```bash
java -cp build/classes/java/main org.parkinglot.Main
```

### Output
The application runs two test scenarios:
1. **Sequential Parking Test** - Demonstrates basic functionality
2. **Concurrent Parking Test** - 10 vehicles competing for 5 spots, demonstrating thread-safety

---

## Project Structure

```
src/main/java/org/parkinglot/
├── Main.java                           # Application entry point with tests
├── ParkingLot.java                     # Main coordinator
├── ParkingSpot.java                    # Thread-safe spot (with locks)
├── ParkingFloor.java                   # Floor manager
├── Vehicle.java                        # Vehicle model
├── VehicleType.java                    # Enum: CAR, BUS, TRUCK
├── SpotType.java                       # Enum: SMALL, MEDIUM, LARGE
│
├── panels/
│   ├── EntryPanel.java                 # Entry gateway (with retry logic)
│   └── ExitPanel.java                  # Exit gateway
│
├── parkingticket/
│   ├── ParkingTicket.java              # Ticket record
│   └── ParkingTicketGenerator.java     # Ticket factory
│
├── parkingStrategy/
│   ├── ParkingStrategy.java            # Strategy interface
│   ├── NearestAvailableSpotStrategy.java
│   └── RandomSpotStrategy.java
│
├── coststrategy/
│   ├── CostComputationStrategy.java    # Strategy interface
│   ├── StandardCostComputationStrategy.java
│   └── PremiumCostComputationStrategy.java
│
├── payment/
│   ├── PaymentProcessor.java           # Strategy interface
│   ├── CardPaymentProcessor.java
│   └── CashPaymentProcessor.java
│
└── [Other supporting packages]
```

---

## Design Patterns Used

| Pattern | Location | Purpose |
|---------|----------|---------|
| **Strategy** | ParkingStrategy, CostComputationStrategy, PaymentProcessor | Pluggable algorithms |
| **Factory** | ParkingSpotFactory, PaymentProcessorFactory | Object creation |
| **Builder** | ParkingLotBuilder | Fluent configuration |
| **Singleton** | Implicit in strategy instances | Single instance per lot |
| **Command** | EntryPanel, ExitPanel | Encapsulate parking operations |

---

## Performance Characteristics

- **Time Complexity**: O(N) where N = number of floors/spots to search
- **Space Complexity**: O(M) where M = number of spots in lot
- **Lock Contention**: Minimal (short critical sections)
- **Scalability**: Supports 100+ concurrent vehicles with proper configuration

---

## Thread Safety Guarantees ✅

✅ **No Double-Booking** - Only one vehicle per spot  
✅ **Atomic Operations** - CAS-like guarantee with locks  
✅ **No Deadlocks** - Single lock hierarchy per spot  
✅ **Memory Safe** - No leaks; locks always released  
✅ **Graceful Failure** - Clear error handling  

---

