package br.gov.mg.meioambiente.slageo.application.mapper;

import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.geojson.GeoJsonReader;
import org.locationtech.jts.io.geojson.GeoJsonWriter;
import org.springframework.stereotype.Component;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@Component
public class GeoJsonGeometryMapper {

    private static final int SRID = 4674;

    private final JsonMapper jsonMapper =
            JsonMapper.builder().build();

    public Geometry toGeometry(
            JsonNode geoJson) {

        if (geoJson == null) {
            return null;
        }

        try {
            Geometry geometry =
                    new GeoJsonReader()
                            .read(
                                    geoJson.toString()
                            );

            if (geometry != null) {
                geometry.setSRID(SRID);
            }

            return geometry;

        } catch (ParseException e) {

            throw new IllegalArgumentException(
                    "GeoJSON inválido.",
                    e
            );
        }
    }

    public JsonNode toJsonNode(
            Geometry geometry) {

        if (geometry == null) {
            return null;
        }

        try {
            String geoJson =
                    new GeoJsonWriter()
                            .write(geometry);

            return jsonMapper.readTree(
                    geoJson
            );

        } catch (Exception e) {

            throw new IllegalArgumentException(
                    "Não foi possível converter a geometria para GeoJSON.",
                    e
            );
        }
    }
}
