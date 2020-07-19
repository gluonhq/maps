package com.gluonhq.impl.maps.tile;

import com.gluonhq.maps.tile.TileRetriever;
import javafx.scene.image.Image;

public abstract class HttpTileRetriever implements TileRetriever {

    private static final String HTTP_AGENT_PREFIX = "Gluon Maps/2.0.0 ";

    public HttpTileRetriever() {

        // set correct http agent, if not available
        String agent = System.getProperty("http.agent");
        if ( agent == null ) {
            agent = String.format("(%s/%s/%s)", System.getProperty("os.name"), System.getProperty("os.version"), System.getProperty("os.arch"));
        }
        if (!agent.startsWith(HTTP_AGENT_PREFIX)) {
            System.setProperty("http.agent", HTTP_AGENT_PREFIX + agent);
        }

    }

    public abstract String buildImageUrlString(int zoom, long i, long j);

    @Override
    public Image loadTile(int zoom, long i, long j) {
        String urlString = buildImageUrlString(zoom, i, j);
        return new Image(urlString, true);
    }

}
