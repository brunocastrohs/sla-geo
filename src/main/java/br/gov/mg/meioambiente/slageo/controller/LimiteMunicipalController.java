package br.gov.mg.meioambiente.slageo.controller;

import br.gov.mg.meioambiente.slageo.application.dto.AtividadeGeometriaDto;
import br.gov.mg.meioambiente.slageo.application.dto.MunicipioIntersecaoDto;
import br.gov.mg.meioambiente.slageo.application.service.interfaces.LimiteMunicipalService;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/validacoes/municipios")
public class LimiteMunicipalController {

    private final LimiteMunicipalService service;

    public LimiteMunicipalController(
            LimiteMunicipalService service) {

        this.service = service;
    }

    @PostMapping
    public ResponseEntity<Boolean>
    intersectaMaisDeUmMunicipio(
            @Valid
            @RequestBody
            AtividadeGeometriaDto dto) {

        return ResponseEntity.ok(
                service.intersectaMaisDeUmMunicipio(
                        dto
                )
        );
    }

    @PostMapping("/intersecoes")
    public ResponseEntity<List<MunicipioIntersecaoDto>>
    buscarMunicipiosIntersectados(
            @Valid
            @RequestBody
            AtividadeGeometriaDto dto) {

        return ResponseEntity.ok(
                service.buscarMunicipiosIntersectados(
                        dto
                )
        );
    }
}
