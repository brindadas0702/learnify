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
import java.net.URLDecoder;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

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
server.createContext("/api/files/open", exchange -> {

    if (!exchange.getRequestMethod().equals("GET")) {
        sendResponse(exchange, "Invalid request.");
        return;
    }

    Integer studentID = getLoggedInStudentID(exchange);

    if (studentID == null) {
        sendResponse(exchange, "Please login first.");
        return;
    }

    try {
        String idText = getQueryValue(exchange, "fileID");
        int fileID = Integer.parseInt(idText);

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

        Path filePath = Path.of("storage", "uploads",
                selectedFile.getStoredName());

        if (!Files.isRegularFile(filePath)) {
            sendResponse(exchange, "Stored file not found.");
            return;
        }

        byte[] fileData = Files.readAllBytes(filePath);

        exchange.getResponseHeaders().set(
                "Content-Type", selectedFile.getFileType());

        exchange.getResponseHeaders().set(
                "Content-Disposition", "inline; filename=\""
                        + selectedFile.getStoredName() + "\"");

        exchange.getResponseHeaders().set(
                "X-Content-Type-Options", "nosniff");

        exchange.sendResponseHeaders(200, fileData.length);

        try (OutputStream output = exchange.getResponseBody()) {
            output.write(fileData);
        }

    } catch (Exception e) {
        sendResponse(exchange, "Could not open file.");
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


// Read a value from the URL query string
private static String getQueryValue(
        HttpExchange exchange, String key) {

    String query = exchange.getRequestURI().getRawQuery();

    if (query == null) {
        return null;
    }

    for (String parameter : query.split("&")) {

        String[] pair = parameter.split("=", 2);

        try {
            String parameterName = URLDecoder.decode(
                    pair[0], StandardCharsets.UTF_8);

            if (parameterName.equals(key) && pair.length == 2) {
                return URLDecoder.decode(
                        pair[1], StandardCharsets.UTF_8);
            }

        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    return null;
}



// Get value from simple JSON
private static String getValue(
        String data,
        String key) {

String search = "\"" + key + "\":\"";

int start = data.indexOf(search);

if (start == -1) {
        return null;
}

start += search.length();

int end = data.indexOf("\"", start);

if (end == -1) {
        return null;
}

return data.substring(start, end);
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