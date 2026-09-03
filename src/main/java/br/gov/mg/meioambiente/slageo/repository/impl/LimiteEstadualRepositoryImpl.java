package br.gov.mg.meioambiente.slageo.repository.impl;

import br.gov.mg.meioambiente.slageo.repository.interfaces.LimiteEstadualRepository;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class LimiteEstadualRepositoryImpl
        implements LimiteEstadualRepository {

    /*
     * Bounding Box externo de Minas Gerais.
     *
     * Obtido diretamente da tabela limite_estadual:
     *
     * SELECT
     *     ST_XMin(extent),
     *     ST_YMin(extent),
     *     ST_XMax(extent),
     *     ST_YMax(extent)
     * FROM (
     *     SELECT ST_Extent(geom) AS extent
     *     FROM limite_estadual
     *     WHERE uf = 'MG'
     * ) dados;
     */
    private static final double MG_MIN_X =
            -51.0454546390694;

    private static final double MG_MIN_Y =
            -22.92225740860977;

    private static final double MG_MAX_X =
            -39.85643447623598;

    private static final double MG_MAX_Y =
            -14.232850706862903;

    /*
     * Bounding Box interno comprovadamente
     * contido no território de Minas Gerais.
     *
     * Qualquer geometria cujo envelope esteja
     * integralmente dentro deste retângulo pode
     * ser aprovada sem validação topológica.
     */
    private static final double MG_INTERNO_MIN_X =
            -46.806889;

    private static final double MG_INTERNO_MIN_Y =
            -21.360054;

    private static final double MG_INTERNO_MAX_X =
            -42.229929;

    private static final double MG_INTERNO_MAX_Y =
            -15.262752;

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public LimiteEstadualRepositoryImpl(
            NamedParameterJdbcTemplate jdbcTemplate) {

        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public boolean estaIntegralmenteEmMinasGerais(
            String geoJson) {

        String sql = """
                WITH entrada AS (
                    SELECT ST_SetSRID(
                        ST_GeomFromGeoJSON(:geoJson),
                        4674
                    ) AS geom
                ),

                envelope AS (
                    SELECT
                        geom,
                        ST_XMin(ST_Envelope(geom)) AS min_x,
                        ST_YMin(ST_Envelope(geom)) AS min_y,
                        ST_XMax(ST_Envelope(geom)) AS max_x,
                        ST_YMax(ST_Envelope(geom)) AS max_y
                    FROM entrada
                ),

                validacao_bbox AS (
                    SELECT
                        geom,

                        (
                            min_x >= :mgMinX
                            AND min_y >= :mgMinY
                            AND max_x <= :mgMaxX
                            AND max_y <= :mgMaxY
                        ) AS dentro_bbox_externo,

                        (
                            min_x >= :mgInternoMinX
                            AND min_y >= :mgInternoMinY
                            AND max_x <= :mgInternoMaxX
                            AND max_y <= :mgInternoMaxY
                        ) AS dentro_bbox_interno

                    FROM envelope
                )

                SELECT
                    CASE
                        /*
                         * Fora do BBOX externo:
                         * certamente não está integralmente
                         * dentro de Minas Gerais.
                         */
                        WHEN NOT dentro_bbox_externo
                            THEN FALSE

                        /*
                         * Dentro do BBOX interno:
                         * certamente está dentro de Minas.
                         *
                         * Evita ST_CoveredBy.
                         */
                        WHEN dentro_bbox_interno
                            THEN TRUE

                        /*
                         * Região próxima às bordas:
                         * executa validação topológica
                         * contra o polígono real do estado.
                         */
                        ELSE EXISTS (
                            SELECT 1
                            FROM limite_estadual estado
                            WHERE estado.uf = 'MG'

                              AND estado.geom && geom

                              AND ST_CoveredBy(
                                  geom,
                                  estado.geom
                              )
                        )
                    END
                FROM validacao_bbox
                """;

        MapSqlParameterSource parametros =
                new MapSqlParameterSource()
                        .addValue(
                                "geoJson",
                                geoJson
                        )
                        .addValue(
                                "mgMinX",
                                MG_MIN_X
                        )
                        .addValue(
                                "mgMinY",
                                MG_MIN_Y
                        )
                        .addValue(
                                "mgMaxX",
                                MG_MAX_X
                        )
                        .addValue(
                                "mgMaxY",
                                MG_MAX_Y
                        )
                        .addValue(
                                "mgInternoMinX",
                                MG_INTERNO_MIN_X
                        )
                        .addValue(
                                "mgInternoMinY",
                                MG_INTERNO_MIN_Y
                        )
                        .addValue(
                                "mgInternoMaxX",
                                MG_INTERNO_MAX_X
                        )
                        .addValue(
                                "mgInternoMaxY",
                                MG_INTERNO_MAX_Y
                        );

        Boolean resultado =
                jdbcTemplate.queryForObject(
                        sql,
                        parametros,
                        Boolean.class
                );

        return Boolean.TRUE.equals(resultado);
    }
}
