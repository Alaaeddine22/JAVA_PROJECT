-- =====================================================
-- STREAMING PLATFORM - DATABASE SEED SCRIPT
-- Run this in XAMPP phpMyAdmin or MySQL CLI
-- =====================================================

-- Create database
CREATE DATABASE IF NOT EXISTS streamingplatform;
USE streamingplatform;

-- =====================================================
-- TABLE: topics
-- =====================================================
DROP TABLE IF EXISTS topics;
CREATE TABLE topics (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) UNIQUE NOT NULL,
    message_count INT DEFAULT 0,
    last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- Insert sample topics
INSERT INTO topics (name, message_count) VALUES
('UserLogins', 2500),
('PaymentEvents', 1800),
('ClickStream', 3200),
('SystemLogs', 1500),
('Notifications', 900),
('Analytics', 750),
('Errors', 350);

-- =====================================================
-- TABLE: ip_topic_stats
-- =====================================================
DROP TABLE IF EXISTS ip_topic_stats;
CREATE TABLE ip_topic_stats (
    id INT AUTO_INCREMENT PRIMARY KEY,
    producer_id VARCHAR(255) NOT NULL,
    topic VARCHAR(255) NOT NULL,
    message_count INT DEFAULT 1,
    last_seen TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY unique_producer_topic (producer_id, topic)
);

-- Insert sample producer/IP statistics
INSERT INTO ip_topic_stats (producer_id, topic, message_count) VALUES
-- Producer 1 posts to 5 topics (MOST ACTIVE)
('Producer-1', 'UserLogins', 500),
('Producer-1', 'PaymentEvents', 300),
('Producer-1', 'ClickStream', 800),
('Producer-1', 'SystemLogs', 200),
('Producer-1', 'Analytics', 150),

-- Producer 2 posts to 4 topics
('Producer-2', 'UserLogins', 400),
('Producer-2', 'ClickStream', 600),
('Producer-2', 'Errors', 100),
('Producer-2', 'Notifications', 250),

-- Producer 3 posts to 3 topics
('Producer-3', 'PaymentEvents', 500),
('Producer-3', 'SystemLogs', 300),
('Producer-3', 'Analytics', 200),

-- Client-1 posts to 2 topics
('Client-1', 'ClickStream', 400),
('Client-1', 'UserLogins', 300),

-- Server-A posts to 2 topics  
('Server-A', 'SystemLogs', 500),
('Server-A', 'Errors', 250),

-- API-Gateway posts to 1 topic
('API-Gateway', 'Notifications', 400);

-- =====================================================
-- VERIFICATION QUERIES
-- =====================================================

-- Query 1: Which IP posts to the most topics?
SELECT 
    producer_id, 
    COUNT(DISTINCT topic) as topic_count, 
    SUM(message_count) as total_messages
FROM ip_topic_stats
GROUP BY producer_id
ORDER BY topic_count DESC, total_messages DESC;

-- Query 2: Messages per topic
SELECT name, message_count 
FROM topics 
ORDER BY message_count DESC;

-- Query 3: Most active producers per topic
SELECT 
    topic,
    producer_id,
    message_count
FROM ip_topic_stats
ORDER BY topic, message_count DESC;
