package com.dmobile.trilhas;

import android.Manifest;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.SystemClock;
import android.provider.Settings;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.dmobile.trilhas.servico.RastreamentoService;
import com.dmobile.trilhas.util.Dialogos;
import com.dmobile.trilhas.util.Formatador;
import com.dmobile.trilhas.util.MapaUtils;
import com.dmobile.trilhas.util.MarcadorFactory;
import com.dmobile.trilhas.util.Preferencias;

import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.CustomZoomButtonsController;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.CopyrightOverlay;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Polygon;
import org.osmdroid.views.overlay.Polyline;

import java.util.ArrayList;
import java.util.List;

/**
 * Tela 2 - Registrar trilha.
 *
 * Mostra a posição atual em um mapa (OpenStreetMap, biblioteca osmdroid) com
 * marcador personalizado e círculo de acurácia. Sobre o mapa ficam:
 * velocidade instantânea e máxima, cronômetro e distância percorrida.
 * A gravação em si é feita pelo {@link RastreamentoService}; esta tela
 * apenas exibe os dados.
 *
 * Quando não há gravação, a tela escuta o GPS diretamente ("pré-visualização")
 * para que o usuário veja onde está antes de iniciar.
 */
public class RegistrarTrilhaActivity extends BaseActivity
        implements LocationListener, RastreamentoService.Ouvinte {

    private static final int REQ_PERMISSOES = 10;
    private static final String CHAVE_PERMISSAO_PEDIDA = "permissao_pedida";

    /** Zoom ao localizar o usuário: nível de rua, em que o círculo de ±5 m já aparece. */
    private static final double ZOOM_INICIAL = 19.0;
    /** Zoom e centro (Brasil) usados enquanto ainda não há posição do GPS. */
    private static final double ZOOM_SEM_POSICAO = 4.0;
    private static final GeoPoint CENTRO_PADRAO = new GeoPoint(-14.2, -51.9);
    /** Duração da animação da câmera ao seguir o usuário (ms). */
    private static final long DURACAO_ANIMACAO = 700;
    /** Abaixo desta velocidade (m/s) o rumo informado pelo GPS não é confiável. */
    private static final float VELOCIDADE_MIN_RUMO = 0.8f;
    /** Deslocamento mínimo (m) para calcular o rumo entre duas leituras. */
    private static final float DESLOCAMENTO_MIN_RUMO = 3f;

    // ----- Componentes da interface -----
    private MapView mapa;
    private View camadaControles;
    private View painelTopo;
    private View painelBaixo;
    private TextView tvVelocidadeAtual;
    private TextView tvVelocidadeMaxima;
    private TextView tvDistancia;
    private TextView tvPrecisao;
    private TextView tvStatus;
    private TextView tvModo;
    private Chronometer cronometro;
    private Button btnIniciarParar;
    private ImageButton btnCentralizar;

    // ----- Elementos desenhados no mapa -----
    private Marker marcadorUsuario;
    private Marker indicadorRumo;
    private Polygon circuloPrecisao;
    private Polyline linhaPercurso;
    private CopyrightOverlay creditos;
    private final List<GeoPoint> percursoDesenhado = new ArrayList<>();

    // ----- Estado -----
    private LocationManager gerenciadorLocal;
    private RastreamentoService servico;
    private boolean vinculado;
    private boolean escutandoPrevia;
    private boolean permissaoPedida;
    private boolean seguirUsuario = true;
    private boolean primeiraPosicao = true;
    private float ultimoRumo = 0f;
    private Location ultimaPosicao;

    /** Conexão com o serviço de rastreamento. */
    private final ServiceConnection conexao = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName nome, IBinder binder) {
            servico = ((RastreamentoService.LocalBinder) binder).getServico();
            vinculado = true;
            servico.setOuvinte(RegistrarTrilhaActivity.this);
            if (RastreamentoService.isGravando()) {
                sincronizarComServico();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName nome) {
            servico = null;
            vinculado = false;
        }
    };

    // =====================================================================
    // Ciclo de vida
    // =====================================================================

    @Override
    protected void onCreate(Bundle estadoSalvo) {
        super.onCreate(estadoSalvo);
        setContentView(R.layout.activity_registrar_trilha);
        habilitarVoltar();

        mapa = findViewById(R.id.mapa);
        camadaControles = findViewById(R.id.camada_controles);
        painelTopo = findViewById(R.id.painel_topo);
        painelBaixo = findViewById(R.id.painel_baixo);
        tvVelocidadeAtual = findViewById(R.id.tv_velocidade_atual);
        tvVelocidadeMaxima = findViewById(R.id.tv_velocidade_maxima);
        tvDistancia = findViewById(R.id.tv_distancia);
        tvPrecisao = findViewById(R.id.tv_precisao);
        tvStatus = findViewById(R.id.tv_status);
        tvModo = findViewById(R.id.tv_modo_navegacao);
        cronometro = findViewById(R.id.cronometro);
        btnIniciarParar = findViewById(R.id.btn_iniciar_parar);
        btnCentralizar = findViewById(R.id.btn_centralizar);

        ajustarAsBarrasDoSistema(camadaControles);

        gerenciadorLocal = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (estadoSalvo != null) {
            permissaoPedida = estadoSalvo.getBoolean(CHAVE_PERMISSAO_PEDIDA, false);
        }

        configurarMapa();

        btnIniciarParar.setOnClickListener(v -> {
            if (RastreamentoService.isGravando()) {
                confirmarFinalizacao();
            } else {
                pedirNomeEIniciar();
            }
        });
        btnCentralizar.setOnClickListener(v -> {
            seguirUsuario = true;
            btnCentralizar.setVisibility(View.GONE);
            if (ultimaPosicao != null) {
                moverCamera(ultimaPosicao, true);
            }
        });

        // Mantém os créditos do mapa logo acima do botão inferior
        camadaControles.addOnLayoutChangeListener((v, l, t, r, b, ol, ot, or, ob) -> posicionarCreditos());

        mostrarEstadoParado();
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (RastreamentoService.isGravando()) {
            // Reconecta à gravação em andamento
            bindService(new Intent(this, RastreamentoService.class), conexao, Context.BIND_AUTO_CREATE);
        } else if (temPermissaoLocalizacao()) {
            iniciarPrevia();
        } else if (!permissaoPedida) {
            pedirPermissoes();
        } else {
            tvStatus.setText(R.string.status_sem_permissao);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        mapa.onResume();
        // As configurações podem ter sido alteradas em outra tela
        aplicarConfiguracoes();
    }

    @Override
    protected void onPause() {
        mapa.onPause();
        super.onPause();
    }

    @Override
    protected void onStop() {
        pararPrevia();
        desvincularServico();
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        mapa.onDetach();
        super.onDestroy();
    }

    @Override
    protected void onSaveInstanceState(Bundle estado) {
        super.onSaveInstanceState(estado);
        estado.putBoolean(CHAVE_PERMISSAO_PEDIDA, permissaoPedida);
    }

    @Override
    public void finish() {
        if (RastreamentoService.isGravando()) {
            Toast.makeText(this, R.string.gravacao_continua, Toast.LENGTH_SHORT).show();
        }
        super.finish();
    }

    // =====================================================================
    // Menu da ActionBar (atalho para as configurações)
    // =====================================================================

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_registrar, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.acao_configuracoes) {
            startActivity(new Intent(this, ConfiguracaoActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // =====================================================================
    // Mapa
    // =====================================================================

    /** Cria a linha do percurso, o círculo de acurácia e o marcador do usuário. */
    private void configurarMapa() {
        creditos = MapaUtils.configurarMapa(this, mapa);
        // Sem botões de zoom: o usuário usa gestos de pinça
        mapa.getZoomController().setVisibility(CustomZoomButtonsController.Visibility.NEVER);
        mapa.getController().setZoom(ZOOM_SEM_POSICAO);
        mapa.getController().setCenter(CENTRO_PADRAO);

        // Se o usuário arrastar o mapa, para de seguir e mostra o botão de centralizar
        mapa.setOnTouchListener((v, evento) -> {
            if (evento.getActionMasked() == MotionEvent.ACTION_MOVE && seguirUsuario) {
                seguirUsuario = false;
                btnCentralizar.setVisibility(View.VISIBLE);
            }
            return false; // não consome o toque: o mapa continua respondendo
        });

        float d = getResources().getDisplayMetrics().density;
        int corUsuario = getResources().getColor(R.color.marcador_usuario);

        // Linha do percurso gravado
        linhaPercurso = new Polyline(mapa);
        linhaPercurso.getOutlinePaint().setColor(getResources().getColor(R.color.linha_percurso));
        linhaPercurso.getOutlinePaint().setStrokeWidth(6 * d);
        linhaPercurso.setInfoWindow(null);

        // Círculo semitransparente: raio = precisão informada pelo GPS
        circuloPrecisao = new Polygon(mapa);
        circuloPrecisao.getFillPaint().setColor(
                Color.argb(50, Color.red(corUsuario), Color.green(corUsuario), Color.blue(corUsuario)));
        circuloPrecisao.getOutlinePaint().setColor(
                Color.argb(140, Color.red(corUsuario), Color.green(corUsuario), Color.blue(corUsuario)));
        circuloPrecisao.getOutlinePaint().setStrokeWidth(2 * d);
        circuloPrecisao.setInfoWindow(null);
        circuloPrecisao.setVisible(false);

        // Indicador de rumo: triângulo que gira em volta do usuário (desenhado com Canvas)
        indicadorRumo = new Marker(mapa);
        indicadorRumo.setIcon(MarcadorFactory.criarIndicadorRumo(this));
        indicadorRumo.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER);
        indicadorRumo.setFlat(true); // gira junto com o mapa (aponta o rumo real)
        indicadorRumo.setInfoWindow(null);
        indicadorRumo.setOnMarkerClickListener((m, v) -> true);
        indicadorRumo.setVisible(false);

        // Marcador personalizado do usuário: círculo verde com a pessoa caminhando.
        // Não é "flat": fica sempre em pé, mesmo com o mapa girado (Course Up).
        marcadorUsuario = new Marker(mapa);
        marcadorUsuario.setIcon(MarcadorFactory.criarMarcadorUsuario(this));
        marcadorUsuario.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER);
        marcadorUsuario.setInfoWindow(null);
        marcadorUsuario.setOnMarkerClickListener((m, v) -> true);
        marcadorUsuario.setVisible(false);

        // Ordem de desenho: linha, círculo, indicador de rumo e, por cima, o marcador
        mapa.getOverlays().add(linhaPercurso);
        mapa.getOverlays().add(circuloPrecisao);
        mapa.getOverlays().add(indicadorRumo);
        mapa.getOverlays().add(marcadorUsuario);
    }

    /** Aplica tipo de mapa e modo de navegação definidos na tela de configuração. */
    private void aplicarConfiguracoes() {
        boolean courseUp = Preferencias.isCourseUp(this);
        tvModo.setText(courseUp ? R.string.navegacao_course_up : R.string.navegacao_north_up);
        MapaUtils.aplicarTipoMapa(this, mapa);
        if (ultimaPosicao != null && seguirUsuario) {
            moverCamera(ultimaPosicao, false);
        } else if (!courseUp) {
            mapa.setMapOrientation(0f); // North Up: mapa voltado para o norte
        }
    }

    /** Coloca o texto de créditos do mapa acima do botão Iniciar/Finalizar. */
    private void posicionarCreditos() {
        if (creditos == null || camadaControles.getHeight() == 0) {
            return;
        }
        int margem = Math.round(6 * getResources().getDisplayMetrics().density);
        creditos.setOffset(margem + camadaControles.getPaddingLeft(),
                camadaControles.getHeight() - painelBaixo.getTop() + margem);
        mapa.invalidate();
    }

    /** Atualiza marcador, círculo de acurácia e câmera com a nova posição. */
    private void atualizarPosicaoNoMapa(Location local) {
        Location anterior = ultimaPosicao;
        ultimaPosicao = local;
        atualizarRumo(anterior, local);

        float precisao = local.hasAccuracy() ? local.getAccuracy() : 0f;
        tvPrecisao.setText(getString(R.string.rotulo_precisao, Formatador.precisao(precisao)));

        GeoPoint posicao = new GeoPoint(local.getLatitude(), local.getLongitude());

        marcadorUsuario.setPosition(posicao);
        marcadorUsuario.setVisible(true);

        indicadorRumo.setPosition(posicao);
        // O osmdroid gira o marcador no sentido anti-horário; o rumo é horário
        indicadorRumo.setRotation(-ultimoRumo);
        indicadorRumo.setVisible(true);

        circuloPrecisao.setPoints(Polygon.pointsAsCircle(posicao, Math.max(precisao, 1f)));
        circuloPrecisao.setVisible(precisao > 0);

        if (seguirUsuario) {
            moverCamera(local, !primeiraPosicao);
            primeiraPosicao = false;
        }
        mapa.invalidate();
    }

    /**
     * Rumo do deslocamento (graus a partir do norte, sentido horário). Usa o
     * informado pelo GPS quando o usuário está em movimento; senão, calcula a
     * direção entre a leitura anterior e a atual, se o deslocamento superar
     * o erro do GPS. Parado, mantém o último rumo conhecido.
     */
    private void atualizarRumo(Location anterior, Location atual) {
        if (atual.hasBearing() && atual.hasSpeed() && atual.getSpeed() >= VELOCIDADE_MIN_RUMO) {
            ultimoRumo = atual.getBearing();
        } else if (anterior != null) {
            float limiar = Math.max(DESLOCAMENTO_MIN_RUMO, atual.hasAccuracy() ? atual.getAccuracy() : 0f);
            if (anterior.distanceTo(atual) > limiar) {
                ultimoRumo = (anterior.bearingTo(atual) + 360f) % 360f;
            }
        }
    }

    /**
     * Posiciona o mapa sobre o usuário respeitando o modo de navegação:
     * North Up (orientação 0) ou Course Up (mapa girado para que o rumo do
     * deslocamento aponte para o topo da tela).
     */
    private void moverCamera(Location local, boolean animar) {
        GeoPoint alvo = new GeoPoint(local.getLatitude(), local.getLongitude());
        float orientacao = Preferencias.isCourseUp(this) ? -ultimoRumo : 0f;
        double zoom = primeiraPosicao ? ZOOM_INICIAL : mapa.getZoomLevelDouble();
        if (animar) {
            mapa.getController().animateTo(alvo, zoom, DURACAO_ANIMACAO, orientacao);
        } else {
            mapa.getController().setZoom(zoom);
            mapa.getController().setCenter(alvo);
            mapa.setMapOrientation(orientacao);
        }
    }

    // =====================================================================
    // Permissões e pré-visualização (sem gravação)
    // =====================================================================

    private boolean temPermissaoLocalizacao() {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.M
                || checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    private void pedirPermissoes() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return;
        }
        permissaoPedida = true;
        List<String> permissoes = new ArrayList<>();
        permissoes.add(Manifest.permission.ACCESS_FINE_LOCATION);
        permissoes.add(Manifest.permission.ACCESS_COARSE_LOCATION);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Para exibir a notificação da gravação em segundo plano
            permissoes.add(Manifest.permission.POST_NOTIFICATIONS);
        }
        requestPermissions(permissoes.toArray(new String[0]), REQ_PERMISSOES);
    }

    @Override
    public void onRequestPermissionsResult(int codigo, String[] permissoes, int[] resultados) {
        super.onRequestPermissionsResult(codigo, permissoes, resultados);
        if (codigo != REQ_PERMISSOES) {
            return;
        }
        if (temPermissaoLocalizacao()) {
            iniciarPrevia();
        } else {
            tvStatus.setText(R.string.status_sem_permissao);
            Toast.makeText(this, R.string.permissao_necessaria, Toast.LENGTH_LONG).show();
        }
    }

    /** Liga o GPS apenas para mostrar a posição antes de iniciar a gravação. */
    private void iniciarPrevia() {
        if (escutandoPrevia || RastreamentoService.isGravando() || !temPermissaoLocalizacao()) {
            return;
        }
        if (!gerenciadorLocal.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            tvStatus.setText(R.string.status_gps_desligado);
            sugerirLigarGps();
            return;
        }
        try {
            // Mostra rapidamente a última posição conhecida enquanto o GPS fixa o sinal
            Location ultima = gerenciadorLocal.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            if (ultima == null && gerenciadorLocal.getAllProviders().contains(LocationManager.NETWORK_PROVIDER)) {
                ultima = gerenciadorLocal.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            }
            if (ultima != null) {
                atualizarPosicaoNoMapa(ultima);
            }
            gerenciadorLocal.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 0f, this);
            escutandoPrevia = true;
            tvStatus.setText(R.string.status_aguardando_sinal);
        } catch (SecurityException e) {
            tvStatus.setText(R.string.status_sem_permissao);
        }
    }

    private void pararPrevia() {
        if (escutandoPrevia) {
            try {
                gerenciadorLocal.removeUpdates(this);
            } catch (SecurityException ignorada) {
                // nada a fazer
            }
            escutandoPrevia = false;
        }
    }

    private void sugerirLigarGps() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.gps_desligado_titulo)
                .setMessage(R.string.gps_desligado_mensagem)
                .setPositiveButton(R.string.abrir_configuracoes, (d, w) ->
                        startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)))
                .setNegativeButton(R.string.cancelar, null)
                .show();
    }

    // LocationListener da pré-visualização
    @Override
    public void onLocationChanged(Location local) {
        tvStatus.setText(R.string.status_pronto);
        double velocidade = local.hasSpeed() && local.getSpeed() > 0
                ? local.getSpeed()
                : RastreamentoService.calcularVelocidade(ultimaPosicao, local);
        tvVelocidadeAtual.setText(Formatador.velocidadeNumero(velocidade));
        atualizarPosicaoNoMapa(local);
    }

    @Override
    public void onProviderDisabled(String provedor) {
        tvStatus.setText(R.string.status_gps_desligado);
    }

    @Override
    public void onProviderEnabled(String provedor) {
        tvStatus.setText(R.string.status_aguardando_sinal);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onStatusChanged(String provedor, int status, Bundle extras) {
    }

    // =====================================================================
    // Início e fim da gravação
    // =====================================================================

    private void pedirNomeEIniciar() {
        if (!temPermissaoLocalizacao()) {
            pedirPermissoes();
            return;
        }
        if (!gerenciadorLocal.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            sugerirLigarGps();
            return;
        }
        String sugestao = getString(R.string.nome_trilha_padrao,
                Formatador.dataHoraCurta(System.currentTimeMillis()));
        Dialogos.pedirNome(this, R.string.nova_trilha, sugestao, R.string.iniciar, this::iniciarGravacao);
    }

    /** Inicia o serviço em primeiro plano e conecta a tela a ele. */
    private void iniciarGravacao(String nome) {
        pararPrevia();
        Intent intent = new Intent(this, RastreamentoService.class)
                .setAction(RastreamentoService.ACAO_INICIAR)
                .putExtra(RastreamentoService.EXTRA_NOME, nome);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent);
        } else {
            startService(intent);
        }
        bindService(new Intent(this, RastreamentoService.class), conexao, Context.BIND_AUTO_CREATE);

        // Limpa o percurso de uma gravação anterior
        percursoDesenhado.clear();
        if (linhaPercurso != null) {
            linhaPercurso.setPoints(percursoDesenhado);
        }
    }

    private void confirmarFinalizacao() {
        Dialogos.confirmar(this, R.string.finalizar_trilha,
                getString(R.string.finalizar_trilha_mensagem), R.string.finalizar, this::finalizarGravacao);
    }

    private void finalizarGravacao() {
        if (servico == null) {
            return;
        }
        final long idSalvo = servico.finalizar();
        String resumo = getString(R.string.resumo_trilha,
                Formatador.distancia(servico.getDistancia()),
                Formatador.duracao(SystemClock.elapsedRealtime() - servico.getInicioElapsed()),
                Formatador.velocidade(servico.getVelocidadeMaxima()));
        desvincularServico();
        mostrarEstadoParado();
        iniciarPrevia();

        if (idSalvo > 0) {
            new AlertDialog.Builder(this)
                    .setTitle(R.string.trilha_salva)
                    .setMessage(resumo)
                    .setPositiveButton(R.string.ver_no_mapa, (d, w) -> {
                        Intent abrir = new Intent(this, VisualizarTrilhaActivity.class)
                                .putExtra(VisualizarTrilhaActivity.EXTRA_TRILHA_ID, idSalvo);
                        startActivity(abrir);
                    })
                    .setNegativeButton(R.string.fechar, null)
                    .show();
        } else {
            Toast.makeText(this, R.string.trilha_descartada, Toast.LENGTH_LONG).show();
        }
    }

    private void desvincularServico() {
        if (vinculado) {
            if (servico != null) {
                servico.setOuvinte(null);
            }
            unbindService(conexao);
            vinculado = false;
            servico = null;
        }
    }

    // =====================================================================
    // Callbacks do serviço (RastreamentoService.Ouvinte)
    // =====================================================================

    @Override
    public void onRastreamentoIniciado() {
        sincronizarComServico();
    }

    @Override
    public void onNovaPosicao(Location local, boolean pontoRegistrado) {
        if (pontoRegistrado && linhaPercurso != null) {
            percursoDesenhado.add(new GeoPoint(local.getLatitude(), local.getLongitude()));
            linhaPercurso.setPoints(percursoDesenhado);
        }
        tvStatus.setText(R.string.status_gravando);
        atualizarEstatisticas();
        atualizarPosicaoNoMapa(local);
    }

    @Override
    public void onGpsDesativado() {
        tvStatus.setText(R.string.status_gps_desligado);
        sugerirLigarGps();
    }

    /** Atualiza toda a tela com o estado atual da gravação do serviço. */
    private void sincronizarComServico() {
        if (servico == null || !RastreamentoService.isGravando()) {
            return;
        }
        btnIniciarParar.setText(R.string.finalizar);
        btnIniciarParar.setBackgroundResource(R.drawable.bg_botao_parar);
        btnIniciarParar.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_parar, 0, 0, 0);
        tvStatus.setText(R.string.status_gravando);

        cronometro.setBase(servico.getInicioElapsed());
        cronometro.start();
        atualizarEstatisticas();

        // Redesenha o percurso já registrado (ex.: após girar a tela)
        percursoDesenhado.clear();
        percursoDesenhado.addAll(servico.getPercurso());
        if (linhaPercurso != null) {
            linhaPercurso.setPoints(percursoDesenhado);
        }
        Location ultima = servico.getUltimaLeitura();
        if (ultima != null) {
            atualizarPosicaoNoMapa(ultima);
        }
    }

    private void atualizarEstatisticas() {
        if (servico == null) {
            return;
        }
        tvVelocidadeAtual.setText(Formatador.velocidadeNumero(servico.getVelocidadeAtual()));
        tvVelocidadeMaxima.setText(Formatador.velocidade(servico.getVelocidadeMaxima()));
        tvDistancia.setText(Formatador.distancia(servico.getDistancia()));
    }

    /** Estado da tela quando não há gravação em andamento. */
    private void mostrarEstadoParado() {
        btnIniciarParar.setText(R.string.iniciar_trilha);
        btnIniciarParar.setBackgroundResource(R.drawable.bg_botao_primario);
        btnIniciarParar.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_iniciar, 0, 0, 0);
        cronometro.stop();
        cronometro.setBase(SystemClock.elapsedRealtime());
        tvVelocidadeAtual.setText(Formatador.velocidadeNumero(0));
        tvVelocidadeMaxima.setText(Formatador.velocidade(0));
        tvDistancia.setText(Formatador.distancia(0));
    }
}
