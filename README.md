# Data Compression Suite - Desktop Application

## 📝 Overview
**Data Compression Suite** is a high-performance Java desktop application designed for efficient data encoding and decoding. The project focuses on the manual implementation of fundamental compression algorithms, providing a unified tool to process both text and image data. 

[cite_start]The primary goal of this project was to explore the mathematical foundations of data redundancy and implement low-level data processing without relying on third-party compression libraries[cite: 77, 103].

## 🛠 Key Features & Technical Challenges
* [cite_start]**Manual Algorithm Implementation:** Unlike standard applications, all logic for Huffman, LZW, and JPEG (DCT/Quantization) was **coded from scratch**, demonstrating deep knowledge of bitwise operations and data structures.
* [cite_start]**Hybrid Data Processing:** The application handles lossless text compression and lossy image compression within a single modular environment[cite: 78, 450, 482].
* [cite_start]**Binary Stream Management:** Custom logic for handling bit-level writing and reading, essential for prefix-code based algorithms[cite: 522, 1638].
* [cite_start]**Desktop UX:** A clean, responsive GUI built with Java Swing, featuring real-time logging of compression ratios and execution status[cite: 80, 553, 554].

## ⚙️ Core Algorithms
### 1. Lossless Compression (Textual Data)
* [cite_start]**Huffman Coding:** Implements a frequency-based prefix binary tree for optimal entropy-based encoding[cite: 79, 263, 264].
* [cite_start]**LZW (Lempel-Ziv-Welch):** A dictionary-based adaptive algorithm that optimizes recurring patterns without prior statistical analysis[cite: 79, 314, 315].

### 2. Lossy Compression (Digital Images)
* **JPEG-based Compression:** A multi-stage pipeline including:
  * [cite_start]Color space conversion (RGB to YCbCr) and 4:2:0 Downsampling[cite: 380, 383, 409].
  * [cite_start]Discrete Cosine Transform (DCT) on 8x8 pixel blocks[cite: 385, 386, 411].
  * [cite_start]Quantization and Zigzag scanning for high-frequency data reduction[cite: 388, 390, 412].
  * [cite_start]RLE and Huffman coding for final bitstream generation[cite: 393, 395, 415].

## 🛠 Tech Stack
* **Language:** Java 17
* [cite_start]**Framework:** Java Swing (Desktop GUI) [cite: 480]
* [cite_start]**Build Tool:** Maven [cite: 54]
* [cite_start]**Architecture:** Modular DAO-inspired design with a unified `Compressor` interface[cite: 486, 505].

## 🚀 Getting Started
1. **Clone the repository.**
2. [cite_start]**Build with Maven:** `mvn clean install`[cite: 45].
3. [cite_start]**Launch:** Run the `CompressionApp` class[cite: 490].
   * [cite_start]Supported text formats: `.txt`, `.csv`[cite: 638, 639].
   * [cite_start]Supported image formats: `.jpg`, `.jpeg`, `.png`[cite: 640].

## 🎓 Context
[cite_start]Developed as a technical research project during studies at the **National University of Kyiv-Mohyla Academy (NaUKMA)**[cite: 2, 28].
