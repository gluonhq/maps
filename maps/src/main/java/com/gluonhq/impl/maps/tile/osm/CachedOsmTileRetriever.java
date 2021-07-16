/*
 * Copyright (c) 2016, 2021, Gluon
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
package com.gluonhq.impl.maps.tile.osm;

import com.gluonhq.attach.storage.StorageService;
import javafx.scene.image.Image;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CachedOsmTileRetriever extends OsmTileRetriever {

    private static final Logger logger = Logger.getLogger(CachedOsmTileRetriever.class.getName());
    private static final int TIMEOUT = 10000;
    private static final ExecutorService EXECUTOR = Executors.newFixedThreadPool(2, new DaemonThreadFactory());

    static File cacheRoot;
    static boolean hasFileCache;
    static {
        try {
            Optional<File> storageRoot = StorageService.create()
                    .flatMap(StorageService::getPrivateStorage);
            if (storageRoot.isEmpty()) {
                cacheRoot = Files.createTempDirectory(".gluonmaps").toFile();
                logger.info("StorageService implementation not found.\n" +
                        "Tiles will be cached at a temporary directory: " + cacheRoot);
            } else {
                cacheRoot = new File(storageRoot.get(), ".gluonmaps");
            }
            logger.fine("[JVDBG] cacheroot = " + cacheRoot);
            if (!cacheRoot.isDirectory()) {
                hasFileCache = cacheRoot.mkdirs();
            } else {
                hasFileCache = true;
            }
            logger.fine("hasfilecache = " + hasFileCache);
        } catch (IOException ex) {
            hasFileCache = false;
            logger.log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public CompletableFuture<Image> loadTile(int zoom, long i, long j) {
        Image image = fromFileCache(zoom, i, j);
        if (image != null) {
            return CompletableFuture.completedFuture(image);
        }
        return CompletableFuture.supplyAsync(() -> {
            logger.fine("start downloading tile " + zoom + "/" + i + "/" + j);
            try {
                return CacheThread.cacheImage(zoom, i, j);
            } catch (IOException e) {
                throw new RuntimeException("Error " + e.getMessage());
            }
        }, EXECUTOR);
    }


    static private Image fromFileCache(int zoom, long i, long j) {
        if (!hasFileCache) {
            return null;
        }
        String tag = zoom + File.separator + i + File.separator + j + ".png";
        File f = new File(cacheRoot, tag);
        return f.exists() ? new Image(f.toURI().toString(), true) : null;
    }

    private static class CacheThread {

        public static Image cacheImage(int zoom, long i, long j) throws IOException {
            File file = doCache(buildImageUrlString(zoom, i, j), zoom, i, j);
            return new Image(new FileInputStream(file));
        }

        private static File doCache(String urlString, int zoom, long i, long j) throws IOException {
            final URLConnection openConnection;
            URL url = new URL(urlString);
            openConnection = url.openConnection();
            openConnection.addRequestProperty("User-Agent", httpAgent);
            openConnection.setConnectTimeout(TIMEOUT);
            openConnection.setReadTimeout(TIMEOUT);
            try (InputStream inputStream = openConnection.getInputStream()) {
                String enc = File.separator + zoom + File.separator + i + File.separator + j + ".png";
                logger.fine("retrieve " + urlString + " and store " + enc);
                File candidate = new File(cacheRoot, enc);
                candidate.getParentFile().mkdirs();
                try (FileOutputStream fos = new FileOutputStream(candidate)) {
                    byte[] buff = new byte[4096];
                    int len = inputStream.read(buff);
                    while (len > 0) {
                        fos.write(buff, 0, len);
                        len = inputStream.read(buff);
                    }
                    return candidate;
                }
            }
        }
    }

    private static class DaemonThreadFactory implements ThreadFactory {

        @Override
        public Thread newThread(final Runnable r) {
            Thread t = new Thread(r);
            t.setDaemon(true);
            return t;
        }
    }
    
}
