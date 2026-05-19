package com.shop;

import software.amazon.awscdk.Duration;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.apigateway.LambdaIntegration;
import software.amazon.awscdk.services.apigateway.MethodOptions;
import software.amazon.awscdk.services.apigateway.RequestValidator;
import software.amazon.awscdk.services.apigateway.RequestValidatorOptions;
import software.amazon.awscdk.services.apigateway.RestApi;
import software.amazon.awscdk.services.iam.PolicyStatement;
import software.amazon.awscdk.services.lambda.Code;
import software.amazon.awscdk.services.lambda.Function;
import software.amazon.awscdk.services.lambda.Runtime;
import software.amazon.awscdk.services.s3.Bucket;
import software.amazon.awscdk.services.s3.IBucket;
import software.amazon.awscdk.services.sqs.IQueue;
import software.amazon.awscdk.services.sqs.Queue;
import software.constructs.Construct;

import java.util.List;
import java.util.Map;

public class ImportServiceStack extends Stack {

    private static final String BUCKET_NAME = "aws-shop-epam-import-service";
    private static final String UPLOADED_FOLDER = "uploaded";

    public ImportServiceStack(final Construct scope, final String id, final StackProps props) {
        super(scope, id, props);

        // Bucket was created manually in AWS Console (task 5.1), import it by name
        IBucket importBucket = Bucket.fromBucketName(this, "ImportBucket", BUCKET_NAME);

        Function importProductsFile = Function.Builder.create(this, "ImportProductsFileFunction")
            .functionName("importProductsFile")
            .runtime(Runtime.JAVA_17)
            .handler("com.shop.handlers.ImportProductsFileHandler::handleRequest")
            .code(Code.fromAsset("target/import-service.jar"))
            .memorySize(512)
            .timeout(Duration.seconds(15))
            .environment(Map.of(
                "BUCKET_NAME", BUCKET_NAME,
                "UPLOADED_FOLDER", UPLOADED_FOLDER
            ))
            .build();

        importProductsFile.addToRolePolicy(PolicyStatement.Builder.create()
            .actions(List.of("s3:PutObject"))
            .resources(List.of(importBucket.arnForObjects(UPLOADED_FOLDER + "/*")))
            .build());

        IQueue catalogItemsQueue = Queue.fromQueueArn(this, "CatalogItemsQueue",
            String.format("arn:aws:sqs:%s:%s:catalogItemsQueue", getRegion(), getAccount()));

        Function importFileParser = Function.Builder.create(this, "ImportFileParserFunction")
            .functionName("importFileParser")
            .runtime(Runtime.JAVA_17)
            .handler("com.shop.handlers.ImportFileParserHandler::handleRequest")
            .code(Code.fromAsset("target/import-service.jar"))
            .memorySize(512)
            .timeout(Duration.seconds(30))
            .environment(Map.of(
                "BUCKET_NAME", BUCKET_NAME,
                "CATALOG_ITEMS_QUEUE_URL", catalogItemsQueue.getQueueUrl()
            ))
            .build();

        catalogItemsQueue.grantSendMessages(importFileParser);

        importFileParser.addToRolePolicy(PolicyStatement.Builder.create()
            .actions(List.of("s3:GetObject", "s3:DeleteObject"))
            .resources(List.of(importBucket.arnForObjects(UPLOADED_FOLDER + "/*")))
            .build());

        importFileParser.addToRolePolicy(PolicyStatement.Builder.create()
            .actions(List.of("s3:PutObject"))
            .resources(List.of(importBucket.arnForObjects("parsed/*")))
            .build());

        RestApi api = RestApi.Builder.create(this, "ImportServiceApi")
            .restApiName("Import Service API")
            .description("Import Service REST API")
            .build();

        RequestValidator requestValidator = api.addRequestValidator("ImportRequestValidator",
            RequestValidatorOptions.builder()
                .requestValidatorName("Validate query params")
                .validateRequestParameters(true)
                .build());

        var importResource = api.getRoot().addResource("import");
        importResource.addMethod("GET", new LambdaIntegration(importProductsFile),
            MethodOptions.builder()
                .requestParameters(Map.of("method.request.querystring.name", true))
                .requestValidator(requestValidator)
                .build());
    }
}
