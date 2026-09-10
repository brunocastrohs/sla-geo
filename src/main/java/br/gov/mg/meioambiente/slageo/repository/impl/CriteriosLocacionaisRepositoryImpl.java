package br.gov.mg.meioambiente.slageo.repository.impl;

import br.gov.mg.meioambiente.slageo.repository.interfaces.CriteriosLocacionaisRepository;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class CriteriosLocacionaisRepositoryImpl
        implements CriteriosLocacionaisRepository {

    private final NamedParameterJdbcTemplate
            jdbcTemplate;

    public CriteriosLocacionaisRepositoryImpl(
            NamedParameterJdbcTemplate jdbcTemplate) {

        this.jdbcTemplate =
                jdbcTemplate;
    }

    @Override
    public void salvar(
            Long idDenominacao,
            String criteriosLocacionais) {

        String sql = """
                UPDATE sla.tb_denominacao
                SET criterios_locacionais =
                    CAST(:criteriosLocacionais AS jsonb)
                WHERE id_denominacao = :idDenominacao
                  AND data_excl IS NULL
                """;

        MapSqlParameterSource params =
                new MapSqlParameterSource()
                        .addValue(
                                "idDenominacao",
                                idDenominacao
                        )
                        .addValue(
                                "criteriosLocacionais",
                                criteriosLocacionais
                        );

        int atualizados =
                jdbcTemplate.update(
                        sql,
                        params
                );

        if (atualizados == 0) {

            throw new IllegalArgumentException(
                    "Denominação não encontrada: "
                            + idDenominacao
                            + "."
            );
        }
    }

    @Override
    public String buscar(
            Long idDenominacao) {

        String sql = """
                SELECT criterios_locacionais::text
                FROM sla.tb_denominacao
                WHERE id_denominacao = :idDenominacao
                  AND data_excl IS NULL
                """;

        MapSqlParameterSource params =
                new MapSqlParameterSource()
                        .addValue(
                                "idDenominacao",
                                idDenominacao
                        );

        List<String> resultados =
                jdbcTemplate.query(
                        sql,
                        params,
                        (rs, rowNum) ->
                                rs.getString(
                                        "criterios_locacionais"
                                )
                );

        if (resultados.isEmpty()) {

            throw new IllegalArgumentException(
                    "Denominação não encontrada: "
                            + idDenominacao
                            + "."
            );
        }

        return resultados.getFirst();
    }

    @Override
    public void limpar(
            Long idDenominacao) {

        String sql = """
                UPDATE sla.tb_denominacao
                SET criterios_locacionais = NULL
                WHERE id_denominacao = :idDenominacao
                """;

        MapSqlParameterSource params =
                new MapSqlParameterSource()
                        .addValue(
                                "idDenominacao",
                                idDenominacao
                        );

        jdbcTemplate.update(
                sql,
                params
        );
    }
}
