package model;

public class Quiz {

    private int quizID;
    private Integer studentID;
    private String title;
    private String description;

    public Quiz(int quizID, Integer studentID,
                String title, String description) {
        this.quizID = quizID;
        this.studentID = studentID;
        this.title = title;
        this.description = description;
    }

    public int getQuizID() {
        return quizID;
    }

    public Integer getStudentID() {
        return studentID;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }
}
