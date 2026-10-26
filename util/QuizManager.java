package util;

import model.Quiz;
import model.Question;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class QuizManager {

    // Get all quizzes: built-in quizzes and student-created quizzes
    public static List<Quiz> getAllQuizzes() {
        List<Quiz> quizzes = new ArrayList<>();

        String sql = "SELECT QuizID, StudentID, Title, Description "
                   + "FROM quizzes ORDER BY QuizID DESC";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                int quizID = rs.getInt("QuizID");
                int creatorID = rs.getInt("StudentID");
                Integer studentID = rs.wasNull() ? null : creatorID;

                quizzes.add(new Quiz(
                    quizID,
                    studentID,
                    rs.getString("Title"),
                    rs.getString("Description")
                ));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return quizzes;
    }

    // Create a student-owned quiz
    public static int createQuiz(int studentID, String title, String description) {
        String sql = "INSERT INTO quizzes (StudentID, Title, Description) VALUES (?, ?, ?)";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setInt(1, studentID);
            ps.setString(2, title);
            ps.setString(3, description);

            if (ps.executeUpdate() > 0) {
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        return rs.getInt(1);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return -1;
    }

    // Add a multiple-choice question
    public static boolean addQuestion(
            int quizID, String question, String optionA, String optionB,
            String optionC, String optionD, String correctOption) {

        String sql = "INSERT INTO quiz_questions "
                   + "(QuizID, Question, OptionA, OptionB, OptionC, OptionD, CorrectOption) "
                   + "VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, quizID);
            ps.setString(2, question);
            ps.setString(3, optionA);
            ps.setString(4, optionB);
            ps.setString(5, optionC);
            ps.setString(6, optionD);
            ps.setString(7, correctOption);

            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    // Retrieve quiz questions, including correct answers for server-side grading only.
    // The API must never send CorrectOption to the browser.
    public static List<Question> getQuestions(int quizID) {
        List<Question> questions = new ArrayList<>();

        String sql = "SELECT QuestionID, QuizID, Question, OptionA, OptionB, "
                   + "OptionC, OptionD, CorrectOption "
                   + "FROM quiz_questions WHERE QuizID = ? ORDER BY QuestionID";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, quizID);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    questions.add(new Question(
                        rs.getInt("QuestionID"),
                        rs.getInt("QuizID"),
                        rs.getString("Question"),
                        rs.getString("OptionA"),
                        rs.getString("OptionB"),
                        rs.getString("OptionC"),
                        rs.getString("OptionD"),
                        rs.getString("CorrectOption")
                    ));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return questions;
    }

    // Only the creator can add questions to a student-created quiz.
    public static boolean isQuizOwner(int quizID, int studentID) {
        String sql = "SELECT QuizID FROM quizzes WHERE QuizID = ? AND StudentID = ?";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, quizID);
            ps.setInt(2, studentID);

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    // Grade answers on the server. The score is returned but never stored.
    // Only questions belonging to this quiz are counted.
    public static int calculateScore(int quizID, Map<Integer, String> answers) {
        int score = 0;

        String sql = "SELECT QuestionID, CorrectOption "
                   + "FROM quiz_questions WHERE QuizID = ?";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, quizID);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int questionID = rs.getInt("QuestionID");
                    String correctOption = rs.getString("CorrectOption");
                    String studentAnswer = answers.get(questionID);

                    if (studentAnswer != null
                            && studentAnswer.equalsIgnoreCase(correctOption)) {
                        score++;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }

        return score;
    }
}
