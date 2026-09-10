package br.gov.mg.meioambiente.slageo.application.service.impl;

import br.gov.mg.meioambiente.slageo.application.mapper.GeoJsonGeometryMapper;
import br.gov.mg.meioambiente.slageo.application.service.interfaces.CriteriosLocacionaisService;
import br.gov.mg.meioambiente.slageo.integration.ide.IdeSisemaClient;
import br.gov.mg.meioambiente.slageo.model.DenominacaoGeometria;
import br.gov.mg.meioambiente.slageo.repository.interfaces.CriteriosLocacionaisRepository;
import br.gov.mg.meioambiente.slageo.repository.interfaces.DenominacaoGeometriaRepository;

import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.MultiPoint;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Point;

import org.springframework.stereotype.Service;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;
import tools.jackson.databind.json.JsonMapper;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
public class CriteriosLocacionaisServiceImpl
        implements CriteriosLocacionaisService {

    private final DenominacaoGeometriaRepository
            denominacaoRepository;

    private final CriteriosLocacionaisRepository
            criteriosRepository;

    private final GeoJsonGeometryMapper
            geoJsonGeometryMapper;

    private final IdeSisemaClient
            ideSisemaClient;

    private final JsonMapper
            jsonMapper;

    public CriteriosLocacionaisServiceImpl(
            DenominacaoGeometriaRepository denominacaoRepository,
            CriteriosLocacionaisRepository criteriosRepository,
            GeoJsonGeometryMapper geoJsonGeometryMapper,
            IdeSisemaClient ideSisemaClient) {

        this.denominacaoRepository =
                denominacaoRepository;

        this.criteriosRepository =
                criteriosRepository;

        this.geoJsonGeometryMapper =
                geoJsonGeometryMapper;

        this.ideSisemaClient =
                ideSisemaClient;

        this.jsonMapper =
                JsonMapper.builder().build();
    }

    @Override
    public JsonNode calcular(
            Long idDenominacao) {

        DenominacaoGeometria denominacao =
                denominacaoRepository
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

        if (denominacao.getDataExcl() != null) {

            throw new IllegalArgumentException(
                    "Denominação não encontrada: "
                            + idDenominacao
                            + "."
            );
        }

        if (denominacao.getGeometria() == null
                || denominacao.getGeometria().isEmpty()) {

            throw new IllegalArgumentException(
                    "A denominação não possui geometria."
            );
        }

        Integer idDenom =
                denominacao
                        .getDenominacao()
                        .getId();

        List<JsonNode> geometrias =
                prepararGeometrias(
                        denominacao.getGeometria(),
                        idDenom
                );

        ArrayNode resultados =
                jsonMapper.createArrayNode();

        int quantidadeIdentificados = 0;

        for (Regra regra : regras()) {

            if (!aplicavel(
                    idDenom,
                    regra.idPergunta())) {

                continue;
            }

            for (String camada :
                    regra.camadas()) {

                String operacao =
                        obterOperacao(
                                idDenom,
                                regra.idPergunta()
                        );

                Integer distancia =
                        obterDistancia(
                                idDenom,
                                regra.idPergunta()
                        );

                JsonNode resposta =
                        ideSisemaClient.consultar(
                                camada,
                                operacao,
                                distancia,
                                !retornaSomenteYesNo(
                                        regra.idPergunta()
                                ),
                                geometrias
                        );

                ResultadoComparacao comparacao =
                        analisarResposta(
                                regra,
                                resposta
                        );

                if (comparacao.identificado()) {
                    quantidadeIdentificados++;
                }

                ObjectNode resultado =
                        jsonMapper.createObjectNode();

                resultado.put(
                        "idPergunta",
                        regra.idPergunta()
                );

                resultado.put(
                        "camada",
                        camada
                );

                resultado.put(
                        "operacao",
                        operacao
                );

                if (distancia != null) {

                    resultado.put(
                            "distancia",
                            distancia
                    );
                }

                resultado.put(
                        "resultado",
                        comparacao.resultado()
                );

                resultado.put(
                        "criterioIdentificado",
                        comparacao.identificado()
                );

                if (comparacao.detalhes() != null) {

                    resultado.set(
                            "detalhes",
                            comparacao.detalhes()
                    );
                }

                resultados.add(
                        resultado
                );
            }
        }

        ObjectNode documento =
                jsonMapper.createObjectNode();

        documento.put(
                "idDenominacao",
                denominacao.getIdDenominacao()
        );

        documento.put(
                "idSolic",
                denominacao.getIdSolic()
        );

        documento.put(
                "idDenom",
                idDenom
        );

        documento.put(
                "denominacao",
                denominacao
                        .getDenominacao()
                        .getDescricao()
        );

        documento.put(
                "executadoEm",
                LocalDateTime.now()
                        .toString()
        );

        documento.put(
                "quantidadeConsultas",
                resultados.size()
        );

        documento.put(
                "quantidadeCriteriosIdentificados",
                quantidadeIdentificados
        );

        documento.set(
                "criterios",
                resultados
        );

        criteriosRepository.salvar(
                idDenominacao,
                documento.toString()
        );

        return documento;
    }

    @Override
    public JsonNode buscar(
            Long idDenominacao) {

        String resultado =
                criteriosRepository.buscar(
                        idDenominacao
                );

        if (resultado == null
                || resultado.isBlank()) {

            return jsonMapper
                    .nullNode();
        }

        try {

            return jsonMapper.readTree(
                    resultado
            );

        } catch (Exception e) {

            throw new IllegalStateException(
                    "Não foi possível ler os critérios locacionais armazenados.",
                    e
            );
        }
    }

    private ResultadoComparacao analisarResposta(
            Regra regra,
            JsonNode resposta) {

        if (resposta == null
                || !resposta.isArray()
                || resposta.isEmpty()) {

            return new ResultadoComparacao(
                    "No",
                    false,
                    null
            );
        }

        boolean encontrouYes = false;
        boolean identificado = false;

        ArrayNode detalhesAgregados =
                jsonMapper.createArrayNode();

        for (JsonNode item : resposta) {

            JsonNode result =
                    item.get(
                            "result"
                    );

            if (result == null
                    || !result.isArray()
                    || result.isEmpty()) {

                continue;
            }

            String yesNo =
                    result.get(0)
                            .asText();

            if (!"Yes".equalsIgnoreCase(
                    yesNo)) {

                continue;
            }

            encontrouYes = true;

            JsonNode detalhes =
                    result.size() > 1
                            ? result.get(1)
                            : null;

            if (detalhes != null
                    && !detalhes.isNull()) {

                detalhesAgregados.add(
                        detalhes
                );
            }

            if (passaFiltroAtributos(
                    regra,
                    detalhes)) {

                identificado = true;
            }
        }

        JsonNode detalhes =
                detalhesAgregados.isEmpty()
                        ? null
                        : detalhesAgregados;

        return new ResultadoComparacao(
                encontrouYes
                        ? "Yes"
                        : "No",
                identificado,
                detalhes
        );
    }

    private boolean passaFiltroAtributos(
            Regra regra,
            JsonNode detalhes) {

        if (regra.contains().isEmpty()
                && regra.notContains().isEmpty()) {

            return true;
        }

        String valor =
                detalhes == null
                        ? ""
                        : detalhes.toString();

        boolean possuiContains =
                regra.contains().isEmpty();

        for (String esperado :
                regra.contains()) {

            if (valor.contains(
                    esperado)) {

                possuiContains = true;
                break;
            }
        }

        if (!possuiContains) {
            return false;
        }

        for (String proibido :
                regra.notContains()) {

            if (valor.contains(
                    proibido)) {

                return false;
            }
        }

        return true;
    }

    private boolean aplicavel(
            Integer idDenom,
            Long idPergunta) {

        if (idDenom == 1) {

            return idPergunta != 63L
                    && idPergunta != 86L
                    && idPergunta != 46L
                    && idPergunta != 181L;
        }

        if (idDenom == 9) {

            return idPergunta == 63L;
        }

        if (idDenom == 11) {

            return idPergunta == 42L;
        }

        if (idDenom == 6) {

            return idPergunta == 86L
                    || idPergunta == 42L;
        }

        if (idDenom == 7) {

            return idPergunta == 46L;
        }

        if (idDenom == 8) {

            return idPergunta == 181L;
        }

        return false;
    }

    private String obterOperacao(
            Integer idDenom,
            Long idPergunta) {

        if (idDenom == 7
                && idPergunta == 46L) {

            return "within a distance";
        }

        return "intersects";
    }

    private Integer obterDistancia(
            Integer idDenom,
            Long idPergunta) {

        if (idDenom == 7
                && idPergunta == 46L) {

            return 50;
        }

        return null;
    }

    private boolean retornaSomenteYesNo(
            Long idPergunta) {

        return idPergunta == 82L
                || idPergunta == 83L
                || idPergunta == 86L
                || idPergunta == 88L
                || idPergunta == 181L;
    }

    private List<JsonNode> prepararGeometrias(
            Geometry geometry,
            Integer idDenom) {

        if (idDenom == 6
                || idDenom == 7
                || idDenom == 8) {

            return prepararPontos(
                    geometry
            );
        }

        if (geometry instanceof MultiPolygon) {

            List<JsonNode> resultado =
                    new ArrayList<>();

            for (int i = 0;
                 i < geometry.getNumGeometries();
                 i++) {

                Geometry componente =
                        geometry.getGeometryN(i);

                componente.setSRID(
                        4674
                );

                resultado.add(
                        geoJsonGeometryMapper
                                .toJsonNode(
                                        componente
                                )
                );
            }

            return resultado;
        }

        return List.of(
                geoJsonGeometryMapper
                        .toJsonNode(
                                geometry
                        )
        );
    }

    private List<JsonNode> prepararPontos(
            Geometry geometry) {

        if (geometry instanceof Point) {

            return List.of(
                    geoJsonGeometryMapper
                            .toJsonNode(
                                    geometry
                            )
            );
        }

        if (geometry instanceof MultiPoint) {

            List<JsonNode> resultado =
                    new ArrayList<>();

            for (int i = 0;
                 i < geometry.getNumGeometries();
                 i++) {

                Geometry ponto =
                        geometry.getGeometryN(i);

                ponto.setSRID(
                        4674
                );

                resultado.add(
                        geoJsonGeometryMapper
                                .toJsonNode(
                                        ponto
                                )
                );
            }

            return resultado;
        }

        throw new IllegalArgumentException(
                "A denominação "
                        + "6, 7 ou 8 deve possuir geometria Point ou MultiPoint."
        );
    }

    private List<Regra> regras() {

        List<Regra> regras =
                new ArrayList<>();

        regras.add(
                regra(
                        22L,
                        List.of(
                                "IDE:ide_2010_mg_unidades_conservacao_federais_pol",
                                "IDE:ide_2010_mg_unidades_conservacao_municipais_pol",
                                "IDE:ide_2010_mg_unidades_conservacao_estaduais_pol"
                        ),
                        List.of(
                                "Protecao Integral",
                                "Proteção Integral"
                        ),
                        List.of()
                )
        );

        regras.add(
                regra(
                        63L,
                        List.of(
                                "ide_2411_mg_areasprioritarias_mapa_geral_pol"
                        ),
                        List.of(
                                "EXTREMAMENTE ALTA",
                                "Extremamente Alta",
                                "ESPECIAL"
                        ),
                        List.of()
                )
        );

        regras.add(
                regra(
                        23L,
                        List.of(
                                "ide_2011_mg_amortecimento_uc_plano_manejo_pol",
                                "ide_2011_mg_amortecimento_uc_raio_3km_pol"
                        ),
                        List.of(
                                "Protecao Integral",
                                "Proteção Integral"
                        ),
                        List.of()
                )
        );

        regras.add(
                regra(
                        24L,
                        List.of(
                                "IDE:ide_2010_mg_unidades_conservacao_federais_pol",
                                "IDE:ide_2010_mg_unidades_conservacao_municipais_pol",
                                "ide_2010_mg_reservas_particulares_patrimonio_natural_pol",
                                "IDE:ide_2010_mg_unidades_conservacao_estaduais_pol"
                        ),
                        List.of(
                                "Uso Sustentavel",
                                "Uso Sustentável"
                        ),
                        List.of(
                                "APA",
                                "Área de Proteção Ambiental"
                        )
                )
        );

        regras.add(
                regra(
                        25L,
                        List.of(
                                "ide_2011_mg_amortecimento_uc_plano_manejo_pol",
                                "ide_2011_mg_amortecimento_uc_raio_3km_pol"
                        ),
                        List.of(
                                "Uso Sustentavel",
                                "Uso Sustentável"
                        ),
                        List.of()
                )
        );

        regras.add(
                regra(
                        26L,
                        List.of(
                                "IDE:ide_2010_mg_unidades_conservacao_federais_pol",
                                "IDE:ide_2010_mg_unidades_conservacao_municipais_pol",
                                "ide_2010_mg_reservas_particulares_patrimonio_natural_pol",
                                "IDE:ide_2010_mg_unidades_conservacao_estaduais_pol"
                        ),
                        List.of(
                                "APA",
                                "Área de Proteção Ambiental"
                        ),
                        List.of()
                )
        );

        regras.add(
                regra(
                        82L,
                        List.of(
                                "ide_2012_mg_reserva_biosfera_caatinga_pol",
                                "ide_2012_mg_reserva_biosfera_mata_atlantica_pol",
                                "ide_2012_mg_reserva_biosfera_serra_espinhaco_pol"
                        )
                )
        );

        regras.add(
                regra(
                        83L,
                        List.of(
                                "ide_2013_mg_corredor_ecologico_sossego_caratinga_pol",
                                "ide_2013_mg_corredor_ecologico_espinhaco_serra_curral_pol",
                                "ide_2013_mg_corredor_ecologico_serra_moeda_aredes_pol"
                        )
                )
        );

        regras.add(
                regra(
                        84L,
                        List.of(
                                "ide_2016_mg_sitios_ramsar_pol"
                        )
                )
        );

        regras.add(
                regra(
                        85L,
                        List.of(
                                "ide_2008_mg_bacia_enquadrada_classe_especial_pol",
                                "ide_2008_mg_trecho_enquadrada_classe_especial_lin"
                        )
                )
        );

        regras.add(
                regra(
                        86L,
                        List.of(
                                "ide_2007_mg_area_conflito_recursos_hidricos_pol"
                        )
                )
        );

        regras.add(
                regra(
                        87L,
                        List.of(
                                "ide_2002_mg_potencialidade_cavidades_pol"
                        ),
                        List.of(
                                "Muito Alto",
                                "Alto",
                                "Médio",
                                "Baixo",
                                "Ocorrência Improvável"
                        ),
                        List.of()
                )
        );

        regras.add(
                regra(
                        88L,
                        List.of(
                                "ide_2001_mg_raio_protecao_cavidades_pol"
                        )
                )
        );

        regras.add(
                regra(
                        42L,
                        List.of(
                                "ide_2009_mg_rios_preservacao_permanente_pol"
                        )
                )
        );

        regras.add(
                regra(
                        46L,
                        Arrays.asList(
                                "ide_2019_do_enq_corpos_dagua_do6_lin",
                                "ide_2019_gd_enq_corpos_dagua_gd2_lin",
                                "ide_2019_sf_enq_corpos_dagua_sf4_lin",
                                "ide_2019_gd_enq_corpos_dagua_gd3_lin",
                                "ide_2019_jq_enq_corpos_dagua_jq2_lin",
                                "ide_2019_pn_enq_corpos_dagua_pn2_lin",
                                "ide_2019_bu_enq_corpos_dagua_bu1_lin",
                                "ide_2019_do_enq_corpos_dagua_do5_lin",
                                "ide_2019_sf_enq_corpos_dagua_sf5_lin",
                                "ide_2019_in_enq_corpos_dagua_in1_lin",
                                "ide_2019_ip_enq_corpos_dagua_ip1_lin",
                                "ide_2019_iu_enq_corpos_dagua_iu1_lin",
                                "ide_2019_ju_enq_corpos_dagua_ju1_lin",
                                "ide_2019_pa_enq_corpos_dagua_pa1_lin",
                                "ide_2019_mu_enq_corpos_dagua_mu1_lin",
                                "ide_2019_sf_enq_corpos_dagua_sf2_lin",
                                "ide_2019_sf_enq_corpos_dagua_sf7_lin",
                                "ide_2019_sf_enq_corpos_dagua_sf3_lin",
                                "ide_2019_pe_enq_corpos_dagua_pe1_lin",
                                "ide_2019_do_enq_corpos_dagua_do2_lin",
                                "ide_2019_do_enq_corpos_dagua_do1_lin",
                                "ide_2019_do_enq_corpos_dagua_do3_lin",
                                "ide_2019_sm_enq_corpos_dagua_sm1_lin",
                                "ide_2019_gd_enq_corpos_dagua_gd5_lin",
                                "ide_2019_do_enq_corpos_dagua_do4_lin",
                                "ide_2019_sf_enq_corpos_dagua_sf8_lin",
                                "ide_2019_gd_enq_corpos_dagua_gd4_lin",
                                "ide_2019_sf_enq_corpos_dagua_sf10_lin",
                                "ide_2019_sf_enq_corpos_dagua_sf1_lin",
                                "ide_2019_gd_enq_corpos_dagua_gd8_lin",
                                "ide_2019_gd_enq_corpos_dagua_gd7_lin",
                                "ide_2019_gd_enq_corpos_dagua_gd1_lin",
                                "ide_2019_jq_enq_corpos_dagua_jq1_lin",
                                "ide_2019_pn_enq_corpos_dagua_pn1_lin",
                                "ide_2019_pn_enq_corpos_dagua_pn3_lin",
                                "ide_2019_jq_enq_corpos_dagua_jq3_lin",
                                "ide_2019_sf_enq_corpos_dagua_sf9_lin",
                                "ide_2019_ib_enq_corpos_dagua_ib1_lin",
                                "ide_2019_gd_enq_corpos_dagua_gd6_lin",
                                "ide_2019_ps_enq_corpos_dagua_ps2_lin",
                                "ide_2019_ps_enq_corpos_dagua_ps1_lin",
                                "ide_2019_sf_enq_corpos_dagua_sf6_lin",
                                "ide_2019_pj_enq_corpos_dagua_pj1_lin"
                        ),
                        List.of(
                                "Especial"
                        ),
                        List.of()
                )
        );

        regras.add(
                regra(
                        181L,
                        List.of(
                                "ide_2007_mg_restricao_contole_rh_sub_pol",
                                "ide_2007_sf_rpe_100_pol"
                        )
                )
        );

        return regras;
    }

    private Regra regra(
            Long idPergunta,
            List<String> camadas) {

        return new Regra(
                idPergunta,
                camadas,
                List.of(),
                List.of()
        );
    }

    private Regra regra(
            Long idPergunta,
            List<String> camadas,
            List<String> contains,
            List<String> notContains) {

        return new Regra(
                idPergunta,
                camadas,
                contains,
                notContains
        );
    }

    private record Regra(
            Long idPergunta,
            List<String> camadas,
            List<String> contains,
            List<String> notContains) {
    }

    private record ResultadoComparacao(
            String resultado,
            boolean identificado,
            JsonNode detalhes) {
    }
}
