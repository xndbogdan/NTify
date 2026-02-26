/*
 * Copyright [2023-2025] [Gianluca Beil]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.spotifyxp.utils;

import com.spotifyxp.logging.ConsoleLogging;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

/**
 * LRU cache for album art and other images.
 * Provides async loading with callback support.
 */
public class ImageCache {
    private static final int MAX_SIZE = 50;
    private static final ExecutorService executor = Executors.newFixedThreadPool(2, r -> {
        Thread t = new Thread(r, "ImageCache-Loader");
        t.setDaemon(true);
        return t;
    });

    private static final LinkedHashMap<String, byte[]> cache =
            new LinkedHashMap<String, byte[]>(MAX_SIZE, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<String, byte[]> eldest) {
                    return size() > MAX_SIZE;
                }
            };

    /**
     * Load an image asynchronously from URL, using cache if available.
     * @param url The URL to load from
     * @param callback Called on EDT with the loaded InputStream, or null on failure
     */
    public static void loadAsync(String url, Consumer<InputStream> callback) {
        synchronized (cache) {
            byte[] cached = cache.get(url);
            if (cached != null) {
                SwingUtilities.invokeLater(() -> callback.accept(new ByteArrayInputStream(cached)));
                return;
            }
        }

        executor.submit(() -> {
            try {
                URL imageUrl = new URL(url);
                try (InputStream is = imageUrl.openStream();
                     ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                    byte[] buffer = new byte[8192];
                    int bytesRead;
                    while ((bytesRead = is.read(buffer)) != -1) {
                        baos.write(buffer, 0, bytesRead);
                    }
                    byte[] imageData = baos.toByteArray();

                    synchronized (cache) {
                        cache.put(url, imageData);
                    }

                    SwingUtilities.invokeLater(() -> callback.accept(new ByteArrayInputStream(imageData)));
                }
            } catch (IOException e) {
                ConsoleLogging.warning("Failed to load image: " + url);
                SwingUtilities.invokeLater(() -> callback.accept(null));
            }
        });
    }

    /**
     * Load an image synchronously from URL, using cache if available.
     * @param url The URL to load from
     * @return InputStream for the image, or null on failure
     */
    public static InputStream loadSync(String url) {
        synchronized (cache) {
            byte[] cached = cache.get(url);
            if (cached != null) {
                return new ByteArrayInputStream(cached);
            }
        }

        try {
            URL imageUrl = new URL(url);
            try (InputStream is = imageUrl.openStream();
                 ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = is.read(buffer)) != -1) {
                    baos.write(buffer, 0, bytesRead);
                }
                byte[] imageData = baos.toByteArray();

                synchronized (cache) {
                    cache.put(url, imageData);
                }

                return new ByteArrayInputStream(imageData);
            }
        } catch (IOException e) {
            ConsoleLogging.warning("Failed to load image: " + url);
            return null;
        }
    }

    /**
     * Clear the entire cache.
     */
    public static void clear() {
        synchronized (cache) {
            cache.clear();
        }
    }

    /**
     * Get current cache size.
     */
    public static int size() {
        synchronized (cache) {
            return cache.size();
        }
    }
}
