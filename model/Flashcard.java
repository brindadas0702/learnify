package model;

public class Flashcard {

    private int flashcardID;
    private int studentID;
    private String question;
    private String answer;

    public Flashcard() {
    }

    public Flashcard(int studentID, String question, String answer) {
        this.studentID = studentID;
        this.question = question;
        this.answer = answer;
    }

    public Flashcard(int flashcardID, int studentID,
                     String question, String answer) {
        this.flashcardID = flashcardID;
        this.studentID = studentID;
        this.question = question;
        this.answer = answer;
    }

    public int getFlashcardID() {
        return flashcardID;
    }

    public void setFlashcardID(int flashcardID) {
        this.flashcardID = flashcardID;
    }

    public int getStudentID() {
        return studentID;
    }

    public void setStudentID(int studentID) {
        this.studentID = studentID;
    }

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    public String getAnswer() {
        return answer;
    }

    public void setAnswer(String answer) {
        this.answer = answer;
    }
}
