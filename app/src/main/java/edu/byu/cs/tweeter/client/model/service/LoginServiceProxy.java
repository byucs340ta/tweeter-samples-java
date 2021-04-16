package edu.byu.cs.tweeter.client.model.service;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import edu.byu.cs.tweeter.model.domain.User;
import edu.byu.cs.tweeter.client.model.net.ServerFacade;
import edu.byu.cs.tweeter.model.net.TweeterRemoteException;
import edu.byu.cs.tweeter.model.service.request.LoginRequest;
import edu.byu.cs.tweeter.model.service.response.LoginResponse;
import edu.byu.cs.tweeter.client.util.ByteArrayUtils;
import edu.byu.cs.tweeter.model.service.LoginService;

/**
 * Contains the business logic to support the login operation. Not a pure proxy because
 * it doesn't implement the {@link LoginService} interface. It can't because this class is
 * called asynchronously, but we need the implementing class on the server to be synchronous.
 */
public class LoginServiceProxy {

    public static final String EXCEPTION_KEY = "ExceptionKey";
    public static  final String LOGIN_RESPONSE_KEY = "LoginResponseKey";

    private static final String URL_PATH = "/login";

    private final Observer observer;

    /**
     * An observer interface to be implemented by observers who want to be notified when
     * asynchronous operations complete.
     */
    public interface Observer {
        void loginSuccessful(LoginResponse loginResponse);
        void loginUnsuccessful(LoginResponse loginResponse);
        void handleException(Exception exception);
    }

    /**
     * Creates an instance.
     *
     * @param observer the observer who wants to be notified when any asynchronous operations
     *                 complete.
     */
     public LoginServiceProxy(Observer observer) {
        this.observer = observer;
     }

    /**
     * Makes an asynchronous login request.
     *
     * @param loginRequest the login request.
     */
    public void login(LoginRequest loginRequest) {
        LoginTask loginTask = getLoginTask(loginRequest);
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.submit(loginTask);
    }

    /**
     * Returns an instance of {@link ServerFacade}. Allows mocking of the ServerFacade class for
     * testing purposes. All usages of ServerFacade should get their instance from this method to
     * allow for proper mocking.
     *
     * @return the instance.
     */
    ServerFacade getServerFacade() {
        return new ServerFacade();
    }


    /**
     * Returns an instance of {@link LoginTask}. Allows mocking of the LoginTask class for
     * testing purposes. All usages of LoginTask should get their instance from this method to
     * allow for proper mocking.
     *
     * @return the instance.
     */
    LoginTask getLoginTask(LoginRequest request) {
        return new LoginTask(request, new MessageHandler(Looper.getMainLooper(), observer));
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
            Exception exception = (Exception) bundle.getSerializable(EXCEPTION_KEY);

            if(exception == null) {
                LoginResponse loginResponse = (LoginResponse) bundle.getSerializable(LOGIN_RESPONSE_KEY);
                if(loginResponse.isSuccess()) {
                    observer.loginSuccessful(loginResponse);
                } else {
                    observer.loginUnsuccessful(loginResponse);
                }
            } else {
                observer.handleException(exception);
            }
        }
    }

    /**
     * The task that makes the login request on a background thread.
     */
    private class LoginTask extends BackgroundTask {

        private final LoginRequest request;

        public LoginTask(LoginRequest request, Handler messageHandler) {
            super(messageHandler);
            this.request = request;
        }

        /**
         * Invoked on a background thread to log the user in.
         */
        @Override
        public void run() {
            try {
                LoginResponse loginResponse = getServerFacade().login(request, URL_PATH);

                if(loginResponse.isSuccess()) {
                    loadImage(loginResponse.getUser());
                }

                sendMessage(LOGIN_RESPONSE_KEY, loginResponse);
            } catch (IOException | TweeterRemoteException ex) {
                sendMessage(EXCEPTION_KEY, ex);
            }
        }

        /**
         * Loads the profile image for the user.
         *
         * @param user the user whose profile image is to be loaded.
         */
        private void loadImage(User user) throws IOException {
            byte [] bytes = ByteArrayUtils.bytesFromUrl(user.getImageUrl());
            user.setImageBytes(bytes);
        }
    }
}
