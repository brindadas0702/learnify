import util.DBConnection;
import java.sql.Connection;
public class TestConnection {
    public static void main(String[] args) {
        Connection connection = DBConnection.getConnection();
        if (connection != null) {
            System.out.println("TEST SUCCESSFUL");
        } else {
            System.out.println("TEST FAILED");
        }
    }
}