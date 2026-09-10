package br.gov.mg.meioambiente.slageo.controller;

import br.gov.mg.meioambiente.slageo.application.dto.DenominacaoGeometriaDto;
import br.gov.mg.meioambiente.slageo.application.service.interfaces.DenominacaoGeometriaService;
import br.gov.mg.meioambiente.slageo.repository.interfaces.LimiteMunicipalRepository;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping(
        "/api/v1/denominacoes/geometrias"
)
public class DenominacaoGeometriaController {

    private final DenominacaoGeometriaService service;

    public DenominacaoGeometriaController(
            DenominacaoGeometriaService service) {

        this.service = service;
    }

    @PostMapping
    public ResponseEntity<DenominacaoGeometriaDto> salvar(
            @Valid
            @RequestBody
            DenominacaoGeometriaDto dto) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        service.salvar(dto)
                );
    }

    @PutMapping("/{idDenominacao}")
    public ResponseEntity<DenominacaoGeometriaDto> atualizar(
            @PathVariable
            Long idDenominacao,

            @Valid
            @RequestBody
            DenominacaoGeometriaDto dto) {

        return ResponseEntity.ok(
                service.atualizar(
                        idDenominacao,
                        dto
                )
        );
    }

    @GetMapping("/{idDenominacao}")
    public ResponseEntity<DenominacaoGeometriaDto> buscarPorId(
            @PathVariable
            Long idDenominacao) {

        return ResponseEntity.ok(
                service.buscarPorId(
                        idDenominacao
                )
        );
    }

    @GetMapping("/solicitacao/{idSolic}")
    public ResponseEntity<List<DenominacaoGeometriaDto>>
    buscarPorIdSolic(
            @PathVariable
            Integer idSolic) {

        return ResponseEntity.ok(
                service.buscarPorIdSolic(
                        idSolic
                )
        );
    }

    @GetMapping
    public ResponseEntity<List<DenominacaoGeometriaDto>>
    buscarTodos() {

        return ResponseEntity.ok(
                service.buscarTodos()
        );
    }

    @DeleteMapping("/{idDenominacao}")
    public ResponseEntity<Void> remover(
            @PathVariable
            Long idDenominacao) {

        service.remover(
                idDenominacao
        );

        return ResponseEntity
                .noContent()
                .build();
    }

    @GetMapping("/{idDenominacao}/center")
    public ResponseEntity<double[]> buscarCentro(
            @PathVariable
            Long idDenominacao) {

        return ResponseEntity.ok(
                service.buscarCentro(
                        idDenominacao
                )
        );
    }

    @PostMapping("/validacoes/minas-gerais")
    public ResponseEntity<Boolean>
    estaIntegralmenteEmMinasGerais(
            @RequestBody
            DenominacaoGeometriaDto dto) {

        return ResponseEntity.ok(
                service.estaIntegralmenteEmMinasGerais(
                        dto
                )
        );
    }

    @PostMapping("/validacoes/municipios/quantidade")
    public ResponseEntity<Long>
    contarMunicipiosIntersectados(
            @RequestBody
            DenominacaoGeometriaDto dto) {

        return ResponseEntity.ok(
                service.contarMunicipiosIntersectados(
                        dto
                )
        );
    }

    @PostMapping("/validacoes/municipios/multiplos")
    public ResponseEntity<Boolean>
    intersectaMaisDeUmMunicipio(
            @RequestBody
            DenominacaoGeometriaDto dto) {

        return ResponseEntity.ok(
                service.intersectaMaisDeUmMunicipio(
                        dto
                )
        );
    }

    @PostMapping("/validacoes/municipios/intersecoes")
    public ResponseEntity<
            List<LimiteMunicipalRepository.MunicipioIntersecao>>
    buscarMunicipiosIntersectados(
            @RequestBody
            DenominacaoGeometriaDto dto) {

        return ResponseEntity.ok(
                service.buscarMunicipiosIntersectados(
                        dto
                )
        );
    }
}
