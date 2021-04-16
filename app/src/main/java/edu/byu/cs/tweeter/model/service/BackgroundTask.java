package edu.byu.cs.tweeter.model.service;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

import java.io.Serializable;

public abstract class BackgroundTask implements Runnable {

    private final Handler messageHandler;

    /**
     * Creates an instance.
     *
     * @param messageHandler the messageHandler that handles the result of this task.
     */
    public BackgroundTask(Handler messageHandler) {
        this.messageHandler = messageHandler;
    }

    /**
     * Sends a message to the message handler containing the specified value.
     *
     * @param key the key of the value to be sent in the message.
     * @param value the value to be sent in the message.
     */
    protected void sendMessage(String key, Serializable value) {
        Message message = Message.obtain();
        Bundle messageBundle = new Bundle();
        messageBundle.putSerializable(key, value);
        message.setData(messageBundle);
        messageHandler.sendMessage(message);
    }
}
