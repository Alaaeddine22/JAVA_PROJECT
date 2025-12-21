-- =====================================================
-- STREAMING PLATFORM - H2 DATABASE SEED SCRIPT
-- This will be auto-executed or can be run manually
-- =====================================================

-- Create messages table (if not exists)
CREATE TABLE IF NOT EXISTS messages (
    id IDENTITY PRIMARY KEY,
    topic VARCHAR(255) NOT NULL,
    producer_id VARCHAR(255),
    content VARCHAR(1024),
    timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create statistics table
CREATE TABLE IF NOT EXISTS message_stats (
    id IDENTITY PRIMARY KEY,
    topic VARCHAR(255) NOT NULL,
    message_count INT DEFAULT 0,
    last_processed TIMESTAMP
);

-- =====================================================
-- SAMPLE DATA: 100 Messages across different topics
-- =====================================================

-- UserLogins topic (25 messages)
INSERT INTO messages (topic, producer_id, content) VALUES
('UserLogins', 'Producer-1', 'User john_doe logged in from 192.168.1.10'),
('UserLogins', 'Producer-1', 'User jane_smith logged in from 10.0.0.55'),
('UserLogins', 'Producer-2', 'User admin logged in from 127.0.0.1'),
('UserLogins', 'Client-1', 'User guest123 logged in from 172.16.0.8'),
('UserLogins', 'Producer-1', 'User mike_wilson failed login attempt'),
('UserLogins', 'Producer-2', 'User sarah_jones logged out'),
('UserLogins', 'Client-1', 'User new_user registered'),
('UserLogins', 'Producer-1', 'User john_doe session renewed'),
('UserLogins', 'Producer-2', 'User jane_smith changed password'),
('UserLogins', 'Producer-1', 'User admin granted superuser access'),
('UserLogins', 'Client-1', 'User guest456 logged in from 192.168.2.20'),
('UserLogins', 'Producer-2', 'User test_user created'),
('UserLogins', 'Producer-1', 'User john_doe updated profile'),
('UserLogins', 'Client-1', 'User jane_smith enabled 2FA'),
('UserLogins', 'Producer-2', 'User mike_wilson locked out after 3 attempts'),
('UserLogins', 'Producer-1', 'User sarah_jones requested password reset'),
('UserLogins', 'Client-1', 'User new_user verified email'),
('UserLogins', 'Producer-1', 'User admin viewed audit log'),
('UserLogins', 'Producer-2', 'User guest123 session expired'),
('UserLogins', 'Client-1', 'User john_doe logged in from mobile'),
('UserLogins', 'Producer-1', 'User jane_smith API key generated'),
('UserLogins', 'Producer-2', 'User admin bulk import started'),
('UserLogins', 'Client-1', 'User test_user deleted account'),
('UserLogins', 'Producer-1', 'User mike_wilson account unlocked'),
('UserLogins', 'Producer-2', 'User sarah_jones role updated to manager');

-- PaymentEvents topic (25 messages)
INSERT INTO messages (topic, producer_id, content) VALUES
('PaymentEvents', 'Producer-3', 'Payment $99.99 received from customer_001'),
('PaymentEvents', 'Producer-1', 'Payment $250.00 processed via Stripe'),
('PaymentEvents', 'Producer-3', 'Refund $45.00 issued to customer_042'),
('PaymentEvents', 'Producer-1', 'Subscription renewed $9.99/month'),
('PaymentEvents', 'Producer-3', 'Payment failed: insufficient funds'),
('PaymentEvents', 'Producer-1', 'Invoice #12345 generated'),
('PaymentEvents', 'Producer-3', 'Payment $500.00 received via PayPal'),
('PaymentEvents', 'Producer-1', 'Chargeback dispute opened'),
('PaymentEvents', 'Producer-3', 'Payment $75.50 completed'),
('PaymentEvents', 'Producer-1', 'Coupon SAVE20 applied'),
('PaymentEvents', 'Producer-3', 'Payment $199.00 pending verification'),
('PaymentEvents', 'Producer-1', 'Wire transfer $1000 received'),
('PaymentEvents', 'Producer-3', 'Payment retry scheduled'),
('PaymentEvents', 'Producer-1', 'Tax calculation updated for order'),
('PaymentEvents', 'Producer-3', 'Payment $55.00 confirmed'),
('PaymentEvents', 'Producer-1', 'Subscription cancelled - refund pending'),
('PaymentEvents', 'Producer-3', 'Payment $120.00 via Apple Pay'),
('PaymentEvents', 'Producer-1', 'Credit memo #CM-789 issued'),
('PaymentEvents', 'Producer-3', 'Payment declined: card expired'),
('PaymentEvents', 'Producer-1', 'Partial refund $25.00 processed'),
('PaymentEvents', 'Producer-3', 'Invoice paid in full'),
('PaymentEvents', 'Producer-1', 'Payment $350.00 received'),
('PaymentEvents', 'Producer-3', 'Auto-billing failed'),
('PaymentEvents', 'Producer-1', 'Payment method updated'),
('PaymentEvents', 'Producer-3', 'Transaction ID: TXN-2025-001234');

-- ClickStream topic (20 messages)
INSERT INTO messages (topic, producer_id, content) VALUES
('ClickStream', 'Producer-1', 'Click: Homepage banner - user_001'),
('ClickStream', 'Producer-2', 'Click: Product page - SKU-12345'),
('ClickStream', 'Client-1', 'Click: Add to cart button'),
('ClickStream', 'Producer-1', 'Click: Checkout button'),
('ClickStream', 'Producer-2', 'Click: Search bar - query: "shoes"'),
('ClickStream', 'Client-1', 'Click: Navigation menu - Categories'),
('ClickStream', 'Producer-1', 'Click: Filter - Price: $50-$100'),
('ClickStream', 'Producer-2', 'Click: Sort by - Best selling'),
('ClickStream', 'Client-1', 'Click: Product image zoom'),
('ClickStream', 'Producer-1', 'Click: Reviews tab'),
('ClickStream', 'Producer-2', 'Click: Add to wishlist'),
('ClickStream', 'Client-1', 'Click: Share on social media'),
('ClickStream', 'Producer-1', 'Click: Apply coupon code'),
('ClickStream', 'Producer-2', 'Click: Change shipping address'),
('ClickStream', 'Client-1', 'Click: Payment method selection'),
('ClickStream', 'Producer-1', 'Click: Order confirmation'),
('ClickStream', 'Producer-2', 'Click: Track order link'),
('ClickStream', 'Client-1', 'Click: Contact support button'),
('ClickStream', 'Producer-1', 'Click: Subscribe newsletter'),
('ClickStream', 'Producer-2', 'Click: Download invoice PDF');

-- SystemLogs topic (15 messages)
INSERT INTO messages (topic, producer_id, content) VALUES
('SystemLogs', 'Server-A', 'INFO: Application started on port 8080'),
('SystemLogs', 'Producer-3', 'INFO: Database connection pool initialized'),
('SystemLogs', 'Server-A', 'WARN: High memory usage detected: 85%'),
('SystemLogs', 'Producer-1', 'INFO: Cache cleared successfully'),
('SystemLogs', 'Server-A', 'INFO: Scheduled job completed: cleanup'),
('SystemLogs', 'Producer-3', 'DEBUG: Request processed in 45ms'),
('SystemLogs', 'Server-A', 'INFO: SSL certificate renewed'),
('SystemLogs', 'Producer-1', 'WARN: Slow query detected: 2.5s'),
('SystemLogs', 'Server-A', 'INFO: Backup completed: 2.5GB'),
('SystemLogs', 'Producer-3', 'INFO: Service health check: OK'),
('SystemLogs', 'Server-A', 'DEBUG: Thread pool size: 50'),
('SystemLogs', 'Producer-1', 'INFO: Configuration reloaded'),
('SystemLogs', 'Server-A', 'WARN: Disk space low: 10GB remaining'),
('SystemLogs', 'Producer-3', 'INFO: Metrics exported to monitoring'),
('SystemLogs', 'Server-A', 'INFO: Rate limiter activated');

-- Errors topic (10 messages)
INSERT INTO messages (topic, producer_id, content) VALUES
('Errors', 'Server-A', 'ERROR: NullPointerException in UserService.java:125'),
('Errors', 'Producer-2', 'ERROR: Connection timeout to external API'),
('Errors', 'Server-A', 'ERROR: OutOfMemoryError - heap space'),
('Errors', 'Producer-2', 'ERROR: Invalid JSON format in request body'),
('Errors', 'Server-A', 'ERROR: Database connection refused'),
('Errors', 'Producer-2', 'ERROR: File not found: config.yaml'),
('Errors', 'Server-A', 'ERROR: Authentication failed for user admin'),
('Errors', 'Producer-2', 'ERROR: Maximum retry attempts exceeded'),
('Errors', 'Server-A', 'ERROR: SSL handshake failed'),
('Errors', 'Producer-2', 'ERROR: Queue overflow - messages dropped');

-- Notifications topic (5 messages)
INSERT INTO messages (topic, producer_id, content) VALUES
('Notifications', 'API-Gateway', 'PUSH: New order received - Order #5678'),
('Notifications', 'Producer-2', 'EMAIL: Welcome email sent to new_user@mail.com'),
('Notifications', 'API-Gateway', 'SMS: OTP 123456 sent to +1234567890'),
('Notifications', 'Producer-2', 'PUSH: Sale alert - 50% off today'),
('Notifications', 'API-Gateway', 'EMAIL: Password reset link sent');

-- =====================================================
-- VERIFICATION QUERY
-- =====================================================
-- SELECT topic, COUNT(*) as count FROM messages GROUP BY topic ORDER BY count DESC;
