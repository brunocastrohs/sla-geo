package br.gov.mg.meioambiente.slageo.integration.minio;

import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.GetObjectResponse;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.StatObjectArgs;
import io.minio.errors.ErrorResponseException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.InputStream;

@Component
public class MinioStorageClientImpl implements MinioStorageClient {

    private final MinioClient minioClient;
    private final String bucket;

    public MinioStorageClientImpl(
        MinioClient minioClient,
        @Value("${minio.bucket}") String bucket
    ) {
        this.minioClient = minioClient;
        this.bucket = bucket;
    }

    @Override
    public void salvar(
        String objectId,
        InputStream inputStream,
        long tamanho,
        String contentType
    ) {
        validarObjectId(objectId);

        if (inputStream == null) {
            throw new IllegalArgumentException(
                "O conteúdo do arquivo não pode ser nulo."
            );
        }

        if (tamanho <= 0) {
            throw new IllegalArgumentException(
                "O arquivo deve possuir conteúdo."
            );
        }

        String tipo = contentType;

        if (tipo == null || tipo.isBlank()) {
            tipo = "application/octet-stream";
        }

        try {
            minioClient.putObject(
                PutObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectId)
                    .stream(inputStream, tamanho, -1)
                    .contentType(tipo)
                    .build()
            );
        } catch (Exception e) {
            throw new IllegalStateException(
                "Não foi possível salvar o arquivo no MinIO: " + objectId,
                e
            );
        }
    }

    @Override
    public InputStream recuperar(String objectId) {
        validarObjectId(objectId);

        try {
            GetObjectResponse response = minioClient.getObject(
                GetObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectId)
                    .build()
            );

            return response;
        } catch (Exception e) {
            throw new IllegalStateException(
                "Não foi possível recuperar o arquivo do MinIO: " + objectId,
                e
            );
        }
    }

    @Override
    public void remover(String objectId) {
        validarObjectId(objectId);

        try {
            minioClient.removeObject(
                RemoveObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectId)
                    .build()
            );
        } catch (Exception e) {
            throw new IllegalStateException(
                "Não foi possível remover o arquivo do MinIO: " + objectId,
                e
            );
        }
    }

    @Override
    public boolean existe(String objectId) {
        validarObjectId(objectId);

        try {
            minioClient.statObject(
                StatObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectId)
                    .build()
            );

            return true;

        } catch (ErrorResponseException e) {

            if ("NoSuchKey".equals(e.errorResponse().code())
                || "NoSuchObject".equals(e.errorResponse().code())) {
                return false;
            }

            throw new IllegalStateException(
                "Não foi possível consultar o arquivo no MinIO: " + objectId,
                e
            );

        } catch (Exception e) {
            throw new IllegalStateException(
                "Não foi possível consultar o arquivo no MinIO: " + objectId,
                e
            );
        }
    }

    private void validarObjectId(String objectId) {
        if (objectId == null || objectId.isBlank()) {
            throw new IllegalArgumentException(
                "O identificador do arquivo não pode ser vazio."
            );
        }
    }
}
