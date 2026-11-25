package com.dmobile.trilhas.modelo;

/**
 * Uma posição do usuário ao longo do percurso (linha da tabela "ponto").
 */
public class Ponto {

    private final double latitude;
    private final double longitude;
    private final long dataHora;

    public Ponto(double latitude, double longitude, long dataHora) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.dataHora = dataHora;
    }

    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }
    public long getDataHora() { return dataHora; }
}
