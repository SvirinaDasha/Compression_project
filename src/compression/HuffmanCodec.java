package compression;

import java.io.*;
import java.util.*;

public class HuffmanCodec {

    private static class Node implements Comparable<Node> {
        short value;
        int freq;
        Node left, right;

        Node(short value, int freq) {
            this.value = value;
            this.freq = freq;
        }

        Node(Node left, Node right) {
            this.left = left;
            this.right = right;
            this.freq = left.freq + right.freq;
        }

        boolean isLeaf() {
            return left == null && right == null;
        }

        @Override
        public int compareTo(Node o) {
            return Integer.compare(this.freq, o.freq);
        }
    }

    private void buildCodeMap(Node node, String code, Map<Short, String> map) {
        if (node.isLeaf()) {
            map.put(node.value, code);
        } else {
            buildCodeMap(node.left, code + "0", map);
            buildCodeMap(node.right, code + "1", map);
        }
    }

    public void compressRLE(List<Short> input, OutputStream out) throws IOException {
        Map<Short, Integer> freq = new HashMap<>();
        for (short val : input) {
            freq.put(val, freq.getOrDefault(val, 0) + 1);
        }

        PriorityQueue<Node> pq = new PriorityQueue<>();
        for (Map.Entry<Short, Integer> entry : freq.entrySet()) {
            pq.add(new Node(entry.getKey(), entry.getValue()));
        }

        while (pq.size() > 1) {
            Node left = pq.poll();
            Node right = pq.poll();
            pq.add(new Node(left, right));
        }

        Node root = pq.poll();
        Map<Short, String> codeMap = new HashMap<>();
        buildCodeMap(root, "", codeMap);

        // Зберігаємо дерево
        DataOutputStream dout = new DataOutputStream(out);
        writeTree(root, dout);

        // Зберігаємо кількість символів
        dout.writeInt(input.size());

        // Записуємо закодовані біти
        BitSet bits = new BitSet();
        int bitIndex = 0;
        for (short val : input) {
            String code = codeMap.get(val);
            for (char c : code.toCharArray()) {
                if (c == '1') bits.set(bitIndex);
                bitIndex++;
            }
        }

        byte[] bytes = bits.toByteArray();
        dout.writeInt(bytes.length);
        dout.write(bytes);
    }

    public List<Short> decompressRLE(InputStream in) throws IOException {
        DataInputStream din = new DataInputStream(in);
        Node root = readTree(din);
        int count = din.readInt();
        int bytesLength = din.readInt();
        byte[] bytes = new byte[bytesLength];
        din.readFully(bytes);

        BitSet bits = BitSet.valueOf(bytes);
        List<Short> output = new ArrayList<>();
        Node current = root;
        for (int i = 0, decoded = 0; decoded < count; i++) {
            current = bits.get(i) ? current.right : current.left;
            if (current.isLeaf()) {
                output.add(current.value);
                current = root;
                decoded++;
            }
        }

        return output;
    }

    private void writeTree(Node node, DataOutputStream out) throws IOException {
        if (node.isLeaf()) {
            out.writeBoolean(true);
            out.writeShort(node.value);
        } else {
            out.writeBoolean(false);
            writeTree(node.left, out);
            writeTree(node.right, out);
        }
    }

    private Node readTree(DataInputStream in) throws IOException {
        boolean isLeaf = in.readBoolean();
        if (isLeaf) {
            return new Node(in.readShort(), 0);
        } else {
            Node left = readTree(in);
            Node right = readTree(in);
            return new Node(left, right);
        }
    }
}
