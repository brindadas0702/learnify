package util;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class GoalManager {

    public static boolean addGoal(Goal goal) {
        String sql = "INSERT INTO goals (StudentID, GoalTitle, GoalType, TargetDate, TargetHours, CompletedHours, Status) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, goal.getStudentID());
            statement.setString(2, goal.getGoalTitle());
            statement.setString(3, goal.getGoalType());
            statement.setString(4, goal.getTargetDate());
            statement.setDouble(5, goal.getTargetHours());
            statement.setDouble(6, goal.getCompletedHours());
            statement.setString(7, goal.getStatus());

            return statement.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static List<Goal> getAllGoals() {
        List<Goal> goals = new ArrayList<>();
        String sql = "SELECT * FROM goals ORDER BY TargetDate ASC";

        try (Connection connection = DBConnection.getConnection();
             Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery(sql)) {

            while (rs.next()) {
                Goal goal = new Goal(
                        rs.getInt("GoalID"),
                        rs.getInt("StudentID"),
                        rs.getString("GoalTitle"),
                        rs.getString("GoalType"),
                        rs.getString("TargetDate"),
                        rs.getDouble("TargetHours"),
                        rs.getDouble("CompletedHours"),
                        rs.getString("Status")
                );
                goals.add(goal);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return goals;
    }

    public static List<Goal> getGoalsByStudent(int studentID) {
        List<Goal> goals = new ArrayList<>();
        String sql = "SELECT * FROM goals WHERE StudentID = ? ORDER BY TargetDate ASC";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, studentID);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    Goal goal = new Goal(
                            rs.getInt("GoalID"),
                            rs.getInt("StudentID"),
                            rs.getString("GoalTitle"),
                            rs.getString("GoalType"),
                            rs.getString("TargetDate"),
                            rs.getDouble("TargetHours"),
                            rs.getDouble("CompletedHours"),
                            rs.getString("Status")
                    );
                    goals.add(goal);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return goals;
    }

    public static boolean updateGoalProgress(int goalID, double completedHours, String status) {
        String sql = "UPDATE goals SET CompletedHours = ?, Status = ? WHERE GoalID = ?";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setDouble(1, completedHours);
            statement.setString(2, status);
            statement.setInt(3, goalID);

            return statement.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean deleteGoal(int goalID) {
        String sql = "DELETE FROM goals WHERE GoalID = ?";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, goalID);
            return statement.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
}
