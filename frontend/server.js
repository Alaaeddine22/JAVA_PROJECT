/**
 * Streaming Platform API Server
 * Connects to Neo4j database with CRUD operations
 * 
 * Neo4j Connection Details:
 * - URI: bolt://localhost:7687
 * - Database: neo4j
 * - User: neo4j
 * - Password: ALAA2004@
 */

const express = require('express');
const cors = require('cors');
const neo4j = require('neo4j-driver');

const app = express();
const PORT = 3001;

// Neo4j connection configuration
const NEO4J_URI = 'neo4j://127.0.0.1:7687';
const NEO4J_USER = 'neo4j';
const NEO4J_PASSWORD = 'ALAA2004@';
const NEO4J_DATABASE = 'neo4j';

// Create Neo4j driver
let driver;
try {
    driver = neo4j.driver(NEO4J_URI, neo4j.auth.basic(NEO4J_USER, NEO4J_PASSWORD));
    console.log('[Neo4j] Driver created successfully');
} catch (error) {
    console.error('[Neo4j] Failed to create driver:', error.message);
}

// Middleware
app.use(cors());
app.use(express.json());

// Helper function to run Neo4j queries
async function runQuery(cypher, params = {}) {
    const session = driver.session({ database: NEO4J_DATABASE });
    try {
        const result = await session.run(cypher, params);
        return result.records;
    } finally {
        await session.close();
    }
}

// Helper to extract numeric value
function getNumber(val) {
    if (val === null || val === undefined) return 0;
    return val.toNumber ? val.toNumber() : val;
}

// =====================================================
// READ ENDPOINTS
// =====================================================

// Health check
app.get('/api/health', async (req, res) => {
    try {
        await driver.verifyConnectivity();
        res.json({ status: 'healthy', neo4j: 'connected' });
    } catch (error) {
        res.status(500).json({ status: 'unhealthy', error: error.message });
    }
});

// Get all topics with statistics
app.get('/api/topics', async (req, res) => {
    try {
        const records = await runQuery(`
            MATCH (t:Topic)
            OPTIONAL MATCH (p:Producer)-[r:PUBLISHES_TO]->(t)
            RETURN t.name as name, 
                   COALESCE(t.messageCount, 0) as messageCount,
                   COUNT(DISTINCT p) as producerCount,
                   t.lastUpdated as lastUpdated
            ORDER BY messageCount DESC
            LIMIT 100
        `);

        const topics = records.map(record => ({
            name: record.get('name'),
            messageCount: getNumber(record.get('messageCount')),
            producerCount: getNumber(record.get('producerCount')),
            status: 'active'
        }));

        res.json(topics);
    } catch (error) {
        console.error('[API] Error fetching topics:', error.message);
        res.status(500).json({ error: error.message });
    }
});

// Get all producers with statistics
app.get('/api/producers', async (req, res) => {
    try {
        const records = await runQuery(`
            MATCH (p:Producer)
            OPTIONAL MATCH (p)-[r:PUBLISHES_TO]->(t:Topic)
            RETURN p.id as id,
                   COUNT(DISTINCT t) as topicCount,
                   COALESCE(SUM(r.messageCount), 0) as messageCount,
                   p.lastSeen as lastSeen
            ORDER BY messageCount DESC
            LIMIT 100
        `);

        const producers = records.map(record => ({
            id: record.get('id'),
            topicCount: getNumber(record.get('topicCount')),
            messageCount: getNumber(record.get('messageCount')),
            lastSeen: record.get('lastSeen')
        }));

        res.json(producers);
    } catch (error) {
        console.error('[API] Error fetching producers:', error.message);
        res.status(500).json({ error: error.message });
    }
});

// Get all consumers
app.get('/api/consumers', async (req, res) => {
    try {
        const records = await runQuery(`
            MATCH (c:Consumer)
            OPTIONAL MATCH (c)-[r:SUBSCRIBES_TO]->(t:Topic)
            RETURN c.id as id,
                   COUNT(DISTINCT t) as topicCount,
                   c.lastSeen as lastSeen
            ORDER BY topicCount DESC
            LIMIT 100
        `);

        const consumers = records.map(record => ({
            id: record.get('id'),
            topicCount: getNumber(record.get('topicCount')),
            lastSeen: record.get('lastSeen')
        }));

        res.json(consumers);
    } catch (error) {
        console.error('[API] Error fetching consumers:', error.message);
        res.status(500).json({ error: error.message });
    }
});

