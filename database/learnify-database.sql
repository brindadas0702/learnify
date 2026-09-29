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
 
select * from students;

select * from goals;