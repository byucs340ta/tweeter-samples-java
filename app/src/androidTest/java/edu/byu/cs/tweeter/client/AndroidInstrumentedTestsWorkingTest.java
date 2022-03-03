package edu.byu.cs.tweeter.client;

import android.content.Context;

import androidx.lifecycle.Lifecycle;
import androidx.test.core.app.ActivityScenario;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import de.mannodermaus.junit5.ActivityScenarioExtension;
import edu.byu.cs.tweeter.client.view.login.LoginActivity;
import edu.byu.cs.tweeter.client.view.main.MainActivity;

public class AndroidInstrumentedTestsWorkingTest {

    @RegisterExtension
    ActivityScenarioExtension<LoginActivity> scenarioExtension = ActivityScenarioExtension.launch(LoginActivity.class);

    @BeforeEach
    public void setup() {
        // Called before each test, set up any common code between tests
        System.out.println("BeforeEach Called");
    }

    @Test
    public void useAppContext() {
        ActivityScenario<LoginActivity> scenario = scenarioExtension.getScenario();
        scenario.moveToState(Lifecycle.State.RESUMED);
        // Context of the app under test.
        Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        Assertions.assertEquals("edu.byu.cs.tweeter", appContext.getPackageName());
    }
    @Test
    public void failTest(){
        Assertions.assertEquals(true, false);
    }


}
