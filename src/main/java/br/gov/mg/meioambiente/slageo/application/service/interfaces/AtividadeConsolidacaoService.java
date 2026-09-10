package br.gov.mg.meioambiente.slageo.application.service.interfaces;

public interface AtividadeConsolidacaoService {

    void criarOuAtualizar(
            Integer idSolic
    );

    void atualizarSeExistir(
            Integer idSolic
    );
}
