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
package com.gluonhq.impl.maps;

import com.gluonhq.charm.down.common.PlatformFactory;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.concurrent.Task;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 */
public class ImageRetriever {


    private static final Logger logger = Logger.getLogger( ImageRetriever.class.getName() );

    static String host = "http://tile.openstreetmap.org/";
    static File cacheRoot;
    static boolean hasFileCache = false;
    static CacheThread cacheThread = null;

    static {
        try {
            File storageRoot = PlatformFactory.getPlatform().getPrivateStorage();
            cacheRoot = new File(storageRoot, ".gluonmaps");
            logger.fine("[JVDBG] cacheroot = " + cacheRoot);
            if (!cacheRoot.isDirectory()) {
                hasFileCache = cacheRoot.mkdirs();
            } else {
                hasFileCache = true;
            }
            if (hasFileCache) {
                cacheThread = new CacheThread(cacheRoot.getPath());
                cacheThread.start();
            }
            logger.info("hasfilecache = " + hasFileCache);
        } catch (IOException ex) {
            hasFileCache = false;
            logger.log(Level.SEVERE, null, ex);
        }
    }

    static ReadOnlyDoubleProperty fillImage(ImageView imageView, int zoom, long i, long j) {
        Image image = fromFileCache(zoom, i, j);
        if (image == null) {
            String urlString = host + zoom + "/" + i + "/" + j + ".png";
            if (hasFileCache) {
                Task<Object> task = new Task() {
                    @Override
                    protected Object call() throws Exception {
                        cacheThread.cacheImage(urlString, zoom, i, j);
                        return null; // can't return image yet
                    }
                };
                Thread t = new Thread(task);
                t.start();
            }
            image = new Image(urlString, true);
        }
        imageView.setImage(image);
        return image.progressProperty();
    }

    /**
     * Return an image from filecache, or null if the cache doesn't contain the
     * image
     *
     * @param zoom
     * @param i
     * @param j
     * @return
     */
    static private Image fromFileCache(int zoom, long i, long j) {
        if (!hasFileCache) {
            return null;
        }
        String tag = zoom + File.separator + i + File.separator + j + ".png";
        File f = new File(cacheRoot, tag);
        if (f.exists()) {
            Image answer = new Image(f.toURI().toString(), true);
            return answer;
        }
        return null;
    }

    private static class CacheThread extends Thread {

        private boolean active = true;
        private String basePath;
        private final Set<String> offered = new HashSet<>();
        private final BlockingDeque<String> deque = new LinkedBlockingDeque<>();

        public CacheThread(String basePath) {
            this.basePath = basePath;
            setDaemon(true);
            setName("TileType CacheImagesThread");
        }

        public void deactivate() {
            this.active = false;
        }

        @Override
        public void run() {
            while (active) {
                try {
                    String key = deque.pollFirst(10, TimeUnit.SECONDS);
                    if (key != null) {
                        String url = key.substring(0, key.lastIndexOf(";"));
                        String[] split = key.substring(key.lastIndexOf(";") + 1).split("/");
                        int zoom = Integer.parseInt(split[0]);
                        long i = Long.parseLong(split[1]);
                        long j = Long.parseLong(split[2]);
                        doCache(url, zoom, i, j);
                    }
                } catch (InterruptedException e) {
                    logger.log(Level.WARNING, null, e);
                }
            }
        }

        public void cacheImage(String url, int zoom, long i, long j) {
            String key = url + ";" + zoom + "/" + i + "/" + j;
            synchronized (offered) {
                if (!offered.contains(key)) {
                    offered.add(key);
                    deque.offerFirst(key);
                }
            }
        }

        private void doCache(String urlString, int zoom, long i, long j) {
            try {
                URL url = new URL(urlString);
                try (InputStream inputStream = url.openConnection().getInputStream()) {
                    String enc = File.separator + zoom + File.separator + i + File.separator + j + ".png";
                    logger.info("retrieve " + urlString + " and store " + enc);
                    File candidate = new File(cacheRoot, enc);
                    candidate.getParentFile().mkdirs();
                    try (FileOutputStream fos = new FileOutputStream(candidate)) {
                        byte[] buff = new byte[4096];
                        int len = inputStream.read(buff);
                        while (len > 0) {
                            fos.write(buff, 0, len);
                            len = inputStream.read(buff);
                        }
                        fos.close();
                    }
                }
            } catch (IOException ex) {
                logger.log(Level.SEVERE, null, ex);
            }
        }

    }

}
