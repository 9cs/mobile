package com.dmobile.trilhas.util;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.text.InputType;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.FrameLayout;

import com.dmobile.trilhas.R;

/**
 * Diálogos reutilizados por várias telas (pedir nome, confirmar ação).
 */
public final class Dialogos {

    /** Recebe o texto digitado pelo usuário. */
    public interface AoInformarTexto {
        void aoInformar(String texto);
    }

    private Dialogos() {
    }

    /**
     * Mostra um diálogo com um campo de texto. O botão de confirmação só
     * fecha o diálogo quando o texto não está vazio.
     */
    public static void pedirNome(final Activity atividade, int titulo, String valorInicial,
                                 int textoConfirmar, final AoInformarTexto callback) {
        final EditText campo = new EditText(atividade);
        campo.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        campo.setSingleLine(true);
        campo.setHint(R.string.dica_nome_trilha);
        if (valorInicial != null) {
            campo.setText(valorInicial);
            campo.setSelection(valorInicial.length());
        }

        // Margens laterais para o campo não encostar nas bordas do diálogo
        FrameLayout container = new FrameLayout(atividade);
        int margem = atividade.getResources().getDimensionPixelSize(R.dimen.espaco_grande);
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT);
        lp.leftMargin = margem;
        lp.rightMargin = margem;
        lp.topMargin = margem / 2;
        container.addView(campo, lp);

        final AlertDialog dialogo = new AlertDialog.Builder(atividade)
                .setTitle(titulo)
                .setView(container)
                .setPositiveButton(textoConfirmar, null) // listener definido abaixo
                .setNegativeButton(R.string.cancelar, null)
                .create();

        dialogo.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface d) {
                dialogo.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                    String texto = campo.getText().toString().trim();
                    if (texto.isEmpty()) {
                        campo.setError(atividade.getString(R.string.erro_nome_vazio));
                        return;
                    }
                    dialogo.dismiss();
                    callback.aoInformar(texto);
                });
            }
        });
        // Abre o teclado automaticamente
        if (dialogo.getWindow() != null) {
            dialogo.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        }
        dialogo.show();
    }

    /** Pede confirmação antes de executar uma ação (ex.: apagar). */
    public static void confirmar(Activity atividade, int titulo, String mensagem,
                                 int textoConfirmar, final Runnable acao) {
        new AlertDialog.Builder(atividade)
                .setTitle(titulo)
                .setMessage(mensagem)
                .setPositiveButton(textoConfirmar, (d, w) -> acao.run())
                .setNegativeButton(R.string.cancelar, null)
                .show();
    }
}
