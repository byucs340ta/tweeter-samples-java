package edu.byu.cs.tweeter.client.model.service;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import edu.byu.cs.tweeter.model.domain.User;
import edu.byu.cs.tweeter.client.model.net.ServerFacade;
import edu.byu.cs.tweeter.model.service.request.FollowingRequest;
import edu.byu.cs.tweeter.model.service.response.FollowingResponse;

public class FollowingServiceProxyTest {

    private FollowingRequest validRequest;
    private FollowingRequest invalidRequest;

    private FollowingResponse successResponse;
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

        // Setup request objects to use in the tests
        validRequest = new FollowingRequest(currentUser.getAlias(), 3, null);
        invalidRequest = new FollowingRequest(null, 0, null);

        // Setup success and failure responses to be used in the tests
        successResponse = new FollowingResponse(Arrays.asList(resultUser1, resultUser2, resultUser3), false);
        failureResponse = new FollowingResponse("An exception occurred");

        // Setup an observer for the FollowingServiceProxy
        observer = new FollowingServiceObserver();

        // Prepare the countdown latch
        resetCountdownLatch();
    }

    private void resetCountdownLatch() {
        countDownLatch = new CountDownLatch(1);
    }

    /**
     * A {@link FollowingServiceProxy.Observer} implementation that can be used to get the value
     * eventually returned by an asynchronous call on the {@link FollowingServiceProxy}. Counts down
     * on the countDownLatch so tests can wait for the background thread to call a method on the
     * observer.
     */
    private class FollowingServiceObserver implements FollowingServiceProxy.Observer {

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

    private FollowingServiceProxy setupFollowingServiceProxySpy(FollowingResponse serverFacadeResponse) {
        ServerFacade mockServerFacade = Mockito.mock(ServerFacade.class);
        try {
            Mockito.when(mockServerFacade.getFollowees(Mockito.any(), Mockito.any())).thenReturn(serverFacadeResponse);
        } catch (Exception e) {
            // We won't actually get an exception while setting up the mock
        }

        FollowingServiceProxy followingServiceProxy = new FollowingServiceProxy(observer);
        FollowingServiceProxy followingServiceProxySpy = Mockito.spy(followingServiceProxy);
        Mockito.when(followingServiceProxySpy.getServerFacade()).thenReturn(mockServerFacade);

        return followingServiceProxySpy;
    }

    /**
     * Verify that for successful requests, the {@link FollowingServiceProxy#getFollowees(FollowingRequest)}
     * asynchronous method eventually returns the same result as the {@link ServerFacade}.
     */
    @Test
    public void testGetFollowees_validRequest_correctResponse() throws InterruptedException {
        FollowingServiceProxy followingServiceProxySpy = setupFollowingServiceProxySpy(successResponse);

        followingServiceProxySpy.getFollowees(validRequest);
        countDownLatch.await();

        Assert.assertEquals(successResponse, observer.getResponse());
    }

    /**
     * Verify that for successful requests, the the {@link FollowingServiceProxy#getFollowees(FollowingRequest)}
     * method loads the profile image of each user included in the result.
     */
    @Test
    public void testGetFollowees_validRequest_loadsProfileImages() throws InterruptedException {
        FollowingServiceProxy followingServiceProxySpy = setupFollowingServiceProxySpy(successResponse);

        followingServiceProxySpy.getFollowees(validRequest);
        countDownLatch.await();

        List<User> followees = observer.getResponse().getFollowees();
        Assert.assertTrue(followees.size() > 0);

        for(User user : followees) {
            Assert.assertNotNull(user.getImageBytes());
        }
    }

    /**
     * Verify that for unsuccessful requests, the the {@link FollowingServiceProxy#getFollowees(FollowingRequest)}
     * method returns the same failure response as the server facade.
     */
    @Test
    public void testGetFollowees_invalidRequest_returnsNoFollowees() throws InterruptedException {
        FollowingServiceProxy followingServiceProxySpy = setupFollowingServiceProxySpy(failureResponse);

        followingServiceProxySpy.getFollowees(invalidRequest);
        countDownLatch.await();

        Assert.assertEquals(failureResponse, observer.getResponse());
    }

    /**
     * Verify that when an IOException occurs while loading an image, the {@link FollowingServiceProxy.Observer#handleException(Exception)}
     * method of the Service's observer is called and the exception is passed to it.
     */
    @Test
    public void testGetFollowees_exceptionThrownLoadingImages_observerHandleExceptionMethodCalled() throws IOException, InterruptedException {
        FollowingServiceProxy followingServiceProxySpy = setupFollowingServiceProxySpy(successResponse);

        // Create a task spy for the FollowingServiceProxy that throws an exception when it's loadImages method is called
        FollowingServiceProxy.RetrieveFollowingTask retrieveFollowingTask = followingServiceProxySpy.getRetrieveFollowingTask(validRequest);
        FollowingServiceProxy.RetrieveFollowingTask retrieveFollowingTaskSpy = Mockito.spy(retrieveFollowingTask);

        IOException exception = new IOException();
        Mockito.doThrow(exception).when(retrieveFollowingTaskSpy).loadImages(Mockito.any());

        // Make the FollowingServiceProxy spy use the RetrieveFollowingTask spy
        Mockito.when(followingServiceProxySpy.getRetrieveFollowingTask(Mockito.any())).thenReturn(retrieveFollowingTaskSpy);

        followingServiceProxySpy.getFollowees(validRequest);
        countDownLatch.await();

        Assert.assertEquals(exception, observer.getException());
    }
}
