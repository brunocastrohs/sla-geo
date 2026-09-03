package br.gov.mg.meioambiente.slageo.repository.impl;

import br.gov.mg.meioambiente.slageo.repository.interfaces.LimiteMunicipalRepository;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class LimiteMunicipalRepositoryImpl
        implements LimiteMunicipalRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public LimiteMunicipalRepositoryImpl(
            NamedParameterJdbcTemplate jdbcTemplate) {

        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public long contarMunicipiosIntersectados(
            String geoJson) {

        String sql = """
                WITH entrada AS (
                    SELECT ST_SetSRID(
                        ST_GeomFromGeoJSON(:geoJson),
                        4674
                    ) AS geom
                )
                SELECT COUNT(DISTINCT municipio.codigo_ibge)
                FROM limite_municipal municipio
                CROSS JOIN entrada
                WHERE municipio.uf = 'MG'
                  AND municipio.geom && entrada.geom
                  AND ST_Intersects(
                      municipio.geom,
                      entrada.geom
                  )
                  AND NOT ST_Touches(
                      municipio.geom,
                      entrada.geom
                  )
                """;

        Long quantidade = jdbcTemplate.queryForObject(
                sql,
                parametros(geoJson),
                Long.class
        );

        return quantidade == null
                ? 0L
                : quantidade;
    }

    @Override
    public boolean intersectaMaisDeUmMunicipio(
            String geoJson) {

        String sql = """
                WITH entrada AS (
                    SELECT ST_SetSRID(
                        ST_GeomFromGeoJSON(:geoJson),
                        4674
                    ) AS geom
                ),
                municipios AS (
                    SELECT DISTINCT municipio.codigo_ibge
                    FROM limite_municipal municipio
                    CROSS JOIN entrada
                    WHERE municipio.uf = 'MG'
                      AND municipio.geom && entrada.geom
                      AND ST_Intersects(
                          municipio.geom,
                          entrada.geom
                      )
                      AND NOT ST_Touches(
                          municipio.geom,
                          entrada.geom
                      )
                    LIMIT 2
                )
                SELECT COUNT(*) > 1
                FROM municipios
                """;

        Boolean resultado = jdbcTemplate.queryForObject(
                sql,
                parametros(geoJson),
                Boolean.class
        );

        return Boolean.TRUE.equals(resultado);
    }

    @Override
    public List<MunicipioIntersecao>
    buscarMunicipiosIntersectados(
            String geoJson) {

        String sql = """
                WITH entrada AS (
                    SELECT ST_SetSRID(
                        ST_GeomFromGeoJSON(:geoJson),
                        4674
                    ) AS geom
                ),
                candidatos AS (
                    SELECT
                        municipio.codigo_ibge,
                        municipio.nome,
                        ST_Intersection(
                            municipio.geom,
                            entrada.geom
                        ) AS geom_intersecao
                    FROM limite_municipal municipio
                    CROSS JOIN entrada
                    WHERE municipio.uf = 'MG'
                      AND municipio.geom && entrada.geom
                      AND ST_Intersects(
                          municipio.geom,
                          entrada.geom
                      )
                      AND NOT ST_Touches(
                          municipio.geom,
                          entrada.geom
                      )
                ),
                intersecoes AS (
                    SELECT
                        codigo_ibge,
                        nome,
                        ST_Area(
                            geom_intersecao::geography
                        ) AS area_intersecao_m2
                    FROM candidatos
                    WHERE NOT ST_IsEmpty(
                        geom_intersecao
                    )
                )
                SELECT
                    codigo_ibge,
                    nome,
                    area_intersecao_m2
                FROM intersecoes
                WHERE area_intersecao_m2 > 0
                ORDER BY
                    area_intersecao_m2 DESC,
                    nome ASC
                """;

        return jdbcTemplate.query(
                sql,
                parametros(geoJson),
                (rs, rowNum) ->
                        new MunicipioIntersecao(
                                rs.getString(
                                        "codigo_ibge"
                                ),
                                rs.getString(
                                        "nome"
                                ),
                                rs.getDouble(
                                        "area_intersecao_m2"
                                )
                        )
        );
    }

    private MapSqlParameterSource parametros(
            String geoJson) {

        return new MapSqlParameterSource()
                .addValue(
                        "geoJson",
                        geoJson
                );
    }
}
