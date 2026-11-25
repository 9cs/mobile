package com.dmobile.trilhas.servico;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Looper;
import android.os.SystemClock;

import com.dmobile.trilhas.R;
import com.dmobile.trilhas.RegistrarTrilhaActivity;
import com.dmobile.trilhas.dados.TrilhaDao;
import com.dmobile.trilhas.util.Formatador;
import org.osmdroid.util.GeoPoint;

import java.util.ArrayList;
import java.util.List;

/**
 * Serviço em primeiro plano responsável pelo registro da trilha.
 *
 * Recebe as posições do GPS (LocationManager), calcula distância e
 * velocidades, grava cada ponto no SQLite e avisa a tela de registro
 * (quando ela está visível) por meio da interface {@link Ouvinte}.
 * Por ser um serviço em primeiro plano, a gravação continua mesmo com a
 * tela desligada ou com o usuário em outro aplicativo.
 */
public class RastreamentoService extends Service implements LocationListener {

    /** Ação usada no Intent que inicia a gravação. */
    public static final String ACAO_INICIAR = "com.dmobile.trilhas.acao.INICIAR";
    /** Nome da trilha (extra do Intent de início). */
    public static final String EXTRA_NOME = "nome_trilha";

    private static final String CANAL_ID = "rastreamento_trilha";
    private static final int NOTIFICACAO_ID = 1001;

    /** Intervalo mínimo entre atualizações do GPS (ms). */
    private static final long INTERVALO_GPS = 1000;
    /** Leituras com precisão pior que este valor (m) não entram no percurso. */
    private static final float PRECISAO_MAXIMA = 40f;
    /** Deslocamento mínimo (m) para registrar um novo ponto (filtra o "ruído" parado). */
    private static final float DESLOCAMENTO_MINIMO = 3f;
    /** Velocidades acima deste valor (m/s ~ 250 km/h) são tratadas como erro de leitura. */
    private static final float VELOCIDADE_IMPOSSIVEL = 70f;

    /** Callbacks enviados para a tela de registro (sempre na thread principal). */
    public interface Ouvinte {
        /** A gravação começou (estatísticas zeradas e cronômetro disponível). */
        void onRastreamentoIniciado();

        /**
         * Nova leitura do GPS.
         *
         * @param local            posição recebida
         * @param pontoRegistrado  true se a posição foi gravada no percurso
         */
        void onNovaPosicao(Location local, boolean pontoRegistrado);

        /** O usuário desligou o GPS durante a gravação. */
        void onGpsDesativado();
    }

    /** Binder local: a atividade obtém a referência direta ao serviço. */
    public class LocalBinder extends Binder {
        public RastreamentoService getServico() {
            return RastreamentoService.this;
        }
    }

    /** Indica se existe uma gravação em andamento (consultado pelas telas). */
    private static volatile boolean gravando = false;

    private final IBinder binder = new LocalBinder();
    private final List<GeoPoint> percurso = new ArrayList<>();

    private LocationManager gerenciadorLocal;
    private NotificationManager gerenciadorNotificacao;
    private TrilhaDao dao;
    private Ouvinte ouvinte;

    // Estado da trilha em andamento
    private long trilhaId = -1;
    private String nomeTrilha;
    private long inicioMillis;          // relógio de parede (gravado no banco)
    private long inicioElapsed;         // relógio monotônico (usado pelo cronômetro)
    private double distancia;           // metros
    private double velocidadeAtual;     // m/s
    private double velocidadeMaxima;    // m/s
    private Location ultimoPontoAceito;
    private Location ultimaLeitura;
    private long ultimaNotificacao;

    /** Id da trilha em gravação (usado para impedir que ela seja apagada). */
    private static volatile long trilhaEmGravacao = -1;

    public static boolean isGravando() {
        return gravando;
    }

    /** Id da trilha que está sendo gravada, ou -1 se não houver gravação. */
    public static long getTrilhaEmGravacao() {
        return gravando ? trilhaEmGravacao : -1;
    }

    // =====================================================================
    // Ciclo de vida
    // =====================================================================

    @Override
    public void onCreate() {
        super.onCreate();
        gerenciadorLocal = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        gerenciadorNotificacao = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        dao = new TrilhaDao(this);
        criarCanalNotificacao();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && ACAO_INICIAR.equals(intent.getAction())) {
            iniciar(intent.getStringExtra(EXTRA_NOME));
        } else if (!gravando) {
            // Reinício sem dados (ex.: processo recriado pelo sistema): nada a fazer.
            stopSelf();
        }
        // Não recriar automaticamente: o estado em memória seria perdido.
        return START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public void onDestroy() {
        // Se o serviço for destruído sem finalizar, salva o que já foi registrado.
        if (gravando) {
            pararAtualizacoesGps();
            salvarEstatisticas();
            gravando = false;
        }
        super.onDestroy();
    }

