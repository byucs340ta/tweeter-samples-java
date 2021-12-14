package edu.byu.cs.tweeter.client.model.service;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import edu.byu.cs.tweeter.client.model.net.ServerFacade;
import edu.byu.cs.tweeter.model.domain.AuthToken;
import edu.byu.cs.tweeter.model.domain.User;
import edu.byu.cs.tweeter.model.net.request.FollowingRequest;
import edu.byu.cs.tweeter.model.net.response.FollowingResponse;
import edu.byu.cs.tweeter.util.FakeData;

public class FollowServiceTest {

    private User currentUser;
    private AuthToken currentAuthToken;

    //private FollowingResponse successResponse;
    //private FollowingResponse failureResponse;

    private FollowService followServiceSpy;
    private FollowServiceObserver observer;

    private CountDownLatch countDownLatch;

    /**
     * Create a FollowService spy that uses a mock ServerFacade to return known responses to
     * requests.
     */
    @Before
    public void setup() {
        currentUser = new User("FirstName", "LastName", null);
        currentAuthToken = new AuthToken();

        // Setup success and failure responses to be used in the tests
        //successResponse = new FollowingResponse(success_followees, false);

        //failureResponse = new FollowingResponse("An exception occurred");

        followServiceSpy = Mockito.spy(new FollowService());

        // Setup an observer for the FollowService
        observer = new FollowServiceObserver();

        // Prepare the countdown latch
        resetCountDownLatch();
    }

    private void resetCountDownLatch() {
        countDownLatch = new CountDownLatch(1);
    }

    private void awaitCountDownLatch() throws InterruptedException {
        countDownLatch.await();
        resetCountDownLatch();
    }

    /**
     * A {@link FollowService.GetFollowingObserver} implementation that can be used to get the values
     * eventually returned by an asynchronous call on the {@link FollowService}. Counts down
     * on the countDownLatch so tests can wait for the background thread to call a method on the
     * observer.
     */
    private class FollowServiceObserver implements FollowService.GetFollowingObserver {

        private boolean success;
        private String message;
        private List<User> followees;
        private boolean hasMorePages;
        private Exception exception;

        @Override
        public void handleSuccess(List<User> followees, boolean hasMorePages) {
            this.success = true;
            this.message = null;
            this.followees = followees;
            this.hasMorePages = hasMorePages;
            this.exception = null;

            countDownLatch.countDown();
        }

        @Override
        public void handleFailure(String message) {
            this.success = false;
            this.message = message;
            this.followees = null;
            this.hasMorePages = false;
            this.exception = null;

            countDownLatch.countDown();
        }

        @Override
        public void handleException(Exception exception) {
            this.success = false;
            this.message = null;
            this.followees = null;
            this.hasMorePages = false;
            this.exception = exception;

            countDownLatch.countDown();
        }

        public boolean isSuccess() {
            return success;
        }

        public String getMessage() {
            return message;
        }

        public List<User> getFollowees() {
            return followees;
        }

        public boolean getHasMorePages() {
            return hasMorePages;
        }

        public Exception getException() {
            return exception;
        }
    }


    private static void assertEquals(FollowingResponse response, FollowServiceObserver observer) {
        Assert.assertEquals(response.isSuccess(), observer.isSuccess());
        Assert.assertEquals(response.getMessage(), observer.getMessage());
        Assert.assertEquals(response.getFollowees(), observer.getFollowees());
        Assert.assertEquals(response.getHasMorePages(), observer.getHasMorePages());
        Assert.assertNull(observer.getException());
    }

    private static void assertEquals(Exception exception, FollowServiceObserver observer) {
        Assert.assertFalse(observer.isSuccess());
        Assert.assertNull(observer.getMessage());
        Assert.assertNull(observer.getFollowees());
        Assert.assertFalse(observer.getHasMorePages());
        Assert.assertEquals(exception, observer.getException());
    }

    /**
     * Verify that for successful requests, the {@link FollowService#getFollowees}
     * asynchronous method eventually returns the same result as the {@link ServerFacade}.
     */
    @Test
    public void testGetFollowees_validRequest_correctResponse() throws InterruptedException {
        followServiceSpy.getFollowees(currentAuthToken, currentUser, 3, null, observer);
        awaitCountDownLatch();

        List<User> expectedFollowees = new FakeData().getFakeUsers().subList(0, 3);

        Assert.assertEquals(true, observer.isSuccess());
        Assert.assertEquals("", observer.getMessage());
        Assert.assertEquals(expectedFollowees, observer.getFollowees());
        Assert.assertEquals(true, observer.getHasMorePages());
        Assert.assertNull(observer.getException());
    }

    /**
     * Verify that for successful requests, the the {@link FollowService#getFollowees}
     * method loads the profile image of each user included in the result.
     */
    @Test
    public void testGetFollowees_validRequest_loadsProfileImages() throws InterruptedException {
        followServiceSpy.getFollowees(currentAuthToken, currentUser, 3, null, observer);
        awaitCountDownLatch();

        List<User> followees = observer.getFollowees();
        Assert.assertTrue(followees.size() > 0);

        for(User user : followees) {
            Assert.assertNotNull(user.getImageBytes());
        }
    }

    /**
     * Verify that for unsuccessful requests, the the {@link FollowService#getFollowees}
     * method returns the same failure response as the server facade.
     */
    @Test
    public void testGetFollowees_invalidRequest_returnsNoFollowees() throws InterruptedException {
        followServiceSpy.getFollowees(null, null, 0, null, observer);
        awaitCountDownLatch();

        assertEquals(failureResponse, observer);
    }

    /**
     * Verify that when an IOException occurs while loading an image, the {@link FollowService.GetFollowingObserver#handleException(Exception)}
     * method of the Service's observer is called and the exception is passed to it.
     */
    @Test
    public void testGetFollowees_exceptionThrownLoadingImages_observerHandleExceptionMethodCalled() throws IOException, InterruptedException {
        // Create a task spy for the FollowService that throws an exception when it's loadImages method is called
        FollowService.GetFollowingTask getFollowingTask =
                followServiceSpy.getGetFollowingTask(currentAuthToken, currentUser, 3, null, observer);
        FollowService.GetFollowingTask getFollowingTaskSpy = Mockito.spy(getFollowingTask);

        IOException exception = new IOException();
        Mockito.doThrow(exception).when(getFollowingTaskSpy).loadImages(Mockito.any());

        // Make the FollowService spy use the RetrieveFollowingTask spy
        Mockito.when(followServiceSpy.getGetFollowingTask(Mockito.any(), Mockito.any(), Mockito.anyInt(), Mockito.any(), Mockito.any()))
                .thenReturn(getFollowingTaskSpy);

        followServiceSpy.getFollowees(currentAuthToken, currentUser, 3, null, observer);
        awaitCountDownLatch();

        Assert.assertEquals(exception, observer.getException());
    }
}
