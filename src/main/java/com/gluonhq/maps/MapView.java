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
package com.gluonhq.maps;

import com.gluonhq.charm.down.Platform;
import java.util.LinkedList;
import java.util.List;

import com.gluonhq.impl.maps.BaseMap;
import javafx.animation.Animation.Status;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.geometry.Point2D;
import javafx.scene.layout.Region;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

/**
 *
 * This is the top UI element of the map component. The center location and the
 * zoom level of the map can be altered by input events (mouse/touch/gestures)
 * or by calling the methods setCenter and setZoom.
 */
public class MapView extends Region {

    private final BaseMap baseMap;
    private Timeline t;
    private final List<MapLayer> layers = new LinkedList<>();
    private Rectangle clip;
    private MapPoint centerPoint = null;
    private boolean zooming = false;
    
    /**
     * Create a MapView component.
     */
    public MapView() {
        baseMap = new BaseMap();
        getChildren().add(baseMap);
        registerInputListeners();

        baseMap.centerLat().addListener(o -> markDirty());
        baseMap.centerLon().addListener(o -> markDirty());
        clip = new Rectangle();
        this.setClip(clip);
        this.layoutBoundsProperty().addListener(e -> {
            // in case our assigned space changes, AND in case we are requested
            // to center at a specific point, we need to re-center.
            if (centerPoint != null) {
                // we will set the center to a slightly different location first, in order 
                // to trigger the invalidationListeners.
                setCenter(centerPoint.getLatitude()+.00001, centerPoint.getLongitude()+.00001);
                setCenter(centerPoint);
            }
        });
    }

    
    private void registerInputListeners() {
        setOnMousePressed(t -> {
            if (zooming) return;
            baseMap.x0 = t.getX();
            baseMap.y0 = t.getY();
            centerPoint = null; // once the user starts moving, we don't track the center anymore.
        });
        setOnMouseDragged(t -> {
            if (zooming) return;
            baseMap.moveX(baseMap.x0 - t.getX());
            baseMap.moveY(baseMap.y0 - t.getY());
            baseMap.x0 = t.getX();
            baseMap.y0 = t.getY();
        });
        setOnZoomStarted(t -> zooming = true);
        setOnZoomFinished(t -> zooming = false);
        setOnZoom(t -> baseMap.zoom(t.getZoomFactor() - 1, t.getX(), t.getY()));
        if (Platform.isDesktop()) {
            setOnScroll(t -> baseMap.zoom(t.getDeltaY() > 1 ? .1 : -.1, t.getX(), t.getY()));
        }
    }

    /**
     * Request the map to set its zoom level to the specified value. The map
     * considers this request, but it does not guarantee the zoom level will be
     * set to the provided value
     *
     * @param zoom the requested zoom level
     */
    public void setZoom(double zoom) {
        baseMap.setZoom(zoom);
    }

    /**
     * Returns the preferred zoom level of this map.
     * @return the zoom level
     */
    public double getZoom() {
        return baseMap.getZoom();
    }

    /**
     * Request the map to position itself around the specified center
     *
     * @param mapPoint
     */
    public void setCenter(MapPoint mapPoint) {
        setCenter(mapPoint.getLatitude(), mapPoint.getLongitude());
    }

    /**
     * Returns the center point of this map
     * @return the center point
     */
    public MapPoint getCenter() {
        Point2D center = baseMap.getCenter();
        return new MapPoint(center.getX(), center.getY());
    }

    /**
     * Request the map to position itself around the specified center
     *
     * @param lat
     * @param lon
     */
    public void setCenter(double lat, double lon) {
        this.centerPoint = new MapPoint(lat, lon);
        baseMap.setCenter(lat, lon);
    }

    /**
     * Add a new layer on top of this map. Layers are displayed in order of
     * addition, with the last added layer to be on top
     *
     * @param child
     */
    public void addLayer(MapLayer child) {
        child.setBaseMap(this.baseMap);
        layers.add(child);
        this.getChildren().add(child);
    }

    /**
     * Removes the specified layer from the map
     *
     * @param child
     */
    public void removeLayer(MapLayer child) {
        layers.remove(child);
        this.getChildren().remove(child);
    }

    /**
     * Wait a bit, then move to the specified mapPoint in seconds time
     *
     * @param waitTime the time to wait before we start moving
     * @param mapPoint the destination of the move
     * @param seconds the time the move should take
     */
    public void flyTo(double waitTime, MapPoint mapPoint, double seconds) {
        if ((t != null) && (t.getStatus() == Status.RUNNING)) {
            t.stop();
        }
        double currentLat = baseMap.centerLat().get();
        double currentLon = baseMap.centerLon().get();
        t = new Timeline(
            new KeyFrame(Duration.ZERO, new KeyValue(baseMap.prefCenterLat(), currentLat), new KeyValue(baseMap.prefCenterLon(), currentLon)),
            new KeyFrame(Duration.seconds(waitTime), new KeyValue(baseMap.prefCenterLat(), currentLat), new KeyValue(baseMap.prefCenterLon(), currentLon)),
            new KeyFrame(Duration.seconds(waitTime + seconds), new KeyValue(baseMap.prefCenterLat(), mapPoint.getLatitude()), new KeyValue(baseMap.prefCenterLon(), mapPoint.getLongitude(), Interpolator.EASE_BOTH))
        );
        t.play();
    }

    private boolean dirty = false;

    protected void markDirty() {
        dirty = true;
        this.setNeedsLayout(true);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void layoutChildren() {
        final double w = getWidth();
        final double h = getHeight();

        if (dirty) {
            for (MapLayer layer : layers) {
                layer.layoutLayer();
            }
        }
        super.layoutChildren();
        dirty = false;

        // we need to get these values or we won't be notified on new changes
        baseMap.centerLon().get();
        baseMap.centerLat().get();

        // update clip
        clip.setWidth(w);
        clip.setHeight(h);
    }
}
