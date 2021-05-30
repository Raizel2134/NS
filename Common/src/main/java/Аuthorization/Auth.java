package Аuthorization;

public interface Auth {
    void start() throws AuthServiceException;
    void stop();
    boolean isLoginAccepted(String username, String password);
}
