package br.gov.mg.meioambiente.slageo.controller;

import br.gov.mg.meioambiente.slageo.application.dto.AtividadeGeometriaDto;
import br.gov.mg.meioambiente.slageo.application.service.interfaces.AtividadeGeometriaService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/atividades/geometrias")
public class AtividadeGeometriaController {

    private final AtividadeGeometriaService service;

    public AtividadeGeometriaController(
            AtividadeGeometriaService service) {

        this.service = service;
    }

    @GetMapping("/{idAtividade}/center")
    public double[] buscarCentro(
            @PathVariable Long idAtividade) {

        return service.buscarCentro(idAtividade);
    }

    @PostMapping
    public ResponseEntity<AtividadeGeometriaDto> salvar(
            @Valid
            @RequestBody
            AtividadeGeometriaDto dto) {

        AtividadeGeometriaDto resultado =
                service.salvar(dto);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(resultado);
    }

    @PutMapping("/{idAtividade}")
    public ResponseEntity<AtividadeGeometriaDto> atualizar(
            @PathVariable Long idAtividade,
            @Valid
            @RequestBody
            AtividadeGeometriaDto dto) {

        dto.setIdAtividade(idAtividade);

        return ResponseEntity.ok(
                service.atualizar(dto)
        );
    }

    @PutMapping
    public ResponseEntity<AtividadeGeometriaDto>
    salvarOuAtualizar(
            @Valid
            @RequestBody
            AtividadeGeometriaDto dto) {

        return ResponseEntity.ok(
                service.salvarOuAtualizar(dto)
        );
    }

    @GetMapping("/{idAtividade}")
    public ResponseEntity<AtividadeGeometriaDto>
    buscarPorIdAtividade(
            @PathVariable Long idAtividade) {

        return ResponseEntity.ok(
                service.buscarPorIdAtividade(
                        idAtividade
                )
        );
    }

    @GetMapping
    public ResponseEntity<List<AtividadeGeometriaDto>>
    buscarTodos() {

        return ResponseEntity.ok(
                service.buscarTodos()
        );
    }

    @GetMapping("/solicitacao/{idSolic}")
    public ResponseEntity<List<AtividadeGeometriaDto>>
    buscarPorIdSolic(
            @PathVariable Long idSolic) {

        return ResponseEntity.ok(
                service.buscarPorIdSolic(
                        idSolic
                )
        );
    }

    @DeleteMapping("/{idAtividade}")
    public ResponseEntity<Void> remover(
            @PathVariable Long idAtividade) {

        service.remover(idAtividade);

        return ResponseEntity.noContent().build();
    }
}
