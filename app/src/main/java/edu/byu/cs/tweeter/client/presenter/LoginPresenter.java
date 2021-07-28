package edu.byu.cs.tweeter.client.presenter;

import android.util.Log;

import edu.byu.cs.tweeter.client.cache.Cache;
import edu.byu.cs.tweeter.client.model.service.LoginService;
import edu.byu.cs.tweeter.model.domain.AuthToken;
import edu.byu.cs.tweeter.model.domain.User;

/**
 * The presenter for the login functionality of the application.
 */
public class LoginPresenter implements LoginService.Observer {

    private static final String LOG_TAG = "LoginPresenter";

    private final View view;

    /**
     * The interface by which this presenter communicates with it's view.
     */
    public interface View {
        void loginSuccessful(User user, AuthToken authToken);
        void loginUnsuccessful(String message);
    }

    /**
     * Creates an instance.
     *
     * @param view the view for which this class is the presenter.
     */
    public LoginPresenter(View view) {
        // An assertion would be better, but Android doesn't support Java assertions
        if(view == null) {
            throw new NullPointerException();
        }
        this.view = view;
    }

    /**
     * Initiates the login process.
     *
     * @param username the user's username.
     * @param password the user's password.
     */
    public void initiateLogin(String username, String password) {
        LoginService loginService = new LoginService(this);
        loginService.login(username, password);
    }

    /**
     * Invoked when the login request completes if the login was successful. Notifies the view of
     * the successful login.
     *
     * @param user the logged-in user.
     * @param authToken the session auth token.
     */
    @Override
    public void loginSuccessful(User user, AuthToken authToken) {
        // Cache user session information
        Cache.getInstance().setCurrUser(user);
        Cache.getInstance().setCurrUserAuthToken(authToken);

        view.loginSuccessful(user, authToken);
    }

    /**
     * Invoked when the login request completes if the login request was unsuccessful. Notifies the
     * view of the unsuccessful login.
     *
     * @param message error message.
     */
    @Override
    public void loginUnsuccessful(String message) {
        String errorMessage = "Failed to login: " + message;
        Log.e(LOG_TAG, errorMessage);
        view.loginUnsuccessful(errorMessage);
    }

    /**
     * A callback indicating that an exception occurred in an asynchronous method this class is
     * observing.
     *
     * @param exception the exception.
     */
    @Override
    public void handleException(Exception exception) {
        String errorMessage = "Failed to login because of exception: " + exception.getMessage();
        Log.e(LOG_TAG, errorMessage, exception);
        view.loginUnsuccessful(errorMessage);
    }
}
