package dao;

import model.Student;
import util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class StudentDAO {

    // Register a new student
    public boolean registerStudent(Student student) {

        String sql =
                "INSERT INTO students " +
                "(StudentName, EmailID, Password) " +
                "VALUES (?, ?, ?)";

        try {

            Connection connection =
                    DBConnection.getConnection();

            PreparedStatement statement =
                    connection.prepareStatement(sql);

            statement.setString(
                    1,
                    student.getStudentName()
            );

            statement.setString(
                    2,
                    student.getEmailID()
            );

            statement.setString(
                    3,
                    student.getPassword()
            );

            int result =
                    statement.executeUpdate();

            connection.close();

            return result > 0;

        } catch (Exception e) {

            e.printStackTrace();
            return false;
        }
    }


    // Find a student using email
    public Student getStudentByEmail(String email) {

        String sql =
                "SELECT * FROM students WHERE EmailID = ?";

        try {

            Connection connection =
                    DBConnection.getConnection();

            PreparedStatement statement =
                    connection.prepareStatement(sql);

            statement.setString(1, email);

            ResultSet result =
                    statement.executeQuery();

            if (result.next()) {

                Student student = new Student(
                        result.getString("StudentName"),
                        result.getString("EmailID"),
                        result.getString("Password")
                );

                student.setStudentID(
                        result.getInt("StudentID")
                );

                connection.close();

                return student;
            }

            connection.close();

        } catch (Exception e) {

            e.printStackTrace();
        }

        return null;
    }
}