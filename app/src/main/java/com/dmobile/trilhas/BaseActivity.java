package com.dmobile.trilhas;

import android.annotation.TargetApi;
import android.app.ActionBar;
import android.app.Activity;
import android.os.Build;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowInsets;

/**
 * Comportamentos comuns a todas as telas do aplicativo:
 * botão "voltar" na barra de ação e ajuste às barras do sistema.
 */
public abstract class BaseActivity extends Activity {

    /** Exibe a seta de voltar na ActionBar. */
    protected void habilitarVoltar() {
        ActionBar barra = getActionBar();
        if (barra != null) {
            barra.setDisplayHomeAsUpEnabled(true);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * A partir do Android 15 o conteúdo é desenhado atrás das barras do
     * sistema (edge-to-edge): a barra de título/status cobre o topo e a barra
     * de navegação cobre a base. Este método acrescenta ao padding da view o
     * espaço ocupado por essas barras (o inset superior já inclui a altura da
     * ActionBar), evitando que textos e botões fiquem escondidos.
     * Em versões anteriores os insets chegam zerados e nada muda.
     */
    protected void ajustarAsBarrasDoSistema(final View view) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT_WATCH) {
            return; // WindowInsets não existe antes da API 20
        }
        final int padEsq = view.getPaddingLeft();
        final int padDir = view.getPaddingRight();
        final int padBaixo = view.getPaddingBottom();
        final int padTopo = view.getPaddingTop();
        view.setOnApplyWindowInsetsListener((v, insets) -> {
            aplicarPadding(v, insets, padEsq, padTopo, padDir, padBaixo);
            return insets;
        });
        view.requestApplyInsets();
    }

    @TargetApi(Build.VERSION_CODES.KITKAT_WATCH)
    @SuppressWarnings("deprecation")
    private static void aplicarPadding(View v, WindowInsets insets, int esq, int topo, int dir, int baixo) {
        v.setPadding(esq + insets.getSystemWindowInsetLeft(),
                topo + insets.getSystemWindowInsetTop(),
                dir + insets.getSystemWindowInsetRight(),
                baixo + insets.getSystemWindowInsetBottom());
    }
}
