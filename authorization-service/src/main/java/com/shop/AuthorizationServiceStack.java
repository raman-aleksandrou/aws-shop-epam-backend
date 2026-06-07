package com.shop;

import software.amazon.awscdk.CfnOutput;
import software.amazon.awscdk.Duration;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.lambda.Code;
import software.amazon.awscdk.services.lambda.Function;
import software.amazon.awscdk.services.lambda.Runtime;
import software.constructs.Construct;

import java.util.Map;

public class AuthorizationServiceStack extends Stack {

    public AuthorizationServiceStack(final Construct scope, final String id, final StackProps props) {
        super(scope, id, props);

        // Credentials are stored as env vars: key = GitHub username, value = password
        String githubUsername = System.getenv().getOrDefault("GITHUB_USERNAME", "raman-aleksandrou");
        String githubPassword = System.getenv().getOrDefault("GITHUB_PASSWORD", "");
        // Lambda env var keys must match [a-zA-Z][a-zA-Z0-9_]+ — replace hyphens with underscores
        String envKey = githubUsername.replaceAll("[^a-zA-Z0-9_]", "_");

        Function basicAuthorizer = Function.Builder.create(this, "BasicAuthorizerFunction")
            .functionName("basicAuthorizer")
            .runtime(Runtime.JAVA_17)
            .handler("com.shop.handlers.BasicAuthorizerHandler::handleRequest")
            .code(Code.fromAsset("target/authorization-service.jar"))
            .memorySize(512)
            .timeout(Duration.seconds(10))
            .environment(Map.of(envKey, githubPassword))
            .build();

        CfnOutput.Builder.create(this, "BasicAuthorizerArn")
            .exportName("BasicAuthorizerArn")
            .value(basicAuthorizer.getFunctionArn())
            .build();
    }
}
