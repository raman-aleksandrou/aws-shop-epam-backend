package com.shop.handlers;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayCustomAuthorizerEvent;
import com.amazonaws.services.lambda.runtime.events.IamPolicyResponse;

import java.util.Base64;
import java.util.List;

public class BasicAuthorizerHandler implements RequestHandler<APIGatewayCustomAuthorizerEvent, IamPolicyResponse> {

    @Override
    public IamPolicyResponse handleRequest(APIGatewayCustomAuthorizerEvent event, Context context) {
        System.out.println("BasicAuthorizer invoked, methodArn=" + event.getMethodArn());

        String token = event.getAuthorizationToken();
        if (token == null || token.isBlank()) {
            System.out.println("No authorization token provided → 401");
            throw new RuntimeException("Unauthorized");
        }

        String encoded = token.startsWith("Basic ") ? token.substring(6) : token;

        String decoded;
        try {
            decoded = new String(Base64.getDecoder().decode(encoded));
        } catch (IllegalArgumentException e) {
            System.out.println("Invalid Base64 token → 403");
            return denyResponse("unknown", event.getMethodArn());
        }

        int colonIndex = decoded.indexOf(':');
        if (colonIndex < 0) {
            System.out.println("Token has no ':' separator → 403");
            return denyResponse("unknown", event.getMethodArn());
        }

        String username = decoded.substring(0, colonIndex);
        String password = decoded.substring(colonIndex + 1);
        System.out.println("Authorizing user: " + username);

        String envKey = username.replaceAll("[^a-zA-Z0-9_]", "_");
        String storedPassword = System.getenv(envKey);
        if (storedPassword != null && storedPassword.equals(password)) {
            System.out.println("Access granted for: " + username);
            return buildResponse(username, IamPolicyResponse.allowStatement(event.getMethodArn()));
        } else {
            System.out.println("Access denied for: " + username + " → 403");
            return denyResponse(username, event.getMethodArn());
        }
    }

    private IamPolicyResponse denyResponse(String principalId, String methodArn) {
        return buildResponse(principalId, IamPolicyResponse.denyStatement(methodArn));
    }

    private IamPolicyResponse buildResponse(String principalId, IamPolicyResponse.Statement statement) {
        return IamPolicyResponse.builder()
            .withPrincipalId(principalId)
            .withPolicyDocument(IamPolicyResponse.PolicyDocument.builder()
                .withStatement(List.of(statement))
                .build())
            .build();
    }
}
