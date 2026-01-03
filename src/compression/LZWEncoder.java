package compression;

import java.io.*;
import java.util.*;

public class LZWEncoder implements Compressor {

    @Override
    public void compress(File input, File output) throws IOException {
        StringBuilder inputText = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader(input))) {
            String line;
            while ((line = reader.readLine()) != null) {
                inputText.append(line).append("\n");
            }
        }

        if (inputText.length() == 0) {
            throw new IOException("The file is empty or the text could not be read");
        }

        Map<String, Integer> dictionary = new HashMap<>();
        for (int i = 0; i < 256; i++) {
            dictionary.put("" + (char) i, i);
        }

        String w = "";
        List<Integer> compressed = new ArrayList<>();
        int dictSize = 256;

        for (char c : inputText.toString().toCharArray()) {
            String wc = w + c;
            if (dictionary.containsKey(wc)) {
                w = wc;
            } else {
                compressed.add(dictionary.get(w));
                dictionary.put(wc, dictSize++);
                w = "" + c;
            }
        }

        if (!w.isEmpty()) {
            compressed.add(dictionary.get(w));
        }

        try (DataOutputStream out = new DataOutputStream(new FileOutputStream(output))) {
            for (int code : compressed) {
                out.writeInt(code);
            }
        }
    }


    @Override
    public void decompress(File input, File output) throws IOException {
        List<Integer> compressed = new ArrayList<>();
        try (DataInputStream in = new DataInputStream(new FileInputStream(input))) {
            while (in.available() > 0) {
                compressed.add(in.readInt());
            }
        }

        if (compressed.isEmpty()) {
            throw new IOException("The compressed file is empty or corrupted");
        }

        Map<Integer, String> dictionary = new HashMap<>();
        for (int i = 0; i < 256; i++) {
            dictionary.put(i, "" + (char) i);
        }

        StringBuilder result = new StringBuilder();
        Iterator<Integer> it = compressed.iterator();
        Integer firstCode = it.next();

        if (firstCode == null) {
            throw new IOException("Unable to read the first element in the compressed file");
        }

        String w = "" + (char) firstCode.intValue();
        result.append(w);
        int dictSize = 256;

        while (it.hasNext()) {
            Integer k = it.next();
            if (k == null) continue;
            String entry;
            if (dictionary.containsKey(k)) {
                entry = dictionary.get(k);
            } else if (k == dictSize) {
                entry = w + w.charAt(0);
            } else {
                throw new IllegalArgumentException("Bad compressed k: " + k);
            }

            result.append(entry);
            dictionary.put(dictSize++, w + entry.charAt(0));
            w = entry;
        }

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(output))) {
            writer.write(result.toString());
        }
    }
}
