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

---

## 👤 Author

Student Project - 4EME Java Course
