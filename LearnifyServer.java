import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpExchange;

import dao.StudentDAO;
import model.Student;
import util.PasswordHash;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class LearnifyServer {

private static String getSessionID(HttpExchange exchange) {

    String cookieHeader =
            exchange.getRequestHeaders().getFirst("Cookie");

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


public static void main(String[] args) throws Exception {

HttpServer server = HttpServer.create(
        new InetSocketAddress(8000),
        0
);

// Signup
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

        sendResponse(
                exchange,
                "Account created successfully!"
        );

        } else {

        sendResponse(
                exchange,
                "Registration failed. Email may already exist."
        );
        }
});


// Login
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


    String email =
            getValue(data, "email");

    String password =
            getValue(data, "password");


    // Check fields

    if (email == null || email.isBlank()
            || password == null || password.isBlank()) {

        sendResponse(
                exchange,
                "Please enter email and password."
        );

        return;
    }


    // Find student

    StudentDAO studentDAO =
            new StudentDAO();

    Student student =
            studentDAO.getStudentByEmail(email);


    // Check if account exists

    if (student == null) {

        sendResponse(
                exchange,
                "Invalid email or password."
        );

        return;
    }


    // Hash entered password

    String hashedPassword =
            PasswordHash.hashPassword(password);


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

        if (!Files.exists(filePath)
                || Files.isDirectory(filePath)) {

        sendResponse(
                exchange,
                "File not found."
        );

        return;
        }

        byte[] response =
                Files.readAllBytes(filePath);

        String contentType =
                getContentType(requestPath);

        exchange.getResponseHeaders().set(
                "Content-Type",
                contentType
        );

        exchange.sendResponseHeaders(
                200,
                response.length
        );

        OutputStream output =
                exchange.getResponseBody();

        output.write(response);
        output.close();
});

server.start();

System.out.println("Learnify server started!");
System.out.println(
        "Open http://localhost:8000/signup.html"
);
}

// Get value from simple JSON
private static String getValue(
        String data,
        String key) {

String search =
        "\"" + key + "\":\"";

int start =
        data.indexOf(search);

if (start == -1) {
        return null;
}

start += search.length();

int end =
        data.indexOf("\"", start);

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

OutputStream output =
        exchange.getResponseBody();

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