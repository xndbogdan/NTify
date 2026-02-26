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

import org.apache.batik.anim.dom.SAXSVGDocumentFactory;
import org.apache.batik.transcoder.Transcoder;
import org.apache.batik.transcoder.TranscoderException;
import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.TranscoderOutput;
import org.apache.batik.transcoder.image.PNGTranscoder;
import org.apache.batik.util.XMLResourceDescriptor;
import org.apache.commons.io.IOUtils;
import org.w3c.dom.Document;

import javax.swing.*;
import java.awt.*;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.LinkedHashMap;
import java.util.Map;

public class SVGUtils {
    private static final int CACHE_SIZE = 100;

    // Cache for transcoded PNG data, keyed by SVG content hash + dimensions
    private static final LinkedHashMap<String, byte[]> pngCache =
            new LinkedHashMap<String, byte[]>(CACHE_SIZE, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<String, byte[]> eldest) {
                    return size() > CACHE_SIZE;
                }
            };

    // Reusable parser class name
    private static final String parser = XMLResourceDescriptor.getXMLParserClassName();

    private static String getCacheKey(String svgContent, int width, int height) {
        return svgContent.hashCode() + "_" + width + "x" + height;
    }
    public static ImageIcon svgToImageIcon(InputStream stream, int width, int height) {
        try {
            String svgContent = IOUtils.toString(stream, Charset.defaultCharset());
            String cacheKey = getCacheKey(svgContent, width, height);

            // Check cache first
            synchronized (pngCache) {
                byte[] cached = pngCache.get(cacheKey);
                if (cached != null) {
                    return new ImageIcon(cached);
                }
            }

            // Create a transcoder for PNG output
            Transcoder transcoder = new PNGTranscoder();

            // Set the width and height for the output image
            transcoder.addTranscodingHint(PNGTranscoder.KEY_WIDTH, (float) width);
            transcoder.addTranscodingHint(PNGTranscoder.KEY_HEIGHT, (float) height);

            // Create a document from the SVG content
            SAXSVGDocumentFactory factory = new SAXSVGDocumentFactory(parser);
            Document document = factory.createDocument(null, new ByteArrayInputStream(svgContent.getBytes()));

            // Create a transcoder input
            TranscoderInput transcoderInput = new TranscoderInput(document);

            // Create a transcoder output
            TranscoderOutput transcoderOutput = new TranscoderOutput(new java.io.ByteArrayOutputStream());

            // Perform the transcoding (SVG to PNG)
            transcoder.transcode(transcoderInput, transcoderOutput);

            // Get the PNG data
            byte[] pngImageData = ((java.io.ByteArrayOutputStream) transcoderOutput.getOutputStream()).toByteArray();

            // Store in cache
            synchronized (pngCache) {
                pngCache.put(cacheKey, pngImageData);
            }

            // Create an ImageIcon from the PNG data
            return new ImageIcon(pngImageData);
        } catch (IOException | TranscoderException ex) {
            throw new RuntimeException(ex);
        }
    }

    @SuppressWarnings("all")
    public static ImageIcon svgToImageIconSameSize(InputStream stream, Dimension size) {
        return svgToImageIcon(stream, size.height, size.height);
    }

    public static InputStream svgToImageInputStreamSameSize(InputStream stream, Dimension size) {
        try {
            String svgContent = IOUtils.toString(stream, Charset.defaultCharset());
            String cacheKey = getCacheKey(svgContent, size.width, size.height);

            // Check cache first
            synchronized (pngCache) {
                byte[] cached = pngCache.get(cacheKey);
                if (cached != null) {
                    return new ByteArrayInputStream(cached);
                }
            }

            // Create a transcoder for PNG output
            Transcoder transcoder = new PNGTranscoder();

            // Set the width and height for the output image
            transcoder.addTranscodingHint(PNGTranscoder.KEY_WIDTH, (float) size.width);
            transcoder.addTranscodingHint(PNGTranscoder.KEY_HEIGHT, (float) size.height);

            // Create a document from the SVG content
            SAXSVGDocumentFactory factory = new SAXSVGDocumentFactory(parser);
            Document document = factory.createDocument(null, new ByteArrayInputStream(svgContent.getBytes()));

            // Create a transcoder input
            TranscoderInput transcoderInput = new TranscoderInput(document);

            // Create a transcoder output
            TranscoderOutput transcoderOutput = new TranscoderOutput(new java.io.ByteArrayOutputStream());

            // Perform the transcoding (SVG to PNG)
            transcoder.transcode(transcoderInput, transcoderOutput);

            // Get the PNG data
            byte[] pngImageData = ((java.io.ByteArrayOutputStream) transcoderOutput.getOutputStream()).toByteArray();

            // Store in cache
            synchronized (pngCache) {
                pngCache.put(cacheKey, pngImageData);
            }

            // Return as InputStream
            return new ByteArrayInputStream(pngImageData);
        } catch (IOException | TranscoderException ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * Clear the SVG cache.
     */
    public static void clearCache() {
        synchronized (pngCache) {
            pngCache.clear();
        }
    }
}
