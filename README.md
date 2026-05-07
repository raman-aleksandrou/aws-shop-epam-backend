# AWS Shop Backend

## Tasks

### Task 3.1

Create a lambda function called `getProductsList` under the same AWS CDK Stack file of Product Service which will be triggered by the HTTP GET method.

- The requested URL should be `/products`.
- The response from the lambda should be a full array of products (mock data should be used - this mock data should be stored in Product Service).
- This endpoint should be integrated with Frontend app for PLP (Product List Page) representation.

### Task 3.2

Create a lambda function called `getProductsById` under the same AWS CDK Stack file of Product Service which will be triggered by the HTTP GET method.

- The requested URL should be `/products/{productId}` (what `productId` is in your application is up to you - productName, UUID, etc.).
- The response from the lambda should be 1 searched product from an array of products (mock data should be used - this mock data should be stored in Product Service).
- This endpoint is not needed to be integrated with Frontend right now.

### Swagger Documentation

OpenAPI 3.0 documentation for the Product Service API is available in [`openapi.yaml`](./openapi.yaml).

To view it, open [https://editor.swagger.io/](https://editor.swagger.io/) and import the file via **File → Import file**.

---

### Task 3.3

Commit all your work to separate branch (e.g. `task-3` from the latest master) in your own repository.

Create a pull request to the master branch.

Submit link to the pull request to Crosscheck page in RS App.


Task 4.1
1.  Use AWS Console to create two database tables in DynamoDB. Expected schemas for products and stocks:
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
2. Write a script to fill tables with test examples. Store it in your Github repository. Execute it for your DB to fill data.
