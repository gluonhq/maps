/*
 * Copyright (c) 2020, Gluon
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

import com.gluonhq.maps.tile.TileRetriever;
import com.gluonhq.maps.tile.TileRetrieverProvider;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.beans.property.ReadOnlyDoubleWrapper;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import java.util.logging.Logger;

public class TileImageView extends ImageView {

    private static final Logger logger = Logger.getLogger(TileImageView.class.getName());
    private static final TileRetriever TILE_RETRIEVER = TileRetrieverProvider.getInstance().load();

    public TileImageView(int zoom, long i, long j) {
        setFitHeight(256);
        setFitWidth(256);
        setPreserveRatio(true);
        setProgress(0);
        CompletableFuture<Image> future = TILE_RETRIEVER.loadTile(zoom, i, j);
        if (!future.isDone()) {
            Optional.ofNullable(placeholderImageSupplier).ifPresent(s -> setImage(s.get()));
            logger.fine("start downloading tile " + zoom + "/" + i + "/" + j);
            downloading.setValue(true);
            future.handle((image, t) -> {
                if (t != null) {
                    logger.fine("Tile " + zoom + "/" + i + "/" + j + " failed with exception");
                    setException(new Exception(t));
                    return null;
                }
                return image;
            }).thenAccept(file -> {
                logger.fine("Tile from downloaded file " + zoom + "/" + i + "/" + j);
                downloading.setValue(false);
                setImage(file);
                setProgress(1);
            });
        } else {
            logger.fine("Tile from file cache");
            setImage(future.getNow(null));
            setProgress(1);
        }
    }

    private static Supplier<Image> placeholderImageSupplier;

    public static void setPlaceholderImageSupplier(Supplier<Image> supplier) {
        placeholderImageSupplier = supplier;
    }

    private final ReadOnlyBooleanWrapper downloading = new ReadOnlyBooleanWrapper(TileImageView.this, "downloading", false);

    public final boolean isDownloading() {
        return downloading != null && downloading.get();
    }

    public final BooleanProperty downloadingProperty() {
        return downloading;
    }

    private ReadOnlyObjectWrapper<Exception> exception;

    private void setException(Exception value) {
        exceptionPropertyImpl().set(value);
    }

    public final Exception getException() {
        return exception == null ? null : exception.get();
    }

    public final ReadOnlyObjectProperty<Exception> exceptionProperty() {
        return exceptionPropertyImpl().getReadOnlyProperty();
    }

    private ReadOnlyObjectWrapper<Exception> exceptionPropertyImpl() {
        if (exception == null) {
            exception = new ReadOnlyObjectWrapper<Exception>(this, "exception");
        }
        return exception;
    }

    private ReadOnlyDoubleWrapper progress;


    private void setProgress(double value) {
        progressPropertyImpl().set(value);
    }

    public final double getProgress() {
        return progress == null ? 0.0 : progress.get();
    }

    public final ReadOnlyDoubleProperty progressProperty() {
        return progressPropertyImpl().getReadOnlyProperty();
    }

    private ReadOnlyDoubleWrapper progressPropertyImpl() {
        if (progress == null) {
            progress = new ReadOnlyDoubleWrapper(this, "progress");
        }
        return progress;
    }

}
