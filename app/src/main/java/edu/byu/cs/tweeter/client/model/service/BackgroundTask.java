package edu.byu.cs.tweeter.client.model.service;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

public abstract class BackgroundTask implements Runnable {

    protected interface BundleLoader {
        void load(Bundle msgBundle);
    }

    private static final String LOG_TAG = "Task";

    public static final String SUCCESS_KEY = "success";
    public static final String MESSAGE_KEY = "message";
    public static final String EXCEPTION_KEY = "exception";

    protected final Handler messageHandler;

    protected BackgroundTask(Handler messageHandler) {
        this.messageHandler = messageHandler;
    }

    @Override
    public void run() {
        try {
            runTask();
        } catch (Exception ex) {
            Log.e(LOG_TAG, ex.getMessage(), ex);
            sendExceptionMessage(ex);
        }
    }

    // This method is public instead of protected to make it accessible to test cases
    public void sendSuccessMessage() {
        Bundle msgBundle = createSuccessBundle();
        sendMessage(msgBundle);
    }

    // This method is public instead of protected to make it accessible to test cases
    public void sendSuccessMessage(BundleLoader bundleLoader) {
        Bundle msgBundle = createSuccessBundle();
        bundleLoader.load(msgBundle);
        sendMessage(msgBundle);
    }

    // This method is public instead of protected to make it accessible to test cases
    public void sendFailedMessage(String message) {
        Bundle msgBundle = createFailedBundle();
        msgBundle.putString(MESSAGE_KEY, message);
        sendMessage(msgBundle);
    }

    // This method is public instead of protected to make it accessible to test cases
    public void sendExceptionMessage(Exception exception) {
        Bundle msgBundle = createFailedBundle();
        msgBundle.putSerializable(EXCEPTION_KEY, exception);
        sendMessage(msgBundle);
    }

    private void sendMessage(Bundle msgBundle) {
        Message msg = Message.obtain();
        msg.setData(msgBundle);

        messageHandler.sendMessage(msg);
    }

    private Bundle createSuccessBundle() {
        Bundle msgBundle = new Bundle();
        msgBundle.putBoolean(SUCCESS_KEY, true);
        return msgBundle;
    }

    private Bundle createFailedBundle() {
        Bundle msgBundle = new Bundle();
        msgBundle.putBoolean(SUCCESS_KEY, false);
        return msgBundle;
    }

    protected abstract void runTask();
}
