# Streaming Platform - Java Project

A real-time data streaming platform with multi-database architecture supporting H2, MySQL (XAMPP), and Neo4j for different data needs.

---

## 🏗️ Project Structure

```
JAVA_PROJECT-devedits/
├── pom.xml                              # Maven configuration with all dependencies
├── README.md                            # This documentation file
│
├── src/main/java/com/streamingplatform/
│   ├── Main.java                        # Application entry point with console commands
│   ├── TestProducer.java               # Generates test messages for the broker
│   ├── InteractiveClient.java          # Interactive command-line client
│   │
│   ├── analysis/                        # Data Analysis & Export
│   │   ├── DataExporter.java           # ⭐ NEW: Export data to CSV/JSON from H2 & Neo4j
│   │   └── StreamAnalytics.java        # Stream statistics and analysis
│   │
│   ├── client/                          # Client-side components
│   │   ├── Producer.java               # Message producer client
│   │   └── Consumer.java               # Message consumer client
│   │
│   ├── net/                             # Network layer
│   │   ├── SocketServer.java           # TCP server on port 8080
│   │   └── ClientWorker.java           # Handles individual client connections
│   │
│   ├── persistence/                     # Database Connections
│   │   ├── DatabaseManager.java        # H2 database connection (fast local storage)
│   │   ├── MySQLManager.java           # ⭐ MySQL/XAMPP connection manager
│   │   ├── Neo4jManager.java           # ⭐ Neo4j graph database connection
│   │   └── MessageDAO.java             # Data Access Object for messages
│   │
│   ├── service/                         # Business Logic Services
│   │   ├── BrokerService.java          # Core message broker service
│   │   ├── MySQLSyncService.java       # ⭐ Syncs H2 → MySQL (every 5s)
│   │   ├── Neo4jSyncService.java       # ⭐ Syncs H2 → Neo4j (every 5s)
│   │   ├── Neo4jToMySQLSync.java       # ⭐ NEW: Syncs Neo4j → MySQL on demand
│   │   └── ArchiveService.java         # ⭐ Auto-deletes old messages
│   │
│   └── ui/
│       └── Dashboard.java              # JavaFX real-time dashboard
│
├── sql/                                 # SQL seed scripts
│   ├── h2_seed.sql                     # Sample data for H2
│   ├── mysql_seed.sql                  # Sample data for MySQL
│   └── neo4j_seed.cypher               # Sample data for Neo4j
│
└── neo4j/
    └── analysis.cypher                 # Neo4j analysis queries
```

---

## 🔗 Database Architecture & Connections

### Backend ↔ MySQL ↔ Neo4j Complete Architecture

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                              BACKEND (Main.java)                                 │
│                                                                                  │
│  ┌────────────────────────────────────────────────────────────────────────────┐ │
│  │                         SERVICE LAYER                                       │ │
│  │  ┌───────────────────┐  ┌───────────────────┐  ┌───────────────────────┐  │ │
│  │  │  MySQLSyncService │  │  Neo4jSyncService │  │  Neo4jToMySQLSync     │  │ │
│  │  │  (every 5s)       │  │  (every 5s)       │  │  (on command)         │  │ │
│  │  └─────────┬─────────┘  └─────────┬─────────┘  └───────────┬───────────┘  │ │
│  └────────────┼──────────────────────┼────────────────────────┼──────────────┘ │
│               │                      │                        │                 │
│  ┌────────────┼──────────────────────┼────────────────────────┼──────────────┐ │
│  │            │           PERSISTENCE LAYER                   │              │ │
│  │            ▼                      ▼                        │              │ │
│  │  ┌─────────────────┐    ┌─────────────────┐               │              │ │
│  │  │  MySQLManager   │    │  Neo4jManager   │◄──────────────┘              │ │
│  │  │  .getConnection │    │  .getDriver()   │                              │ │
│  │  └────────┬────────┘    └────────┬────────┘                              │ │
│  └───────────┼──────────────────────┼───────────────────────────────────────┘ │
└──────────────┼──────────────────────┼───────────────────────────────────────────┘
               │                      │
               ▼                      ▼
    ┌──────────────────┐    ┌──────────────────┐
    │      MYSQL       │    │      NEO4J       │
    │   (XAMPP)        │    │    (Desktop)     │
    │                  │    │                  │
    │ localhost:3306   │◄───│ localhost:7687   │
    │                  │    │                  │
    │ Tables:          │    │ Nodes:           │
    │ - topics         │    │ - Topic          │
    │ - producers      │    │ - producer       │
    │ - consumers      │    │ - consumer       │
    │ - partitions     │    │ - partition      │
    └──────────────────┘    └──────────────────┘
           ▲                        │
           │   Neo4jToMySQLSync     │
           └────────────────────────┘
