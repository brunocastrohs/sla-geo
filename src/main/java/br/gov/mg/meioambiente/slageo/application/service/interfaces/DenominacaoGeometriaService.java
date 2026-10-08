package br.gov.mg.meioambiente.slageo.application.service.interfaces;

import br.gov.mg.meioambiente.slageo.application.dto.ArquivoDenominacaoDto;
import br.gov.mg.meioambiente.slageo.application.dto.DenominacaoGeometriaDto;
import br.gov.mg.meioambiente.slageo.repository.interfaces.LimiteMunicipalRepository;

import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface DenominacaoGeometriaService {

    DenominacaoGeometriaDto salvar(
            DenominacaoGeometriaDto dto,
            MultipartFile arquivo
    );

    DenominacaoGeometriaDto atualizar(
            Long idDenominacao,
            DenominacaoGeometriaDto dto,
            MultipartFile arquivo
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

    ArquivoDenominacaoDto recuperarArquivo(
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
