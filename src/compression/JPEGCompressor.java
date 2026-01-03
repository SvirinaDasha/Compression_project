package compression;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class JPEGCompressor implements Compressor {
    private static final int BLOCK_SIZE = 8;

    private final int quality;

    public JPEGCompressor() {
        this(30);
    }

    public JPEGCompressor(int quality) {
        this.quality = Math.max(1, Math.min(100, quality));
    }

    private static final int[][] BASE_Q_Y = {
            {16,11,10,16,24,40,51,61}, {12,12,14,19,26,58,60,55},
            {14,13,16,24,40,57,69,56}, {14,17,22,29,51,87,80,62},
            {18,22,37,56,68,109,103,77}, {24,35,55,64,81,104,113,92},
            {49,64,78,87,103,121,120,101}, {72,92,95,98,112,100,103,99}
    };

    private static final int[][] BASE_Q_C = {
            {17,18,24,47,99,99,99,99}, {18,21,26,66,99,99,99,99},
            {24,26,56,99,99,99,99,99}, {47,66,99,99,99,99,99,99},
            {99,99,99,99,99,99,99,99}, {99,99,99,99,99,99,99,99},
            {99,99,99,99,99,99,99,99}, {99,99,99,99,99,99,99,99}
    };

    private int[][] scaleQuantTable(int[][] table) {
        double scale = (quality < 50) ? 5000.0 / quality : 200.0 - 2 * quality;
        int[][] result = new int[BLOCK_SIZE][BLOCK_SIZE];
        for (int i = 0; i < BLOCK_SIZE; i++)
            for (int j = 0; j < BLOCK_SIZE; j++) {
                int val = (int) Math.round((table[i][j] * scale + 50) / 100);
                result[i][j] = Math.max(1, Math.min(255, val));
            }
        return result;
    }

    @Override
    public void compress(File input, File output) throws IOException {
        BufferedImage img = ImageIO.read(input);
        int width = img.getWidth();
        int height = img.getHeight();

        double[][] y = new double[height][width];
        double[][] cb = new double[height][width];
        double[][] cr = new double[height][width];

        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                Color color = new Color(img.getRGB(j, i));
                int r = color.getRed();
                int g = color.getGreen();
                int b = color.getBlue();

                y[i][j] = 0.299 * r + 0.587 * g + 0.114 * b - 128;
                cb[i][j] = -0.168736 * r - 0.331264 * g + 0.5 * b;
                cr[i][j] = 0.5 * r - 0.418688 * g - 0.081312 * b;
            }
        }

        double[][] cb420 = downsample(cb);
        double[][] cr420 = downsample(cr);

        ByteArrayOutputStream yStream = new ByteArrayOutputStream();
        ByteArrayOutputStream cbStream = new ByteArrayOutputStream();
        ByteArrayOutputStream crStream = new ByteArrayOutputStream();

        compressChannel(y, scaleQuantTable(BASE_Q_Y), yStream);
        compressChannel(cb420, scaleQuantTable(BASE_Q_C), cbStream);
        compressChannel(cr420, scaleQuantTable(BASE_Q_C), crStream);

        try (DataOutputStream out = new DataOutputStream(new FileOutputStream(output))) {
            out.writeInt(width);
            out.writeInt(height);
            out.writeInt(yStream.size());
            out.writeInt(cbStream.size());
            out.writeInt(crStream.size());
            yStream.writeTo(out);
            cbStream.writeTo(out);
            crStream.writeTo(out);
        }
    }

    @Override
    public void decompress(File input, File output) throws IOException {
        try (DataInputStream in = new DataInputStream(new FileInputStream(input))) {
            int width = in.readInt();
            int height = in.readInt();
            int ySize = in.readInt();
            int cbSize = in.readInt();
            int crSize = in.readInt();

            byte[] yBytes = in.readNBytes(ySize);
            byte[] cbBytes = in.readNBytes(cbSize);
            byte[] crBytes = in.readNBytes(crSize);

            double[][] y = decompressChannel(new ByteArrayInputStream(yBytes), scaleQuantTable(BASE_Q_Y), height, width);
            double[][] cb420 = decompressChannel(new ByteArrayInputStream(cbBytes), scaleQuantTable(BASE_Q_C), height / 2, width / 2);
            double[][] cr420 = decompressChannel(new ByteArrayInputStream(crBytes), scaleQuantTable(BASE_Q_C), height / 2, width / 2);

            double[][] cb = upsample420(cb420, width, height);
            double[][] cr = upsample420(cr420, width, height);

            BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            for (int i = 0; i < height; i++) {
                for (int j = 0; j < width; j++) {
                    double Y = y[i][j] + 128;
                    double Cb = cb[i][j];
                    double Cr = cr[i][j];
                    int r = clamp((int) (Y + 1.402 * Cr));
                    int g = clamp((int) (Y - 0.344136 * Cb - 0.714136 * Cr));
                    int b = clamp((int) (Y + 1.772 * Cb));
                    image.setRGB(j, i, new Color(r, g, b).getRGB());
                }
            }

            ImageIO.write(image, "jpg", output);
        }
    }

    private void compressChannel(double[][] channel, int[][] qTable, OutputStream out) throws IOException {
        List<Short> rleData = new ArrayList<>();
        int prevDC = 0;

        for (int y = 0; y < channel.length; y += BLOCK_SIZE) {
            for (int x = 0; x < channel[0].length; x += BLOCK_SIZE) {
                double[][] block = new double[BLOCK_SIZE][BLOCK_SIZE];
                for (int i = 0; i < BLOCK_SIZE; i++) {
                    for (int j = 0; j < BLOCK_SIZE; j++) {
                        int yy = y + i, xx = x + j;
                        block[i][j] = (yy < channel.length && xx < channel[0].length) ? channel[yy][xx] : 0;
                    }
                }
                int[][] quant = quantize(applyDCT(block), qTable);
                int[] zigzag = zigzagScan(quant);
                int dc = zigzag[0];
                rleData.add((short) (dc - prevDC));
                prevDC = dc;

                int zeroCount = 0;
                for (int i = 1; i < zigzag.length; i++) {
                    if (zigzag[i] == 0) {
                        zeroCount++;
                    } else {
                        rleData.add((short) zeroCount);
                        rleData.add((short) zigzag[i]);
                        zeroCount = 0;
                    }
                }
                rleData.add((short) 0xFFFF); // Block end
            }
        }

        new HuffmanCodec().compressRLE(rleData, out);
    }

    private double[][] decompressChannel(InputStream in, int[][] qTable, int height, int width) throws IOException {
        List<Short> rle = new HuffmanCodec().decompressRLE(in);
        double[][] channel = new double[height][width];
        int xBlocks = (int) Math.ceil(width / 8.0);
        int yBlocks = (int) Math.ceil(height / 8.0);

        int index = 0;
        int prevDC = 0;

        for (int by = 0; by < yBlocks; by++) {
            for (int bx = 0; bx < xBlocks; bx++) {
                if (index >= rle.size()) break;
                int[] zigzag = new int[64];
                int dcDiff = rle.get(index++);
                zigzag[0] = prevDC + dcDiff;
                prevDC = zigzag[0];

                int zi = 1;
                while (index < rle.size()) {
                    short val = rle.get(index++);
                    if (val == (short) 0xFFFF) break; // Block end
                    short coef = rle.get(index++);
                    for (int z = 0; z < val && zi < 64; z++) zigzag[zi++] = 0;
                    if (zi < 64) zigzag[zi++] = coef;
                }

                double[][] block = applyIDCT(dequantize(inverseZigzag(zigzag), qTable));
                for (int i = 0; i < BLOCK_SIZE; i++) {
                    for (int j = 0; j < BLOCK_SIZE; j++) {
                        int yy = by * BLOCK_SIZE + i;
                        int xx = bx * BLOCK_SIZE + j;
                        if (yy < height && xx < width)
                            channel[yy][xx] = block[i][j];
                    }
                }
            }
        }
        return channel;
    }

    private double[][] downsample(double[][] input) {
        int height = input.length / 2;
        int width = input[0].length / 2;
        double[][] output = new double[height][width];
        for (int i = 0; i < height; i++)
            for (int j = 0; j < width; j++)
                output[i][j] = (input[2 * i][2 * j] + input[2 * i + 1][2 * j] +
                        input[2 * i][2 * j + 1] + input[2 * i + 1][2 * j + 1]) / 4.0;
        return output;
    }

    private double[][] upsample420(double[][] input, int targetWidth, int targetHeight) {
        double[][] result = new double[targetHeight][targetWidth];
        for (int i = 0; i < targetHeight; i++) {
            for (int j = 0; j < targetWidth; j++) {
                result[i][j] = input[i / 2][j / 2];
            }
        }
        return result;
    }

    private double[][] applyDCT(double[][] block) {
        double[][] result = new double[BLOCK_SIZE][BLOCK_SIZE];
        for (int u = 0; u < BLOCK_SIZE; u++) {
            for (int v = 0; v < BLOCK_SIZE; v++) {
                double sum = 0.0;
                for (int x = 0; x < BLOCK_SIZE; x++) {
                    for (int y = 0; y < BLOCK_SIZE; y++) {
                        sum += block[x][y] *
                                Math.cos((2 * x + 1) * u * Math.PI / 16) *
                                Math.cos((2 * y + 1) * v * Math.PI / 16);
                    }
                }
                double cu = (u == 0) ? 1 / Math.sqrt(2) : 1;
                double cv = (v == 0) ? 1 / Math.sqrt(2) : 1;
                result[u][v] = 0.25 * cu * cv * sum;
            }
        }
        return result;
    }

    private double[][] applyIDCT(double[][] block) {
        double[][] result = new double[BLOCK_SIZE][BLOCK_SIZE];
        for (int x = 0; x < BLOCK_SIZE; x++) {
            for (int y = 0; y < BLOCK_SIZE; y++) {
                double sum = 0.0;
                for (int u = 0; u < BLOCK_SIZE; u++) {
                    for (int v = 0; v < BLOCK_SIZE; v++) {
                        double cu = (u == 0) ? 1 / Math.sqrt(2) : 1;
                        double cv = (v == 0) ? 1 / Math.sqrt(2) : 1;
                        sum += cu * cv * block[u][v] *
                                Math.cos((2 * x + 1) * u * Math.PI / 16) *
                                Math.cos((2 * y + 1) * v * Math.PI / 16);
                    }
                }
                result[x][y] = 0.25 * sum;
            }
        }
        return result;
    }

    private int[][] quantize(double[][] block, int[][] table) {
        int[][] result = new int[BLOCK_SIZE][BLOCK_SIZE];
        for (int i = 0; i < BLOCK_SIZE; i++)
            for (int j = 0; j < BLOCK_SIZE; j++)
                result[i][j] = (int) Math.round(block[i][j] / table[i][j]);
        return result;
    }

    private double[][] dequantize(int[][] block, int[][] table) {
        double[][] result = new double[BLOCK_SIZE][BLOCK_SIZE];
        for (int i = 0; i < BLOCK_SIZE; i++)
            for (int j = 0; j < BLOCK_SIZE; j++)
                result[i][j] = block[i][j] * table[i][j];
        return result;
    }

    private int[] zigzagScan(int[][] block) {
        int[] result = new int[BLOCK_SIZE * BLOCK_SIZE];
        int index = 0;
        for (int i = 0; i < 2 * BLOCK_SIZE - 1; i++) {
            int x = (i < BLOCK_SIZE) ? 0 : i - BLOCK_SIZE + 1;
            int y = (i < BLOCK_SIZE) ? i : BLOCK_SIZE - 1;
            while (x < BLOCK_SIZE && y >= 0) {
                result[index++] = block[x][y];
                x++;
                y--;
            }
        }
        return result;
    }

    private int[][] inverseZigzag(int[] arr) {
        int[][] block = new int[BLOCK_SIZE][BLOCK_SIZE];
        int index = 0;
        for (int i = 0; i < 2 * BLOCK_SIZE - 1; i++) {
            int x = (i < BLOCK_SIZE) ? 0 : i - BLOCK_SIZE + 1;
            int y = (i < BLOCK_SIZE) ? i : BLOCK_SIZE - 1;
            while (x < BLOCK_SIZE && y >= 0) {
                block[x][y] = arr[index++];
                x++;
                y--;
            }
        }
        return block;
    }

    private int clamp(int val) {
        return Math.max(0, Math.min(255, val));
    }
}

