package br.gov.mg.meioambiente.slageo.model;

import br.gov.mg.meioambiente.slageo.model.converter.DenominacaoEnumConverter;
import br.gov.mg.meioambiente.slageo.model.enums.DenominacaoEnum;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "tb_denominacao",
        schema = "sla"
)
public class DenominacaoGeometria {

    @Id
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "denominacao_seq"
    )
    @SequenceGenerator(
            name = "denominacao_seq",
            sequenceName = "sla.denominacao_seq",
            allocationSize = 1
    )
    @Column(
            name = "id_denominacao",
            nullable = false
    )
    private Long idDenominacao;

    @Column(
            name = "id_solic",
            nullable = false
    )
    private Integer idSolic;

    @Convert(
            converter =
                    DenominacaoEnumConverter.class
    )
    @Column(
            name = "id_denom",
            nullable = false
    )
    private DenominacaoEnum denominacao;

    @Column(name = "data_excl")
    private LocalDateTime dataExcl;

    @Column(
            name = "geom",
            columnDefinition =
                    "geometry(Geometry,4674)"
    )
    private Geometry geometria;

    @Column(
            name = "geo_hash",
            length = 32
    )
    private String geoHash;

    @Column(
            name = "geom_point",
            columnDefinition =
                    "geometry(Point,4674)"
    )
    private Point geomPoint;

    public DenominacaoGeometria() {
    }

    public Long getIdDenominacao() {
        return idDenominacao;
    }

    public void setIdDenominacao(
            Long idDenominacao) {

        this.idDenominacao =
                idDenominacao;
    }

    public Integer getIdSolic() {
        return idSolic;
    }

    public void setIdSolic(
            Integer idSolic) {

        this.idSolic =
                idSolic;
    }

    public DenominacaoEnum getDenominacao() {
        return denominacao;
    }

    public void setDenominacao(
            DenominacaoEnum denominacao) {

        this.denominacao =
                denominacao;
    }

    public LocalDateTime getDataExcl() {
        return dataExcl;
    }

    public void setDataExcl(
            LocalDateTime dataExcl) {

        this.dataExcl =
                dataExcl;
    }

    public Geometry getGeometria() {
        return geometria;
    }

    public void setGeometria(
            Geometry geometria) {

        this.geometria =
                geometria;
    }

    public String getGeoHash() {
        return geoHash;
    }

    public void setGeoHash(
            String geoHash) {

        this.geoHash =
                geoHash;
    }

    public Point getGeomPoint() {
        return geomPoint;
    }

    public void setGeomPoint(
            Point geomPoint) {

        this.geomPoint =
                geomPoint;
    }
}
