package br.com.siecola.aws_lambda_products_api.handler;

import br.com.siecola.aws_lambda_products_api.model.Product;
import br.com.siecola.aws_lambda_products_api.model.ProductEvent;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.spec.PutItemSpec;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import com.amazonaws.services.sqs.model.SendMessageRequest;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.*;
import java.time.Instant;

public class ProductsHandler implements RequestStreamHandler {
    private static final String DYNAMODB_TABLE_NAME = "products";

    public void handleRequest(
            InputStream inputStream,
            OutputStream outputStream,
            Context context)
            throws IOException {

        JSONParser parser = new JSONParser();
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        JSONObject responseJson = new JSONObject();

        AmazonDynamoDB client = AmazonDynamoDBClientBuilder.defaultClient();
        DynamoDB dynamoDb = new DynamoDB(client);

        try {
            JSONObject event = (JSONObject) parser.parse(reader);

            if (event.get("body") != null) {
                Product product = new Product((String) event.get("body"));

                dynamoDb.getTable(DYNAMODB_TABLE_NAME)
                        .putItem(new PutItemSpec().withItem(new Item().withNumber("id", product.getId())
                                .withString("name", product.getName())
                                .withString("code", product.getCode())
                                .withString("model", product.getModel())
                                .withDouble("price", product.getPrice())
                        ));
                this.sendProductEvent(product, "CREATED");
            }

            JSONObject responseBody = new JSONObject();
            responseBody.put("message", "New product created");

            JSONObject headerJson = new JSONObject();
            headerJson.put("x-custom-header", "custom header");

            responseJson.put("statusCode", 200);
            responseJson.put("headers", headerJson);
            responseJson.put("body", responseBody.toString());

        } catch (ParseException pex) {
            responseJson.put("statusCode", 400);
            responseJson.put("exception", pex);
        }

        OutputStreamWriter writer = new OutputStreamWriter(outputStream, "UTF-8");
        writer.write(responseJson.toString());
        writer.close();
    }

    public void handleGetByParam(
            InputStream inputStream, OutputStream outputStream, Context context)
            throws IOException {

        JSONParser parser = new JSONParser();
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        JSONObject responseJson = new JSONObject();

        AmazonDynamoDB client = AmazonDynamoDBClientBuilder.defaultClient();
        DynamoDB dynamoDb = new DynamoDB(client);

        Item result = null;
        try {
            JSONObject event = (JSONObject) parser.parse(reader);
            JSONObject responseBody = new JSONObject();

            if (event.get("pathParameters") != null) {
                JSONObject pps = (JSONObject) event.get("pathParameters");
                if (pps.get("id") != null) {
                    int id = Integer.parseInt((String) pps.get("id"));
                    result = dynamoDb.getTable(DYNAMODB_TABLE_NAME).getItem("id", id);
                }
            } else if (event.get("queryStringParameters") != null) {
                JSONObject qps = (JSONObject) event.get("queryStringParameters");
                if (qps.get("id") != null) {

                    int id = Integer.parseInt((String) qps.get("id"));
                    result = dynamoDb.getTable(DYNAMODB_TABLE_NAME)
                            .getItem("id", id);
                }
            }
            if (result != null) {
                Product product = new Product(result.toJSON());

                this.sendProductEvent(product, "RETRIEVED");

                responseBody.put("Product", product);
                responseJson.put("statusCode", 200);
            } else {
                responseBody.put("message", "No product found");
                responseJson.put("statusCode", 404);
            }

            JSONObject headerJson = new JSONObject();
            headerJson.put("x-custom-header", "custom header");

            responseJson.put("headers", headerJson);
            responseJson.put("body", responseBody.toString());

        } catch (ParseException pex) {
            responseJson.put("statusCode", 400);
            responseJson.put("exception", pex);
        }

        OutputStreamWriter writer = new OutputStreamWriter(outputStream, "UTF-8");
        writer.write(responseJson.toString());
        writer.close();
    }

    private void sendProductEvent(Product product, String event) {
        ProductEvent productEvent = new ProductEvent();
        productEvent.setCode(product.getCode());
        productEvent.setEvent(event);
        productEvent.setProductId(product.getId());
        productEvent.setTimestamp(Instant.now().toEpochMilli());

        SendMessageRequest send_msg_request = new SendMessageRequest()
                .withQueueUrl("https://sqs.us-west-2.amazonaws.com/946835467386/product-events")
                .withMessageBody(productEvent.toString());
        AmazonSQS sqs = AmazonSQSClientBuilder.defaultClient();
        sqs.sendMessage(send_msg_request);
    }
}