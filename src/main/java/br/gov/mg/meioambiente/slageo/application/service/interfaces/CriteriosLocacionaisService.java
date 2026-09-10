package br.gov.mg.meioambiente.slageo.application.service.interfaces;

import tools.jackson.databind.JsonNode;

public interface CriteriosLocacionaisService {

    JsonNode calcular(
            Long idDenominacao
    );

    JsonNode buscar(
            Long idDenominacao
    );
}
