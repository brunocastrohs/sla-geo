package br.gov.mg.meioambiente.slageo.application.service.impl;

import br.gov.mg.meioambiente.slageo.application.dto.DenominacaoGeometriaDto;
import br.gov.mg.meioambiente.slageo.application.mapper.DenominacaoGeometriaMapper;
import br.gov.mg.meioambiente.slageo.application.mapper.GeoJsonGeometryMapper;
import br.gov.mg.meioambiente.slageo.application.service.interfaces.AtividadeConsolidacaoService;
import br.gov.mg.meioambiente.slageo.application.service.interfaces.DenominacaoGeometriaService;
import br.gov.mg.meioambiente.slageo.application.service.interfaces.GeoJsonValidationService;
import br.gov.mg.meioambiente.slageo.model.DenominacaoGeometria;
import br.gov.mg.meioambiente.slageo.model.enums.DenominacaoEnum;
import br.gov.mg.meioambiente.slageo.repository.interfaces.DenominacaoGeometriaRepository;
import br.gov.mg.meioambiente.slageo.repository.interfaces.LimiteEstadualRepository;
import br.gov.mg.meioambiente.slageo.repository.interfaces.LimiteMunicipalRepository;

import org.locationtech.jts.geom.Point;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class DenominacaoGeometriaServiceImpl
        implements DenominacaoGeometriaService {

    private final DenominacaoGeometriaRepository
            repository;

    private final LimiteEstadualRepository
            limiteEstadualRepository;

    private final LimiteMunicipalRepository
            limiteMunicipalRepository;

    private final DenominacaoGeometriaMapper
            mapper;

    private final GeoJsonGeometryMapper
            geoJsonGeometryMapper;

    private final GeoJsonValidationService
            geoJsonValidationService;

    private final AtividadeConsolidacaoService
            atividadeConsolidacaoService;

    public DenominacaoGeometriaServiceImpl(
            DenominacaoGeometriaRepository repository,
            LimiteEstadualRepository limiteEstadualRepository,
            LimiteMunicipalRepository limiteMunicipalRepository,
            DenominacaoGeometriaMapper mapper,
            GeoJsonGeometryMapper geoJsonGeometryMapper,
            GeoJsonValidationService geoJsonValidationService,
            AtividadeConsolidacaoService atividadeConsolidacaoService) {

        this.repository =
                repository;

        this.limiteEstadualRepository =
                limiteEstadualRepository;

        this.limiteMunicipalRepository =
                limiteMunicipalRepository;

        this.mapper =
                mapper;

        this.geoJsonGeometryMapper =
                geoJsonGeometryMapper;

        this.geoJsonValidationService =
                geoJsonValidationService;

        this.atividadeConsolidacaoService =
                atividadeConsolidacaoService;
    }

    @Override
    @Transactional
    public DenominacaoGeometriaDto salvar(
            DenominacaoGeometriaDto dto) {

        validarDto(dto);

        DenominacaoGeometria entity =
                mapper.toEntity(dto);

        entity.setIdDenominacao(null);
        entity.setDataExcl(null);

        DenominacaoGeometria salva =
                repository.saveAndFlush(
                        entity
                );

        atividadeConsolidacaoService
                .criarOuAtualizar(
                        salva.getIdSolic()
                );

        return mapper.toDto(
                salva
        );
    }

    @Override
    @Transactional
    public DenominacaoGeometriaDto atualizar(
            Long idDenominacao,
            DenominacaoGeometriaDto dto) {

        validarDto(dto);

        DenominacaoGeometria existente =
                buscarEntidadeAtiva(
                        idDenominacao
                );

        Integer idSolicAnterior =
                existente.getIdSolic();

        existente.setIdSolic(
                dto.getIdSolic()
        );

        existente.setDenominacao(
                DenominacaoEnum.fromId(
                        dto.getIdDenom()
                )
        );

        existente.setGeometria(
                geoJsonGeometryMapper
                        .toGeometry(
                                dto.getGeoJson()
                        )
        );

        existente.setGeoHash(
                dto.getGeoHash()
        );

        DenominacaoGeometria atualizada =
                repository.saveAndFlush(
                        existente
                );

        if (idSolicAnterior.equals(
                atualizada.getIdSolic())) {

            atividadeConsolidacaoService
                    .atualizarSeExistir(
                            idSolicAnterior
                    );

        } else {

            atividadeConsolidacaoService
                    .atualizarSeExistir(
                            idSolicAnterior
                    );

            atividadeConsolidacaoService
                    .criarOuAtualizar(
                            atualizada.getIdSolic()
                    );
        }

        return mapper.toDto(
                atualizada
        );
    }

    @Override
    @Transactional(readOnly = true)
    public DenominacaoGeometriaDto buscarPorId(
            Long idDenominacao) {

        return mapper.toDto(
                buscarEntidadeAtiva(
                        idDenominacao
                )
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<DenominacaoGeometriaDto>
    buscarPorIdSolic(
            Integer idSolic) {

        return repository
                .findByIdSolicAndDataExclIsNull(
                        idSolic
                )
                .stream()
                .map(mapper::toDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<DenominacaoGeometriaDto>
    buscarTodos() {

        return repository
                .findByDataExclIsNull()
                .stream()
                .map(mapper::toDto)
                .toList();
    }

    @Override
    @Transactional
    public void remover(
            Long idDenominacao) {

        DenominacaoGeometria entity =
                buscarEntidadeAtiva(
                        idDenominacao
                );

        Integer idSolic =
                entity.getIdSolic();

        entity.setDataExcl(
                LocalDateTime.now()
        );

        repository.saveAndFlush(
                entity
        );

        atividadeConsolidacaoService
                .atualizarSeExistir(
                        idSolic
                );
    }

    @Override
    @Transactional(readOnly = true)
    public double[] buscarCentro(
            Long idDenominacao) {

        DenominacaoGeometria entity =
                buscarEntidadeAtiva(
                        idDenominacao
                );

        if (entity.getGeometria() == null
                || entity.getGeometria().isEmpty()) {

            throw new IllegalArgumentException(
                    "A denominação não possui geometria."
            );
        }

        Point ponto =
                entity
                        .getGeometria()
                        .getInteriorPoint();

        return new double[]{
                ponto.getX(),
                ponto.getY()
        };
    }

    @Override
    @Transactional(readOnly = true)
    public boolean estaIntegralmenteEmMinasGerais(
            DenominacaoGeometriaDto dto) {

        String geoJson =
                obterGeoJsonValidado(
                        dto
                );

        return limiteEstadualRepository
                .estaIntegralmenteEmMinasGerais(
                        geoJson
                );
    }

    @Override
    @Transactional(readOnly = true)
    public long contarMunicipiosIntersectados(
            DenominacaoGeometriaDto dto) {

        String geoJson =
                obterGeoJsonValidado(
                        dto
                );

        return limiteMunicipalRepository
                .contarMunicipiosIntersectados(
                        geoJson
                );
    }

    @Override
    @Transactional(readOnly = true)
    public boolean intersectaMaisDeUmMunicipio(
            DenominacaoGeometriaDto dto) {

        String geoJson =
                obterGeoJsonValidado(
                        dto
                );

        return limiteMunicipalRepository
                .intersectaMaisDeUmMunicipio(
                        geoJson
                );
    }

    @Override
    @Transactional(readOnly = true)
    public List<LimiteMunicipalRepository.MunicipioIntersecao>
    buscarMunicipiosIntersectados(
            DenominacaoGeometriaDto dto) {

        String geoJson =
                obterGeoJsonValidado(
                        dto
                );

        return limiteMunicipalRepository
                .buscarMunicipiosIntersectados(
                        geoJson
                );
    }

    private void validarDto(
            DenominacaoGeometriaDto dto) {

        if (dto == null) {

            throw new IllegalArgumentException(
                    "Os dados da denominação são obrigatórios."
            );
        }

        if (dto.getIdSolic() == null) {

            throw new IllegalArgumentException(
                    "O idSolic é obrigatório."
            );
        }

        if (dto.getIdDenom() == null) {

            throw new IllegalArgumentException(
                    "O idDenom é obrigatório."
            );
        }

        DenominacaoEnum.fromId(
                dto.getIdDenom()
        );

        obterGeoJsonValidado(
                dto
        );
    }

    private String obterGeoJsonValidado(
            DenominacaoGeometriaDto dto) {

        if (dto == null) {

            throw new IllegalArgumentException(
                    "Os dados da denominação são obrigatórios."
            );
        }

        if (dto.getGeoJson() == null) {

            throw new IllegalArgumentException(
                    "O GeoJSON é obrigatório."
            );
        }

        String geoJson =
                dto.getGeoJson()
                        .toString();

        geoJsonValidationService
                .validar(
                        geoJson
                );

        return geoJson;
    }

    private DenominacaoGeometria buscarEntidadeAtiva(
            Long idDenominacao) {

        DenominacaoGeometria entity =
                repository
                        .findById(
                                idDenominacao
                        )
                        .orElseThrow(
                                () ->
                                        new IllegalArgumentException(
                                                "Denominação não encontrada: "
                                                        + idDenominacao
                                                        + "."
                                        )
                        );

        if (entity.getDataExcl() != null) {

            throw new IllegalArgumentException(
                    "Denominação não encontrada: "
                            + idDenominacao
                            + "."
            );
        }

        return entity;
    }
}
