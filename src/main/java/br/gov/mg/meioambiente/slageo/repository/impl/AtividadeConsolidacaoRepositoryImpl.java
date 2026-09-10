package br.gov.mg.meioambiente.slageo.repository.impl;

import br.gov.mg.meioambiente.slageo.repository.interfaces.AtividadeConsolidacaoRepository;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class AtividadeConsolidacaoRepositoryImpl
        implements AtividadeConsolidacaoRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public AtividadeConsolidacaoRepositoryImpl(
            NamedParameterJdbcTemplate jdbcTemplate) {

        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public int criarAtividade(
            Integer idSolic) {

        String sql = """
                INSERT INTO sla.tb_atividade (
                    id_atividade,
                    id_solic,
                    geom
                )
                SELECT
                    nextval('sla.seq_atividade'),
                    :idSolic,
                    ST_SetSRID(
                        ST_Collect(d.geom),
                        4674
                    )
                FROM sla.tb_denominacao d
                WHERE d.id_solic = :idSolic
                  AND d.data_excl IS NULL
                  AND d.geom IS NOT NULL
                """;

        MapSqlParameterSource params =
                new MapSqlParameterSource()
                        .addValue(
                                "idSolic",
                                idSolic
                        );

        return jdbcTemplate.update(
                sql,
                params
        );
    }

    @Override
    public int atualizarGeometria(
            Integer idSolic) {

        String sql = """
                UPDATE sla.tb_atividade a
                SET geom = (
                    SELECT ST_SetSRID(
                        ST_Collect(d.geom),
                        4674
                    )
                    FROM sla.tb_denominacao d
                    WHERE d.id_solic = :idSolic
                      AND d.data_excl IS NULL
                      AND d.geom IS NOT NULL
                )
                WHERE a.id_solic = :idSolic
                """;

        MapSqlParameterSource params =
                new MapSqlParameterSource()
                        .addValue(
                                "idSolic",
                                idSolic
                        );

        return jdbcTemplate.update(
                sql,
                params
        );
    }
}
