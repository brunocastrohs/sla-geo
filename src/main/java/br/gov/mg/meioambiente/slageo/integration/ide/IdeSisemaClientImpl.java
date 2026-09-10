package br.gov.mg.meioambiente.slageo.integration.ide;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;

@Component
public class IdeSisemaClientImpl
        implements IdeSisemaClient {

    private final RestClient restClient;

    private final JsonMapper jsonMapper;

    private final String baseUrl;

    private final String token;

    public IdeSisemaClientImpl(
            @Value("${ide.sisema.base-url}")
            String baseUrl,

            @Value("${ide.sisema.token}")
            String token) {

        this.restClient =
                RestClient.create();

        this.jsonMapper =
                JsonMapper.builder().build();

        this.baseUrl =
                removerBarraFinal(baseUrl);

        this.token =
                token;
    }

    @Override
    public JsonNode consultar(
            String camada,
            String operacao,
            Integer distancia,
            boolean retornarLayerFeature,
            List<JsonNode> geometrias) {

        ArrayNode requisicoes =
                jsonMapper.createArrayNode();

        for (JsonNode geoJson : geometrias) {

            ObjectNode consulta =
                    jsonMapper.createObjectNode();

            consulta.put(
                    "wmslayer",
                    camada
            );

            consulta.put(
                    "operation",
                    operacao
            );

            if (distancia != null) {

                consulta.put(
                        "distance",
                        distancia
                );
            }

            ArrayNode retorno =
                    consulta.putArray(
                            "return"
                    );

            retorno.add(
                    "yesno"
            );

            if (retornarLayerFeature) {

                retorno.add(
                        "layerfeature"
                );
            }

            ObjectNode feature =
                    jsonMapper.createObjectNode();

            feature.put(
                    "type",
                    "Feature"
            );

            feature.set(
                    "geometry",
                    geoJson
            );

            consulta.set(
                    "geometry",
                    feature
            );

            requisicoes.add(
                    consulta
            );
        }

        try {

            JsonNode response =
                    restClient
                            .post()
                            .uri(
                                    baseUrl
                                            + "/query"
                            )
                            .contentType(
                                    MediaType.APPLICATION_JSON
                            )
                            .header(
                                    HttpHeaders.AUTHORIZATION,
                                    "Bearer " + token
                            )
                            .body(
                                    requisicoes
                            )
                            .retrieve()
                            .body(
                                    JsonNode.class
                            );

            if (response == null) {

                return jsonMapper
                        .createArrayNode();
            }

            return response;

        } catch (Exception e) {

            throw new IllegalStateException(
                    "Erro ao consultar a IDE SISEMA na camada "
                            + camada
                            + ".",
                    e
            );
        }
    }

    private String removerBarraFinal(
            String valor) {

        if (valor == null) {
            return "";
        }

        while (valor.endsWith("/")) {

            valor =
                    valor.substring(
                            0,
                            valor.length() - 1
                    );
        }

        return valor;
    }
}
