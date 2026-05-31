package com.shop.handlers;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.APIGatewayCustomAuthorizerEvent;
import com.amazonaws.services.lambda.runtime.events.IamPolicyResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Base64;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class BasicAuthorizerHandlerTest {

    private BasicAuthorizerHandler handler;
    private Context context;
    private static final String METHOD_ARN = "arn:aws:execute-api:eu-central-1:123456789:api/prod/GET/import";

    @BeforeEach
    void setUp() {
        handler = new BasicAuthorizerHandler();
        context = mock(Context.class);
    }

    private APIGatewayCustomAuthorizerEvent buildEvent(String token) {
        APIGatewayCustomAuthorizerEvent event = new APIGatewayCustomAuthorizerEvent();
        event.setAuthorizationToken(token);
        event.setMethodArn(METHOD_ARN);
        return event;
    }

    private String basicToken(String username, String password) {
        return "Basic " + Base64.getEncoder().encodeToString((username + ":" + password).getBytes());
    }

    @SuppressWarnings("unchecked")
    private String getEffect(IamPolicyResponse response) {
        Map<String, Object>[] statements = (Map<String, Object>[]) response.getPolicyDocument().get("Statement");
        return (String) statements[0].get("Effect");
    }

    @Test
    void returns401WhenTokenIsAbsent() {
        RuntimeException ex = assertThrows(RuntimeException.class,
            () -> handler.handleRequest(buildEvent(null), context));
        assertEquals("Unauthorized", ex.getMessage());
    }

    @Test
    void returns401WhenTokenIsBlank() {
        RuntimeException ex = assertThrows(RuntimeException.class,
            () -> handler.handleRequest(buildEvent("   "), context));
        assertEquals("Unauthorized", ex.getMessage());
    }

    @Test
    void returns403WhenBase64IsInvalid() {
        IamPolicyResponse response = handler.handleRequest(buildEvent("Basic !!!invalid!!!"), context);
        assertEquals(IamPolicyResponse.DENY, getEffect(response));
    }

    @Test
    void returns403WhenTokenHasNoColon() {
        String noColon = "Basic " + Base64.getEncoder().encodeToString("usernameonly".getBytes());
        IamPolicyResponse response = handler.handleRequest(buildEvent(noColon), context);
        assertEquals(IamPolicyResponse.DENY, getEffect(response));
    }

    @Test
    void returns403WhenCredentialsAreWrong() {
        IamPolicyResponse response = handler.handleRequest(buildEvent(basicToken("unknownUser", "wrongPass")), context);
        assertEquals("unknownUser", response.getPrincipalId());
        assertEquals(IamPolicyResponse.DENY, getEffect(response));
    }
}
