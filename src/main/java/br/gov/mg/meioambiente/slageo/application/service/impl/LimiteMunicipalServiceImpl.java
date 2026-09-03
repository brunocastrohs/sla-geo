package br.gov.mg.meioambiente.slageo.application.service.impl;

import br.gov.mg.meioambiente.slageo.application.dto.AtividadeGeometriaDto;
import br.gov.mg.meioambiente.slageo.application.dto.MunicipioIntersecaoDto;
import br.gov.mg.meioambiente.slageo.application.service.interfaces.LimiteMunicipalService;
import br.gov.mg.meioambiente.slageo.repository.interfaces.LimiteMunicipalRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class LimiteMunicipalServiceImpl
        implements LimiteMunicipalService {

    private final LimiteMunicipalRepository repository;

    public LimiteMunicipalServiceImpl(
            LimiteMunicipalRepository repository) {

        this.repository = repository;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean intersectaMaisDeUmMunicipio(
            AtividadeGeometriaDto dto) {

        return repository.intersectaMaisDeUmMunicipio(
                dto.getGeoJson().toString()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<MunicipioIntersecaoDto>
    buscarMunicipiosIntersectados(
            AtividadeGeometriaDto dto) {

        return repository
                .buscarMunicipiosIntersectados(
                        dto.getGeoJson().toString()
                )
                .stream()
                .map(municipio ->
                        new MunicipioIntersecaoDto(
                                municipio.codigoIbge(),
                                municipio.nome(),
                                municipio.areaIntersecaoM2()
                        )
                )
                .toList();
    }
}
