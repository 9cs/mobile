package com.dmobile.trilhas.dados;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import android.location.Location;

import java.util.Calendar;

import static com.dmobile.trilhas.dados.TrilhasDbHelper.*;

/**
 * Trilha de exemplo gravada no banco na primeira vez que o aplicativo é
 * instalado (chamada por {@link TrilhasDbHelper#onCreate}). Assim a lista de
 * trilhas já abre com um percurso para demonstração.
 *
 * O percurso segue o caminho do Parque Metropolitano de Pituaçu (Salvador-BA)
 * no OpenStreetMap: 131 pontos, um a cada ~20 m, cerca de 2,6 km. Os horários
 * simulam uma caminhada entre 4 e 5,5 km/h; distância e velocidades são
 * calculadas a partir dos próprios pontos, como numa gravação real.
 */
final class TrilhaExemplo {

    static final String NOME = "Trilha de exemplo - Parque de Pituaçu";

    /** Precisão (m) atribuída a cada ponto, típica de um GPS ao ar livre. */
    private static final float PRECISAO = 5f;

    /** Pontos do percurso: {latitude, longitude}. */
    private static final double[][] PONTOS = {
            {-12.966667, -38.414428}, {-12.966571, -38.414584}, {-12.966476, -38.414741},
            {-12.966412, -38.414913}, {-12.966348, -38.415085}, {-12.966303, -38.415263},
            {-12.966282, -38.415446}, {-12.966307, -38.415628}, {-12.966377, -38.415797},
            {-12.966459, -38.415961}, {-12.966520, -38.416134}, {-12.966575, -38.416310},
            {-12.966631, -38.416485}, {-12.966636, -38.416669}, {-12.966636, -38.416854},
            {-12.966677, -38.417031}, {-12.966734, -38.417207}, {-12.966791, -38.417382},
            {-12.966848, -38.417557}, {-12.966905, -38.417732}, {-12.966962, -38.417907},
            {-12.966934, -38.418083}, {-12.966779, -38.418171}, {-12.966616, -38.418241},
            {-12.966437, -38.418228}, {-12.966308, -38.418133}, {-12.966217, -38.417973},
            {-12.966127, -38.417814}, {-12.966036, -38.417654}, {-12.965945, -38.417495},
            {-12.965855, -38.417335}, {-12.965764, -38.417176}, {-12.965794, -38.416994},
            {-12.965825, -38.416813}, {-12.965855, -38.416631}, {-12.965778, -38.416484},
            {-12.965621, -38.416422}, {-12.965442, -38.416415}, {-12.965283, -38.416481},
            {-12.965158, -38.416610}, {-12.965074, -38.416772}, {-12.965000, -38.416941},
            {-12.964926, -38.417109}, {-12.964852, -38.417277}, {-12.964778, -38.417445},
            {-12.964704, -38.417614}, {-12.964630, -38.417782}, {-12.964556, -38.417950},
            {-12.964488, -38.418120}, {-12.964451, -38.418301}, {-12.964415, -38.418482},
            {-12.964388, -38.418662}, {-12.964431, -38.418841}, {-12.964474, -38.419021},
            {-12.964542, -38.419186}, {-12.964666, -38.419320}, {-12.964791, -38.419452},
            {-12.964928, -38.419572}, {-12.965065, -38.419692}, {-12.965201, -38.419812},
            {-12.965338, -38.419932}, {-12.965439, -38.420083}, {-12.965490, -38.420252},
            {-12.965479, -38.420436}, {-12.965364, -38.420558}, {-12.965310, -38.420734},
            {-12.965267, -38.420910}, {-12.965334, -38.421081}, {-12.965327, -38.421262},
            {-12.965305, -38.421445}, {-12.965251, -38.421614}, {-12.965117, -38.421734},
            {-12.964948, -38.421774}, {-12.964768, -38.421780}, {-12.964606, -38.421854},
            {-12.964476, -38.421979}, {-12.964316, -38.422063}, {-12.964181, -38.422185},
            {-12.964048, -38.422309}, {-12.963915, -38.422433}, {-12.963782, -38.422558},
            {-12.963649, -38.422682}, {-12.963516, -38.422806}, {-12.963383, -38.422930},
            {-12.963250, -38.423054}, {-12.963116, -38.423178}, {-12.963017, -38.423331},
            {-12.962929, -38.423492}, {-12.962862, -38.423663}, {-12.962795, -38.423834},
            {-12.962728, -38.424005}, {-12.962713, -38.424189}, {-12.962701, -38.424373},
            {-12.962689, -38.424557}, {-12.962677, -38.424742}, {-12.962665, -38.424926},
            {-12.962621, -38.425098}, {-12.962518, -38.425250}, {-12.962415, -38.425401},
            {-12.962261, -38.425496}, {-12.962096, -38.425568}, {-12.961925, -38.425623},
            {-12.961750, -38.425666}, {-12.961575, -38.425709}, {-12.961400, -38.425752},
            {-12.961225, -38.425795}, {-12.961050, -38.425838}, {-12.960875, -38.425881},
            {-12.960700, -38.425924}, {-12.960525, -38.425967}, {-12.960354, -38.426021},
            {-12.960189, -38.426095}, {-12.960082, -38.426232}, {-12.959999, -38.426396},
            {-12.959899, -38.426545}, {-12.959751, -38.426640}, {-12.959571, -38.426638},
            {-12.959392, -38.426621}, {-12.959214, -38.426596}, {-12.959049, -38.426523},
            {-12.958885, -38.426448}, {-12.958764, -38.426312}, {-12.958649, -38.426172},
            {-12.958636, -38.425988}, {-12.958622, -38.425804}, {-12.958473, -38.425846},
            {-12.958306, -38.425917}, {-12.958140, -38.425987}, {-12.957973, -38.426057},
            {-12.957807, -38.426127}, {-12.957639, -38.426154}
    };

