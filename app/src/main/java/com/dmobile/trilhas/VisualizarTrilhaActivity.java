package com.dmobile.trilhas;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.dmobile.trilhas.dados.TrilhaDao;
import com.dmobile.trilhas.modelo.Ponto;
import com.dmobile.trilhas.modelo.Trilha;
import com.dmobile.trilhas.servico.RastreamentoService;
import com.dmobile.trilhas.util.Dialogos;
import com.dmobile.trilhas.util.Formatador;
import com.dmobile.trilhas.util.MapaUtils;
import com.dmobile.trilhas.util.MarcadorFactory;
import com.dmobile.trilhas.util.Preferencias;

import org.osmdroid.util.BoundingBox;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.CustomZoomButtonsController;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.CopyrightOverlay;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Polyline;

import java.util.ArrayList;
import java.util.List;

/**
 * Tela 3 (detalhe) - Visualizar trilha.
 *
 * Desenha no mapa (OpenStreetMap, biblioteca osmdroid) o trajeto realizado,
 * com marcadores personalizados de início e fim, e mostra sobreposto ao mapa,
 * em texto: data/hora de início, velocidade média e máxima, distância total e
 * duração da trilha.
 */
public class VisualizarTrilhaActivity extends BaseActivity {

    /** Extra obrigatório: id da trilha a exibir. */
    public static final String EXTRA_TRILHA_ID = "trilha_id";

    /** Zoom usado quando a trilha tem um único ponto. */
    private static final double ZOOM_PONTO_UNICO = 17.0;

    private MapView mapa;
    private View camadaInfo;
    private View painelInfo;
    private CopyrightOverlay creditos;
    private TrilhaDao dao;
    private Trilha trilha;
    private final List<GeoPoint> percurso = new ArrayList<>();

