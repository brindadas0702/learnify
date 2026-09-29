package util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

public class PasswordHash {

    public static String hashPassword(String password) {

        try {

            MessageDigest md =
                    MessageDigest.getInstance("SHA-256");

            byte[] hash =
                    md.digest(password.getBytes(StandardCharsets.UTF_8));

            StringBuilder result = new StringBuilder();

            for (byte b : hash) {

                result.append(
                        String.format("%02x", b)
                );
            }

            return result.toString();

        } catch (Exception e) {

            e.printStackTrace();
            return null;
        }
    }
}