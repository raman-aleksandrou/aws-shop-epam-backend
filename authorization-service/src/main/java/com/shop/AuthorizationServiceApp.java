package com.shop;

import software.amazon.awscdk.App;
import software.amazon.awscdk.StackProps;

public class AuthorizationServiceApp {
    public static void main(final String[] args) {
        App app = new App();

        new AuthorizationServiceStack(app, "AuthorizationServiceStack", StackProps.builder().build());

        app.synth();
    }
}
