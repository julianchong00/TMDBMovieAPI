package Helper;

import java.sql.*;

public class DatabaseController {

    private static final String JDBC_DRIVER = "org.h2.Driver";
    private static final String DB_URL = "jdbc:h2:./ProfileDB";
    private static final String USER = "username";
    private static final String PASS = "password";

    private static final String GET_IMAGE_BASE_URL = "https://image.tmdb.org/t/p/";

    public static void createTable() {
        Connection connection = null;
        Statement statement = null;
        try {
            Class.forName(JDBC_DRIVER);

            connection = DriverManager.getConnection(DB_URL, USER, PASS);
            statement = connection.createStatement();
            String sql = "CREATE TABLE IF NOT EXISTS Profiles " +
                    "(id INTEGER not NULL, " +
                    " title VARCHAR(255), " +
                    " description VARCHAR(8000), " +
                    " filename VARCHAR(255), " +
                    " link VARCHAR(500), " +
                    " imageSize VARCHAR (255))";

            statement.executeUpdate(sql);
            statement.close();
            connection.close();

        } catch (SQLException se) {
            se.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void insertRecord(Integer id, String name, String description, String filename, String link, String imageSize) {
        Connection connection = null;
        PreparedStatement statement = null;
        try {
            Class.forName(JDBC_DRIVER);

            connection = DriverManager.getConnection(DB_URL, USER, PASS);
            statement = connection.prepareStatement("INSERT INTO Profiles (id, title, description, filename, link, imageSize) VALUES (?,?,?,?,?,?)");
            statement.setInt(1, id);
            statement.setString(2, name);
            statement.setString(3, description);
            statement.setString(4, filename);
            statement.setString(5, link);
            statement.setString(6, imageSize);
            statement.executeUpdate();
            statement.close();
            connection.close();

        } catch (SQLException se) {
            se.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void deleteRecord(Integer id, String imageSize) {
        Connection connection = null;
        PreparedStatement statement = null;
        try {
            Class.forName(JDBC_DRIVER);

            connection = DriverManager.getConnection(DB_URL, USER, PASS);
            statement = connection.prepareStatement("DELETE FROM Profiles WHERE (id = ? AND imageSize = ?)");
            statement.setInt(1, id);
            statement.setString(2, imageSize);

            statement.executeUpdate();
            statement.close();
            connection.close();

        } catch (SQLException se) {
            se.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static Connection getConnection() {
        Connection connection = null;
        try {
            connection = DriverManager.getConnection(DB_URL, USER, PASS);
        } catch (SQLException se) {
            se.printStackTrace();
        }
        return connection;
    }
}

