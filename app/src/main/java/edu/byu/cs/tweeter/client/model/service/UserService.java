package edu.byu.cs.tweeter.client.model.service;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import edu.byu.cs.tweeter.client.model.net.ServerFacade;
import edu.byu.cs.tweeter.model.domain.AuthToken;
import edu.byu.cs.tweeter.model.domain.User;
import edu.byu.cs.tweeter.model.net.request.LoginRequest;
import edu.byu.cs.tweeter.model.net.response.LoginResponse;

/**
 * Contains the business logic to support the login operation.
 */
public class UserService {

    private static final String URL_PATH = "/login";

    private final Observer observer;

    private ServerFacade serverFacade;

    /**
     * An observer interface to be implemented by observers who want to be notified when
     * asynchronous operations complete.
     */
    public interface Observer {
        void handleSuccess(User user, AuthToken authToken);
        void handleFailure(String message);
        void handleException(Exception exception);
    }

    /**
     * Creates an instance.
     *
     * @param observer the observer who wants to be notified when any asynchronous operations
     *                 complete.
     */
     public UserService(Observer observer) {
        this.observer = observer;
     }

    /**
     * Makes an asynchronous login request.
     *
     * @param loginRequest the request that contains the login information.
     */
    public void login(LoginRequest loginRequest) {
        LoginTask loginTask = getLoginTask(loginRequest);
        BackgroundTaskUtils.runTask(loginTask);
    }

    /**
     * Returns an instance of {@link ServerFacade}. Allows mocking of the ServerFacade class for
     * testing purposes. All usages of ServerFacade should get their instance from this method to
     * allow for proper mocking.
     *
     * @return the instance.
     */
    ServerFacade getServerFacade() {
        if(serverFacade == null) {
            serverFacade = new ServerFacade();
        }

        return serverFacade;
    }


    /**
     * Returns an instance of {@link LoginTask}. Allows mocking of the LoginTask class for
     * testing purposes. All usages of LoginTask should get their instance from this method to
     * allow for proper mocking.
     *
     * @return the instance.
     */
    LoginTask getLoginTask(LoginRequest loginRequest) {
        return new LoginTask(loginRequest, new MessageHandler(Looper.getMainLooper(), observer));
    }

    /**
     * Handles messages from the background task indicating that the task is done, by invoking
     * methods on the observer.
     */
    private static class MessageHandler extends Handler {

        private final Observer observer;

        MessageHandler(Looper looper, Observer observer) {
            super(looper);
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
    private class LoginTask extends BackgroundTask {

        private static final String LOG_TAG = "LoginTask";

        public static final String USER_KEY = "user";
        public static final String AUTH_TOKEN_KEY = "auth-token";

        /**
         * The user's username (or "alias" or "handle"). E.g., "@susan".
         */
        private final String username;
        /**
         * The user's password.
         */
        private final String password;

        public LoginTask(LoginRequest loginRequest, Handler messageHandler) {
            super(messageHandler);

            this.username = loginRequest.getUsername();
            this.password = loginRequest.getPassword();
        }

        @Override
        protected void runTask() {
            try {
                LoginRequest request = new LoginRequest(username, password);
                LoginResponse response = getServerFacade().login(request, URL_PATH);

                if(response.isSuccess()) {
                    BackgroundTaskUtils.loadImage(response.getUser());
                    sendSuccessMessage(response.getUser(), response.getAuthToken());
                }
                else {
                    sendFailedMessage(response.getMessage());
                }
            } catch (Exception ex) {
                Log.e(LOG_TAG, ex.getMessage(), ex);
                sendExceptionMessage(ex);
            }
        }

        private void sendSuccessMessage(User loggedInUser, AuthToken authToken) {
            sendSuccessMessage(new BundleLoader() {
                @Override
                public void load(Bundle msgBundle) {
                    msgBundle.putSerializable(USER_KEY, loggedInUser);
                    msgBundle.putSerializable(AUTH_TOKEN_KEY, authToken);
                }
            });
        }
    }
}
