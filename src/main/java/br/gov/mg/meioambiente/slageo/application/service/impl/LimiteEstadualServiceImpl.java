package br.gov.mg.meioambiente.slageo.application.service.impl;

import br.gov.mg.meioambiente.slageo.application.dto.AtividadeGeometriaDto;
import br.gov.mg.meioambiente.slageo.application.service.interfaces.LimiteEstadualService;
import br.gov.mg.meioambiente.slageo.repository.interfaces.LimiteEstadualRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LimiteEstadualServiceImpl
        implements LimiteEstadualService {

    private final LimiteEstadualRepository repository;

    public LimiteEstadualServiceImpl(
            LimiteEstadualRepository repository) {

        this.repository = repository;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean validar(
            AtividadeGeometriaDto dto) {

        return repository.estaIntegralmenteEmMinasGerais(
                dto.getGeoJson().toString()
        );
    }
}
