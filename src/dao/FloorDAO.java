package dao;

import exception.DataAccessException;
import model.Floor;
import util.DBConnection;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

// Reads parking floors from the database.
public class FloorDAO {

    public List<Floor> getAllFloors() {
        List<Floor> floors = new ArrayList<>();
        String sql = "SELECT id, name, level FROM floor ORDER BY level";

        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                floors.add(new Floor(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getInt("level")));
            }

        } catch (SQLException e) {
            throw new DataAccessException("Could not load floors", e);
        }

        return floors;
    }
}
