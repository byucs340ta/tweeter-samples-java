package edu.byu.cs.tweeter.client.model.service;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import edu.byu.cs.tweeter.model.domain.AuthToken;
import edu.byu.cs.tweeter.model.domain.User;
import edu.byu.cs.tweeter.util.FakeData;
import edu.byu.cs.tweeter.util.Pair;

/**
 * Contains the business logic to support the login operation.
 */
public class UserService {

    /**
     * An observer interface to be implemented by observers who want to be notified when
     * asynchronous operations complete.
     */
    public interface LoginObserver {
        void handleSuccess(User user, AuthToken authToken);
        void handleFailure(String message);
        void handleException(Exception exception);
    }

    /**
     * Creates an instance.
     */
    public UserService() {
    }

    /**
     * Makes an asynchronous login request.
     *
     * @param username the user's name.
     * @param password the user's password.
     */
    public void login(String username, String password, LoginObserver observer) {
        LoginTask loginTask = getLoginTask(username, password, observer);
        BackgroundTaskUtils.runTask(loginTask);
    }

    /**
     * Returns an instance of {@link LoginTask}. Allows mocking of the LoginTask class for
     * testing purposes. All usages of LoginTask should get their instance from this method to
     * allow for proper mocking.
     *
     * @return the instance.
     */
    LoginTask getLoginTask(String username, String password, LoginObserver observer) {
        return new LoginTask(username, password, new MessageHandler(observer));
    }

    /**
     * Handles messages from the background task indicating that the task is done, by invoking
     * methods on the observer.
     */
    private static class MessageHandler extends Handler {

        private final LoginObserver observer;

        MessageHandler(LoginObserver observer) {
            super(Looper.getMainLooper());
            this.observer = observer;
        }

        @Override
        public void handleMessage(Message message) {
            Bundle bundle = message.getData();
            boolean success = bundle.getBoolean(LoginTask.SUCCESS_KEY);
            if (success) {
                User user = (User) bundle.getSerializable(LoginTask.USER_KEY);
                AuthToken authToken = (AuthToken) bundle.getSerializable(LoginTask.AUTH_TOKEN_KEY);
                observer.handleSuccess(user, authToken);
            } else if (bundle.containsKey(LoginTask.MESSAGE_KEY)) {
                String errorMessage = bundle.getString(LoginTask.MESSAGE_KEY);
                observer.handleFailure(errorMessage);
            } else if (bundle.containsKey(LoginTask.EXCEPTION_KEY)) {
                Exception ex = (Exception) bundle.getSerializable(LoginTask.EXCEPTION_KEY);
                observer.handleException(ex);
            }
        }
    }

    /**
     * Background task that logs in a user (i.e., starts a session).
     */
    private static class LoginTask extends BackgroundTask {

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

                BackgroundTaskUtils.loadImage(this.user);
                sendSuccessMessage();
            } catch (Exception ex) {
                Log.e(LOG_TAG, ex.getMessage(), ex);
                sendExceptionMessage(ex);
            }
        }

        // This method is public so it can be accessed by test cases
        public FakeData getFakeData() {
            return new FakeData();
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
}
