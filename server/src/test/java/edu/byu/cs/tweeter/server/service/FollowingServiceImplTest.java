package edu.byu.cs.tweeter.server.service;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Arrays;

import edu.byu.cs.tweeter.model.domain.User;
import edu.byu.cs.tweeter.model.service.request.FollowingRequest;
import edu.byu.cs.tweeter.model.service.response.FollowingResponse;
import edu.byu.cs.tweeter.server.dao.FollowingDAO;

public class FollowingServiceImplTest {

    private FollowingRequest request;
    private FollowingResponse expectedResponse;
    private FollowingDAO mockFollowingDAO;
    private FollowingServiceImpl followingServiceImplSpy;

    @BeforeEach
    public void setup() {
        User currentUser = new User("FirstName", "LastName", null);

        User resultUser1 = new User("FirstName1", "LastName1",
                "https://faculty.cs.byu.edu/~jwilkerson/cs340/tweeter/images/donald_duck.png");
        User resultUser2 = new User("FirstName2", "LastName2",
                "https://faculty.cs.byu.edu/~jwilkerson/cs340/tweeter/images/daisy_duck.png");
        User resultUser3 = new User("FirstName3", "LastName3",
                "https://faculty.cs.byu.edu/~jwilkerson/cs340/tweeter/images/daisy_duck.png");

        // Setup a request object to use in the tests
        request = new FollowingRequest(currentUser.getAlias(), 3, null);

        // Setup a mock FollowingDAO that will return known responses
        expectedResponse = new FollowingResponse(Arrays.asList(resultUser1, resultUser2, resultUser3), false);
        mockFollowingDAO = Mockito.mock(FollowingDAO.class);
        Mockito.when(mockFollowingDAO.getFollowees(request)).thenReturn(expectedResponse);

        followingServiceImplSpy = Mockito.spy(FollowingServiceImpl.class);
        Mockito.when(followingServiceImplSpy.getFollowingDAO()).thenReturn(mockFollowingDAO);
    }

    /**
     * Verify that the {@link FollowingServiceImpl#getFollowees(FollowingRequest)}
     * method returns the same result as the {@link FollowingDAO} class.
     */
    @Test
    public void testGetFollowees_validRequest_correctResponse() {
        FollowingResponse response = followingServiceImplSpy.getFollowees(request);
        Assertions.assertEquals(expectedResponse, response);
    }
}