// Database statistics
app.get('/api/stats', async (req, res) => {
    try {
        const [topicsResult, producersResult, consumersResult, relationsResult] = await Promise.all([
            runQuery('MATCH (t:Topic) RETURN COUNT(t) as count'),
            runQuery('MATCH (p:Producer) RETURN COUNT(p) as count'),
            runQuery('MATCH (c:Consumer) RETURN COUNT(c) as count'),
            runQuery('MATCH ()-[r]->() RETURN COUNT(r) as count')
        ]);

        const getValue = (result) => result.length === 0 ? 0 : getNumber(result[0].get('count'));

        res.json({
            topics: getValue(topicsResult),
            producers: getValue(producersResult),
            consumers: getValue(consumersResult),
            relations: getValue(relationsResult),
            nodes: getValue(topicsResult) + getValue(producersResult) + getValue(consumersResult)
        });
    } catch (error) {
        console.error('[API] Error fetching stats:', error.message);
        res.status(500).json({ error: error.message });
    }
});

// Chart data endpoint - topic distribution by message count
app.get('/api/charts/topic-distribution', async (req, res) => {
    try {
        const records = await runQuery(`
            MATCH (t:Topic)
            RETURN t.name as name, COALESCE(t.messageCount, 0) as count
            ORDER BY count DESC
            LIMIT 10
        `);

        const data = records.map(r => ({
            name: r.get('name'),
            count: getNumber(r.get('count'))
        }));

        res.json(data);
    } catch (error) {
        res.status(500).json({ error: error.message });
    }
});

// Chart data endpoint - producer activity
app.get('/api/charts/producer-activity', async (req, res) => {
    try {
        const records = await runQuery(`
            MATCH (p:Producer)-[r:PUBLISHES_TO]->(t:Topic)
            RETURN p.id as id, COUNT(DISTINCT t) as topics, SUM(r.messageCount) as messages
            ORDER BY messages DESC
            LIMIT 10
        `);

        const data = records.map(r => ({
            id: r.get('id'),
            topics: getNumber(r.get('topics')),
            messages: getNumber(r.get('messages'))
        }));

        res.json(data);
    } catch (error) {
        res.status(500).json({ error: error.message });
    }
});

// Chart data endpoint - entity distribution (for pie chart)
app.get('/api/charts/entity-distribution', async (req, res) => {
    try {
        const [topics, producers, consumers] = await Promise.all([
            runQuery('MATCH (t:Topic) RETURN COUNT(t) as count'),
            runQuery('MATCH (p:Producer) RETURN COUNT(p) as count'),
            runQuery('MATCH (c:Consumer) RETURN COUNT(c) as count')
        ]);

        res.json([
            { name: 'Topics', count: getNumber(topics[0]?.get('count')) },
            { name: 'Producers', count: getNumber(producers[0]?.get('count')) },
            { name: 'Consumers', count: getNumber(consumers[0]?.get('count')) }
        ]);
    } catch (error) {
        res.status(500).json({ error: error.message });
    }
});

// =====================================================
// CRUD - TOPICS
// =====================================================

// Create topic
app.post('/api/topics', async (req, res) => {
    try {
        const { name, messageCount = 0 } = req.body;
        if (!name) {
            return res.status(400).json({ error: 'Topic name is required' });
        }

        await runQuery(`
            MERGE (t:Topic {name: $name})
            SET t.messageCount = $messageCount, t.lastUpdated = datetime()
        `, { name, messageCount: neo4j.int(messageCount) });

        res.json({ success: true, message: `Topic '${name}' created successfully` });
    } catch (error) {
        console.error('[API] Error creating topic:', error.message);
        res.status(500).json({ error: error.message });
    }
});

