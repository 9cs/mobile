package com.dmobile.trilhas.dados;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.location.Location;

import com.dmobile.trilhas.modelo.Ponto;
import com.dmobile.trilhas.modelo.Trilha;

import java.util.ArrayList;
import java.util.List;

import static com.dmobile.trilhas.dados.TrilhasDbHelper.*;

/**
 * Objeto de acesso a dados: concentra todas as operações SQL sobre as
 * tabelas "trilha" e "ponto".
 */
public class TrilhaDao {

    private final TrilhasDbHelper helper;

    public TrilhaDao(Context contexto) {
        helper = TrilhasDbHelper.getInstancia(contexto);
    }

    // =====================================================================
    // Inserção / atualização (usadas durante o registro da trilha)
    // =====================================================================

    /** Cria uma nova trilha e devolve o seu id. */
    public long inserirTrilha(String nome, long dataInicio) {
        ContentValues v = new ContentValues();
        v.put(T_NOME, nome);
        v.put(T_DATA_INICIO, dataInicio);
        v.put(T_DATA_FIM, dataInicio);
        return helper.getWritableDatabase().insert(TABELA_TRILHA, null, v);
    }

    /** Grava uma posição do percurso. */
    public void inserirPonto(long trilhaId, Location local) {
        ContentValues v = new ContentValues();
        v.put(P_TRILHA_ID, trilhaId);
        v.put(P_LATITUDE, local.getLatitude());
        v.put(P_LONGITUDE, local.getLongitude());
        if (local.hasAltitude()) v.put(P_ALTITUDE, local.getAltitude());
        if (local.hasAccuracy()) v.put(P_PRECISAO, local.getAccuracy());
        if (local.hasSpeed()) v.put(P_VELOCIDADE, local.getSpeed());
        v.put(P_DATA_HORA, local.getTime() > 0 ? local.getTime() : System.currentTimeMillis());
        helper.getWritableDatabase().insert(TABELA_PONTO, null, v);
    }

    /**
     * Atualiza as estatísticas da trilha. É chamada a cada ponto aceito (assim
     * os dados ficam salvos mesmo se o app for encerrado) e ao finalizar.
     */
    public void atualizarEstatisticas(long trilhaId, long dataFim, double distancia,
                                      double velMedia, double velMaxima) {
        ContentValues v = new ContentValues();
        v.put(T_DATA_FIM, dataFim);
        v.put(T_DISTANCIA, distancia);
        v.put(T_VEL_MEDIA, velMedia);
        v.put(T_VEL_MAXIMA, velMaxima);
        helper.getWritableDatabase().update(TABELA_TRILHA, v, T_ID + "=?",
                new String[]{String.valueOf(trilhaId)});
    }

    /** Altera o nome de uma trilha. */
    public boolean renomear(long trilhaId, String novoNome) {
        ContentValues v = new ContentValues();
        v.put(T_NOME, novoNome);
        return helper.getWritableDatabase().update(TABELA_TRILHA, v, T_ID + "=?",
                new String[]{String.valueOf(trilhaId)}) > 0;
    }

    // =====================================================================
    // Consultas
    // =====================================================================

    /** Lista todas as trilhas, da mais recente para a mais antiga. */
    public List<Trilha> listarTrilhas() {
        List<Trilha> lista = new ArrayList<>();
        Cursor c = helper.getReadableDatabase().query(TABELA_TRILHA, null, null, null,
                null, null, T_DATA_INICIO + " DESC");
        try {
            while (c.moveToNext()) {
                lista.add(lerTrilha(c));
            }
        } finally {
            c.close();
        }
        return lista;
    }

    /** Busca uma trilha pelo id (ou null se não existir). */
    public Trilha buscarTrilha(long trilhaId) {
        Cursor c = helper.getReadableDatabase().query(TABELA_TRILHA, null, T_ID + "=?",
                new String[]{String.valueOf(trilhaId)}, null, null, null);
        try {
            return c.moveToFirst() ? lerTrilha(c) : null;
        } finally {
            c.close();
        }
    }

