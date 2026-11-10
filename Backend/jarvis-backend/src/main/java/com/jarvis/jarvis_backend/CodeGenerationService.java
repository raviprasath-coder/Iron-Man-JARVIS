package com.jarvis.jarvis_backend;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;

@Service
public class CodeGenerationService {

    // Detection patterns
    private static final Pattern CODE_REQUEST_PATTERN = Pattern.compile(
            "(?:write|generate|create|build|make|give me|show me|draft)\\s+(?:a\\s+|an\\s+)?(?:(python|java|javascript|js|html|css|c|cpp|c\\+\\+|sql|bash|shell)\\s+)?(?:code|program|script|app|application|game|file)?\\s*(?:for|to|that\\s+can|implementing)?\\s+([\\w\\s\\-\\.\\+]+?)(?:\\s+(?:in|using)\\s+(python|java|javascript|js|html|css|c|cpp|c\\+\\+|sql))?(?:\\s+(?:and\\s+save(?:\\s+it)?\\s+(?:as|to|in)|into)\\s+([\\w\\-\\.\\/\\\\]+))?$",
            Pattern.CASE_INSENSITIVE);

    public static class CodeRequest {
        public String topic;
        public String language; // python, java, html, javascript, etc.
        public String targetFileName;
        public boolean explicitSave;

        public CodeRequest(String topic, String language, String targetFileName, boolean explicitSave) {
            this.topic = topic;
            this.language = language;
            this.targetFileName = targetFileName;
            this.explicitSave = explicitSave;
        }
    }

    public boolean isCodeRequest(String message) {
        if (message == null) return false;
        String lower = message.trim().toLowerCase();

        // Direct code starters
        if (lower.startsWith("code for") || lower.startsWith("code to") ||
            lower.startsWith("program for") || lower.startsWith("script for") ||
            lower.startsWith("app for") || lower.startsWith("game for")) {
            return true;
        }

        // Exclude simple file creation like "create file notes.txt with content ..."
        if (lower.contains("create file") && lower.contains("with content")) {
            return false;
        }

        boolean hasActionVerb = lower.contains("write") || lower.contains("generate") ||
                                lower.contains("create") || lower.contains("build") ||
                                lower.contains("make") || lower.contains("draft") ||
                                lower.contains("give me") || lower.contains("show me") ||
                                lower.contains("code");

        boolean hasCodeTarget = lower.contains("code") || lower.contains("program") ||
                                lower.contains("script") || lower.contains("app") ||
                                lower.contains("application") || lower.contains("game") ||
                                lower.contains("html") || lower.contains("css") ||
                                lower.contains("website") || lower.contains("webpage") ||
                                lower.contains("portfolio") || lower.contains("calculator") ||
                                lower.contains("snake") || lower.contains("tic tac toe") ||
                                lower.contains("fibonacci") || lower.contains("prime") ||
                                lower.contains("algorithm") || lower.contains("sort") ||
                                lower.contains("python") || lower.contains("java") ||
                                lower.contains("javascript");

        return hasActionVerb && hasCodeTarget;
    }

    public CodeRequest parseRequest(String message) {
        String trimmed = message.trim();
        String lower = trimmed.toLowerCase();

        String language = "python"; // Default
        String topic = "program";
        String targetFileName = null;
        boolean explicitSave = false;

        // Detect language
        if (lower.contains("java") && !lower.contains("javascript")) {
            language = "java";
        } else if (lower.contains("javascript") || lower.contains("js")) {
            language = "javascript";
        } else if (lower.contains("html") || lower.contains("website") || lower.contains("webpage") || lower.contains("portfolio")) {
            language = "html";
        } else if (lower.contains("c++") || lower.contains("cpp")) {
            language = "cpp";
        } else if (lower.contains("c language") || lower.contains("in c ")) {
            language = "c";
        } else if (lower.contains("python") || lower.contains("py")) {
            language = "python";
        }

        // Check for explicit save filename
        Matcher saveMatcher = Pattern.compile("(?:save(?:\\s+it)?\\s+(?:as|to|in)|into)\\s+([\\w\\-\\.\\/\\\\]+)", Pattern.CASE_INSENSITIVE).matcher(trimmed);
        if (saveMatcher.find()) {
            targetFileName = saveMatcher.group(1).trim();
            explicitSave = true;
        }

        // Clean topic
        String clean = trimmed
                .replaceAll("(?i)^(?:jarvis,?\\s*)?(?:please\\s+)?(?:can\\s+you\\s+)?", "")
                .replaceAll("(?i)^(?:write|generate|create|build|make|give me|show me)\\s+(?:a\\s+|an\\s+)?", "")
                .replaceAll("(?i)^(?:(?:python|java|javascript|js|html|css|c|cpp)\\s+)?(?:code|program|script|app|application|game|file|website|page|webpage)?\\s*(?:for|to|about|of|that\\s+calculates|that\\s+computes|that\\s+prints|that\\s+runs)?\\s*", "")
                .replaceAll("(?i)\\s+(?:in|using)\\s+(?:python|java|javascript|js|html|css|c|cpp).*$", "")
                .replaceAll("(?i)\\s+(?:and\\s+save|save|into).*$", "")
                .trim();

        if (!clean.isEmpty()) {
            topic = clean;
        }

        // Auto-generate filename if not provided
        if (targetFileName == null || targetFileName.isBlank()) {
            String safeTopic = topic.replaceAll("[^a-zA-Z0-9_]+", "_").toLowerCase();
            if (safeTopic.length() > 20) {
                safeTopic = safeTopic.substring(0, 20);
            }
            if (safeTopic.isEmpty()) {
                safeTopic = "stark_code";
            }

            switch (language) {
                case "java":
                    String className = toPascalCase(safeTopic);
                    targetFileName = className + ".java";
                    break;
                case "html":
                    targetFileName = safeTopic + ".html";
                    break;
                case "javascript":
                    targetFileName = safeTopic + ".js";
                    break;
                case "cpp":
                    targetFileName = safeTopic + ".cpp";
                    break;
                case "c":
                    targetFileName = safeTopic + ".c";
                    break;
                case "python":
                default:
                    targetFileName = safeTopic + ".py";
                    break;
            }
        }

        return new CodeRequest(topic, language, targetFileName, explicitSave);
    }

