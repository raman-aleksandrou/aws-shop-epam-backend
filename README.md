# AWS Shop Backend

CloudFront link - [https://d1ondnw0chspyw.cloudfront.net/](https://d20yrfgj13ai1q.cloudfront.net/)

## Tasks

### Task 3.1 ✅  

Create a lambda function called `getProductsList` under the same AWS CDK Stack file of Product Service which will be triggered by the HTTP GET method.

- The requested URL should be `/products`.
- The response from the lambda should be a full array of products (mock data should be used - this mock data should be stored in Product Service).
- This endpoint should be integrated with Frontend app for PLP (Product List Page) representation.

### Task 3.2 ✅  

Create a lambda function called `getProductsById` under the same AWS CDK Stack file of Product Service which will be triggered by the HTTP GET method.

- The requested URL should be `/products/{productId}` (what `productId` is in your application is up to you - productName, UUID, etc.).
- The response from the lambda should be 1 searched product from an array of products (mock data should be used - this mock data should be stored in Product Service).
- This endpoint is not needed to be integrated with Frontend right now.

### Swagger Documentation ✅  

OpenAPI 3.0 documentation for the Product Service API is available in [`openapi.yaml`](./openapi.yaml).

To view it, open [https://editor.swagger.io/](https://editor.swagger.io/) and import the file via **File → Import file**.

---

### Task 3.3 ✅  
Commit all your work to separate branch (e.g. `task-3` from the latest master) in your own repository.

Create a pull request to the master branch.

Submit link to the pull request to Crosscheck page in RS App.


### Task 4.1 ✅  
-  Use AWS Console to create two database tables in DynamoDB. Expected schemas for products and stocks:
Product model:

  products:
    id -  uuid (Primary key)
    title - text, not null
    description - text
    price - integer
Stock model:

  stocks:
    product_id - uuid (Foreign key from products.id)
    count - integer (Total number of products in stock, can't be exceeded)
- Write a script to fill tables with test examples. Store it in your Github repository. Execute it for your DB to fill data.

### Task 4.2 ✅  
- Extend your AWS CDK Stack with data about your database table and pass it to lambda’s environment variables section.
- Integrate the getProductsList lambda to return via GET /products request a list of products from the database (joined stocks and products tables).
- Implement a Product model on FE side as a joined model of product and stock by productId. For example:

BE: Separate tables in DynamoDB

  Stock model example in DB:
  {
    product_id: '19ba3d6a-f8ed-491b-a192-0a33b71b38c4',
    count: 2
  }


  Product model example in DB:
  {
    id: '19ba3d6a-f8ed-491b-a192-0a33b71b38c4'
    title: 'Product Title',
    description: 'This product ...',
    price: 200
  }
FE: One product model as a result of BE models join (product and it's stock)

  Product model example on Frontend side:
  {
    id: '19ba3d6a-f8ed-491b-a192-0a33b71b38c4',
    count: 2
    price: 200,
    title: ‘Product Title’,
    description: ‘This product ...’
  }
NOTE: This setup means User cannot buy more than product.count (no more items in stock) - but this is future functionality on FE side.

- Integrate the getProductsById lambda to return via GET /products/{productId} request a single product from the database.

### Task 4.3 ✅  
- Create a lambda function called createProduct under the Product Service which will be triggered by the HTTP POST method.
- The requested URL should be /products.
- Implement its logic so it will be creating a new item in a Products table.
- Save the URL (API Gateway URL) to execute the implemented lambda functions for later - you'll need to provide it in the PR (e.g in PR's description) when submitting the task.

```
POST https://byb2npd55e.execute-api.eu-central-1.amazonaws.com/prod/products
Content-Type: application/json

{
  "title": "New Product by POST-1",
  "description": "Product description",
  "price": 99.99,
  "count": 10
}

Response will be with id and with 201 Status:
{
    "id": "ceead6c2-5ad0-4f46-a36e-ef92d234b7c5",
    "title": "New Product by POST-1",
    "description": "Product description",
    "price": 99.99,
    "count": 10
}
```
✅  POST /products lambda functions returns error 400 status code if product data is invalid
✅  All lambdas return error 500 status code on any error (DB connection, any unhandled error in code)
✅  All lambdas do console.log for each incoming requests and their arguments
✅  Transaction based creation of product (in case stock creation is failed then related to this stock product is not created and not ready to be used by the end user and vice versa)

FE-link: https://d20yrfgj13ai1q.cloudfront.net/

### Task 5.1 ✅  

- Create a new service called import-service at the same level as Product Service with its own AWS CDK Stack. The backend project structure should look like this:
   backend-repository
      product-service
      import-service
- In the AWS Console create and configure a new S3 bucket with a folder called uploaded.
- s3://aws-shop-epam-import-service/uploaded/
- arn:aws:s3:::aws-shop-epam-import-service/uploaded/
![alt text](pics/image-1.png)

### Task 5.2 ✅ 
- Create a lambda function called importProductsFile under the Import Service which will be triggered by the HTTP GET method.
- The requested URL should be /import.

https://6g3uxcq0d6.execute-api.eu-central-1.amazonaws.com/prod/import

- Implement its logic so it will be expecting a request with a name of CSV file with products and creating a new Signed URL with the following key: uploaded/${fileName}.
- The name will be passed in a query string as a name parameter and should be described in the AWS CDK Stack as a request parameter.
- Update AWS CDK Stack with policies to allow lambda functions to interact with S3.
- The response from the lambda should be clean Signed URL, as a string.
- The lambda endpoint should be integrated with the frontend by updating import property of the API paths configuration.
#### Updated for product and import and created PR:
https://github.com/raman-aleksandrou/nodejs-aws-shop-react/pull/2

### Task 5.3 ✅ 
- Create a lambda function called importFileParser under the Import Service which will be triggered by an S3 event.
- The event should be s3:ObjectCreated:*
- Configure the event to be fired only by changes in the uploaded folder in S3.
- The lambda function should use a readable stream to get an object from S3, parse it using csv-parser package and log each record to be shown in CloudWatch.
#### It was tested by adding file(test-products.csv) to bucket and checking logs
- aws s3 cp D:\AWS-Developer-EPAM\aws-shop-epam-backend\import-service\test-products.csv s3://aws-shop-epam-import-service/uploaded/test-products.csv
- aws logs tail /aws/lambda/importFileParser --follow
![alt text](pics/image.png)

### Additional features
✅  importProductsFile lambda is covered by unit tests. You should consider to mock S3 and other AWS SDK methods so not trigger actual AWS services while unit testing.
✅  importFileParser lambda is covered by unit tests.
✅  At the end of the stream the lambda function should move the file from the uploaded folder into the parsed folder (move the file means that file should be copied into a new folder in the same bucket called parsed, and then deleted from uploaded folder)
Before(no files were imported and no folder for it):
![alt text](pics/image-2.png)
After adding folder was ctreacted and lamda was triggered:
![alt text](pics/image-3.png)
![alt text](pics/image-7.png)
Upload is empty:
![alt text](pics/image-4.png)
Parsed contains this file:
![alt text](pics/image-5.png)

### Task 6.1 ✅ 
- Create a lambda function called catalogBatchProcess under the Product Service which will be triggered by an SQS event.
![alt text](pics/image-8.png)
- Create an SQS queue called catalogItemsQueue, in the AWS CDK Stack.
![alt text](pics/image-6.png)
- Configure the SQS to trigger lambda catalogBatchProcess with 5 messages at once via batchSize property.
- The lambda function should iterate over all SQS messages and create corresponding products in the products table.
![alt text](pics/image-9.png)
![alt text](pics/image-10.png)
![alt text](pics/image-11.png)

### Task 6.2 ✅

- Update the importFileParser lambda function in the Import Service to send each CSV record into SQS.
- It should no longer log entries from the readable stream to CloudWatch.
Tested by importing from FE https://d20yrfgj13ai1q.cloudfront.net/admin/products
![alt text](pics/image-12.png)
![alt text](pics/image-13.png)
![alt text](pics/image-14.png)

### Task 6.3 ✅
- Create an SNS topic createProductTopic and email subscription in the AWS CDK Stack of the Product Service.
![alt text](pics/image-15.png)
- Create a subscription for this SNS topic with an email endpoint type with your own email in there.
![alt text](pics/image-16.png)
- Update the catalogBatchProcess lambda function in the Product Service to send an event to the SNS topic once it creates products.
Tested by sending new product and checking email.
![alt text](pics/image-17.png)
![alt text](pics/image-18.png)
![alt text](pics/image-19.png)

#### Additional (optional) tasks
✅ catalogBatchProcess lambda is covered by unit tests
✅ set a Filter Policy for SNS createProductTopic in AWS CDK Stack and create an additional email subscription to distribute messages to different emails depending on the filter for any product attribute
![alt text](pics/image-20.png)
 Filter policy setup:
  ┌──────────────┬────────────────────────────────────┬──────────────┐
  │ Subscription │               Email                │    Filter    │
  ├──────────────┼────────────────────────────────────┼──────────────┤
  │ Premium      │ raman.aleksandrou@gmail.com        │ price >= 100 │
  ├──────────────┼────────────────────────────────────┼──────────────┤
  │ Budget       │ roman.aleksandrov1@yandex.by       │ price < 100  │
  └──────────────┴────────────────────────────────────┴──────────────┘

### Task 7.1 ✅ 
- Create a new service called authorization-service at the same level as Product and Import services with its own AWS CDK Stack. The backend project structure should look like this:
   backend-repository
      product-service
      import-service
      authorization-service
- Create a lambda function called basicAuthorizer under the Authorization Service.
- This lambda should have at least one environment variable with the following credentials:
{yours_github_account_login}=TEST_PASSWORD
{yours_github_account_login} - your GitHub account name. Login for test user should be your GitHub account name
TEST_PASSWORD - password string. Password for test user must be "TEST_PASSWORD"
example: johndoe=TEST_PASSWORD
- This basicAuthorizer lambda should take Basic Authorization token, decode it and check that credentials provided by token exist in the lambda environment variable.
- This lambda should return 403 HTTP status if access is denied for this user (invalid authorization_token) and 401 HTTP status if Authorization header is not provided.
- In case of successfull authorizations, lambda should return IAM policy, which is enabling the invocation of desired method Documentation.

**
 NOTE: Do not send your credentials to the GitHub. Use .env file and dotenv package to add environment variables to the lambda. Add .env file to .gitignore file.
**
 ### Testing
 "methodArn": "arn:aws:execute-api:eu-central-1:678364257956:*/GET/products"
 - Success
 raman-aleksandrou │ TEST_PASSWORD | cmFtYW4tYWxla3NhbmRyb3U6VEVTVF9QQVNTV09SRA==
 ![alt text](pics/image-24.png)
 ![alt text](pics/image-21.png)
 
 - Fail
 raman-aleksandrou │ TEST_PASSWORD-WRONG | cmFtYW4tYWxla3NhbmRyb3U6VEVTVF9QQVNTV09SRC1XUk9ORw==
![alt text](pics/image-22.png)
![alt text](pics/image-23.png)

### Task 7.2 ✅ 
- Add Lambda authorization to the /import path of the Import Service API Gateway.
- Use your basicAuthorizer lambda as the Lambda authorizer
 ### Testing on Windows PowerShell
Base URL: https://6g3uxcq0d6.execute-api.eu-central-1.amazonaws.com/prod/import
Credentials: raman-aleksandrou / TEST_PASSWORD
Token: cmFtYW4tYWxla3NhbmRyb3U6VEVTVF9QQVNTV09SRA==
1. Unauthorized - curl.exe -i "https://6g3uxcq0d6.execute-api.eu-central-1.amazonaws.com/prod/import?name=test.csv"
HTTP/1.1 401 Unauthorized
Content-Type: application/json
Content-Length: 26
Connection: keep-alive
Date: Thu, 28 May 2026 19:05:46 GMT
x-amz-apigw-id: eFvBpE_KFiAEKJg=
x-amzn-RequestId: 70e5cd48-c862-4e60-81f3-4d2a84a1786d
x-amzn-ErrorType: UnauthorizedException
X-Cache: Error from cloudfront
Via: 1.1 4123e89e0fc83589e2324128a6b4b23e.cloudfront.net (CloudFront)
X-Amz-Cf-Pop: WAW51-P2
X-Amz-Cf-Id: EmAMfde0c8G05KJEM4uI1HsoBh802YZaVQYVTO90a8NkAm5WqHAUXQ==

{"message":"Unauthorized"}
2. Forbidden -   curl.exe -i "https://6g3uxcq0d6.execute-api.eu-central-1.amazonaws.com/prod/import?name=test.csv" -H "Authorization: Basic cmFtYW4tYWxla3NhbmRyb3U6V1JPTkc="
HTTP/1.1 403 Forbidden
Content-Type: application/json
Content-Length: 110
Connection: keep-alive
Date: Thu, 28 May 2026 19:15:24 GMT
x-amz-apigw-id: eFwbzEkEFiAEDmQ=
x-amzn-RequestId: 534bdfa7-682c-4a78-bc86-b51b844a073f
x-amzn-ErrorType: AccessDeniedException
X-Cache: Error from cloudfront
Via: 1.1 d5dc130df504729356d2dede87be3764.cloudfront.net (CloudFront)
X-Amz-Cf-Pop: WAW51-P2
X-Amz-Cf-Id: BW0Su2qVztwuwpLLZwDgldMeWxWTqAoIErjvjc9F03TJZYHNWQDb0Q==

{"Message":"User is not authorized to access this resource with an explicit deny in an identity-based policy"}
3. OK -  curl.exe -i "https://6g3uxcq0d6.execute-api.eu-central-1.amazonaws.com/prod/import?name=test.csv" -H "Authorization: Basic cmFtYW4tYWxla3NhbmRyb3U6VEVTVF9QQVNTV09SRA=="
HTTP/1.1 200 OK
Content-Type: text/plain
Content-Length: 1550
Connection: keep-alive
Date: Thu, 28 May 2026 19:17:12 GMT
X-Amzn-Trace-Id: Root=1-6a1894b3-433044b15ea6a7710f86ac87;Parent=1aa80bce28651b7c;Sampled=0;Lineage=2:f3d743bd:0
x-amzn-RequestId: f9c4991e-d560-4897-9546-731f7f0eeb93
Access-Control-Allow-Origin: *
x-amz-apigw-id: eFwsHGYcliAEWCw=
X-Cache: Miss from cloudfront
Via: 1.1 ba172beaa058835048fe52f15497da64.cloudfront.net (CloudFront)
X-Amz-Cf-Pop: WAW51-P2
X-Amz-Cf-Id: G2OI0JKmoVQGo5JLFeehl9yqdU17d_YJb4M146-bf1mg5Y9Q6AGttg==

https://aws-shop-epam-import-service.s3.eu-central-1.amazonaws.com/uploaded/test.csv?X-Amz-Security-Token=IQoJb3JpZ2luX2VjEOv%2F%2F%2F%2F%2F%2F%2F%2F%2F%2FwEaDGV1LWNlbnRyYWwtMSJGMEQCID1IlPOXnqP4l%2FaEbwfBVYZML76RumjaWqao2474YdeWAiBvE2N01BNd9vGPpmXZS%2F9MQkRWWk7iKuEOm05cbkAIbir%2BAwi0%2F%2F%2F%2F%2F%2F%2F%2F%2F%2F8BEAAaDDY3ODM2NDI1Nzk1NiIMcWhUTCPtINGznJl8KtIDe1okyaoi2AXmeRbuoU4nqm3JQydCKuujh5CwHtsX9MTfYgQqpcAXmvgk8q8AgFITLUbIqcdAmPzuqHL7u%2BXuXsc7XP6K0YN07ScaHOw25gKD9qfAUr%2B6EfcUUCs0MnjAdoztcDp92LZrUbJH0zirLT94JrNZQdC9%2BjV4292Uuq38e2EsSBIaQm3kQY%2FdQErEHCaQeOZbSNOyT6xQh%2FyF0oCn2nkE7DFBq6MsUrNgu4ywbut6LRrvS89mf4c8k6VUWmrTTxU6Dz%2BtEKQR6W0d7LV6at012NdlwXCgoMsYytVuf0af0xKFC8%2BnlXpbKVaOihra2wy81U6W%2F6fF%2FsPNjhjiMDZrbqcbdhaEO7cwAN7le4VJF8j50Jaf%2BUayUhUqN98%2Bbr3y9s4vp7aIpRDzJcpg%2FiFo37j2c0LVKwLbhGn4UW8k16JWoZYHo74bSvWxUpYWFCkTwFDOY%2FjvVEOfSSNNR865maxcZLnOfC6RaLAXFLlfMVPk4w7BF70alxSUztxq2xEc7MK2yNKB1VQ%2By%2FcMX5pnheatJioQnUsbeLXj3%2FPHrMp9jaQE9HF%2BrovksyKyHBpHKJ9gfRqidBRjushaIrmHSK6JlkAz9f%2FF5LdUujCzqeLQBjqiAdk4j%2F9X1l5yFb28j5hB7FHqwEMcRYL3zZ2vmnf3I%2FLKayydANMWDn35PNAy4%2FBmeZHLtDN87fW%2BPULXKtnPPj4tIhASrzl6YoRFr4Tud45rP56XXH7cGZU1JfuSsGB%2BFSu3VMmKWITBEGlxmFKkDu%2Bx9Ez%2BEK7JgURWTnAAY6TlGOnKH5YlVG3Lqz1BtJVI4Xnx5EwyiBSx3jdsWlCZruhpLg%3D%3D&X-Amz-Algorithm=AWS4-HMAC-SHA256&X-Amz-Date=20260528T191712Z&X-Amz-SignedHeaders=content-type%3Bhost&X-Amz-Expires=300&X-Amz-Credential=ASIAZ34NJD2SCFIOKDDP%2F20260528%2Feu-central-1%2Fs3%2Faws4_request&X-Amz-Signature=b637e8eaa526a97c055f001b0433633bfb396d720552cd360fe09e45026efbb3

### Task 7.3 ✅ 
1. Request from the client application to the /import path of the Import Service should have Basic Authorization header:
```console
  Authorization: Basic {authorization_token}
```
- {authorization_token} is a base64-encoded {yours_github_account_login}:TEST_PASSWORD
- example: Authorization: Basic sGLzdRxvZmw0ZXs0UGFzcw==
2. Client should get authorization_token value from browser localStorage
  const authorization_token = localStorage.getItem('authorization_token')

  FE-pullrequest https://github.com/raman-aleksandrou/nodejs-aws-shop-react/pull/3
 ### Testing 
 Added token to localStorage

 ![alt text](pics/image-26.png)

 Then was made upload file and checked response and headers

 ![alt text](pics/image-25.png)

 And new product was added