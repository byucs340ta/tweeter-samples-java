package edu.byu.cs.tweeter.client.presenter;

import android.util.Log;

import java.util.List;

import edu.byu.cs.tweeter.model.domain.AuthToken;
import edu.byu.cs.tweeter.model.domain.User;
import edu.byu.cs.tweeter.client.model.service.FollowingServiceProxy;
import edu.byu.cs.tweeter.model.service.FollowingService;
import edu.byu.cs.tweeter.model.service.request.FollowingRequest;
import edu.byu.cs.tweeter.model.service.response.FollowingResponse;

/**
 * The presenter for the "following" functionality of the application.
 */
public class FollowingPresenter implements FollowingServiceProxy.Observer {

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

    User getLastFollowee() {
        return lastFollowee;
    }

    boolean isHasMorePages() {
        return hasMorePages;
    }

    boolean isLoading() {
        return isLoading;
    }

    /**
     * Called by the view to request that another page of "following" users be loaded.
     */
    public void loadMoreItems() {
        if (!isLoading && hasMorePages) {
            isLoading = true;
            view.setLoading(true);

            FollowingRequest request = new FollowingRequest(user.getAlias(), PAGE_SIZE, (lastFollowee == null ? null : lastFollowee.getAlias()));
            getFollowing(request);
        }
    }

    /**
     * Requests the users that the user specified in the request is following. Uses information in
     * the request object to limit the number of followees returned and to return the next set of
     * followees after any that were returned for a previous request. This is an asynchronous
     * operation.
     *
     * @param request contains the data required to fulfill the request.
     */
    public void getFollowing(FollowingRequest request) {
        getFollowingService(this).getFollowees(request);
    }

    /**
     * Returns an instance of {@link FollowingService}. Allows mocking of the FollowingService class
     * for testing purposes. All usages of FollowingService should get their FollowingService
     * instance from this method to allow for mocking of the instance.
     *
     * @return the instance.
     */
    public FollowingServiceProxy getFollowingService(FollowingServiceProxy.Observer observer) {
        return new FollowingServiceProxy(observer);
    }

    /**
     * Adds new followees retrieved asynchronously from the service to the view.
     *
     * @param followingResponse the response.
     */
    @Override
    public void followeesRetrieved(FollowingResponse followingResponse) {
        List<User> followees = followingResponse.getFollowees();

        lastFollowee = (followees.size() > 0) ? followees.get(followees.size() -1) : null;
        hasMorePages = followingResponse.getHasMorePages();

        view.setLoading(false);
        view.addItems(followees);
        isLoading = false;
    }

    /**
     * Notifies the view that an exception occurred in an asynchronous method this class is
     * observing.
     *
     * @param exception the exception.
     */
    @Override
    public void handleException(Exception exception) {
        Log.e(LOG_TAG, exception.getMessage(), exception);

        view.setLoading(false);
        view.displayErrorMessage(exception.getMessage());
        isLoading = false;
    }
}
