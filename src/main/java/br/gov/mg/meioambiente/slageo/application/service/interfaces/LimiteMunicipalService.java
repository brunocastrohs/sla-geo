package br.gov.mg.meioambiente.slageo.application.service.interfaces;

import br.gov.mg.meioambiente.slageo.application.dto.AtividadeGeometriaDto;
import br.gov.mg.meioambiente.slageo.application.dto.MunicipioIntersecaoDto;

import java.util.List;

public interface LimiteMunicipalService {

    boolean intersectaMaisDeUmMunicipio(
            AtividadeGeometriaDto dto
    );

    List<MunicipioIntersecaoDto>
    buscarMunicipiosIntersectados(
            AtividadeGeometriaDto dto
    );
}
