// =====================================================
// NEO4J SEED SCRIPT - JAVA_PROJECT Database
// Run this in Neo4j Browser at http://localhost:7474
// Database: JAVA_PROJECT, Password: ALAA2004@
// =====================================================

// Step 1: Clean up old data
MATCH (n) DETACH DELETE n;

// =====================================================
// Step 2: Create Topics
// =====================================================
CREATE (t1:Topic {name: 'UserLogins', messageCount: 2500, lastUpdated: datetime()})
CREATE (t2:Topic {name: 'PaymentEvents', messageCount: 1800, lastUpdated: datetime()})
CREATE (t3:Topic {name: 'ClickStream', messageCount: 3200, lastUpdated: datetime()})
CREATE (t4:Topic {name: 'SystemLogs', messageCount: 1500, lastUpdated: datetime()})
CREATE (t5:Topic {name: 'Notifications', messageCount: 900, lastUpdated: datetime()})
CREATE (t6:Topic {name: 'Errors', messageCount: 350, lastUpdated: datetime()})
CREATE (t7:Topic {name: 'Analytics', messageCount: 750, lastUpdated: datetime()});

// =====================================================
// Step 3: Create Producers
// =====================================================
CREATE (p1:Producer {id: 'Producer-1', lastSeen: datetime()})
CREATE (p2:Producer {id: 'Producer-2', lastSeen: datetime()})
CREATE (p3:Producer {id: 'Producer-3', lastSeen: datetime()})
CREATE (p4:Producer {id: 'Client-1', lastSeen: datetime()})
CREATE (p5:Producer {id: 'Server-A', lastSeen: datetime()})
CREATE (p6:Producer {id: 'API-Gateway', lastSeen: datetime()});

// =====================================================
// Step 4: Create Consumers
// =====================================================
CREATE (c1:Consumer {id: 'Dashboard-UI', lastSeen: datetime()})
CREATE (c2:Consumer {id: 'Analytics-Service', lastSeen: datetime()})
CREATE (c3:Consumer {id: 'Alert-System', lastSeen: datetime()})
CREATE (c4:Consumer {id: 'Logger', lastSeen: datetime()})
CREATE (c5:Consumer {id: 'Backup-Service', lastSeen: datetime()});

// =====================================================
// Step 5: Create PUBLISHES_TO Relationships
// =====================================================
MATCH (p:Producer {id: 'Producer-1'}), (t:Topic {name: 'UserLogins'})
CREATE (p)-[:PUBLISHES_TO {messageCount: 500, lastUpdated: datetime()}]->(t);

MATCH (p:Producer {id: 'Producer-1'}), (t:Topic {name: 'PaymentEvents'})
CREATE (p)-[:PUBLISHES_TO {messageCount: 300, lastUpdated: datetime()}]->(t);

MATCH (p:Producer {id: 'Producer-1'}), (t:Topic {name: 'ClickStream'})
CREATE (p)-[:PUBLISHES_TO {messageCount: 800, lastUpdated: datetime()}]->(t);

MATCH (p:Producer {id: 'Producer-1'}), (t:Topic {name: 'SystemLogs'})
CREATE (p)-[:PUBLISHES_TO {messageCount: 200, lastUpdated: datetime()}]->(t);

MATCH (p:Producer {id: 'Producer-1'}), (t:Topic {name: 'Analytics'})
CREATE (p)-[:PUBLISHES_TO {messageCount: 150, lastUpdated: datetime()}]->(t);

MATCH (p:Producer {id: 'Producer-2'}), (t:Topic {name: 'UserLogins'})
CREATE (p)-[:PUBLISHES_TO {messageCount: 400, lastUpdated: datetime()}]->(t);

MATCH (p:Producer {id: 'Producer-2'}), (t:Topic {name: 'ClickStream'})
CREATE (p)-[:PUBLISHES_TO {messageCount: 600, lastUpdated: datetime()}]->(t);

MATCH (p:Producer {id: 'Producer-2'}), (t:Topic {name: 'Errors'})
CREATE (p)-[:PUBLISHES_TO {messageCount: 100, lastUpdated: datetime()}]->(t);

MATCH (p:Producer {id: 'Producer-2'}), (t:Topic {name: 'Notifications'})
CREATE (p)-[:PUBLISHES_TO {messageCount: 250, lastUpdated: datetime()}]->(t);

MATCH (p:Producer {id: 'Producer-3'}), (t:Topic {name: 'PaymentEvents'})
CREATE (p)-[:PUBLISHES_TO {messageCount: 500, lastUpdated: datetime()}]->(t);

