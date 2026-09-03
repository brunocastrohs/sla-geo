package br.gov.mg.meioambiente.slageo.application.dto;

import tools.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotNull;

public class AtividadeGeometriaDto {

    @NotNull
    private Long idSolic;

    @NotNull
    private Long idAtividade;

    @NotNull
    private JsonNode geoJson;

    public AtividadeGeometriaDto() {
    }

    public AtividadeGeometriaDto(
            Long idSolic,
            Long idAtividade,
            JsonNode geoJson) {

        this.idSolic = idSolic;
        this.idAtividade = idAtividade;
        this.geoJson = geoJson;
    }

    public Long getIdSolic() {
        return idSolic;
    }

    public void setIdSolic(Long idSolic) {
        this.idSolic = idSolic;
    }

    public Long getIdAtividade() {
        return idAtividade;
    }

    public void setIdAtividade(Long idAtividade) {
        this.idAtividade = idAtividade;
    }

    public JsonNode getGeoJson() {
        return geoJson;
    }

    public void setGeoJson(JsonNode geoJson) {
        this.geoJson = geoJson;
    }
}
