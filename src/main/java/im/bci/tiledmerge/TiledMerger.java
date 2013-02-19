/*
 * Copyright (c) 2013 devnewton <devnewton@bci.im>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * * Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 *
 * * Neither the name of 'devnewton <devnewton@bci.im>' nor the names of
 *   its contributors may be used to endorse or promote products derived
 *   from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package im.bci.tiledmerge;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import tiled.core.MapLayer;
import tiled.core.Tile;
import tiled.core.TileLayer;
import tiled.core.TileSet;
import tiled.io.TMXMapReader;
import tiled.io.TMXMapWriter;

/**
 *
 * @author devnewton
 */
public class TiledMerger {

    private Map<File, tiled.core.Map> maps = new HashMap<File, tiled.core.Map>();
    private List<MergeableTile> tiles = new ArrayList<MergeableTile>();

    void addTmx(File file) {
        System.out.println("Merge " + file);
        try {
            TMXMapReader reader = new TMXMapReader();
            FileInputStream is = new FileInputStream(file);
            try {
                tiled.core.Map map = reader.readMap(is);
                maps.put(file, map);

                for (TileSet tileset : map.getTileSets()) {
                    for (Tile tile : tileset) {
                        MergeableTile mergeable = new MergeableTile(tile, file);
                        mergeOrAddToTile(mergeable);
                    }
                }
            } finally {
                is.close();
            }
        } catch (Exception ex) {
            Logger.getLogger(TiledMerger.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    void merge() {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    private void mergeOrAddToTile(MergeableTile newTile) {
        for (MergeableTile existingTile : tiles) {
            if (existingTile.mergeWith(newTile)) {
                return;
            }
        }
        tiles.add(newTile);
    }

    TileSet saveMergedTileset(File outputDir, String mergedTilesetName) throws IOException {
        File mergeTilesetFile = new File(outputDir.getCanonicalPath() + File.separator + mergedTilesetName);
        TileSet mergedTileset = buildMergedTileset(mergeTilesetFile.getCanonicalPath().replace(".tsx", ""));
        TMXMapWriter writer = new TMXMapWriter();
        try {
            System.out.println("Write merged tileset " + mergeTilesetFile);
            mergedTileset.setSource(mergeTilesetFile.getCanonicalPath());
            writer.writeTileset(mergedTileset, mergeTilesetFile.getCanonicalPath());
        } catch (Exception ex) {
            Logger.getLogger(TiledMerger.class.getName()).log(Level.SEVERE, null, ex);
        }
        return mergedTileset;
    }

    private TileSet buildMergedTileset(String tileImagePrefix) throws IOException {
        TileSet tileset = new TileSet();
        for (MergeableTile mergeableTile : tiles) {
            mergeableTile.createMergedTile(tileset, tileImagePrefix);
        }
        return tileset;
    }

    void saveMergedMaps(TileSet mergedTileset, String mergedMapFilenamePrefix) {
        for (Entry<File, tiled.core.Map> entry : maps.entrySet()) {
            saveMergedMap(entry.getKey(), buildMergedMap(entry.getValue(), mergedTileset), mergedMapFilenamePrefix);
        }
    }

    private void saveMergedMap(File file, tiled.core.Map map, String prefix) {
        File mergedOuputFile = new File(file.getParent() + File.separator + prefix + file.getName());
        TMXMapWriter writer = new TMXMapWriter();
        try {
            writer.writeMap(map, mergedOuputFile.getCanonicalPath());
        } catch (Exception ex) {
            Logger.getLogger(TiledMerger.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    private tiled.core.Map buildMergedMap(tiled.core.Map originalMap, TileSet mergedTileset) {
        tiled.core.Map mergedMap = new tiled.core.Map(originalMap.getWidth(), originalMap.getHeight());
        mergedMap.setProperties((Properties) originalMap.getProperties().clone());
        mergedMap.setTileWidth(originalMap.getTileWidth());
        mergedMap.setTileHeight(originalMap.getTileHeight());
        mergedMap.addTileset(mergedTileset);
        for (MapLayer layer : originalMap.getLayers()) {
            if (layer instanceof TileLayer) {
                mergedMap.addLayer(buildMergedLayer(mergedMap, originalMap, (TileLayer) layer, mergedTileset));
            } else {
                try {
                    mergedMap.addLayer((MapLayer) layer.clone());
                } catch (CloneNotSupportedException ex) {
                    Logger.getLogger(TiledMerger.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
        return mergedMap;
    }

    private TileLayer buildMergedLayer(tiled.core.Map mergedMap, tiled.core.Map originalMap, TileLayer originalLayer, TileSet mergedTileset) {
        TileLayer layer = new TileLayer(mergedMap, originalLayer.getWidth(), originalLayer.getHeight());
        layer.setProperties((Properties) originalLayer.getProperties().clone());
        layer.setName(originalLayer.getName());
        for (int y = 0; y < layer.getWidth(); ++y) {
            for (int x = 0; x < layer.getHeight(); ++x) {
                layer.setTileAt(x, y, findMergedTile(originalLayer.getTileAt(x, y)));
            }
        }
        return layer;
    }

    private Tile findMergedTile(Tile originalTile) {
        if (null == originalTile) {
            return null;
        }
        for (MergeableTile mergableTile : tiles) {
            Tile mergedTile = mergableTile.getMerged(originalTile);
            if (null != mergedTile) {
                return mergedTile;
            }
        }
        System.out.println("Cannot find merged tile for gid=" + originalTile.getGid());
        return null;
    }
}
