/*
 * Copyright (c) 2016, Gluon
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL GLUON BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.gluonhq.impl.maps;

import com.gluonhq.maps.MapPoint;
import com.gluonhq.maps.MapView;
import javafx.application.Platform;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.value.ChangeListener;
import javafx.collections.ObservableList;
import javafx.geometry.Point2D;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.shape.Rectangle;

import java.lang.ref.SoftReference;
import java.util.*;
import java.util.logging.Logger;

import static java.lang.Math.ceil;
import static java.lang.Math.floor;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.beans.property.ReadOnlyDoubleWrapper;

/**
 *
 * The BaseMap provides the underlying maptiles.
 * On top of this, additional layers can be rendered.
 */
public class BaseMap extends Group {

    private static final Logger logger = Logger.getLogger( BaseMap.class.getName() );

    /**
     * When the zoom-factor is less than TIPPING below an integer, we will use
     * the higher-level zoom and scale down.
     */
    public static final double TIPPING = 0.2;

    /**
     * The maximum zoom level this map supports.
     */
    public static final int MAX_ZOOM = 20;
    private final Map<Long, SoftReference<MapTile>>[] tiles = new HashMap[MAX_ZOOM];

    private double lat;
    private double lon;
    private boolean abortedTileLoad;

//    static final boolean DEBUG = false;
    private final Rectangle area;
    private final ReadOnlyDoubleWrapper centerLon = new ReadOnlyDoubleWrapper();
    private final ReadOnlyDoubleWrapper centerLat = new ReadOnlyDoubleWrapper();
    private final ReadOnlyDoubleWrapper zoom = new ReadOnlyDoubleWrapper();
    
    private final DoubleProperty prefCenterLon = new SimpleDoubleProperty();
    private final DoubleProperty prefCenterLat = new SimpleDoubleProperty();
    private final DoubleProperty prefZoom = new SimpleDoubleProperty();
    
    private double zoomValue;



    public double x0, y0;
    private boolean dirty = true;

    private final ChangeListener<Number> resizeListener = (o, oldValue, newValue) -> markDirty();
    private ChangeListener<Scene> sceneListener;   

    public BaseMap() {
        for (int i = 0; i < tiles.length; i++) {
            tiles[i] = new HashMap<>();
        }
        area = new Rectangle(-10, -10, 810, 610);
        area.setVisible(false);
       
        prefCenterLat.addListener(o -> doSetCenter(prefCenterLat.get(), prefCenterLon.get()));
        prefCenterLon.addListener(o -> doSetCenter(prefCenterLat.get(), prefCenterLon.get()));
        prefZoom.addListener(o -> doZoom(prefZoom.get()));

        area.widthProperty().addListener(resizeListener);
        area.heightProperty().addListener(resizeListener);
        area.translateXProperty().bind(translateXProperty().multiply(-1));
        area.translateYProperty().bind(translateYProperty().multiply(-1));

        if (sceneListener == null) {
            sceneListener = (o, oldScene, newScene) -> {
                    if (newScene != null) {
                        //TODO Do we need to unbind from previous scene?
                        getParent().layoutBoundsProperty().addListener(e -> {
                            area.setWidth(getParent().getLayoutBounds().getWidth());
                            area.setHeight(getParent().getLayoutBounds().getHeight());
                        });
                        markDirty();
                    }
                    if (abortedTileLoad) {
                        abortedTileLoad = false;
                        doSetCenter(lat, lon);
                    }
            };
        }
        this.sceneProperty().addListener(sceneListener);
    }
    
    /**
     * Move the center of this map to the specified coordinates
     * @param lat the latitude of the new center
     * @param lon the longitude of the new center
     */
    public void setCenter(double lat, double lon) {
        prefCenterLat.set(lat);
        prefCenterLon.set(lon);
    }

    public void setCenter(Point2D center) {
        prefCenterLat.set(center.getX());
        prefCenterLon.set(center.getY());
    }

    public Point2D getCenter() {
        return new Point2D(prefCenterLat.get(), prefCenterLon.get());
    }

