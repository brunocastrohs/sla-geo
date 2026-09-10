package br.gov.mg.meioambiente.slageo.repository.interfaces;

import br.gov.mg.meioambiente.slageo.model.DenominacaoGeometria;
import br.gov.mg.meioambiente.slageo.model.enums.DenominacaoEnum;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DenominacaoGeometriaRepository
        extends JpaRepository<
                DenominacaoGeometria,
                Long> {

    List<DenominacaoGeometria>
    findByIdSolicAndDataExclIsNull(
            Integer idSolic
    );

    List<DenominacaoGeometria>
    findByIdSolicAndDenominacaoAndDataExclIsNull(
            Integer idSolic,
            DenominacaoEnum denominacao
    );

    List<DenominacaoGeometria>
    findByDataExclIsNull();
}
