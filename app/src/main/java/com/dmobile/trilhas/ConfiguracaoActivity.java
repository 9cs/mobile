package com.dmobile.trilhas;

import android.os.Bundle;
import android.widget.RadioGroup;
import android.widget.Toast;

import com.dmobile.trilhas.util.Preferencias;

/**
 * Tela 1 - Configuração.
 *
 * Permite escolher:
 *  - Tipo de mapa: vetorial (padrão) ou satélite;
 *  - Forma de navegação: North Up (padrão) ou Course Up.
 *
 * As escolhas são gravadas imediatamente em SharedPreferences
 * ({@link Preferencias}) e respeitadas pelas telas que exibem mapas.
 */
public class ConfiguracaoActivity extends BaseActivity {

    private RadioGroup grupoTipoMapa;
    private RadioGroup grupoNavegacao;

    @Override
    protected void onCreate(Bundle estadoSalvo) {
        super.onCreate(estadoSalvo);
        setContentView(R.layout.activity_configuracao);
        habilitarVoltar();
        ajustarAsBarrasDoSistema(findViewById(R.id.conteudo_configuracao));

        grupoTipoMapa = findViewById(R.id.grupo_tipo_mapa);
        grupoNavegacao = findViewById(R.id.grupo_navegacao);

        carregarConfiguracoes();

        // Salva assim que o usuário muda a opção
        grupoTipoMapa.setOnCheckedChangeListener((grupo, idMarcado) -> {
            int tipo = idMarcado == R.id.opcao_satelite
                    ? Preferencias.MAPA_SATELITE
                    : Preferencias.MAPA_VETORIAL;
            Preferencias.setTipoMapa(this, tipo);
            avisarSalvo();
        });
        grupoNavegacao.setOnCheckedChangeListener((grupo, idMarcado) -> {
            int navegacao = idMarcado == R.id.opcao_course_up
                    ? Preferencias.NAVEGACAO_COURSE_UP
                    : Preferencias.NAVEGACAO_NORTH_UP;
            Preferencias.setNavegacao(this, navegacao);
            avisarSalvo();
        });

        findViewById(R.id.btn_restaurar_padrao).setOnClickListener(v -> {
            // Volta para os valores padrão (vetorial + North Up)
            grupoTipoMapa.check(R.id.opcao_vetorial);
            grupoNavegacao.check(R.id.opcao_north_up);
        });
    }

    /** Marca os botões de acordo com os valores salvos. */
    private void carregarConfiguracoes() {
        grupoTipoMapa.check(Preferencias.getTipoMapa(this) == Preferencias.MAPA_SATELITE
                ? R.id.opcao_satelite : R.id.opcao_vetorial);
        grupoNavegacao.check(Preferencias.isCourseUp(this)
                ? R.id.opcao_course_up : R.id.opcao_north_up);
    }

    private void avisarSalvo() {
        Toast.makeText(this, R.string.configuracao_salva, Toast.LENGTH_SHORT).show();
    }
}
