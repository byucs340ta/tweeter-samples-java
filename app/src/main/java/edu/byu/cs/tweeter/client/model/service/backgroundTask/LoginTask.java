package edu.byu.cs.tweeter.client.model.service.backgroundTask;

import android.os.Bundle;
import android.os.Handler;
import android.util.Log;

import edu.byu.cs.tweeter.model.domain.AuthToken;
import edu.byu.cs.tweeter.model.domain.User;
import edu.byu.cs.tweeter.util.FakeData;
import edu.byu.cs.tweeter.util.Pair;

/**
 * Background task that logs in a user (i.e., starts a session).
 */
public class LoginTask extends BackgroundTask {

    private static final String LOG_TAG = "LoginTask";

    public static final String USER_KEY = "user";
    public static final String AUTH_TOKEN_KEY = "auth-token";

    /**
     * The user's username (or "alias" or "handle"). E.g., "@susan".
     */
    private String username;
    /**
     * The user's password.
     */
    private String password;

    /**
     * The logged-in user returned by the server.
     */
    protected User user;

    /**
     * The auth token returned by the server.
     */
    protected AuthToken authToken;

    public LoginTask(String username, String password, Handler messageHandler) {
        super(messageHandler);

        this.username = username;
        this.password = password;
    }

    @Override
    protected void runTask() {
        try {
            Pair<User, AuthToken> loginResult = doLogin();

            this.user = loginResult.getFirst();
            this.authToken = loginResult.getSecond();

            sendSuccessMessage();
        } catch (Exception ex) {
            Log.e(LOG_TAG, ex.getMessage(), ex);
            sendExceptionMessage(ex);
        }
    }

    // This method is public so it can be accessed by test cases
    public FakeData getFakeData() {
        return FakeData.getInstance();
    }

    // This method is public so it can be accessed by test cases
    public Pair<User, AuthToken> doLogin() {
        User loggedInUser = getFakeData().getFirstUser();
        AuthToken authToken = getFakeData().getAuthToken();
        return new Pair<>(loggedInUser, authToken);
    }

    protected void loadSuccessBundle(Bundle msgBundle) {
        msgBundle.putSerializable(USER_KEY, this.user);
        msgBundle.putSerializable(AUTH_TOKEN_KEY, this.authToken);
    }
}
