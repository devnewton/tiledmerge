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
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import org.apache.commons.io.FileUtils;
import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import tiled.core.TileSet;

public class App {

    @Option(name = "-o", usage = "output directory (default to current directory)")
    private File outputDir = new File(".").getAbsoluteFile();
    
    @Option(name = "-t", usage = "merged tileset filename")
    private String mergedTilesetName = "merged.tsx";
    
    @Option(name = "-p", usage = "merged map filename prefix")
    private String mergedMapFilenamePrefix = "merged_";
    
    @Argument(required = true, multiValued = true, usage = "input tmx files or directories containing tmx (default to current directory)")
    private List<File> inputs;

    public static void main(String[] args) throws IOException {
        App app = new App();
        CmdLineParser parser = new CmdLineParser(app);
        try {
            parser.parseArgument(args);
        } catch (CmdLineException e) {
            System.err.println(e.getMessage());
            System.err.println("tiledmerge [args] input1.tmx input2.tmx /dir/with/tmx/files/ ...");
            parser.printUsage(System.err);
            return;
        }
        app.merge();
    }

    private void merge() throws IOException {
        TiledMerger merger = new TiledMerger();
        for (File input : inputs) {
            if (input.isFile()) {
                merger.addTmx(input);
            } else if (input.isDirectory()) {
                Iterator it = FileUtils.iterateFiles(input, new String[]{"tmx"}, true);
                while (it.hasNext()) {
                    File file = (File) it.next();
                    if(!file.getName().startsWith(mergedMapFilenamePrefix)) {
                        merger.addTmx(file);   
                    }
                }
            }
        }
        TileSet mergedTileset = merger.saveMergedTileset(outputDir, mergedTilesetName);
        merger.saveMergedMaps(mergedTileset, mergedMapFilenamePrefix);
    }
}