    public String generateOfflineCode(String topic, String language, String fileName) {
        String lowerTopic = topic.toLowerCase();

        // 1. CALCULATOR
        if (lowerTopic.contains("calculator") || lowerTopic.contains("calc")) {
            if ("html".equals(language)) {
                return generateHtmlCalculator();
            } else if ("java".equals(language)) {
                return generateJavaCalculator(fileName);
            }
            return generatePythonCalculator();
        }

        // 2. SNAKE GAME
        if (lowerTopic.contains("snake")) {
            if ("html".equals(language) || "javascript".equals(language)) {
                return generateHtmlSnakeGame();
            }
            return generatePythonSnakeGame();
        }

        // 3. TIC TAC TOE
        if (lowerTopic.contains("tic tac toe") || lowerTopic.contains("tictactoe") || lowerTopic.contains("x and o")) {
            if ("html".equals(language)) {
                return generateHtmlTicTacToe();
            }
            return generatePythonTicTacToe();
        }

        // 4. FIBONACCI
        if (lowerTopic.contains("fibonacci") || lowerTopic.contains("fib")) {
            if ("java".equals(language)) {
                return generateJavaFibonacci(fileName);
            }
            return generatePythonFibonacci();
        }

        // 5. PRIME NUMBERS
        if (lowerTopic.contains("prime")) {
            if ("java".equals(language)) {
                return generateJavaPrimes(fileName);
            }
            return generatePythonPrimes();
        }

        // 6. FACTORIAL
        if (lowerTopic.contains("factorial")) {
            return generatePythonFactorial();
        }

        // 7. TO-DO LIST
        if (lowerTopic.contains("todo") || lowerTopic.contains("to do") || lowerTopic.contains("task")) {
            if ("html".equals(language)) {
                return generateHtmlTodoList();
            }
            return generatePythonTodoList();
        }

        // 8. BINARY SEARCH / SEARCHING
        if (lowerTopic.contains("binary search") || lowerTopic.contains("search")) {
            return generatePythonBinarySearch();
        }

        // 9. SORTING ALGORITHMS
        if (lowerTopic.contains("sort") || lowerTopic.contains("bubble") || lowerTopic.contains("merge sort") || lowerTopic.contains("quick sort")) {
            return generatePythonSorting();
        }

        // 10. PASSWORD GENERATOR
        if (lowerTopic.contains("password")) {
            return generatePythonPasswordGenerator();
        }

        // 11. WEATHER APP
        if (lowerTopic.contains("weather")) {
            return generatePythonWeatherApp();
        }

        // 12. PORTFOLIO WEBSITE
        if (lowerTopic.contains("portfolio") || lowerTopic.contains("website") || lowerTopic.contains("landing page")) {
            return generateHtmlPortfolio();
        }

        // 13. HANGMAN GAME
        if (lowerTopic.contains("hangman")) {
            return generatePythonHangman();
        }

        // 14. PALINDROME
        if (lowerTopic.contains("palindrome")) {
            return generatePythonPalindrome();
        }

        // GENERAL DYNAMIC TEMPLATE FOR ANY TOPIC
        return generateGeneralDynamicCode(topic, language, fileName);
    }

    private String toPascalCase(String input) {
        StringBuilder sb = new StringBuilder();
        boolean capitalizeNext = true;
        for (char c : input.toCharArray()) {
            if (c == '_' || Character.isWhitespace(c)) {
                capitalizeNext = true;
            } else if (capitalizeNext) {
                sb.append(Character.toUpperCase(c));
                capitalizeNext = false;
            } else {
                sb.append(c);
            }
        }
        if (sb.length() == 0) return "StarkModule";
        return sb.toString();
    }

    // ==========================================
    // BUILT-IN PYTHON CODE IMPLEMENTATIONS
    // ==========================================
    private String generatePythonCalculator() {
        return """
#!/usr/bin/env python3
\"\"\"
J.A.R.V.I.S. Standard Calculator System
Author: Stark Industries Protocol
\"\"\"

import math

def add(x, y):
    return x + y

def subtract(x, y):
    return x - y

def multiply(x, y):
    return x * y

def divide(x, y):
    if y == 0:
        raise ValueError("Error: Division by zero is undefined.")
    return x / y

def power(x, y):
    return math.pow(x, y)

def square_root(x):
    if x < 0:
        raise ValueError("Error: Negative value under square root.")
    return math.sqrt(x)

def main():
    print("=" * 45)
    print("   J.A.R.V.I.S. COMPUTATIONAL MATRIX (CALCULATOR)")
    print("=" * 45)
    print("Operations:")
    print(" 1. Addition (+)")
    print(" 2. Subtraction (-)")
    print(" 3. Multiplication (*)")
    print(" 4. Division (/)")
    print(" 5. Exponentiation (^)")
    print(" 6. Square Root (sqrt)")
    print(" 7. Exit")
    print("-" * 45)

    history = []

    while True:
        choice = input("\\nEnter operation (1-7): ").strip()
        if choice == '7' or choice.lower() == 'exit':
            print("Shutting down calculation module. Have a good day, sir.")
            break

        try:
            if choice == '6':
                num = float(input("Enter number: "))
                result = square_root(num)
                record = f"sqrt({num}) = {result}"
                print(f"Result: {record}")
                history.append(record)
                continue

            num1 = float(input("Enter first number: "))
            num2 = float(input("Enter second number: "))

            if choice == '1':
                res = add(num1, num2)
                op = "+"
            elif choice == '2':
                res = subtract(num1, num2)
                op = "-"
            elif choice == '3':
                res = multiply(num1, num2)
                op = "*"
            elif choice == '4':
                res = divide(num1, num2)
                op = "/"
            elif choice == '5':
                res = power(num1, num2)
                op = "^"
            else:
                print("Invalid directive. Please choose from 1 to 7.")
                continue

            record = f"{num1} {op} {num2} = {res}"
            print(f"Result: {record}")
            history.append(record)

        except ValueError as err:
            print(f"Calculation Error: {err}")
        except Exception as e:
            print(f"Unexpected anomaly: {e}")

if __name__ == '__main__':
    main()
""";
    }

    private String generatePythonSnakeGame() {
        return """
#!/usr/bin/env python3
\"\"\"
J.A.R.V.I.S. Terminal Snake Game
Author: Stark Industries Protocol
\"\"\"

import curses
import random

def main(stdscr):
    # Setup window and keypad
    curses.curs_set(0)
    stdscr.nodelay(1)
    stdscr.timeout(100)

    sh, sw = stdscr.getmaxyx()
    w = curses.newwin(sh, sw, 0, 0)
    w.keypad(1)

    # Snake coordinates
    snk_x = sw // 4
    snk_y = sh // 2
    snake = [
        [snk_y, snk_x],
        [snk_y, snk_x - 1],
        [snk_y, snk_x - 2]
    ]

    # Spawn first food
    food = [sh // 2, sw // 2]
    w.addch(int(food[0]), int(food[1]), curses.ACS_PI)

    key = curses.KEY_RIGHT
    score = 0

    while True:
        next_key = w.getch()
        key = key if next_key == -1 else next_key

        # Collision with boundary
        if (snake[0][0] in [0, sh - 1] or
            snake[0][1] in [0, sw - 1] or
            snake[0] in snake[1:]):
            msg = f" GAME OVER // FINAL SCORE: {score} "
            w.addstr(sh // 2, (sw - len(msg)) // 2, msg, curses.A_STANDOUT)
            w.refresh()
            curses.napms(2000)
            break

        # Calculate new head
        new_head = [snake[0][0], snake[0][1]]

        if key == curses.KEY_DOWN:
            new_head[0] += 1
        elif key == curses.KEY_UP:
            new_head[0] -= 1
        elif key == curses.KEY_LEFT:
            new_head[1] -= 1
        elif key == curses.KEY_RIGHT:
            new_head[1] += 1

        snake.insert(0, new_head)

        # Check food collision
        if snake[0] == food:
            score += 10
            food = None
            while food is None:
                nf = [
                    random.randint(1, sh - 2),
                    random.randint(1, sw - 2)
                ]
                food = nf if nf not in snake else None
            w.addch(food[0], food[1], curses.ACS_PI)
        else:
            tail = snake.pop()
            w.addch(int(tail[0]), int(tail[1]), ' ')

        w.addch(int(snake[0][0]), int(snake[0][1]), curses.ACS_CKBOARD)
        w.addstr(0, 2, f" JARVIS SNAKE // SCORE: {score} ")

if __name__ == '__main__':
    try:
        curses.wrapper(main)
    except Exception as e:
        print(f"Snake requires a terminal screen: {e}")
""";
    }

