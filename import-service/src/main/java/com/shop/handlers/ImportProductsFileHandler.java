package com.shop.handlers;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.time.Duration;
import java.util.Map;

public class ImportProductsFileHandler
        implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private static final Map<String, String> HEADERS = Map.of(
        "Content-Type", "text/plain",
        "Access-Control-Allow-Origin", "*"
    );

    private final String bucketName = System.getenv("BUCKET_NAME");
    private final String uploadedFolder = System.getenv("UPLOADED_FOLDER");

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent request, Context context) {
        context.getLogger().log("Incoming request: GET /import, params: " + request.getQueryStringParameters());

        try {
            Map<String, String> params = request.getQueryStringParameters();
            if (params == null || !params.containsKey("name") || params.get("name").isBlank()) {
                return new APIGatewayProxyResponseEvent()
                    .withStatusCode(400)
                    .withHeaders(HEADERS)
                    .withBody("{\"message\":\"Query parameter 'name' is required\"}");
            }

            String fileName = params.get("name");
            String objectKey = uploadedFolder + "/" + fileName;

            try (S3Presigner presigner = S3Presigner.create()) {
                PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(objectKey)
                    .contentType("text/csv")
                    .build();

                PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                    .signatureDuration(Duration.ofMinutes(5))
                    .putObjectRequest(putObjectRequest)
                    .build();

                PresignedPutObjectRequest presigned = presigner.presignPutObject(presignRequest);
                String signedUrl = presigned.url().toString();

                context.getLogger().log("Generated pre-signed URL for key: " + objectKey);

                return new APIGatewayProxyResponseEvent()
                    .withStatusCode(200)
                    .withHeaders(HEADERS)
                    .withBody(signedUrl);
            }
        } catch (Exception e) {
            context.getLogger().log("ERROR importProductsFile: " + e.getMessage());
            return new APIGatewayProxyResponseEvent()
                .withStatusCode(500)
                .withHeaders(HEADERS)
                .withBody("{\"message\":\"Internal Server Error\"}");
        }
    }
}
