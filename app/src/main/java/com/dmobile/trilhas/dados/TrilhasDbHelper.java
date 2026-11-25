package com.dmobile.trilhas.dados;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Cria e mantém o banco SQLite do aplicativo.
 *
 * Modelo de dados (duas tabelas, relação 1:N):
 *
 *   trilha (_id, nome, data_inicio, data_fim, distancia,
 *           velocidade_media, velocidade_maxima)
 *
 *   ponto  (_id, trilha_id -> trilha._id, latitude, longitude, altitude,
 *           precisao, velocidade, data_hora)
 */
public class TrilhasDbHelper extends SQLiteOpenHelper {

    private static final String NOME_BANCO = "trilhas.db";
    private static final int VERSAO_BANCO = 1;

    // ---------- Tabela "trilha" ----------
    public static final String TABELA_TRILHA = "trilha";
    public static final String T_ID = "_id";
    public static final String T_NOME = "nome";
    public static final String T_DATA_INICIO = "data_inicio";
    public static final String T_DATA_FIM = "data_fim";
    public static final String T_DISTANCIA = "distancia";
    public static final String T_VEL_MEDIA = "velocidade_media";
    public static final String T_VEL_MAXIMA = "velocidade_maxima";

    // ---------- Tabela "ponto" ----------
    public static final String TABELA_PONTO = "ponto";
    public static final String P_ID = "_id";
    public static final String P_TRILHA_ID = "trilha_id";
    public static final String P_LATITUDE = "latitude";
    public static final String P_LONGITUDE = "longitude";
    public static final String P_ALTITUDE = "altitude";
    public static final String P_PRECISAO = "precisao";
    public static final String P_VELOCIDADE = "velocidade";
    public static final String P_DATA_HORA = "data_hora";

    private static final String SQL_CRIAR_TRILHA =
            "CREATE TABLE " + TABELA_TRILHA + " ("
                    + T_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + T_NOME + " TEXT NOT NULL, "
                    + T_DATA_INICIO + " INTEGER NOT NULL, "
                    + T_DATA_FIM + " INTEGER, "
                    + T_DISTANCIA + " REAL NOT NULL DEFAULT 0, "
                    + T_VEL_MEDIA + " REAL NOT NULL DEFAULT 0, "
                    + T_VEL_MAXIMA + " REAL NOT NULL DEFAULT 0)";

    private static final String SQL_CRIAR_PONTO =
            "CREATE TABLE " + TABELA_PONTO + " ("
                    + P_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + P_TRILHA_ID + " INTEGER NOT NULL REFERENCES "
                    + TABELA_TRILHA + "(" + T_ID + ") ON DELETE CASCADE, "
                    + P_LATITUDE + " REAL NOT NULL, "
                    + P_LONGITUDE + " REAL NOT NULL, "
                    + P_ALTITUDE + " REAL, "
                    + P_PRECISAO + " REAL, "
                    + P_VELOCIDADE + " REAL, "
                    + P_DATA_HORA + " INTEGER NOT NULL)";

    private static final String SQL_INDICE_PONTO =
            "CREATE INDEX idx_ponto_trilha ON " + TABELA_PONTO + "(" + P_TRILHA_ID + ")";

    private static TrilhasDbHelper instancia;

    /** Instância única (singleton) compartilhada por atividades e serviço. */
    public static synchronized TrilhasDbHelper getInstancia(Context contexto) {
        if (instancia == null) {
            instancia = new TrilhasDbHelper(contexto.getApplicationContext());
        }
        return instancia;
    }

    private TrilhasDbHelper(Context contexto) {
        super(contexto, NOME_BANCO, null, VERSAO_BANCO);
    }

    @Override
    public void onConfigure(SQLiteDatabase db) {
        super.onConfigure(db);
        // Habilita as chaves estrangeiras (ON DELETE CASCADE)
        db.setForeignKeyConstraintsEnabled(true);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CRIAR_TRILHA);
        db.execSQL(SQL_CRIAR_PONTO);
        db.execSQL(SQL_INDICE_PONTO);
        // Primeira instalação: grava uma trilha de exemplo para demonstração
        TrilhaExemplo.inserir(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int versaoAntiga, int versaoNova) {
        // Primeira versão do banco: nada a migrar.
    }
}
