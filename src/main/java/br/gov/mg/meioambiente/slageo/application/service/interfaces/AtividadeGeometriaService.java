package br.gov.mg.meioambiente.slageo.application.service.interfaces;

import br.gov.mg.meioambiente.slageo.application.dto.AtividadeGeometriaDto;

import java.util.List;

public interface AtividadeGeometriaService {

    AtividadeGeometriaDto salvar(
            AtividadeGeometriaDto dto
    );

    AtividadeGeometriaDto atualizar(
            AtividadeGeometriaDto dto
    );

    AtividadeGeometriaDto salvarOuAtualizar(
            AtividadeGeometriaDto dto
    );

    AtividadeGeometriaDto buscarPorIdAtividade(
            Long idAtividade
    );

    List<AtividadeGeometriaDto> buscarPorIdSolic(
            Long idSolic
    );

    List<AtividadeGeometriaDto> buscarTodos();

    void remover(
            Long idAtividade
    );

    double[] buscarCentro(
            Long idAtividade
    );
}