// Update topic
app.put('/api/topics/:name', async (req, res) => {
    try {
        const { name } = req.params;
        const { newName, messageCount } = req.body;

        let setClause = 't.lastUpdated = datetime()';
        const params = { name };

        if (newName) {
            setClause += ', t.name = $newName';
            params.newName = newName;
        }
        if (messageCount !== undefined) {
            setClause += ', t.messageCount = $messageCount';
            params.messageCount = neo4j.int(messageCount);
        }

        const result = await runQuery(`
            MATCH (t:Topic {name: $name})
            SET ${setClause}
            RETURN t
        `, params);

        if (result.length === 0) {
            return res.status(404).json({ error: 'Topic not found' });
        }

        res.json({ success: true, message: `Topic '${name}' updated successfully` });
    } catch (error) {
        console.error('[API] Error updating topic:', error.message);
        res.status(500).json({ error: error.message });
    }
});

// Delete topic
app.delete('/api/topics/:name', async (req, res) => {
    try {
        const { name } = req.params;

        const result = await runQuery(`
            MATCH (t:Topic {name: $name})
            DETACH DELETE t
            RETURN count(*) as deleted
        `, { name });

        res.json({ success: true, message: `Topic '${name}' deleted successfully` });
    } catch (error) {
        console.error('[API] Error deleting topic:', error.message);
        res.status(500).json({ error: error.message });
    }
});

// =====================================================
// CRUD - PRODUCERS
// =====================================================

// Create producer
app.post('/api/producers', async (req, res) => {
    try {
        const { id } = req.body;
        if (!id) {
            return res.status(400).json({ error: 'Producer ID is required' });
        }

        await runQuery(`
            MERGE (p:Producer {id: $id})
            SET p.lastSeen = datetime()
        `, { id });

        res.json({ success: true, message: `Producer '${id}' created successfully` });
    } catch (error) {
        console.error('[API] Error creating producer:', error.message);
        res.status(500).json({ error: error.message });
    }
});

// Update producer
app.put('/api/producers/:id', async (req, res) => {
    try {
        const { id } = req.params;
        const { newId } = req.body;

        let setClause = 'p.lastSeen = datetime()';
        const params = { id };

        if (newId) {
            setClause += ', p.id = $newId';
            params.newId = newId;
        }

        const result = await runQuery(`
            MATCH (p:Producer {id: $id})
            SET ${setClause}
            RETURN p
        `, params);

        if (result.length === 0) {
            return res.status(404).json({ error: 'Producer not found' });
        }

        res.json({ success: true, message: `Producer '${id}' updated successfully` });
    } catch (error) {
        console.error('[API] Error updating producer:', error.message);
        res.status(500).json({ error: error.message });
    }
});

// Delete producer
app.delete('/api/producers/:id', async (req, res) => {
    try {
        const { id } = req.params;

        await runQuery(`
            MATCH (p:Producer {id: $id})
            DETACH DELETE p
        `, { id });

        res.json({ success: true, message: `Producer '${id}' deleted successfully` });
    } catch (error) {
        console.error('[API] Error deleting producer:', error.message);
        res.status(500).json({ error: error.message });
    }
});

// =====================================================
// CRUD - CONSUMERS
// =====================================================

// Create consumer
app.post('/api/consumers', async (req, res) => {
    try {
        const { id, group } = req.body;
        if (!id) {
            return res.status(400).json({ error: 'Consumer ID is required' });
        }

        await runQuery(`
            MERGE (c:Consumer {id: $id})
            SET c.lastSeen = datetime(), c.group = $group
        `, { id, group: group || null });

        res.json({ success: true, message: `Consumer '${id}' created successfully` });
    } catch (error) {
        console.error('[API] Error creating consumer:', error.message);
        res.status(500).json({ error: error.message });
    }
});

// Update consumer
app.put('/api/consumers/:id', async (req, res) => {
    try {
        const { id } = req.params;
        const { newId, group } = req.body;

        let setClause = 'c.lastSeen = datetime()';
        const params = { id };

        if (newId) {
            setClause += ', c.id = $newId';
            params.newId = newId;
        }
        if (group !== undefined) {
            setClause += ', c.group = $group';
            params.group = group;
        }

        const result = await runQuery(`
            MATCH (c:Consumer {id: $id})
            SET ${setClause}
            RETURN c
        `, params);

        if (result.length === 0) {
            return res.status(404).json({ error: 'Consumer not found' });
        }

        res.json({ success: true, message: `Consumer '${id}' updated successfully` });
    } catch (error) {
        console.error('[API] Error updating consumer:', error.message);
        res.status(500).json({ error: error.message });
    }
});