```

### Key Files & Connections

| Layer | File | Connection |
|-------|------|------------|
| **Backend Entry** | `Main.java` | Starts all services |
| **MySQL Connection** | `MySQLManager.java` | `jdbc:mysql://localhost:3306` |
| **Neo4j Connection** | `Neo4jManager.java` | `bolt://localhost:7687` |
| **H2 → MySQL Sync** | `MySQLSyncService.java` | Runs every 5 seconds |
| **H2 → Neo4j Sync** | `Neo4jSyncService.java` | Runs every 5 seconds |
| **Neo4j → MySQL Bridge** | `Neo4jToMySQLSync.java` | On `sync-neo4j` command |

### Main.java - The Backend Hub

```java
public static void main(String[] args) {
    // 1. Initialize ALL databases
    DatabaseManager.initializeDatabase();     // H2 (local fast storage)
    MySQLManager.initializeDatabase();        // MySQL (XAMPP)
    Neo4jManager.initializeDatabase();        // Neo4j (Graph DB)
    
    // 2. Start background sync services
    MySQLSyncService mysqlSyncService = new MySQLSyncService();
    mysqlSyncService.start();                 // H2 → MySQL every 5s
    
    Neo4jSyncService neo4jSyncService = new Neo4jSyncService();
    neo4jSyncService.start();                 // H2 → Neo4j every 5s
    
    // 3. Console command for Neo4j → MySQL sync
    case "sync-neo4j" -> Neo4jToMySQLSync.syncAll();
}
```

---

### Multi-Database Flow

```
┌─────────────────────────────────────────────────────────────────────────┐
│                        STREAMING PLATFORM                                │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                          │
│   ┌──────────────┐      ┌──────────────┐      ┌──────────────┐         │
│   │   PRODUCER   │ ───► │  TCP SERVER  │ ───► │   H2 (SQL)   │         │
│   │   Client     │      │  Port 8080   │      │  Fast Write  │         │
│   └──────────────┘      └──────────────┘      └──────┬───────┘         │
│                                                       │                  │
│                              ┌────────────────────────┼───────────────┐ │
│                              │                        │               │ │
│                              ▼                        ▼               │ │
│                    ┌──────────────────┐    ┌──────────────────┐       │ │
│                    │   MySQLSync      │    │   Neo4jSync      │       │ │
│                    │   (every 5s)     │    │   (every 5s)     │       │ │
│                    └────────┬─────────┘    └────────┬─────────┘       │ │
│                             │                        │                 │ │
│                             ▼                        ▼                 │ │
│                    ┌──────────────────┐    ┌──────────────────┐       │ │
│                    │   MySQL (XAMPP)  │◄───│   Neo4j Graph    │       │ │
│                    │   Analytics DB   │    │   Relationships  │       │ │
│                    │   localhost:3306 │    │   localhost:7687 │       │ │
│                    └──────────────────┘    └──────────────────┘       │ │
│                             ▲                        │                 │ │
│                             │    Neo4jToMySQLSync    │                 │ │
│                             └────────────────────────┘                 │ │
│                                   (on demand)                          │ │
│                                                                          │
└─────────────────────────────────────────────────────────────────────────┘
```

### Connection Details

| Database | URL | User | Password | Purpose |
|----------|-----|------|----------|---------|
| **H2** | `jdbc:h2:./streamingservice_db` | sa | (none) | Fast message ingestion |
| **MySQL** | `jdbc:mysql://localhost:3306/streamingplatform` | root | (none) | Analytics & queries |
| **Neo4j** | `bolt://localhost:7687` | neo4j | ALAA2004@ | Graph relationships |

