package br.gov.mg.meioambiente.slageo.controller;

import br.gov.mg.meioambiente.slageo.application.service.interfaces.CriteriosLocacionaisService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import tools.jackson.databind.JsonNode;

@RestController
@RequestMapping(
        "/api/v1/denominacoes/geometrias"
)
public class CriteriosLocacionaisController {

    private final CriteriosLocacionaisService
            service;

    public CriteriosLocacionaisController(
            CriteriosLocacionaisService service) {

        this.service =
                service;
    }

    @PostMapping(
            "/{idDenominacao}/criterios-locacionais"
    )
    public ResponseEntity<JsonNode>
    calcular(
            @PathVariable
            Long idDenominacao) {

        return ResponseEntity.ok(
                service.calcular(
                        idDenominacao
                )
        );
    }

    @GetMapping(
            "/{idDenominacao}/criterios-locacionais"
    )
    public ResponseEntity<JsonNode>
    buscar(
            @PathVariable
            Long idDenominacao) {

        return ResponseEntity.ok(
                service.buscar(
                        idDenominacao
                )
        );
    }
}
