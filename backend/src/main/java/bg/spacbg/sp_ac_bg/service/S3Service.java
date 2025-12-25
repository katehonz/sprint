package bg.spacbg.sp_ac_bg.service;

import bg.spacbg.sp_ac_bg.config.ObjectStorageConfig;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Object;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.List;
import java.util.Optional;

@Service
public class S3Service {

    private final ObjectStorageConfig config;
    private S3Client s3Client;

    public S3Service(ObjectStorageConfig config) {
        this.config = config;
        if (config.isEnabled()) {
            initS3Client();
        }
    }

    private void initS3Client() {
        if (!config.isEnabled() || config.getAccessKey() == null || config.getSecretKey() == null || config.getBucket() == null) {
            return;
        }

        AwsBasicCredentials credentials = AwsBasicCredentials.create(config.getAccessKey(), config.getSecretKey());
        S3ClientBuilder builder = S3Client.builder()
                .credentialsProvider(StaticCredentialsProvider.create(credentials))
                .region(Region.of(config.getRegion()));

        if (config.getEndpoint() != null && !config.getEndpoint().isEmpty()) {
            builder.endpointOverride(URI.create(config.getEndpoint()));
        }

        if (config.isForcePathStyle()) {
            builder.forcePathStyle(true);
        }

        this.s3Client = builder.build();
    }

    public boolean isEnabled() {
        return config.isEnabled() && s3Client != null;
    }

    public String uploadFile(String key, InputStream inputStream, long contentLength, String contentType) throws IOException {
        if (!isEnabled()) {
            throw new IllegalStateException("S3 service is not enabled or not configured properly.");
        }

        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(config.getBucket())
                .key(config.getPrefix() != null && !config.getPrefix().isEmpty() ? config.getPrefix() + "/" + key : key)
                .contentType(contentType)
                .contentLength(contentLength)
                .build();

        s3Client.putObject(putObjectRequest, RequestBody.fromInputStream(inputStream, contentLength));
        return config.getPrefix() != null && !config.getPrefix().isEmpty() ? config.getPrefix() + "/" + key : key;
    }

    public InputStream downloadFile(String key) {
        if (!isEnabled()) {
            throw new IllegalStateException("S3 service is not enabled or not configured properly.");
        }

        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(config.getBucket())
                .key(key)
                .build();

        return s3Client.getObject(getObjectRequest);
    }

    public List<S3Object> listFiles(String prefix) {
        if (!isEnabled()) {
            throw new IllegalStateException("S3 service is not enabled or not configured properly.");
        }

        ListObjectsV2Request listObjectsV2Request = ListObjectsV2Request.builder()
                .bucket(config.getBucket())
                .prefix(config.getPrefix() != null && !config.getPrefix().isEmpty() ? config.getPrefix() + "/" + prefix : prefix)
                .build();

        return s3Client.listObjectsV2(listObjectsV2Request).contents();
    }
}
