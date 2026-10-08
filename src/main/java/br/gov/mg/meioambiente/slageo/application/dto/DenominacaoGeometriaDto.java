package br.gov.mg.meioambiente.slageo.application.dto;

import jakarta.validation.constraints.NotNull;
import tools.jackson.databind.JsonNode;

public class DenominacaoGeometriaDto {

    private Long idDenominacao;

    @NotNull
    private Integer idSolic;

    @NotNull
    private Integer idDenom;

    private String denominacao;

    @NotNull
    private JsonNode geoJson;

    private String geoHash;

    private String arquivoId;

    public DenominacaoGeometriaDto() {
    }

    public Long getIdDenominacao() {
        return idDenominacao;
    }

    public void setIdDenominacao(Long idDenominacao) {
        this.idDenominacao = idDenominacao;
    }

    public Integer getIdSolic() {
        return idSolic;
    }

    public void setIdSolic(Integer idSolic) {
        this.idSolic = idSolic;
    }

    public Integer getIdDenom() {
        return idDenom;
    }

    public void setIdDenom(Integer idDenom) {
        this.idDenom = idDenom;
    }

    public String getDenominacao() {
        return denominacao;
    }

    public void setDenominacao(String denominacao) {
        this.denominacao = denominacao;
    }

    public JsonNode getGeoJson() {
        return geoJson;
    }

    public void setGeoJson(JsonNode geoJson) {
        this.geoJson = geoJson;
    }

    public String getGeoHash() {
        return geoHash;
    }

    public void setGeoHash(String geoHash) {
        this.geoHash = geoHash;
    }

    public String getArquivoId() {
        return arquivoId;
    }

    public void setArquivoId(String arquivoId) {
        this.arquivoId = arquivoId;
    }
}
