package com.dmobile.trilhas;

import android.app.Application;
import android.content.Context;

import org.osmdroid.config.Configuration;

/**
 * Classe Application: executada uma vez, antes de qualquer tela.
 * Configura a biblioteca de mapas osmdroid (OpenStreetMap).
 */
public class TrilhasApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        // Carrega as configurações do osmdroid (pastas de cache etc.)
        Configuration.getInstance().load(this,
                getSharedPreferences("osmdroid", Context.MODE_PRIVATE));
        // A política de uso dos servidores do OpenStreetMap exige que cada
        // aplicativo se identifique com um "User-Agent" próprio.
        Configuration.getInstance().setUserAgentValue(getPackageName() + "/1.0");
    }
}