    // =====================================================================
    // Controle da gravação
    // =====================================================================

    /** Cria a trilha no banco, entra em primeiro plano e liga o GPS. */
    private void iniciar(String nome) {
        if (gravando) {
            return; // já existe uma gravação em andamento
        }
        nomeTrilha = nome != null ? nome : getString(R.string.app_name);
        inicioMillis = System.currentTimeMillis();
        inicioElapsed = SystemClock.elapsedRealtime();
        distancia = 0;
        velocidadeAtual = 0;
        velocidadeMaxima = 0;
        ultimoPontoAceito = null;
        ultimaLeitura = null;
        percurso.clear();

        // O startForeground deve ser chamado logo após startForegroundService
        entrarEmPrimeiroPlano();

        trilhaId = dao.inserirTrilha(nomeTrilha, inicioMillis);
        try {
            gerenciadorLocal.requestLocationUpdates(LocationManager.GPS_PROVIDER,
                    INTERVALO_GPS, 0f, this, Looper.getMainLooper());
        } catch (SecurityException | IllegalArgumentException e) {
            // Sem permissão ou sem GPS: desfaz a trilha criada e encerra.
            dao.apagar(trilhaId);
            trilhaId = -1;
            sairDoPrimeiroPlano();
            stopSelf();
            return;
        }
        trilhaEmGravacao = trilhaId;
        gravando = true;
        if (ouvinte != null) {
            ouvinte.onRastreamentoIniciado();
        }
    }

    /**
     * Finaliza a gravação e grava as estatísticas definitivas.
     *
     * @return id da trilha salva, ou -1 se nenhuma posição foi registrada
     *         (nesse caso a trilha vazia é descartada).
     */
    public long finalizar() {
        if (!gravando) {
            return -1;
        }
        gravando = false;
        pararAtualizacoesGps();

        long idSalvo = trilhaId;
        if (percurso.isEmpty()) {
            dao.apagar(trilhaId);
            idSalvo = -1;
        } else {
            salvarEstatisticas();
        }
        trilhaId = -1;
        sairDoPrimeiroPlano();
        stopSelf();
        return idSalvo;
    }

    private void pararAtualizacoesGps() {
        try {
            gerenciadorLocal.removeUpdates(this);
        } catch (SecurityException ignorada) {
            // permissão revogada: não há o que remover
        }
    }

    private void salvarEstatisticas() {
        if (trilhaId > 0) {
            dao.atualizarEstatisticas(trilhaId, System.currentTimeMillis(), distancia,
                    getVelocidadeMedia(), velocidadeMaxima);
        }
    }

    // =====================================================================
    // Recebimento das posições (LocationListener)
    // =====================================================================

    @Override
    public void onLocationChanged(Location local) {
        if (!gravando) {
            return;
        }
        Location anterior = ultimaLeitura;
        ultimaLeitura = local;

        // Velocidade instantânea: usa a informada pelo GPS; se ela não existir
        // (ou vier zerada, como no emulador), calcula pelo deslocamento desde a
        // leitura anterior, desde que esse deslocamento supere o erro do GPS.
        if (local.hasSpeed() && local.getSpeed() > 0) {
            velocidadeAtual = local.getSpeed();
        } else {
            velocidadeAtual = calcularVelocidade(anterior, local);
        }
        if (velocidadeAtual > VELOCIDADE_IMPOSSIVEL) {
            velocidadeAtual = 0;
        }

        boolean registrado = false;
        boolean leituraConfiavel = !local.hasAccuracy() || local.getAccuracy() <= PRECISAO_MAXIMA;
        if (leituraConfiavel) {
            if (velocidadeAtual > velocidadeMaxima) {
                velocidadeMaxima = velocidadeAtual;
            }
            if (ultimoPontoAceito == null) {
                registrado = true; // primeiro ponto do percurso
            } else {
                float deslocamento = ultimoPontoAceito.distanceTo(local);
                // Exige deslocamento maior que parte do erro para não somar o "tremor" do GPS
                float limiar = Math.max(DESLOCAMENTO_MINIMO, local.getAccuracy() * 0.5f);
                if (deslocamento >= limiar) {
                    distancia += deslocamento;
                    registrado = true;
                }
            }
        }

        if (registrado) {
            ultimoPontoAceito = local;
            percurso.add(new GeoPoint(local.getLatitude(), local.getLongitude()));
            dao.inserirPonto(trilhaId, local);
            salvarEstatisticas();
        }

        atualizarNotificacao();
        if (ouvinte != null) {
            ouvinte.onNovaPosicao(local, registrado);
        }
    }

