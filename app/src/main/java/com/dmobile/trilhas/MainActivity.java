package com.dmobile.trilhas;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import com.dmobile.trilhas.dados.TrilhaDao;
import com.dmobile.trilhas.servico.RastreamentoService;
import com.dmobile.trilhas.util.Formatador;

/**
 * Tela inicial: menu de acesso às três funcionalidades do aplicativo
 * (Registrar trilha, Consultar trilhas e Configuração) e um resumo
 * das trilhas já registradas.
 */
public class MainActivity extends BaseActivity {

    private TextView tvTotalTrilhas;
    private TextView tvTotalDistancia;
    private TextView tvSubtituloRegistrar;
    private View indicadorGravando;
    private TrilhaDao dao;

    @Override
    protected void onCreate(Bundle estadoSalvo) {
        super.onCreate(estadoSalvo);
        setContentView(R.layout.activity_main);
        ajustarAsBarrasDoSistema(findViewById(R.id.conteudo_principal));

        dao = new TrilhaDao(this);
        tvTotalTrilhas = findViewById(R.id.tv_total_trilhas);
        tvTotalDistancia = findViewById(R.id.tv_total_distancia);
        tvSubtituloRegistrar = findViewById(R.id.tv_subtitulo_registrar);
        indicadorGravando = findViewById(R.id.indicador_gravando);

        findViewById(R.id.cartao_registrar).setOnClickListener(v ->
                startActivity(new Intent(this, RegistrarTrilhaActivity.class)));
        findViewById(R.id.cartao_consultar).setOnClickListener(v ->
                startActivity(new Intent(this, ConsultarTrilhasActivity.class)));
        findViewById(R.id.cartao_configuracao).setOnClickListener(v ->
                startActivity(new Intent(this, ConfiguracaoActivity.class)));
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Atualiza o resumo sempre que a tela volta a ficar visível
        long total = dao.contarTrilhas();
        tvTotalTrilhas.setText(String.valueOf(total));
        ((TextView) findViewById(R.id.tv_rotulo_trilhas)).setText(
                total == 1 ? R.string.rotulo_trilha_singular : R.string.rotulo_trilhas);
        tvTotalDistancia.setText(Formatador.distancia(dao.somarDistancias()));

        boolean gravando = RastreamentoService.isGravando();
        indicadorGravando.setVisibility(gravando ? View.VISIBLE : View.GONE);
        tvSubtituloRegistrar.setText(gravando
                ? R.string.menu_registrar_gravando
                : R.string.menu_registrar_descricao);
    }
}
