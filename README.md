I developed a robust Java-based Quiz Application focused on Java and Data Structures & Algorithms (DSA) topics. The application features a timed quiz system, where users must answer randomized multiple-choice questions within a strict time limit. To ensure accurate scoring, I implemented multithreading to detect timeouts during input, automatically submitting or terminating the quiz if the user fails to answer in time.

The app supports a retry mechanism, allowing users one additional attempt with a fresh timer and reshuffled questions. It accurately tracks the best score and shortest time, providing a fair evaluation of performance across attempts. Quiz results are persistently logged into a history file, recording the user's name, score, and time taken.

Key features include real-time input timeout handling, score validation, structured file I/O for loading questions and saving results, and thorough input validation for edge cases. The project emphasizes modular coding and clean architectural practices, making it a practical tool for reinforcing Java and DSA knowledge under time pressure.

Tools and Technologies Used:

1. Java (Core Java) – For all application logic including multithreading, file I/O, and control structures.
2. Java Collections Framework – Used List, ArrayList, and Collections.shuffle() for managing and randomizing questions.
3. Multithreading – To implement time-bound input using separate threads and Thread.join(timeout).
4. File I/O (java.io) – To load quiz questions from a file and save user scores to a history file.
5. Scanner – For reading user input from the console.
6. Object-Oriented Programming (OOP) – Encapsulated quiz data using a Question class.
7. Command-Line Interface (CLI) – The app is purely backend-based, with no graphical user interface (GUI).