    /**
     * Velocidade (m/s) entre duas leituras: distância / tempo. Devolve 0 se o
     * deslocamento for menor que a precisão (provável "tremor" do GPS parado).
     */
    public static double calcularVelocidade(Location anterior, Location atual) {
        if (anterior == null || atual.getTime() <= anterior.getTime()) {
            return 0;
        }
        float distancia = anterior.distanceTo(atual);
        float erro = atual.hasAccuracy() ? atual.getAccuracy() : 0f;
        if (distancia <= erro) {
            return 0;
        }
        return distancia * 1000.0 / (atual.getTime() - anterior.getTime());
    }

    @Override
    public void onProviderDisabled(String provedor) {
        if (gravando && ouvinte != null) {
            ouvinte.onGpsDesativado();
        }
    }

    @Override
    public void onProviderEnabled(String provedor) {
        // nada a fazer: as atualizações voltam automaticamente
    }

    /** Obrigatório em APIs < 29 (método abstrato da interface). */
    @Override
    @SuppressWarnings("deprecation")
    public void onStatusChanged(String provedor, int status, Bundle extras) {
    }

    // =====================================================================
    // Notificação de primeiro plano
    // =====================================================================

    private void criarCanalNotificacao() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel canal = new NotificationChannel(CANAL_ID,
                    getString(R.string.canal_notificacao), NotificationManager.IMPORTANCE_LOW);
            canal.setDescription(getString(R.string.canal_notificacao_descricao));
            gerenciadorNotificacao.createNotificationChannel(canal);
        }
    }

    private void entrarEmPrimeiroPlano() {
        Notification notificacao = construirNotificacao();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICACAO_ID, notificacao, ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION);
        } else {
            startForeground(NOTIFICACAO_ID, notificacao);
        }
    }

    @SuppressWarnings("deprecation")
    private void sairDoPrimeiroPlano() {
        stopForeground(true);
    }

    /** Atualiza o texto da notificação no máximo a cada 5 segundos. */
    private void atualizarNotificacao() {
        long agora = SystemClock.elapsedRealtime();
        if (agora - ultimaNotificacao >= 5000) {
            ultimaNotificacao = agora;
            gerenciadorNotificacao.notify(NOTIFICACAO_ID, construirNotificacao());
        }
    }

    @SuppressWarnings("deprecation")
    private Notification construirNotificacao() {
        // Tocar na notificação abre a tela de registro
        Intent abrir = new Intent(this, RegistrarTrilhaActivity.class)
                .setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }
        PendingIntent pendente = PendingIntent.getActivity(this, 0, abrir, flags);

        Notification.Builder builder = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? new Notification.Builder(this, CANAL_ID)
                : new Notification.Builder(this);

        String texto = getString(R.string.notificacao_texto,
                Formatador.distancia(distancia), Formatador.velocidade(velocidadeAtual));

        return builder
                .setSmallIcon(R.drawable.ic_notificacao)
                .setContentTitle(getString(R.string.notificacao_titulo, nomeTrilha))
                .setContentText(texto)
                .setContentIntent(pendente)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setWhen(inicioMillis)
                .setUsesChronometer(true)
                .build();
    }

    // =====================================================================
    // Dados para a tela de registro
    // =====================================================================

    public void setOuvinte(Ouvinte ouvinte) {
        this.ouvinte = ouvinte;
    }

    public long getTrilhaId() { return trilhaId; }

    public String getNomeTrilha() { return nomeTrilha; }

    /** Base do cronômetro (SystemClock.elapsedRealtime() no início). */
    public long getInicioElapsed() { return inicioElapsed; }

    public double getDistancia() { return distancia; }

    public double getVelocidadeAtual() { return velocidadeAtual; }

    public double getVelocidadeMaxima() { return velocidadeMaxima; }

    /** Velocidade média = distância total / tempo transcorrido (m/s). */
    public double getVelocidadeMedia() {
        double segundos = (System.currentTimeMillis() - inicioMillis) / 1000.0;
        return segundos > 0 ? distancia / segundos : 0;
    }

    /** Última leitura do GPS (pode ser null). */
    public Location getUltimaLeitura() { return ultimaLeitura; }

    /** Cópia do percurso registrado até agora (para redesenhar a linha). */
    public List<GeoPoint> getPercurso() {
        return new ArrayList<>(percurso);
    }
}
