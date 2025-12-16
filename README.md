# ♟️ Chess Game

A sophisticated chess application built in Java with an advanced AI engine, featuring a modern SWT-based GUI and comprehensive chess engine capabilities.

## Features

### 🎮 Game Features
- **Interactive Chess Board**: Full-featured chess board with drag-and-drop piece movement
- **Game Modes**: Human vs Human, Human vs AI, AI vs AI
- **Opening Book**: Extensive opening repertoire for strategic play
- **Move Validation**: Complete rule enforcement including castling, en passant, and promotion

### 🤖 AI Engine Features
- **Advanced Search Algorithms**: 
  - Alpha-beta pruning with iterative deepening
  - Quiescence search for tactical accuracy
  - Null move pruning for performance
  - Late move reduction (LMR)
  - Futility pruning for endgame optimization
- **Evaluation Functions**:
  - Material evaluation with piece-square tables
  - Pawn structure analysis
  - King safety evaluation
  - Piece mobility and coordination
  - Endgame-specific evaluations
- **Performance Optimizations**:
  - Transposition table with Zobrist hashing
  - Move ordering for better alpha-beta cuts
  - Parallel search capabilities
  - Memory management and cleanup
- **Search Engines**:
  - Standard search engine for regular play
  - Advanced search engine with enhanced features
  - Tournament search engine for competitive play
  - Parallel search engine for multi-core systems

### 🧪 Testing & Analysis
- **Performance Benchmarks**: Comprehensive performance testing suite
- **ELO Strength Testing**: AI strength evaluation and rating
- **Chess Puzzle Solving**: Tactical puzzle solving capabilities
- **Position Analysis**: Deep position evaluation and debugging
- **Move Analysis**: Detailed move evaluation and selection debugging

### 🔧 Technical Features
- **Cross-Platform**: Runs on Windows, macOS, and Linux
- **Native Compilation**: GraalVM native image support for fast startup
- **Memory Management**: Intelligent memory usage with cleanup mechanisms
- **Resource Management**: Efficient resource allocation and deallocation
- **Error Handling**: Robust error handling and recovery mechanisms

## Prerequisites

- **Java 21+**: Required for modern Java features
- **Gradle 8.0+**: Build system (wrapper included)
- **GraalVM** (optional): For native image compilation

## Installation

### Using Gradle (Recommended)

1. **Clone the repository**:
   ```bash
   git clone https://github.com/seerainer/ChessGame.git
   cd ChessGame
   ```

2. **Build the project**:
   ```bash
   ./gradlew build
   ```

3. **Run the application**:
   ```bash
   ./gradlew run
   ```

### Using Pre-built Distribution

1. **Build distribution**:
   ```bash
   ./gradlew distTar  # Creates tar archive
   # OR
   ./gradlew distZip  # Creates zip archive
   ```

2. **Extract and run**:
   ```bash
   # Extract the archive from build/distributions/
   # Run the executable from bin/ChessGame (Unix) or bin/ChessGame.bat (Windows)
   ```

### Native Image Compilation

For faster startup times, compile to native image:

```bash
./gradlew nativeCompile
```

The native executable will be created in `build/native/nativeCompile/`.

## Usage

### Starting the Game

Launch the application and you'll see the chess board with an intuitive interface:

- **Drag and drop** pieces to make moves
- **Menu bar** for game options and settings

### Game Controls

- **New Game**: Start a fresh game
- **Game Mode**: Switch between Human vs Human, Human vs AI, AI vs AI

### AI Configuration

The AI can be configured with various parameters:
- **Search depth**: How many moves ahead to calculate
- **Time limits**: Maximum thinking time per move
- **Opening book**: Enable/disable opening book usage
- **Evaluation weights**: Fine-tune positional evaluation

## Development

### Project Structure

