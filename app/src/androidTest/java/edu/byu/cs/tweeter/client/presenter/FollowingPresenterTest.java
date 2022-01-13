package edu.byu.cs.tweeter.client.presenter;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.util.Arrays;
import java.util.List;

import edu.byu.cs.tweeter.client.model.service.FollowService;
import edu.byu.cs.tweeter.model.domain.AuthToken;
import edu.byu.cs.tweeter.model.domain.User;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class FollowingPresenterTest {

    private static final String MALE_IMAGE_URL = "https://faculty.cs.byu.edu/~jwilkerson/cs340/tweeter/images/donald_duck.png";
    private static final String FEMALE_IMAGE_URL = "https://faculty.cs.byu.edu/~jwilkerson/cs340/tweeter/images/daisy_duck.png";

    private final User user1 = new User("Allen", "Anderson", MALE_IMAGE_URL);
    private final User user2 = new User("Amy", "Ames", FEMALE_IMAGE_URL);
    private final User user3 = new User("Bob", "Bobson", MALE_IMAGE_URL);
    private final User user4 = new User("Bonnie", "Beatty", FEMALE_IMAGE_URL);
    private final User user5 = new User("Chris", "Colston", MALE_IMAGE_URL);

    private User fakeUser;
    private AuthToken fakeAuthToken;
    private FollowService followingServiceMock;
    private FollowingPresenter followingPresenterSpy;
    private FollowingPresenter.View followingViewMock;

    /**
     * Setup mocks and spies needed to let test cases control what users are returned
     * by {@link FollowService}.
     * Setup mock {@link FollowingPresenter} to verify that {@link FollowingPresenter}
     * correctly calls view methods.
     */
    @Before
    public void setup() {
        fakeUser = new User("Paul", "Bunyon", "@Paul_Bunyon_123", "https://s3.amazon.com/paul_bunyon");
        fakeAuthToken = new AuthToken("abc-123-xyz-789", "August 12, 2021 3:01 PM");

        // followingViewMock is used to verify that FollowingPresenter correctly calls view methods.
        followingViewMock = Mockito.mock(FollowingPresenter.View.class);

        // Create the mocks and spies needed to let test cases control what users are returned
        // FollowService.
        FollowingPresenter followingPresenter = new FollowingPresenter(followingViewMock, fakeUser, fakeAuthToken);
        followingPresenterSpy = Mockito.spy(followingPresenter);

        followingServiceMock = Mockito.mock(FollowService.class);
        Mockito.doReturn(followingServiceMock).when(followingPresenterSpy).getFollowingService();
    }

    /**
     * Verify that {@link FollowingPresenter} has the correct initial state.
     */
    @Test
    public void testInitialPresenterState() {
        assertNull(followingPresenterSpy.getLastFollowee());
        assertTrue(followingPresenterSpy.isHasMorePages());
        assertFalse(followingPresenterSpy.isLoading());
    }

    @Test
    public void testLoadMoreItems_CorrectParamsPassedToStatusService() {
        List<User> followees = Arrays.asList(user1, user2, user3, user4, user5);

        Answer<Void> manyFolloweesAnswer = new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                AuthToken authToken = invocation.getArgument(0);
                User user = invocation.getArgument(1);
                int limit = invocation.getArgument(2);
                User lastFollowee = invocation.getArgument(3);

                // Assert that the parameters are correct
                Assert.assertEquals(fakeUser, user);
                Assert.assertEquals(fakeAuthToken, authToken);
                Assert.assertEquals(limit, FollowingPresenter.PAGE_SIZE);
                Assert.assertEquals(lastFollowee, followingPresenterSpy.getLastFollowee());

                FollowService.GetFollowingObserver observer = invocation.getArgument(4);
                observer.handleSuccess(followees, true);
                return null;
            }
        };
        Mockito.doAnswer(manyFolloweesAnswer).when(followingServiceMock).getFollowees(Mockito.any(), Mockito.any(), Mockito.anyInt(), Mockito.any(), Mockito.any());
        followingPresenterSpy.loadMoreItems();
    }



    /**
     * Verify that {@link FollowingPresenter#loadMoreItems} works correctly when there
     * are some pages of followees.
     */
    @Test
    public void testLoadMoreItems_GetFolloweesSuccess() throws InterruptedException {
        List<User> followees = Arrays.asList(user1, user2, user3, user4, user5);

        Answer<Void> manyFolloweesAnswer = new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                FollowService.GetFollowingObserver observer = invocation.getArgument(4);
                observer.handleSuccess(followees, true);
                return null;
            }
        };
        Mockito.doAnswer(manyFolloweesAnswer).when(followingServiceMock).getFollowees(Mockito.any(), Mockito.any(), Mockito.anyInt(), Mockito.any(), Mockito.any());

        followingPresenterSpy.loadMoreItems();

        assertEquals(user5, followingPresenterSpy.getLastFollowee());
        assertTrue(followingPresenterSpy.isHasMorePages());
        assertFalse(followingPresenterSpy.isLoading());

        Mockito.verify(followingViewMock).setLoading(true);
        Mockito.verify(followingViewMock).setLoading(false);
        Mockito.verify(followingViewMock).addItems(Arrays.asList(user1, user2, user3, user4, user5));
    }

    /**
     * Verify that {@link FollowingPresenter#loadMoreItems} works correctly when there
     * are between two and three pages of followees.
     */
    @Test
    public void testLoadMoreItems_GetFolloweesFailsWithErrorMessage() throws InterruptedException {
        Answer<Void> failureAnswer = new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                FollowService.GetFollowingObserver observer = invocation.getArgument(4);
                observer.handleFailure("failure message");
                return null;
            }
        };
        Mockito.doAnswer(failureAnswer).when(followingServiceMock).getFollowees(Mockito.any(), Mockito.any(), Mockito.anyInt(), Mockito.any(), Mockito.any());

        followingPresenterSpy.loadMoreItems();

        assertFalse(followingPresenterSpy.isLoading());

        Mockito.verify(followingViewMock).setLoading(true);
        Mockito.verify(followingViewMock).setLoading(false);
        Mockito.verify(followingViewMock).displayErrorMessage("Failed to retrieve followees: " + "failure message");
        Mockito.verify(followingViewMock, Mockito.times(0)).addItems(Mockito.any());
    }

    @Test
    public void testLoadMoreItems_GetFolloweesFailsWithExceptionMessage() throws InterruptedException {
        Answer<Void> exceptionAnswer = new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                FollowService.GetFollowingObserver observer = invocation.getArgument(4);
                observer.handleException(new Exception("The exception message"));
                return null;
            }
        };
        Mockito.doAnswer(exceptionAnswer).when(followingServiceMock).getFollowees(Mockito.any(), Mockito.any(), Mockito.anyInt(), Mockito.any(), Mockito.any());

        followingPresenterSpy.loadMoreItems();


        assertFalse(followingPresenterSpy.isLoading());

        Mockito.verify(followingViewMock).setLoading(true);
        Mockito.verify(followingViewMock).setLoading(false);
        Mockito.verify(followingViewMock).displayErrorMessage("Failed to retrieve followees because of exception: " + "The exception message");
        Mockito.verify(followingViewMock, Mockito.times(0)).addItems(Mockito.any());
    }


}