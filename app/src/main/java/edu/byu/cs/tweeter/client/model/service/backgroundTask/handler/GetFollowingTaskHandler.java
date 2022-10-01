package edu.byu.cs.tweeter.client.model.service.backgroundTask.handler;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import java.util.List;

import edu.byu.cs.tweeter.client.model.service.FollowService;
import edu.byu.cs.tweeter.client.model.service.backgroundTask.GetFollowingTask;
import edu.byu.cs.tweeter.model.domain.User;

/**
 * Handles messages from the background task indicating that the task is done, by invoking
 * methods on the observer.
 */
public class GetFollowingTaskHandler extends Handler {

    private final FollowService.GetFollowingObserver observer;

    public GetFollowingTaskHandler(FollowService.GetFollowingObserver observer) {
        super(Looper.getMainLooper());
        this.observer = observer;
    }

    @Override
    public void handleMessage(Message message) {
        Bundle bundle = message.getData();
        boolean success = bundle.getBoolean(GetFollowingTask.SUCCESS_KEY);
        if (success) {
            List<User> followees = (List<User>) bundle.getSerializable(GetFollowingTask.FOLLOWEES_KEY);
            boolean hasMorePages = bundle.getBoolean(GetFollowingTask.MORE_PAGES_KEY);
            observer.handleSuccess(followees, hasMorePages);
        } else if (bundle.containsKey(GetFollowingTask.MESSAGE_KEY)) {
            String errorMessage = bundle.getString(GetFollowingTask.MESSAGE_KEY);
            observer.handleFailure(errorMessage);
        } else if (bundle.containsKey(GetFollowingTask.EXCEPTION_KEY)) {
            Exception ex = (Exception) bundle.getSerializable(GetFollowingTask.EXCEPTION_KEY);
            observer.handleException(ex);
        }
    }
}