// Delete consumer
app.delete('/api/consumers/:id', async (req, res) => {
    try {
        const { id } = req.params;

        await runQuery(`
            MATCH (c:Consumer {id: $id})
            DETACH DELETE c
        `, { id });

        res.json({ success: true, message: `Consumer '${id}' deleted successfully` });
    } catch (error) {
        console.error('[API] Error deleting consumer:', error.message);
        res.status(500).json({ error: error.message });
    }
});

// =====================================================
// RELATIONSHIPS
// =====================================================

// Create PUBLISHES_TO relationship
app.post('/api/relationships/publishes', async (req, res) => {
    try {
        const { producerId, topicName, messageCount = 0 } = req.body;
        if (!producerId || !topicName) {
            return res.status(400).json({ error: 'Producer ID and Topic name are required' });
        }

        await runQuery(`
            MATCH (p:Producer {id: $producerId})
            MATCH (t:Topic {name: $topicName})
            MERGE (p)-[r:PUBLISHES_TO]->(t)
            SET r.messageCount = $messageCount, r.lastUpdated = datetime()
        `, { producerId, topicName, messageCount: neo4j.int(messageCount) });

        res.json({ success: true, message: 'Relationship created successfully' });
    } catch (error) {
        res.status(500).json({ error: error.message });
    }
});

// Create SUBSCRIBES_TO relationship
app.post('/api/relationships/subscribes', async (req, res) => {
    try {
        const { consumerId, topicName } = req.body;
        if (!consumerId || !topicName) {
            return res.status(400).json({ error: 'Consumer ID and Topic name are required' });
        }

        await runQuery(`
            MATCH (c:Consumer {id: $consumerId})
            MATCH (t:Topic {name: $topicName})
            MERGE (c)-[r:SUBSCRIBES_TO]->(t)
            SET r.lastUpdated = datetime()
        `, { consumerId, topicName });

        res.json({ success: true, message: 'Subscription created successfully' });
    } catch (error) {
        res.status(500).json({ error: error.message });
    }
});

// Graceful shutdown
process.on('SIGINT', async () => {
    console.log('\n[Server] Shutting down...');
    await driver.close();
    process.exit(0);
});

// Start server
app.listen(PORT, () => {
    console.log(`
╔═══════════════════════════════════════════════════════════════════════╗
║     Streaming Platform API Server - With CRUD Support                 ║
╠═══════════════════════════════════════════════════════════════════════╣
║  Server:    http://localhost:${PORT}                                    ║
║  Neo4j:     ${NEO4J_URI}                                    ║
║  Database:  ${NEO4J_DATABASE}                                              ║
╚═══════════════════════════════════════════════════════════════════════╝

READ Endpoints:
  GET  /api/health                      - Health check
  GET  /api/stats                       - Database statistics
  GET  /api/topics                      - All topics
  GET  /api/producers                   - All producers
  GET  /api/consumers                   - All consumers
  GET  /api/charts/topic-distribution   - Chart data: topic distribution
  GET  /api/charts/producer-activity    - Chart data: producer activity
  GET  /api/charts/entity-distribution  - Chart data: entity pie chart

CRUD - Topics:
  POST   /api/topics          - Create topic {name, messageCount}
  PUT    /api/topics/:name    - Update topic {newName, messageCount}
  DELETE /api/topics/:name    - Delete topic

CRUD - Producers:
  POST   /api/producers       - Create producer {id}
  PUT    /api/producers/:id   - Update producer {newId}
  DELETE /api/producers/:id   - Delete producer

CRUD - Consumers:
  POST   /api/consumers       - Create consumer {id, group}
  PUT    /api/consumers/:id   - Update consumer {newId, group}
  DELETE /api/consumers/:id   - Delete consumer

Relationships:
  POST /api/relationships/publishes  - {producerId, topicName, messageCount}
  POST /api/relationships/subscribes - {consumerId, topicName}
`);
});
