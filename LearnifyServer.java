//This class is the main server for the Learnify application. 
// It handles HTTP requests for user signup, login, session management, and serving frontend files.

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpExchange;

//User management imports
import dao.StudentDAO;
import model.Student;
import util.PasswordHash;

//Goal management imports
import model.Goal;
import util.GoalManager;
import java.util.List;

//Flashcard management imports
import model.Flashcard;
import util.FlashcardManager;

//Notebook file management imports
import model.StudyFile;
import util.StudyFileManager;
import java.util.Map;
import java.util.HashMap;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

//Quiz management imports
import model.Quiz;
import util.QuizManager;
import model.Question;


import java.nio.charset.StandardCharsets;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;


//This is the main class for the server
public class LearnifyServer {

private static String getSessionID(HttpExchange exchange) {

    String cookieHeader = exchange.getRequestHeaders().getFirst("Cookie");

    if (cookieHeader == null) {
        return null;
    }

    for (String cookie : cookieHeader.split(";")) {

        String[] parts = cookie.trim().split("=", 2);

        if (parts.length == 2
                && parts[0].equals("LEARNIFY_SESSION")) {

            return parts[1];
        }
    }
    return null;
}


private static Integer getLoggedInStudentID(HttpExchange exchange) {
    String sessionID = getSessionID(exchange);
    if (sessionID == null) {
        return null;
    }
    return SessionManager.getStudentID(sessionID);
}

public static void main(String[] args) throws Exception {

HttpServer server = HttpServer.create(
        new InetSocketAddress(8000),
        0
);


// Signup function
server.createContext("/api/signup", exchange -> {

        if (!exchange.getRequestMethod().equals("POST")) {
        sendResponse(exchange, "Invalid request.");
        return;
        }

        String data = new String(
                exchange.getRequestBody().readAllBytes(),
                StandardCharsets.UTF_8
        );

        String name = getValue(data, "name");
        String email = getValue(data, "email");
        String password = getValue(data, "password");

        // Check fields
        if (name == null || name.isBlank()
                || email == null || email.isBlank()
                || password == null || password.isBlank()) {

        sendResponse(
                exchange,
                "Please fill all the fields."
        );

        return;
        }

        // Basic email validation
        if (!email.contains("@")) {

        sendResponse(
                exchange,
                "Please enter a valid email."
        );

        return;
        }

        // Hash password
        String hashedPassword =
                PasswordHash.hashPassword(password);

        // Create Student object
        Student student = new Student(
                name,
                email,
                hashedPassword
        );

        // Save student
        StudentDAO studentDAO = new StudentDAO();

        boolean registered =
                studentDAO.registerStudent(student);

        if (registered) {

         // Find the newly created student
        Student createdStudent = studentDAO.getStudentByEmail(email);

         if (createdStudent != null) {
        // Create session for the new student
        String sessionID =SessionManager.createSession(createdStudent.getStudentID()
        );

        // Store session ID in browser cookie
        exchange.getResponseHeaders().add(
                "Set-Cookie",
                "LEARNIFY_SESSION=" + sessionID
                + "; Path=/; HttpOnly; SameSite=Lax; Max-Age=86400"
        );

        sendResponse(
                exchange,
                "Account created successfully!"
        );

    } else {
        sendResponse(
                exchange,
                "Account created, but session could not be created."
        );
    }
        } else {

        sendResponse(
                exchange,
                "Registration failed. Email may already exist."
        );
        }
});


// Login function
server.createContext("/api/login", exchange -> {

    if (!exchange.getRequestMethod().equals("POST")) {

        sendResponse(
                exchange,
                "Invalid request."
        );

        return;
    }

    String data = new String(
            exchange.getRequestBody().readAllBytes(),
            StandardCharsets.UTF_8
    );

    String email = getValue(data, "email");
    String password = getValue(data, "password");

    // Check fields
    if (email == null || email.isBlank() || password == null || password.isBlank()) {

        sendResponse(
                exchange,
                "Please enter email and password."
        );

        return;
    }

    // Find student
    StudentDAO studentDAO =
            new StudentDAO();

    Student student = studentDAO.getStudentByEmail(email);

    // Check if account exists
    if (student == null) {

        sendResponse(
                exchange,
                "Invalid email or password."
        );

        return;
    }


    // Hash entered password
    String hashedPassword = PasswordHash.hashPassword(password);


    // Compare passwords
        if (hashedPassword != null && hashedPassword.equals(student.getPassword())) {

    // Create a session for the student
    String sessionID =
            SessionManager.createSession(student.getStudentID());

    // Save the session ID in the browser's cookie
    exchange.getResponseHeaders().add(
            "Set-Cookie",
            "LEARNIFY_SESSION=" + sessionID
                + "; Path=/; HttpOnly; SameSite=Lax; Max-Age=86400"
    );

    sendResponse(
            exchange,
            "Login successful!"
    );

} else {
        sendResponse(
                exchange,
                "Invalid email or password."
        );
    }

});


//Sign up remaining session check
server.createContext("/api/session-status", exchange -> {
    String sessionID = getSessionID(exchange);
    Integer studentID = null;

    if (sessionID != null) {
        studentID = SessionManager.getStudentID(sessionID);
    }

    if (studentID != null) {
        sendResponse(
                exchange,
                "logged_in"
        );

    } else {
        sendResponse(
                exchange,
                "logged_out"
        );
    }
});

//Login remaining session check
server.createContext("/api/session", exchange -> {

    String sessionID = getSessionID(exchange);

    Integer studentID = null;

    if (sessionID != null) {
        studentID = SessionManager.getStudentID(sessionID);
    }

    if (studentID != null) {

        sendResponse(
                exchange,
                "Logged in. Student ID: " + studentID
        );

    } else {

        sendResponse(
                exchange,
                "Not logged in."
        );
    }
});


// Logout session code
server.createContext("/api/logout", exchange -> {

    String sessionID = getSessionID(exchange);

    if (sessionID != null) {
        SessionManager.destroySession(sessionID);
    }

    // Remove the browser's cookie
    exchange.getResponseHeaders().add(
            "Set-Cookie",
            "LEARNIFY_SESSION=; Path=/; HttpOnly; SameSite=Lax; Max-Age=0"
    );

    sendResponse(exchange, "Logged out successfully.");
});


// Goal management code - API - Add goal
server.createContext("/api/goals", exchange -> {

    if (!exchange.getRequestMethod().equals("POST")) {
        sendResponse(
                exchange,
                "Invalid request."
        );
        return;
    }

    Integer studentID = getLoggedInStudentID(exchange);

    if (studentID == null) {

        sendResponse(
                exchange,
                "Please login first."
        );
        return;
    }

    String data =
            new String(
                    exchange.getRequestBody().readAllBytes(),
                    StandardCharsets.UTF_8
            );

    String title = getValue(data, "title");

    String type = getValue(data, "type");

    String targetDate = getValue(data, "targetDate");

    String targetHoursText = getValue(data, "targetHours");

    String completedHoursText = getValue(data, "completedHours");

    if (title == null || title.isBlank()
            || type == null || type.isBlank()
            || targetDate == null || targetDate.isBlank()
            || targetHoursText == null
            || completedHoursText == null) {

        sendResponse(
                exchange,
                "Please fill all fields."
        );
        return;
    }

    try {
        double targetHours = Double.parseDouble(targetHoursText);

        double completedHours = Double.parseDouble(completedHoursText);

        if (targetHours <= 0
                || completedHours < 0
                || completedHours > targetHours) {

            sendResponse(
                    exchange,
                    "Invalid study hours."
            );

            return;
        }

        String status =
                completedHours >= targetHours
                        ? "Completed"
                        : "In Progress";


        Goal goal =
                new Goal(
                        studentID,
                        title,
                        type,
                        targetDate,
                        targetHours,
                        completedHours,
                        status
                );


        boolean added =
                GoalManager.addGoal(goal);

        if (added) {

            sendResponse(
                    exchange,
                    "Goal added successfully!"
            );

        } else {

            sendResponse(
                    exchange,
                    "Failed to add goal."
            );
        }
    } catch (NumberFormatException e) {

        sendResponse(
                exchange,
                "Invalid number entered."
        );
    }
});


// Get Goals - Goal management code - API - List goals
server.createContext("/api/goals/list", exchange -> {

    if (!exchange.getRequestMethod().equals("GET")) {

        sendResponse(
                exchange,
                "Invalid request."
        );

        return;
    }

    Integer studentID = getLoggedInStudentID(exchange);

    if (studentID == null) {
        sendResponse(
                exchange,
                "Please login first."
        );
        return;
    }

    List<Goal> goals = GoalManager.getGoalsByStudent(studentID);

    StringBuilder response = new StringBuilder();

    for (Goal goal : goals) {

        response.append(
                goal.getGoalID()
        ).append("|");

        response.append(
                goal.getGoalTitle()
        ).append("|");

        response.append(
                goal.getGoalType()
        ).append("|");

        response.append(
                goal.getTargetDate()
        ).append("|");

        response.append(
                goal.getTargetHours()
        ).append("|");

        response.append(
                goal.getCompletedHours()
        ).append("|");

        response.append(
                goal.getStatus()
        ).append("|");

        response.append(
                goal.getProgressPercentage()
        ).append("\n");
    }
    sendResponse(
            exchange,
            response.toString()
    );
});


// Update Goal Progress - Goal management code - API - Update goal
server.createContext("/api/goals/update", exchange -> {

    if (!exchange.getRequestMethod().equals("POST")) {

        sendResponse(
                exchange,
                "Invalid request."
        );
        return;
    }

    Integer studentID = getLoggedInStudentID(exchange);
    if (studentID == null) {

        sendResponse(
                exchange,
                "Please login first."
        );

        return;
    }

    String data =
            new String(
                    exchange.getRequestBody().readAllBytes(),
                    StandardCharsets.UTF_8
            );

    try {
        int goalID =
                Integer.parseInt(
                        getValue(data, "goalID")
                );

        double completedHours =
                Double.parseDouble(
                        getValue(data, "completedHours")
                );

        GoalManager.getGoalsByStudent(studentID);

        String status = "In Progress";

        List<Goal> goals = GoalManager.getGoalsByStudent(studentID);

        for (Goal goal : goals) {
            if (goal.getGoalID() == goalID) {

                if (completedHours >= goal.getTargetHours()) {
                    status = "Completed";
                }
                break;
            }
        }

        boolean updated =
                GoalManager.updateGoalProgress(
                        goalID,
                        studentID,
                        completedHours,
                        status
                );

        if (updated) {
            sendResponse(
                    exchange,
                    "Goal updated successfully!"
            );

        } else {
            sendResponse(
                    exchange,
                    "Goal not found."
            );
        }
    } catch (Exception e) {

        sendResponse(
                exchange,
                "Invalid goal data."
        );
    }
});

// Delete Goal - Goal management code - API - Delete goal
server.createContext("/api/goals/delete", exchange -> {

    if (!exchange.getRequestMethod().equals("POST")) {

        sendResponse(
                exchange,
                "Invalid request."
        );

        return;
    }

    Integer studentID =
            getLoggedInStudentID(exchange);

    if (studentID == null) {

        sendResponse(
                exchange,
                "Please login first."
        );

        return;
    }

    String data =
            new String(
                    exchange.getRequestBody().readAllBytes(),
                    StandardCharsets.UTF_8
            );

    try {

        int goalID =
                Integer.parseInt(
                        getValue(data, "goalID")
                );

        boolean deleted =
                GoalManager.deleteGoal(
                        goalID,
                        studentID
                );

        if (deleted) {

            sendResponse(
                    exchange,
                    "Goal deleted successfully!"
            );

        } else {

            sendResponse(
                    exchange,
                    "Goal not found."
            );
        }

    } catch (Exception e) {

        sendResponse(
                exchange,
                "Invalid goal ID."
        );
    }
});


//FLASHCARD MANAGEMENT CODE     
// ADD: Create a flashcard
server.createContext("/api/flashcards/add", exchange -> {
        Integer studentID = getLoggedInStudentID(exchange);

        if (studentID == null) {
        exchange.sendResponseHeaders(401, -1);
        exchange.close();
        return;
        }

        String data = new String(
        exchange.getRequestBody().readAllBytes(),
        java.nio.charset.StandardCharsets.UTF_8
        );

        String question = getValue(data, "question");
        String answer = getValue(data, "answer");

        if (question == null || answer == null ||
        question.trim().isEmpty() || answer.trim().isEmpty()) {
        byte[] response = "Please enter both question and answer."
                .getBytes(java.nio.charset.StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(400, response.length);
        exchange.getResponseBody().write(response);
        exchange.close();
        return;
        }

        Flashcard flashcard = new Flashcard(
        studentID, question, answer
        );

        String response = FlashcardManager.addFlashcard(flashcard)
        ? "success" : "error";

        byte[] bytes = response.getBytes(
        java.nio.charset.StandardCharsets.UTF_8
        );

        exchange.sendResponseHeaders(
        response.equals("success") ? 200 : 500, bytes.length
        );
        exchange.getResponseBody().write(bytes);
        exchange.close();
});

// READ: Get the logged-in student's flashcards
server.createContext("/api/flashcards/list", exchange -> {
        Integer studentID = getLoggedInStudentID(exchange);

        if (studentID == null) {
        exchange.sendResponseHeaders(401, -1);
        exchange.close();
        return;
        }

        List<Flashcard> flashcards =
        FlashcardManager.getFlashcardsByStudent(studentID);

        StringBuilder response = new StringBuilder();

        for (Flashcard card : flashcards) {
        response.append(card.getFlashcardID()).append("|")
                .append(java.net.URLEncoder.encode(
                card.getQuestion(), "UTF-8")).append("|")
                .append(java.net.URLEncoder.encode(
                card.getAnswer(), "UTF-8")).append("\n");
        }

        byte[] bytes = response.toString().getBytes(
        java.nio.charset.StandardCharsets.UTF_8
        );

        exchange.getResponseHeaders().set(
        "Content-Type", "text/plain; charset=UTF-8"
        );
        exchange.sendResponseHeaders(200, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
});

// UPDATE: Edit a flashcard
server.createContext("/api/flashcards/update", exchange -> {
        Integer studentID = getLoggedInStudentID(exchange);

        if (studentID == null) {
        exchange.sendResponseHeaders(401, -1);
        exchange.close();
        return;
        }

        String data = new String(
        exchange.getRequestBody().readAllBytes(),
        java.nio.charset.StandardCharsets.UTF_8
        );

        String idText = getValue(data, "flashcardID");
        String question = getValue(data, "question");
        String answer = getValue(data, "answer");

        boolean success = false;

        try {
        if (idText != null && question != null && answer != null
                && !question.trim().isEmpty()
                && !answer.trim().isEmpty()) {

                int flashcardID = Integer.parseInt(idText);

                success = FlashcardManager.updateFlashcard(
                flashcardID, studentID, question, answer
                );
        }
        } catch (NumberFormatException e) {
        success = false;
        }

        byte[] bytes = (success ? "success" : "error").getBytes(
        java.nio.charset.StandardCharsets.UTF_8
        );

        exchange.sendResponseHeaders(success ? 200 : 400, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
});

// DELETE: Remove a flashcard
server.createContext("/api/flashcards/delete", exchange -> {
        Integer studentID = getLoggedInStudentID(exchange);

        if (studentID == null) {
        exchange.sendResponseHeaders(401, -1);
        exchange.close();
        return;
        }

        String data = new String(
        exchange.getRequestBody().readAllBytes(),
        java.nio.charset.StandardCharsets.UTF_8
        );

        String idText = getValue(data, "flashcardID");
        boolean success = false;

        try {
        if (idText != null) {
                int flashcardID = Integer.parseInt(idText);

                success = FlashcardManager.deleteFlashcard(
                flashcardID, studentID
                );
        }
        } catch (NumberFormatException e) {
        success = false;
        }

        byte[] bytes = (success ? "success" : "error").getBytes(
        java.nio.charset.StandardCharsets.UTF_8
        );

        exchange.sendResponseHeaders(success ? 200 : 400, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
});


//NOTEBOOK FILE MANAGEMENT CODE
 // UPLOAD: Save a study file
server.createContext("/api/files/upload", exchange -> {

    if (!exchange.getRequestMethod().equals("POST")) {
        sendResponse(exchange, "Invalid request.");
        return;
    }

    Integer studentID = getLoggedInStudentID(exchange);

    if (studentID == null) {
        sendResponse(exchange, "Please login first.");
        return;
    }

    try {
        String contentType = exchange.getRequestHeaders()
                .getFirst("Content-Type");

        if (contentType == null ||
                !contentType.toLowerCase().startsWith("multipart/form-data")) {
            sendResponse(exchange, "Please select a file to upload.");
            return;
        }

        // Limit uploads to approximately 10 MB
        String lengthHeader = exchange.getRequestHeaders()
                .getFirst("Content-Length");

        if (lengthHeader != null &&
                Long.parseLong(lengthHeader) > 10 * 1024 * 1024 + 65536) {
            sendResponse(exchange, "File is too large. Maximum size is 10 MB.");
            return;
        }

        byte[] requestData = exchange.getRequestBody().readAllBytes();

        if (requestData.length > 10 * 1024 * 1024 + 65536) {
            sendResponse(exchange, "File is too large. Maximum size is 10 MB.");
            return;
        }

        Map<String, MultipartPart> parts =
                parseMultipart(requestData, contentType);

        MultipartPart titlePart = parts.get("title");
        MultipartPart filePart = parts.get("file");

        String title = titlePart == null ? "" :
                new String(titlePart.data, StandardCharsets.UTF_8).trim();

        if (title.isBlank() || filePart == null ||
                filePart.data.length == 0 || filePart.filename == null ||
                filePart.filename.isBlank()) {
            sendResponse(exchange, "Please enter a title and select a file.");
            return;
        }

        String originalName = Path.of(filePart.filename)
                .getFileName().toString();

        String lowerName = originalName.toLowerCase();
        String extension = "";

        int dot = lowerName.lastIndexOf('.');
        if (dot >= 0) {
            extension = lowerName.substring(dot);
        }

        if (!extension.matches("\\.(pdf|png|jpg|jpeg)")) {
            sendResponse(exchange, "Only PDF, PNG, JPG and JPEG files are allowed.");
            return;
        }

        Path uploadDirectory = Path.of("storage", "uploads");
        Files.createDirectories(uploadDirectory);

        // Generate a unique stored name instead of trusting the uploaded name
        String storedName = UUID.randomUUID() + extension;
        Path savedPath = uploadDirectory.resolve(storedName);

        Files.write(savedPath, filePart.data);

        String savedType = getUploadedFileType(extension);

        StudyFile file = new StudyFile(
                0,
                studentID,
                title,
                originalName,
                storedName,
                savedType
        );

        if (StudyFileManager.addFile(file)) {
            sendResponse(exchange, "success");
        } else {
            Files.deleteIfExists(savedPath);
            sendResponse(exchange, "Could not save file details.");
        }

    } catch (Exception e) {
        e.printStackTrace();
        sendResponse(exchange, "File upload failed.");
    }
});


// READ: List the logged-in student's files
server.createContext("/api/files/list", exchange -> {

    if (!exchange.getRequestMethod().equals("GET")) {
        sendResponse(exchange, "Invalid request.");
        return;
    }

    Integer studentID = getLoggedInStudentID(exchange);

    if (studentID == null) {
        sendResponse(exchange, "Please login first.");
        return;
    }

    List<StudyFile> files =
            StudyFileManager.getFilesByStudent(studentID);

    StringBuilder response = new StringBuilder();

    for (StudyFile file : files) {
        response.append(file.getFileID()).append("|")
                .append(java.net.URLEncoder.encode(
                        file.getTitle(), StandardCharsets.UTF_8)).append("|")
                .append(java.net.URLEncoder.encode(
                        file.getOriginalName(), StandardCharsets.UTF_8)).append("|")
                .append(java.net.URLEncoder.encode(
                        file.getFileType(), StandardCharsets.UTF_8)).append("\n");
    }

    exchange.getResponseHeaders().set(
            "Content-Type", "text/plain; charset=UTF-8");

    sendResponse(exchange, response.toString());
});


// OPEN: Open a file owned by the logged-in student

 // OPEN: Open a saved file belonging to the logged-in student
server.createContext("/api/files/open", exchange -> {

    Integer studentID = getLoggedInStudentID(exchange);

    if (studentID == null) {
        exchange.sendResponseHeaders(401, -1);
        exchange.close();
        return;
    }

    if (!exchange.getRequestMethod().equals("GET")) {
        exchange.sendResponseHeaders(405, -1);
        exchange.close();
        return;
    }

    // Get the file ID from the URL
    String query = exchange.getRequestURI().getRawQuery();
    String idText = null;

    if (query != null) {
        for (String parameter : query.split("&")) {
            String[] parts = parameter.split("=", 2);

            if (parts.length == 2 && parts[0].equals("id")) {
                idText = parts[1];
                break;
            }
        }
    }

    int fileID;

    try {
        fileID = Integer.parseInt(idText);
    } catch (Exception e) {
        exchange.sendResponseHeaders(400, -1);
        exchange.close();
        return;
    }

    // Find the file among this student's files
    StudyFile selectedFile = null;

    List<StudyFile> studentFiles =
            StudyFileManager.getFilesByStudent(studentID);

    for (StudyFile file : studentFiles) {
        if (file.getFileID() == fileID) {
            selectedFile = file;
            break;
        }
    }

    if (selectedFile == null) {
        exchange.sendResponseHeaders(404, -1);
        exchange.close();
        return;
    }

    try {
        // Locate the actual uploaded file
        Path uploadFolder = Path.of("storage", "uploads")
                .toAbsolutePath().normalize();

        Path filePath = uploadFolder
                .resolve(selectedFile.getStoredName())
                .normalize();

        // Prevent access outside the upload folder
        if (!filePath.startsWith(uploadFolder)
                || !Files.isRegularFile(filePath)) {
            exchange.sendResponseHeaders(404, -1);
            exchange.close();
            return;
        }

        // Allow only supported file types
        String originalName = selectedFile.getOriginalName()
                .toLowerCase(java.util.Locale.ROOT);

        String contentType;

        if (originalName.endsWith(".pdf")) {
            contentType = "application/pdf";
        } else if (originalName.endsWith(".png")) {
            contentType = "image/png";
        } else if (originalName.endsWith(".jpg")
                || originalName.endsWith(".jpeg")) {
            contentType = "image/jpeg";
        } else {
            exchange.sendResponseHeaders(415, -1);
            exchange.close();
            return;
        }

        byte[] fileBytes = Files.readAllBytes(filePath);

        exchange.getResponseHeaders().set(
                "Content-Type", contentType
        );

        exchange.getResponseHeaders().set(
                "Content-Disposition", "inline"
        );

        exchange.getResponseHeaders().set(
                "X-Content-Type-Options", "nosniff"
        );

        exchange.sendResponseHeaders(200, fileBytes.length);

        try (OutputStream output = exchange.getResponseBody()) {
            output.write(fileBytes);
        }

    } catch (IOException e) {
        e.printStackTrace();

        if (exchange.getResponseCode() == -1) {
            exchange.sendResponseHeaders(500, -1);
        }

        exchange.close();
    }
});


// DELETE: Remove a file belonging to the logged-in student
server.createContext("/api/files/delete", exchange -> {

    if (!exchange.getRequestMethod().equals("POST")) {
        sendResponse(exchange, "Invalid request.");
        return;
    }

    Integer studentID = getLoggedInStudentID(exchange);

    if (studentID == null) {
        sendResponse(exchange, "Please login first.");
        return;
    }

    try {
        String data = new String(
                exchange.getRequestBody().readAllBytes(),
                StandardCharsets.UTF_8);

        int fileID = Integer.parseInt(getValue(data, "fileID"));

        StudyFile selectedFile = null;

        for (StudyFile file :
                StudyFileManager.getFilesByStudent(studentID)) {
            if (file.getFileID() == fileID) {
                selectedFile = file;
                break;
            }
        }

        if (selectedFile == null) {
            sendResponse(exchange, "File not found.");
            return;
        }

        boolean deleted = StudyFileManager.deleteFile(fileID, studentID);

        if (deleted) {
            Path filePath = Path.of("storage", "uploads", selectedFile.getStoredName());

            Files.deleteIfExists(filePath);
            sendResponse(exchange, "success");
        } else {
            sendResponse(exchange, "Could not delete file.");
        }

    } catch (Exception e) {
        sendResponse(exchange, "Invalid file ID.");
    }
});

//QUIZ

server.createContext("/api/quizzes/list", exchange -> {

    // Only logged-in students can view quizzes
    Integer studentID = getLoggedInStudentID(exchange);

    if (studentID == null) {
        exchange.sendResponseHeaders(401, -1);
        exchange.close();
        return;
    }

    // Retrieve all quizzes from the database
    List<Quiz> quizzes = QuizManager.getAllQuizzes();

    // Build a JSON response
    StringBuilder response = new StringBuilder();
    response.append("[");

    for (int i = 0; i < quizzes.size(); i++) {

        Quiz quiz = quizzes.get(i);

        if (i > 0) {
            response.append(",");
        }

        response.append("{");
        response.append("\"quizID\":")
                .append(quiz.getQuizID()).append(",");

        response.append("\"studentID\":");
        if (quiz.getStudentID() == null) {
            response.append("null");
        } else {
            response.append(quiz.getStudentID());
        }
        response.append(",");
        response.append("\"isOwner\":")
                .append(quiz.getStudentID() != null
                        && quiz.getStudentID().equals(studentID))
                .append(",");

        response.append("\"title\":\"")
                .append(escapeJson(quiz.getTitle())).append("\",");

        response.append("\"description\":\"")
                .append(escapeJson(quiz.getDescription() == null
                        ? "" : quiz.getDescription()))
                .append("\"");

        response.append("}");
    }

    response.append("]");

    // Send the JSON response
    byte[] bytes = response.toString().getBytes(
            java.nio.charset.StandardCharsets.UTF_8
    );

    exchange.getResponseHeaders().set(
            "Content-Type", "application/json; charset=UTF-8"
    );

    exchange.sendResponseHeaders(200, bytes.length);

    try (java.io.OutputStream os = exchange.getResponseBody()) {
        os.write(bytes);
    }
});


// Create a new quiz
server.createContext("/api/quizzes/create", exchange -> {

    // Only logged-in students can create quizzes
    Integer studentID = getLoggedInStudentID(exchange);

    if (studentID == null) {
        exchange.sendResponseHeaders(401, -1);
        exchange.close();
        return;
    }

    // Read the request body
    String data = new String(
        exchange.getRequestBody().readAllBytes(),
        java.nio.charset.StandardCharsets.UTF_8
    );

    // Get quiz details from the request
    String title = getValue(data, "title");
    String description = getValue(data, "description");

    // Validate the title
    if (title == null || title.trim().isEmpty()) {
        sendResponse(exchange, "Quiz title is required.");
        return;
    }

    // Create the quiz in the database
    int quizID = QuizManager.createQuiz(
        studentID,
        title.trim(),
        description == null ? "" : description.trim()
    );

    if (quizID != -1) {
        sendResponse(exchange, "Quiz created successfully. ID: " + quizID);
    } else {
        sendResponse(exchange, "Failed to create quiz.");
    }
});

// Add a question to a quiz
server.createContext("/api/quizzes/questions/add", exchange -> {

    // Check whether the student is logged in
    Integer studentID = getLoggedInStudentID(exchange);

    if (studentID == null) {
        exchange.sendResponseHeaders(401, -1);
        exchange.close();
        return;
    }

    // Read the request data
    String data = new String(
        exchange.getRequestBody().readAllBytes(),
        java.nio.charset.StandardCharsets.UTF_8
    );

    // Get question details
    String quizIDText = getValue(data, "quizID");
    String question = getValue(data, "question");
    String optionA = getValue(data, "optionA");
    String optionB = getValue(data, "optionB");
    String optionC = getValue(data, "optionC");
    String optionD = getValue(data, "optionD");
    String correctOption = getValue(data, "correctOption");

    // Validate the quiz ID
    int quizID;

    try {
        quizID = Integer.parseInt(quizIDText);
    } catch (Exception e) {
        sendResponse(exchange, "Invalid quiz ID.");
        return;
    }

    // Only the quiz creator can add questions
    if (!QuizManager.isQuizOwner(quizID, studentID)) {
        exchange.sendResponseHeaders(403, -1);
        exchange.close();
        return;
    }

    // Validate all question fields
    if (question == null || question.trim().isEmpty()
            || optionA == null || optionA.trim().isEmpty()
            || optionB == null || optionB.trim().isEmpty()
            || optionC == null || optionC.trim().isEmpty()
            || optionD == null || optionD.trim().isEmpty()
            || correctOption == null) {

        sendResponse(exchange, "Please fill in the question and all four options.");
        return;
    }

    correctOption = correctOption.trim().toUpperCase();

    if (!correctOption.equals("A")
            && !correctOption.equals("B")
            && !correctOption.equals("C")
            && !correctOption.equals("D")) {

        sendResponse(exchange, "Correct option must be A, B, C, or D.");
        return;
    }

    // Save the question
    boolean success = QuizManager.addQuestion(
        quizID,
        question.trim(),
        optionA.trim(),
        optionB.trim(),
        optionC.trim(),
        optionD.trim(),
        correctOption
    );

    if (success) {
        sendResponse(exchange, "Question added successfully.");
    } else {
        sendResponse(exchange, "Failed to add question.");
    }
});


// Get questions for a quiz
server.createContext("/api/quizzes/questions", exchange -> {

    // Only logged-in students can view questions
    Integer studentID = getLoggedInStudentID(exchange);

    if (studentID == null) {
        exchange.sendResponseHeaders(401, -1);
        exchange.close();
        return;
    }

    // Read the quiz ID from the URL
    String path = exchange.getRequestURI().getRawQuery();

    String quizIDText = null;

    if (path != null && path.startsWith("id=")) {
        quizIDText = path.substring(3);
    }

    int quizID;

    try {
        quizID = Integer.parseInt(quizIDText);
    } catch (Exception e) {
        sendResponse(exchange, "Invalid quiz ID.");
        return;
    }

    // Retrieve the questions
    List<Question> questions = QuizManager.getQuestions(quizID);

    // Build JSON without sending correct answers
    StringBuilder response = new StringBuilder("[");
    
    for (int i = 0; i < questions.size(); i++) {

        Question question = questions.get(i);

        if (i > 0) {
            response.append(",");
        }

        response.append("{");
        response.append("\"questionID\":")
                .append(question.getQuestionID()).append(",");
        response.append("\"question\":\"")
                .append(escapeJson(question.getQuestion())).append("\",");
        response.append("\"optionA\":\"")
                .append(escapeJson(question.getOptionA())).append("\",");
        response.append("\"optionB\":\"")
                .append(escapeJson(question.getOptionB())).append("\",");
        response.append("\"optionC\":\"")
                .append(escapeJson(question.getOptionC())).append("\",");
        response.append("\"optionD\":\"")
                .append(escapeJson(question.getOptionD())).append("\"");
        response.append("}");
    }

    response.append("]");

    byte[] bytes = response.toString().getBytes(
        java.nio.charset.StandardCharsets.UTF_8
    );

    exchange.getResponseHeaders().set(
        "Content-Type", "application/json; charset=UTF-8"
    );

    exchange.sendResponseHeaders(200, bytes.length);

    try (java.io.OutputStream os = exchange.getResponseBody()) {
        os.write(bytes);
    }
});


// Submit quiz answers and calculate score
server.createContext("/api/quizzes/submit", exchange -> {

    // Check login
    Integer studentID = getLoggedInStudentID(exchange);

    if (studentID == null) {
        exchange.sendResponseHeaders(401, -1);
        exchange.close();
        return;
    }

    // Read submitted data
    String data = new String(
        exchange.getRequestBody().readAllBytes(),
        java.nio.charset.StandardCharsets.UTF_8
    );

    String quizIDText = getValue(data, "quizID");
    String answersText = getValue(data, "answers");

    int quizID;

    try {
        quizID = Integer.parseInt(quizIDText);
    } catch (Exception e) {
        sendResponse(exchange, "Invalid quiz ID.");
        return;
    }

    if (answersText == null || answersText.trim().isEmpty()) {
        sendResponse(exchange, "No answers received.");
        return;
    }

    // Expected answer format:
    // 1:A,2:C,3:B
    java.util.Map<Integer, String> answers =
        new java.util.HashMap<>();

    try {
        String[] answerPairs = answersText.split(",");

        for (String pair : answerPairs) {

            String[] parts = pair.split(":");

            if (parts.length != 2) {
                sendResponse(exchange, "Invalid answer format.");
                return;
            }

            int questionID = Integer.parseInt(parts[0].trim());
            String answer = parts[1].trim().toUpperCase();

            if (!answer.equals("A")
                    && !answer.equals("B")
                    && !answer.equals("C")
                    && !answer.equals("D")) {
                sendResponse(exchange, "Invalid answer option.");
                return;
            }

            answers.put(questionID, answer);
        }

    } catch (Exception e) {
        sendResponse(exchange, "Invalid answer data.");
        return;
    }

    // Calculate score on the server
    int score = QuizManager.calculateScore(quizID, answers);

    if (score < 0) {
        sendResponse(exchange, "Unable to calculate score.");
        return;
    }

    int totalQuestions = QuizManager.getQuestions(quizID).size();

    sendResponse(
        exchange,
        "Score: " + score + "/" + totalQuestions
    );
});



// Serve frontend files
server.createContext("/", exchange -> {

        String requestPath =
                exchange.getRequestURI().getPath();

        if (requestPath.equals("/")) {
        requestPath = "/index.html";
        }

        Path filePath = Path.of(
                "frontend" + requestPath
        );

        if (!Files.exists(filePath) || Files.isDirectory(filePath)) {

        sendResponse(
                exchange,
                "File not found."
        );

        return;
        }

        byte[] response = Files.readAllBytes(filePath);

        String contentType = getContentType(requestPath);

        exchange.getResponseHeaders().set(
                "Content-Type",
                contentType
        );

        exchange.sendResponseHeaders(
                200,
                response.length
        );

        OutputStream output = exchange.getResponseBody();

        output.write(response);
        output.close();
});

server.start();

System.out.println("Learnify server started!");
System.out.println("Open http://localhost:8000");
}


//FILE MANAGEMENT NOTEBOOK
 // Represents one part of an uploaded multipart request
private static class MultipartPart {
    String filename;
    byte[] data;

    MultipartPart(String filename, byte[] data) {
        this.filename = filename;
        this.data = data;
    }
}


// Read the title and file from a multipart upload
private static Map<String, MultipartPart> parseMultipart(
        byte[] requestData, String contentType) throws IOException {

    Map<String, MultipartPart> parts = new HashMap<>();

    Matcher boundaryMatcher = Pattern.compile(
            "boundary=(?:\"([^\"]+)\"|([^;\\s]+))",
            Pattern.CASE_INSENSITIVE
    ).matcher(contentType);

    if (!boundaryMatcher.find()) {
        throw new IOException("Missing multipart boundary.");
    }

    String boundary = boundaryMatcher.group(1) != null
            ? boundaryMatcher.group(1)
            : boundaryMatcher.group(2);

    // ISO-8859-1 preserves each original byte when converted back
    String body = new String(
            requestData, StandardCharsets.ISO_8859_1);

    String[] sections = body.split(
            Pattern.quote("--" + boundary));

    for (String section : sections) {

        if (section.equals("--") || section.equals("--\r\n")) {
            continue;
        }

        if (section.startsWith("\r\n")) {
            section = section.substring(2);
        }

        int headerEnd = section.indexOf("\r\n\r\n");

        if (headerEnd < 0) {
            continue;
        }

        String headers = section.substring(0, headerEnd);
        String content = section.substring(headerEnd + 4);

        if (content.endsWith("\r\n")) {
            content = content.substring(0, content.length() - 2);
        }

        Matcher nameMatcher = Pattern.compile(
                "name=\"([^\"]+)\"").matcher(headers);

        if (!nameMatcher.find()) {
            continue;
        }

        String name = nameMatcher.group(1);

        Matcher filenameMatcher = Pattern.compile(
                "filename=\"([^\"]*)\"").matcher(headers);

        String filename = filenameMatcher.find()
                ? filenameMatcher.group(1)
                : null;

        byte[] data = content.getBytes(
                StandardCharsets.ISO_8859_1);

        parts.put(name, new MultipartPart(filename, data));
    }

    return parts;
}


// Determine content type from the allowed extension
private static String getUploadedFileType(String extension) {

    switch (extension.toLowerCase()) {
        case ".pdf":
            return "application/pdf";

        case ".png":
            return "image/png";

        case ".jpg":
        case ".jpeg":
            return "image/jpeg";

        default:
            return "application/octet-stream";
    }
}


// Read a string value from a JSON request body.
// Supports escaped quotation marks, backslashes and common newline escapes.
private static String getValue(String data, String key) {

    String search = "\"" + key + "\"";
    int keyPosition = data.indexOf(search);

    if (keyPosition == -1) {
        return null;
    }

    int colon = data.indexOf(":", keyPosition + search.length());

    if (colon == -1) {
        return null;
    }

    int start = colon + 1;

    while (start < data.length()
            && Character.isWhitespace(data.charAt(start))) {
        start++;
    }

    // This helper is intended for JSON string values.
    if (start >= data.length() || data.charAt(start) != '"') {
        return null;
    }

    start++;
    StringBuilder value = new StringBuilder();
    boolean escaped = false;

    for (int i = start; i < data.length(); i++) {
        char c = data.charAt(i);

        if (escaped) {
            switch (c) {
                case '"': value.append('"'); break;
                case '\\': value.append('\\'); break;
                case 'n': value.append('\n'); break;
                case 'r': value.append('\r'); break;
                case 't': value.append('\t'); break;
                case 'b': value.append('\b'); break;
                case 'f': value.append('\f'); break;
                default: value.append(c); break;
            }
            escaped = false;
        } else if (c == '\\') {
            escaped = true;
        } else if (c == '"') {
            return value.toString();
        } else {
            value.append(c);
        }
    }

    return null;
}

// Escape text so it can safely be included inside a JSON string.
private static String escapeJson(String value) {
    if (value == null) {
        return "";
    }

    StringBuilder escaped = new StringBuilder();

    for (int i = 0; i < value.length(); i++) {
        char c = value.charAt(i);

        switch (c) {
            case '"': escaped.append("\\\""); break;
            case '\\': escaped.append("\\\\"); break;
            case '\n': escaped.append("\\n"); break;
            case '\r': escaped.append("\\r"); break;
            case '\t': escaped.append("\\t"); break;
            case '\b': escaped.append("\\b"); break;
            case '\f': escaped.append("\\f"); break;
            default:
                if (c < 0x20) {
                    escaped.append(String.format("\\u%04x", (int) c));
                } else {
                    escaped.append(c);
                }
        }
    }

    return escaped.toString();
}

// Send text response
private static void sendResponse(
        HttpExchange exchange,
        String message)
        throws IOException {

byte[] response =
        message.getBytes(StandardCharsets.UTF_8);

exchange.getResponseHeaders().set(
        "Content-Type",
        "text/plain"
);

exchange.sendResponseHeaders(
        200,
        response.length
);

OutputStream output = exchange.getResponseBody();

output.write(response);
output.close();
}

// Decide file type
private static String getContentType(
        String fileName) {

if (fileName.endsWith(".html")) {
        return "text/html";
}

if (fileName.endsWith(".css")) {
        return "text/css";
}

if (fileName.endsWith(".js")) {
        return "application/javascript";
}

return "text/plain";
}
}