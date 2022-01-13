package edu.byu.cs.tweeter.server.dao;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import edu.byu.cs.tweeter.model.domain.AuthToken;
import edu.byu.cs.tweeter.model.domain.User;
import edu.byu.cs.tweeter.model.net.request.FollowingRequest;
import edu.byu.cs.tweeter.model.net.response.FollowingResponse;

public class FollowingDAOTest {
    private final AuthToken testUserAuthToken = new AuthToken();

    private final User user1 = new User("Daffy", "Duck", "");
    private final User user2 = new User("Fred", "Flintstone", "");
    private final User user3 = new User("Barney", "Rubble", "");
    private final User user4 = new User("Wilma", "Rubble", "");
    private final User user5 = new User("Clint", "Eastwood", "");
    private final User user6 = new User("Mother", "Teresa", "");
    private final User user7 = new User("Harriett", "Hansen", "");
    private final User user8 = new User("Zoe", "Zabriski", "");

    private FollowDAO followDAOSpy;

    @Before
    public void setup() {
        followDAOSpy = Mockito.spy(new FollowDAO());
    }

    @Test
    public void testGetFollowees_noFolloweesForUser() {
        List<User> followees = Collections.emptyList();
        Mockito.when(followDAOSpy.getDummyFollowees()).thenReturn(followees);

        FollowingRequest request = new FollowingRequest(testUserAuthToken, user1.getAlias(), 10, null);
        FollowingResponse response = followDAOSpy.getFollowees(request);

        Assert.assertEquals(0, response.getFollowees().size());
        Assert.assertFalse(response.getHasMorePages());
    }

    @Test
    public void testGetFollowees_oneFollowerForUser_limitGreaterThanUsers() {
        List<User> followees = Collections.singletonList(user2);
        Mockito.when(followDAOSpy.getDummyFollowees()).thenReturn(followees);

        FollowingRequest request = new FollowingRequest(testUserAuthToken, user1.getAlias(), 10, null);
        FollowingResponse response = followDAOSpy.getFollowees(request);

        Assert.assertEquals(1, response.getFollowees().size());
        Assert.assertTrue(response.getFollowees().contains(user2));
        Assert.assertFalse(response.getHasMorePages());
    }

    @Test
    public void testGetFollowees_twoFollowersForUser_limitEqualsUsers() {
        List<User> followees = Arrays.asList(user2, user3);
        Mockito.when(followDAOSpy.getDummyFollowees()).thenReturn(followees);

        FollowingRequest request = new FollowingRequest(testUserAuthToken, user3.getAlias(), 2, null);
        FollowingResponse response = followDAOSpy.getFollowees(request);

        Assert.assertEquals(2, response.getFollowees().size());
        Assert.assertTrue(response.getFollowees().contains(user2));
        Assert.assertTrue(response.getFollowees().contains(user3));
        Assert.assertFalse(response.getHasMorePages());
    }

    @Test
    public void testGetFollowees_limitLessThanUsers_endsOnPageBoundary() {
        List<User> followees = Arrays.asList(user2, user3, user4, user5, user6, user7);
        Mockito.when(followDAOSpy.getDummyFollowees()).thenReturn(followees);

        FollowingRequest request = new FollowingRequest(testUserAuthToken, user5.getAlias(), 2, null);
        FollowingResponse response = followDAOSpy.getFollowees(request);

        // Verify first page
        Assert.assertEquals(2, response.getFollowees().size());
        Assert.assertTrue(response.getFollowees().contains(user2));
        Assert.assertTrue(response.getFollowees().contains(user3));
        Assert.assertTrue(response.getHasMorePages());

        // Get and verify second page
        request = new FollowingRequest(testUserAuthToken, user5.getAlias(), 2, response.getFollowees().get(1).getAlias());
        response = followDAOSpy.getFollowees(request);

        Assert.assertEquals(2, response.getFollowees().size());
        Assert.assertTrue(response.getFollowees().contains(user4));
        Assert.assertTrue(response.getFollowees().contains(user5));
        Assert.assertTrue(response.getHasMorePages());

        // Get and verify third page
        request = new FollowingRequest(testUserAuthToken, user5.getAlias(), 2, response.getFollowees().get(1).getAlias());
        response = followDAOSpy.getFollowees(request);

        Assert.assertEquals(2, response.getFollowees().size());
        Assert.assertTrue(response.getFollowees().contains(user6));
        Assert.assertTrue(response.getFollowees().contains(user7));
        Assert.assertFalse(response.getHasMorePages());
    }


    @Test
    public void testGetFollowees_limitLessThanUsers_notEndsOnPageBoundary() {
        List<User> followees = Arrays.asList(user2, user3, user4, user5, user6, user7, user8);
        Mockito.when(followDAOSpy.getDummyFollowees()).thenReturn(followees);

        FollowingRequest request = new FollowingRequest(testUserAuthToken, user6.getAlias(), 2, null);
        FollowingResponse response = followDAOSpy.getFollowees(request);

        // Verify first page
        Assert.assertEquals(2, response.getFollowees().size());
        Assert.assertTrue(response.getFollowees().contains(user2));
        Assert.assertTrue(response.getFollowees().contains(user3));
        Assert.assertTrue(response.getHasMorePages());

        // Get and verify second page
        request = new FollowingRequest(testUserAuthToken, user6.getAlias(), 2, response.getFollowees().get(1).getAlias());
        response = followDAOSpy.getFollowees(request);

        Assert.assertEquals(2, response.getFollowees().size());
        Assert.assertTrue(response.getFollowees().contains(user4));
        Assert.assertTrue(response.getFollowees().contains(user5));
        Assert.assertTrue(response.getHasMorePages());

        // Get and verify third page
        request = new FollowingRequest(testUserAuthToken, user6.getAlias(), 2, response.getFollowees().get(1).getAlias());
        response = followDAOSpy.getFollowees(request);

        Assert.assertEquals(2, response.getFollowees().size());
        Assert.assertTrue(response.getFollowees().contains(user6));
        Assert.assertTrue(response.getFollowees().contains(user7));
        Assert.assertTrue(response.getHasMorePages());

        // Get and verify fourth page
        request = new FollowingRequest(testUserAuthToken, user6.getAlias(), 2, response.getFollowees().get(1).getAlias());
        response = followDAOSpy.getFollowees(request);

        Assert.assertEquals(1, response.getFollowees().size());
        Assert.assertTrue(response.getFollowees().contains(user8));
        Assert.assertFalse(response.getHasMorePages());
    }
}
