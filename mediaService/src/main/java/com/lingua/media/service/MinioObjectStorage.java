package com.lingua.media.service;

import com.lingua.media.config.ApplicationProperties;
import com.lingua.media.exception.StorageException;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.SetBucketPolicyArgs;
import java.io.ByteArrayInputStream;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class MinioObjectStorage implements ObjectStorage {

    private static final Logger LOG = LoggerFactory.getLogger(MinioObjectStorage.class);

    private static final String PUBLIC_READ_POLICY =
        """
        {
          "Version": "2012-10-17",
          "Statement": [
            {
              "Effect": "Allow",
              "Principal": {"AWS": ["*"]},
              "Action": ["s3:GetObject"],
              "Resource": ["arn:aws:s3:::%s/*"]
            }
          ]
        }
        """;

    private final MinioClient minioClient;

    private final String publicUrl;

    /** Buckets already checked since startup, to avoid asking MinIO on every upload. */
    private final Set<String> readyBuckets = ConcurrentHashMap.newKeySet();

    public MinioObjectStorage(ApplicationProperties properties) {
        ApplicationProperties.Minio minio = properties.minio();
        this.minioClient = MinioClient.builder().endpoint(minio.endpoint()).credentials(minio.accessKey(), minio.secretKey()).build();
        this.publicUrl = minio.publicUrl().replaceAll("/+$", "");
    }

    @Override
    public String store(String bucket, String objectKey, byte[] content, String contentType) {
        try {
            ensureBucketExists(bucket);
            minioClient.putObject(
                PutObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectKey)
                    .stream(new ByteArrayInputStream(content), content.length, -1)
                    .contentType(contentType)
                    .build()
            );
        } catch (Exception e) {
            throw new StorageException("Le stockage des fichiers est indisponible", e);
        }
        return publicUrl + "/" + bucket + "/" + objectKey;
    }

    @Override
    public void remove(String bucket, String objectKey) {
        try {
            minioClient.removeObject(RemoveObjectArgs.builder().bucket(bucket).object(objectKey).build());
        } catch (Exception e) {
            throw new StorageException("Le stockage des fichiers est indisponible", e);
        }
    }

    private void ensureBucketExists(String bucket) throws Exception {
        if (readyBuckets.contains(bucket)) {
            return;
        }
        if (!minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucket).build())) {
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
            minioClient.setBucketPolicy(SetBucketPolicyArgs.builder().bucket(bucket).config(PUBLIC_READ_POLICY.formatted(bucket)).build());
            LOG.info("Bucket '{}' created with public read policy", bucket);
        }
        readyBuckets.add(bucket);
    }
}
