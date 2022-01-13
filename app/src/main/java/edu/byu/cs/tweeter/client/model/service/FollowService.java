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
import edu.byu.cs.tweeter.model.domain.AuthToken;
import edu.byu.cs.tweeter.model.domain.User;
import edu.byu.cs.tweeter.model.net.TweeterRemoteException;
import edu.byu.cs.tweeter.model.net.request.FollowingRequest;
import edu.byu.cs.tweeter.model.net.response.FollowingResponse;

/**
 * Contains the business logic for getting the users a user is following.
 */
public class FollowService {

    static final String URL_PATH = "/getfollowing";

    private ServerFacade serverFacade;

    /**
     * An observer interface to be implemented by observers who want to be notified when
     * asynchronous operations complete.
     */
    public interface GetFollowingObserver {
        void handleSuccess(List<User> followees, boolean hasMorePages);
        void handleFailure(String message);
        void handleException(Exception exception);
    }

    /**
     * Creates an instance.
     */
    public FollowService() {}

    /**
     * Requests the users that the user specified in the request is following.
     * Limits the number of followees returned and returns the next set of
     * followees after any that were returned in a previous request.
     * This is an asynchronous operation.
     *
     * @param authToken the session auth token.
     * @param targetUser the user for whom followees are being retrieved.
     * @param limit the maximum number of followees to return.
     * @param lastFollowee the last followee returned in the previous request (can be null).
     */
    public void getFollowees(AuthToken authToken, User targetUser, int limit, User lastFollowee, GetFollowingObserver observer) {
        GetFollowingTask followingTask = getGetFollowingTask(authToken, targetUser, limit, lastFollowee, observer);
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

        return new ServerFacade();
    }

    /**
     * Returns an instance of {@link GetFollowingTask}. Allows mocking of the
     * GetFollowingTask class for testing purposes. All usages of GetFollowingTask
     * should get their instance from this method to allow for proper mocking.
     *
     * @return the instance.
     */
    // This method is public so it can be accessed by test cases
    public GetFollowingTask getGetFollowingTask(AuthToken authToken, User targetUser, int limit, User lastFollowee, GetFollowingObserver observer) {
        return new GetFollowingTask(authToken, targetUser, limit, lastFollowee, new MessageHandler(observer));
    }

    /**
     * Handles messages from the background task indicating that the task is done, by invoking
     * methods on the observer.
     */
    public static class MessageHandler extends Handler {

        private final GetFollowingObserver observer;

        public MessageHandler(GetFollowingObserver observer) {
            super(Looper.getMainLooper());
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

        /**
         * Auth token for logged-in user.
         */
        protected AuthToken authToken;
        /**
         * The user whose following is being retrieved.
         * (This can be any user, not just the currently logged-in user.)
         */
        protected User targetUser;
        /**
         * Maximum number of followed users to return (i.e., page size).
         */
        protected int limit;
        /**
         * The last person being followed returned in the previous page of results (can be null).
         * This allows the new page to begin where the previous page ended.
         */
        protected User lastFollowee;
        /**
         * The followee users returned by the server.
         */
        private List<User> followees;
        /**
         * If there are more pages, returned by the server.
         */
        private boolean hasMorePages;
        public GetFollowingTask(AuthToken authToken, User targetUser, int limit, User lastFollowee,
                                Handler messageHandler) {
            super(messageHandler);
            this.authToken = authToken;
            this.targetUser = targetUser;
            this.limit = limit;
            this.lastFollowee = lastFollowee;
        }

        @Override
        protected void runTask() {
            try {
                String targetUserAlias = targetUser == null ? null : targetUser.getAlias();
                String lastFolloweeAlias = lastFollowee == null ? null : lastFollowee.getAlias();

                FollowingRequest request = new FollowingRequest(authToken, targetUserAlias, limit, lastFolloweeAlias);
                FollowingResponse response = getServerFacade().getFollowees(request, URL_PATH);

                if(response.isSuccess()) {
                    this.followees = response.getFollowees();
                    this.hasMorePages = response.getHasMorePages();
                    sendSuccessMessage();
                }
                else {
                    sendFailedMessage(response.getMessage());
                }
            } catch (IOException | TweeterRemoteException ex) {
                Log.e(LOG_TAG, "Failed to get followees", ex);
                sendExceptionMessage(ex);
            }
        }

        protected void loadSuccessBundle(Bundle msgBundle) {
            msgBundle.putSerializable(FOLLOWEES_KEY, (Serializable) this.followees);
            msgBundle.putBoolean(MORE_PAGES_KEY, this.hasMorePages);
        }
    }

}
