package edu.byu.cs.tweeter.model.service;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;

import edu.byu.cs.tweeter.model.domain.User;
import edu.byu.cs.tweeter.model.net.ServerFacade;
import edu.byu.cs.tweeter.model.service.request.FollowingRequest;
import edu.byu.cs.tweeter.model.service.response.FollowingResponse;

public class FollowingServiceTest {

    private FollowingRequest validRequest;
    private FollowingRequest invalidRequest;

    private FollowingResponse successResponse;
    private FollowingResponse failureResponse;

    private FollowingServiceObserver observer;
    private FollowingService followingServiceSpy;

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

        // Setup request objects to use in the tests
        validRequest = new FollowingRequest(currentUser.getAlias(), 3, null);
        invalidRequest = new FollowingRequest(null, 0, null);

        // Setup a mock ServerFacade that will return known responses
        successResponse = new FollowingResponse(Arrays.asList(resultUser1, resultUser2, resultUser3), false);
        ServerFacade mockServerFacade = Mockito.mock(ServerFacade.class);
        Mockito.when(mockServerFacade.getFollowees(validRequest)).thenReturn(successResponse);

        failureResponse = new FollowingResponse("An exception occurred");
        Mockito.when(mockServerFacade.getFollowees(invalidRequest)).thenReturn(failureResponse);

        // Setup an observer for the FollowingService
        observer = new FollowingServiceObserver();
        resetCountdownLatch();

        // Create a FollowingService instance and wrap it with a spy that will use the mock server facade
        FollowingService followingService = new FollowingService(observer);
        followingServiceSpy = Mockito.spy(followingService);
        Mockito.when(followingServiceSpy.getServerFacade()).thenReturn(mockServerFacade);
    }

    private void resetCountdownLatch() {
        countDownLatch = new CountDownLatch(1);
    }

    /**
     * A {@link FollowingService.Observer} implementation that can be used to get the value eventually
     * returned by an asynchronous call on the {@link FollowingService}. Counts down on the
     * countDownLatch so tests can wait for the background thread to call a method on the observer.
     */
    private class FollowingServiceObserver implements FollowingService.Observer {

        private FollowingResponse response;
        private Exception exception;

        @Override
        public void followeesRetrieved(FollowingResponse response) {
            this.response = response;
            countDownLatch.countDown();
        }

        @Override
        public void handleException(Exception exception) {
            this.exception = exception;
            countDownLatch.countDown();
        }

        public FollowingResponse getResponse() {
            return response;
        }

        public Exception getException() {
            return exception;
        }
    }

    /**
     * Verify that when the {@link FollowingService#getFollowees(FollowingRequest)} is called with
     * a valid request, a valid success response is returned.
     */
    @Test
    public void testGetFollowees_validRequest_passesCorrectResponseToObserver() throws InterruptedException {
        followingServiceSpy.getFollowees(validRequest);

        // Wait for the background thread to finish and invoke a method on the observer
        countDownLatch.await();
        resetCountdownLatch();

        Assert.assertEquals(successResponse, observer.getResponse());
        Assert.assertNull(observer.getException());
    }

    /**
     * Verify that for successful requests, the profile image of each user is included in the result.
     */
    @Test
    public void testGetFollowees_validRequest_loadsProfileImages() throws InterruptedException {
        followingServiceSpy.getFollowees(validRequest);

        countDownLatch.await();
        resetCountdownLatch();

        for(User user : observer.getResponse().getFollowees()) {
            Assert.assertNotNull(user.getImageBytes());
        }
    }

    /**
     * Verify that for unsuccessful requests, the {@link FollowingService} returns the same result
     * as the {@link ServerFacade}.
     */
    @Test
    public void testGetFollowees_invalidRequest_returnsNoFollowees() throws InterruptedException {
        followingServiceSpy.getFollowees(invalidRequest);

        countDownLatch.await();
        resetCountdownLatch();

        Assert.assertEquals(failureResponse, observer.getResponse());
        Assert.assertNull(observer.getException());
    }

    /**
     * Verify that when an IOException occurs while loading an image, the exception is passed to
     * the observer.
     */
    @Test
    public void testGetFollowees_exceptionThrownLoadingImages_observerReceivesException() throws IOException, InterruptedException {
        // Mock the task to make it throw an exception when it attempts to load an image
        FollowingService.RetrieveFollowingTask retrieveFollowingTask = followingServiceSpy.getRetrieveFollowingTask(validRequest);
        FollowingService.RetrieveFollowingTask retrieveFollowingTaskSpy = Mockito.spy(retrieveFollowingTask);

        Mockito.when(followingServiceSpy.getRetrieveFollowingTask(validRequest)).thenReturn(retrieveFollowingTaskSpy);

        IOException exception = new IOException();
        Mockito.doThrow(exception).when(retrieveFollowingTaskSpy).loadImages(Mockito.any());

        followingServiceSpy.getFollowees(validRequest);

        countDownLatch.await();
        resetCountdownLatch();

        Assert.assertEquals(exception, observer.getException());
        Assert.assertNull(observer.getResponse());
    }
}
