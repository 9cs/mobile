package com.dmobile.trilhas.util;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Lê e grava as configurações do usuário (SharedPreferences).
 * Os valores são consultados por todas as telas que exibem mapa.
 */
public final class Preferencias {

    private static final String ARQUIVO = "configuracoes_trilhas";
    private static final String CHAVE_TIPO_MAPA = "tipo_mapa";
    private static final String CHAVE_NAVEGACAO = "navegacao";

    /** Tipo de mapa: vetorial (padrão). */
    public static final int MAPA_VETORIAL = 0;
    /** Tipo de mapa: imagem de satélite. */
    public static final int MAPA_SATELITE = 1;

    /** Navegação North Up: o mapa fica sempre orientado para o norte (padrão). */
    public static final int NAVEGACAO_NORTH_UP = 0;
    /** Navegação Course Up: o mapa gira conforme o rumo do deslocamento. */
    public static final int NAVEGACAO_COURSE_UP = 1;

    private Preferencias() {
    }

    private static SharedPreferences prefs(Context contexto) {
        return contexto.getApplicationContext().getSharedPreferences(ARQUIVO, Context.MODE_PRIVATE);
    }

    public static int getTipoMapa(Context contexto) {
        return prefs(contexto).getInt(CHAVE_TIPO_MAPA, MAPA_VETORIAL);
    }

    public static void setTipoMapa(Context contexto, int tipo) {
        prefs(contexto).edit().putInt(CHAVE_TIPO_MAPA, tipo).apply();
    }

    public static int getNavegacao(Context contexto) {
        return prefs(contexto).getInt(CHAVE_NAVEGACAO, NAVEGACAO_NORTH_UP);
    }

    public static void setNavegacao(Context contexto, int navegacao) {
        prefs(contexto).edit().putInt(CHAVE_NAVEGACAO, navegacao).apply();
    }

    public static boolean isCourseUp(Context contexto) {
        return getNavegacao(contexto) == NAVEGACAO_COURSE_UP;
    }
}
