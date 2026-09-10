package br.gov.mg.meioambiente.slageo.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.locationtech.jts.geom.Geometry;

@Entity
@Table(
        name = "tb_atividade",
        schema = "sla"
)
public class AtividadeGeometria {

    @Id
    @Column(
            name = "id_atividade",
            nullable = false
    )
    private Long idAtividade;

    @Column(
            name = "id_solic",
            nullable = false
    )
    private Long idSolic;

    @Column(
            name = "geom",
            columnDefinition = "geometry(Geometry,4674)"
    )
    private Geometry geometria;

    public AtividadeGeometria() {
    }

    public AtividadeGeometria(
            Long idAtividade,
            Long idSolic,
            Geometry geometria) {

        this.idAtividade = idAtividade;
        this.idSolic = idSolic;
        this.geometria = geometria;
    }

    public Long getIdAtividade() {
        return idAtividade;
    }

    public void setIdAtividade(
            Long idAtividade) {

        this.idAtividade = idAtividade;
    }

    public Long getIdSolic() {
        return idSolic;
    }

    public void setIdSolic(
            Long idSolic) {

        this.idSolic = idSolic;
    }

    public Geometry getGeometria() {
        return geometria;
    }

    public void setGeometria(
            Geometry geometria) {

        this.geometria = geometria;
    }
}
