/*
 * Copyright (c) 2016, Gluon
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.

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

import com.gluonhq.charm.down.common.JavaFXPlatform;
import javafx.animation.Animation.Status;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.scene.layout.Region;
import javafx.util.Duration;

/**
 *
 * This is the top UI element of the map component.
 * The center location and the zoom level of the map can be altered by input events (mouse/touch/gestures)
 * or by calling the methods setCenter and setZoom.
 * TODO: add layers back here.
 */
public class MapView extends Region {

    private final BaseMap baseMap;
    private Timeline t;

    public MapView() {
        baseMap = new BaseMap();
        getChildren().add(baseMap);

        setOnMousePressed(t -> {
            baseMap.x0 = t.getSceneX();
            baseMap.y0 = t.getSceneY();
        });
        setOnMouseDragged(t -> {
            baseMap.moveX(baseMap.x0 - t.getSceneX());
            baseMap.moveY(baseMap.y0 - t.getSceneY());
            baseMap.x0 = t.getSceneX();
            baseMap.y0 = t.getSceneY();
        });
        setOnZoom(t -> baseMap.zoom( t.getZoomFactor()-1, (baseMap.x0 + t.getSceneX()) / 2.0, (baseMap.y0 + t.getSceneY()) / 2.0));
        if (JavaFXPlatform.isDesktop()) {
            setOnScroll(t -> baseMap.zoom(t.getDeltaY() > 1 ? .1 : -.1, t.getSceneX(), t.getSceneY()));
        }

    }

    public void setZoom(double zoom) {
        baseMap.setZoom(zoom);
    }

    public void setCenter(MapPoint mapPoint) {
        setCenter(mapPoint.getLatitude(), mapPoint.getLongitude());
    }

    public void setCenter(double lat, double lon) {
        baseMap.setCenter(lat, lon);
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
                new KeyFrame(Duration.ZERO, new KeyValue(baseMap.centerLat(), currentLat), new KeyValue(baseMap.centerLon(), currentLon)),
                new KeyFrame(Duration.seconds(waitTime), new KeyValue(baseMap.centerLat(), currentLat), new KeyValue(baseMap.centerLon(), currentLon)),
                new KeyFrame(Duration.seconds(waitTime + seconds), new KeyValue(baseMap.centerLat(), mapPoint.getLatitude()), new KeyValue(baseMap.centerLon(), mapPoint.getLongitude(), Interpolator.EASE_BOTH))
        );
        t.play();
    }

}
