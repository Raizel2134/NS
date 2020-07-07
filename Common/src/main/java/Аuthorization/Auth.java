package Аuthorization;

import java.util.List;

public interface Auth {
    void start() throws AuthServiceException;
    void stop();
    boolean isLoginAccepted(String username, String password);
}
