package edu.byu.cs.tweeter.client.model.service;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import java.io.IOException;
import java.io.Serializable;
import java.util.List;

import edu.byu.cs.tweeter.client.model.net.ServerFacade;
import edu.byu.cs.tweeter.model.domain.User;
import edu.byu.cs.tweeter.model.net.TweeterRemoteException;
import edu.byu.cs.tweeter.model.net.request.FollowingRequest;
import edu.byu.cs.tweeter.model.net.response.FollowingResponse;

/**
 * Contains the business logic for getting the users a user is following.
 */
public class FollowService {

    static final String URL_PATH = "/getfollowing";

    private final Observer observer;

    private ServerFacade serverFacade;

    /**
     * An observer interface to be implemented by observers who want to be notified when
     * asynchronous operations complete.
     */
    public interface Observer {
        void handleSuccess(List<User> followees, boolean hasMorePages);
        void handleFailure(String message);
        void handleException(Exception exception);
    }

    /**
     * Creates an instance.
     *
     * @param observer the observer who wants to be notified when any asynchronous operations complete.
     */
    public FollowService(Observer observer) {
        // An assertion would be better, but Android doesn't support Java assertions
        if(observer == null) {
            throw new NullPointerException();
        }

        this.observer = observer;
    }

    /**
     * Requests the users that the user specified in the request is following.
     * Limits the number of followees returned and returns the next set of
     * followees after any that were returned in a previous request.
     * This is an asynchronous operation.
     *
     * @param followingRequest a request for a page of followees.
     */
    public void getFollowees(FollowingRequest followingRequest) {
        GetFollowingTask followingTask = getGetFollowingTask(followingRequest);
        BackgroundTaskUtils.runTask(followingTask);
    }

    /**
     * Returns an instance of {@link ServerFacade}. Allows mocking of the ServerFacade class for
     * testing purposes. All usages of ServerFacade should get their instance from this method to
     * allow for proper mocking.
     *
     * @return the instance.
     */
    public ServerFacade getServerFacade() {
        if(serverFacade == null) {
            serverFacade = new ServerFacade();
        }

        return serverFacade;
    }

    /**
     * Returns an instance of {@link GetFollowingTask}. Allows mocking of the
     * GetFollowingTask class for testing purposes. All usages of GetFollowingTask
     * should get their instance from this method to allow for proper mocking.
     *
     * @return the instance.
     */
    // This method is public so it can be accessed by test cases
    public GetFollowingTask getGetFollowingTask(FollowingRequest request) {
        return new GetFollowingTask(request,
                new MessageHandler(Looper.getMainLooper(), observer));
    }

    /**
     * Handles messages from the background task indicating that the task is done, by invoking
     * methods on the observer.
     */
    public static class MessageHandler extends Handler {

        private final Observer observer;

        public MessageHandler(Looper looper, Observer observer) {
            super(looper);
            this.observer = observer;
        }

        @Override
        public void handleMessage(Message message) {
            Bundle bundle = message.getData();
            boolean success = bundle.getBoolean(GetFollowingTask.SUCCESS_KEY);
            if (success) {
                List<User> followees = (List<User>) bundle.getSerializable(GetFollowingTask.FOLLOWEES_KEY);
                boolean hasMorePages = bundle.getBoolean(GetFollowingTask.MORE_PAGES_KEY);
                observer.handleSuccess(followees, hasMorePages);
            } else if (bundle.containsKey(GetFollowingTask.MESSAGE_KEY)) {
                String errorMessage = bundle.getString(GetFollowingTask.MESSAGE_KEY);
                observer.handleFailure(errorMessage);
            } else if (bundle.containsKey(GetFollowingTask.EXCEPTION_KEY)) {
                Exception ex = (Exception) bundle.getSerializable(GetFollowingTask.EXCEPTION_KEY);
                observer.handleException(ex);
            }
        }
    }

    /**
     * Background task that retrieves a page of other users being followed by a specified user.
     */
    public class GetFollowingTask extends BackgroundTask {

        private static final String LOG_TAG = "GetFollowingTask";

        public static final String FOLLOWEES_KEY = "followees";
        public static final String MORE_PAGES_KEY = "more-pages";

        private final FollowingRequest request;

        public GetFollowingTask(FollowingRequest request, Handler messageHandler) {
            super(messageHandler);
            this.request = request;
        }

        protected void sendSuccessMessage(List<User> followees, boolean hasMorePages) {
            sendSuccessMessage(new BundleLoader() {
                @Override
                public void load(Bundle msgBundle) {
                    msgBundle.putSerializable(FOLLOWEES_KEY, (Serializable) followees);
                    msgBundle.putBoolean(MORE_PAGES_KEY, hasMorePages);
                }
            });
        }

        @Override
        protected void runTask() {
            try {
                FollowingResponse response = getServerFacade().getFollowees(request, URL_PATH);

                if(response.isSuccess()) {
                    loadImages(response.getFollowees());
                    sendSuccessMessage(response.getFollowees(), response.getHasMorePages());
                }
                else {
                    sendFailedMessage(response.getMessage());
                }
            } catch (IOException | TweeterRemoteException ex) {
                Log.e(LOG_TAG, "Failed to get followees", ex);
                sendExceptionMessage(ex);
            }
        }

        // This method is public so it can be accessed by test cases
        public void loadImages(List<User> followees) throws IOException {
            for (User u : followees) {
                BackgroundTaskUtils.loadImage(u);
            }
        }
    }

}
