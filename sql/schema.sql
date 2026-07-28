-- ParkVault - Smart Parking System
-- SQLite schema: drop + create + seed. Run automatically on first launch
-- by DatabaseInitializer, or manually via SetupDatabase.

DROP TABLE IF EXISTS booking;
DROP TABLE IF EXISTS slot;
DROP TABLE IF EXISTS floor;
DROP TABLE IF EXISTS rate;

CREATE TABLE floor (
    id    INTEGER PRIMARY KEY AUTOINCREMENT,
    name  TEXT NOT NULL,
    level INTEGER NOT NULL
);

CREATE TABLE slot (
    id           INTEGER PRIMARY KEY AUTOINCREMENT,
    floor_id     INTEGER NOT NULL,
    code         TEXT NOT NULL,
    vehicle_type TEXT NOT NULL,               -- CAR or BIKE
    status       TEXT NOT NULL DEFAULT 'FREE', -- FREE, OCCUPIED or DISABLED
    FOREIGN KEY (floor_id) REFERENCES floor(id)
);

CREATE TABLE booking (
    id            INTEGER PRIMARY KEY AUTOINCREMENT,
    slot_id       INTEGER NOT NULL,
    vehicle_no    TEXT NOT NULL,
    entry_time    TEXT NOT NULL,              -- yyyy-MM-dd HH:mm:ss
    expected_exit TEXT NOT NULL,
    actual_exit   TEXT,                        -- null while the booking is ACTIVE
    amount        REAL,                        -- filled in at checkout
    status        TEXT NOT NULL DEFAULT 'ACTIVE', -- ACTIVE or COMPLETED
    FOREIGN KEY (slot_id) REFERENCES slot(id)
);

CREATE TABLE rate (
    id           INTEGER PRIMARY KEY AUTOINCREMENT,
    vehicle_type TEXT NOT NULL UNIQUE,         -- CAR or BIKE
    hourly_rate  REAL NOT NULL
);

-- ----- Rates (Taka per hour) -----
INSERT INTO rate (vehicle_type, hourly_rate) VALUES
('CAR', 60.00),
('BIKE', 30.00);

-- ----- Floors -----
INSERT INTO floor (name, level) VALUES
('Ground Floor', 0),
('First Floor', 1),
('Second Floor', 2);

-- ----- Slots: 20 per floor (A on floor 1, B on floor 2, C on floor 3) -----
-- Floor 1 (Ground): A-01..A-14 CAR, A-15..A-20 BIKE
INSERT INTO slot (floor_id, code, vehicle_type, status) VALUES
(1,'A-01','CAR','FREE'),(1,'A-02','CAR','FREE'),(1,'A-03','CAR','OCCUPIED'),
(1,'A-04','CAR','FREE'),(1,'A-05','CAR','FREE'),(1,'A-06','CAR','FREE'),
(1,'A-07','CAR','FREE'),(1,'A-08','CAR','OCCUPIED'),(1,'A-09','CAR','FREE'),
(1,'A-10','CAR','FREE'),(1,'A-11','CAR','FREE'),(1,'A-12','CAR','FREE'),
(1,'A-13','CAR','FREE'),(1,'A-14','CAR','FREE'),
(1,'A-15','BIKE','FREE'),(1,'A-16','BIKE','FREE'),(1,'A-17','BIKE','FREE'),
(1,'A-18','BIKE','FREE'),(1,'A-19','BIKE','FREE'),(1,'A-20','BIKE','DISABLED');

-- Floor 2 (First): B-01..B-14 CAR, B-15..B-20 BIKE
INSERT INTO slot (floor_id, code, vehicle_type, status) VALUES
(2,'B-01','CAR','FREE'),(2,'B-02','CAR','FREE'),(2,'B-03','CAR','FREE'),
(2,'B-04','CAR','FREE'),(2,'B-05','CAR','OCCUPIED'),(2,'B-06','CAR','FREE'),
(2,'B-07','CAR','FREE'),(2,'B-08','CAR','FREE'),(2,'B-09','CAR','FREE'),
(2,'B-10','CAR','FREE'),(2,'B-11','CAR','FREE'),(2,'B-12','CAR','FREE'),
(2,'B-13','CAR','FREE'),(2,'B-14','CAR','DISABLED'),
(2,'B-15','BIKE','FREE'),(2,'B-16','BIKE','FREE'),(2,'B-17','BIKE','FREE'),
(2,'B-18','BIKE','FREE'),(2,'B-19','BIKE','FREE'),(2,'B-20','BIKE','FREE');

-- Floor 3 (Second): C-01..C-14 CAR, C-15..C-20 BIKE
INSERT INTO slot (floor_id, code, vehicle_type, status) VALUES
(3,'C-01','CAR','FREE'),(3,'C-02','CAR','FREE'),(3,'C-03','CAR','FREE'),
(3,'C-04','CAR','FREE'),(3,'C-05','CAR','FREE'),(3,'C-06','CAR','FREE'),
(3,'C-07','CAR','FREE'),(3,'C-08','CAR','FREE'),(3,'C-09','CAR','FREE'),
(3,'C-10','CAR','OCCUPIED'),(3,'C-11','CAR','FREE'),(3,'C-12','CAR','FREE'),
(3,'C-13','CAR','FREE'),(3,'C-14','CAR','FREE'),
(3,'C-15','BIKE','FREE'),(3,'C-16','BIKE','FREE'),(3,'C-17','BIKE','FREE'),
(3,'C-18','BIKE','FREE'),(3,'C-19','BIKE','DISABLED'),(3,'C-20','BIKE','DISABLED');

-- ----- Seed bookings for the pre-occupied slots -----
-- Times use SQLite's localtime so they line up with Java's LocalDateTime.
-- A-03: an ordinary active booking (expires two hours from now).
INSERT INTO booking (slot_id, vehicle_no, entry_time, expected_exit, status)
VALUES ((SELECT id FROM slot WHERE code='A-03' AND floor_id=1),
        'DHA-GA-11-2233',
        datetime('now','localtime','-1 hours'),
        datetime('now','localtime','+2 hours'), 'ACTIVE');

-- A-08: already past its expected exit, so the ExpiryScheduler auto-frees it
-- within 30 seconds of launch (a live multithreading demonstration).
INSERT INTO booking (slot_id, vehicle_no, entry_time, expected_exit, status)
VALUES ((SELECT id FROM slot WHERE code='A-08' AND floor_id=1),
        'DHA-KHA-45-6789',
        datetime('now','localtime','-3 hours'),
        datetime('now','localtime','-10 minutes'), 'ACTIVE');

-- B-05: ordinary active booking.
INSERT INTO booking (slot_id, vehicle_no, entry_time, expected_exit, status)
VALUES ((SELECT id FROM slot WHERE code='B-05' AND floor_id=2),
        'CTG-HA-22-1100',
        datetime('now','localtime','-30 minutes'),
        datetime('now','localtime','+1 hours'), 'ACTIVE');

-- C-10: ordinary active booking.
INSERT INTO booking (slot_id, vehicle_no, entry_time, expected_exit, status)
VALUES ((SELECT id FROM slot WHERE code='C-10' AND floor_id=3),
        'DHA-GA-33-4455',
        datetime('now','localtime','-2 hours'),
        datetime('now','localtime','+3 hours'), 'ACTIVE');
