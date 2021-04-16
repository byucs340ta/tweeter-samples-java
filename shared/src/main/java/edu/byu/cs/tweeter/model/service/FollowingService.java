package edu.byu.cs.tweeter.model.service;

import edu.byu.cs.tweeter.model.service.request.FollowingRequest;
import edu.byu.cs.tweeter.model.service.response.FollowingResponse;

/**
 * Defines the interface for the 'following' service.
 */
public interface FollowingService {

    /**
     * Returns the users that the user specified in the request is following. Uses information in
     * the request object to limit the number of followees returned and to return the next set of
     * followees after any that were returned in a previous request.
     *
     * @param request contains the data required to fulfill the request.
     * @return the followees.
     */
    FollowingResponse getFollowees(FollowingRequest request);
}
