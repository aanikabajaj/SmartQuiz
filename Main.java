import java.io.*;
import java.util.*;
import java.util.concurrent.*;

public class Main {
    private static final String QUESTIONS_FILE = "questions.txt";
    private static final String HISTORY_FILE = "history.txt";
    private static final String USERS_FILE = "users.txt";
    private static final String ATTEMPTS_FILE = "attempts.txt";
    private static final String ADMIN_PASSWORD = "admin123";
    private static Scanner scanner = new Scanner(System.in);

    public static void main(String[] args) {
        System.out.println("Welcome to the Java + DSA Quiz!");

        while (true) {
            System.out.println("\nMain Menu:");
            System.out.println("1. User Login");
            System.out.println("2. Admin Mode");
            System.out.println("3. View Leaderboard");
            System.out.println("4. Exit");
            System.out.print("Choose an option: ");

            int choice = getIntInput(1, 4);
            
            switch (choice) {
                case 1:
                    userLogin();
                    break;
                case 2:
                    adminMode();
                    break;
                case 3:
                    showLeaderboard();
                    break;
                case 4:
                    System.out.println("Thank you for using the quiz app!");
                    scanner.close();
                    return;
            }
        }
    }

    private static boolean registerNewUser(String username) {
        try {
            File file = new File(USERS_FILE);
            if (!file.exists()) {
                file.createNewFile();
                return true;
            }

            BufferedReader br = new BufferedReader(new FileReader(USERS_FILE));
            String line;
            while ((line = br.readLine()) != null) {
                if (line.equals(username)) {
                    br.close();
                    return false;
                }
            }
            br.close();

            FileWriter fw = new FileWriter(USERS_FILE, true);
            fw.write(username + "\n");
            fw.close();
            return true;
        } catch (IOException e) {
            System.out.println("Error accessing user database: " + e.getMessage());
            return false;
        }
    }

    private static void recordAttempt(String username) {
        try {
            FileWriter fw = new FileWriter(ATTEMPTS_FILE, true);
            fw.write(username + "\n");
            fw.close();
        } catch (IOException e) {
            System.out.println("Error recording attempt: " + e.getMessage());
        }
    }

    private static void userLogin() {
        System.out.println("\nUser Login:");
        System.out.print("Enter username: ");
        String username = scanner.nextLine();

        if (hasUserAttempted(username)) {
            System.out.println("You have already attempted the quiz. Only one attempt allowed per user.");
            return;
        }

        if (registerNewUser(username)) {
            System.out.println("New user registered!");
        }

        startQuiz(username);
    }

    private static boolean hasUserAttempted(String username) {
        try {
            File file = new File(ATTEMPTS_FILE);
            if (!file.exists()) {
                return false;
            }

            BufferedReader br = new BufferedReader(new FileReader(ATTEMPTS_FILE));
            String line;
            while ((line = br.readLine()) != null) {
                if (line.equals(username)) {
                    br.close();
                    return true;
                }
            }
            br.close();
            return false;
        } catch (IOException e) {
            System.out.println("Error checking attempts: " + e.getMessage());
            return false;
        }
    }

    private static void startQuiz(String username) {
        try {
            List<Question> questions = loadQuestions(QUESTIONS_FILE);

            if (questions.isEmpty()) {
                System.out.println("No questions found.");
                return;
            }

            System.out.println("\nYou have 1 minute to complete the quiz!");
            int score = 0;
            long startTime = System.currentTimeMillis();
            final long quizDuration = 60 * 1000; // 1 minute timer
            boolean timeUp = false;

            Collections.shuffle(questions);

            for (int i = 0; i < questions.size() && !timeUp; i++) {
                Question q = questions.get(i);
                long remainingTime = quizDuration - (System.currentTimeMillis() - startTime);
                
                if (remainingTime <= 0) {
                    System.out.println("\n⏰ Time's up! The quiz has ended.");
                    timeUp = true;
                    break;
                }

                System.out.println("\nQuestion " + (i + 1) + " of " + questions.size());
                System.out.println(q.getQuestionText());
                for (String option : q.getOptions()) {
                    System.out.println(option);
                }

                System.out.print("Your answer (A/B/C/D): ");
                String answer = readAnswerWithTimeout(remainingTime);

                if (answer == null) {
                    System.out.println("\n⏰ Time's up while waiting for your answer!");
                    timeUp = true;
                    break;
                }

                answer = answer.trim().toUpperCase();
                if (answer.length() == 1 && answer.charAt(0) == q.getCorrectOption()) {
                    System.out.println();
                    score++;
                } 

                remainingTime = quizDuration - (System.currentTimeMillis() - startTime);
                System.out.printf("Time remaining: %.1f seconds%n", remainingTime / 1000.0);
            }

            long endTime = System.currentTimeMillis();
            long timeTakenSec = (endTime - startTime) / 1000;

            System.out.println("\nQuiz finished!");
            System.out.println("Score: " + score + "/" + questions.size());
            System.out.println("Time taken: " + timeTakenSec + " seconds");

            saveResult(username, score, questions.size(), timeTakenSec);
            recordAttempt(username);
        } catch (Exception e) {
            System.out.println("⏰ Time's up while waiting for your answer!");
        } finally {
            System.out.println("\nPress Enter to return to main menu...");
            try {
                System.in.read();
                // Clear the input buffer
                while (System.in.available() > 0) {
                    System.in.read();
                }
            } catch (IOException e) {
                // Ignore
            }
        }
    }

