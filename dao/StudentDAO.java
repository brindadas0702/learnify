package dao;

import model.Student;
import util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;

public class StudentDAO {

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
}