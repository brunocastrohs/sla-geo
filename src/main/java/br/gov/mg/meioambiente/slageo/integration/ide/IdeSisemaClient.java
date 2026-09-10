package br.gov.mg.meioambiente.slageo.integration.ide;

import tools.jackson.databind.JsonNode;

import java.util.List;

public interface IdeSisemaClient {

    JsonNode consultar(
            String camada,
            String operacao,
            Integer distancia,
            boolean retornarLayerFeature,
            List<JsonNode> geometrias
    );
}
