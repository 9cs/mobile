package com.dmobile.trilhas.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Typeface;

import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;

import com.dmobile.trilhas.R;

/**
 * Desenha (com Canvas) os marcadores personalizados usados nos mapas,
 * no lugar dos marcadores padrão da biblioteca de mapas.
 */
public final class MarcadorFactory {

    private MarcadorFactory() {
    }

    /**
     * Marcador da posição do usuário: círculo com uma seta de navegação.
     * A seta aponta para cima (norte); a tela gira o marcador conforme o rumo.
     */
    public static Drawable criarMarcadorUsuario(Context contexto) {
        float d = contexto.getResources().getDisplayMetrics().density;
        // Tamanho pequeno para não esconder o círculo de acurácia desenhado em volta
        int tamanho = Math.round(28 * d);
        Bitmap bitmap = Bitmap.createBitmap(tamanho, tamanho, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        Paint tinta = new Paint(Paint.ANTI_ALIAS_FLAG);

        float centro = tamanho / 2f;
        float raio = centro - 1.5f * d;

        // Sombra suave
        tinta.setColor(0x40000000);
        canvas.drawCircle(centro, centro + d, raio, tinta);
        // Borda branca
        tinta.setColor(Color.WHITE);
        canvas.drawCircle(centro, centro, raio, tinta);
        // Disco colorido
        tinta.setColor(contexto.getResources().getColor(R.color.marcador_usuario));
        canvas.drawCircle(centro, centro, raio - 2.5f * d, tinta);

        // Seta (aponta para cima = norte; o mapa gira o marcador conforme o rumo)
        Path seta = new Path();
        seta.moveTo(centro, centro - raio * 0.62f);
        seta.lineTo(centro + raio * 0.46f, centro + raio * 0.48f);
        seta.lineTo(centro, centro + raio * 0.22f);
        seta.lineTo(centro - raio * 0.46f, centro + raio * 0.48f);
        seta.close();
        tinta.setColor(Color.WHITE);
        canvas.drawPath(seta, tinta);

        return new BitmapDrawable(contexto.getResources(), bitmap);
    }

    /**
     * Pino em forma de gota com uma letra no centro (ex.: "I" para início e
     * "F" para fim da trilha). A âncora deve ser (0.5, 1.0).
     */
    public static Drawable criarPino(Context contexto, int cor, String letra) {
        float d = contexto.getResources().getDisplayMetrics().density;
        int largura = Math.round(36 * d);
        int altura = Math.round(48 * d);
        Bitmap bitmap = Bitmap.createBitmap(largura, altura, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        Paint tinta = new Paint(Paint.ANTI_ALIAS_FLAG);

        float cx = largura / 2f;
        float raio = largura / 2f - 2 * d;
        float cy = raio + 2 * d;

        // Corpo da gota: círculo + triângulo apontando para baixo
        Path gota = new Path();
        gota.addCircle(cx, cy, raio, Path.Direction.CW);
        Path ponta = new Path();
        ponta.moveTo(cx - raio * 0.72f, cy + raio * 0.68f);
        ponta.lineTo(cx, altura - d);
        ponta.lineTo(cx + raio * 0.72f, cy + raio * 0.68f);
        ponta.close();

        tinta.setColor(Color.WHITE);
        canvas.drawPath(gota, tinta);
        canvas.drawPath(ponta, tinta);

        tinta.setColor(cor);
        canvas.drawCircle(cx, cy, raio - 2.5f * d, tinta);
        Path pontaInterna = new Path();
        pontaInterna.moveTo(cx - raio * 0.6f, cy + raio * 0.6f);
        pontaInterna.lineTo(cx, altura - 4 * d);
        pontaInterna.lineTo(cx + raio * 0.6f, cy + raio * 0.6f);
        pontaInterna.close();
        canvas.drawPath(pontaInterna, tinta);

        // Letra centralizada
        tinta.setColor(Color.WHITE);
        tinta.setTextAlign(Paint.Align.CENTER);
        tinta.setTypeface(Typeface.DEFAULT_BOLD);
        tinta.setTextSize(16 * d);
        Paint.FontMetrics fm = tinta.getFontMetrics();
        float baseTexto = cy - (fm.ascent + fm.descent) / 2f;
        canvas.drawText(letra, cx, baseTexto, tinta);

        return new BitmapDrawable(contexto.getResources(), bitmap);
    }
}