    private void doSetCenter(double lat, double lon) {
        this.lat = lat;
        this.lon = lon;
        if (getScene() == null) {
            abortedTileLoad = true;
            return;
        }
        double activeZoom = zoom.get();
        double n = Math.pow(2, activeZoom);
        double lat_rad = Math.PI * lat / 180;
        double id = n / 360. * (180 + lon);
        double jd = n * (1 - (Math.log(Math.tan(lat_rad) + 1 / Math.cos(lat_rad)) / Math.PI)) / 2;
        double mex = id * 256;
        double mey = jd * 256;
        double ttx = mex - this.getMyWidth() / 2;
        double tty = mey - this.getMyHeight() / 2;
        setTranslateX(-1 * ttx);
        setTranslateY(-1 * tty);
        logger.config("setCenter, tx = " + this.getTranslateX() + ", with = " + this.getMyWidth() / 2 + ", mex = " + mex);

        markDirty();
    }

    /**
     * Move the center of the map horizontally by a number of pixels. After this
     * operation, it will be checked if new tiles need to be downloaded
     *
     * @param dx the number of pixels
     */
    public void moveX(double dx) {
        setTranslateX(getTranslateX() - dx);
        markDirty();
    }

    /**
     * Move the center of the map vertically by a number of pixels. After this
     * operation, it will be checked if new tiles need to be downloaded
     *
     * @param dy the number of pixels
     */
    public void moveY(double dy) {
        double z = zoom.get();
        double maxty = 256 * Math.pow(2, z) - getMyHeight();
        logger.config("ty = " + getTranslateY() + " and dy = " + dy);
        if (getTranslateY() <= 0) {
            if (getTranslateY() + maxty >= 0) {
                setTranslateY(Math.min(0, getTranslateY() - dy));
            } else {
                setTranslateY(-maxty + 1);
            }
        } else {
            setTranslateY(0);
        }
        markDirty();
    }

    /**
     * set the zoom level of the map to the specified value
     * @param z the new zoom level
     */
    public void setZoom(double z) {
        logger.fine("setZoom called");
        prefZoom.set(z);
        
    }

    /**
     * Returns the preferred zoom level of this map.
     * @return the zoom level
     */
    public double getZoom() {
        return prefZoom.get();
    }

    private void doZoom(double z) {
        zoom.set(z);
        doSetCenter(this.lat, this.lon);
        markDirty();
    }

    /**
     * Only called internally when a zoom around a specific point is requested.
     * @param delta
     * @param pivotX
     * @param pivotY 
     */
    public void zoom(double delta, double pivotX, double pivotY) {
        double dz = delta;// > 0 ? .1 : -.1;
        double zp = zoom.get();
        logger.fine("Zoom called, zp = " + zp + ", delta = " + delta + ", px = " + pivotX + ", py = " + pivotY);
        double txold = getTranslateX();
        double t1x = pivotX - getTranslateX();
        double t2x = 1. - Math.pow(2, dz);
        double totX = t1x * t2x;
        double tyold = getTranslateY();
        double t1y = pivotY - tyold;
        double t2y = 1. - Math.pow(2, dz);
        double totY = t1y * t2y;
        logger.fine("zp = " + zp + ", txold = " + txold + ", totx = " + totX + ", tyold = " + tyold + ", toty = " + totY);
        if ((delta > 0)) {
            if (zp < MAX_ZOOM) {
                setTranslateX(txold + totX);
                setTranslateY(tyold + totY);
                zoom.set(zp + delta);
                markDirty();
            }
        } else if (zp > 1) {
            double nz = zp + delta;
            if (Math.pow(2, nz) * 256 > getMyHeight()) {
                // also, we need to fit on the current screen
                setTranslateX(txold + totX);
                setTranslateY(tyold + totY);
                zoom.set(zp + delta);
                markDirty();
            } else {
                logger.warning("sorry, would be too small");
            }
        }
        logger.fine("after, zp = " + zoom.get() + ", tx = " + getTranslateX());
    }



    public MapPoint getMapPosition(double sceneX, double sceneY) {
        final SimpleDoubleProperty _lat = new SimpleDoubleProperty();
        final SimpleDoubleProperty _lon = new SimpleDoubleProperty();
        calculateCoords(sceneX - getTranslateX(), sceneY - getTranslateY(), _lat, _lon);  
        return new MapPoint(_lat.get(), _lon.get());
    }
    
    public Point2D getMapPoint(double lat, double lon) {
        return getMapPoint(zoom.get(), lat, lon);
    }

