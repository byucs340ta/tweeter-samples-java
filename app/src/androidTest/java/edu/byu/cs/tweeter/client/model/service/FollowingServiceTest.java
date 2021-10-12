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

public class FollowingServiceTest {

    private FollowingRequest validRequest;
    private FollowingRequest invalidRequest;

    private List<User> success_followees;
    private boolean success_hasMorePages;
    private FollowingResponse successResponse;

    private String failure_message;
    private FollowingResponse failureResponse;

    private FollowingServiceObserver observer;

    private CountDownLatch countDownLatch;

    /**
     * Create a FollowingService spy that uses a mock ServerFacade to return known responses to
     * requests.
     */
    @Before
    public void setup() {
        User currentUser = new User("FirstName", "LastName", null);

        User resultUser1 = new User("FirstName1", "LastName1",
                "https://faculty.cs.byu.edu/~jwilkerson/cs340/tweeter/images/donald_duck.png");
        User resultUser2 = new User("FirstName2", "LastName2",
                "https://faculty.cs.byu.edu/~jwilkerson/cs340/tweeter/images/daisy_duck.png");
        User resultUser3 = new User("FirstName3", "LastName3",
                "https://faculty.cs.byu.edu/~jwilkerson/cs340/tweeter/images/daisy_duck.png");

        // Setup valid and invalid requests to be used in the tests
        validRequest = new FollowingRequest(new AuthToken(), currentUser.getAlias(), 3, null);
        invalidRequest = new FollowingRequest(null, null, 0, null);

        // Setup success and failure responses to be used in the tests
        success_followees = Arrays.asList(resultUser1, resultUser2, resultUser3);
        success_hasMorePages = false;
        successResponse = new FollowingResponse(success_followees, success_hasMorePages);

        failure_message = "An exception occurred";
        failureResponse = new FollowingResponse(failure_message);

        // Setup an observer for the FollowingService
        observer = new FollowingServiceObserver();

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
     * A {@link FollowingService.Observer} implementation that can be used to get the values
     * eventually returned by an asynchronous call on the {@link FollowingService}. Counts down
     * on the countDownLatch so tests can wait for the background thread to call a method on the
     * observer.
     */
    private class FollowingServiceObserver implements FollowingService.Observer {

        private boolean success;
        private String message;
        private List<User> followees;
        private boolean hasMorePages;
        private Exception exception;

        @Override
        public void followeesRetrieved(List<User> followees, boolean hasMorePages) {
            this.success = true;
            this.message = null;
            this.followees = followees;
            this.hasMorePages = hasMorePages;
            this.exception = null;

            countDownLatch.countDown();
        }

        @Override
        public void followeesNotRetrieved(String message) {
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

    private FollowingService setupFollowingServiceSpy(FollowingResponse serverFacadeResponse) {
        ServerFacade mockServerFacade = Mockito.mock(ServerFacade.class);
        try {
            Mockito.when(mockServerFacade.getFollowees(Mockito.any(), Mockito.any())).thenReturn(serverFacadeResponse);
        } catch (Exception e) {
            // We won't actually get an exception while setting up the mock
        }

        FollowingService followingService = new FollowingService(observer);
        FollowingService followingServiceSpy = Mockito.spy(followingService);
        Mockito.when(followingServiceSpy.getServerFacade()).thenReturn(mockServerFacade);

        return followingServiceSpy;
    }

    private static void assertEquals(FollowingResponse response, FollowingServiceObserver observer) {
        Assert.assertEquals(response.isSuccess(), observer.isSuccess());
        Assert.assertEquals(response.getMessage(), observer.getMessage());
        Assert.assertEquals(response.getFollowees(), observer.getFollowees());
        Assert.assertEquals(response.getHasMorePages(), observer.getHasMorePages());
        Assert.assertEquals(null, observer.getException());
    }

    private static void assertEquals(Exception exception, FollowingServiceObserver observer) {
        Assert.assertEquals(false, observer.isSuccess());
        Assert.assertEquals(null, observer.getMessage());
        Assert.assertEquals(null, observer.getFollowees());
        Assert.assertEquals(false, observer.getHasMorePages());
        Assert.assertEquals(exception, observer.getException());
    }

    /**
     * Verify that for successful requests, the {@link FollowingService#getFollowees}
     * asynchronous method eventually returns the same result as the {@link ServerFacade}.
     */
    @Test
    public void testGetFollowees_validRequest_correctResponse() throws InterruptedException {
        FollowingService followingServiceSpy = setupFollowingServiceSpy(successResponse);

        followingServiceSpy.getFollowees(validRequest);
        awaitCountDownLatch();

        assertEquals(successResponse, observer);
    }

    /**
     * Verify that for successful requests, the the {@link FollowingService#getFollowees}
     * method loads the profile image of each user included in the result.
     */
    @Test
    public void testGetFollowees_validRequest_loadsProfileImages() throws InterruptedException {
        FollowingService followingServiceSpy = setupFollowingServiceSpy(successResponse);

        followingServiceSpy.getFollowees(validRequest);
        awaitCountDownLatch();

        List<User> followees = observer.getFollowees();
        Assert.assertTrue(followees.size() > 0);

        for(User user : followees) {
            Assert.assertNotNull(user.getImageBytes());
        }
    }

    /**
     * Verify that for unsuccessful requests, the the {@link FollowingService#getFollowees}
     * method returns the same failure response as the server facade.
     */
    @Test
    public void testGetFollowees_invalidRequest_returnsNoFollowees() throws InterruptedException {
        FollowingService followingServiceSpy = setupFollowingServiceSpy(failureResponse);

        followingServiceSpy.getFollowees(invalidRequest);
        awaitCountDownLatch();

        assertEquals(failureResponse, observer);
    }

    /**
     * Verify that when an IOException occurs while loading an image, the {@link FollowingService.Observer#handleException(Exception)}
     * method of the Service's observer is called and the exception is passed to it.
     */
    @Test
    public void testGetFollowees_exceptionThrownLoadingImages_observerHandleExceptionMethodCalled() throws IOException, InterruptedException {
        FollowingService followingServiceSpy = setupFollowingServiceSpy(successResponse);

        // Create a task spy for the FollowingService that throws an exception when it's loadImages method is called
        FollowingService.GetFollowingTask getFollowingTask =
                followingServiceSpy.getGetFollowingTask(validRequest);
        FollowingService.GetFollowingTask getFollowingTaskSpy = Mockito.spy(getFollowingTask);

        IOException exception = new IOException();
        Mockito.doThrow(exception).when(getFollowingTaskSpy).loadImages(Mockito.any());

        // Make the FollowingService spy use the RetrieveFollowingTask spy
        Mockito.when(followingServiceSpy.getGetFollowingTask(Mockito.any())).thenReturn(getFollowingTaskSpy);

        followingServiceSpy.getFollowees(validRequest);
        awaitCountDownLatch();

        Assert.assertEquals(exception, observer.getException());
    }
}
