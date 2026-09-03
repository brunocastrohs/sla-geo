package br.gov.mg.meioambiente.slageo.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.locationtech.jts.geom.MultiPolygon;

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
            nullable = false,
            columnDefinition = "geometry(MultiPolygon,4674)"
    )
    private MultiPolygon geometria;

    public AtividadeGeometria() {
    }

    public AtividadeGeometria(
            Long idSolic,
            Long idAtividade,
            MultiPolygon geometria) {

        this.idSolic = idSolic;
        this.idAtividade = idAtividade;
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

    public MultiPolygon getGeometria() {
        return geometria;
    }

    public void setGeometria(
            MultiPolygon geometria) {

        if (geometria == null || geometria.isEmpty()) {
            throw new IllegalArgumentException(
                    "A geometria não pode ser vazia"
            );
        }

        if (!geometria.isValid()) {
            throw new IllegalArgumentException(
                    "A geometria informada é inválida"
            );
        }

        this.geometria = geometria;
    }
}