```
src/
├── main/java/io/github/seerainer/chess/
│   ├── Main.java                 # Application entry point
│   ├── ChessGameUI.java         # Main user interface
│   ├── ChessBoard.java          # Board representation
│   ├── ChessAI.java             # AI coordinator
│   ├── ai/                      # AI engine components
│   │   ├── SearchAlgorithms.java
│   │   ├── PositionEvaluator.java
│   │   ├── TranspositionTable.java
│   │   ├── OpeningBook.java
│   │   └── ...
│   ├── config/                  # Configuration classes
│   └── utils/                   # Utility classes
└── test/java/io/github/seerainer/chess/test/
    ├── PerformanceBenchmarkTest.java
    ├── ELOStrengthTest.java
    ├── ChessPuzzleSolvingTest.java
    └── ...
```

### Running Tests

```bash
# Run all tests
./gradlew test

# Run specific test categories
./gradlew test --tests "*Performance*"
./gradlew test --tests "*ELO*"
./gradlew test --tests "*Puzzle*"
```

### Building for Distribution

```bash
# Create executable JAR
./gradlew jar

# Create application distribution
./gradlew distTar
./gradlew distZip

# Create native image
./gradlew nativeCompile
```

## Configuration

The application uses a configuration system with the following key areas:

- **AI Settings**: Search parameters, evaluation weights, time management
- **Memory Management**: Heap limits, garbage collection tuning
- **Performance**: Threading, parallel search configuration

## Performance

### Benchmarks

The chess engine includes comprehensive performance benchmarks:

- **Nodes per second**: Search speed measurement
- **Tactical puzzle solving**: Puzzle-solving accuracy and speed
- **Position evaluation**: Evaluation function performance
- **Memory usage**: Memory consumption analysis

### Optimization Tips

- **Increase heap size** for deeper searches: `-Xmx4g`
- **Enable parallel search** for multi-core systems
- **Use native compilation** for reduced startup time
- **Adjust search depth** based on hardware capabilities

## Contributing

1. **Fork the repository**
2. **Create a feature branch**: `git checkout -b feature/amazing-feature`
3. **Make your changes** and add tests
4. **Run the test suite**: `./gradlew test`
5. **Commit your changes**: `git commit -m 'Add amazing feature'`
6. **Push to the branch**: `git push origin feature/amazing-feature`
7. **Open a Pull Request**

### Code Style

- Follow Java naming conventions
- Use meaningful variable and method names
- Add JavaDoc comments for public APIs
- Include unit tests for new features
- Maintain consistent formatting

## License

This is free and unencumbered software released into the public domain.

## Acknowledgments

- **ChessLib**: Core chess library for move generation and validation
- **Eclipse SWT**: Cross-platform GUI toolkit
- **GraalVM**: Native image compilation support
- **JUnit**: Testing framework

## Technical Details

### Dependencies

- **Eclipse SWT 3.132.0**: GUI framework
- **ChessLib 1.3.4**: Chess engine library
- **JUnit Jupiter 6.0.1**: Testing framework
- **AssertJ 3.27.6**: Assertion library
- **JMH 1.37**: Performance benchmarking

### System Requirements

- **Memory**: Minimum 512MB RAM, recommended 2GB+
- **Storage**: 50MB for installation
- **Display**: 800x600 minimum resolution
- **OS**: Windows 10+, macOS 10.14+, Linux (GTK 3.0+)

## Version History

### **v0.1.1**
- Upgrade Java language version from 24 to 25
- Introduce performance tracking in ChessAI with total nodes searched and search time
- Implement adaptive search depth calculation based on position complexity
- Enhance iterative deepening with adaptive depth and better time management
- Add new performance statistics methods in ChessAI
- Clean up and optimize various AI components for better performance

### **v0.1.0**: Initial release with basic gameplay and AI
- Advanced search algorithms and evaluation functions
- Cross-platform support with native compilation
- Comprehensive testing suite

## Support

For questions, bug reports, or feature requests:
- **Issues**: [GitHub Issues](https://github.com/seerainer/ChessGame/issues)
- **Discussions**: [GitHub Discussions](https://github.com/seerainer/ChessGame/discussions)

---

*Built with ❤️ for chess enthusiasts*

