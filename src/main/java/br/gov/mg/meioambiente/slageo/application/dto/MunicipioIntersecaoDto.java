package br.gov.mg.meioambiente.slageo.application.dto;

public class MunicipioIntersecaoDto {

    private String codigoIbge;
    private String nome;
    private Double areaIntersecaoM2;

    public MunicipioIntersecaoDto() {
    }

    public MunicipioIntersecaoDto(
            String codigoIbge,
            String nome,
            Double areaIntersecaoM2) {

        this.codigoIbge = codigoIbge;
        this.nome = nome;
        this.areaIntersecaoM2 = areaIntersecaoM2;
    }

    public String getCodigoIbge() {
        return codigoIbge;
    }

    public void setCodigoIbge(String codigoIbge) {
        this.codigoIbge = codigoIbge;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public Double getAreaIntersecaoM2() {
        return areaIntersecaoM2;
    }

    public void setAreaIntersecaoM2(
            Double areaIntersecaoM2) {

        this.areaIntersecaoM2 = areaIntersecaoM2;
    }
}
