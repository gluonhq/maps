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

import com.gluonhq.impl.maps.BaseMap;
import javafx.scene.Parent;

/**
 * A MapLayer can be added on top a BaseMap (which provides the map tiles).
 * MapLayers contain specific functionality that is rendered by overriding the
 * {@link #layoutLayer()} method.
 * <p>
 * There are 2 reasons why the {@link #layoutLayer() } will be called:
 * <ul>
 * <li>The MapLayer {@link #layoutLayer() } method will be called by the MapView
 * in case the coordinates (center/zoom) are changed.
 * <li>When the content of the MapLayer implementation changes (e.g. a POI is
 * added or moved), it should call the {@link #markDirty() } method.
 * This will mark this layer dirty and request it to be recalculated in the next
 * Pulse.
 * </ul>
 * <p>
 * The MapLayer has access to the {@link #baseMap} instance that renders
 * the map tiles and it can use the methods provided by the {@link BaseMap}
 */
public class MapLayer extends Parent {

    private boolean dirty = false;

    protected BaseMap baseMap;

    /**
     * Only the MapView should call this method. We want implementations to
     * access the BaseMap (since they need to be able to act on changes in
     * center/zoom values) but they can not modify it.
     *
     * @param baseMap
     */
    final void setBaseMap(BaseMap baseMap) {
        this.baseMap = baseMap;
        initialize();
    }

    /**
     * This method is called by the framework when the MapLayer is created and
     * added to the Map. At this point, it is safe to use the
     * <code>baseMap</code> and its fields.
     * The default implementation doesn't do anything. It is up to specific
     * layers to add layer-specific initialization.
     */
    protected void initialize() {
    }

    /**
     * Implementations should call this function when the content of the data
     * has changed. It will set the <code>dirty</code> flag, and it will
     * request the layer to be reconsidered during the next pulse.
     */
    protected void markDirty() {
        this.dirty = true;
        this.requestLayout();
    }

    @Override
    protected void layoutChildren() {
        if (dirty) {
            layoutLayer();
        }
    }
    /**
     * This method is called when a Pulse is running and it is detected that
     * the layer should be redrawn, as a consequence of an earlier call to
     * {@link #markDirty() } (which should happen in case the info in the
     * specific layer has changed) or when the {@link com.gluonhq.maps.MapView}
     * has its dirty flag set to true (which happens when the map is moved/zoomed).
     * The default implementation doesn't do anything. It is up to specific
     * layers to add layer-specific rendering.
     */
    protected void layoutLayer() {
    }

}