---

## 📡 Database Connection Files

### 1. MySQLManager.java
**Location:** `src/main/java/com/streamingplatform/persistence/MySQLManager.java`

```java
// Connection settings
private static final String DB_URL = "jdbc:mysql://localhost:3306/";
private static final String DB_NAME = "streamingplatform";
private static final String USER = "root";
private static final String PASSWORD = "";  // XAMPP default
```

**Tables Created:**
- `topics` - Topic names and message counts
- `producers` - Producer IDs
- `consumers` - Consumer IDs with group assignments
- `partitions` - Partition assignments to topics
- `consumer_groups` - Consumer group metadata
- `ip_topic_stats` - Producer-Topic relationships

---

### 2. Neo4jManager.java
**Location:** `src/main/java/com/streamingplatform/persistence/Neo4jManager.java`

```java
// Connection settings
private static final String URI = "bolt://localhost:7687";
private static final String USER = "neo4j";
private static final String PASSWORD = "ALAA2004@";
private static final String DATABASE = "JAVA_PROJECT";
```

**Node Types:**
- `Topic` - Message topics
- `producer` - Message producers
- `consumer` - Message consumers
- `partition` - Topic partitions
- `consumer_group` - Consumer groups

---

### 3. Neo4jToMySQLSync.java ⭐ NEW
**Location:** `src/main/java/com/streamingplatform/service/Neo4jToMySQLSync.java`

**Purpose:** Syncs all data FROM Neo4j TO MySQL on demand.

```java
// Example: Sync producers from Neo4j to MySQL
Result result = session.run("MATCH (p:producer) RETURN p.id as producerId");
while (result.hasNext()) {
    ps.setString(1, record.get("producerId").asString());
    ps.executeUpdate();
}
```

**Console Command:** `sync-neo4j`

---

## � MySQL ↔ Neo4j Bridge - How It Works

### Visual Flow

```
┌─────────────────────────────────────────────────────────────────┐
│                     NEO4J DATABASE                               │
│                  (bolt://localhost:7687)                         │
│                                                                  │
│  ┌──────────┐   ┌──────────┐   ┌──────────┐   ┌──────────────┐  │
│  │  Topic   │   │ producer │   │ consumer │   │  partition   │  │
│  │  Nodes   │   │  Nodes   │   │  Nodes   │   │    Nodes     │  │
│  └────┬─────┘   └────┬─────┘   └────┬─────┘   └──────┬───────┘  │
└───────┼──────────────┼──────────────┼────────────────┼──────────┘
        │              │              │                │
        │         Neo4jToMySQLSync.java                │
        │         (sync-neo4j command)                 │
        ▼              ▼              ▼                ▼
┌─────────────────────────────────────────────────────────────────┐
│                     MYSQL DATABASE                               │
│                  (localhost:3306/streamingplatform)              │
│                                                                  │
│  ┌──────────┐   ┌───────────┐   ┌───────────┐   ┌────────────┐  │
│  │  topics  │   │ producers │   │ consumers │   │ partitions │  │
│  │  table   │   │   table   │   │   table   │   │   table    │  │
│  └──────────┘   └───────────┘   └───────────┘   └────────────┘  │
└─────────────────────────────────────────────────────────────────┘
```

### The Bridge Code

The `Neo4jToMySQLSync.java` imports BOTH database managers:

```java
import com.streamingplatform.persistence.MySQLManager;   // MySQL Connection
import com.streamingplatform.persistence.Neo4jManager;   // Neo4j Connection
```

### How Data Flows (Example: syncTopics)

```java
private static int syncTopics() throws Exception {
    // 1️⃣ Get MySQL connection
    Connection mysqlConn = MySQLManager.getConnection();
    PreparedStatement ps = mysqlConn.prepareStatement(
        "INSERT INTO topics (name, message_count) VALUES (?, ?)"
    );

    // 2️⃣ Query Neo4j for all Topics
    try (Session session = Neo4jManager.getDriver().session()) {
        Result result = session.run(
            "MATCH (t:Topic) RETURN t.name as name, t.messageCount as messageCount"
        );
        
        // 3️⃣ For each Neo4j record, insert into MySQL
        while (result.hasNext()) {
            Record record = result.next();
            ps.setString(1, record.get("name").asString());        // Neo4j → 
            ps.setInt(2, record.get("messageCount").asInt());      // Neo4j → 
            ps.executeUpdate();                                     // → MySQL!
        }
    }
    return count;
}
```

