package gui;

import compression.*;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

import java.awt.BorderLayout;

public class CompressionApp extends JFrame {

    private JComboBox<String> algorithmBox;
    private final JTextArea logArea = new JTextArea(10, 40);
    private File selectedInputFile;
    private File generatedOutputFile;
    private Compressor currentCompressor;
    private final JTextArea inputArea = new JTextArea(5, 40);
    private final JTextArea outputArea = new JTextArea(5, 40);

    public CompressionApp() {
        super("Compression App");

        algorithmBox = new JComboBox<>(new String[]{"Huffman", "LZW", "JPEG"});
        algorithmBox.addActionListener(e -> {
            updateCompressor();
            showSupportedExtensions();
        });
        updateCompressor();

        JButton selectInputButton = new JButton("Select Input File");
        selectInputButton.addActionListener(this::onSelectInput);

        JButton compressButton = new JButton("Compress");
        compressButton.addActionListener(this::compress);

        JButton decompressButton = new JButton("Decompress");
        decompressButton.addActionListener(this::decompress);

        JButton clearButton = new JButton("Clear");
        clearButton.addActionListener(e -> {
            inputArea.setText("");
            outputArea.setText("");
            selectedInputFile = null;
            generatedOutputFile = null;
            log("🔄 State reset.");
        });


        JPanel controls = new JPanel(new GridLayout(0, 1));
        controls.add(algorithmBox);
        controls.add(selectInputButton);
        controls.add(compressButton);
        controls.add(decompressButton);
        controls.add(clearButton);

        inputArea.setBorder(BorderFactory.createTitledBorder("Input Text (optional)"));
        outputArea.setBorder(BorderFactory.createTitledBorder("Output Preview"));
        inputArea.setLineWrap(true);
        outputArea.setLineWrap(true);
        outputArea.setEditable(false);

        JPanel ioPanel = new JPanel(new GridLayout(2, 1));
        ioPanel.add(new JScrollPane(inputArea));
        ioPanel.add(new JScrollPane(outputArea));

        logArea.setEditable(false);
        JScrollPane logScroll = new JScrollPane(logArea);
        logScroll.setBorder(BorderFactory.createTitledBorder("Log"));

        setLayout(new BorderLayout());
        add(controls, BorderLayout.WEST);
        add(ioPanel, BorderLayout.CENTER);
        add(logScroll, BorderLayout.SOUTH);

        setDefaultCloseOperation(EXIT_ON_CLOSE);
        pack();
        setLocationRelativeTo(null);
        setVisible(true);

    }

    private void updateCompressor() {
        String selected = (String) algorithmBox.getSelectedItem();
        switch (selected) {
            case "Huffman" -> currentCompressor = new HuffmanEncoder();
            case "LZW" -> currentCompressor = new LZWEncoder();
            case "JPEG" -> currentCompressor = new JPEGCompressor();
        }
    }

    private void showSupportedExtensions() {
        String algo = (String) algorithmBox.getSelectedItem();
        String message = switch (algo) {
            case "JPEG" -> "JPEG supports input: .jpg, .jpeg, .png";
            default -> algo + " supports: .txt, .csv";
        };
        JOptionPane.showMessageDialog(this, message, "Supported Formats", JOptionPane.INFORMATION_MESSAGE);
    }

    private void onSelectInput(ActionEvent e) {
        JFileChooser chooser = new JFileChooser();
        String algo = (String) algorithmBox.getSelectedItem();

        if ("JPEG".equals(algo)) {
            chooser.setFileFilter(new FileNameExtensionFilter("Image files", "jpg", "jpeg", "png", "jpegcomp"));
        } else {
            chooser.setFileFilter(new FileNameExtensionFilter("Text files", "txt", "csv"));
        }

        JTextField searchField = new JTextField();
        chooser.setAccessory(searchField);
        searchField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { updateFilter(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { updateFilter(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) {}

            private void updateFilter() {
                File guess = new File(searchField.getText());
                chooser.setSelectedFile(guess);
            }
        });

        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            selectedInputFile = chooser.getSelectedFile();
            log("📥 Selected file: " + selectedInputFile.getName());
        }
    }

