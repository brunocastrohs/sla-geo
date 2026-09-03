package br.gov.mg.meioambiente.slageo.application.mapper;

import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.geojson.GeoJsonReader;
import org.locationtech.jts.io.geojson.GeoJsonWriter;

import org.springframework.stereotype.Component;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@Component
public class GeoJsonGeometryMapper {

    private static final int SRID = 4674;

    private final JsonMapper jsonMapper;

    public GeoJsonGeometryMapper() {
        this.jsonMapper = JsonMapper.builder().build();
    }

    public MultiPolygon toMultiPolygon(
            JsonNode geoJson) {

        if (geoJson == null) {
            return null;
        }

        try {
            GeoJsonReader reader =
                    new GeoJsonReader();

            Geometry geometry =
                    reader.read(
                            geoJson.toString()
                    );

            geometry.setSRID(SRID);

            if (geometry instanceof MultiPolygon multiPolygon) {

                multiPolygon.setSRID(SRID);

                return multiPolygon;
            }

            if (geometry instanceof Polygon polygon) {

                MultiPolygon multiPolygon =
                        polygon
                                .getFactory()
                                .createMultiPolygon(
                                        new Polygon[]{
                                                polygon
                                        }
                                );

                multiPolygon.setSRID(SRID);

                return multiPolygon;
            }

            throw new IllegalArgumentException(
                    "A geometria deve ser Polygon ou MultiPolygon."
            );

        } catch (ParseException e) {

            throw new IllegalArgumentException(
                    "GeoJSON inválido.",
                    e
            );
        }
    }

    public JsonNode toJsonNode(
            MultiPolygon geometry) {

        if (geometry == null) {
            return null;
        }

        try {
            GeoJsonWriter writer =
                    new GeoJsonWriter();

            String geoJson =
                    writer.write(geometry);

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
