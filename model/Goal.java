package model;
import model.Goal;

public class Goal {

    private int goalID;
    private int studentID;
    private String goalTitle;
    private String goalType;
    private String targetDate;
    private double targetHours;
    private double completedHours;
    private String status;

    public Goal() {
    }

    public Goal(int studentID, String goalTitle, String goalType, String targetDate, double targetHours, double completedHours, String status) {
        this.studentID = studentID;
        this.goalTitle = goalTitle;
        this.goalType = goalType;
        this.targetDate = targetDate;
        this.targetHours = targetHours;
        this.completedHours = completedHours;
        this.status = status;
    }

    public Goal(int goalID, int studentID, String goalTitle, String goalType, String targetDate, double targetHours, double completedHours, String status) {
        this.goalID = goalID;
        this.studentID = studentID;
        this.goalTitle = goalTitle;
        this.goalType = goalType;
        this.targetDate = targetDate;
        this.targetHours = targetHours;
        this.completedHours = completedHours;
        this.status = status;
    }

    public int getGoalID() {
        return goalID;
    }

    public void setGoalID(int goalID) {
        this.goalID = goalID;
    }

    public int getStudentID() {
        return studentID;
    }

    public void setStudentID(int studentID) {
        this.studentID = studentID;
    }

    public String getGoalTitle() {
        return goalTitle;
    }

    public void setGoalTitle(String goalTitle) {
        this.goalTitle = goalTitle;
    }

    public String getGoalType() {
        return goalType;
    }

    public void setGoalType(String goalType) {
        this.goalType = goalType;
    }

    public String getTargetDate() {
        return targetDate;
    }

    public void setTargetDate(String targetDate) {
        this.targetDate = targetDate;
    }

    public double getTargetHours() {
        return targetHours;
    }

    public void setTargetHours(double targetHours) {
        this.targetHours = targetHours;
    }

    public double getCompletedHours() {
        return completedHours;
    }

    public void setCompletedHours(double completedHours) {
        this.completedHours = completedHours;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public double getProgressPercentage() {
        if (targetHours <= 0) {
            return 0.0;
        }
        double percentage = (completedHours / targetHours) * 100.0;
        return Math.min(percentage, 100.0);
    }

    public double getRemainingHours() {
        double remaining = targetHours - completedHours;
        return Math.max(remaining, 0.0);
    }
}
