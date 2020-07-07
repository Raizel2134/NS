package Аuthorization;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.sql.*;
import java.util.LinkedList;
import java.util.List;

public class AuthService implements Auth {

    private String URL = "jdbc:mysql://localhost:3306/storage?serverTimezone=UTC&useSSL=false";
    private static final String LOGIN = "student";
    private static final String PASS = "student";
    private Connection connection;
    private List<PreparedStatement> preparedStatements;
    private PreparedStatement authQuery;
    private PreparedStatement dbQuery;

    public AuthService (String url){
        this.URL = url;
    }

    public synchronized void start() {
        try {
            this.connection = DriverManager.getConnection(URL,LOGIN,PASS);
            prepareStatements();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public AuthService(){}

    private synchronized void testPrefill() throws AuthServiceException {
        String [] [] testUsers = {
                {"login_1",  "pass_1"},
                {"login_2",  "pass_2"},
                {"login_3",  "pass_3"}};
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            for (int i = 0; i < 3; i++) {
                digest.update(testUsers[i][1].getBytes());
                if(Files.notExists(Paths.get(URL).getParent().resolve(testUsers[i][0])))
                    Files.createDirectory(Paths.get(URL).getParent().resolve(testUsers[i][0]));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void prepareStatements () throws SQLException {
        preparedStatements = new LinkedList<>();
        dbQuery = connection.prepareStatement("USE storage");
        authQuery = connection.prepareStatement("SELECT * FROM user WHERE user = ? AND password = ?");
        preparedStatements.add(dbQuery);
        preparedStatements.add(authQuery);
    }

    public void stop() {
        try {
            for (PreparedStatement ps: preparedStatements) ps.close();
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