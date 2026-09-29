package util;

import java.sql.Connection;
import java.sql.DriverManager;

public class DBConnection {

    private static final String URL =
            "jdbc:mysql://localhost:3306/learnify";

    private static final String USER = "root";

    private static final String PASSWORD = "tiger";

    public static Connection getConnection() {

        Connection connection = null;

        try {
            connection = DriverManager.getConnection(
                    URL,
                    USER,
                    PASSWORD
            );

            System.out.println("Database connected successfully.");

        } catch (Exception e) {
            System.out.println("Database connection failed.");
            e.printStackTrace();
        }

        return connection;
    }
}