    private TrilhaExemplo() {
    }

    /** Insere a trilha e seus pontos. O banco já está dentro de uma transação (onCreate). */
    static void inserir(SQLiteDatabase db) {
        // Início: domingo, 22/11/2026, às 07:30
        Calendar quando = Calendar.getInstance();
        quando.set(2026, Calendar.NOVEMBER, 22, 7, 30, 0);
        quando.set(Calendar.MILLISECOND, 0);
        long inicio = quando.getTimeInMillis();

        ContentValues trilha = new ContentValues();
        trilha.put(T_NOME, NOME);
        trilha.put(T_DATA_INICIO, inicio);
        long trilhaId = db.insert(TABELA_TRILHA, null, trilha);

        double distancia = 0;
        double velocidadeMaxima = 0;
        long instante = inicio;
        float[] resultado = new float[1];
        for (int i = 0; i < PONTOS.length; i++) {
            double velocidade = 0;
            if (i > 0) {
                Location.distanceBetween(PONTOS[i - 1][0], PONTOS[i - 1][1],
                        PONTOS[i][0], PONTOS[i][1], resultado);
                // Ritmo de caminhada variando suavemente entre ~1,1 e ~1,5 m/s (4 a 5,5 km/h)
                velocidade = 1.3 + 0.2 * Math.sin(i / 9.0);
                distancia += resultado[0];
                instante += Math.round(resultado[0] / velocidade * 1000);
                velocidadeMaxima = Math.max(velocidadeMaxima, velocidade);
            }
            ContentValues ponto = new ContentValues();
            ponto.put(P_TRILHA_ID, trilhaId);
            ponto.put(P_LATITUDE, PONTOS[i][0]);
            ponto.put(P_LONGITUDE, PONTOS[i][1]);
            ponto.put(P_PRECISAO, PRECISAO);
            ponto.put(P_VELOCIDADE, velocidade);
            ponto.put(P_DATA_HORA, instante);
            db.insert(TABELA_PONTO, null, ponto);
        }

        // Estatísticas finais, calculadas como numa gravação real
        double segundos = (instante - inicio) / 1000.0;
        ContentValues estatisticas = new ContentValues();
        estatisticas.put(T_DATA_FIM, instante);
        estatisticas.put(T_DISTANCIA, distancia);
        estatisticas.put(T_VEL_MEDIA, segundos > 0 ? distancia / segundos : 0);
        estatisticas.put(T_VEL_MAXIMA, velocidadeMaxima);
        db.update(TABELA_TRILHA, estatisticas, T_ID + "=?", new String[]{String.valueOf(trilhaId)});
    }
}
