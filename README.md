# Checkmates: A Java Chess Minigame

A multiplayer chess game developed as a group project for a university course. This application is built using Java Swing for the graphical user interface and is designed to integrate with a university-provided minigame server framework.

> ⚠️ **IMPORTANT: WORK IN PROGRESS & INCOMPLETE CODEBASE**
>
> Please be aware that this is an **unfinished group project**. The code in this repository represents only the client-side components developed by our group.
>
> The code **will not compile** on its own. This is because it relies on proprietary classes and a server architecture provided by the university, which are not included in this repository. The files are provided for demonstration of our work and code structure only.
>
> Furthermore, comprehensive documentation, unit testing, and systematic bug-testing are not yet complete. This is a work in progress, and the current state reflects its developmental stage.

## Project Overview

Checkmates is a classic chess game implemented as a client-server application. Players can join a game, play against each other in real-time, and communicate via an in-game chat. The game logic is handled by a custom-built engine that supports standard chess rules, including special moves like castling, en passant, and pawn promotion.

## Features

### Core Features (Implemented)
*   **Standard Chess Rules:** The engine correctly validates moves for all pieces, including pawns (initial two-square move, captures), rooks, knights, bishops, queens, and kings.
*   **Graphical User Interface (GUI):** A user-friendly interface built with Java Swing, featuring:
    *   An interactive, clickable chessboard.
    *   Visual highlighting for selected pieces and available legal moves.
    *   A display for move history.
    *   A real-time chat box for player communication.
    *   Dynamic board orientation (can be flipped).
*   **Client-Server Architecture:** The game is designed for multiplayer gameplay, with the client sending moves to a server for validation and broadcasting the updated game state.
*   **Pawn Promotion:** A modal dialog appears when a pawn reaches the final rank, allowing the player to choose their promotion piece (Queen, Rook, Bishop, or Knight).
*   **Visual Feedback:** The board provides immediate visual feedback for illegal moves made while in check by flashing a red border.

### Work in Progress / To-Do
*   **Resign Functionality:** Implementing a "Resign" button to gracefully end the game.
*   **Quit to Lobby:** A functional "Quit" button to return the player to the main server lobby.
*   **Timer Synchronisation:** Full implementation and display of server-synchronised timers for each player.
*   **Captured Pieces Display:** Finalising the UI components to display captured pieces for each player.
*   **Comprehensive Bug Testing:** A thorough process of identifying and fixing bugs in both the engine and GUI.
*   **Code Refactoring and Documentation:** Improving comments, Javadoc, and overall code organisation.

## Architecture

The project follows a decoupled architecture to separate concerns, primarily divided into three main packages:

1.  **Engine (`minigames.client.checkmates.engine`)**
    *   This package contains the "brains" of the game. It is completely independent of the GUI.
    *   `GameBoard.java` manages the overall game state, including the board layout (as a `ChessPiece[][]` array), turn management, move history, and game-over conditions. It processes moves based on Forsyth-Edwards Notation (FEN).
    *   `moveValidator.java` is a static utility class that contains all the complex chess rule logic, such as calculating legal moves, detecting checks, and validating special moves.
    *   `ChessPiece.java` is a data class representing a single piece on the board.

2.  **GUI (`minigames.client.checkmates.gui`)**
    *   This package handles all visual rendering and user interaction using Java Swing.
    *   `GameGui.java` is the main frame that assembles all UI components like the chat, move history, and timers.
    *   `ChessBoardPanel.java` is the core interactive component where the 8x8 grid, pieces, and highlights are drawn. It captures mouse clicks and translates them into game actions.
    *   `ChessBoardContainer.java` is a wrapper for the `ChessBoardPanel` that adds axis labels (A-H, 1-8) and areas for captured pieces.

3.  **Client (`minigames.client.checkmates`)**
    *   This package acts as the "glue" connecting the Engine and GUI to the network.
    *   `Checkmates.java` implements the `GameClient` interface from the university framework. It handles communication with the game server, sending player commands (moves, chat messages) and receiving game state updates.
    *   Interfaces like `GameStateListener` and `PromotionHandler` are used to ensure loose coupling between the engine and the GUI.

## Code Structure Highlights

*   **`Checkmates.java`**: The main client class. Initialises the game, manages server communication, and orchestrates updates between the engine and GUI.
*   **`gui/GameGui.java`**: Constructs the main application window and all its child components (chat, move history, etc.).
*   **`gui/ChessBoardPanel.java`**: Handles the rendering of and user interaction with the chessboard itself. All mouse events related to piece movement are processed here.
*   **`engine/GameBoard.java`**: The central model for the game state. It holds the piece positions and applies the game rules by calling `moveValidator`.
*   **`engine/moveValidator.java`**: A pure-logic static class containing the rules of chess. It determines all possible and legal moves from any given position.

## Authors

*   Ariel Halperin
*   Geoffrey Stewart-Richardson
*   Joshua Hahn
*   Curtis Martin

## Acknowledgements

*   Chess piece assets sourced from [Wikimedia Commons](https://commons.wikimedia.org/wiki/Category:PNG_chess_pieces/Standard_transparent).
