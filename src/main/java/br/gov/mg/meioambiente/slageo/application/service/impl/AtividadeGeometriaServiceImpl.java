package br.gov.mg.meioambiente.slageo.application.service.impl;

import br.gov.mg.meioambiente.slageo.application.dto.AtividadeGeometriaDto;
import br.gov.mg.meioambiente.slageo.application.mapper.AtividadeGeometriaMapper;
import br.gov.mg.meioambiente.slageo.application.service.interfaces.AtividadeGeometriaService;
import br.gov.mg.meioambiente.slageo.application.service.interfaces.GeoJsonValidationService;
import br.gov.mg.meioambiente.slageo.model.AtividadeGeometria;
import br.gov.mg.meioambiente.slageo.repository.interfaces.AtividadeGeometriaRepository;
import br.gov.mg.meioambiente.slageo.repository.interfaces.LimiteEstadualRepository;
import br.gov.mg.meioambiente.slageo.repository.interfaces.LimiteMunicipalRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AtividadeGeometriaServiceImpl
        implements AtividadeGeometriaService {

    private final AtividadeGeometriaRepository atividadeGeometriaRepository;
    private final LimiteEstadualRepository limiteEstadualRepository;
    private final LimiteMunicipalRepository limiteMunicipalRepository;
    private final AtividadeGeometriaMapper mapper;
    private final GeoJsonValidationService geoJsonValidationService;

    public AtividadeGeometriaServiceImpl(
            AtividadeGeometriaRepository atividadeGeometriaRepository,
            LimiteEstadualRepository limiteEstadualRepository,
            LimiteMunicipalRepository limiteMunicipalRepository,
            AtividadeGeometriaMapper mapper,
            GeoJsonValidationService geoJsonValidationService) {

        this.atividadeGeometriaRepository =
                atividadeGeometriaRepository;

        this.limiteEstadualRepository =
                limiteEstadualRepository;

        this.limiteMunicipalRepository =
                limiteMunicipalRepository;

        this.mapper = mapper;

        this.geoJsonValidationService =
                geoJsonValidationService;
    }

    @Override
    @Transactional(readOnly = true)
    public double[] buscarCentro(
            Long idAtividade) {

        AtividadeGeometria atividade =
                atividadeGeometriaRepository
                        .findById(idAtividade)
                        .orElseThrow(
                                () -> new IllegalArgumentException(
                                        "Geometria não encontrada para a atividade "
                                                + idAtividade
                                )
                        );

        var ponto =
                atividade
                        .getGeometria()
                        .getInteriorPoint();

        return new double[]{
                ponto.getX(),
                ponto.getY()
        };
    }

    @Override
    @Transactional
    public AtividadeGeometriaDto salvar(
            AtividadeGeometriaDto dto) {

        validarGeoJson(dto);

        if (atividadeGeometriaRepository.existsById(
                dto.getIdAtividade())) {

            throw new IllegalArgumentException(
                    "Já existe geometria para a atividade "
                            + dto.getIdAtividade()
                            + "."
            );
        }

        AtividadeGeometria entity =
                mapper.toEntity(dto);

        AtividadeGeometria salva =
                atividadeGeometriaRepository.save(entity);

        return mapper.toDto(salva);
    }

    @Override
    @Transactional
    public AtividadeGeometriaDto atualizar(
            AtividadeGeometriaDto dto) {

        validarGeoJson(dto);

        if (!atividadeGeometriaRepository.existsById(
                dto.getIdAtividade())) {

            throw new IllegalArgumentException(
                    "Não existe geometria para a atividade "
                            + dto.getIdAtividade()
                            + "."
            );
        }

        AtividadeGeometria entity =
                mapper.toEntity(dto);

        AtividadeGeometria atualizada =
                atividadeGeometriaRepository.save(entity);

        return mapper.toDto(atualizada);
    }

    @Override
    @Transactional
    public AtividadeGeometriaDto salvarOuAtualizar(
            AtividadeGeometriaDto dto) {

        validarGeoJson(dto);

        AtividadeGeometria entity =
                mapper.toEntity(dto);

        AtividadeGeometria salva =
                atividadeGeometriaRepository.save(entity);

        return mapper.toDto(salva);
    }

    @Override
    @Transactional(readOnly = true)
    public AtividadeGeometriaDto buscarPorIdAtividade(
            Long idAtividade) {

        return atividadeGeometriaRepository
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

        return atividadeGeometriaRepository
                .findByIdSolic(idSolic)
                .stream()
                .map(mapper::toDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AtividadeGeometriaDto> buscarTodos() {

        return atividadeGeometriaRepository
                .findAll()
                .stream()
                .map(mapper::toDto)
                .toList();
    }

    @Override
    @Transactional
    public void remover(
            Long idAtividade) {

        if (!atividadeGeometriaRepository.existsById(
                idAtividade)) {

            throw new IllegalArgumentException(
                    "Geometria não encontrada para a atividade "
                            + idAtividade
                            + "."
            );
        }

        atividadeGeometriaRepository.deleteById(
                idAtividade
        );
    }

    @Override
    @Transactional(readOnly = true)
    public boolean estaIntegralmenteEmMinasGerais(
            AtividadeGeometriaDto dto) {

        String geoJson = obterGeoJsonValidado(dto);

        return limiteEstadualRepository
                .estaIntegralmenteEmMinasGerais(
                        geoJson
                );
    }

    @Override
    @Transactional(readOnly = true)
    public long contarMunicipiosIntersectados(
            AtividadeGeometriaDto dto) {

        String geoJson = obterGeoJsonValidado(dto);

        return limiteMunicipalRepository
                .contarMunicipiosIntersectados(
                        geoJson
                );
    }

    @Override
    @Transactional(readOnly = true)
    public boolean intersectaMaisDeUmMunicipio(
            AtividadeGeometriaDto dto) {

        String geoJson = obterGeoJsonValidado(dto);

        return limiteMunicipalRepository
                .intersectaMaisDeUmMunicipio(
                        geoJson
                );
    }

    @Override
    @Transactional(readOnly = true)
    public List<LimiteMunicipalRepository.MunicipioIntersecao>
    buscarMunicipiosIntersectados(
            AtividadeGeometriaDto dto) {

        String geoJson = obterGeoJsonValidado(dto);

        return limiteMunicipalRepository
                .buscarMunicipiosIntersectados(
                        geoJson
                );
    }

    private void validarGeoJson(
            AtividadeGeometriaDto dto) {

        obterGeoJsonValidado(dto);
    }

    private String obterGeoJsonValidado(
            AtividadeGeometriaDto dto) {

        if (dto == null) {
            throw new IllegalArgumentException(
                    "Os dados da atividade são obrigatórios."
            );
        }

        if (dto.getGeoJson() == null) {
            throw new IllegalArgumentException(
                    "O GeoJSON é obrigatório."
            );
        }

        String geoJson =
                dto.getGeoJson().toString();

        geoJsonValidationService.validar(
                geoJson
        );

        return geoJson;
    }
}
