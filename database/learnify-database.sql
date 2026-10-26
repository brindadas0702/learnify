CREATE DATABASE learnify;

USE learnify;

CREATE TABLE students (
    StudentID INT PRIMARY KEY AUTO_INCREMENT,
    StudentName VARCHAR(100) NOT NULL,
    EmailID VARCHAR(100) NOT NULL UNIQUE,
    Password VARCHAR(255) NOT NULL
);

CREATE TABLE IF NOT EXISTS goals (
    GoalID INT PRIMARY KEY AUTO_INCREMENT,
    StudentID INT,
    GoalTitle VARCHAR(255) NOT NULL,
    GoalType VARCHAR(20) NOT NULL,
    TargetDate DATE NOT NULL,
    TargetHours DECIMAL(5,2) NOT NULL,
    CompletedHours DECIMAL(5,2) DEFAULT 0.00,
    Status VARCHAR(20) DEFAULT 'In Progress',
    FOREIGN KEY (StudentID) REFERENCES students(StudentID) ON DELETE CASCADE
);
 
CREATE TABLE IF NOT EXISTS flashcards (
    FlashcardID INT PRIMARY KEY AUTO_INCREMENT,
    StudentID INT NOT NULL,
    Question TEXT NOT NULL,
    Answer TEXT NOT NULL,
    FOREIGN KEY (StudentID)
        REFERENCES students(StudentID)
        ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS notes (
    NoteID INT PRIMARY KEY AUTO_INCREMENT,
    StudentID INT NOT NULL,
    Title VARCHAR(200) NOT NULL,
    Subject VARCHAR(100) NOT NULL,
    Content TEXT NOT NULL,
    CreatedAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UpdatedAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (StudentID)
        REFERENCES students(StudentID)
        ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS study_files (
    FileID INT PRIMARY KEY AUTO_INCREMENT,
    StudentID INT NOT NULL,
    Title VARCHAR(200) NOT NULL,
    OriginalName VARCHAR(255) NOT NULL,
    StoredName VARCHAR(255) NOT NULL,
    FileType VARCHAR(100),
    UploadedAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    FOREIGN KEY (StudentID)
        REFERENCES students(StudentID)
        ON DELETE CASCADE
);

select * from students;
select * from goals;
select * from flashcards;
select * from notes;
select * from study_files;