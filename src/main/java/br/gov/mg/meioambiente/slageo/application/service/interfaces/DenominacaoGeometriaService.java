package br.gov.mg.meioambiente.slageo.application.service.interfaces;

import br.gov.mg.meioambiente.slageo.application.dto.DenominacaoGeometriaDto;
import br.gov.mg.meioambiente.slageo.repository.interfaces.LimiteMunicipalRepository;

import java.util.List;

public interface DenominacaoGeometriaService {

    DenominacaoGeometriaDto salvar(
            DenominacaoGeometriaDto dto
    );

    DenominacaoGeometriaDto atualizar(
            Long idDenominacao,
            DenominacaoGeometriaDto dto
    );

    DenominacaoGeometriaDto buscarPorId(
            Long idDenominacao
    );

    List<DenominacaoGeometriaDto> buscarPorIdSolic(
            Integer idSolic
    );

    List<DenominacaoGeometriaDto> buscarTodos();

    void remover(
            Long idDenominacao
    );

    double[] buscarCentro(
            Long idDenominacao
    );

    boolean estaIntegralmenteEmMinasGerais(
            DenominacaoGeometriaDto dto
    );

    long contarMunicipiosIntersectados(
            DenominacaoGeometriaDto dto
    );

    boolean intersectaMaisDeUmMunicipio(
            DenominacaoGeometriaDto dto
    );

    List<LimiteMunicipalRepository.MunicipioIntersecao>
    buscarMunicipiosIntersectados(
            DenominacaoGeometriaDto dto
    );
}
