package com.shop.handlers;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.events.S3Event;
import com.amazonaws.services.lambda.runtime.events.models.s3.S3EventNotification;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CopyObjectRequest;
import software.amazon.awssdk.services.s3.model.CopyObjectResponse;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectResponse;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ImportFileParserHandlerTest {

    private S3Client s3Client;
    private SqsClient sqsClient;
    private ImportFileParserHandler handler;
    private Context context;

    @BeforeEach
    void setUp() {
        s3Client = mock(S3Client.class);
        sqsClient = mock(SqsClient.class);
        when(sqsClient.sendMessage(any(SendMessageRequest.class)))
            .thenReturn(SendMessageResponse.builder().messageId("msg-id").build());
        handler = new ImportFileParserHandler(s3Client, sqsClient);
        context = mock(Context.class);
        when(context.getLogger()).thenReturn(mock(LambdaLogger.class));
    }

    private S3Event buildS3Event(String bucket, String key) {
        S3EventNotification.S3BucketEntity bucketEntity =
            new S3EventNotification.S3BucketEntity(bucket, null, null);
        S3EventNotification.S3ObjectEntity objectEntity =
            new S3EventNotification.S3ObjectEntity(key, null, null, null, null);
        S3EventNotification.S3Entity s3Entity =
            new S3EventNotification.S3Entity(null, bucketEntity, objectEntity, null);
        S3EventNotification.S3EventNotificationRecord record =
            new S3EventNotification.S3EventNotificationRecord(
                null, null, null, null, null, null, null, s3Entity, null);
        return new S3Event(List.of(record));
    }

    private ResponseInputStream<GetObjectResponse> csvStream(String csv) {
        byte[] bytes = csv.getBytes(StandardCharsets.UTF_8);
        return new ResponseInputStream<>(
            GetObjectResponse.builder().build(),
            AbortableInputStream.create(new ByteArrayInputStream(bytes))
        );
    }

    @Test
    void sendsOneMessagePerCsvRecord() {
        String csv = "title,description,price,count\nProduct A,Desc A,10,5\nProduct B,Desc B,20,3\n";
        when(s3Client.getObject(any(GetObjectRequest.class))).thenReturn(csvStream(csv));
        when(s3Client.copyObject(any(CopyObjectRequest.class))).thenReturn(CopyObjectResponse.builder().build());
        when(s3Client.deleteObject(any(DeleteObjectRequest.class))).thenReturn(DeleteObjectResponse.builder().build());

        handler.handleRequest(buildS3Event("test-bucket", "uploaded/test.csv"), context);

        verify(sqsClient, times(2)).sendMessage(any(SendMessageRequest.class));
    }

    @Test
    void messageBodyContainsCsvFields() {
        String csv = "title,description,price,count\nProduct A,Desc A,10,5\n";
        when(s3Client.getObject(any(GetObjectRequest.class))).thenReturn(csvStream(csv));
        when(s3Client.copyObject(any(CopyObjectRequest.class))).thenReturn(CopyObjectResponse.builder().build());
        when(s3Client.deleteObject(any(DeleteObjectRequest.class))).thenReturn(DeleteObjectResponse.builder().build());

        handler.handleRequest(buildS3Event("test-bucket", "uploaded/test.csv"), context);

        ArgumentCaptor<SendMessageRequest> captor = ArgumentCaptor.forClass(SendMessageRequest.class);
        verify(sqsClient).sendMessage(captor.capture());
        String body = captor.getValue().messageBody();
        assertTrue(body.contains("Product A"));
        assertTrue(body.contains("Desc A"));
        assertTrue(body.contains("10"));
        assertTrue(body.contains("5"));
    }

    @Test
    void doesNotSendMessagesForEmptyCsv() {
        String csv = "title,description,price,count\n";
        when(s3Client.getObject(any(GetObjectRequest.class))).thenReturn(csvStream(csv));
        when(s3Client.copyObject(any(CopyObjectRequest.class))).thenReturn(CopyObjectResponse.builder().build());
        when(s3Client.deleteObject(any(DeleteObjectRequest.class))).thenReturn(DeleteObjectResponse.builder().build());

        handler.handleRequest(buildS3Event("test-bucket", "uploaded/empty.csv"), context);

        verify(sqsClient, never()).sendMessage(any(SendMessageRequest.class));
    }

    @Test
    void movesFileToParsedAfterSending() {
        String csv = "title,price\nProduct A,10\n";
        when(s3Client.getObject(any(GetObjectRequest.class))).thenReturn(csvStream(csv));
        when(s3Client.copyObject(any(CopyObjectRequest.class))).thenReturn(CopyObjectResponse.builder().build());
        when(s3Client.deleteObject(any(DeleteObjectRequest.class))).thenReturn(DeleteObjectResponse.builder().build());

        handler.handleRequest(buildS3Event("test-bucket", "uploaded/test.csv"), context);

        verify(s3Client).copyObject(any(CopyObjectRequest.class));
        verify(s3Client).deleteObject(any(DeleteObjectRequest.class));
    }

    @Test
    void copiesFileToCorrectParsedKey() {
        String csv = "title,price\nProduct A,10\n";
        when(s3Client.getObject(any(GetObjectRequest.class))).thenReturn(csvStream(csv));
        when(s3Client.copyObject(any(CopyObjectRequest.class))).thenReturn(CopyObjectResponse.builder().build());
        when(s3Client.deleteObject(any(DeleteObjectRequest.class))).thenReturn(DeleteObjectResponse.builder().build());

        handler.handleRequest(buildS3Event("test-bucket", "uploaded/products.csv"), context);

        ArgumentCaptor<CopyObjectRequest> copyCaptor = ArgumentCaptor.forClass(CopyObjectRequest.class);
        verify(s3Client).copyObject(copyCaptor.capture());
        assertEquals("parsed/products.csv", copyCaptor.getValue().destinationKey());

        ArgumentCaptor<DeleteObjectRequest> deleteCaptor = ArgumentCaptor.forClass(DeleteObjectRequest.class);
        verify(s3Client).deleteObject(deleteCaptor.capture());
        assertEquals("uploaded/products.csv", deleteCaptor.getValue().key());
    }
}
