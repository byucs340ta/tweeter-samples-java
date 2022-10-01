package edu.byu.cs.tweeter.client.model.service.backgroundTask;

import android.os.Bundle;
import android.os.Handler;
import android.util.Log;

import java.io.Serializable;
import java.util.List;

import edu.byu.cs.tweeter.model.domain.AuthToken;
import edu.byu.cs.tweeter.model.domain.User;
import edu.byu.cs.tweeter.util.FakeData;
import edu.byu.cs.tweeter.util.Pair;

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

    protected void loadSuccessBundle(Bundle msgBundle) {
        msgBundle.putSerializable(FOLLOWEES_KEY, (Serializable) this.followees);
        msgBundle.putBoolean(MORE_PAGES_KEY, this.hasMorePages);
    }

    @Override
    protected void runTask() {
        try {
            Pair<List<User>, Boolean> pageOfUsers = getFollowees();
            this.followees = pageOfUsers.getFirst();
            this.hasMorePages = pageOfUsers.getSecond();

            sendSuccessMessage();
        } catch (Exception ex) {
            Log.e(LOG_TAG, "Failed to get followees", ex);
            sendExceptionMessage(ex);
        }
    }

    // This method is public so it can be accessed by test cases
    public FakeData getFakeData() {
        return FakeData.getInstance();
    }

    // This method is public so it can be accessed by test cases
    public Pair<List<User>, Boolean> getFollowees() {
        return getFakeData().getPageOfUsers(lastFollowee, limit, targetUser);
    }
}
