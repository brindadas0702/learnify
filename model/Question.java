
package model;

public class Question {

    private int questionID;
    private int quizID;
    private String question;
    private String optionA;
    private String optionB;
    private String optionC;
    private String optionD;
    private String correctOption;

    public Question(int questionID, int quizID,
                    String question, String optionA,
                    String optionB, String optionC,
                    String optionD, String correctOption) {

        this.questionID = questionID;
        this.quizID = quizID;
        this.question = question;
        this.optionA = optionA;
        this.optionB = optionB;
        this.optionC = optionC;
        this.optionD = optionD;
        this.correctOption = correctOption;
    }

    public int getQuestionID() {
        return questionID;
    }

    public int getQuizID() {
        return quizID;
    }

    public String getQuestion() {
        return question;
    }

    public String getOptionA() {
        return optionA;
    }

    public String getOptionB() {
        return optionB;
    }

    public String getOptionC() {
        return optionC;
    }

    public String getOptionD() {
        return optionD;
    }

    public String getCorrectOption() {
        return correctOption;
    }
}
