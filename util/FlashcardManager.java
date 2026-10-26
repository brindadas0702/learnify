package util;

import model.Flashcard;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class FlashcardManager {

    // CREATE: Add a flashcard
    public static boolean addFlashcard(Flashcard flashcard) {

        String sql = "INSERT INTO flashcards " +
                "(StudentID, Question, Answer) VALUES (?, ?, ?)";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, flashcard.getStudentID());
            statement.setString(2, flashcard.getQuestion());
            statement.setString(3, flashcard.getAnswer());

            return statement.executeUpdate() > 0;

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // READ: Get flashcards belonging to one student
    public static List<Flashcard> getFlashcardsByStudent(int studentID) {

        List<Flashcard> flashcards = new ArrayList<>();

        String sql = "SELECT * FROM flashcards " +
                "WHERE StudentID = ? ORDER BY FlashcardID DESC";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, studentID);

            try (ResultSet rs = statement.executeQuery()) {

                while (rs.next()) {
                    Flashcard flashcard = new Flashcard(
                            rs.getInt("FlashcardID"),
                            rs.getInt("StudentID"),
                            rs.getString("Question"),
                            rs.getString("Answer")
                    );

                    flashcards.add(flashcard);
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return flashcards;
    }

    // UPDATE: Edit a flashcard
    public static boolean updateFlashcard(
            int flashcardID, int studentID,
            String question, String answer) {

        String sql = "UPDATE flashcards SET Question = ?, Answer = ? " +
                "WHERE FlashcardID = ? AND StudentID = ?";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, question);
            statement.setString(2, answer);
            statement.setInt(3, flashcardID);
            statement.setInt(4, studentID);

            return statement.executeUpdate() > 0;

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // DELETE: Remove a flashcard
    public static boolean deleteFlashcard(int flashcardID, int studentID) {

        String sql = "DELETE FROM flashcards " +
                "WHERE FlashcardID = ? AND StudentID = ?";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, flashcardID);
            statement.setInt(2, studentID);

            return statement.executeUpdate() > 0;

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
}
