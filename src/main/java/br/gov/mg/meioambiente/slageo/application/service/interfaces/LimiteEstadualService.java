package br.gov.mg.meioambiente.slageo.application.service.interfaces;

import br.gov.mg.meioambiente.slageo.application.dto.AtividadeGeometriaDto;

public interface LimiteEstadualService {

    boolean validar(
            AtividadeGeometriaDto dto
    );
}
