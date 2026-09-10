package br.gov.mg.meioambiente.slageo.model.converter;

import br.gov.mg.meioambiente.slageo.model.enums.DenominacaoEnum;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = false)
public class DenominacaoEnumConverter
        implements AttributeConverter<
                DenominacaoEnum,
                Integer> {

    @Override
    public Integer convertToDatabaseColumn(
            DenominacaoEnum attribute) {

        if (attribute == null) {
            return null;
        }

        return attribute.getId();
    }

    @Override
    public DenominacaoEnum convertToEntityAttribute(
            Integer dbData) {

        if (dbData == null) {
            return null;
        }

        return DenominacaoEnum.fromId(
                dbData
        );
    }
}
