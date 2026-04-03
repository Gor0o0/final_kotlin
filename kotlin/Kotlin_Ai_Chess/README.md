# Kotlin AI Chess

This project aims to develop a 3D chess game using Kotlin.

## Project Structure

The project is structured into multiple modules to promote a clean architecture and separation of concerns:

-   **`core`**: Contains the core game logic, including chess rules, board state management, and AI algorithms. This module is platform-agnostic.
    -   `domain`: Defines the core business entities, use cases, and interfaces.
    -   `data`: Handles data sources and repositories for the core logic.
-   **`desktop`**: Implements the desktop-specific presentation layer and any desktop-specific data handling (e.g., UI rendering, input).
    -   `presentation`: Manages the user interface and interaction logic.
    -   `data`: Handles local data storage or platform-specific data sources.

## Getting Started

Further instructions on building and running the project will be added here.
