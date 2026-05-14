package com.shop;

import software.amazon.awscdk.App;
import software.amazon.awscdk.StackProps;

public class ProductServiceApp {
    public static void main(final String[] args) {
        App app = new App();

        new ProductServiceStack(app, "ProductServiceStack", StackProps.builder().build());

        app.synth();
    }
}
