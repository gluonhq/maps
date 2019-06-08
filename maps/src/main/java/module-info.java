module com.gluonhq.maps {
    requires transitive javafx.controls;

    requires com.gluonhq.attach.storage;
    requires com.gluonhq.attach.util;

    requires java.logging;

    uses com.gluonhq.maps.tile.TileRetriever;

    exports com.gluonhq.maps;
    exports com.gluonhq.maps.tile;
}