### Entities Synced

| Neo4j Node | MySQL Table | Key Field |
|------------|-------------|-----------|
| `Topic` | `topics` | `name` |
| `producer` | `producers` | `producer_id` |
| `consumer` | `consumers` | `consumer_id` |
| `partition` | `partitions` | `partition_id` |
| `consumer_group` | `consumer_groups` | `group_id` |

---

## �🖥️ Console Commands

| Command | Description |
|---------|-------------|
| **H2 Export** | |
| `export` | Export H2 messages to CSV |
| `json` | Export H2 messages to JSON |
| **Neo4j Export** | |
| `neo4j-topics` | Export Topics from Neo4j to CSV |
| `neo4j-producers` | Export Producers from Neo4j to CSV |
| `neo4j-consumers` | Export Consumers from Neo4j to CSV |
| `neo4j-json` | Export all Neo4j data to JSON |
| **Combined** | |
| `backup` | Full timestamped backup (H2 + Neo4j) |
| **Database Sync** | |
| `sync-neo4j` | ⭐ Sync Neo4j data to MySQL |
| `preview-neo4j` | Preview Neo4j data before sync |
| **Stats** | |
| `stats` | Top producers by topic count (MySQL) |
| `neo4j` | Top producers by topic count (Neo4j) |
| `topics` | List all topics from Neo4j |
| **Maintenance** | |
| `cleanup` | Delete old messages from H2 |
| `help` | Show all commands |
| `exit` | Shutdown platform |

---

## 📅 Updates - December 21, 2025

### New Features Added

1. **Neo4j Integration**
   - Added Neo4j Java Driver dependency
   - Created `Neo4jManager.java` for database connection
   - Created `Neo4jSyncService.java` for H2 → Neo4j sync

2. **MySQL/XAMPP Integration**
   - Added MySQL Connector/J dependency
   - Created `MySQLManager.java` for database connection
   - Created `MySQLSyncService.java` for H2 → MySQL sync

3. **Neo4j → MySQL Sync** ⭐
   - Created `Neo4jToMySQLSync.java`
   - Syncs: Topics, Producers, Consumers, Partitions, Consumer Groups
   - Console command: `sync-neo4j`

4. **Enhanced DataExporter**
   - Added Neo4j export methods (CSV, JSON)
   - Added timestamped backup support
   - Exports from both H2 and Neo4j

5. **Archive Service**
   - Created `ArchiveServicerjava`
   - Auto-deletes messages older than 5 minutes
   - Prevents storage exhaustion

---

## 🚀 How to Run

### Prerequisites
1. **Java 17+** installed
2. **Maven** installed
3. **XAMPP** running (MySQL on port 3306)
4. **Neo4j Desktop** running with database `JAVA_PROJECT`

### Start the Platform
```bash
mvn exec:java -Dexec.mainClass=com.streamingplatform.Main
```

### Generate Test Data
```bash
mvn exec:java -Dexec.mainClass=com.streamingplatform.TestProducer
```

---

## 📦 Dependencies (pom.xml)

```xml
<!-- H2 Database - Fast local storage -->
<dependency>
    <groupId>com.h2database</groupId>
    <artifactId>h2</artifactId>
    <version>2.2.224</version>
</dependency>

<!-- MySQL Connector - XAMPP integration -->
<dependency>
    <groupId>mysql</groupId>
    <artifactId>mysql-connector-java</artifactId>
    <version>8.0.33</version>
</dependency>

<!-- Neo4j Driver - Graph database -->
<dependency>
    <groupId>org.neo4j.driver</groupId>
    <artifactId>neo4j-java-driver</artifactId>
    <version>5.14.0</version>
</dependency>

<!-- JavaFX - Dashboard UI -->
<dependency>
    <groupId>org.openjfx</groupId>
    <artifactId>javafx-controls</artifactId>
    <version>17.0.6</version>
</dependency>
```

