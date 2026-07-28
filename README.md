# ParkVault — Smart Parking System

A desktop **Smart Parking System** built with plain **Java + Swing + JDBC + SQLite**.
Pick a floor, see a live colour-coded grid of parking slots, book a slot for a
vehicle, check it out with an automatic fee, search for where a vehicle is
parked, and let a background thread auto-release expired bookings.

> This project was migrated from an earlier movie-ticket-booking system
> (ShowVault). The seat-booking concurrency model mapped naturally onto parking
> slots, so the architecture was reused and renamed to the parking domain.

![ParkVault main window](screenshots/parkvault_main.png)

---

## Features

1. **Floor selector** — switch between the three parking floors.
2. **Live slot grid** — colour-coded: **green** FREE, **red** OCCUPIED, **grey** DISABLED.
3. **Book a slot** — enter a vehicle number and the expected hours.
4. **Auto-release** — a background thread frees bookings past their expected exit.
5. **Checkout** — fee = hours parked × the hourly rate for the slot's vehicle type.
6. **Find my vehicle** — search by vehicle number to see its floor and slot.
7. **Booking history** — a table of every past and present booking.

---

## The three graded requirements

The teacher's requirements are **Exception Handling**, **Multithreading**, and a
**Frontend**. Here is exactly where each one lives.

### 1. Exception Handling (layer by layer)
- **DAO layer** (`dao/`): every method uses try-with-resources for the
  `Connection`/`Statement`/`ResultSet`, catches `SQLException`, and re-throws it
  wrapped in a meaningful [`DataAccessException`](src/exception/DataAccessException.java).
  Nothing is swallowed.
- **Service layer** (`service/`): throws typed business exceptions —
  [`SlotOccupiedException`](src/exception/SlotOccupiedException.java),
  [`BookingNotFoundException`](src/exception/BookingNotFoundException.java),
  [`InvalidVehicleException`](src/exception/InvalidVehicleException.java) (plate
  validated with a regex).
- **UI layer** (`ui/`): one catch point per action inside a `SwingWorker.done()`,
  shows a friendly `JOptionPane`, and logs the full stack trace via
  [`AppLogger`](src/util/AppLogger.java). No `printStackTrace()`-only handling,
  no empty catch blocks.

### 2. Multithreading (three mechanisms)
- [`ExpiryScheduler`](src/concurrent/ExpiryScheduler.java) — a
  `ScheduledExecutorService` that runs **every 30 seconds**, frees expired
  bookings, and refreshes the UI via `SwingUtilities.invokeLater()`. It is shut
  down cleanly when the window closes.
- [`SlotLockManager`](src/concurrent/SlotLockManager.java) — a
  `ConcurrentHashMap<Integer, ReentrantLock>`, one lock per slot id, acquired
  with `tryLock(2, TimeUnit.SECONDS)` and always released in a `finally` block.
- **`SwingWorker`** — every database call runs off the Event Dispatch Thread, so
  the UI never blocks.
- **Demo button** — *"Simulate 10 Concurrent Bookings"* fires 10 threads at the
  **same** slot at once and prints a result log: exactly **1 success and 9
  `SlotOccupiedException` rejections**.

### 3. Frontend
A modern **Java Swing** interface (Nimbus look-and-feel plus custom Java2D
painting): a gradient header, rounded cards, colour-coded parking-slot glyphs, a
booking dialog, and a history table.

---

## Concurrency correctness (why it is actually safe)

The Java lock alone is **not** the authoritative guard — it only serialises
threads inside one JVM. The real guard is at the SQL level. Booking runs inside
a transaction and occupies a slot with a single **atomic conditional update**:

```sql
UPDATE slot SET status = 'OCCUPIED' WHERE id = ? AND status = 'FREE'
```

The affected-row count decides the winner:
- **1 row updated** → this booking won; insert the booking row and `commit()`.
- **0 rows updated** → another booking already took it → `rollback()` and throw
  `SlotOccupiedException`.

See [`SlotDAO.tryOccupySlot`](src/dao/SlotDAO.java) and
[`ParkingService.bookSlot`](src/service/ParkingService.java)
(`setAutoCommit(false)` → `commit()` on success, `rollback()` in `catch`).

---