    private Point2D getMapPoint(double zoom, double lat, double lon) {
        if (this.getScene() == null) {
            return null;
        }
        double n = Math.pow(2, zoom);
        double lat_rad = Math.PI * lat / 180;
        double id = n / 360. * (180 + lon);
        double jd = n * (1 - (Math.log(Math.tan(lat_rad) + 1 / Math.cos(lat_rad)) / Math.PI)) / 2;
        double mex = id * 256;
        double mey = jd * 256;
        double ttx = mex - this.getMyWidth() / 2;
        double tty = mey - this.getMyHeight() / 2;
        double x = this.getTranslateX() + mex;
        double y = this.getTranslateY() + mey;
        Point2D answer = new Point2D(x, y);
        return answer;
    }

    public ReadOnlyDoubleProperty centerLon() {
        return centerLon.getReadOnlyProperty();
    }

    public ReadOnlyDoubleProperty centerLat() {
        return centerLat.getReadOnlyProperty();
    }
    
    public ReadOnlyDoubleProperty zoom() {
        return zoom.getReadOnlyProperty();
    }
    
    public DoubleProperty prefCenterLon() {
        return prefCenterLon;
    }
    
    public DoubleProperty prefCenterLat() {
        return prefCenterLat;
    }
    
    private final void loadTiles() {
        logger.fine("[JVDBG] loadTiles");
        if (getScene() == null) {
            logger.fine("[JVDBG] can't load tiles, scene null");
            return;
        }
        int nearestZoom = (Math.min((int) floor(zoom.get() + TIPPING), MAX_ZOOM - 1));
        double activeZoom = zoom.get();
        double deltaZ = nearestZoom - activeZoom;
        long i_max = 1 << nearestZoom;
        long j_max = 1 << nearestZoom;
        double tx = getTranslateX();
        double ty = getTranslateY();
        double width = getMyWidth();
        double height = getMyHeight();
        long imin = Math.max(0, (long) (-tx * Math.pow(2, deltaZ) / 256) - 1);
        long jmin = Math.max(0, (long) (-ty * Math.pow(2, deltaZ) / 256));
        long imax = Math.min(i_max, imin + (long) (width * Math.pow(2, deltaZ) / 256) + 3);
        long jmax = Math.min(j_max, jmin + (long) (height * Math.pow(2, deltaZ) / 256) + 3);
        logger.fine("Zoom = " + nearestZoom + ", active = " + activeZoom + ", tx = " + tx + ", loadtiles, check i-range: " + imin + ", " + imax + " and j-range: " + jmin + ", " + jmax);
        for (long i = imin; i < imax; i++) {
            for (long j = jmin; j < jmax; j++) {
                Long key = i * i_max + j;
                SoftReference<MapTile> ref = tiles[nearestZoom].get(key);
                if ((ref == null) || (ref.get() == null)) {
                    if (ref != null) {
                        logger.fine("RECLAIMED: z=" + nearestZoom + ",i=" + i + ",j=" + j);
                    }
                    MapTile tile = new MapTile(this, nearestZoom, i, j);
                    tiles[nearestZoom].put(key, new SoftReference<>(tile));
                    MapTile covering = getCoveringTile(tile);
                    if (covering != null) {
                        covering.addCovering(tile);
                        if (!getChildren().contains(covering)) {
                            getChildren().add(covering);
                        }
                    }

                    getChildren().add(tile);
                } else {
                    MapTile tile = ref.get();
                    if (!getChildren().contains(tile)) {
                        getChildren().add(tile);
                    }
                }
            }
        }
        //   calculateCenterCoords();
        cleanupTiles();
    }

    /**
     * Find the "nearest" lower-zoom tile that covers a specific tile. This is
     * used to find out what tile we have to show while a new tile is still
     * loading
     *
     * @param zoom
     * @param i
     * @param j
     * @return the lower-zoom tile which covers the specified tile
     */
    protected MapTile findCovering(int zoom, long i, long j) {
        while (zoom > 0) {
            zoom--;
            i = i / 2;
            j = j / 2;
            MapTile candidate = findTile(zoom, i, j);
            if ((candidate != null) && (!candidate.loading())) {
                return candidate;
            }
        }
        return null;
    }