---

## 📊 Current Data Stats

After running `sync-neo4j`:

| MySQL Table | Records |
|-------------|---------|
| topics | 40,000 |
| producers | 3,908 |
| consumers | 0 |
| partitions | 0 |
| consumer_groups | 0 |
//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

---

## 🔢 Relational Algebra Operations Implementation

This section demonstrates where each relational algebra operation is implemented in the codebase across Frontend, Backend (Java), and Neo4j.

### 1. **RS: Select All (Projection)**
Retrieve all records from a relation without filtering.

#### 🎨 Frontend (`frontend/server.js`)
```javascript
// Line 70-80: Get all topics
app.get('/api/topics', async (req, res) => {
    const records = await runQuery(`
        MATCH (t:Topic)
        OPTIONAL MATCH (p:Producer)-[r:PUBLISHES_TO]->(t)
        RETURN t.name as name, 
               COALESCE(t.messageCount, 0) as messageCount,
               COUNT(DISTINCT p) as producerCount
        ORDER BY messageCount DESC
        LIMIT 100
    `);
});

// Line 98-108: Get all producers
app.get('/api/producers', async (req, res) => {
    const records = await runQuery(`
        MATCH (p:Producer)
        OPTIONAL MATCH (p)-[r:PUBLISHES_TO]->(t:Topic)
        RETURN p.id as id,
               COUNT(DISTINCT t) as topicCount
    `);
});

// Line 126-135: Get all consumers
app.get('/api/consumers', async (req, res) => {
    const records = await runQuery(`
        MATCH (c:Consumer)
        OPTIONAL MATCH (c)-[r:SUBSCRIBES_TO]->(t:Topic)
        RETURN c.id as id
    `);
});
```

#### ☕ Backend Java
```java
// File: src/main/java/com/streamingplatform/analysis/DataExporter.java
// Line 42: Export all messages from H2
ResultSet rs = conn.createStatement().executeQuery("SELECT * FROM messages ORDER BY id");

// File: src/main/java/com/streamingplatform/service/Neo4jSyncService.java
// Line 98: Get all distinct producers
String producerQuery = "SELECT DISTINCT producer_id FROM messages WHERE producer_id IS NOT NULL";
```

#### 🌐 Neo4j (`neo4j/analysis.cypher`)
```cypher
// Line 16-18: Select all topics with message counts
MATCH (t:Topic)<-[:PUBLISHED_TO]-(m:Message)
RETURN t.name, count(m) as message_count
ORDER BY message_count DESC;
```

