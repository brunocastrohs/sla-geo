package br.gov.mg.meioambiente.slageo.application.service.interfaces;

import br.gov.mg.meioambiente.slageo.application.dto.AtividadeGeometriaDto;
import br.gov.mg.meioambiente.slageo.repository.interfaces.LimiteMunicipalRepository;

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

    boolean estaIntegralmenteEmMinasGerais(
            AtividadeGeometriaDto dto
    );

    long contarMunicipiosIntersectados(
            AtividadeGeometriaDto dto
    );

    boolean intersectaMaisDeUmMunicipio(
            AtividadeGeometriaDto dto
    );

    List<LimiteMunicipalRepository.MunicipioIntersecao>
    buscarMunicipiosIntersectados(
            AtividadeGeometriaDto dto
    );

    double[] buscarCentro(Long idAtividade);
}