    @Override
    protected void onCreate(Bundle estadoSalvo) {
        super.onCreate(estadoSalvo);
        setContentView(R.layout.activity_visualizar_trilha);
        habilitarVoltar();
        camadaInfo = findViewById(R.id.camada_info);
        ajustarAsBarrasDoSistema(camadaInfo);

        dao = new TrilhaDao(this);
        long id = getIntent().getLongExtra(EXTRA_TRILHA_ID, -1);
        trilha = dao.buscarTrilha(id);
        if (trilha == null) {
            Toast.makeText(this, R.string.trilha_nao_encontrada, Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        for (Ponto p : dao.listarPontos(id)) {
            percurso.add(new GeoPoint(p.getLatitude(), p.getLongitude()));
        }

        painelInfo = findViewById(R.id.painel_info);
        preencherInformacoes();

        mapa = findViewById(R.id.mapa);
        configurarMapa();
    }

    /** Preenche o painel de informações sobreposto ao mapa. */
    private void preencherInformacoes() {
        setTitle(trilha.getNome());
        ((TextView) findViewById(R.id.tv_nome)).setText(trilha.getNome());
        ((TextView) findViewById(R.id.tv_inicio)).setText(
                getString(R.string.rotulo_inicio, Formatador.dataHora(trilha.getDataInicio())));
        ((TextView) findViewById(R.id.tv_duracao)).setText(Formatador.duracao(trilha.getDuracao()));
        ((TextView) findViewById(R.id.tv_distancia)).setText(Formatador.distancia(trilha.getDistancia()));
        ((TextView) findViewById(R.id.tv_vel_media)).setText(Formatador.velocidade(trilha.getVelocidadeMedia()));
        ((TextView) findViewById(R.id.tv_vel_maxima)).setText(Formatador.velocidade(trilha.getVelocidadeMaxima()));
    }

    // ----- Ciclo de vida do MapView -----

    @Override
    protected void onResume() {
        super.onResume();
        if (mapa != null) {
            mapa.onResume();
            MapaUtils.aplicarTipoMapa(this, mapa);
        }
    }

    @Override
    protected void onPause() {
        if (mapa != null) mapa.onPause();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        if (mapa != null) mapa.onDetach();
        super.onDestroy();
    }

    // =====================================================================
    // Mapa
    // =====================================================================

    private void configurarMapa() {
        creditos = MapaUtils.configurarMapa(this, mapa);
        mapa.getZoomController().setVisibility(CustomZoomButtonsController.Visibility.SHOW_AND_FADEOUT);

        if (percurso.isEmpty()) {
            Toast.makeText(this, R.string.trilha_sem_pontos, Toast.LENGTH_LONG).show();
            return;
        }

        float d = getResources().getDisplayMetrics().density;
        // Contorno branco + linha colorida: o trajeto fica visível também no satélite
        mapa.getOverlays().add(criarLinha(getResources().getColor(R.color.contorno_percurso), 9 * d));
        mapa.getOverlays().add(criarLinha(getResources().getColor(R.color.linha_percurso), 5 * d));

        // Marcadores personalizados: bandeira verde no início e bandeira quadriculada
        // de chegada no fim; tocar numa bandeira mostra a data/hora
        mapa.getOverlays().add(criarBandeira(percurso.get(0),
                MarcadorFactory.criarBandeiraInicio(this), R.string.inicio, trilha.getDataInicio()));
        if (percurso.size() > 1) {
            mapa.getOverlays().add(criarBandeira(percurso.get(percurso.size() - 1),
                    MarcadorFactory.criarBandeiraChegada(this), R.string.fim, trilha.getDataFim()));
        }

        // O enquadramento precisa do tamanho da tela: espera o primeiro layout
        mapa.addOnFirstLayoutListener((v, l, t, r, b) -> enquadrarPercurso());
    }

    private Polyline criarLinha(int cor, float largura) {
        Polyline linha = new Polyline(mapa);
        linha.setPoints(percurso);
        linha.getOutlinePaint().setColor(cor);
        linha.getOutlinePaint().setStrokeWidth(largura);
        linha.setInfoWindow(null);
        return linha;
    }

    /** Bandeira fincada no ponto: a âncora é a base do mastro (centro, embaixo). */
    private Marker criarBandeira(GeoPoint posicao, Drawable icone,
                                 int titulo, long dataHora) {
        Marker bandeira = new Marker(mapa);
        bandeira.setPosition(posicao);
        bandeira.setIcon(icone);
        bandeira.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        bandeira.setTitle(getString(titulo));
        bandeira.setSnippet(Formatador.dataHora(dataHora));
        return bandeira;
    }

    /**
     * Ajusta o mapa para mostrar o trajeto inteiro na área livre (abaixo do
     * painel de informações).
     * North Up: mapa voltado para o norte. Course Up: o mapa é girado para
     * que o rumo geral da trilha (do início ao fim) aponte para o topo.
     */
    private void enquadrarPercurso() {
        int altura = mapa.getHeight();
        int topoLivre = painelInfo.getBottom();
        int baixoLivre = altura - camadaInfo.getPaddingBottom();
        if (altura == 0 || baixoLivre <= topoLivre) {
            return;
        }
        // Desloca o "centro" do mapa para o meio da área livre
        int centroLivre = (topoLivre + baixoLivre) / 2;
        mapa.setMapCenterOffset(0, centroLivre - altura / 2);

        // Créditos do mapa logo acima da barra de navegação
        int margem = Math.round(6 * getResources().getDisplayMetrics().density);
        creditos.setOffset(margem, camadaInfo.getPaddingBottom());

        boolean courseUp = Preferencias.isCourseUp(this) && percurso.size() > 1;
        if (percurso.size() == 1) {
            mapa.getController().setZoom(ZOOM_PONTO_UNICO);
            mapa.getController().setCenter(percurso.get(0));
        } else {
            // Amplia a área para compensar a parte coberta pelo painel
            // (e a rotação, no Course Up) e deixar uma margem em volta
            float escala = (float) altura / (baixoLivre - topoLivre) * (courseUp ? 1.6f : 1.25f);
            BoundingBox limites = BoundingBox.fromGeoPointsSafe(percurso).increaseByScale(escala);
            mapa.zoomToBoundingBox(limites, false);
        }
        if (courseUp) {
            double rumo = percurso.get(0).bearingTo(percurso.get(percurso.size() - 1));
            mapa.setMapOrientation(-(float) rumo);
        } else {
            mapa.setMapOrientation(0f);
        }
        mapa.invalidate();
    }

    // =====================================================================
    // Menu: renomear / apagar a trilha exibida
    // =====================================================================

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_visualizar, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.acao_renomear) {
            Dialogos.pedirNome(this, R.string.renomear_trilha, trilha.getNome(), R.string.salvar, novoNome -> {
                dao.renomear(trilha.getId(), novoNome);
                trilha.setNome(novoNome);
                preencherInformacoes();
                Toast.makeText(this, R.string.trilha_renomeada, Toast.LENGTH_SHORT).show();
            });
            return true;
        } else if (id == R.id.acao_apagar) {
            if (trilha.getId() == RastreamentoService.getTrilhaEmGravacao()) {
                Toast.makeText(this, R.string.erro_apagar_em_gravacao, Toast.LENGTH_LONG).show();
                return true;
            }
            Dialogos.confirmar(this, R.string.apagar_trilha,
                    getString(R.string.apagar_trilha_mensagem, trilha.getNome()), R.string.apagar, () -> {
                        dao.apagar(trilha.getId());
                        Toast.makeText(this, R.string.trilha_apagada, Toast.LENGTH_SHORT).show();
                        finish();
                    });
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