    private String generatePythonTicTacToe() {
        return """
#!/usr/bin/env python3
\"\"\"
J.A.R.V.I.S. Tic-Tac-Toe AI Simulation
\"\"\"

import random

def print_board(board):
    print("\\n")
    print(f" {board[0]} | {board[1]} | {board[2]} ")
    print("---+---+---")
    print(f" {board[3]} | {board[4]} | {board[5]} ")
    print("---+---+---")
    print(f" {board[6]} | {board[7]} | {board[8]} ")
    print("\\n")

def check_winner(board, player):
    win_conditions = [
        [0, 1, 2], [3, 4, 5], [6, 7, 8], # Horizontal
        [0, 3, 6], [1, 4, 7], [2, 5, 8], # Vertical
        [0, 4, 8], [2, 4, 6]             # Diagonal
    ]
    for combo in win_conditions:
        if board[combo[0]] == board[combo[1]] == board[combo[2]] == player:
            return True
    return False

def is_board_full(board):
    return all(cell in ['X', 'O'] for cell in board)

def get_best_move(board, computer, human):
    # 1. Win if possible
    for i in range(9):
        if board[i] not in ['X', 'O']:
            board[i] = computer
            if check_winner(board, computer):
                return i
            board[i] = str(i + 1)

    # 2. Block human
    for i in range(9):
        if board[i] not in ['X', 'O']:
            board[i] = human
            if check_winner(board, human):
                board[i] = computer
                return i
            board[i] = str(i + 1)

    # 3. Take center
    if board[4] not in ['X', 'O']:
        return 4

    # 4. Take random available
    available = [i for i in range(9) if board[i] not in ['X', 'O']]
    return random.choice(available) if available else 0

def main():
    print("====================================")
    print("   J.A.R.V.I.S. TIC-TAC-TOE PROTOCOL")
    print("====================================")
    print("You are playing as 'X'. JARVIS plays as 'O'.")

    board = [str(i + 1) for i in range(9)]
    human = 'X'
    jarvis = 'O'

    print_board(board)

    while True:
        # Human turn
        try:
            move = int(input("Choose position (1-9): ")) - 1
            if move < 0 or move > 8 or board[move] in ['X', 'O']:
                print("Invalid grid coordinate. Try again.")
                continue
            board[move] = human
        except ValueError:
            print("Please enter a valid numeric value.")
            continue

        print_board(board)

        if check_winner(board, human):
            print("Impressive, sir! You have defeated the system.")
            break

        if is_board_full(board):
            print("Stalemate. Tactical equilibrium achieved.")
            break

        # JARVIS turn
        print("JARVIS calculating optimal trajectory...")
        ai_move = get_best_move(board, jarvis, human)
        board[ai_move] = jarvis
        print(f"JARVIS moves to position {ai_move + 1}:")
        print_board(board)

        if check_winner(board, jarvis):
            print("JARVIS wins. Tactical advantage secured, sir.")
            break

        if is_board_full(board):
            print("Game drawn. A balanced confrontation.")
            break

if __name__ == '__main__':
    main()
""";
    }

    private String generatePythonFibonacci() {
        return """
#!/usr/bin/env python3
\"\"\"
J.A.R.V.I.S. Fibonacci Sequence Generator & Benchmarker
\"\"\"

import time

def fib_iterative(n):
    \"\"\"Calculates first n Fibonacci numbers iteratively (O(N) time, O(N) space)\"\"\"
    if n <= 0:
        return []
    if n == 1:
        return [0]
    
    seq = [0, 1]
    while len(seq) < n:
        seq.append(seq[-1] + seq[-2])
    return seq

def fib_nth(n):
    \"\"\"Calculates N-th Fibonacci number in O(1) auxiliary space\"\"\"
    if n <= 0:
        return 0
    if n == 1:
        return 1
    a, b = 0, 1
    for _ in range(2, n + 1):
        a, b = b, a + b
    return b

def main():
    print("=" * 50)
    print("   J.A.R.V.I.S. FIBONACCI ALGORITHMIC SUITE")
    print("=" * 50)
    
    try:
        count = int(input("Enter number of terms to generate (e.g. 15): "))
        if count <= 0:
            print("Term count must be greater than zero.")
            return

        start = time.perf_counter()
        series = fib_iterative(count)
        elapsed = (time.perf_counter() - start) * 1000

        print(f"\\nGenerated {count} Fibonacci Terms in {elapsed:.4f} ms:")
        print("-" * 50)
        for i, val in enumerate(series, 1):
            print(f"F({i:02d}) = {val}")
        print("-" * 50)

        nth = fib_nth(count)
        print(f"The {count}th Fibonacci number is: {nth:,}")

    except ValueError:
        print("Invalid input. Please provide an integer value.")

if __name__ == '__main__':
    main()
""";
    }

    private String generatePythonPrimes() {
        return """
#!/usr/bin/env python3
\"\"\"
J.A.R.V.I.S. Prime Number Engine (Sieve of Eratosthenes)
\"\"\"

import math
import time

def is_prime(n):
    \"\"\"Deterministic primality test up to sqrt(N)\"\"\"
    if n < 2:
        return False
    if n in (2, 3):
        return True
    if n % 2 == 0 or n % 3 == 0:
        return False
    i = 5
    w = 2
    while i * i <= n:
        if n % i == 0:
            return False
        i += w
        w = 6 - w
    return True

def sieve_of_eratosthenes(limit):
    \"\"\"Generates all primes up to limit in O(N log log N)\"\"\"
    if limit < 2:
        return []
    sieve = [True] * (limit + 1)
    sieve[0] = sieve[1] = False
    for p in range(2, int(math.isqrt(limit)) + 1):
        if sieve[p]:
            sieve[p*p : limit+1 : p] = [False] * len(sieve[p*p : limit+1 : p])
    return [p for p in range(2, limit + 1) if sieve[p]]

def main():
    print("=" * 50)
    print("   J.A.R.V.I.S. PRIME COMPUTATION MATRIX")
    print("=" * 50)

    try:
        limit = int(input("Calculate all prime numbers up to: "))
        if limit < 2:
            print("Limit must be at least 2.")
            return

        start = time.perf_counter()
        primes = sieve_of_eratosthenes(limit)
        elapsed = (time.perf_counter() - start) * 1000

        print(f"\\nFound {len(primes)} primes up to {limit:,} in {elapsed:.3f} ms.")
        print("-" * 50)
        
        # Display preview
        preview = primes[:30]
        print("First 30 primes:")
        print(", ".join(map(str, preview)))
        if len(primes) > 30:
            print(f"... and {len(primes) - 30} additional primes.")
            print(f"Largest prime in range: {primes[-1]:,}")

    except ValueError:
        print("Invalid input integer.")

if __name__ == '__main__':
    main()
""";
    }

