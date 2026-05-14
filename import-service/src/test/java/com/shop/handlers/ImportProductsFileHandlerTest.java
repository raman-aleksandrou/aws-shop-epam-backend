package com.shop.handlers;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ImportProductsFileHandlerTest {

    private ImportProductsFileHandler handler;
    private Context context;

    @BeforeEach
    void setUp() {
        handler = new ImportProductsFileHandler();
        context = mock(Context.class);
        when(context.getLogger()).thenReturn(mock(LambdaLogger.class));
    }

    @Test
    void returnsBadRequestWhenNameParamMissing() {
        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent();
        request.setQueryStringParameters(null);

        APIGatewayProxyResponseEvent response = handler.handleRequest(request, context);

        assertEquals(400, response.getStatusCode());
    }

    @Test
    void returnsBadRequestWhenNameParamBlank() {
        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent();
        request.setQueryStringParameters(Map.of("name", "  "));

        APIGatewayProxyResponseEvent response = handler.handleRequest(request, context);

        assertEquals(400, response.getStatusCode());
    }

    @Test
    void returnsSignedUrlWhenNameParamProvided() throws MalformedURLException {
        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent();
        request.setQueryStringParameters(Map.of("name", "products.csv"));

        URL fakeUrl = new URL("https://s3.amazonaws.com/bucket/uploaded/products.csv?signed=token");

        PresignedPutObjectRequest presignedRequest = mock(PresignedPutObjectRequest.class);
        when(presignedRequest.url()).thenReturn(fakeUrl);

        S3Presigner presigner = mock(S3Presigner.class);
        when(presigner.presignPutObject(any(PutObjectPresignRequest.class))).thenReturn(presignedRequest);

        try (MockedStatic<S3Presigner> presignerStatic = mockStatic(S3Presigner.class)) {
            presignerStatic.when(S3Presigner::create).thenReturn(presigner);

            APIGatewayProxyResponseEvent response = handler.handleRequest(request, context);

            assertEquals(200, response.getStatusCode());
            assertTrue(response.getBody().contains("s3.amazonaws.com"));
        }
    }
}
