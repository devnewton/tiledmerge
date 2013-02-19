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

import java.awt.Image;
import java.awt.image.BufferedImage;
import java.awt.image.PixelGrabber;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import tiled.core.Tile;
import tiled.core.TileSet;

/**
 *
 * @author devnewton
 */
public class MergeableTile {

    private List<OriginalTilesInfos> originalTiles = new ArrayList<OriginalTilesInfos>();
    private final BufferedImage image;
    private final Properties properties;
    private final int width;
    private final int height;
    private Tile mergedTile;

    MergeableTile(Tile tile, File file) {
        originalTiles.add(new OriginalTilesInfos(tile, file));
        this.image = tile.getImage();
        this.properties = tile.getProperties();
        this.width = tile.getWidth();
        this.height = tile.getHeight();
    }

    boolean mergeWith(MergeableTile other) {
        if (canMergeWith(other)) {
            originalTiles.addAll(other.originalTiles);
            return true;
        } else {
            return false;
        }
    }

    private boolean canMergeWith(MergeableTile other) {
        return properties.equals(other.properties) && width == other.width && height == other.height && areImagesPixelEquals(image, other.image);
    }

    private boolean areImagesPixelEquals(Image a, Image b) {
        try {
            return Arrays.equals(convertToPixels(a), convertToPixels(b));
        } catch (InterruptedException ex) {
            Logger.getLogger(MergeableTile.class.getName()).log(Level.SEVERE, null, ex);
            return false;
        }
    }

    public static int[] convertToPixels(Image img) throws InterruptedException {

        int width = img.getWidth(null);
        int height = img.getHeight(null);
        int[] pixel = new int[width * height];
        PixelGrabber pg = new PixelGrabber(img, 0, 0, width, height, pixel, 0, width);
        pg.grabPixels();
        return pixel;

    }

    void createMergedTile(TileSet tileset, String tileImagePrefix) throws IOException {
        mergedTile = new Tile(tileset);
        mergedTile.setProperties(properties);
        mergedTile.setImage(image);
        tileset.addNewTile(mergedTile);
        mergedTile.setTilebmpFile(new File(tileImagePrefix + "_" + mergedTile.getId() + ".png"));
        ImageIO.write((BufferedImage)mergedTile.getImage(), "png", mergedTile.getTilebmpFile());
    }

    Tile getMerged(Tile originalTile) {
        for(OriginalTilesInfos infos : originalTiles) {
            if(infos.getTile() == originalTile) {
                return mergedTile;
            }
        }
        return null;
    }
}
