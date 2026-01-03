# Data Compression Suite - Desktop Application

## 📝 Overview
**Data Compression Suite** is a high-performance Java desktop application designed for efficient data encoding and decoding. The project focuses on the manual implementation of fundamental compression algorithms, providing a unified tool to process both text and image data. 

The primary goal of this project was to explore the mathematical foundations of data redundancy and implement low-level data processing without relying on third-party compression libraries.

## 🛠 Key Features & Technical Challenges
**Manual Algorithm Implementation:** Unlike standard applications, all logic for Huffman, LZW, and JPEG (DCT/Quantization) was **coded from scratch**, demonstrating deep knowledge of bitwise operations and data structures.
**Hybrid Data Processing:** The application handles lossless text compression and lossy image compression within a single modular environment
**Binary Stream Management:** Custom logic for handling bit-level writing and reading, essential for prefix-code based algorithms
**Desktop UX:** A clean, responsive GUI built with Java Swing, featuring real-time logging of compression ratios and execution status

## ⚙️ Core Algorithms
### 1. Lossless Compression (Textual Data)
**Huffman Coding:** Implements a frequency-based prefix binary tree for optimal entropy-based encoding.
**LZW (Lempel-Ziv-Welch):** A dictionary-based adaptive algorithm that optimizes recurring patterns without prior statistical analysis.

### 2. Lossy Compression (Digital Images)
* **JPEG-based Compression:** A multi-stage pipeline including:
 Color space conversion (RGB to YCbCr) and 4:2:0 Downsampling.
 Discrete Cosine Transform (DCT) on 8x8 pixel blocks.
 Quantization and Zigzag scanning for high-frequency data reduction.
 RLE and Huffman coding for final bitstream generation.

## 🛠 Tech Stack
* **Language:** Java 17
**Framework:** Java Swing (Desktop GUI) 
**Build Tool:** Maven 
**Architecture:** Modular DAO-inspired design with a unified `Compressor` interface.

## 🚀 Getting Started
1. **Clone the repository.**
2. **Build with Maven:** `mvn clean install`
3. *Launch:** Run the `CompressionApp` class
   Supported text formats: `.txt`, `.csv`
   Supported image formats: `.jpg`, `.jpeg`, `.png`.

## 🎓 Context
Developed as a technical research project during studies at the **National University of Kyiv-Mohyla Academy (NaUKMA)**
