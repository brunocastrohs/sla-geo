package br.gov.mg.meioambiente.slageo.application.service.interfaces;

import org.locationtech.jts.geom.Geometry;

public interface GeoJsonValidationService {

    Geometry validar(
            String geoJson
    );
}
