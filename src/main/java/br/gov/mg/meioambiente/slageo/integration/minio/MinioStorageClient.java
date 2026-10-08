package br.gov.mg.meioambiente.slageo.integration.minio;

import java.io.InputStream;

public interface MinioStorageClient {

    void salvar(
        String objectId,
        InputStream inputStream,
        long tamanho,
        String contentType
    );

    InputStream recuperar(String objectId);

    void remover(String objectId);

    boolean existe(String objectId);
}
