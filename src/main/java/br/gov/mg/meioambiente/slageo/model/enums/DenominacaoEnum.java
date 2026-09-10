package br.gov.mg.meioambiente.slageo.model.enums;

import java.util.Arrays;

public enum DenominacaoEnum {

    ADA_PRELIMINAR(
            1,
            "Área diretamente afetada (ADA) preliminar"
    ),

    AID_PRELIMINAR(
            2,
            "Área de influência direta (AID) preliminar"
    ),

    AII_PRELIMINAR(
            3,
            "Área de influência indireta (AII) preliminar"
    ),

    DEMAIS_REGULARIZADAS(
            4,
            "Demais tipos de intervenção ambiental, já regularizadas"
    ),

    SUPRESSAO_AUTORIZADA(
            5,
            "Área de supressão de vegetação nativa autorizada"
    ),

    CAPTACAO_SUPERFICIAL(
            6,
            "Ponto(s) de captação de recursos hídricos superficiais"
    ),

    LANCAMENTO_EFLUENTES(
            7,
            "Ponto(s) de lançamento de efluentes e/ou disposição de resíduos"
    ),

    CAPTACAO_SUBTERRANEA(
            8,
            "Ponto(s) de captação de água subterrânea"
    ),

    SUPRESSAO_PEDIDO(
            9,
            "Área de supressão de vegetação nativa objeto do pedido de Autorização para Intervenção Ambiental"
    ),

    INTERVENCAO_APP_REGULARIZADA(
            10,
            "Área de intervenção ambiental em APP, já regularizada"
    ),

    INTERVENCAO_APP_PEDIDO(
            11,
            "Área de intervenção ambiental em APP objeto do pedido de Autorização para Intervenção Ambiental"
    ),

    DEMAIS_PEDIDO(
            12,
            "Demais tipos de intervenção ambiental, objeto do pedido de Autorização para Intervenção Ambiental"
    );

    private final int id;
    private final String descricao;

    DenominacaoEnum(
            int id,
            String descricao) {

        this.id = id;
        this.descricao = descricao;
    }

    public int getId() {
        return id;
    }

    public String getDescricao() {
        return descricao;
    }

    public static DenominacaoEnum fromId(
            Integer id) {

        if (id == null) {
            return null;
        }

        return Arrays.stream(values())
                .filter(tipo -> tipo.id == id)
                .findFirst()
                .orElseThrow(
                        () -> new IllegalArgumentException(
                                "Tipo de denominação inválido: "
                                        + id
                                        + ". Valores permitidos: 1 a 12."
                        )
                );
    }

    public static String getDescricaoById(
            Integer id) {

        DenominacaoEnum tipo =
                fromId(id);

        return tipo != null
                ? tipo.getDescricao()
                : null;
    }
}
