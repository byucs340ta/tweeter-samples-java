package edu.byu.cs.tweeter.client.presenter;

import android.util.Log;

import edu.byu.cs.tweeter.client.model.service.LoginServiceProxy;
import edu.byu.cs.tweeter.model.domain.AuthToken;
import edu.byu.cs.tweeter.model.domain.User;
import edu.byu.cs.tweeter.model.service.request.LoginRequest;
import edu.byu.cs.tweeter.model.service.response.LoginResponse;

/**
 * The presenter for the login functionality of the application.
 */
public class LoginPresenter implements LoginServiceProxy.Observer {

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
        LoginRequest loginRequest = new LoginRequest(username, password);
        LoginServiceProxy loginService = new LoginServiceProxy(this);
        loginService.login(loginRequest);
    }

    /**
     * Invoked when the login request completes if the login was successful. Notifies the view of
     * the successful login.
     *
     * @param loginResponse the response.
     */
    @Override
    public void loginSuccessful(LoginResponse loginResponse) {
        view.loginSuccessful(loginResponse.getUser(), loginResponse.getAuthToken());
    }

    /**
     * Invoked when the login request completes if the login request was unsuccessful. Notifies the
     * view of the unsuccessful login.
     *
     * @param loginResponse the response.
     */
    @Override
    public void loginUnsuccessful(LoginResponse loginResponse) {
        view.loginUnsuccessful("Failed to login. " + loginResponse.getMessage());
    }

    /**
     * A callback indicating that an exception occurred in an asynchronous method this class is
     * observing.
     *
     * @param exception the exception.
     */
    @Override
    public void handleException(Exception exception) {
        Log.e(LOG_TAG, exception.getMessage(), exception);
        view.loginUnsuccessful("Failed to login because of exception: " + exception.getMessage());
    }
}
