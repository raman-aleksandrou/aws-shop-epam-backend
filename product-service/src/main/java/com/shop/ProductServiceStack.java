package com.shop;

import software.amazon.awscdk.CfnOutput;
import software.amazon.awscdk.Duration;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.apigateway.AuthorizationType;
import software.amazon.awscdk.services.apigateway.CognitoUserPoolsAuthorizer;
import software.amazon.awscdk.services.apigateway.CorsOptions;
import software.amazon.awscdk.services.apigateway.GatewayResponseOptions;
import software.amazon.awscdk.services.apigateway.LambdaIntegration;
import software.amazon.awscdk.services.apigateway.MethodOptions;
import software.amazon.awscdk.services.apigateway.ResponseType;
import software.amazon.awscdk.services.apigateway.RestApi;
import software.amazon.awscdk.services.cognito.AuthFlow;
import software.amazon.awscdk.services.cognito.AutoVerifiedAttrs;
import software.amazon.awscdk.services.cognito.CognitoDomainOptions;
import software.amazon.awscdk.services.cognito.OAuthFlows;
import software.amazon.awscdk.services.cognito.OAuthScope;
import software.amazon.awscdk.services.cognito.OAuthSettings;
import software.amazon.awscdk.services.cognito.PasswordPolicy;
import software.amazon.awscdk.services.cognito.SignInAliases;
import software.amazon.awscdk.services.cognito.StandardAttribute;
import software.amazon.awscdk.services.cognito.StandardAttributes;
import software.amazon.awscdk.services.cognito.UserPool;
import software.amazon.awscdk.services.cognito.UserPoolClientOptions;
import software.amazon.awscdk.services.cognito.UserPoolDomainOptions;
import software.amazon.awscdk.services.dynamodb.ITable;
import software.amazon.awscdk.services.dynamodb.Table;
import software.amazon.awscdk.services.lambda.Code;
import software.amazon.awscdk.services.lambda.Function;
import software.amazon.awscdk.services.lambda.Runtime;
import software.amazon.awscdk.services.lambda.eventsources.SqsEventSource;
import software.amazon.awscdk.services.sns.NumericConditions;
import software.amazon.awscdk.services.sns.SubscriptionFilter;
import software.amazon.awscdk.services.sns.Topic;
import software.amazon.awscdk.services.sns.subscriptions.EmailSubscription;
import software.amazon.awscdk.services.sns.subscriptions.EmailSubscriptionProps;
import software.amazon.awscdk.services.sqs.Queue;
import software.constructs.Construct;

import java.util.List;
import java.util.Map;

public class ProductServiceStack extends Stack {

    private static final String PRODUCTS_TABLE_NAME = "products";
    private static final String STOCKS_TABLE_NAME = "stocks";
    private static final String CLIENT_APP_URL = "https://d20yrfgj13ai1q.cloudfront.net/";

