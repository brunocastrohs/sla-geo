package br.gov.mg.meioambiente.slageo.repository.interfaces;

import br.gov.mg.meioambiente.slageo.model.AtividadeGeometria;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AtividadeGeometriaRepository
        extends JpaRepository<AtividadeGeometria, Long> {

    List<AtividadeGeometria> findByIdSolic(
            Long idSolic
    );

    boolean existsByIdSolic(
            Long idSolic
    );
}
