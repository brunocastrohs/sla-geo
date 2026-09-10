package br.gov.mg.meioambiente.slageo.application.service.impl;

import br.gov.mg.meioambiente.slageo.application.service.interfaces.AtividadeConsolidacaoService;
import br.gov.mg.meioambiente.slageo.repository.interfaces.AtividadeConsolidacaoRepository;
import br.gov.mg.meioambiente.slageo.repository.interfaces.AtividadeGeometriaRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AtividadeConsolidacaoServiceImpl
        implements AtividadeConsolidacaoService {

    private final AtividadeGeometriaRepository
            atividadeGeometriaRepository;

    private final AtividadeConsolidacaoRepository
            atividadeConsolidacaoRepository;

    public AtividadeConsolidacaoServiceImpl(
            AtividadeGeometriaRepository atividadeGeometriaRepository,
            AtividadeConsolidacaoRepository atividadeConsolidacaoRepository) {

        this.atividadeGeometriaRepository =
                atividadeGeometriaRepository;

        this.atividadeConsolidacaoRepository =
                atividadeConsolidacaoRepository;
    }

    @Override
    @Transactional
    public void criarOuAtualizar(
            Integer idSolic) {

        if (idSolic == null) {

            throw new IllegalArgumentException(
                    "O idSolic é obrigatório para consolidar a atividade."
            );
        }

        boolean atividadeExiste =
                atividadeGeometriaRepository
                        .existsByIdSolic(
                                idSolic.longValue()
                        );

        if (atividadeExiste) {

            atividadeConsolidacaoRepository
                    .atualizarGeometria(
                            idSolic
                    );

            return;
        }

        int registrosInseridos =
                atividadeConsolidacaoRepository
                        .criarAtividade(
                                idSolic
                        );

        if (registrosInseridos == 0) {

            throw new IllegalArgumentException(
                    "Não foi possível criar a atividade para a solicitação "
                            + idSolic
                            + "."
            );
        }
    }

    @Override
    @Transactional
    public void atualizarSeExistir(
            Integer idSolic) {

        if (idSolic == null) {
            return;
        }

        boolean atividadeExiste =
                atividadeGeometriaRepository
                        .existsByIdSolic(
                                idSolic.longValue()
                        );

        if (!atividadeExiste) {
            return;
        }

        atividadeConsolidacaoRepository
                .atualizarGeometria(
                        idSolic
                );
    }
}
