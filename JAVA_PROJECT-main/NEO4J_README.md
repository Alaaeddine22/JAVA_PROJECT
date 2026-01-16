# 🧠 Neo4j Cheat Sheet & CRUD Operations

This document provides a comprehensive list of Cypher commands for managing the Neo4j Graph Database in the Streaming Platform project.

## 🔌 Connection Details
- **URI**: `bolt://localhost:7687`
- **User**: `neo4j`
- **Password**: `ALAA2004@`
- **Database**: `JAVA_PROJECT`

---

## 🏗️ 1. CREATE (Insert Data)

### Create Nodes
**Create a Topic**
```cypher
CREATE (t:Topic {name: 'sports-updates', messageCount: 0, lastUpdated: datetime()})
RETURN t;
```

**Create a Producer**
```cypher
CREATE (p:Producer {id: 'producer-1', lastSeen: datetime()})
RETURN p;
```

**Create a Consumer**
```cypher
CREATE (c:Consumer {id: 'consumer-A', group: 'group-1', lastSeen: datetime()})
RETURN c;
```

**Create a Partition**
```cypher
CREATE (part:Partition {id: 'partition-0', topic: 'sports-updates'})
RETURN part;
```

### Create Relationships
**Producer PUBLISHES_TO Topic**
```cypher
MATCH (p:Producer {id: 'producer-1'})
MATCH (t:Topic {name: 'sports-updates'})
MERGE (p)-[r:PUBLISHES_TO]->(t)
SET r.messageCount = 0, r.lastUpdated = datetime()
RETURN p, r, t;
```

**Consumer SUBSCRIBES_TO Topic**
```cypher
MATCH (c:Consumer {id: 'consumer-A'})
MATCH (t:Topic {name: 'sports-updates'})
MERGE (c)-[r:SUBSCRIBES_TO]->(t)
SET r.offset = 0, r.lastConsumed = datetime()
RETURN c, r, t;
```

---

## 🔍 2. READ (Select Data)

### Select All Nodes
**List all Topics**
```cypher
MATCH (t:Topic)
RETURN t.name, t.messageCount
ORDER BY t.messageCount DESC;
```

**List all Producers**
```cypher
MATCH (p:Producer)
RETURN p.id, p.lastSeen;
```

### Filtering & patterns
**Find which Topics a Producer writes to**
```cypher
MATCH (p:Producer {id: 'producer-1'})-[r:PUBLISHES_TO]->(t:Topic)
RETURN t.name, r.messageCount;
```

**Find all Consumers for a specific Topic**
```cypher
MATCH (c:Consumer)-[:SUBSCRIBES_TO]->(t:Topic {name: 'sports-updates'})
RETURN c.id, c.group;
```

**Count Relationships (Analytic)**
```cypher
MATCH (p:Producer)-[r:PUBLISHES_TO]->(t:Topic)
RETURN t.name, COUNT(p) as ProducerCount
ORDER BY ProducerCount DESC;
```

---

## ✏️ 3. UPDATE (Edit Data)

### Update Node Properties
**Update Message Count for a Topic**
```cypher
MATCH (t:Topic {name: 'sports-updates'})
SET t.messageCount = 150, t.lastUpdated = datetime()
RETURN t;
```

**Update Producer Status**
```cypher
MATCH (p:Producer {id: 'producer-1'})
SET p.status = 'ACTIVE', p.lastSeen = datetime()
RETURN p;
```

### Update Relationship Properties
**Increment Message Count on Relationship**
```cypher
MATCH (p:Producer {id: 'producer-1'})-[r:PUBLISHES_TO]->(t:Topic {name: 'sports-updates'})
SET r.messageCount = r.messageCount + 1
RETURN r;
```

---

## 🗑️ 4. DELETE (Remove Data)

### Delete Specific Nodes
**Delete a specific Consumer**
```cypher
MATCH (c:Consumer {id: 'consumer-old'})
DETACH DELETE c;
```
*(Note: `DETACH DELETE` removes the node AND its relationships)*

**Delete a Topic and its relationships**
```cypher
MATCH (t:Topic {name: 'deprecated-topic'})
DETACH DELETE t;
```

### Delete All Data (Reset Database)
**⚠️ WARNING: Use with caution!**
```cypher
MATCH (n)
DETACH DELETE n;
```

---

## 📊 5. Advanced Analysis Queries

**Top 5 Most Active Topics**
```cypher
MATCH (t:Topic)
RETURN t.name, t.messageCount
ORDER BY t.messageCount DESC
LIMIT 5;
```

**Producers who publish to multiple Topics**
```cypher
MATCH (p:Producer)-[:PUBLISHES_TO]->(t:Topic)
WITH p, count(t) as TopicCount
WHERE TopicCount > 1
RETURN p.id, TopicCount
ORDER BY TopicCount DESC;
```

**Orphan Topics (No Producers)**
```cypher
MATCH (t:Topic)
WHERE NOT (()-[:PUBLISHES_TO]->(t))
RETURN t.name;
```

**System Stats Overview**
```cypher
MATCH (t:Topic) WITH count(t) as Topics
MATCH (p:Producer) WITH Topics, count(p) as Producers
MATCH (c:Consumer) WITH Topics, Producers, count(c) as Consumers
RETURN Topics, Producers, Consumers;
```

---

## 🛠️ Constraints & Indexes
It is recommended to create indexes for performance:

```cypher
CREATE CONSTRAINT FOR (t:Topic) REQUIRE t.name IS UNIQUE;
CREATE CONSTRAINT FOR (p:Producer) REQUIRE p.id IS UNIQUE;
CREATE CONSTRAINT FOR (c:Consumer) REQUIRE c.id IS UNIQUE;
CREATE INDEX FOR (t:Topic) ON (t.lastUpdated);
```
