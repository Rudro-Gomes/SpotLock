package dao;

import exception.DataAccessException;
import model.Slot;
import model.SlotStatus;
import model.VehicleType;
import util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

// Reads parking slots and performs the atomic occupy / free updates that keep
// the seat status correct even under concurrent bookings.
public class SlotDAO {

    public List<Slot> getSlotsForFloor(int floorId) {
        List<Slot> slots = new ArrayList<>();
        String sql = "SELECT id, floor_id, code, vehicle_type, status "
                   + "FROM slot WHERE floor_id = ? ORDER BY code";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, floorId);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    slots.add(mapSlot(rs));
                }
            }

        } catch (SQLException e) {
            throw new DataAccessException("Could not load slots for floor " + floorId, e);
        }

        return slots;
    }

    public Slot getSlotById(int slotId) {
        String sql = "SELECT id, floor_id, code, vehicle_type, status "
                   + "FROM slot WHERE id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, slotId);

            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next() ? mapSlot(rs) : null;
            }

        } catch (SQLException e) {
            throw new DataAccessException("Could not load slot " + slotId, e);
        }
    }

    // The authoritative concurrency guard. The condition and the write happen in
    // ONE indivisible statement, so if several bookings race for the same slot
    // the database itself picks a single winner. Returns true only if this call
    // flipped the slot from FREE to OCCUPIED (affected-row count == 1).
    // Runs on the caller's transaction connection.
    public boolean tryOccupySlot(Connection conn, int slotId) throws SQLException {
        String sql = "UPDATE slot SET status = 'OCCUPIED' "
                   + "WHERE id = ? AND status = 'FREE'";

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, slotId);
            return pstmt.executeUpdate() == 1;
        }
    }

    // Releases a slot back to FREE. Runs on the caller's transaction connection.
    public void freeSlot(Connection conn, int slotId) throws SQLException {
        String sql = "UPDATE slot SET status = 'FREE' WHERE id = ?";

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, slotId);
            pstmt.executeUpdate();
        }
    }

    private Slot mapSlot(ResultSet rs) throws SQLException {
        return new Slot(
                rs.getInt("id"),
                rs.getInt("floor_id"),
                rs.getString("code"),
                VehicleType.fromDb(rs.getString("vehicle_type")),
                SlotStatus.fromDb(rs.getString("status")));
    }
}
