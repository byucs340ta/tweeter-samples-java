package edu.byu.cs.tweeter.model.net;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import edu.byu.cs.tweeter.model.domain.User;
import edu.byu.cs.tweeter.model.service.request.FollowingRequest;
import edu.byu.cs.tweeter.model.service.response.FollowingResponse;

class ServerFacadeTest {

    private final User user1 = new User("Daffy", "Duck", "");
    private final User user2 = new User("Fred", "Flintstone", "");
    private final User user3 = new User("Barney", "Rubble", "");
    private final User user4 = new User("Wilma", "Rubble", "");
    private final User user5 = new User("Clint", "Eastwood", "");
    private final User user6 = new User("Mother", "Teresa", "");
    private final User user7 = new User("Harriett", "Hansen", "");
    private final User user8 = new User("Zoe", "Zabriski", "");

    private ServerFacade serverFacadeSpy;

    @BeforeEach
    void setup() {
        serverFacadeSpy = Mockito.spy(new ServerFacade());
    }

    @Test
    void testGetFollowees_noFolloweesForUser() {
        List<User> followees = Collections.emptyList();
        Mockito.when(serverFacadeSpy.getDummyFollowees()).thenReturn(followees);

        FollowingRequest request = new FollowingRequest(user1.getAlias(), 10, null);
        FollowingResponse response = serverFacadeSpy.getFollowees(request);

        Assertions.assertEquals(0, response.getFollowees().size());
        Assertions.assertFalse(response.getHasMorePages());
    }

    @Test
    void testGetFollowees_oneFollowerForUser_limitGreaterThanUsers() {
        List<User> followees = Collections.singletonList(user2);
        Mockito.when(serverFacadeSpy.getDummyFollowees()).thenReturn(followees);

        FollowingRequest request = new FollowingRequest(user1.getAlias(), 10, null);
        FollowingResponse response = serverFacadeSpy.getFollowees(request);

        Assertions.assertEquals(1, response.getFollowees().size());
        Assertions.assertTrue(response.getFollowees().contains(user2));
        Assertions.assertFalse(response.getHasMorePages());
    }

    @Test
    void testGetFollowees_twoFollowersForUser_limitEqualsUsers() {
        List<User> followees = Arrays.asList(user2, user3);
        Mockito.when(serverFacadeSpy.getDummyFollowees()).thenReturn(followees);

        FollowingRequest request = new FollowingRequest(user3.getAlias(), 2, null);
        FollowingResponse response = serverFacadeSpy.getFollowees(request);

        Assertions.assertEquals(2, response.getFollowees().size());
        Assertions.assertTrue(response.getFollowees().contains(user2));
        Assertions.assertTrue(response.getFollowees().contains(user3));
        Assertions.assertFalse(response.getHasMorePages());
    }

    @Test
    void testGetFollowees_limitLessThanUsers_endsOnPageBoundary() {
        List<User> followees = Arrays.asList(user2, user3, user4, user5, user6, user7);
        Mockito.when(serverFacadeSpy.getDummyFollowees()).thenReturn(followees);

        FollowingRequest request = new FollowingRequest(user5.getAlias(), 2, null);
        FollowingResponse response = serverFacadeSpy.getFollowees(request);

        // Verify first page
        Assertions.assertEquals(2, response.getFollowees().size());
        Assertions.assertTrue(response.getFollowees().contains(user2));
        Assertions.assertTrue(response.getFollowees().contains(user3));
        Assertions.assertTrue(response.getHasMorePages());

        // Get and verify second page
        request = new FollowingRequest(user5.getAlias(), 2, response.getFollowees().get(1).getAlias());
        response = serverFacadeSpy.getFollowees(request);

        Assertions.assertEquals(2, response.getFollowees().size());
        Assertions.assertTrue(response.getFollowees().contains(user4));
        Assertions.assertTrue(response.getFollowees().contains(user5));
        Assertions.assertTrue(response.getHasMorePages());

        // Get and verify third page
        request = new FollowingRequest(user5.getAlias(), 2, response.getFollowees().get(1).getAlias());
        response = serverFacadeSpy.getFollowees(request);

        Assertions.assertEquals(2, response.getFollowees().size());
        Assertions.assertTrue(response.getFollowees().contains(user6));
        Assertions.assertTrue(response.getFollowees().contains(user7));
        Assertions.assertFalse(response.getHasMorePages());
    }


    @Test
    void testGetFollowees_limitLessThanUsers_notEndsOnPageBoundary() {
        List<User> followees = Arrays.asList(user2, user3, user4, user5, user6, user7, user8);
        Mockito.when(serverFacadeSpy.getDummyFollowees()).thenReturn(followees);

        FollowingRequest request = new FollowingRequest(user6.getAlias(), 2, null);
        FollowingResponse response = serverFacadeSpy.getFollowees(request);

        // Verify first page
        Assertions.assertEquals(2, response.getFollowees().size());
        Assertions.assertTrue(response.getFollowees().contains(user2));
        Assertions.assertTrue(response.getFollowees().contains(user3));
        Assertions.assertTrue(response.getHasMorePages());

        // Get and verify second page
        request = new FollowingRequest(user6.getAlias(), 2, response.getFollowees().get(1).getAlias());
        response = serverFacadeSpy.getFollowees(request);

        Assertions.assertEquals(2, response.getFollowees().size());
        Assertions.assertTrue(response.getFollowees().contains(user4));
        Assertions.assertTrue(response.getFollowees().contains(user5));
        Assertions.assertTrue(response.getHasMorePages());

        // Get and verify third page
        request = new FollowingRequest(user6.getAlias(), 2, response.getFollowees().get(1).getAlias());
        response = serverFacadeSpy.getFollowees(request);

        Assertions.assertEquals(2, response.getFollowees().size());
        Assertions.assertTrue(response.getFollowees().contains(user6));
        Assertions.assertTrue(response.getFollowees().contains(user7));
        Assertions.assertTrue(response.getHasMorePages());

        // Get and verify fourth page
        request = new FollowingRequest(user6.getAlias(), 2, response.getFollowees().get(1).getAlias());
        response = serverFacadeSpy.getFollowees(request);

        Assertions.assertEquals(1, response.getFollowees().size());
        Assertions.assertTrue(response.getFollowees().contains(user8));
        Assertions.assertFalse(response.getHasMorePages());
    }
}