    private String generatePythonFactorial() {
        return """
#!/usr/bin/env python3
\"\"\"
J.A.R.V.I.S. Factorial Matrix
\"\"\"

import math
import sys

# Increase string conversion limit for massive integers
sys.set_int_max_str_digits(50000)

def factorial_iterative(n):
    if n < 0:
        raise ValueError("Factorial is not defined for negative numbers.")
    result = 1
    for i in range(2, n + 1):
        result *= i
    return result

def main():
    print("=" * 45)
    print("   J.A.R.V.I.S. FACTORIAL COMPUTATION")
    print("=" * 45)

    try:
        n = int(input("Enter non-negative integer: "))
        fact = factorial_iterative(n)
        str_fact = str(fact)

        print(f"\\n{n}! =")
        if len(str_fact) > 100:
            print(f"{str_fact[:50]} ... [total {len(str_fact)} digits] ... {str_fact[-50:]}")
        else:
            print(str_fact)
            
        print(f"\\nTotal digit count: {len(str_fact)}")

    except ValueError as e:
        print(f"Input Error: {e}")

if __name__ == '__main__':
    main()
""";
    }

    private String generatePythonTodoList() {
        return """
#!/usr/bin/env python3
\"\"\"
J.A.R.V.I.S. Task & Directives Manager
\"\"\"

import json
import os
from datetime import datetime

DATA_FILE = "tasks_db.json"

def load_tasks():
    if os.path.exists(DATA_FILE):
        with open(DATA_FILE, 'r', encoding='utf-8') as f:
            return json.load(f)
    return []

def save_tasks(tasks):
    with open(DATA_FILE, 'w', encoding='utf-8') as f:
        json.dump(tasks, f, indent=2)

def add_task(title, priority="NORMAL"):
    tasks = load_tasks()
    task = {
        "id": len(tasks) + 1,
        "title": title,
        "priority": priority.upper(),
        "completed": False,
        "created_at": datetime.now().strftime("%Y-%m-%d %H:%M:%S")
    }
    tasks.append(task)
    save_tasks(tasks)
    print(f"Task #{task['id']} added: {title}")

def list_tasks():
    tasks = load_tasks()
    if not tasks:
        print("No tasks registered in the system, sir.")
        return
    print("\\n--- ACTIVE DIRECTIVES ---")
    for t in tasks:
        status = "[DONE]" if t["completed"] else "[TODO]"
        print(f"{t['id']:02d}. {status} [{t['priority']}] {t['title']} ({t['created_at']})")
    print("-------------------------\\n")

def complete_task(task_id):
    tasks = load_tasks()
    for t in tasks:
        if t["id"] == task_id:
            t["completed"] = True
            save_tasks(tasks)
            print(f"Directive #{task_id} marked complete, sir.")
            return
    print(f"Task #{task_id} not found.")

def main():
    while True:
        print("\\n1. List Tasks | 2. Add Task | 3. Complete Task | 4. Exit")
        choice = input("Enter choice (1-4): ").strip()
        if choice == '1':
            list_tasks()
        elif choice == '2':
            title = input("Enter task title: ")
            prio = input("Priority (LOW, NORMAL, HIGH): ").strip() or "NORMAL"
            add_task(title, prio)
        elif choice == '3':
            tid = int(input("Enter task ID to complete: "))
            complete_task(tid)
        elif choice == '4':
            print("System saved. Exiting task protocol.")
            break

if __name__ == '__main__':
    main()
""";
    }

    private String generatePythonBinarySearch() {
        return """
#!/usr/bin/env python3
\"\"\"
J.A.R.V.I.S. Binary Search Algorithm
\"\"\"

def binary_search(arr, target):
    low = 0
    high = len(arr) - 1
    steps = 0

    while low <= high:
        steps += 1
        mid = (low + high) // 2
        guess = arr[mid]

        print(f"Step {steps}: Search range [{low} .. {high}], inspecting index {mid} (Value: {guess})")

        if guess == target:
            return mid, steps
        if guess > target:
            high = mid - 1
        else:
            low = mid + 1

    return -1, steps

def main():
    data = sorted([12, 45, 67, 89, 102, 145, 230, 310, 450, 520, 680, 790, 850, 999])
    print(f"Sorted Dataset ({len(data)} items): {data}\\n")

    target = int(input("Enter target value to search: "))
    idx, steps = binary_search(data, target)

    if idx != -1:
        print(f"\\nTarget {target} located at index {idx} in {steps} step(s)!")
    else:
        print(f"\\nTarget {target} is not present in data set (searched {steps} intervals).")

if __name__ == '__main__':
    main()
""";
    }

    private String generatePythonSorting() {
        return """
#!/usr/bin/env python3
\"\"\"
J.A.R.V.I.S. Algorithmic Sort Benchmark Suite (QuickSort & MergeSort)
\"\"\"

import random
import time

def quick_sort(arr):
    if len(arr) <= 1:
        return arr
    pivot = arr[len(arr) // 2]
    left = [x for x in arr if x < pivot]
    middle = [x for x in arr if x == pivot]
    right = [x for x in arr if x > pivot]
    return quick_sort(left) + middle + quick_sort(right)

def merge_sort(arr):
    if len(arr) <= 1:
        return arr
    mid = len(arr) // 2
    left = merge_sort(arr[:mid])
    right = merge_sort(arr[mid:])
    return merge(left, right)

def merge(left, right):
    result = []
    i = j = 0
    while i < len(left) and j < len(right):
        if left[i] <= right[j]:
            result.append(left[i])
            i += 1
        else:
            result.append(right[j])
            j += 1
    result.extend(left[i:])
    result.extend(right[j:])
    return result

def main():
    size = 10000
    print(f"Generating {size:,} random integers for sorting test...")
    sample = [random.randint(1, 100000) for _ in range(size)]

    start = time.perf_counter()
    sorted_quick = quick_sort(sample)
    t_quick = (time.perf_counter() - start) * 1000

    start = time.perf_counter()
    sorted_merge = merge_sort(sample)
    t_merge = (time.perf_counter() - start) * 1000

    print(f"QuickSort completed in: {t_quick:.2f} ms")
    print(f"MergeSort completed in: {t_merge:.2f} ms")
    print(f"Verification: {'PASSED' if sorted_quick == sorted_merge else 'FAILED'}")

if __name__ == '__main__':
    main()
""";
    }

    private String generatePythonPasswordGenerator() {
        return """
#!/usr/bin/env python3
\"\"\"
J.A.R.V.I.S. Cryptographic Password Generator
\"\"\"

import secrets
import string

def generate_secure_password(length=16, use_symbols=True):
    if length < 8:
        raise ValueError("Password length should be at least 8 characters.")

    chars = string.ascii_letters + string.digits
    if use_symbols:
        chars += "!@#$%^&*()-_=+[]{}<>?"

    # Ensure at least one of each required class
    password = [
        secrets.choice(string.ascii_uppercase),
        secrets.choice(string.ascii_lowercase),
        secrets.choice(string.digits)
    ]
    if use_symbols:
        password.append(secrets.choice("!@#$%^&*()-_=+[]{}<>?"))

    # Fill remaining
    password += [secrets.choice(chars) for _ in range(length - len(password))]
    secrets.SystemRandom().shuffle(password)
    return "".join(password)

def main():
    print("=" * 45)
    print("   J.A.R.V.I.S. CIPHER KEY GENERATOR")
    print("=" * 45)
    
    count = int(input("How many keys to generate: ") or 5)
    length = int(input("Key length (default 18): ") or 18)

    print("\\nGenerated Secure Passwords:")
    print("-" * 45)
    for i in range(1, count + 1):
        pw = generate_secure_password(length)
        print(f" Key #{i:02d}: {pw}")
    print("-" * 45)

if __name__ == '__main__':
    main()
""";
    }