## Architecture

A clean layered design; each package has one responsibility.

```
src/
  Main.java              Entry point: prepares DB, wires layers, shows the window
  SetupDatabase.java     Optional helper to rebuild the DB from schema.sql
  ui/                    Swing frontend
    MainFrame            Floor selector + toolbar + grid + legend + scheduler
    SlotGridPanel        Live colour-coded slot grid (book / checkout)
    BookingDialog        Vehicle number + expected hours form
    HistoryPanel         Read-only booking history table
    UITheme              Shared palette, fonts, and custom-painted components
  service/               Business logic
    ParkingService       Booking, checkout, search, auto-release, demo
    BillingService       Fee = chargeable hours × hourly rate
  dao/                   Data Access Objects (all SQL lives here)
    FloorDAO, SlotDAO, BookingDAO, RateDAO
  model/                 Plain data classes / enums
    Floor, Slot, Booking, VehicleType, SlotStatus
  exception/             Typed exceptions
    SlotOccupiedException, BookingNotFoundException,
    InvalidVehicleException, DataAccessException
  concurrent/            Threading
    SlotLockManager, ExpiryScheduler
  util/                  Infrastructure
    DBConnection, DatabaseInitializer, AppLogger, TimeUtil
sql/schema.sql           Drop + create + seed (3 floors, 60 slots, rates)
lib/                     sqlite-jdbc driver (the only dependency)
```

### Database schema

| Table     | Key columns |
|-----------|-------------|
| `floor`   | id, name, level |
| `slot`    | id, floor_id → floor, code (`A-01`), vehicle_type (CAR/BIKE), status (FREE/OCCUPIED/DISABLED) |
| `booking` | id, slot_id → slot, vehicle_no, entry_time, expected_exit, actual_exit, amount, status (ACTIVE/COMPLETED) |
| `rate`    | id, vehicle_type (unique), hourly_rate |

Seed data: 3 floors × 20 slots (60 total), rates CAR = Tk.60/hr, BIKE = Tk.30/hr,
and a few pre-occupied slots (one of which is intentionally past-due to
demonstrate auto-release).

---

## Setup & Run

**Requirements:** JDK 17+ (only the bundled `lib/sqlite-jdbc` driver is needed —
no MySQL, no server, no extra install).

### In VS Code
Open this folder and run [`Main.java`](src/Main.java). The database is created
automatically on first launch.

### From the command line

macOS / Linux:
```bash
javac -d bin -cp "lib/sqlite-jdbc-3.46.1.3.jar" $(find src -name "*.java")
java -cp "bin:lib/sqlite-jdbc-3.46.1.3.jar" Main
```

Windows (PowerShell): use `;` as the classpath separator:
```powershell
javac -d bin -cp "lib/sqlite-jdbc-3.46.1.3.jar" (Get-ChildItem -Recurse src -Filter *.java).FullName
java -cp "bin;lib/sqlite-jdbc-3.46.1.3.jar" Main
```

To wipe all bookings and reseed, run `SetupDatabase` the same way, or just delete
`database/parkvault.db` and relaunch.

---

## How to demo this in 3 minutes

1. **Launch** `Main`. The grid shows Ground Floor: green = free, red = occupied
   (A-03, A-08), grey = disabled (A-20).
2. **Auto-release (multithreading):** wait ~30 seconds without touching anything.
   Slot **A-08** (seeded past its exit time) turns from red to green on its own —
   that is the background `ExpiryScheduler` firing.
3. **Book a slot (exceptions + transaction):** click any green slot, enter a
   vehicle number like `DHA-GA-12-3456`, choose hours, click **Book**. It turns
   red. Try an invalid number like `!!` to see the validation exception.
4. **Find my vehicle:** click **Find My Vehicle**, type the number you just
   booked — it reports the floor and slot.
5. **Checkout (billing):** click that red slot → **Yes** → a receipt shows the
   hours and fee. The slot turns green again.
6. **Concurrency (the graded highlight):** click **Simulate 10 Concurrent
   Bookings**. A log appears showing **1 SUCCESS and 9 REJECTED** — proof that
   the atomic SQL guard lets exactly one thread win.
7. **History:** click **History** to see every booking in a table.
# SpotLock
