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

        System.out.println("Signup data received:");
        System.out.println(data);

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