    private void compress(ActionEvent e) {
        try {
            File inputFile = selectedInputFile;
            if (inputArea.getText().isBlank() && inputFile == null) {
                showError("Please enter text or select a file.");
                return;
            }

            if (!inputArea.getText().isBlank()) {
                inputFile = File.createTempFile("manual_input", ".txt");
                try (BufferedWriter writer = new BufferedWriter(new FileWriter(inputFile))) {
                    writer.write(inputArea.getText());
                }
            }

            String baseName = inputFile.getName().replaceAll("\\.[^.]+$", "");
            String extension = currentCompressor instanceof JPEGCompressor ? ".jpegcomp" : ".out";
            File output = new File(inputFile.getParent(), "compressed_" + baseName + extension);

            // 🔽 ВСТАВ ЦЕ СЮДИ: ВИМІРЮВАННЯ ЧАСУ
            long startTime = System.currentTimeMillis();
            currentCompressor.compress(inputFile, output);
            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;
            log("⏱ Compression time: " + duration + " ms");

            generatedOutputFile = output;
            selectedInputFile = output;

            log("✅ Compression successful: " + output.getName());
            showContentPreview(output);

            int confirm = JOptionPane.showConfirmDialog(this,
                    "Compression complete. Save result to your PC?",
                    "Save?", JOptionPane.YES_NO_OPTION);

            if (confirm == JOptionPane.YES_OPTION) {
                downloadOutput();
            }

            if (inputFile.length() > 5 * 1024 * 1024 && currentCompressor instanceof JPEGCompressor) {
                showError("Max image size is 5MB.");
                return;
            }

            if (inputFile.length() > 1 * 1024 * 1024 && !(currentCompressor instanceof JPEGCompressor)) {
                showError("Max text file size is 1MB.");
                return;
            }

        } catch (Exception ex) {
            showError("❌ Compression failed: " + ex.getMessage());
        }
    }


    private void decompress(ActionEvent e) {
        try {
            if (selectedInputFile == null && generatedOutputFile == null) {
                showError("No input for decompression.");
                return;
            }

            File fileToUse = generatedOutputFile != null ? generatedOutputFile : selectedInputFile;
            String baseName = fileToUse.getName().replaceAll("\\.[^.]+$", "");

            File output;
            if (currentCompressor instanceof JPEGCompressor) {
                String[] options = {".jpg", ".png"};
                int choice = JOptionPane.showOptionDialog(this,
                        "In which format would you like to save the decompressed image?",
                        "Choose Format",
                        JOptionPane.DEFAULT_OPTION,
                        JOptionPane.QUESTION_MESSAGE,
                        null,
                        options,
                        options[0]);

                String extension = choice == 1 ? ".png" : ".jpg";
                output = new File(fileToUse.getParent(), "decompressed_" + baseName + extension);
            } else {
                output = new File(fileToUse.getParent(), "decompressed_" + baseName + ".txt");
            }

            currentCompressor.decompress(fileToUse, output);
            generatedOutputFile = output;

            log("✅ Decompression successful.");
            showContentPreview(output);

            JFileChooser chooser = new JFileChooser();
            chooser.setDialogTitle("Save Decompressed File");
            chooser.setSelectedFile(new File(output.getName()));

            if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
                Files.copy(output.toPath(), chooser.getSelectedFile().toPath(), StandardCopyOption.REPLACE_EXISTING);
                JOptionPane.showMessageDialog(this, "File saved successfully.", "Saved", JOptionPane.INFORMATION_MESSAGE);
                generatedOutputFile = null;
            }

        } catch (Exception ex) {
            showError("❌ Decompression failed: " + ex.getMessage());
        }
    }

    private void downloadOutput() {
        if (generatedOutputFile == null || !generatedOutputFile.exists()) {
            showError("Nothing to download");
            return;
        }

        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Save Output File");

        String suggestedName = generatedOutputFile.getName();
        if (currentCompressor instanceof JPEGCompressor) {
            if (suggestedName.endsWith(".png")) {
                chooser.setSelectedFile(new File("result_image.png"));
            } else {
                chooser.setSelectedFile(new File("result_image.jpegcomp"));
            }
        } else {
            chooser.setSelectedFile(new File("result_" + suggestedName));
        }

        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                Files.copy(generatedOutputFile.toPath(), chooser.getSelectedFile().toPath(), StandardCopyOption.REPLACE_EXISTING);
                JOptionPane.showMessageDialog(this, "File saved successfully.", "Saved", JOptionPane.INFORMATION_MESSAGE);
                generatedOutputFile = null;
            } catch (IOException ex) {
                showError("Failed to save file: " + ex.getMessage());
            }
        }
    }


    private void showContentPreview(File file) {
        String name = file.getName().toLowerCase();
        if (name.endsWith(".jpegcomp") || name.endsWith(".png")) {
            outputArea.setText("[Binary image content]");
            return;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
            outputArea.setText(sb.toString());
        } catch (IOException e) {
            outputArea.setText("Preview unavailable");
        }
    }

    private void showError(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Error", JOptionPane.ERROR_MESSAGE);
    }

    private void log(String msg) {
        logArea.append(msg + "\n");
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(CompressionApp::new);
    }

}


