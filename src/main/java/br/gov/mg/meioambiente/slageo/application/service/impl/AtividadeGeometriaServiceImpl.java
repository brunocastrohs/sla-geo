package br.gov.mg.meioambiente.slageo.application.service.impl;

import br.gov.mg.meioambiente.slageo.application.dto.AtividadeGeometriaDto;
import br.gov.mg.meioambiente.slageo.application.mapper.AtividadeGeometriaMapper;
import br.gov.mg.meioambiente.slageo.application.service.interfaces.AtividadeGeometriaService;
import br.gov.mg.meioambiente.slageo.model.AtividadeGeometria;
import br.gov.mg.meioambiente.slageo.repository.interfaces.AtividadeGeometriaRepository;

import org.locationtech.jts.geom.Point;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AtividadeGeometriaServiceImpl
        implements AtividadeGeometriaService {

    private final AtividadeGeometriaRepository repository;
    private final AtividadeGeometriaMapper mapper;

    public AtividadeGeometriaServiceImpl(
            AtividadeGeometriaRepository repository,
            AtividadeGeometriaMapper mapper) {

        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    @Transactional
    public AtividadeGeometriaDto salvar(
            AtividadeGeometriaDto dto) {

        if (repository.existsById(
                dto.getIdAtividade())) {

            throw new IllegalArgumentException(
                    "Já existe geometria para a atividade "
                            + dto.getIdAtividade()
                            + "."
            );
        }

        AtividadeGeometria entity =
                mapper.toEntity(dto);

        return mapper.toDto(
                repository.save(entity)
        );
    }

    @Override
    @Transactional
    public AtividadeGeometriaDto atualizar(
            AtividadeGeometriaDto dto) {

        if (!repository.existsById(
                dto.getIdAtividade())) {

            throw new IllegalArgumentException(
                    "Geometria não encontrada para a atividade "
                            + dto.getIdAtividade()
                            + "."
            );
        }

        AtividadeGeometria entity =
                mapper.toEntity(dto);

        return mapper.toDto(
                repository.save(entity)
        );
    }

    @Override
    @Transactional
    public AtividadeGeometriaDto salvarOuAtualizar(
            AtividadeGeometriaDto dto) {

        return mapper.toDto(
                repository.save(
                        mapper.toEntity(dto)
                )
        );
    }

    @Override
    @Transactional(readOnly = true)
    public AtividadeGeometriaDto buscarPorIdAtividade(
            Long idAtividade) {

        return repository
                .findById(idAtividade)
                .map(mapper::toDto)
                .orElseThrow(
                        () -> new IllegalArgumentException(
                                "Geometria não encontrada para a atividade "
                                        + idAtividade
                                        + "."
                        )
                );
    }

    @Override
    @Transactional(readOnly = true)
    public List<AtividadeGeometriaDto> buscarPorIdSolic(
            Long idSolic) {

        return repository
                .findByIdSolic(idSolic)
                .stream()
                .map(mapper::toDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AtividadeGeometriaDto> buscarTodos() {

        return repository
                .findAll()
                .stream()
                .map(mapper::toDto)
                .toList();
    }

    @Override
    @Transactional
    public void remover(
            Long idAtividade) {

        if (!repository.existsById(idAtividade)) {

            throw new IllegalArgumentException(
                    "Geometria não encontrada para a atividade "
                            + idAtividade
                            + "."
            );
        }

        repository.deleteById(idAtividade);
    }

    @Override
    @Transactional(readOnly = true)
    public double[] buscarCentro(
            Long idAtividade) {

        AtividadeGeometria atividade =
                repository
                        .findById(idAtividade)
                        .orElseThrow(
                                () -> new IllegalArgumentException(
                                        "Geometria não encontrada para a atividade "
                                                + idAtividade
                                                + "."
                                )
                        );

        if (atividade.getGeometria() == null
                || atividade.getGeometria().isEmpty()) {

            throw new IllegalArgumentException(
                    "A atividade não possui geometria."
            );
        }

        Point ponto =
                atividade
                        .getGeometria()
                        .getInteriorPoint();

        return new double[]{
                ponto.getX(),
                ponto.getY()
        };
    }
}