    /**
     * Return a specific tile
     *
     * @param zoom the zoomlevel
     * @param i the x-index
     * @param j the y-index
     * @return the tile, only if it is still in the cache
     */
    private MapTile findTile(int zoom, long i, long j) {
        Long key = i * (1 << zoom) + j;
        SoftReference<MapTile> exists = tiles[zoom].get(key);
        return (exists == null) ? null : exists.get();
    }

    private void cleanupTiles() {
        logger.fine("START CLEANUP, zp = " + zoom.get());
        double zp = zoom.get();
        List<MapTile> toRemove = new LinkedList<>();
        Parent parent = this.getParent();
        ObservableList<Node> children = this.getChildren();
        for (Node child : children) {
            if (child instanceof MapTile) {
                MapTile tile = (MapTile) child;
                boolean intersects = tile.getBoundsInParent().intersects(area.getBoundsInParent());
                logger.fine("evaluate tile " + tile + ", is = " + intersects + ", tzoom = " + tile.getZoomLevel());
                if (!intersects) {
                    logger.fine("not shown");
                    boolean loading = tile.loading();
                    logger.fine("Reap " + tile + " loading? " + loading);
                    if (!loading) {
                        toRemove.add(tile);
                    }
                } else if (tile.getZoomLevel() > ceil(zp)) {
                    logger.fine("too detailed");
                    toRemove.add(tile);
                } else if ((tile.getZoomLevel() < floor(zp + TIPPING)) && (!tile.isCovering()) && (!(ceil(zp) >= MAX_ZOOM))) {
                    logger.fine("not enough detailed");
                    toRemove.add(tile);
                }
            }
        }

        getChildren().removeAll(toRemove);

        logger.fine("DONE CLEANUP, #children = " + getChildren().size());
    }

    private void clearTiles() {

        List<Node> toRemove = new ArrayList<>();
        ObservableList<Node> children = this.getChildren();
        for (Node child : children) {
            if (child instanceof MapTile) {
                toRemove.add(child);
            }
        }
        getChildren().removeAll(children);

        for (int i = 0; i < tiles.length; i++) {
            tiles[i].clear();
        }

    }


    private MapTile getCoveringTile(MapTile tile) {
        int z = tile.myZoom;
        if (z > 0) {
            long pi = tile.i / 2;
            long pj = tile.j / 2;
            long i_max = 1 << (z - 1);
            Long key = pi * i_max + pj;
            // LongTuple it = new LongTuple(i,j);
            SoftReference<MapTile> ref = tiles[z - 1].get(key);
            if (ref != null) {
                logger.fine("[JVDBG] COVERING TILE FOUND!");
                return ref.get();
            } else {
                logger.fine("not tile found for " + z + ", " + pi + ", " + pj);
            }
        }
        return null;
    }

    /**
     * Called by the JavaFX Application Thread when a pulse is running.
     * In case the dirty flag has been set, we know that something has changed
     * and we need to reload/clean the tiles.
     */
    @Override
    protected void layoutChildren() {
        if (dirty) {
            loadTiles();
            dirty = false;
        }
        super.layoutChildren();
    }
    
    private void calculateCenterCoords() {
        double x = ((MapView)this.getParent()).getWidth()/2-this.getTranslateX();
        double y = ((MapView)this.getParent()).getHeight()/2 - this.getTranslateY();
        calculateCoords(x, y, centerLat, centerLon);
    }
    
    private void calculateCoords(double x, double y, SimpleDoubleProperty lat, SimpleDoubleProperty lon) {        
        double z = zoom.get();
        double latrad = Math.PI - (2.0 * Math.PI * y) / (Math.pow(2, z)*256.);
        double mlat = Math.toDegrees(Math.atan(Math.sinh(latrad)));
        double mlon = x / (256*Math.pow(2, z)) * 360 - 180;
        lon.set(mlon);
        lat.set(mlat);
    }
    
    /**
     * When something changes that would lead to a change in UI representation 
     * (e.g. map is dragged or zoomed), this method should be called.
     * This method will NOT update the map immediately, but it will set a 
     * flag and request a next pulse. 
     * This is much more performant than redrawing the map on each input event.
     */
    private void markDirty() {
        this.dirty = true;
        calculateCenterCoords();
        this.setNeedsLayout(true);
        Platform.requestNextPulse();
    }

    private double getMyWidth() {
        return this.getParent().getLayoutBounds().getWidth();
    }

    private double getMyHeight() {
        return this.getParent().getLayoutBounds().getHeight();
    }

}
