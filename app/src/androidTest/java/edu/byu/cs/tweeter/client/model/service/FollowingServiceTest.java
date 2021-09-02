package edu.byu.cs.tweeter.client.model.service;

import android.os.Looper;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import edu.byu.cs.tweeter.model.domain.AuthToken;
import edu.byu.cs.tweeter.model.domain.User;
import edu.byu.cs.tweeter.util.Pair;

public class FollowingServiceTest {

    private AuthToken validRequest_authToken;
    private User validRequest_targetUser;
    private int validRequest_limit;
    private User validRequest_lastFollowee;

    private AuthToken invalidRequest_authToken;
    private User invalidRequest_targetUser;
    private int invalidRequest_limit;
    private User invalidRequest_lastFollowee;

    private List<User> successResponse_followees;
    private boolean successResponse_hasMorePages;

    private String failureResponse_message;

    private FollowingService.GetFollowingTask validRequest_GetFollowingTaskSpy;
    private FollowingService.GetFollowingTask invalidRequest_GetFollowingTaskSpy;

    private FollowingServiceObserver observer;
    private FollowingService followingServiceSpy;

    private CountDownLatch countDownLatch;

    @Before
    public void setup() {
        User currentUser = new User("FirstName", "LastName", null);

        User resultUser1 = new User("FirstName1", "LastName1",
                "https://faculty.cs.byu.edu/~jwilkerson/cs340/tweeter/images/donald_duck.png");
        User resultUser2 = new User("FirstName2", "LastName2",
                "https://faculty.cs.byu.edu/~jwilkerson/cs340/tweeter/images/daisy_duck.png");
        User resultUser3 = new User("FirstName3", "LastName3",
                "https://faculty.cs.byu.edu/~jwilkerson/cs340/tweeter/images/daisy_duck.png");

        // Setup request data to use in the tests
        validRequest_authToken = new AuthToken();
        validRequest_targetUser = currentUser;
        validRequest_limit = 3;
        validRequest_lastFollowee = null;

        invalidRequest_authToken = null;
        invalidRequest_targetUser = null;
        invalidRequest_limit = 0;
        invalidRequest_lastFollowee = null;

        // Setup response data to use in the tests
        successResponse_followees = Arrays.asList(resultUser1, resultUser2, resultUser3);
        successResponse_hasMorePages = false;

        failureResponse_message = "An exception occurred";

        // Setup an observer for the FollowingService
        observer = new FollowingServiceObserver();
        resetCountDownLatch();

        // Create a FollowingService instance and wrap it with a spy that will use mock tasks
        FollowingService followingService = new FollowingService(observer);
        followingServiceSpy = Mockito.spy(followingService);

        FollowingService.GetFollowingTask validRequest_GetFollowingTask =
                new FollowingService.GetFollowingTask(validRequest_authToken, validRequest_targetUser,
                        validRequest_limit, validRequest_lastFollowee,
                        new FollowingService.MessageHandler(Looper.getMainLooper(), observer));
        validRequest_GetFollowingTaskSpy = Mockito.spy(validRequest_GetFollowingTask);
        Pair<List<User>, Boolean> successResponse_followees = new Pair<>(this.successResponse_followees, successResponse_hasMorePages);
        Mockito.when(validRequest_GetFollowingTaskSpy.getFollowees()).thenReturn(successResponse_followees);
        Mockito.when(followingServiceSpy.getGetFollowingTask(validRequest_authToken, validRequest_targetUser,
                        validRequest_limit, validRequest_lastFollowee)).thenReturn(validRequest_GetFollowingTaskSpy);

        FollowingService.GetFollowingTask invalidRequest_GetFollowingTask =
                new FollowingService.GetFollowingTask(invalidRequest_authToken, invalidRequest_targetUser,
                        invalidRequest_limit, invalidRequest_lastFollowee,
                        new FollowingService.MessageHandler(Looper.getMainLooper(), observer));
        invalidRequest_GetFollowingTaskSpy = Mockito.spy(invalidRequest_GetFollowingTask);
        Answer<Void> runTaskAnswer = new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                invalidRequest_GetFollowingTaskSpy.sendFailedMessage(failureResponse_message);
                return null;
            }
        };
        Mockito.doAnswer(runTaskAnswer).when(invalidRequest_GetFollowingTaskSpy).runTask();
        Mockito.when(followingServiceSpy.getGetFollowingTask(invalidRequest_authToken, invalidRequest_targetUser,
                invalidRequest_limit, invalidRequest_lastFollowee)).thenReturn(invalidRequest_GetFollowingTaskSpy);
    }

    private void resetCountDownLatch() {
        countDownLatch = new CountDownLatch(1);
    }

    private void awaitCountDownLatch() throws InterruptedException {
        countDownLatch.await();
        resetCountDownLatch();
    }

    /**
     * A {@link FollowingService.Observer} implementation that can be used to get the values eventually
     * returned by an asynchronous call on the {@link FollowingService}. Counts down on the
     * countDownLatch so tests can wait for the background thread to call a method on the observer.
     */
    private class FollowingServiceObserver implements FollowingService.Observer {

        private List<User> followees;
        private boolean hasMorePages;
        private String failureMessage;
        private Exception exception;

        @Override
        public void followeesRetrieved(List<User> followees, boolean hasMorePages) {
            this.followees = followees;
            this.hasMorePages = hasMorePages;
            this.failureMessage = null;
            this.exception = null;

            countDownLatch.countDown();
        }

        @Override
        public void followeesNotRetrieved(String message) {
            this.followees = null;
            this.hasMorePages = false;
            this.failureMessage = message;
            this.exception = null;

            countDownLatch.countDown();
        }

        @Override
        public void handleException(Exception exception) {
            this.followees = null;
            this.hasMorePages = false;
            this.failureMessage = null;
            this.exception = exception;

            countDownLatch.countDown();
        }

        public List<User> getFollowees() {
            return followees;
        }
        public boolean getHasMorePages() { return hasMorePages; }
        public String getFailureMessage() { return failureMessage; }
        public Exception getException() {
            return exception;
        }
    }

    /**
     * Verify that when the {@link FollowingService#getFollowees(AuthToken, User, int, User)} is called with
     * a valid request, a valid success response is returned.
     */
    @Test
    public void testGetFollowees_validRequest_passesCorrectResponseToObserver() throws InterruptedException {
        followingServiceSpy.getFollowees(validRequest_authToken, validRequest_targetUser,
                                            validRequest_limit, validRequest_lastFollowee);

        // Wait for the background thread to finish and invoke a method on the observer
        awaitCountDownLatch();

        Assert.assertArrayEquals(successResponse_followees.toArray(), observer.getFollowees().toArray());
        Assert.assertEquals(successResponse_hasMorePages, observer.getHasMorePages());
        Assert.assertNull(observer.getFailureMessage());
        Assert.assertNull(observer.getException());
    }

    /**
     * Verify that for successful requests, the profile image of each user is included in the result.
     */
    @Test
    public void testGetFollowees_validRequest_loadsProfileImages() throws InterruptedException {
        followingServiceSpy.getFollowees(validRequest_authToken, validRequest_targetUser,
                                            validRequest_limit, validRequest_lastFollowee);

        awaitCountDownLatch();

        for(User user : observer.getFollowees()) {
            Assert.assertNotNull(user.getImageBytes());
        }
    }

    /**
     * Verify that for unsuccessful requests, the {@link FollowingService} returns
     * the failure result.
     */
    @Test
    public void testGetFollowees_invalidRequest_returnsNoFollowees() throws InterruptedException {
        followingServiceSpy.getFollowees(invalidRequest_authToken, invalidRequest_targetUser,
                                            invalidRequest_limit, invalidRequest_lastFollowee);

        awaitCountDownLatch();

        Assert.assertNull(observer.getFollowees());
        Assert.assertFalse(observer.getHasMorePages());
        Assert.assertEquals(failureResponse_message, observer.getFailureMessage());
        Assert.assertNull(observer.getException());
    }

    /**
     * Verify that when an IOException occurs while loading an image, the exception is passed to
     * the observer.
     */
    @Test
    public void testGetFollowees_exceptionThrownLoadingImages_observerReceivesException() throws IOException, InterruptedException {
        IOException exception = new IOException();
        Mockito.doThrow(exception).when(validRequest_GetFollowingTaskSpy).loadImages(Mockito.any());

        followingServiceSpy.getFollowees(validRequest_authToken, validRequest_targetUser,
                                            validRequest_limit, validRequest_lastFollowee);

        awaitCountDownLatch();

        Assert.assertNull(observer.getFollowees());
        Assert.assertFalse(observer.getHasMorePages());
        Assert.assertNull(observer.getFailureMessage());
        Assert.assertEquals(exception, observer.getException());
    }
}