    private String generatePythonWeatherApp() {
        return """
#!/usr/bin/env python3
\"\"\"
J.A.R.V.I.S. Weather Diagnostics Terminal
\"\"\"

import random

CITIES = {
    "New York": {"temp": 18, "condition": "Partly Cloudy", "humidity": 55, "wind": 12},
    "London": {"temp": 14, "condition": "Light Rain", "humidity": 78, "wind": 18},
    "Tokyo": {"temp": 22, "condition": "Clear Sky", "humidity": 45, "wind": 8},
    "Malibu": {"temp": 26, "condition": "Sunny / Optimal", "humidity": 38, "wind": 5},
    "Mumbai": {"temp": 31, "condition": "Humid / Sunny", "humidity": 80, "wind": 10}
}

def display_weather(city_name):
    data = CITIES.get(city_name)
    if not data:
        # Generate simulated reading
        data = {
            "temp": random.randint(15, 30),
            "condition": random.choice(["Sunny", "Cloudy", "Rain Showers", "Windy"]),
            "humidity": random.randint(40, 85),
            "wind": random.randint(5, 25)
        }

    print("=" * 45)
    print(f"   WEATHER TELEMETRY: {city_name.upper()}")
    print("=" * 45)
    print(f" Temperature:  {data['temp']}°C ({data['temp'] * 9/5 + 32:.1f}°F)")
    print(f" Condition:    {data['condition']}")
    print(f" Humidity:     {data['humidity']}%")
    print(f" Wind Speed:   {data['wind']} km/h")
    print("=" * 45)

def main():
    print("Known Locations: " + ", ".join(CITIES.keys()))
    loc = input("Enter city name: ").strip().title()
    if loc:
        display_weather(loc)

if __name__ == '__main__':
    main()
""";
    }

    private String generatePythonHangman() {
        return """
#!/usr/bin/env python3
\"\"\"
J.A.R.V.I.S. Hangman Word Decryption Game
\"\"\"

import random

WORDS = ["STARK", "AVENGERS", "REACTOR", "NANOTECH", "JARVIS", "QUANTUM", "VIBRANIUM", "ULTRON"]

STAGES = [
    \"\"\"
       +---+
       |   |
           |
           |
           |
           |
    =========\"\"\",
    \"\"\"
       +---+
       |   |
       O   |
           |
           |
           |
    =========\"\"\",
    \"\"\"
       +---+
       |   |
       O   |
       |   |
           |
           |
    =========\"\"\",
    \"\"\"
       +---+
       |   |
       O   |
      /|   |
           |
           |
    =========\"\"\",
    \"\"\"
       +---+
       |   |
       O   |
      /|\\  |
           |
           |
    =========\"\"\",
    \"\"\"
       +---+
       |   |
       O   |
      /|\\  |
      /    |
           |
    =========\"\"\",
    \"\"\"
       +---+
       |   |
       O   |
      /|\\  |
      / \\  |
           |
    =========\"\"\"
]

def main():
    word = random.choice(WORDS)
    guessed = set()
    lives = 6

    print("====================================")
    print("   J.A.R.V.I.S. CIPHER HANGMAN")
    print("====================================")

    while lives > 0:
        print(STAGES[6 - lives])
        display = [ch if ch in guessed else "_" for ch in word]
        print("\\nWord: " + " ".join(display))
        print(f"Lives Remaining: {lives}")

        if "_" not in display:
            print(f"\\nCipher Decrypted! The word was {word}. Outstanding work, sir.")
            break

        guess = input("Guess a letter: ").strip().upper()
        if not guess or len(guess) != 1 or not guess.isalpha():
            print("Please enter a single alphabetical character.")
            continue

        if guess in guessed:
            print(f"You already tested '{guess}'.")
            continue

        guessed.add(guess)

        if guess in word:
            print(f"Positive match: '{guess}' is present.")
        else:
            lives -= 1
            print(f"Negative match: '{guess}' not found.")

    if lives == 0:
        print(STAGES[6])
        print(f"\\nSecurity lockdown initiated. The encrypted word was '{word}'.")

if __name__ == '__main__':
    main()
""";
    }

    private String generatePythonPalindrome() {
        return """
#!/usr/bin/env python3
\"\"\"
J.A.R.V.I.S. Palindrome Detection Matrix
\"\"\"

import re

def is_palindrome(text):
    clean = re.sub(r'[^a-zA-Z0-9]', '', str(text)).lower()
    return clean == clean[::-1], clean

def main():
    print("=" * 45)
    print("   J.A.R.V.I.S. PALINDROME VERIFIER")
    print("=" * 45)

    test = input("Enter word, sentence, or number: ")
    passed, clean = is_palindrome(test)

    print(f"\\nNormalized string: '{clean}'")
    print(f"Reversed string:   '{clean[::-1]}'")
    
    if passed:
        print("Verdict: POSITIVE. The input is a valid palindrome, sir.")
    else:
        print("Verdict: NEGATIVE. The input is not symmetric.")

if __name__ == '__main__':
    main()
""";
    }

