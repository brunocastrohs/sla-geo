package br.gov.mg.meioambiente.slageo.controller;

import br.gov.mg.meioambiente.slageo.application.dto.AtividadeGeometriaDto;
import br.gov.mg.meioambiente.slageo.application.service.interfaces.LimiteEstadualService;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/validacoes/estado")
public class LimiteEstadualController {

    private final LimiteEstadualService service;

    public LimiteEstadualController(
            LimiteEstadualService service) {

        this.service = service;
    }

    @PostMapping
    public ResponseEntity<Boolean> validar(
            @Valid
            @RequestBody
            AtividadeGeometriaDto dto) {

        return ResponseEntity.ok(
                service.validar(dto)
        );
    }
}
