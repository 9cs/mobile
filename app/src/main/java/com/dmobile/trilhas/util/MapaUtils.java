package com.dmobile.trilhas.util;

import android.content.Context;
import android.graphics.Color;

import org.osmdroid.tileprovider.tilesource.ITileSource;
import org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.MapTileIndex;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.CopyrightOverlay;
import org.osmdroid.views.overlay.Overlay;

/**
 * Configuração dos mapas (biblioteca osmdroid / OpenStreetMap) e aplicação
 * das preferências do usuário.
 */
public final class MapaUtils {

    /** Zoom mínimo (continente) e máximo (nível de rua) permitidos. */
    public static final double ZOOM_MINIMO = 3.0;
    public static final double ZOOM_MAXIMO = 19.0;

    /**
     * Mapa de satélite: imagens "World Imagery" da Esri.
     * O serviço usa o formato .../tile/{zoom}/{linha y}/{coluna x}, por isso
     * a URL de cada bloco é montada manualmente.
     */
    public static final ITileSource SATELITE = new OnlineTileSourceBase(
            "EsriWorldImagery", 0, 19, 256, "",
            new String[]{"https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/"},
            "Imagens © Esri, Maxar, Earthstar Geographics") {
        @Override
        public String getTileURLString(long indice) {
            return getBaseUrl() + MapTileIndex.getZoom(indice) + "/"
                    + MapTileIndex.getY(indice) + "/" + MapTileIndex.getX(indice);
        }
    };

    /** Mapa de ruas (vetorial no sentido de "mapa de ruas"): OpenStreetMap padrão. */
    public static final ITileSource RUAS = TileSourceFactory.MAPNIK;

    private MapaUtils() {
    }

    /**
     * Configurações comuns a todos os mapas do aplicativo: gestos de pinça,
     * texto legível em telas de alta densidade, limites de zoom e o crédito
     * obrigatório dos dados do mapa.
     *
     * @return a sobreposição de créditos, para que a tela possa posicioná-la
     */
    public static CopyrightOverlay configurarMapa(Context contexto, MapView mapa) {
        mapa.setMultiTouchControls(true);
        mapa.setTilesScaledToDpi(true);
        mapa.setMinZoomLevel(ZOOM_MINIMO);
        mapa.setMaxZoomLevel(ZOOM_MAXIMO);
        mapa.setHorizontalMapRepetitionEnabled(false);
        mapa.setVerticalMapRepetitionEnabled(false);

        CopyrightOverlay creditos = new CopyrightOverlay(contexto);
        creditos.setTextSize(10);
        mapa.getOverlays().add(creditos);
        aplicarTipoMapa(contexto, mapa);
        return creditos;
    }

    /** Vetorial (padrão) -> mapa de ruas do OpenStreetMap; Satélite -> Esri. */
    public static void aplicarTipoMapa(Context contexto, MapView mapa) {
        boolean satelite = Preferencias.getTipoMapa(contexto) == Preferencias.MAPA_SATELITE;
        ITileSource desejado = satelite ? SATELITE : RUAS;
        if (mapa.getTileProvider().getTileSource() != desejado) {
            mapa.setTileSource(desejado);
        }
        // Créditos legíveis: texto claro sobre a imagem de satélite, escuro sobre o mapa de ruas
        for (Overlay camada : mapa.getOverlays()) {
            if (camada instanceof CopyrightOverlay) {
                ((CopyrightOverlay) camada).setTextColor(satelite ? Color.WHITE : Color.DKGRAY);
            }
        }
    }
}
