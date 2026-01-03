package compression;

import java.io.File;
import java.io.IOException;

public interface Compressor {
    void compress(File input, File output) throws IOException;
    void decompress(File input, File output) throws IOException;
}