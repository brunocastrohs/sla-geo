package br.gov.mg.meioambiente.slageo.config;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MinioConfig {

    @Bean
    public MinioClient minioClient(
        @Value("${minio.endpoint}") String endpoint,
        @Value("${minio.access-key}") String accessKey,
        @Value("${minio.secret-key}") String secretKey,
        @Value("${minio.bucket}") String bucket
    ) {
        MinioClient client = MinioClient.builder()
            .endpoint(endpoint)
            .credentials(accessKey, secretKey)
            .build();

        garantirBucket(client, bucket);

        return client;
    }

    private void garantirBucket(
        MinioClient client,
        String bucket
    ) {
        try {
            boolean existe = client.bucketExists(
                BucketExistsArgs.builder()
                    .bucket(bucket)
                    .build()
            );

            if (!existe) {
                client.makeBucket(
                    MakeBucketArgs.builder()
                        .bucket(bucket)
                        .build()
                );
            }

        } catch (Exception e) {
            throw new IllegalStateException(
                "Não foi possível inicializar o bucket MinIO: " + bucket,
                e
            );
        }
    }
}
