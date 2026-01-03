package compression;

import java.io.*;
import java.util.*;

public class HuffmanEncoder implements Compressor {

    private static class Node implements Comparable<Node> {
        char ch;
        int freq;
        Node left, right;

        Node(char ch, int freq) {
            this.ch = ch;
            this.freq = freq;
        }

        Node(Node left, Node right) {
            this.ch = 0;
            this.freq = left.freq + right.freq;
            this.left = left;
            this.right = right;
        }

        @Override
        public int compareTo(Node o) {
            return Integer.compare(this.freq, o.freq);
        }

        boolean isLeaf() {
            return left == null && right == null;
        }
    }

    private Map<Character, String> codeMap = new HashMap<>();

    private void buildCodeMap(Node root, String code) {
        if (root.isLeaf()) {
            codeMap.put(root.ch, code);
            return;
        }
        buildCodeMap(root.left, code + "0");
        buildCodeMap(root.right, code + "1");
    }

    @Override
    public void compress(File input, File output) throws IOException {
        StringBuilder text = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader(input))) {
            String line;
            while ((line = reader.readLine()) != null) {
                text.append(line).append("\n");
            }
        }

        Map<Character, Integer> freqMap = new HashMap<>();
        for (char c : text.toString().toCharArray()) {
            freqMap.put(c, freqMap.getOrDefault(c, 0) + 1);
        }

        PriorityQueue<Node> pq = new PriorityQueue<>();
        for (Map.Entry<Character, Integer> entry : freqMap.entrySet()) {
            pq.add(new Node(entry.getKey(), entry.getValue()));
        }

        while (pq.size() > 1) {
            Node left = pq.poll();
            Node right = pq.poll();
            pq.add(new Node(left, right));
        }

        Node root = pq.poll();
        buildCodeMap(root, "");

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(output))) {
            // Зберігаємо кодову таблицю
            writer.write("TABLE\n");
            for (Map.Entry<Character, String> entry : codeMap.entrySet()) {
                writer.write((int) entry.getKey().charValue() + ":" + entry.getValue() + "\n");
            }
            writer.write("DATA\n");

            // Записуємо стиснені дані
            for (char c : text.toString().toCharArray()) {
                writer.write(codeMap.get(c));
            }
        }
    }

    @Override
    public void decompress(File input, File output) throws IOException {
        Map<String, Character> reverseMap = new HashMap<>();
        StringBuilder encodedText = new StringBuilder();

        try (BufferedReader reader = new BufferedReader(new FileReader(input))) {
            String line;
            boolean readingData = false;
            while ((line = reader.readLine()) != null) {
                if (line.equals("TABLE")) continue;
                if (line.equals("DATA")) {
                    readingData = true;
                    continue;
                }
                if (!readingData) {
                    String[] parts = line.split(":");
                    if (parts.length == 2) {
                        char ch = (char) Integer.parseInt(parts[0]);
                        reverseMap.put(parts[1], ch);
                    }
                } else {
                    encodedText.append(line);
                }
            }
        }

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(output))) {
            StringBuilder currentCode = new StringBuilder();
            for (char bit : encodedText.toString().toCharArray()) {
                currentCode.append(bit);
                if (reverseMap.containsKey(currentCode.toString())) {
                    writer.write(reverseMap.get(currentCode.toString()));
                    currentCode.setLength(0);
                }
            }
        }
    }
}