    /** Devolve o percurso da trilha em ordem cronológica. */
    public List<Ponto> listarPontos(long trilhaId) {
        List<Ponto> pontos = new ArrayList<>();
        Cursor c = helper.getReadableDatabase().query(TABELA_PONTO,
                new String[]{P_LATITUDE, P_LONGITUDE, P_DATA_HORA},
                P_TRILHA_ID + "=?", new String[]{String.valueOf(trilhaId)},
                null, null, P_DATA_HORA + " ASC, " + P_ID + " ASC");
        try {
            while (c.moveToNext()) {
                pontos.add(new Ponto(c.getDouble(0), c.getDouble(1), c.getLong(2)));
            }
        } finally {
            c.close();
        }
        return pontos;
    }

    /** Quantidade total de trilhas registradas. */
    public long contarTrilhas() {
        return DatabaseUtils.queryNumEntries(helper.getReadableDatabase(), TABELA_TRILHA);
    }

    /** Soma das distâncias de todas as trilhas (metros). */
    public double somarDistancias() {
        Cursor c = helper.getReadableDatabase().rawQuery(
                "SELECT COALESCE(SUM(" + T_DISTANCIA + "), 0) FROM " + TABELA_TRILHA, null);
        try {
            return c.moveToFirst() ? c.getDouble(0) : 0;
        } finally {
            c.close();
        }
    }

    /** Quantas trilhas começaram dentro do intervalo [inicio, fim]. */
    public long contarNoIntervalo(long inicio, long fim) {
        return DatabaseUtils.queryNumEntries(helper.getReadableDatabase(), TABELA_TRILHA,
                T_DATA_INICIO + " BETWEEN ? AND ?",
                new String[]{String.valueOf(inicio), String.valueOf(fim)});
    }

    // =====================================================================
    // Exclusão (os pontos são apagados junto com a trilha)
    // =====================================================================

    /** Apaga uma trilha específica. */
    public int apagar(long trilhaId) {
        return apagarOnde(T_ID + "=?", new String[]{String.valueOf(trilhaId)});
    }

    /** Apaga as trilhas iniciadas no intervalo [inicio, fim]. */
    public int apagarIntervalo(long inicio, long fim) {
        return apagarOnde(T_DATA_INICIO + " BETWEEN ? AND ?",
                new String[]{String.valueOf(inicio), String.valueOf(fim)});
    }

    /** Apaga todas as trilhas. */
    public int apagarTodas() {
        return apagarOnde(null, null);
    }

    /**
     * Apaga as trilhas que atendem ao filtro e os seus pontos, em uma única
     * transação. A exclusão explícita dos pontos garante a consistência mesmo
     * que as chaves estrangeiras estejam desabilitadas no aparelho.
     */
    private int apagarOnde(String filtro, String[] argumentos) {
        SQLiteDatabase db = helper.getWritableDatabase();
        db.beginTransaction();
        try {
            String subconsulta = "SELECT " + T_ID + " FROM " + TABELA_TRILHA
                    + (filtro != null ? " WHERE " + filtro : "");
            db.delete(TABELA_PONTO, P_TRILHA_ID + " IN (" + subconsulta + ")", argumentos);
            int apagadas = db.delete(TABELA_TRILHA, filtro != null ? filtro : "1", argumentos);
            db.setTransactionSuccessful();
            return apagadas;
        } finally {
            db.endTransaction();
        }
    }

    /** Converte a linha atual do cursor em um objeto Trilha. */
    private Trilha lerTrilha(Cursor c) {
        Trilha t = new Trilha();
        t.setId(c.getLong(c.getColumnIndexOrThrow(T_ID)));
        t.setNome(c.getString(c.getColumnIndexOrThrow(T_NOME)));
        t.setDataInicio(c.getLong(c.getColumnIndexOrThrow(T_DATA_INICIO)));
        t.setDataFim(c.getLong(c.getColumnIndexOrThrow(T_DATA_FIM)));
        t.setDistancia(c.getDouble(c.getColumnIndexOrThrow(T_DISTANCIA)));
        t.setVelocidadeMedia(c.getDouble(c.getColumnIndexOrThrow(T_VEL_MEDIA)));
        t.setVelocidadeMaxima(c.getDouble(c.getColumnIndexOrThrow(T_VEL_MAXIMA)));
        return t;
    }
}
