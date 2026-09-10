package br.gov.mg.meioambiente.slageo.application.service.impl;

import br.gov.mg.meioambiente.slageo.application.service.interfaces.GeoJsonValidationService;

import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.geojson.GeoJsonReader;
import org.springframework.stereotype.Service;

@Service
public class GeoJsonValidationServiceImpl
        implements GeoJsonValidationService {

    private static final int SRID = 4674;

    private static final int MAXIMO_PONTOS =
            100_000;

    @Override
    public Geometry validar(
            String geoJson) {

        if (geoJson == null
                || geoJson.isBlank()) {

            throw new IllegalArgumentException(
                    "O GeoJSON não pode ser nulo ou vazio."
            );
        }

        final Geometry geometry;

        try {

            geometry =
                    new GeoJsonReader()
                            .read(geoJson);

        } catch (ParseException e) {

            throw new IllegalArgumentException(
                    "O GeoJSON informado é inválido.",
                    e
            );
        }

        if (geometry == null
                || geometry.isEmpty()) {

            throw new IllegalArgumentException(
                    "A geometria não pode ser vazia."
            );
        }

        if (geometry.getNumPoints()
                > MAXIMO_PONTOS) {

            throw new IllegalArgumentException(
                    "A geometria excede o limite de "
                            + MAXIMO_PONTOS
                            + " pontos."
            );
        }

        if (!geometry.isValid()) {

            throw new IllegalArgumentException(
                    "A geometria informada é topologicamente inválida."
            );
        }

        geometry.setSRID(SRID);

        return geometry;
    }
}
