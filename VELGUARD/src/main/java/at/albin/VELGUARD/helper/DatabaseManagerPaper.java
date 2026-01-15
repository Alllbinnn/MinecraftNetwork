package at.albin.VELGUARD.helper;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseManagerPaper {

    static {
        try {
            Class.forName("org.mariadb.jdbc.Driver");
        } catch (ClassNotFoundException ignored) {}
    }

    private static final String HOST = "localhost";
    private static final String DB = "users_db";
    private static final String USER = "db_vellscaffolding";
    private static final String PASS = "<SECRET>";

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(
                "jdbc:mariadb://" + HOST + ":3306/" + DB,
                USER,
                PASS
        );
    }
}