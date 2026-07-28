package dao;

import exception.DataAccessException;
import model.VehicleType;
import util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

// Reads the hourly parking rate for each vehicle type. Kept in its own DAO so
// that all SQL stays out of the service layer.
public class RateDAO {

    public double getHourlyRate(VehicleType vehicleType) {
        String sql = "SELECT hourly_rate FROM rate WHERE vehicle_type = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, vehicleType.name());

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getDouble("hourly_rate");
                }
                throw new DataAccessException(
                        "No hourly rate configured for " + vehicleType, null);
            }

        } catch (SQLException e) {
            throw new DataAccessException("Could not load rate for " + vehicleType, e);
        }
    }
}
