package br.gov.mg.meioambiente.slageo.repository.interfaces;

public interface AtividadeConsolidacaoRepository {

    int criarAtividade(
            Integer idSolic
    );

    int atualizarGeometria(
            Integer idSolic
    );
}
