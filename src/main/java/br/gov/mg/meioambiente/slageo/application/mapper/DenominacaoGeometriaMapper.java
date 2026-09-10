package br.gov.mg.meioambiente.slageo.application.mapper;

import br.gov.mg.meioambiente.slageo.application.dto.DenominacaoGeometriaDto;
import br.gov.mg.meioambiente.slageo.model.DenominacaoGeometria;
import br.gov.mg.meioambiente.slageo.model.enums.DenominacaoEnum;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

@Mapper(
        componentModel =
                MappingConstants.ComponentModel.SPRING,
        uses = GeoJsonGeometryMapper.class
)
public interface DenominacaoGeometriaMapper {

    @Mapping(
            source = "geoJson",
            target = "geometria"
    )
    @Mapping(
            source = "idDenom",
            target = "denominacao"
    )
    @Mapping(
            target = "dataExcl",
            ignore = true
    )
    @Mapping(
            target = "geomPoint",
            ignore = true
    )
    DenominacaoGeometria toEntity(
            DenominacaoGeometriaDto dto
    );

    @Mapping(
            source = "geometria",
            target = "geoJson"
    )
    @Mapping(
            source = "denominacao",
            target = "idDenom"
    )
    @Mapping(
            source = "denominacao.descricao",
            target = "denominacao"
    )
    DenominacaoGeometriaDto toDto(
            DenominacaoGeometria entity
    );

    default DenominacaoEnum map(
            Integer idDenom) {

        return DenominacaoEnum.fromId(
                idDenom
        );
    }

    default Integer map(
            DenominacaoEnum denominacao) {

        if (denominacao == null) {
            return null;
        }

        return denominacao.getId();
    }
}