    public ProductServiceStack(final Construct scope, final String id, final StackProps props) {
        super(scope, id, props);

        ITable productsTable = Table.fromTableName(this, "ProductsTable", PRODUCTS_TABLE_NAME);
        ITable stocksTable = Table.fromTableName(this, "StocksTable", STOCKS_TABLE_NAME);

        Map<String, String> env = Map.of(
            "PRODUCTS_TABLE_NAME", PRODUCTS_TABLE_NAME,
            "STOCKS_TABLE_NAME", STOCKS_TABLE_NAME
        );

        Function getProductsList = Function.Builder.create(this, "GetProductsListFunction")
            .functionName("getProductsList")
            .runtime(Runtime.JAVA_17)
            .handler("com.shop.handlers.GetProductsListHandler::handleRequest")
            .code(Code.fromAsset("target/product-service.jar"))
            .memorySize(512)
            .timeout(Duration.seconds(15))
            .environment(env)
            .build();

        Function getProductsById = Function.Builder.create(this, "GetProductsByIdFunction")
            .functionName("getProductsById")
            .runtime(Runtime.JAVA_17)
            .handler("com.shop.handlers.GetProductsByIdHandler::handleRequest")
            .code(Code.fromAsset("target/product-service.jar"))
            .memorySize(512)
            .timeout(Duration.seconds(15))
            .environment(env)
            .build();

        Function createProduct = Function.Builder.create(this, "CreateProductFunction")
            .functionName("createProduct")
            .runtime(Runtime.JAVA_17)
            .handler("com.shop.handlers.CreateProductHandler::handleRequest")
            .code(Code.fromAsset("target/product-service.jar"))
            .memorySize(512)
            .timeout(Duration.seconds(15))
            .environment(env)
            .build();

        productsTable.grantReadData(getProductsList);
        stocksTable.grantReadData(getProductsList);
        productsTable.grantReadData(getProductsById);
        stocksTable.grantReadData(getProductsById);
        productsTable.grantWriteData(createProduct);
        stocksTable.grantWriteData(createProduct);

        Queue catalogItemsQueue = Queue.Builder.create(this, "CatalogItemsQueue")
            .queueName("catalogItemsQueue")
            .build();

        Topic createProductTopic = Topic.Builder.create(this, "CreateProductTopic")
            .topicName("createProductTopic")
            .build();

        // Premium products (price >= 100)
        createProductTopic.addSubscription(new EmailSubscription("raman.aleksandrou@gmail.com",
            EmailSubscriptionProps.builder()
                .filterPolicy(Map.of(
                    "price", SubscriptionFilter.numericFilter(NumericConditions.builder()
                        .greaterThanOrEqualTo(100)
                        .build())
                ))
                .build()));

        // Budget products (price < 100)
        createProductTopic.addSubscription(new EmailSubscription("roman.aleksandrov1@yandex.by",
            EmailSubscriptionProps.builder()
                .filterPolicy(Map.of(
                    "price", SubscriptionFilter.numericFilter(NumericConditions.builder()
                        .lessThan(100)
                        .build())
                ))
                .build()));

        Function catalogBatchProcess = Function.Builder.create(this, "CatalogBatchProcessFunction")
            .functionName("catalogBatchProcess")
            .runtime(Runtime.JAVA_17)
            .handler("com.shop.handlers.CatalogBatchProcessHandler::handleRequest")
            .code(Code.fromAsset("target/product-service.jar"))
            .memorySize(512)
            .timeout(Duration.seconds(15))
            .environment(Map.of(
                "PRODUCTS_TABLE_NAME", PRODUCTS_TABLE_NAME,
                "STOCKS_TABLE_NAME", STOCKS_TABLE_NAME,
                "SNS_TOPIC_ARN", createProductTopic.getTopicArn()
            ))
            .build();

        productsTable.grantWriteData(catalogBatchProcess);
        stocksTable.grantWriteData(catalogBatchProcess);
        createProductTopic.grantPublish(catalogBatchProcess);

        catalogBatchProcess.addEventSource(SqsEventSource.Builder.create(catalogItemsQueue)
            .batchSize(5)
            .build());

        // Cognito User Pool
        UserPool userPool = UserPool.Builder.create(this, "ShopUserPoolV2")
            .userPoolName("aws-shop-epam-user-pool")
            .selfSignUpEnabled(true)
            .signInAliases(SignInAliases.builder().username(true).email(true).build())
            .autoVerify(AutoVerifiedAttrs.builder().email(true).build())
            .standardAttributes(StandardAttributes.builder()
                .email(StandardAttribute.builder().required(false).mutable(true).build())
                .build())
            .passwordPolicy(PasswordPolicy.builder()
                .minLength(8)
                .requireLowercase(false)
                .requireUppercase(false)
                .requireDigits(false)
                .requireSymbols(false)
                .build())
            .build();

        var userPoolClient = userPool.addClient("ShopUserPoolClient",
            UserPoolClientOptions.builder()
                .authFlows(AuthFlow.builder()
                    .userPassword(true)
                    .userSrp(true)
                    .build())
                .oAuth(OAuthSettings.builder()
                    .flows(OAuthFlows.builder().authorizationCodeGrant(true).implicitCodeGrant(true).build())
                    .scopes(List.of(
                        OAuthScope.OPENID,
                        OAuthScope.EMAIL,
                        OAuthScope.PROFILE,
                        OAuthScope.PHONE,
                        OAuthScope.COGNITO_ADMIN
                    ))
                    .callbackUrls(List.of(CLIENT_APP_URL))
                    .build())
                .build());

        var userPoolDomain = userPool.addDomain("ShopUserPoolDomain",
            UserPoolDomainOptions.builder()
                .cognitoDomain(CognitoDomainOptions.builder()
                    .domainPrefix("shop-epam-raman")
                    .build())
                .build());

        CfnOutput.Builder.create(this, "CognitoHostedUiUrl")
            .description("Cognito Hosted UI login page")
            .value(userPoolDomain.baseUrl() + "/login?client_id=" +
                   userPoolClient.getUserPoolClientId() + "&response_type=token&scope=openid+email+profile&redirect_uri=" +
                   CLIENT_APP_URL)
            .build();

        CognitoUserPoolsAuthorizer cognitoAuthorizer = CognitoUserPoolsAuthorizer.Builder
            .create(this, "CognitoAuthorizer")
            .cognitoUserPools(List.of(userPool))
            .identitySource("method.request.header.Authorization")
            .build();

        RestApi api = RestApi.Builder.create(this, "ProductServiceApi")
            .restApiName("Product Service API")
            .description("Product Service REST API")
            .defaultCorsPreflightOptions(CorsOptions.builder()
                .allowOrigins(List.of("*"))
                .allowHeaders(List.of("Content-Type", "Authorization"))
                .allowMethods(List.of("GET", "POST", "OPTIONS"))
                .build())
            .build();

        api.addGatewayResponse("Unauthorized", GatewayResponseOptions.builder()
            .type(ResponseType.UNAUTHORIZED)
            .responseHeaders(Map.of("Access-Control-Allow-Origin", "'*'"))
            .build());

        var productsResource = api.getRoot().addResource("products");
        productsResource.addMethod("GET", new LambdaIntegration(getProductsList),
            MethodOptions.builder()
                .authorizer(cognitoAuthorizer)
                .authorizationType(AuthorizationType.COGNITO)
                .build());
        productsResource.addMethod("POST", new LambdaIntegration(createProduct));
        productsResource.addResource("{productId}")
            .addMethod("GET", new LambdaIntegration(getProductsById));
    }
}