**File Location:** [`Neo4jManager.java:181-206`](file:///c:/Users/real2/OneDrive/Desktop/4EME/javaAv/JAVA_PROJECT-wissversion/JAVA_PROJECT-main%20(1)/JAVA_PROJECT-main/src/main/java/com/streamingplatform/persistence/Neo4jManager.java#L181-L206)

---

### 2. **RC: Relation (Join)**
Create relationships between entities.

#### 🎨 Frontend (`frontend/server.js`)
```javascript
// Line 467-480: Create PUBLISHES_TO relationship
app.post('/api/relationships/publishes', async (req, res) => {
    const { producerId, topicName, messageCount = 0 } = req.body;
    await runQuery(`
        MATCH (p:Producer {id: $producerId})
        MATCH (t:Topic {name: $topicName})
        MERGE (p)-[r:PUBLISHES_TO]->(t)
        SET r.messageCount = $messageCount, r.lastUpdated = datetime()
    `);
});

// Line 489-501: Create SUBSCRIBES_TO relationship
app.post('/api/relationships/subscribes', async (req, res) => {
    await runQuery(`
        MATCH (c:Consumer {id: $consumerId})
        MATCH (t:Topic {name: $topicName})
        MERGE (c)-[r:SUBSCRIBES_TO]->(t)
        SET r.lastUpdated = datetime()
    `);
});
```

#### ☕ Backend Java
```java
// File: src/main/java/com/streamingplatform/persistence/Neo4jManager.java
// Line 108-120: Create PUBLISHES_TO relationship
public static void createPublishesRelation(String producerId, String topicName, int messageCount) {
    session.run(
        "MATCH (p:Producer {id: $producerId}) " +
        "MATCH (t:Topic {name: $topicName}) " +
        "MERGE (p)-[r:PUBLISHES_TO]->(t) " +
        "SET r.messageCount = $messageCount, r.lastUpdated = datetime()"
    );
}

// Line 129-140: Create SUBSCRIBES_TO relationship
public static void createSubscribesRelation(String consumerId, String topicName) {
    session.run(
        "MATCH (c:Consumer {id: $consumerId}) " +
        "MATCH (t:Topic {name: $topicName}) " +
        "MERGE (c)-[r:SUBSCRIBES_TO]->(t) " +
        "SET r.lastUpdated = datetime()"
    );
}
```

**File Location:** [`Neo4jManager.java:108-144`](file:///c:/Users/real2/OneDrive/Desktop/4EME/javaAv/JAVA_PROJECT-wissversion/JAVA_PROJECT-main%20(1)/JAVA_PROJECT-main/src/main/java/com/streamingplatform/persistence/Neo4jManager.java#L108-L144)

#### 🌐 Neo4j
```cypher
// Line 154-159: Join producers with topics
MATCH (p:Producer)-[r:PUBLISHES_TO]->(t:Topic)
RETURN p.id as producer, 
       COUNT(DISTINCT t) as topicCount, 
       SUM(r.messageCount) as totalMessages
ORDER BY topicCount DESC
```

---

### 3. **RF: Filter (Selection)**
Filter records based on conditions.

#### 🎨 Frontend (`frontend/server.js`)
```javascript
// Line 155-158: Filter relationships count
const relationsResult = await runQuery('MATCH ()-[r]->() RETURN COUNT(r) as count');

// Line 200-204: Filter top 10 producers by activity
const records = await runQuery(`
    MATCH (p:Producer)-[r:PUBLISHES_TO]->(t:Topic)
    RETURN p.id as id, COUNT(DISTINCT t) as topics
    ORDER BY messages DESC
    LIMIT 10
`);
```

#### ☕ Backend Java
```java
// File: src/main/java/com/streamingplatform/persistence/MessageDAO.java
// Line 32: Filter messages by topic
String sql = "SELECT content FROM messages WHERE topic = ?";

// File: src/main/java/com/streamingplatform/service/Neo4jSyncService.java
// Line 98: Filter non-null producers
String producerQuery = "SELECT DISTINCT producer_id FROM messages WHERE producer_id IS NOT NULL";
```

**File Location:** [`MessageDAO.java:32`](file:///c:/Users/real2/OneDrive/Desktop/4EME/javaAv/JAVA_PROJECT-wissversion/JAVA_PROJECT-main%20(1)/JAVA_PROJECT-main/src/main/java/com/streamingplatform/persistence/MessageDAO.java#L32)

#### 🌐 Neo4j
```cypher
// Neo4j constraint filters (unique values)
CREATE CONSTRAINT IF NOT EXISTS FOR (t:Topic) REQUIRE t.name IS UNIQUE;
CREATE CONSTRAINT IF NOT EXISTS FOR (p:Producer) REQUIRE p.id IS UNIQUE;
CREATE CONSTRAINT IF NOT EXISTS FOR (c:Consumer) REQUIRE c.id IS UNIQUE;
```

**File Location:** [`Neo4jManager.java:43-45`](file:///c:/Users/real2/OneDrive/Desktop/4EME/javaAv/JAVA_PROJECT-wissversion/JAVA_PROJECT-main%20(1)/JAVA_PROJECT-main/src/main/java/com/streamingplatform/persistence/Neo4jManager.java#L43-L45)

---

### 4. **RA: Aggregation**
Perform aggregate operations (COUNT, SUM, AVG, etc.).

#### 🎨 Frontend (`frontend/server.js`)
```javascript
// Line 152-158: Count all nodes by type
const [topicsResult, producersResult, consumersResult] = await Promise.all([
    runQuery('MATCH (t:Topic) RETURN COUNT(t) as count'),
    runQuery('MATCH (p:Producer) RETURN COUNT(p) as count'),
    runQuery('MATCH (c:Consumer) RETURN COUNT(c) as count')
]);

// Line 177-184: Aggregate message counts by topic
const records = await runQuery(`
    MATCH (t:Topic)
    RETURN t.name as name, COALESCE(t.messageCount, 0) as count
    ORDER BY count DESC
    LIMIT 10
`);

// Line 200-204: Sum messages and count topics per producer
const records = await runQuery(`
    MATCH (p:Producer)-[r:PUBLISHES_TO]->(t:Topic)
    RETURN p.id as id, 
           COUNT(DISTINCT t) as topics, 
           SUM(r.messageCount) as messages
    ORDER BY messages DESC
    LIMIT 10
`);
```

#### ☕ Backend Java
```java
// File: src/main/java/com/streamingplatform/service/Neo4jSyncService.java
// Line 76: Count messages by topic (GROUP BY)
String query = "SELECT topic, COUNT(*) as cnt FROM messages GROUP BY topic";

// Line 109-112: Count messages per producer-topic pair
String relQuery = """
    SELECT producer_id, topic, COUNT(*) as msg_count
    FROM messages
    WHERE producer_id IS NOT NULL
    GROUP BY producer_id, topic
""";
```

**File Location:** [`Neo4jSyncService.java:76-120`](file:///c:/Users/real2/OneDrive/Desktop/4EME/javaAv/JAVA_PROJECT-wissversion/JAVA_PROJECT-main%20(1)/JAVA_PROJECT-main/src/main/java/com/streamingplatform/service/Neo4jSyncService.java#L76-L120)

```java
// File: src/main/java/com/streamingplatform/analysis/DataExporter.java
// Line 323: Count total messages
ResultSet rsTotal = conn.createStatement().executeQuery("SELECT COUNT(*) as total FROM messages");

// Line 331: Group by and count
"SELECT topic, COUNT(*) as count FROM messages GROUP BY topic ORDER BY count DESC"
```

**File Location:** [`DataExporter.java:323-331`](file:///c:/Users/real2/OneDrive/Desktop/4EME/javaAv/JAVA_PROJECT-wissversion/JAVA_PROJECT-main%20(1)/JAVA_PROJECT-main/src/main/java/com/streamingplatform/analysis/DataExporter.java#L323-L331)

#### 🌐 Neo4j
```cypher
// File: src/main/java/com/streamingplatform/persistence/Neo4jManager.java
// Line 154-159: Aggregate - COUNT DISTINCT and SUM
MATCH (p:Producer)-[r:PUBLISHES_TO]->(t:Topic)
RETURN p.id as producer, 
       COUNT(DISTINCT t) as topicCount, 
       SUM(r.messageCount) as totalMessages
ORDER BY topicCount DESC, totalMessages DESC
LIMIT 10

// Line 186-190: Aggregate with OPTIONAL MATCH
MATCH (t:Topic)
OPTIONAL MATCH (p:Producer)-[r:PUBLISHES_TO]->(t)
RETURN t.name as topic, 
       t.messageCount as count, 
       COUNT(DISTINCT p) as producers
ORDER BY count DESC
```

**File Location:** [`Neo4jManager.java:149-176`](file:///c:/Users/real2/OneDrive/Desktop/4EME/javaAv/JAVA_PROJECT-wissversion/JAVA_PROJECT-main%20(1)/JAVA_PROJECT-main/src/main/java/com/streamingplatform/persistence/Neo4jManager.java#L149-L176)

---

### 5. **RM: Edit (Update/Modify)**
Update existing records in the database.

#### 🎨 Frontend (`frontend/server.js`)
```javascript
// Line 262-284: Update topic
app.put('/api/topics/:name', async (req, res) => {
    const { name } = req.params;
    const { newName, messageCount } = req.body;
    
    await runQuery(`
        MATCH (t:Topic {name: $name})
        SET t.name = $newName, 
            t.messageCount = $messageCount,
            t.lastUpdated = datetime()
        RETURN t
    `);
});

// Line 339-357: Update producer
app.put('/api/producers/:id', async (req, res) => {
    await runQuery(`
        MATCH (p:Producer {id: $id})
        SET p.id = $newId, p.lastSeen = datetime()
        RETURN p
    `);
});

// Line 411-433: Update consumer
app.put('/api/consumers/:id', async (req, res) => {
    await runQuery(`
        MATCH (c:Consumer {id: $id})
        SET c.id = $newId, c.group = $group, c.lastSeen = datetime()
        RETURN c
    `);
});
```

#### ☕ Backend Java
```java
// File: src/main/java/com/streamingplatform/persistence/Neo4jManager.java
// Line 56-65: Upsert (MERGE + SET) topic
public static void upsertTopic(String topicName, int messageCount) {
    session.run(
        "MERGE (t:Topic {name: $name}) " +
        "SET t.messageCount = $messageCount, t.lastUpdated = datetime()"
    );
}

// Line 74-82: Upsert producer
public static void upsertProducer(String producerId) {
    session.run(
        "MERGE (p:Producer {id: $id}) " +
        "SET p.lastSeen = datetime()"
    );
}

// Line 91-99: Upsert consumer
public static void upsertConsumer(String consumerId) {
    session.run(
        "MERGE (c:Consumer {id: $id}) " +
        "SET c.lastSeen = datetime()"
    );
}
```

**File Location:** [`Neo4jManager.java:56-103`](file:///c:/Users/real2/OneDrive/Desktop/4EME/javaAv/JAVA_PROJECT-wissversion/JAVA_PROJECT-main%20(1)/JAVA_PROJECT-main/src/main/java/com/streamingplatform/persistence/Neo4jManager.java#L56-L103)

```java
// File: src/main/java/com/streamingplatform/service/MySQLSyncService.java
// Line 69: Update on duplicate key
ON DUPLICATE KEY UPDATE message_count = VALUES(message_count), last_seen = CURRENT_TIMESTAMP

// File: src/main/java/com/streamingplatform/service/Neo4jToMySQLSync.java
// Line 77: Update topics
ON DUPLICATE KEY UPDATE message_count = VALUES(message_count)

// Line 100: Update producers
INSERT INTO producers (producer_id) VALUES (?) ON DUPLICATE KEY UPDATE producer_id = VALUES(producer_id)
```

**File Locations:** 
- [`MySQLSyncService.java:69`](file:///c:/Users/real2/OneDrive/Desktop/4EME/javaAv/JAVA_PROJECT-wissversion/JAVA_PROJECT-main%20(1)/JAVA_PROJECT-main/src/main/java/com/streamingplatform/service/MySQLSyncService.java#L69)
- [`Neo4jToMySQLSync.java:77-220`](file:///c:/Users/real2/OneDrive/Desktop/4EME/javaAv/JAVA_PROJECT-wissversion/JAVA_PROJECT-main%20(1)/JAVA_PROJECT-main/src/main/java/com/streamingplatform/service/Neo4jToMySQLSync.java#L77-L220)

#### 🌐 Neo4j
```cypher
// MERGE (create if not exists) + SET (update)
MERGE (t:Topic {name: $name})
SET t.messageCount = $messageCount, t.lastUpdated = datetime()

// Update relationship properties
MERGE (p)-[r:PUBLISHES_TO]->(t)
SET r.messageCount = $messageCount, r.lastUpdated = datetime()
```

---

### 📊 Summary Table

| Operation | SQL Equivalent | Frontend | Backend | Neo4j |
|-----------|---------------|----------|---------|-------|
| **RS: Select All** | `SELECT *` | `MATCH (n) RETURN n` | `SELECT *` | `MATCH (n) RETURN n` |
| **RC: Relation** | `JOIN` | `MATCH (a)-[r]->(b)` | N/A | `MERGE (a)-[r]->(b)` |
| **RF: Filter** | `WHERE` | Cypher `WHERE` | `WHERE` in SQL | Cypher `WHERE` |
| **RA: Aggregation** | `COUNT/SUM/AVG` | `COUNT/SUM` in Cypher | `GROUP BY` | `COUNT/SUM` |
| **RM: Edit** | `UPDATE` | `SET` in Cypher | `UPDATE / ON DUPLICATE KEY` | `SET / MERGE` |

---

## 👤 Author

Student Project - 4EME Java Course
