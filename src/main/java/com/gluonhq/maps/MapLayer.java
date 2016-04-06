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

import javafx.scene.Parent;

/**
 * A MapLayer can be added on top a BaseMap (which provides the map tiles).
 * MapLayers contain specific functionality that is rendered by overriding the
 * {@link #layoutLayer()} method.
 * As the MapLayer has access to the {@link #baseMap} instance that renders
 * the map tiles, it can listen for changes (center coordinates or zoom level)
 * in this baseMap. 
 * <p>
 * For example, a MapLayer that wants to change the position of its content when
 * the center of the map changes, can implement the {@link #initialize()} 
 * method as follows:
 * <pre>
 * {@code
 *  {@}Override
 *  public void initialize() {
 *      baseMap.centerLat().addListener(o -> markDirty());
 *      baseMap.centerLon().addListener(o -> markDirty());
 *  }
 * }</pre>
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
     * This method is called when a Pulse is running and it is detected that
     * the layer should be redrawn, as a consequence of an earlier call to
     * {@link #markDirty() }.
     * The default implementation doesn't do anything. It is up to specific
     * layers to add layer-specific rendering.
     */
    protected void layoutLayer() {
    }

    /**
     * Layers should call this method whenever something happens that makes
     * them need to recompute their UI values
     */
    protected void markDirty() {
        dirty = true;
        this.setNeedsLayout(true);
    }

    /** {@inheritDoc} */
    @Override protected void layoutChildren() {
        if (dirty) {
            layoutLayer();
        }
        super.layoutChildren();
        dirty = false;
    }


}
