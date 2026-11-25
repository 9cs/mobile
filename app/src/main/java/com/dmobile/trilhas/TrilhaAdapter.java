package com.dmobile.trilhas;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.dmobile.trilhas.modelo.Trilha;
import com.dmobile.trilhas.util.Formatador;

import java.util.ArrayList;
import java.util.List;

/**
 * Adapter da lista de trilhas (cada linha usa o layout item_trilha.xml).
 */
public class TrilhaAdapter extends BaseAdapter {

    private final LayoutInflater inflater;
    private final List<Trilha> trilhas = new ArrayList<>();

    public TrilhaAdapter(Context contexto) {
        inflater = LayoutInflater.from(contexto);
    }

    /** Substitui os itens exibidos. */
    public void setTrilhas(List<Trilha> novas) {
        trilhas.clear();
        trilhas.addAll(novas);
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return trilhas.size();
    }

    @Override
    public Trilha getItem(int posicao) {
        return trilhas.get(posicao);
    }

    @Override
    public long getItemId(int posicao) {
        return trilhas.get(posicao).getId();
    }

    @Override
    public View getView(int posicao, View reaproveitada, ViewGroup pai) {
        ViewHolder holder;
        if (reaproveitada == null) {
            reaproveitada = inflater.inflate(R.layout.item_trilha, pai, false);
            holder = new ViewHolder(reaproveitada);
            reaproveitada.setTag(holder);
        } else {
            holder = (ViewHolder) reaproveitada.getTag();
        }

        Trilha t = getItem(posicao);
        holder.nome.setText(t.getNome());
        holder.data.setText(Formatador.dataHoraCurta(t.getDataInicio()));
        holder.distancia.setText(Formatador.distancia(t.getDistancia()));
        holder.duracao.setText(Formatador.duracao(t.getDuracao()));
        holder.velocidade.setText(Formatador.velocidade(t.getVelocidadeMedia()));
        return reaproveitada;
    }

    /** Guarda as referências das views de uma linha (evita findViewById repetido). */
    private static class ViewHolder {
        final TextView nome;
        final TextView data;
        final TextView distancia;
        final TextView duracao;
        final TextView velocidade;

        ViewHolder(View v) {
            nome = v.findViewById(R.id.item_nome);
            data = v.findViewById(R.id.item_data);
            distancia = v.findViewById(R.id.item_distancia);
            duracao = v.findViewById(R.id.item_duracao);
            velocidade = v.findViewById(R.id.item_velocidade);
        }
    }
}
