package com.dmobile.trilhas.modelo;

/**
 * Representa uma trilha registrada (linha da tabela "trilha").
 * Distâncias em metros, velocidades em metros por segundo e datas em
 * milissegundos desde 01/01/1970 (System.currentTimeMillis()).
 */
public class Trilha {

    private long id;
    private String nome;
    private long dataInicio;
    private long dataFim;
    private double distancia;
    private double velocidadeMedia;
    private double velocidadeMaxima;

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public long getDataInicio() { return dataInicio; }
    public void setDataInicio(long dataInicio) { this.dataInicio = dataInicio; }

    public long getDataFim() { return dataFim; }
    public void setDataFim(long dataFim) { this.dataFim = dataFim; }

    public double getDistancia() { return distancia; }
    public void setDistancia(double distancia) { this.distancia = distancia; }

    public double getVelocidadeMedia() { return velocidadeMedia; }
    public void setVelocidadeMedia(double velocidadeMedia) { this.velocidadeMedia = velocidadeMedia; }

    public double getVelocidadeMaxima() { return velocidadeMaxima; }
    public void setVelocidadeMaxima(double velocidadeMaxima) { this.velocidadeMaxima = velocidadeMaxima; }

    /** Duração da trilha em milissegundos (fim - início). */
    public long getDuracao() {
        return dataFim > dataInicio ? dataFim - dataInicio : 0;
    }
}
