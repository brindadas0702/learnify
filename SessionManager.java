//This class manages the sessions for students. 
// It provides methods to create, retrieve, and destroy sessions.

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

public class SessionManager {

    private static final Map<String, Integer> sessions = new ConcurrentHashMap<>();

    // Create a session for a student
    public static String createSession(int studentID) {

        String sessionID = UUID.randomUUID().toString();

        sessions.put(sessionID, studentID);
        
        return sessionID;
    }

    // Find the student using the session ID
    public static Integer getStudentID(String sessionID) {
        return sessions.get(sessionID);
    }

    // Destroy a session during logout
    public static void destroySession(String sessionID) {
        sessions.remove(sessionID);
    }
}
