package Аuthorization;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;

public class AuthService implements Auth {
    private String URL = "jdbc:mysql://localhost:3306/storage?useSSL=false&serverTimezone=UTC";
    private Connection connection;
    private List<PreparedStatement> preparedStatements;
    private PreparedStatement authQuery;
    private PreparedStatement dbQuery;

    public AuthService(String url) {
        this.URL = url;
    }

    public synchronized void start() {
        try {
            Class.forName("com.mysql.jdbc.Driver");
            this.connection = DriverManager.getConnection("jdbc:mysql://localhost:3306/storage?useSSL=false&serverTimezone=UTC", "student", "student");
            prepareStatements();
        } catch (SQLException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    public AuthService() {
    }

    private void prepareStatements() throws SQLException {
        preparedStatements = new LinkedList<>();
        dbQuery = connection.prepareStatement("USE storage");
        authQuery = connection.prepareStatement("SELECT * FROM user WHERE user = ? AND password = ?");
        preparedStatements.add(dbQuery);
        preparedStatements.add(authQuery);
    }

    public void stop() {
        try {
            for (PreparedStatement ps : preparedStatements) ps.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        try {
            connection.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public synchronized boolean isLoginAccepted(String username, String password) {
        try {
            authQuery.setString(1, username);
            authQuery.setString(2, password);
            return authQuery.executeQuery().next();
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
}