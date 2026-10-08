package br.gov.mg.meioambiente.slageo.application.service.impl;

import br.gov.mg.meioambiente.slageo.application.dto.ArquivoDenominacaoDto;
import br.gov.mg.meioambiente.slageo.application.dto.DenominacaoGeometriaDto;
import br.gov.mg.meioambiente.slageo.application.mapper.DenominacaoGeometriaMapper;
import br.gov.mg.meioambiente.slageo.application.mapper.GeoJsonGeometryMapper;
import br.gov.mg.meioambiente.slageo.application.service.interfaces.AtividadeConsolidacaoService;
import br.gov.mg.meioambiente.slageo.application.service.interfaces.DenominacaoGeometriaService;
import br.gov.mg.meioambiente.slageo.application.service.interfaces.GeoJsonValidationService;
import br.gov.mg.meioambiente.slageo.integration.minio.MinioStorageClient;
import br.gov.mg.meioambiente.slageo.model.DenominacaoGeometria;
import br.gov.mg.meioambiente.slageo.model.enums.DenominacaoEnum;
import br.gov.mg.meioambiente.slageo.repository.interfaces.DenominacaoGeometriaRepository;
import br.gov.mg.meioambiente.slageo.repository.interfaces.LimiteEstadualRepository;
import br.gov.mg.meioambiente.slageo.repository.interfaces.LimiteMunicipalRepository;

