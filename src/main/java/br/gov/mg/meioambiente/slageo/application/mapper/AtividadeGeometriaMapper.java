package br.gov.mg.meioambiente.slageo.application.mapper;

import br.gov.mg.meioambiente.slageo.application.dto.AtividadeGeometriaDto;
import br.gov.mg.meioambiente.slageo.model.AtividadeGeometria;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

@Mapper(
        componentModel = MappingConstants.ComponentModel.SPRING,
        uses = GeoJsonGeometryMapper.class
)
public interface AtividadeGeometriaMapper {

    @Mapping(
            source = "geoJson",
            target = "geometria"
    )
    AtividadeGeometria toEntity(
            AtividadeGeometriaDto dto
    );

    @Mapping(
            source = "geometria",
            target = "geoJson"
    )
    AtividadeGeometriaDto toDto(
            AtividadeGeometria entity
    );
}
