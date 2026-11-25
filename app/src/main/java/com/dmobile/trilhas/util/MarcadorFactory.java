package com.dmobile.trilhas.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;

import com.dmobile.trilhas.R;

/**
 * Desenha (com Canvas) os marcadores personalizados usados nos mapas,
 * no lugar dos marcadores padrão da biblioteca de mapas:
 *
 *  - posição do usuário: círculo verde com uma pessoa caminhando;
 *  - rumo do deslocamento: triângulo laranja que gira em volta do círculo;
 *  - início da trilha: bandeira verde;
 *  - fim da trilha: bandeira quadriculada de chegada.
 */
public final class MarcadorFactory {

    /** Cor do mastro e do contorno das bandeiras. */
    private static final int COR_MASTRO = 0xFF37474F;
    /** Cor da sombra projetada no chão. */
    private static final int COR_SOMBRA = 0x40000000;

    private MarcadorFactory() {
    }

    // =====================================================================
    // Posição do usuário
    // =====================================================================

    /**
     * Marcador da posição do usuário: círculo verde com borda branca e a
     * pessoa caminhando (ícone do aplicativo) no centro. Fica sempre em pé;
     * a direção do deslocamento é mostrada por {@link #criarIndicadorRumo}.
     * Tamanho pequeno (30dp) para não esconder o círculo de acurácia.
     */
    public static Drawable criarMarcadorUsuario(Context contexto) {
        float d = contexto.getResources().getDisplayMetrics().density;
        int tamanho = Math.round(30 * d);
        Bitmap bitmap = Bitmap.createBitmap(tamanho, tamanho, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        Paint tinta = new Paint(Paint.ANTI_ALIAS_FLAG);

        float centro = tamanho / 2f;
        float raio = centro - 1.5f * d;

        // Sombra suave
        tinta.setColor(COR_SOMBRA);
        canvas.drawCircle(centro, centro + d, raio, tinta);
        // Borda branca
        tinta.setColor(Color.WHITE);
        canvas.drawCircle(centro, centro, raio, tinta);
        // Disco verde (cor do aplicativo)
        tinta.setColor(contexto.getResources().getColor(R.color.marcador_usuario));
        canvas.drawCircle(centro, centro, raio - 2.5f * d, tinta);

        // Pessoa caminhando: o mesmo ícone vetorial do app (res/drawable/ic_notificacao.xml)
        @SuppressWarnings("deprecation")
        Drawable pessoa = contexto.getResources().getDrawable(R.drawable.ic_notificacao);
        int margem = Math.round(6.5f * d);
        pessoa.setBounds(margem, margem, tamanho - margem, tamanho - margem);
        pessoa.draw(canvas);

        return new BitmapDrawable(contexto.getResources(), bitmap);
    }

    /**
     * Indicador de rumo: imagem transparente com um triângulo laranja no topo,
     * do lado de fora do círculo do usuário. A tela gira esta imagem conforme
     * o rumo, de modo que o triângulo "orbita" o círculo apontando a direção
     * do deslocamento. Deve ser desenhado no mesmo ponto, com âncora no centro.
     */
    public static Drawable criarIndicadorRumo(Context contexto) {
        float d = contexto.getResources().getDisplayMetrics().density;
        int tamanho = Math.round(48 * d);
        Bitmap bitmap = Bitmap.createBitmap(tamanho, tamanho, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        Paint tinta = new Paint(Paint.ANTI_ALIAS_FLAG);

        float centro = tamanho / 2f;
        Path triangulo = new Path();
        triangulo.moveTo(centro, 1.5f * d);                 // ponta (para cima = norte)
        triangulo.lineTo(centro + 7.5f * d, 11.5f * d);
        triangulo.lineTo(centro - 7.5f * d, 11.5f * d);
        triangulo.close();

        // Preenchimento laranja com contorno branco (visível no mapa e no satélite)
        tinta.setStyle(Paint.Style.FILL);
        tinta.setColor(contexto.getResources().getColor(R.color.indicador_rumo));
        canvas.drawPath(triangulo, tinta);
        tinta.setStyle(Paint.Style.STROKE);
        tinta.setStrokeWidth(1.5f * d);
        tinta.setStrokeJoin(Paint.Join.ROUND);
        tinta.setColor(Color.WHITE);
        canvas.drawPath(triangulo, tinta);

        return new BitmapDrawable(contexto.getResources(), bitmap);
    }

    // =====================================================================
    // Início e fim da trilha
    // =====================================================================

    /** Bandeira verde que marca o início da trilha. Âncora: (0.5, 1.0). */
    public static Drawable criarBandeiraInicio(Context contexto) {
        return criarBandeira(contexto, false);
    }

    /** Bandeira quadriculada de chegada que marca o fim da trilha. Âncora: (0.5, 1.0). */
    public static Drawable criarBandeiraChegada(Context contexto) {
        return criarBandeira(contexto, true);
    }

    /**
     * Desenha uma bandeira presa a um mastro. A base do mastro fica no centro
     * da borda inferior da imagem, que deve ser a âncora do marcador para que
     * a bandeira fique "fincada" exatamente sobre a coordenada.
     *
     * @param quadriculada true para a bandeira de chegada (xadrez preto e branco)
     */
    private static Drawable criarBandeira(Context contexto, boolean quadriculada) {
        float d = contexto.getResources().getDisplayMetrics().density;
        int largura = Math.round(40 * d);
        int altura = Math.round(44 * d);
        Bitmap bitmap = Bitmap.createBitmap(largura, altura, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        Paint tinta = new Paint(Paint.ANTI_ALIAS_FLAG);

        float base = altura - 2 * d;          // ponto em que o mastro toca o chão
        float mastroX = largura / 2f;         // mastro no centro (âncora 0.5)

        // Sombra no chão
        tinta.setColor(COR_SOMBRA);
        canvas.drawOval(new RectF(mastroX - 6 * d, base - 2 * d, mastroX + 6 * d, base + 2 * d), tinta);

        // Pano da bandeira: à direita do mastro, com leve ondulação
        float topo = 3 * d;
        float esquerda = mastroX + 1 * d;
        float direita = largura - 2 * d;
        float alturaPano = 15 * d;
        Path pano = new Path();
        pano.moveTo(esquerda, topo);
        pano.cubicTo(esquerda + 6 * d, topo - 2.5f * d, direita - 6 * d, topo + 2.5f * d, direita, topo);
        pano.lineTo(direita, topo + alturaPano);
        pano.cubicTo(direita - 6 * d, topo + alturaPano + 2.5f * d, esquerda + 6 * d,
                topo + alturaPano - 2.5f * d, esquerda, topo + alturaPano);
        pano.close();

        if (quadriculada) {
            // Fundo branco + quadrados pretos alternados, recortados no formato do pano
            tinta.setStyle(Paint.Style.FILL);
            tinta.setColor(Color.WHITE);
            canvas.drawPath(pano, tinta);
            canvas.save();
            canvas.clipPath(pano);
            int colunas = 4;
            int linhas = 3;
            float lado = (direita - esquerda) / colunas;
            float ladoV = (alturaPano + 5 * d) / linhas;
            tinta.setColor(Color.BLACK);
            for (int l = 0; l < linhas; l++) {
                for (int col = 0; col < colunas; col++) {
                    if ((l + col) % 2 == 0) {
                        float x = esquerda + col * lado;
                        float y = topo - 2.5f * d + l * ladoV;
                        canvas.drawRect(x, y, x + lado, y + ladoV, tinta);
                    }
                }
            }
            canvas.restore();
        } else {
            tinta.setStyle(Paint.Style.FILL);
            tinta.setColor(contexto.getResources().getColor(R.color.bandeira_inicio));
            canvas.drawPath(pano, tinta);
        }
        // Contorno do pano
        tinta.setStyle(Paint.Style.STROKE);
        tinta.setStrokeWidth(1.2f * d);
        tinta.setColor(COR_MASTRO);
        canvas.drawPath(pano, tinta);

        // Mastro com uma bolinha no topo
        tinta.setStyle(Paint.Style.FILL);
        tinta.setColor(COR_MASTRO);
        canvas.drawRoundRect(new RectF(mastroX - 1.5f * d, topo - 1 * d, mastroX + 1.5f * d, base),
                1.5f * d, 1.5f * d, tinta);
        canvas.drawCircle(mastroX, topo - 1 * d, 2.5f * d, tinta);

        return new BitmapDrawable(contexto.getResources(), bitmap);
    }
}
