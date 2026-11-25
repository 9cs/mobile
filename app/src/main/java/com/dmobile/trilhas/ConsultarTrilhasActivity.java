package com.dmobile.trilhas;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.dmobile.trilhas.dados.TrilhaDao;
import com.dmobile.trilhas.modelo.Trilha;
import com.dmobile.trilhas.servico.RastreamentoService;
import com.dmobile.trilhas.util.Dialogos;
import com.dmobile.trilhas.util.Formatador;

import java.util.Calendar;
import java.util.List;

/**
 * Tela 3 - Consultar trilhas.
 *
 * Lista todas as trilhas registradas e permite:
 *  - Consultar: toque em uma trilha para vê-la no mapa;
 *  - Editar: toque longo -> "Renomear";
 *  - Apagar: toque longo -> "Apagar" (uma trilha), ou pelo menu da barra
 *    de ação: "Apagar por período" e "Apagar todas".
 */
public class ConsultarTrilhasActivity extends BaseActivity {

    private TrilhaDao dao;
    private TrilhaAdapter adapter;
    private TextView tvResumo;

    // Datas escolhidas no diálogo "Apagar por período"
    private final Calendar dataInicial = Calendar.getInstance();
    private final Calendar dataFinal = Calendar.getInstance();

    @Override
    protected void onCreate(Bundle estadoSalvo) {
        super.onCreate(estadoSalvo);
        setContentView(R.layout.activity_consultar_trilhas);
        habilitarVoltar();

        dao = new TrilhaDao(this);
        adapter = new TrilhaAdapter(this);
        tvResumo = findViewById(R.id.tv_resumo_lista);

        ajustarAsBarrasDoSistema(findViewById(R.id.raiz_consultar));
        ListView lista = findViewById(R.id.lista_trilhas);
        lista.setEmptyView(findViewById(R.id.vazio));
        lista.setAdapter(adapter);

        // Toque simples: consultar a trilha no mapa
        lista.setOnItemClickListener((pai, view, posicao, id) -> abrirTrilha(id));

        // Toque longo: opções da trilha
        lista.setOnItemLongClickListener((pai, view, posicao, id) -> {
            mostrarOpcoes(adapter.getItem(posicao));
            return true;
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        carregarTrilhas();
    }

    /** Recarrega a lista a partir do banco. */
    private void carregarTrilhas() {
        List<Trilha> trilhas = dao.listarTrilhas();
        adapter.setTrilhas(trilhas);
        double total = 0;
        for (Trilha t : trilhas) {
            total += t.getDistancia();
        }
        tvResumo.setText(getResources().getQuantityString(R.plurals.resumo_lista,
                trilhas.size(), trilhas.size(), Formatador.distancia(total)));
        tvResumo.setVisibility(trilhas.isEmpty() ? View.GONE : View.VISIBLE);
        invalidateOptionsMenu();
    }

    private void abrirTrilha(long id) {
        startActivity(new Intent(this, VisualizarTrilhaActivity.class)
                .putExtra(VisualizarTrilhaActivity.EXTRA_TRILHA_ID, id));
    }

    // =====================================================================
    // Opções de uma trilha (toque longo)
    // =====================================================================

    private void mostrarOpcoes(final Trilha trilha) {
        String[] opcoes = {
                getString(R.string.opcao_ver_mapa),
                getString(R.string.opcao_renomear),
                getString(R.string.opcao_apagar)
        };
        new AlertDialog.Builder(this)
                .setTitle(trilha.getNome())
                .setItems(opcoes, (d, qual) -> {
                    if (qual == 0) {
                        abrirTrilha(trilha.getId());
                    } else if (qual == 1) {
                        renomear(trilha);
                    } else {
                        apagar(trilha);
                    }
                })
                .show();
    }

    /** Editar: altera o nome da trilha. */
    private void renomear(final Trilha trilha) {
        Dialogos.pedirNome(this, R.string.renomear_trilha, trilha.getNome(), R.string.salvar, novoNome -> {
            dao.renomear(trilha.getId(), novoNome);
            Toast.makeText(this, R.string.trilha_renomeada, Toast.LENGTH_SHORT).show();
            carregarTrilhas();
        });
    }

    /** Apagar uma trilha específica. */
    private void apagar(final Trilha trilha) {
        if (trilha.getId() == RastreamentoService.getTrilhaEmGravacao()) {
            Toast.makeText(this, R.string.erro_apagar_em_gravacao, Toast.LENGTH_LONG).show();
            return;
        }
        Dialogos.confirmar(this, R.string.apagar_trilha,
                getString(R.string.apagar_trilha_mensagem, trilha.getNome()), R.string.apagar, () -> {
                    dao.apagar(trilha.getId());
                    Toast.makeText(this, R.string.trilha_apagada, Toast.LENGTH_SHORT).show();
                    carregarTrilhas();
                });
    }

    // =====================================================================
    // Menu: apagar por período / apagar todas
    // =====================================================================

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_consultar, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        // Sem trilhas, não há o que apagar
        boolean temTrilhas = adapter != null && adapter.getCount() > 0;
        menu.findItem(R.id.acao_apagar_periodo).setVisible(temTrilhas);
        menu.findItem(R.id.acao_apagar_todas).setVisible(temTrilhas);
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.acao_apagar_periodo) {
            if (!bloquearSeGravando()) mostrarDialogoPeriodo();
            return true;
        } else if (id == R.id.acao_apagar_todas) {
            if (!bloquearSeGravando()) confirmarApagarTodas();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /** Exclusões em lote não são permitidas durante uma gravação. */
    private boolean bloquearSeGravando() {
        if (RastreamentoService.isGravando()) {
            Toast.makeText(this, R.string.erro_apagar_em_gravacao, Toast.LENGTH_LONG).show();
            return true;
        }
        return false;
    }

    private void confirmarApagarTodas() {
        Dialogos.confirmar(this, R.string.apagar_todas,
                getResources().getQuantityString(R.plurals.apagar_todas_mensagem,
                        adapter.getCount(), adapter.getCount()), R.string.apagar, () -> {
                    int apagadas = dao.apagarTodas();
                    Toast.makeText(this, getResources().getQuantityString(
                            R.plurals.trilhas_apagadas, apagadas, apagadas), Toast.LENGTH_SHORT).show();
                    carregarTrilhas();
                });
    }

    /** Diálogo com duas datas (de / até) para apagar as trilhas do intervalo. */
    private void mostrarDialogoPeriodo() {
        View conteudo = getLayoutInflater().inflate(R.layout.dialogo_periodo, null);
        final Button btnInicio = conteudo.findViewById(R.id.btn_data_inicial);
        final Button btnFim = conteudo.findViewById(R.id.btn_data_final);

        // Sugestão: do dia da trilha mais antiga até hoje
        dataFinal.setTimeInMillis(System.currentTimeMillis());
        if (adapter.getCount() > 0) {
            dataInicial.setTimeInMillis(adapter.getItem(adapter.getCount() - 1).getDataInicio());
        } else {
            dataInicial.setTimeInMillis(System.currentTimeMillis());
        }
        btnInicio.setText(Formatador.data(dataInicial.getTimeInMillis()));
        btnFim.setText(Formatador.data(dataFinal.getTimeInMillis()));

        btnInicio.setOnClickListener(v -> escolherData(dataInicial, btnInicio));
        btnFim.setOnClickListener(v -> escolherData(dataFinal, btnFim));

        new AlertDialog.Builder(this)
                .setTitle(R.string.apagar_periodo)
                .setView(conteudo)
                .setPositiveButton(R.string.continuar, (d, w) -> confirmarApagarPeriodo())
                .setNegativeButton(R.string.cancelar, null)
                .show();
    }

    private void escolherData(final Calendar data, final Button botao) {
        new DatePickerDialog(this, (picker, ano, mes, dia) -> {
            data.set(ano, mes, dia);
            botao.setText(Formatador.data(data.getTimeInMillis()));
        }, data.get(Calendar.YEAR), data.get(Calendar.MONTH), data.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void confirmarApagarPeriodo() {
        // Intervalo: início do primeiro dia (00:00:00) até o fim do último (23:59:59.999)
        final long inicio = inicioDoDia(dataInicial);
        final long fim = fimDoDia(dataFinal);
        if (inicio > fim) {
            Toast.makeText(this, R.string.erro_periodo_invalido, Toast.LENGTH_LONG).show();
            return;
        }
        long quantidade = dao.contarNoIntervalo(inicio, fim);
        if (quantidade == 0) {
            Toast.makeText(this, R.string.nenhuma_trilha_periodo, Toast.LENGTH_LONG).show();
            return;
        }
        String mensagem = getResources().getQuantityString(R.plurals.apagar_periodo_mensagem,
                (int) quantidade, (int) quantidade, Formatador.data(inicio), Formatador.data(fim));
        Dialogos.confirmar(this, R.string.apagar_periodo, mensagem, R.string.apagar, () -> {
            int apagadas = dao.apagarIntervalo(inicio, fim);
            Toast.makeText(this, getResources().getQuantityString(
                    R.plurals.trilhas_apagadas, apagadas, apagadas), Toast.LENGTH_SHORT).show();
            carregarTrilhas();
        });
    }

    private static long inicioDoDia(Calendar data) {
        Calendar c = (Calendar) data.clone();
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTimeInMillis();
    }

    private static long fimDoDia(Calendar data) {
        Calendar c = (Calendar) data.clone();
        c.set(Calendar.HOUR_OF_DAY, 23);
        c.set(Calendar.MINUTE, 59);
        c.set(Calendar.SECOND, 59);
        c.set(Calendar.MILLISECOND, 999);
        return c.getTimeInMillis();
    }
}
