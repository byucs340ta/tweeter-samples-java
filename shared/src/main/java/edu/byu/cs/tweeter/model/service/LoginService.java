package edu.byu.cs.tweeter.model.service;

import edu.byu.cs.tweeter.model.service.request.LoginRequest;
import edu.byu.cs.tweeter.model.service.response.LoginResponse;

public interface LoginService {

    LoginResponse login(LoginRequest request);
}
