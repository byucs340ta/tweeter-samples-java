package edu.byu.cs.tweeter.client.presenter;

import android.os.Looper;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import edu.byu.cs.tweeter.client.model.service.FollowingService;
import edu.byu.cs.tweeter.model.domain.AuthToken;
import edu.byu.cs.tweeter.model.domain.User;
import edu.byu.cs.tweeter.util.FakeData;

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
    private final User user6 = new User("Cindy", "Coats", FEMALE_IMAGE_URL);
    private final User user7 = new User("Dan", "Donaldson", MALE_IMAGE_URL);
    private final User user8 = new User("Dee", "Dempsey", FEMALE_IMAGE_URL);
    private final User user9 = new User("Elliott", "Enderson", MALE_IMAGE_URL);
    private final User user10 = new User("Elizabeth", "Engle", FEMALE_IMAGE_URL);
    private final User user11 = new User("Frank", "Frandson", MALE_IMAGE_URL);
    private final User user12 = new User("Fran", "Franklin", FEMALE_IMAGE_URL);
    private final User user13 = new User("Gary", "Gilbert", MALE_IMAGE_URL);
    private final User user14 = new User("Giovanna", "Giles", FEMALE_IMAGE_URL);
    private final User user15 = new User("Henry", "Henderson", MALE_IMAGE_URL);
    private final User user16 = new User("Helen", "Hopwell", FEMALE_IMAGE_URL);
    private final User user17 = new User("Igor", "Isaacson", MALE_IMAGE_URL);
    private final User user18 = new User("Isabel", "Isaacson", FEMALE_IMAGE_URL);
    private final User user19 = new User("Justin", "Jones", MALE_IMAGE_URL);
    private final User user20 = new User("Jill", "Johnson", FEMALE_IMAGE_URL);
    private final User user21 = new User("John", "Brown", MALE_IMAGE_URL);

    private FakeData fakeDataSpy;
    private FollowingService followingServiceSpy;
    private FollowingPresenter followingPresenterSpy;
    private FollowingPresenter.View followingViewMock;
    private CountDownLatch countDownLatch;

    /**
     * Setup mocks and spies needed to let test cases control what users are returned
     * by {@link FollowingService}.
     * Setup mock {@link FollowingPresenter} to verify that {@link FollowingPresenter}
     * correctly calls view methods.
     */
    @Before
    public void setup() {
        // followingViewMock is used to verify that FollowingPresenter correctly calls view methods.
        followingViewMock = Mockito.mock(FollowingPresenter.View.class);

        // Create the mocks and spies needed to let test cases control what users are returned
        // FollowingService.
        FollowingPresenter followingPresenter = new FollowingPresenter(followingViewMock,
                new User("", "", ""), new AuthToken());
        followingPresenterSpy = Mockito.spy(followingPresenter);

        FollowingService followingService = new FollowingService(followingPresenterSpy);
        followingServiceSpy = Mockito.spy(followingService);

        fakeDataSpy = Mockito.spy(new FakeData());

        FollowingService.MessageHandler messageHandler =
                new FollowingService.MessageHandler(Looper.getMainLooper(), followingPresenterSpy);
        Answer<FollowingService.GetFollowingTask> getFollowingTaskAnswer = new Answer<FollowingService.GetFollowingTask>() {
            @Override
            public FollowingService.GetFollowingTask answer(InvocationOnMock invocation) throws Throwable {
                FollowingService.GetFollowingTask task = new FollowingService.GetFollowingTask(
                        invocation.getArgument(0),
                        invocation.getArgument(1),
                        invocation.getArgument(2),
                        invocation.getArgument(3),
                        messageHandler);
                FollowingService.GetFollowingTask taskSpy = Mockito.spy(task);
                Mockito.when(taskSpy.getFakeData()).thenReturn(fakeDataSpy);
                return taskSpy;
            }
        };
        Mockito.doAnswer(getFollowingTaskAnswer)
                .when(followingServiceSpy)
                .getGetFollowingTask(Mockito.any(), Mockito.any(), Mockito.anyInt(), Mockito.any());
        Mockito.doReturn(followingServiceSpy).when(followingPresenterSpy).getFollowingService(Mockito.any());

        // Configure followingPresenterSpy to decrement the CountdownLatch after observer
        // methods execute, thus unblocking test cases.
        resetCountDownLatch();

        Answer<Void> followeesRetrievedAnswer = new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                invocation.callRealMethod();
                decrementCountDownLatch();
                return null;
            }
        };
        Answer<Void> followeesNotRetrievedAnswer = new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                invocation.callRealMethod();
                decrementCountDownLatch();
                return null;
            }
        };
        Answer<Void> handleExceptionAnswer = new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                invocation.callRealMethod();
                decrementCountDownLatch();
                return null;
            }
        };
        Mockito.doAnswer(followeesRetrievedAnswer).when(followingPresenterSpy)
                .followeesRetrieved(Mockito.any(), Mockito.anyBoolean());
        Mockito.doAnswer(followeesNotRetrievedAnswer).when(followingPresenterSpy)
                .followeesNotRetrieved(Mockito.any());
        Mockito.doAnswer(handleExceptionAnswer).when(followingPresenterSpy)
                .handleException(Mockito.any());
    }

    private void resetCountDownLatch() {
        countDownLatch = new CountDownLatch(1);
    }

    private void decrementCountDownLatch() {
        countDownLatch.countDown();
    }

    private void awaitCountDownLatch() throws InterruptedException {
        countDownLatch.await();
        resetCountDownLatch();
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

    /**
     * Verify that {@link FollowingPresenter#loadMoreItems} works correctly when there are no followees.
     */
    @Test
    public void testLoadMoreItems_noFolloweesForUser() throws InterruptedException {
        List<User> followees = Collections.emptyList();
        Mockito.when(fakeDataSpy.getFakeUsers()).thenReturn(followees);

        followingPresenterSpy.loadMoreItems();
        awaitCountDownLatch();

        assertNull(followingPresenterSpy.getLastFollowee());
        assertFalse(followingPresenterSpy.isHasMorePages());
        assertFalse(followingPresenterSpy.isLoading());

        Mockito.verify(followingViewMock).setLoading(true);
        Mockito.verify(followingViewMock).setLoading(false);
        Mockito.verify(followingViewMock).addItems(Collections.emptyList());
    }

    /**
     * Verify that {@link FollowingPresenter#loadMoreItems} works correctly when there
     * is one followee.
     */
    @Test
    public void testLoadMoreItems_oneFolloweeForUser_limitGreaterThanUsers() throws InterruptedException {
        List<User> followees = Collections.singletonList(user2);
        Mockito.when(fakeDataSpy.getFakeUsers()).thenReturn(followees);

        followingPresenterSpy.loadMoreItems();
        awaitCountDownLatch();

        assertEquals(user2, followingPresenterSpy.getLastFollowee());
        assertFalse(followingPresenterSpy.isHasMorePages());
        assertFalse(followingPresenterSpy.isLoading());

        Mockito.verify(followingViewMock).setLoading(true);
        Mockito.verify(followingViewMock).setLoading(false);
        Mockito.verify(followingViewMock).addItems(Collections.singletonList(user2));
    }

    /**
     * Verify that {@link FollowingPresenter#loadMoreItems} works correctly when there
     * are two followees.
     */
    @Test
    public void testLoadMoreItems_twoFolloweesForUser_limitGreaterThanUsers() throws InterruptedException {
        List<User> followees = Arrays.asList(user2, user3);
        Mockito.when(fakeDataSpy.getFakeUsers()).thenReturn(followees);

        followingPresenterSpy.loadMoreItems();
        awaitCountDownLatch();

        assertEquals(user3, followingPresenterSpy.getLastFollowee());
        assertFalse(followingPresenterSpy.isHasMorePages());
        assertFalse(followingPresenterSpy.isLoading());

        Mockito.verify(followingViewMock).setLoading(true);
        Mockito.verify(followingViewMock).setLoading(false);
        Mockito.verify(followingViewMock).addItems(Arrays.asList(user2, user3));
    }

    /**
     * Verify that {@link FollowingPresenter#loadMoreItems} works correctly when there
     * are exactly two pages of followees.
     */
    @Test
    public void testLoadMoreItems_limitLessThanUsers_endsOnPageBoundary() throws InterruptedException {
        List<User> followees = Arrays.asList(user1, user2, user3, user4, user5, user6, user7,
                user8, user9, user10, user11, user12, user13, user14, user15, user16, user17,
                user18, user19, user20);
        Mockito.when(fakeDataSpy.getFakeUsers()).thenReturn(followees);

        followingPresenterSpy.loadMoreItems();
        awaitCountDownLatch();

        assertEquals(user10, followingPresenterSpy.getLastFollowee());
        assertTrue(followingPresenterSpy.isHasMorePages());
        assertFalse(followingPresenterSpy.isLoading());

        followingPresenterSpy.loadMoreItems();
        awaitCountDownLatch();

        assertEquals(user20, followingPresenterSpy.getLastFollowee());
        assertFalse(followingPresenterSpy.isHasMorePages());
        assertFalse(followingPresenterSpy.isLoading());

        Mockito.verify(followingViewMock, Mockito.times(2)).setLoading(true);
        Mockito.verify(followingViewMock, Mockito.times(2)).setLoading(false);
        Mockito.verify(followingViewMock).addItems(Arrays.asList(user1, user2, user3, user4,
                user5, user6, user7, user8, user9, user10));
        Mockito.verify(followingViewMock).addItems(Arrays.asList(user11, user12, user13, user14,
                user15, user16, user17, user18, user19, user20));
    }

    /**
     * Verify that {@link FollowingPresenter#loadMoreItems} works correctly when there
     * are between two and three pages of followees.
     */
    @Test
    public void testLoadMoreItems_limitLessThanUsers_notEndsOnPageBoundary() throws InterruptedException {
        List<User> followees = Arrays.asList(user1, user2, user3, user4, user5, user6, user7,
                user8, user9, user10, user11, user12, user13, user14, user15, user16, user17,
                user18, user19, user20, user21);
        Mockito.when(fakeDataSpy.getFakeUsers()).thenReturn(followees);

        followingPresenterSpy.loadMoreItems();
        awaitCountDownLatch();

        assertEquals(user10, followingPresenterSpy.getLastFollowee());
        assertTrue(followingPresenterSpy.isHasMorePages());
        assertFalse(followingPresenterSpy.isLoading());

        followingPresenterSpy.loadMoreItems();
        awaitCountDownLatch();

        assertEquals(user20, followingPresenterSpy.getLastFollowee());
        assertTrue(followingPresenterSpy.isHasMorePages());
        assertFalse(followingPresenterSpy.isLoading());

        followingPresenterSpy.loadMoreItems();
        awaitCountDownLatch();

        assertEquals(user21, followingPresenterSpy.getLastFollowee());
        assertFalse(followingPresenterSpy.isHasMorePages());
        assertFalse(followingPresenterSpy.isLoading());

        Mockito.verify(followingViewMock, Mockito.times(3)).setLoading(true);
        Mockito.verify(followingViewMock, Mockito.times(3)).setLoading(false);
        Mockito.verify(followingViewMock).addItems(Arrays.asList(user1, user2, user3, user4,
                user5, user6, user7, user8, user9, user10));
        Mockito.verify(followingViewMock).addItems(Arrays.asList(user11, user12, user13, user14,
                user15, user16, user17, user18, user19, user20));
        Mockito.verify(followingViewMock).addItems(Collections.singletonList(user21));
    }
}
