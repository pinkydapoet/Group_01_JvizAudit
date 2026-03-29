-- 1. Reset the database to ensure a clean slate for the registration system
DROP DATABASE IF EXISTS jvizaudit;
CREATE DATABASE jvizaudit;
USE jvizaudit;

-- 2. Create the Users table (Matches the User.java entity)
CREATE TABLE users (
    user_id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(80) NOT NULL,
    email VARCHAR(120) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL, -- Expanded to 255 to safely hold BCrypt hashes
    role VARCHAR(20) NOT NULL,           -- 'student' or 'instructor'
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 3. Create the Code History table (Matches the CodeHistory.java entity)
CREATE TABLE code_history (
    history_id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    history_name VARCHAR(100) DEFAULT 'Untitled Workspace',
    source_code LONGTEXT,
    language VARCHAR(20) DEFAULT 'Java',
    status VARCHAR(20) DEFAULT 'saved',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
);

-- 4. Create the Diagram Result table
CREATE TABLE diagram_result (
    diagram_id INT AUTO_INCREMENT PRIMARY KEY,
    history_id INT NOT NULL,
    diagram_type VARCHAR(20) NOT NULL,
    mermaid_code LONGTEXT,
    is_valid TINYINT(1) DEFAULT 1,
    error_message LONGTEXT,
    generated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (history_id) REFERENCES code_history(history_id) ON DELETE CASCADE
);

-- 5. Create the Execution Log table
CREATE TABLE execution_log (
    log_id INT AUTO_INCREMENT PRIMARY KEY,
    history_id INT NOT NULL,
    stdin_input LONGTEXT,
    stdout_output LONGTEXT,
    stderr_output LONGTEXT,
    exit_code INT,
    execution_time_ms FLOAT,
    executed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (history_id) REFERENCES code_history(history_id) ON DELETE CASCADE
);

-- 6. Create the Formatter Log table
CREATE TABLE formatter_log (
    fmt_id INT AUTO_INCREMENT PRIMARY KEY,
    history_id INT NOT NULL,
    original_code LONGTEXT,
    formatted_code LONGTEXT,
    success TINYINT(1) DEFAULT 1,
    error_message LONGTEXT,
    formatted_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (history_id) REFERENCES code_history(history_id) ON DELETE CASCADE
);