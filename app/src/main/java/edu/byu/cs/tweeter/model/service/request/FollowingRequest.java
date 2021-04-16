package edu.byu.cs.tweeter.model.service.request;

/**
 * Contains all the information needed to make a request to have the server return the next page of
 * followees for a specified follower.
 */
public class FollowingRequest {

    private final String followerAlias;
    private final int limit;
    private final String lastFolloweeAlias;

    /**
     * Creates an instance.
     *
     * @param followerAlias the alias of the user whose followees are to be returned.
     * @param limit the maximum number of followees to return.
     * @param lastFolloweeAlias the alias of the last followee that was returned in the previous request (null if
     *                     there was no previous request or if no followees were returned in the
     *                     previous request).
     */
    public FollowingRequest(String followerAlias, int limit, String lastFolloweeAlias) {
        this.followerAlias = followerAlias;
        this.limit = limit;
        this.lastFolloweeAlias = lastFolloweeAlias;
    }

    /**
     * Returns the follower whose followees are to be returned by this request.
     *
     * @return the follower.
     */
    public String getFollowerAlias() {
        return followerAlias;
    }

    /**
     * Returns the number representing the maximum number of followees to be returned by this request.
     *
     * @return the limit.
     */
    public int getLimit() {
        return limit;
    }

    /**
     * Returns the last followee that was returned in the previous request or null if there was no
     * previous request or if no followees were returned in the previous request.
     *
     * @return the last followee.
     */
    public String getLastFolloweeAlias() {
        return lastFolloweeAlias;
    }
}
