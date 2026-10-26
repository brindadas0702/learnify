
package util;

import model.StudyFile;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class StudyFileManager {

    // CREATE: Save uploaded file details
    public static boolean addFile(StudyFile file) {

        String sql = "INSERT INTO study_files " +
                "(StudentID, Title, OriginalName, StoredName, FileType) " +
                "VALUES (?, ?, ?, ?, ?)";

        try (Connection connection = DBConnection.getConnection()) {

            if (connection == null) {
                return false;
            }

            try (PreparedStatement statement =
                         connection.prepareStatement(sql)) {

                statement.setInt(1, file.getStudentID());
                statement.setString(2, file.getTitle());
                statement.setString(3, file.getOriginalName());
                statement.setString(4, file.getStoredName());
                statement.setString(5, file.getFileType());

                return statement.executeUpdate() > 0;
            }

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // READ: Get files belonging to one student
    public static List<StudyFile> getFilesByStudent(int studentID) {

        List<StudyFile> files = new ArrayList<>();

        String sql = "SELECT * FROM study_files " +
                "WHERE StudentID = ? ORDER BY UploadedAt DESC";

        try (Connection connection = DBConnection.getConnection()) {

            if (connection == null) {
                return files;
            }

            try (PreparedStatement statement =
                         connection.prepareStatement(sql)) {

                statement.setInt(1, studentID);

                try (ResultSet rs = statement.executeQuery()) {

                    while (rs.next()) {

                        StudyFile file = new StudyFile(
                                rs.getInt("FileID"),
                                rs.getInt("StudentID"),
                                rs.getString("Title"),
                                rs.getString("OriginalName"),
                                rs.getString("StoredName"),
                                rs.getString("FileType")
                        );

                        files.add(file);
                    }
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return files;
    }

    // DELETE: Delete a file record belonging to a student
    public static boolean deleteFile(int fileID, int studentID) {

        String sql = "DELETE FROM study_files " +
                "WHERE FileID = ? AND StudentID = ?";

        try (Connection connection = DBConnection.getConnection()) {

            if (connection == null) {
                return false;
            }

            try (PreparedStatement statement =
                         connection.prepareStatement(sql)) {

                statement.setInt(1, fileID);
                statement.setInt(2, studentID);

                return statement.executeUpdate() > 0;
            }

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
}
