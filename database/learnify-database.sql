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

CREATE TABLE IF NOT EXISTS quizzes (
    QuizID INT PRIMARY KEY AUTO_INCREMENT,
    StudentID INT NULL,
    Title VARCHAR(200) NOT NULL,
    Description VARCHAR(500),
    CreatedAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    FOREIGN KEY (StudentID)
        REFERENCES students(StudentID)
        ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS quiz_questions (
    QuestionID INT PRIMARY KEY AUTO_INCREMENT,
    QuizID INT NOT NULL,
    Question TEXT NOT NULL,
    OptionA VARCHAR(500) NOT NULL,
    OptionB VARCHAR(500) NOT NULL,
    OptionC VARCHAR(500) NOT NULL,
    OptionD VARCHAR(500) NOT NULL,
    CorrectOption CHAR(1) NOT NULL,

    FOREIGN KEY (QuizID)
        REFERENCES quizzes(QuizID)
        ON DELETE CASCADE,

    CHECK (CorrectOption IN ('A', 'B', 'C', 'D'))
);

select * from students;
select * from goals;
select * from flashcards;
select * from study_files;
select * from quizzes;
select * from quiz_questions;
