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
import edu.byu.cs.tweeter.model.service.request.FollowingRequest;
import edu.byu.cs.tweeter.model.service.response.FollowingResponse;
import edu.byu.cs.tweeter.client.util.ByteArrayUtils;
import edu.byu.cs.tweeter.model.service.FollowingService;

/**
 * Contains the business logic for getting the users a user is following. Not a pure proxy because
 * it doesn't implement the {@link FollowingService} interface. It can't because this class is
 * called asynchronously, but we need the implementing class on the server to be synchronous.
 */
public class FollowingServiceProxy {

    private static final String EXCEPTION_KEY = "ExceptionKey";
    private static  final String FOLLOWING_RESPONSE_KEY = "FollowingResponseKey";

    static final String URL_PATH = "/getfollowing";

    private final Observer observer;

    /**
     * An observer interface to be implemented by observers who want to be notified when
     * asynchronous operations complete.
     */
    public interface Observer {
        void followeesRetrieved(FollowingResponse followingResponse);
        void handleException(Exception exception);
    }

    /**
     * Creates an instance.
     *
     * @param observer the observer who wants to be notified when any asynchronous operations complete.
     */
    public FollowingServiceProxy(Observer observer) {
        // An assertion would be better, but Android doesn't support Java assertions
        if(observer == null) {
            throw new NullPointerException();
        }

        this.observer = observer;
    }

    /**
     * Requests the users that the user specified in the request is following. Uses information in
     * the request object to limit the number of followees returned and to return the next set of
     * followees after any that were returned in a previous request. Uses the {@link ServerFacade}
     * to get the followees from the server. This is an asynchronous operation.
     *
     * @param request contains the data required to fulfill the request.
     */
    public void getFollowees(FollowingRequest request) {
        RetrieveFollowingTask followingTask = getRetrieveFollowingTask(request);
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.submit(followingTask);
    }

    /**
     * Returns an instance of {@link ServerFacade}. Allows mocking of the ServerFacade class for
     * testing purposes. All usages of ServerFacade should get their instance from this method to
     * allow for proper mocking.
     *
     * @return the instance.
     */
    public ServerFacade getServerFacade() {
        return new ServerFacade();
    }

    /**
     * Returns an instance of {@link RetrieveFollowingTask}. Allows mocking of the
     * RetrieveFollowingTask class for testing purposes. All usages of RetrieveFollowingTask
     * should get their instance from this method to allow for proper mocking.
     *
     * @param request the request to be processed by the task.
     * @return the instance.
     */
    RetrieveFollowingTask getRetrieveFollowingTask(FollowingRequest request) {
        return new RetrieveFollowingTask(request, new MessageHandler(Looper.getMainLooper(), observer));
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
                FollowingResponse followingResponse = (FollowingResponse) bundle.getSerializable(FOLLOWING_RESPONSE_KEY);
                observer.followeesRetrieved(followingResponse);
            } else {
                observer.handleException(exception);
            }
        }
    }

    /**
     * The task that makes the request to retrieve followees on a background thread.
     */
    class RetrieveFollowingTask extends BackgroundTask {

        private final FollowingRequest request;

        RetrieveFollowingTask(FollowingRequest request, Handler messageHandler) {
            super(messageHandler);
            this.request = request;
        }

        /**
         * Invoked on the background thread to retrieve followees.
         */
        @Override
        public void run() {
            try {
                FollowingResponse response = getServerFacade().getFollowees(request, URL_PATH);

                if(response.isSuccess()) {
                    loadImages(response);
                }

                sendMessage(FOLLOWING_RESPONSE_KEY, response);
            } catch (IOException | TweeterRemoteException ex) {
                sendMessage(EXCEPTION_KEY, ex);
            }
        }

        /**
         * Loads the profile image for each followee included in the response.
         *
         * @param response the response from the followee request.
         */
        void loadImages(FollowingResponse response) throws IOException {
            for(User user : response.getFollowees()) {
                byte [] bytes = ByteArrayUtils.bytesFromUrl(user.getImageUrl());
                user.setImageBytes(bytes);
            }
        }
    }
}