    // ==========================================
    // HTML / JAVASCRIPT IMPLEMENTATIONS
    // ==========================================
    private String generateHtmlCalculator() {
        return """
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>J.A.R.V.I.S. Quantum Calculator</title>
    <style>
        * { margin: 0; padding: 0; box-sizing: border-box; font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; }
        body { min-height: 100vh; background: radial-gradient(circle at center, #1a0808 0%, #050505 100%); display: flex; justify-content: center; align-items: center; }
        .calc-box { width: 340px; background: rgba(20, 15, 20, 0.9); border: 2px solid #d4af37; border-radius: 16px; padding: 25px; box-shadow: 0 0 30px rgba(255, 69, 0, 0.4); backdrop-filter: blur(10px); }
        .calc-title { color: #d4af37; text-align: center; font-size: 18px; letter-spacing: 2px; margin-bottom: 15px; }
        #display { width: 100%; height: 60px; background: #000; border: 1px solid #ff4500; border-radius: 8px; color: #00e5ff; font-size: 28px; text-align: right; padding: 12px; margin-bottom: 20px; outline: none; }
        .btn-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 10px; }
        button { height: 50px; background: rgba(212, 175, 55, 0.1); border: 1px solid rgba(212, 175, 55, 0.4); border-radius: 8px; color: #fff; font-size: 18px; font-weight: bold; cursor: pointer; transition: 0.2s; }
        button:hover { background: rgba(212, 175, 55, 0.3); transform: scale(1.05); }
        button.op { background: rgba(255, 69, 0, 0.2); border-color: #ff4500; color: #ff8c00; }
        button.eq { background: #ff4500; color: #fff; border: none; grid-column: span 2; }
        button.clear { background: rgba(230, 43, 30, 0.3); border-color: #ff3333; color: #ff6666; }
    </style>
</head>
<body>
    <div class="calc-box">
        <div class="calc-title">STARK CALC v4.0</div>
        <input type="text" id="display" readonly value="0">
        <div class="btn-grid">
            <button class="clear" onclick="clearDisplay()">C</button>
            <button class="op" onclick="delChar()">DEL</button>
            <button class="op" onclick="press('%')">%</button>
            <button class="op" onclick="press('/')">/</button>

            <button onclick="press('7')">7</button>
            <button onclick="press('8')">8</button>
            <button onclick="press('9')">9</button>
            <button class="op" onclick="press('*')">*</button>

            <button onclick="press('4')">4</button>
            <button onclick="press('5')">5</button>
            <button onclick="press('6')">6</button>
            <button class="op" onclick="press('-')">-</button>

            <button onclick="press('1')">1</button>
            <button onclick="press('2')">2</button>
            <button onclick="press('3')">3</button>
            <button class="op" onclick="press('+')">+</button>

            <button onclick="press('0')">0</button>
            <button onclick="press('.')">.</button>
            <button class="eq" onclick="calculate()">=</button>
        </div>
    </div>
    <script>
        const d = document.getElementById("display");
        function press(val) { if (d.value === "0" && val !== ".") d.value = val; else d.value += val; }
        function clearDisplay() { d.value = "0"; }
        function delChar() { d.value = d.value.slice(0, -1) || "0"; }
        function calculate() { try { d.value = eval(d.value); } catch(e) { d.value = "Error"; } }
    </script>
</body>
</html>
""";
    }

    private String generateHtmlSnakeGame() {
        return """
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>J.A.R.V.I.S. Arc Snake</title>
    <style>
        body { margin: 0; background: #070609; display: flex; flex-direction: column; align-items: center; justify-content: center; height: 100vh; color: #d4af37; font-family: 'Courier New', Courier, monospace; }
        h1 { margin-bottom: 10px; letter-spacing: 4px; text-shadow: 0 0 10px #ff4500; }
        #canvas { border: 2px solid #d4af37; background: #000; box-shadow: 0 0 30px rgba(0, 229, 255, 0.4); }
        .score-board { margin-top: 15px; font-size: 20px; font-weight: bold; }
    </style>
</head>
<body>
    <h1>J.A.R.V.I.S. SNAKE</h1>
    <canvas id="canvas" width="400" height="400"></canvas>
    <div class="score-board">SCORE: <span id="score">0</span></div>
    <script>
        const canvas = document.getElementById("canvas");
        const ctx = canvas.getContext("2d");
        const grid = 20;
        let count = 0;
        let score = 0;
        let snake = { x: 160, y: 160, dx: grid, dy: 0, cells: [], maxCells: 4 };
        let food = { x: 320, y: 320 };

        function getRandomInt(min, max) { return Math.floor(Math.random() * (max - min)) + min; }

        function loop() {
            requestAnimationFrame(loop);
            if (++count < 6) return;
            count = 0;
            ctx.clearRect(0, 0, canvas.width, canvas.height);

            snake.x += snake.dx;
            snake.y += snake.dy;

            if (snake.x < 0) snake.x = canvas.width - grid;
            else if (snake.x >= canvas.width) snake.x = 0;
            if (snake.y < 0) snake.y = canvas.height - grid;
            else if (snake.y >= canvas.height) snake.y = 0;

            snake.cells.unshift({x: snake.x, y: snake.y});
            if (snake.cells.length > snake.maxCells) snake.cells.pop();

            ctx.fillStyle = '#ff4500';
            ctx.fillRect(food.x, food.y, grid - 1, grid - 1);

            ctx.fillStyle = '#00e5ff';
            snake.cells.forEach((cell, index) => {
                ctx.fillRect(cell.x, cell.y, grid - 1, grid - 1);
                if (cell.x === food.x && cell.y === food.y) {
                    snake.maxCells++;
                    score += 10;
                    document.getElementById("score").textContent = score;
                    food.x = getRandomInt(0, 20) * grid;
                    food.y = getRandomInt(0, 20) * grid;
                }
                for (let i = index + 1; i < snake.cells.length; i++) {
                    if (cell.x === snake.cells[i].x && cell.y === snake.cells[i].y) {
                        snake.x = 160; snake.y = 160; snake.cells = [];
                        snake.maxCells = 4; snake.dx = grid; snake.dy = 0;
                        score = 0; document.getElementById("score").textContent = score;
                    }
                }
            });
        }

        document.addEventListener('keydown', function(e) {
            if (e.key === 'ArrowLeft' && snake.dx === 0) { snake.dx = -grid; snake.dy = 0; }
            else if (e.key === 'ArrowUp' && snake.dy === 0) { snake.dy = -grid; snake.dx = 0; }
            else if (e.key === 'ArrowRight' && snake.dx === 0) { snake.dx = grid; snake.dy = 0; }
            else if (e.key === 'ArrowDown' && snake.dy === 0) { snake.dy = grid; snake.dx = 0; }
        });
        requestAnimationFrame(loop);
    </script>
</body>
</html>
""";
    }

    private String generateHtmlTicTacToe() {
        return """
<!DOCTYPE html>
<html>
<head>
    <title>JARVIS Tic Tac Toe</title>
    <style>
        body { background: #0b090a; color: #d4af37; display: flex; flex-direction: column; align-items: center; justify-content: center; height: 100vh; font-family: sans-serif; }
        .grid { display: grid; grid-template-columns: repeat(3, 100px); grid-gap: 8px; margin: 20px 0; }
        .cell { width: 100px; height: 100px; background: rgba(212, 175, 55, 0.1); border: 2px solid #d4af37; display: flex; align-items: center; justify-content: center; font-size: 40px; cursor: pointer; color: #00e5ff; }
        .cell:hover { background: rgba(212, 175, 55, 0.25); }
        button { padding: 10px 20px; background: #ff4500; border: none; color: #fff; font-weight: bold; cursor: pointer; border-radius: 6px; }
    </style>
</head>
<body>
    <h1>TIC TAC TOE</h1>
    <div id="status">Turn: Player X</div>
    <div class="grid" id="grid"></div>
    <button onclick="resetGame()">Restart Protocol</button>
    <script>
        let turn = 'X', board = Array(9).fill(null), active = true;
        const grid = document.getElementById('grid');
        function render() {
            grid.innerHTML = '';
            board.forEach((val, i) => {
                const cell = document.createElement('div');
                cell.className = 'cell';
                cell.textContent = val || '';
                cell.onclick = () => move(i);
                grid.appendChild(cell);
            });
        }
        function move(i) {
            if (!board[i] && active) {
                board[i] = turn;
                if (checkWin()) {
                    document.getElementById('status').textContent = `Winner: ${turn}!`;
                    active = false;
                } else if (!board.includes(null)) {
                    document.getElementById('status').textContent = 'Draw!';
                    active = false;
                } else {
                    turn = turn === 'X' ? 'O' : 'X';
                    document.getElementById('status').textContent = `Turn: Player ${turn}`;
                }
                render();
            }
        }
        function checkWin() {
            const wins = [[0,1,2],[3,4,5],[6,7,8],[0,3,6],[1,4,7],[2,5,8],[0,4,8],[2,4,6]];
            return wins.some(([a,b,c]) => board[a] && board[a] === board[b] && board[a] === board[c]);
        }
        function resetGame() { board = Array(9).fill(null); active = true; turn = 'X'; document.getElementById('status').textContent = 'Turn: Player X'; render(); }
        render();
    </script>
</body>
</html>
""";
    }

