package model;

public class Student {

    private int studentID;
    private String studentName;
    private String emailID;
    private String password;

    public Student() {
    }

    public Student(String studentName, String emailID, String password) {
        this.studentName = studentName;
        this.emailID = emailID;
        this.password = password;
    }

    public int getStudentID() {
        return studentID;
    }

    public void setStudentID(int studentID) {
        this.studentID = studentID;
    }

    public String getStudentName() {
        return studentName;
    }

    public void setStudentName(String studentName) {
        this.studentName = studentName;
    }

    public String getEmailID() {
        return emailID;
    }

    public void setEmailID(String emailID) {
        this.emailID = emailID;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}