    private static String readAnswerWithTimeout(long timeoutMillis) {
        if (timeoutMillis <= 0) {
            return null;
        }

        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<String> future = executor.submit(() -> {
            try {
                if (scanner.hasNextLine()) {
                    return scanner.nextLine();
                }
            } catch (IllegalStateException e) {
                // Handle scanner closed state
            }
            return null;
        });

        try {
            return future.get(timeoutMillis, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            future.cancel(true);
            return null;
        } catch (Exception e) {
            return null;
        } finally {
            executor.shutdownNow();
            // Clear any remaining input in the buffer
            if (scanner.hasNextLine()) {
                scanner.nextLine();
            }
        }
    }

    private static List<Question> loadQuestions(String filename) {
        List<Question> questions = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(filename))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(";");
                if (parts.length == 6) {
                    String questionText = parts[0];
                    String[] options = { parts[1], parts[2], parts[3], parts[4] };
                    char correctOption = parts[5].charAt(0);
                    questions.add(new Question(questionText, options, correctOption));
                }
            }
        } catch (IOException e) {
            System.out.println("Error loading questions: " + e.getMessage());
        }
        return questions;
    }

    private static void saveResult(String username, int score, int total, long timeTaken) {
        try {
            FileWriter fw = new FileWriter(HISTORY_FILE, true);
            fw.write(username + " scored " + score + "/" + total + " in " + timeTaken + " sec\n");
            fw.close();
        } catch (IOException e) {
            System.out.println("Error saving result: " + e.getMessage());
        }
    }

    private static void adminMode() {
        System.out.print("\nEnter admin password: ");
        String password = scanner.nextLine();

        if (!password.equals(ADMIN_PASSWORD)) {
            System.out.println("Invalid password!");
            return;
        }

        while (true) {
            System.out.println("\nAdmin Menu:");
            System.out.println("1. Add Question");
            System.out.println("2. View All Questions");
            System.out.println("3. View All Results");
            System.out.println("4. Back to Main Menu");
            System.out.print("Choose an option: ");

            int choice = getIntInput(1, 4);
            
            switch (choice) {
                case 1:
                    addQuestion();
                    break;
                case 2:
                    viewAllQuestions();
                    break;
                case 3:
                    viewAllResults();
                    break;
                case 4:
                    return;
            }
        }
    }

    private static void addQuestion() {
        try {
            System.out.println("\nAdd New Question:");
            System.out.print("Enter question text: ");
            String text = scanner.nextLine();

            System.out.print("Enter option A: ");
            String a = scanner.nextLine();
            System.out.print("Enter option B: ");
            String b = scanner.nextLine();
            System.out.print("Enter option C: ");
            String c = scanner.nextLine();
            System.out.print("Enter option D: ");
            String d = scanner.nextLine();

            System.out.print("Enter correct option (A/B/C/D): ");
            String correct = scanner.nextLine().toUpperCase();

            if (!Arrays.asList("A", "B", "C", "D").contains(correct)) {
                System.out.println("Invalid option! Must be A, B, C, or D");
                return;
            }

            FileWriter fw = new FileWriter(QUESTIONS_FILE, true);
            fw.write(text + ";" + a + ";" + b + ";" + c + ";" + d + ";" + correct + "\n");
            fw.close();

            System.out.println("Question added successfully!");
        } catch (IOException e) {
            System.out.println("Error adding question: " + e.getMessage());
        }
    }

    private static void viewAllQuestions() {
        List<Question> questions = loadQuestions(QUESTIONS_FILE);
        if (questions.isEmpty()) {
            System.out.println("No questions found.");
            return;
        }

        System.out.println("\nAll Questions:");
        for (int i = 0; i < questions.size(); i++) {
            Question q = questions.get(i);
            System.out.println("\nQ" + (i + 1) + ": " + q.getQuestionText());
            for (String option : q.getOptions()) {
                System.out.println(option);
            }
            System.out.println("Correct answer: " + q.getCorrectOption());
        }
    }

    private static void viewAllResults() {
        try {
            BufferedReader br = new BufferedReader(new FileReader(HISTORY_FILE));
            String line;
            System.out.println("\nAll Quiz Results:");
            while ((line = br.readLine()) != null) {
                System.out.println(line);
            }
            br.close();
        } catch (IOException e) {
            System.out.println("Error reading results: " + e.getMessage());
        }
    }

    private static void showLeaderboard() {
        try {
            BufferedReader br = new BufferedReader(new FileReader(HISTORY_FILE));
            List<String> results = new ArrayList<>();
            String line;
            
            while ((line = br.readLine()) != null) {
                results.add(line);
            }
            br.close();

            if (results.isEmpty()) {
                System.out.println("No quiz results yet.");
                return;
            }

            Collections.sort(results, (a, b) -> {
                int scoreA = Integer.parseInt(a.split(" scored ")[1].split("/")[0]);
                int scoreB = Integer.parseInt(b.split(" scored ")[1].split("/")[0]);
                return Integer.compare(scoreB, scoreA);
            });

            System.out.println("\nLeaderboard (Top 10):");
            int count = Math.min(10, results.size());
            for (int i = 0; i < count; i++) {
                System.out.println((i + 1) + ". " + results.get(i));
            }
        } catch (IOException e) {
            System.out.println("Error reading leaderboard: " + e.getMessage());
        }
    }

    private static int getIntInput(int min, int max) {
        while (true) {
            try {
                int input = Integer.parseInt(scanner.nextLine());
                if (input >= min && input <= max) {
                    return input;
                }
                System.out.print("Invalid input. Please enter a number between " + min + " and " + max + ": ");
            } catch (NumberFormatException e) {
                System.out.print("Invalid input. Please enter a number: ");
            }
        }
    }
}