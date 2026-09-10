package br.gov.mg.meioambiente.slageo.repository.interfaces;

public interface CriteriosLocacionaisRepository {

    void salvar(
            Long idDenominacao,
            String criteriosLocacionais
    );

    String buscar(
            Long idDenominacao
    );

    void limpar(
            Long idDenominacao
    );
}
