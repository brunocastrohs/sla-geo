package br.gov.mg.meioambiente.slageo.repository.interfaces;

import java.util.List;

public interface LimiteMunicipalRepository {

    long contarMunicipiosIntersectados(
            String geoJson
    );

    boolean intersectaMaisDeUmMunicipio(
            String geoJson
    );

    List<MunicipioIntersecao> buscarMunicipiosIntersectados(
            String geoJson
    );

    record MunicipioIntersecao(
            String codigoIbge,
            String nome,
            Double areaIntersecaoM2
    ) {
    }
}