MATCH (p:Producer {id: 'Producer-3'}), (t:Topic {name: 'SystemLogs'})
CREATE (p)-[:PUBLISHES_TO {messageCount: 300, lastUpdated: datetime()}]->(t);

MATCH (p:Producer {id: 'Producer-3'}), (t:Topic {name: 'Analytics'})
CREATE (p)-[:PUBLISHES_TO {messageCount: 200, lastUpdated: datetime()}]->(t);

MATCH (p:Producer {id: 'Client-1'}), (t:Topic {name: 'ClickStream'})
CREATE (p)-[:PUBLISHES_TO {messageCount: 400, lastUpdated: datetime()}]->(t);

MATCH (p:Producer {id: 'Client-1'}), (t:Topic {name: 'UserLogins'})
CREATE (p)-[:PUBLISHES_TO {messageCount: 300, lastUpdated: datetime()}]->(t);

MATCH (p:Producer {id: 'Server-A'}), (t:Topic {name: 'SystemLogs'})
CREATE (p)-[:PUBLISHES_TO {messageCount: 500, lastUpdated: datetime()}]->(t);

MATCH (p:Producer {id: 'Server-A'}), (t:Topic {name: 'Errors'})
CREATE (p)-[:PUBLISHES_TO {messageCount: 250, lastUpdated: datetime()}]->(t);

MATCH (p:Producer {id: 'API-Gateway'}), (t:Topic {name: 'Notifications'})
CREATE (p)-[:PUBLISHES_TO {messageCount: 400, lastUpdated: datetime()}]->(t);

// =====================================================
// Step 6: Create SUBSCRIBES_TO Relationships
// =====================================================
MATCH (c:Consumer {id: 'Dashboard-UI'}), (t:Topic {name: 'UserLogins'})
CREATE (c)-[:SUBSCRIBES_TO {lastUpdated: datetime()}]->(t);

MATCH (c:Consumer {id: 'Dashboard-UI'}), (t:Topic {name: 'PaymentEvents'})
CREATE (c)-[:SUBSCRIBES_TO {lastUpdated: datetime()}]->(t);

MATCH (c:Consumer {id: 'Dashboard-UI'}), (t:Topic {name: 'ClickStream'})
CREATE (c)-[:SUBSCRIBES_TO {lastUpdated: datetime()}]->(t);

MATCH (c:Consumer {id: 'Dashboard-UI'}), (t:Topic {name: 'Errors'})
CREATE (c)-[:SUBSCRIBES_TO {lastUpdated: datetime()}]->(t);

MATCH (c:Consumer {id: 'Dashboard-UI'}), (t:Topic {name: 'SystemLogs'})
CREATE (c)-[:SUBSCRIBES_TO {lastUpdated: datetime()}]->(t);

MATCH (c:Consumer {id: 'Analytics-Service'}), (t:Topic {name: 'ClickStream'})
CREATE (c)-[:SUBSCRIBES_TO {lastUpdated: datetime()}]->(t);

MATCH (c:Consumer {id: 'Analytics-Service'}), (t:Topic {name: 'Analytics'})
CREATE (c)-[:SUBSCRIBES_TO {lastUpdated: datetime()}]->(t);

MATCH (c:Consumer {id: 'Alert-System'}), (t:Topic {name: 'Errors'})
CREATE (c)-[:SUBSCRIBES_TO {lastUpdated: datetime()}]->(t);

MATCH (c:Consumer {id: 'Logger'}), (t:Topic {name: 'SystemLogs'})
CREATE (c)-[:SUBSCRIBES_TO {lastUpdated: datetime()}]->(t);

MATCH (c:Consumer {id: 'Backup-Service'}), (t:Topic {name: 'PaymentEvents'})
CREATE (c)-[:SUBSCRIBES_TO {lastUpdated: datetime()}]->(t);

// =====================================================
// Step 7: Verification Queries
// =====================================================

// Query 1: Which Producer posts to the most topics?
MATCH (p:Producer)-[r:PUBLISHES_TO]->(t:Topic)
RETURN p.id as producer, COUNT(DISTINCT t) as topicCount, SUM(r.messageCount) as totalMessages
ORDER BY topicCount DESC, totalMessages DESC;

// Query 2: Show all topics with message counts
MATCH (t:Topic)
RETURN t.name as topic, t.messageCount as messages
ORDER BY messages DESC;

// Query 3: Visualize the graph (run this in Neo4j Browser)
MATCH (n) RETURN n LIMIT 100;
