package com.dmobile.trilhas.util;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Funções de formatação de velocidade, distância, duração e datas
 * para exibição na interface (padrão brasileiro).
 */
public final class Formatador {

    private static final Locale BRASIL = new Locale("pt", "BR");

    private Formatador() {
    }

    /** Converte m/s para km/h com uma casa decimal. Ex.: "5,4 km/h". */
    public static String velocidade(double metrosPorSegundo) {
        return String.format(BRASIL, "%.1f km/h", metrosPorSegundo * 3.6);
    }

    /** Apenas o número da velocidade em km/h (sem a unidade). */
    public static String velocidadeNumero(double metrosPorSegundo) {
        return String.format(BRASIL, "%.1f", metrosPorSegundo * 3.6);
    }

    /** Distância em metros (abaixo de 1 km) ou quilômetros. */
    public static String distancia(double metros) {
        if (metros < 1000) {
            return String.format(BRASIL, "%.0f m", metros);
        }
        return String.format(BRASIL, "%.2f km", metros / 1000.0);
    }

    /** Duração no formato HH:MM:SS. */
    public static String duracao(long milissegundos) {
        long totalSegundos = Math.max(0, milissegundos / 1000);
        long horas = totalSegundos / 3600;
        long minutos = (totalSegundos % 3600) / 60;
        long segundos = totalSegundos % 60;
        return String.format(BRASIL, "%02d:%02d:%02d", horas, minutos, segundos);
    }

    /** Data e hora completas. Ex.: "20/11/2026 14:35:10". */
    public static String dataHora(long milissegundos) {
        return new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", BRASIL).format(new Date(milissegundos));
    }

    /** Data e hora sem segundos. Ex.: "20/11/2026 14:35". */
    public static String dataHoraCurta(long milissegundos) {
        return new SimpleDateFormat("dd/MM/yyyy HH:mm", BRASIL).format(new Date(milissegundos));
    }

    /** Somente a data. Ex.: "20/11/2026". */
    public static String data(long milissegundos) {
        return new SimpleDateFormat("dd/MM/yyyy", BRASIL).format(new Date(milissegundos));
    }

    /** Precisão do GPS. Ex.: "± 5 m". */
    public static String precisao(float metros) {
        return String.format(BRASIL, "± %.0f m", metros);
    }
}