import org.locationtech.jts.geom.Point;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class DenominacaoGeometriaServiceImpl
        implements DenominacaoGeometriaService {

    private static final DateTimeFormatter ARQUIVO_TIMESTAMP =
            DateTimeFormatter.ofPattern(
                    "yyyyMMdd'T'HHmmssSSS"
            );

    private final DenominacaoGeometriaRepository repository;

    private final LimiteEstadualRepository
            limiteEstadualRepository;

    private final LimiteMunicipalRepository
            limiteMunicipalRepository;

    private final DenominacaoGeometriaMapper mapper;

    private final GeoJsonGeometryMapper
            geoJsonGeometryMapper;

    private final GeoJsonValidationService
            geoJsonValidationService;

    private final AtividadeConsolidacaoService
            atividadeConsolidacaoService;

    private final MinioStorageClient
            minioStorageClient;

    public DenominacaoGeometriaServiceImpl(
            DenominacaoGeometriaRepository repository,
            LimiteEstadualRepository limiteEstadualRepository,
            LimiteMunicipalRepository limiteMunicipalRepository,
            DenominacaoGeometriaMapper mapper,
            GeoJsonGeometryMapper geoJsonGeometryMapper,
            GeoJsonValidationService geoJsonValidationService,
            AtividadeConsolidacaoService atividadeConsolidacaoService,
            MinioStorageClient minioStorageClient) {

        this.repository = repository;
        this.limiteEstadualRepository =
                limiteEstadualRepository;
        this.limiteMunicipalRepository =
                limiteMunicipalRepository;
        this.mapper = mapper;
        this.geoJsonGeometryMapper =
                geoJsonGeometryMapper;
        this.geoJsonValidationService =
                geoJsonValidationService;
        this.atividadeConsolidacaoService =
                atividadeConsolidacaoService;
        this.minioStorageClient =
                minioStorageClient;
    }

    @Override
    @Transactional
    public DenominacaoGeometriaDto salvar(
            DenominacaoGeometriaDto dto,
            MultipartFile arquivo) {

        validarDto(dto);
        validarArquivoObrigatorio(arquivo);

        String arquivoId =
                gerarArquivoId(
                        dto.getIdSolic(),
                        dto.getIdDenom()
                );

        salvarArquivoMinio(
                arquivoId,
                arquivo
        );

        try {
            DenominacaoGeometria entity =
                    mapper.toEntity(dto);

            entity.setIdDenominacao(null);
            entity.setDataExcl(null);
            entity.setArquivoId(arquivoId);

            DenominacaoGeometria salva =
                    repository.saveAndFlush(entity);

            atividadeConsolidacaoService
                    .criarOuAtualizar(
                            salva.getIdSolic()
                    );

            return mapper.toDto(salva);

        } catch (RuntimeException e) {

            removerArquivoSilenciosamente(
                    arquivoId
            );

            throw e;
        }
    }

    @Override
    @Transactional
    public DenominacaoGeometriaDto atualizar(
            Long idDenominacao,
            DenominacaoGeometriaDto dto,
            MultipartFile arquivo) {

        validarDto(dto);

        DenominacaoGeometria existente =
                buscarEntidadeAtiva(
                        idDenominacao
                );

        Integer idSolicAnterior =
                existente.getIdSolic();

        String arquivoAnterior =
                existente.getArquivoId();

        String novoArquivoId = null;

        if (arquivo != null
                && !arquivo.isEmpty()) {

            validarArquivoZip(arquivo);

            novoArquivoId =
                    gerarArquivoId(
                            dto.getIdSolic(),
                            dto.getIdDenom()
                    );

            salvarArquivoMinio(
                    novoArquivoId,
                    arquivo
            );
        }

        try {
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

            if (novoArquivoId != null) {
                existente.setArquivoId(
                        novoArquivoId
                );
            }

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

            if (novoArquivoId != null
                    && arquivoAnterior != null
                    && !arquivoAnterior.isBlank()
                    && !arquivoAnterior.equals(
                            novoArquivoId)) {

                minioStorageClient.remover(
                        arquivoAnterior
                );
            }

            return mapper.toDto(
                    atualizada
            );

        } catch (RuntimeException e) {

            if (novoArquivoId != null) {
                removerArquivoSilenciosamente(
                        novoArquivoId
                );
            }

            throw e;
        }
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

        String arquivoId =
                entity.getArquivoId();

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

        if (arquivoId != null
                && !arquivoId.isBlank()) {

            minioStorageClient.remover(
                    arquivoId
            );
        }
    }

    @Override
    @Transactional(readOnly = true)
    public ArquivoDenominacaoDto recuperarArquivo(
            Long idDenominacao) {

        DenominacaoGeometria entity =
                buscarEntidadeAtiva(
                        idDenominacao
                );

        String arquivoId =
                entity.getArquivoId();

        if (arquivoId == null
                || arquivoId.isBlank()) {

            throw new IllegalArgumentException(
                    "A denominação não possui arquivo associado."
            );
        }

        if (!minioStorageClient.existe(
                arquivoId)) {

            throw new IllegalArgumentException(
                    "O arquivo associado à denominação não foi encontrado."
            );
        }

        try (
                InputStream inputStream =
                        minioStorageClient
                                .recuperar(
                                        arquivoId
                                )
        ) {

            return new ArquivoDenominacaoDto(
                    arquivoId,
                    inputStream.readAllBytes()
            );

        } catch (IOException e) {

            throw new IllegalStateException(
                    "Não foi possível ler o arquivo da denominação.",
                    e
            );
        }
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
                obterGeoJsonValidado(dto);

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
                obterGeoJsonValidado(dto);

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
                obterGeoJsonValidado(dto);

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
                obterGeoJsonValidado(dto);

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

        obterGeoJsonValidado(dto);
    }

    private void validarArquivoObrigatorio(
            MultipartFile arquivo) {

        if (arquivo == null
                || arquivo.isEmpty()) {

            throw new IllegalArgumentException(
                    "O arquivo ZIP é obrigatório."
            );
        }

        validarArquivoZip(arquivo);
    }

    private void validarArquivoZip(
            MultipartFile arquivo) {

        String nome =
                arquivo.getOriginalFilename();

        if (nome == null
                || !nome.toLowerCase()
                        .endsWith(".zip")) {

            throw new IllegalArgumentException(
                    "O arquivo deve possuir extensão .zip."
            );
        }
    }

    private String gerarArquivoId(
            Integer idSolic,
            Integer idDenom) {

        String timestamp =
                LocalDateTime.now()
                        .format(
                                ARQUIVO_TIMESTAMP
                        );

        return idSolic
                + "_"
                + timestamp
                + "_"
                + idDenom
                + ".zip";
    }

    private void salvarArquivoMinio(
            String arquivoId,
            MultipartFile arquivo) {

        try (
                InputStream inputStream =
                        arquivo.getInputStream()
        ) {

            minioStorageClient.salvar(
                    arquivoId,
                    inputStream,
                    arquivo.getSize(),
                    "application/zip"
            );

        } catch (IOException e) {

            throw new IllegalStateException(
                    "Não foi possível ler o arquivo ZIP.",
                    e
            );
        }
    }

    private void removerArquivoSilenciosamente(
            String arquivoId) {

        try {
            if (arquivoId != null
                    && minioStorageClient
                            .existe(arquivoId)) {

                minioStorageClient.remover(
                        arquivoId
                );
            }

        } catch (RuntimeException ignored) {
            // Compensação best-effort.
        }
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
