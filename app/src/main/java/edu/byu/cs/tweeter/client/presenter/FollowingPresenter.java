package edu.byu.cs.tweeter.client.presenter;

import android.util.Log;

import java.util.List;

import edu.byu.cs.tweeter.client.model.service.FollowingService;
import edu.byu.cs.tweeter.model.domain.AuthToken;
import edu.byu.cs.tweeter.model.domain.User;

/**
 * The presenter for the "following" functionality of the application.
 */
public class FollowingPresenter implements FollowingService.Observer {

    private static final String LOG_TAG = "FollowingPresenter";
    private static final int PAGE_SIZE = 10;

    private final View view;
    private final User user;
    private final AuthToken authToken;

    private User lastFollowee;
    private boolean hasMorePages = true;
    private boolean isLoading = false;

    /**
     * The interface by which this presenter communicates with it's view.
     */
    public interface View {
        void setLoading(boolean value);
        void addItems(List<User> newUsers);
        void displayErrorMessage(String message);
    }

    /**
     * Creates an instance.
     *
     * @param view the view for which this class is the presenter.
     * @param user the user that is currently logged in.
     * @param authToken the auth token for the current session.
     */
    public FollowingPresenter(View view, User user, AuthToken authToken) {
        this.view = view;
        this.user = user;
        this.authToken = authToken;
    }

    public synchronized User getLastFollowee() {
        return lastFollowee;
    }

    private synchronized void setLastFollowee(User lastFollowee) {
        this.lastFollowee = lastFollowee;
    }

    public synchronized boolean isHasMorePages() {
        return hasMorePages;
    }

    private synchronized void setHasMorePages(boolean hasMorePages) {
        this.hasMorePages = hasMorePages;
    }

    public synchronized boolean isLoading() {
        return isLoading;
    }

    private synchronized void setLoading(boolean loading) {
        isLoading = loading;
    }

    /**
     * Called by the view to request that another page of "following" users be loaded.
     */
    public synchronized void loadMoreItems() {
        if (!isLoading && hasMorePages) {
            isLoading = true;
            view.setLoading(true);

            getFollowing(authToken, user, PAGE_SIZE, (lastFollowee == null ? null : lastFollowee));
        }
    }

    /**
     * Requests the users that the user specified in the request is following. Uses information in
     * the request object to limit the number of followees returned and to return the next set of
     * followees after any that were returned for a previous request. This is an asynchronous
     * operation.
     *
     * @param authToken the session auth token.
     * @param targetUser the user for whom followees are being retrieved.
     * @param limit the maximum number of followees to return.
     * @param lastFollowee the last followee returned in the previous request (can be null).
     */
    public void getFollowing(AuthToken authToken, User targetUser, int limit, User lastFollowee) {
        getFollowingService(this).getFollowees(authToken, targetUser, limit, lastFollowee);
    }

    /**
     * Returns an instance of {@link FollowingService}. Allows mocking of the FollowingService class
     * for testing purposes. All usages of FollowingService should get their FollowingService
     * instance from this method to allow for mocking of the instance.
     *
     * @return the instance.
     */
    public FollowingService getFollowingService(FollowingService.Observer observer) {
        return new FollowingService(observer);
    }

    /**
     * Adds new followees retrieved asynchronously from the service to the view.
     *
     * @param followees list of retrieved followees.
     * @param hasMorePages whether or not there are remaining followees to retrieve.
     */
    @Override
    public synchronized void followeesRetrieved(List<User> followees, boolean hasMorePages) {
        this.lastFollowee = (followees.size() > 0) ? followees.get(followees.size() -1) : null;
        this.hasMorePages = hasMorePages;

        view.setLoading(false);
        view.addItems(followees);
        isLoading = false;
    }

    @Override
    public synchronized void followeesNotRetrieved(String message) {
        String errorMessage = "Failed to retrieve followees: " + message;
        Log.e(LOG_TAG, errorMessage);

        view.setLoading(false);
        view.displayErrorMessage(errorMessage);
        isLoading = false;
    }

    /**
     * Notifies the view that an exception occurred in an asynchronous method this class is
     * observing.
     *
     * @param exception the exception.
     */
    @Override
    public synchronized void handleException(Exception exception) {
        String errorMessage = "Failed to retrieve followees because of exception: " + exception.getMessage();
        Log.e(LOG_TAG, errorMessage, exception);

        view.setLoading(false);
        view.displayErrorMessage(errorMessage);
        isLoading = false;
    }
}