    private String generateHtmlTodoList() {
        return """
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>JARVIS Directives Board</title>
    <style>
        body { background: #08070b; color: #f0f0f0; font-family: 'Segoe UI', Tahoma, sans-serif; display: flex; justify-content: center; padding: 40px; }
        .card { width: 100%; max-width: 500px; background: rgba(18, 14, 20, 0.9); border: 1px solid #d4af37; border-radius: 12px; padding: 25px; box-shadow: 0 0 25px rgba(255, 69, 0, 0.2); }
        h1 { color: #d4af37; font-size: 24px; margin-bottom: 20px; text-align: center; }
        .input-row { display: flex; gap: 10px; margin-bottom: 20px; }
        input { flex: 1; padding: 10px 14px; background: #000; border: 1px solid #d4af37; border-radius: 6px; color: #fff; outline: none; }
        button { background: #ff4500; border: none; color: #fff; padding: 10px 18px; border-radius: 6px; cursor: pointer; font-weight: bold; }
        ul { list-style: none; padding: 0; }
        li { display: flex; justify-content: space-between; align-items: center; padding: 12px; background: rgba(255,255,255,0.05); margin-bottom: 8px; border-radius: 6px; }
        li.done span { text-decoration: line-through; color: #888; }
        .del-btn { background: #d92027; padding: 4px 8px; font-size: 12px; }
    </style>
</head>
<body>
    <div class="card">
        <h1>STARK TASK VAULT</h1>
        <div class="input-row">
            <input type="text" id="taskInput" placeholder="Add directive...">
            <button onclick="addTask()">Add</button>
        </div>
        <ul id="taskList"></ul>
    </div>
    <script>
        let tasks = JSON.parse(localStorage.getItem('jarvis_tasks') || '[]');
        function save() { localStorage.setItem('jarvis_tasks', JSON.stringify(tasks)); render(); }
        function render() {
            const ul = document.getElementById('taskList');
            ul.innerHTML = '';
            tasks.forEach((t, i) => {
                const li = document.createElement('li');
                if (t.done) li.className = 'done';
                li.innerHTML = `<span onclick="toggle(${i})" style="cursor:pointer">${t.text}</span> <button class="del-btn" onclick="del(${i})">✕</button>`;
                ul.appendChild(li);
            });
        }
        function addTask() {
            const input = document.getElementById('taskInput');
            if (input.value.trim()) { tasks.push({text: input.value.trim(), done: false}); input.value = ''; save(); }
        }
        function toggle(i) { tasks[i].done = !tasks[i].done; save(); }
        function del(i) { tasks.splice(i, 1); save(); }
        render();
    </script>
</body>
</html>
""";
    }

    private String generateHtmlPortfolio() {
        return """
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Stark Industries // Engineering Portfolio</title>
    <style>
        * { margin: 0; padding: 0; box-sizing: border-box; }
        body { background: #060508; color: #fff; font-family: 'Segoe UI', Tahoma, sans-serif; line-height: 1.6; }
        header { display: flex; justify-content: space-between; align-items: center; padding: 25px 50px; background: rgba(10, 8, 12, 0.85); border-bottom: 1px solid #d4af37; }
        .brand { font-size: 22px; font-weight: bold; color: #d4af37; letter-spacing: 2px; }
        nav a { color: #aaa; text-decoration: none; margin-left: 25px; transition: 0.2s; }
        nav a:hover { color: #00e5ff; }
        .hero { min-height: 70vh; display: flex; flex-direction: column; justify-content: center; align-items: center; text-align: center; padding: 50px 20px; }
        .hero h1 { font-size: 50px; color: #d4af37; margin-bottom: 15px; }
        .hero p { font-size: 20px; color: #00e5ff; max-width: 600px; margin-bottom: 30px; }
        .btn { padding: 12px 30px; background: #ff4500; border: none; border-radius: 30px; color: #fff; font-weight: bold; text-decoration: none; font-size: 16px; transition: 0.3s; }
        .btn:hover { background: #e03b00; transform: scale(1.05); }
        .grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(280px, 1fr)); gap: 25px; padding: 50px; max-width: 1200px; margin: 0 auto; }
        .card { background: rgba(255, 255, 255, 0.04); border: 1px solid rgba(212, 175, 55, 0.3); border-radius: 12px; padding: 25px; }
        .card h3 { color: #d4af37; margin-bottom: 10px; }
    </style>
</head>
<body>
    <header>
        <div class="brand">STARK INDUSTRIES</div>
        <nav><a href="#about">About</a><a href="#projects">Projects</a><a href="#contact">Contact</a></nav>
    </header>
    <section class="hero">
        <h1>TONY STARK</h1>
        <p>Visionary • Innovator • Engineering Mark 85 Autonomous Defense Platforms</p>
        <a href="#projects" class="btn">Explore Projects</a>
    </section>
    <section id="projects" class="grid">
        <div class="card">
            <h3>Arc Reactor v4.0</h3>
            <p>Clean self-sustaining fusion reactor outputting 3 gigajoules per second.</p>
        </div>
        <div class="card">
            <h3>J.A.R.V.I.S. Protocol</h3>
            <p>Heuristic artificial intelligence system managing autonomous defense and file architecture.</p>
        </div>
        <div class="card">
            <h3>Nanotech Mark 85</h3>
            <p>Fluid nanoparticle lattice restructuring instantly on command for all mission profiles.</p>
        </div>
    </section>
</body>
</html>
""";
    }

    // ==========================================
    // JAVA IMPLEMENTATIONS
    // ==========================================
    private String generateJavaCalculator(String fileName) {
        String className = fileName.replaceAll("\\.java$", "");
        return "import java.util.Scanner;\n\n"
                + "public class " + className + " {\n"
                + "    public static void main(String[] args) {\n"
                + "        Scanner scanner = new Scanner(System.in);\n"
                + "        System.out.println(\"=== J.A.R.V.I.S. JAVA CALCULATOR ===\");\n\n"
                + "        while (true) {\n"
                + "            System.out.println(\"\\nSelect Operation:\");\n"
                + "            System.out.println(\"1. Add (+) | 2. Subtract (-) | 3. Multiply (*) | 4. Divide (/) | 5. Exit\");\n"
                + "            System.out.print(\"Choice (1-5): \");\n"
                + "            int choice = scanner.nextInt();\n"
                + "            if (choice == 5) {\n"
                + "                System.out.println(\"Exiting calculation protocol, sir.\");\n"
                + "                break;\n"
                + "            }\n\n"
                + "            System.out.print(\"Enter first number: \");\n"
                + "            double a = scanner.nextDouble();\n"
                + "            System.out.print(\"Enter second number: \");\n"
                + "            double b = scanner.nextDouble();\n\n"
                + "            switch (choice) {\n"
                + "                case 1 -> System.out.println(\"Result: \" + (a + b));\n"
                + "                case 2 -> System.out.println(\"Result: \" + (a - b));\n"
                + "                case 3 -> System.out.println(\"Result: \" + (a * b));\n"
                + "                case 4 -> {\n"
                + "                    if (b == 0) System.out.println(\"Error: Division by zero!\");\n"
                + "                    else System.out.println(\"Result: \" + (a / b));\n"
                + "                }\n"
                + "                default -> System.out.println(\"Invalid option.\");\n"
                + "            }\n"
                + "        }\n"
                + "        scanner.close();\n"
                + "    }\n"
                + "}\n";
    }

