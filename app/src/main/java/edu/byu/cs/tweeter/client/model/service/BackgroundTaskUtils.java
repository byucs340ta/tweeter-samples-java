package edu.byu.cs.tweeter.client.model.service;

import android.util.Log;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import edu.byu.cs.tweeter.client.util.ByteArrayUtils;
import edu.byu.cs.tweeter.model.domain.User;

/**
 * BackgroundTaskUtils contains utility methods needed by background tasks.
 */
public class BackgroundTaskUtils {

    private static final String LOG_TAG = "BackgroundTaskUtils";

    public static void runTask(Runnable task) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(task);
    }

}
