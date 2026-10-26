
package model;

public class StudyFile {

    private int fileID;
    private int studentID;
    private String title;
    private String originalName;
    private String storedName;
    private String fileType;

    public StudyFile() {
    }

    public StudyFile(int fileID, int studentID, String title,
                     String originalName, String storedName,
                     String fileType) {
        this.fileID = fileID;
        this.studentID = studentID;
        this.title = title;
        this.originalName = originalName;
        this.storedName = storedName;
        this.fileType = fileType;
    }

    public int getFileID() {
        return fileID;
    }

    public void setFileID(int fileID) {
        this.fileID = fileID;
    }

    public int getStudentID() {
        return studentID;
    }

    public void setStudentID(int studentID) {
        this.studentID = studentID;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getOriginalName() {
        return originalName;
    }

    public void setOriginalName(String originalName) {
        this.originalName = originalName;
    }

    public String getStoredName() {
        return storedName;
    }

    public void setStoredName(String storedName) {
        this.storedName = storedName;
    }

    public String getFileType() {
        return fileType;
    }

    public void setFileType(String fileType) {
        this.fileType = fileType;
    }
}
