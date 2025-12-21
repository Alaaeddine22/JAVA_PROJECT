-- Table to store messages
CREATE TABLE IF NOT EXISTS messages (
    id IDENTITY PRIMARY KEY,
    topic VARCHAR(255) NOT NULL,
    producer_id VARCHAR(255),
    content VARCHAR(1024),
    timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Table for storing message statistics 
CREATE TABLE IF NOT EXISTS message_stats (
    id IDENTITY PRIMARY KEY,
    topic VARCHAR(255) NOT NULL,
    message_count INT DEFAULT 0,
    last_processed TIMESTAMP
);