    private String generateJavaFibonacci(String fileName) {
        String className = fileName.replaceAll("\\.java$", "");
        return "import java.math.BigInteger;\n"
                + "import java.util.Scanner;\n\n"
                + "public class " + className + " {\n"
                + "    public static BigInteger[] generate(int n) {\n"
                + "        if (n <= 0) return new BigInteger[0];\n"
                + "        BigInteger[] fib = new BigInteger[n];\n"
                + "        fib[0] = BigInteger.ZERO;\n"
                + "        if (n > 1) fib[1] = BigInteger.ONE;\n"
                + "        for (int i = 2; i < n; i++) {\n"
                + "            fib[i] = fib[i - 1].add(fib[i - 2]);\n"
                + "        }\n"
                + "        return fib;\n"
                + "    }\n\n"
                + "    public static void main(String[] args) {\n"
                + "        Scanner scanner = new Scanner(System.in);\n"
                + "        System.out.print(\"Enter number of Fibonacci terms: \");\n"
                + "        int n = scanner.nextInt();\n"
                + "        BigInteger[] sequence = generate(n);\n"
                + "        System.out.println(\"\\n--- Fibonacci Series ---\");\n"
                + "        for (int i = 0; i < sequence.length; i++) {\n"
                + "            System.out.println(\"F(\" + (i + 1) + \") = \" + sequence[i]);\n"
                + "        }\n"
                + "        scanner.close();\n"
                + "    }\n"
                + "}\n";
    }

    private String generateJavaPrimes(String fileName) {
        String className = fileName.replaceAll("\\.java$", "");
        return "import java.util.ArrayList;\n"
                + "import java.util.List;\n"
                + "import java.util.Scanner;\n\n"
                + "public class " + className + " {\n"
                + "    public static boolean isPrime(long n) {\n"
                + "        if (n <= 1) return false;\n"
                + "        if (n <= 3) return true;\n"
                + "        if (n % 2 == 0 || n % 3 == 0) return false;\n"
                + "        for (long i = 5; i * i <= n; i += 6) {\n"
                + "            if (n % i == 0 || n % (i + 2) == 0) return false;\n"
                + "        }\n"
                + "        return true;\n"
                + "    }\n\n"
                + "    public static void main(String[] args) {\n"
                + "        Scanner scanner = new Scanner(System.in);\n"
                + "        System.out.print(\"Find primes up to limit: \");\n"
                + "        long limit = scanner.nextLong();\n"
                + "        List<Long> primes = new ArrayList<>();\n"
                + "        for (long i = 2; i <= limit; i++) {\n"
                + "            if (isPrime(i)) primes.add(i);\n"
                + "        }\n"
                + "        System.out.println(\"Found \" + primes.size() + \" primes up to \" + limit + \":\");\n"
                + "        System.out.println(primes);\n"
                + "        scanner.close();\n"
                + "    }\n"
                + "}\n";
    }

    // ==========================================
    // DYNAMIC GENERAL CODE BUILDER
    // ==========================================
    private String generateGeneralDynamicCode(String topic, String language, String fileName) {
        if ("java".equals(language)) {
            String className = fileName.replaceAll("\\.java$", "");
            return "/**\n"
                    + " * J.A.R.V.I.S. Autonomous Module: " + topic + "\n"
                    + " * Architect: Stark Industries\n"
                    + " */\n"
                    + "public class " + className + " {\n\n"
                    + "    private final String moduleName = \"" + topic + "\";\n\n"
                    + "    public void execute() {\n"
                    + "        System.out.println(\"[JARVIS] Executing protocol: \" + moduleName);\n"
                    + "        // Core implementation routine\n"
                    + "        System.out.println(\"[JARVIS] Telemetry optimal. Processing completed successfully.\");\n"
                    + "    }\n\n"
                    + "    public static void main(String[] args) {\n"
                    + "        " + className + " app = new " + className + "();\n"
                    + "        app.execute();\n"
                    + "    }\n"
                    + "}\n";
        } else if ("html".equals(language)) {
            return "<!DOCTYPE html>\n"
                    + "<html lang=\"en\">\n"
                    + "<head>\n"
                    + "    <meta charset=\"UTF-8\">\n"
                    + "    <title>" + topic + " // Stark Systems</title>\n"
                    + "    <style>\n"
                    + "        body { background: #07060a; color: #00e5ff; font-family: monospace; padding: 40px; }\n"
                    + "        h1 { color: #d4af37; }\n"
                    + "        .panel { border: 1px solid #d4af37; padding: 20px; border-radius: 8px; background: rgba(0,0,0,0.5); }\n"
                    + "    </style>\n"
                    + "</head>\n"
                    + "<body>\n"
                    + "    <div class=\"panel\">\n"
                    + "        <h1>" + topic.toUpperCase() + "</h1>\n"
                    + "        <p>J.A.R.V.I.S. Core Protocol Online.</p>\n"
                    + "    </div>\n"
                    + "</body>\n"
                    + "</html>\n";
        }

        // Default to Python
        return "#!/usr/bin/env python3\n"
                + "\"\"\"\n"
                + "J.A.R.V.I.S. Protocol: " + topic + "\n"
                + "Synthesized by Stark Industries AI Core\n"
                + "\"\"\"\n\n"
                + "import sys\n"
                + "import time\n\n"
                + "class " + toPascalCase(topic) + "Controller:\n"
                + "    \"\"\"Handles operational logic for " + topic + "\"\"\"\n\n"
                + "    def __init__(self):\n"
                + "        self.module_name = \"" + topic + "\"\n"
                + "        self.initialized_at = time.time()\n\n"
                + "    def run(self):\n"
                + "        print(f\"[JARVIS] Launching module: {self.module_name}...\")\n"
                + "        # Implement primary logic here\n"
                + "        time.sleep(0.3)\n"
                + "        print(f\"[JARVIS] Protocol '{self.module_name}' operational and healthy.\")\n"
                + "        return True\n\n"
                + "def main():\n"
                + "    print(\"=\" * 45)\n"
                + "    print(f\"   STARK MATRIX // {('" + topic.toUpperCase() + "')}\")\n"
                + "    print(\"=\" * 45)\n"
                + "    controller = " + toPascalCase(topic) + "Controller()\n"
                + "    controller.run()\n\n"
                + "if __name__ == '__main__':\n"
                + "    main()\n";
    }
}
