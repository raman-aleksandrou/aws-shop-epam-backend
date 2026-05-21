package com.shop.handlers;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.S3Event;
import com.amazonaws.services.lambda.runtime.events.models.s3.S3EventNotification.S3EventNotificationRecord;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CopyObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class ImportFileParserHandler implements RequestHandler<S3Event, Void> {

    private static final String UPLOADED_PREFIX = "uploaded/";
    private static final String PARSED_PREFIX = "parsed/";

    private final S3Client s3Client;

    public ImportFileParserHandler() {
        this.s3Client = S3Client.create();
    }

    public ImportFileParserHandler(S3Client s3Client) {
        this.s3Client = s3Client;
    }

    @Override
    public Void handleRequest(S3Event event, Context context) {
        context.getLogger().log("Incoming S3 event: " + event);

        for (S3EventNotificationRecord record : event.getRecords()) {
            String bucket = record.getS3().getBucket().getName();
            String key = record.getS3().getObject().getUrlDecodedKey();

            context.getLogger().log("Processing file: s3://" + bucket + "/" + key);

            try {
                GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .build();

                try (InputStream s3Stream = s3Client.getObject(getObjectRequest);
                     BufferedReader reader = new BufferedReader(new InputStreamReader(s3Stream, StandardCharsets.UTF_8));
                     CSVParser csvParser = CSVFormat.DEFAULT.builder()
                         .setHeader()
                         .setSkipHeaderRecord(true)
                         .build()
                         .parse(reader)) {

                    for (CSVRecord csvRecord : csvParser) {
                        context.getLogger().log("Parsed record: " + csvRecord.toMap());
                    }
                }

                context.getLogger().log("Finished parsing file: " + key);

                moveToparsed(bucket, key, context);
            } catch (Exception e) {
                context.getLogger().log("ERROR parsing file " + key + ": " + e.getMessage());
                throw new RuntimeException("Failed to parse S3 object: " + key, e);
            }
        }

        return null;
    }

    private void moveToparsed(String bucket, String sourceKey, Context context) {
        String fileName = sourceKey.replace(UPLOADED_PREFIX, "");
        String destKey = PARSED_PREFIX + fileName;

        context.getLogger().log("Copying s3://" + bucket + "/" + sourceKey + " -> " + destKey);

        s3Client.copyObject(CopyObjectRequest.builder()
            .sourceBucket(bucket)
            .sourceKey(sourceKey)
            .destinationBucket(bucket)
            .destinationKey(destKey)
            .build());

        s3Client.deleteObject(DeleteObjectRequest.builder()
            .bucket(bucket)
            .key(sourceKey)
            .build());

        context.getLogger().log("Moved to parsed: " + destKey);
    }
}
