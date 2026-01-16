/**
 * Streaming Platform Dashboard - Full CRUD Application
 * Connects to Neo4j database with Create, Read, Update, Delete operations
 */

const API_BASE_URL = 'http://localhost:3001/api';

// =====================================================
// API SERVICE
// =====================================================
const ApiService = {
    async checkHealth() {
        try {
            const response = await fetch(`${API_BASE_URL}/health`);
            return response.ok;
        } catch {
            return false;
        }
    },

    // READ operations
    async getTopics() {
        try {
            const response = await fetch(`${API_BASE_URL}/topics`);
            if (!response.ok) throw new Error('Failed to fetch topics');
            return await response.json();
        } catch (error) {
            console.error('[API] Topics error:', error);
            return [];
        }
    },

    async getProducers() {
        try {
            const response = await fetch(`${API_BASE_URL}/producers`);
            if (!response.ok) throw new Error('Failed to fetch producers');
            return await response.json();
        } catch (error) {
            console.error('[API] Producers error:', error);
            return [];
        }
    },

    async getConsumers() {
        try {
            const response = await fetch(`${API_BASE_URL}/consumers`);
            if (!response.ok) throw new Error('Failed to fetch consumers');
            return await response.json();
        } catch (error) {
            console.error('[API] Consumers error:', error);
            return [];
        }
    },

    async getStats() {
        try {
            const response = await fetch(`${API_BASE_URL}/stats`);
            if (!response.ok) throw new Error('Failed to fetch stats');
            return await response.json();
        } catch (error) {
            console.error('[API] Stats error:', error);
            return { topics: 0, producers: 0, consumers: 0, relations: 0, nodes: 0 };
        }
    },

    async getTopicDistribution() {
        try {
            const response = await fetch(`${API_BASE_URL}/charts/topic-distribution`);
            if (!response.ok) throw new Error('Failed');
            return await response.json();
        } catch {
            return [];
        }
    },

    async getEntityDistribution() {
        try {
            const response = await fetch(`${API_BASE_URL}/charts/entity-distribution`);
            if (!response.ok) throw new Error('Failed');
            return await response.json();
        } catch {
            return [];
        }
    },

    // CREATE operations
    async createTopic(name, messageCount = 0) {
        const response = await fetch(`${API_BASE_URL}/topics`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ name, messageCount })
        });
        return await response.json();
    },

    async createProducer(id) {
        const response = await fetch(`${API_BASE_URL}/producers`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ id })
        });
        return await response.json();
    },

    async createConsumer(id, group = '') {
        const response = await fetch(`${API_BASE_URL}/consumers`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ id, group })
        });
        return await response.json();
    },

    // UPDATE operations
    async updateTopic(name, data) {
        const response = await fetch(`${API_BASE_URL}/topics/${encodeURIComponent(name)}`, {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(data)
        });
        return await response.json();
    },

    async updateProducer(id, data) {
        const response = await fetch(`${API_BASE_URL}/producers/${encodeURIComponent(id)}`, {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(data)
        });
        return await response.json();
    },

    async updateConsumer(id, data) {
        const response = await fetch(`${API_BASE_URL}/consumers/${encodeURIComponent(id)}`, {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(data)
        });
        return await response.json();
    },

    // DELETE operations
    async deleteTopic(name) {
        const response = await fetch(`${API_BASE_URL}/topics/${encodeURIComponent(name)}`, {
            method: 'DELETE'
        });
        return await response.json();
    },

    async deleteProducer(id) {
        const response = await fetch(`${API_BASE_URL}/producers/${encodeURIComponent(id)}`, {
            method: 'DELETE'
        });
        return await response.json();
    },

    async deleteConsumer(id) {
        const response = await fetch(`${API_BASE_URL}/consumers/${encodeURIComponent(id)}`, {
            method: 'DELETE'
        });
        return await response.json();
    }
};

// =====================================================
// DASHBOARD APPLICATION
// =====================================================
class StreamingDashboard {
    constructor() {
        this.isStreaming = true;
        this.charts = {};
        this.updateInterval = null;
        this.streamInterval = null;
        this.useApi = false;
        this.currentEditItem = null;
        this.currentEditType = null;
        this.currentSection = 'dashboard';
        this.analyticsChartsInitialized = false;

        this.init();
    }

    async init() {
        this.useApi = await ApiService.checkHealth();
        this.updateConnectionStatus(this.useApi);

        this.setupNavigation();
        this.setupEventListeners();
        this.initCharts();
        await this.loadDashboardData();
        this.startRealTimeUpdates();
        this.startMessageStream();
        this.startDynamicChartUpdates();
    }

    updateConnectionStatus(isConnected) {
        const syncStatus = document.querySelector('.sync-status span:last-child');
        const statusDot = document.querySelector('.status-dot');

        if (syncStatus) {
            syncStatus.textContent = isConnected ? 'Neo4j Live' : 'Demo Mode';
        }
        if (statusDot) {
            statusDot.classList.toggle('active', isConnected);
            statusDot.style.background = isConnected ? '#10b981' : '#f59e0b';
        }
    }

    setupNavigation() {
        const navItems = document.querySelectorAll('.nav-item[data-section]');
        navItems.forEach(item => {
            item.addEventListener('click', (e) => {
                e.preventDefault();
                navItems.forEach(nav => nav.classList.remove('active'));
                item.classList.add('active');

                const section = item.dataset.section;
                this.switchSection(section);
            });
        });
    }

    switchSection(section) {
        this.currentSection = section;

        // Get sections
        const dashboardSection = document.querySelector('.main-content:not(.analytics-section)');
        const analyticsSection = document.getElementById('analytics-section');

        // Hide all sections
        if (dashboardSection) dashboardSection.style.display = 'none';
        if (analyticsSection) analyticsSection.style.display = 'none';

        // Show selected section
        if (section === 'analytics') {
            if (analyticsSection) {
                analyticsSection.style.display = 'block';
                if (!this.analyticsChartsInitialized) {
                    this.initAnalyticsCharts();
                    this.analyticsChartsInitialized = true;
                }
                this.loadAnalyticsData();
            }
        } else {
            if (dashboardSection) dashboardSection.style.display = 'block';
        }
    }

    setupEventListeners() {
        document.getElementById('play-btn')?.addEventListener('click', () => {
            this.isStreaming = true;
            document.getElementById('play-btn').classList.add('active');
            document.getElementById('pause-btn').classList.remove('active');
        });

        document.getElementById('pause-btn')?.addEventListener('click', () => {
            this.isStreaming = false;
            document.getElementById('pause-btn').classList.add('active');
            document.getElementById('play-btn').classList.remove('active');
        });

        document.getElementById('chart-timeframe')?.addEventListener('change', (e) => {
            this.updateThroughputChart(parseInt(e.target.value));
        });

        document.getElementById('sync-btn')?.addEventListener('click', async (e) => {
            e.preventDefault();
            this.showToast('Refreshing data...', 'success');
            await this.loadDashboardData();
            this.showToast('Data refreshed!', 'success');
        });

        document.getElementById('export-btn')?.addEventListener('click', (e) => {
            e.preventDefault();
            this.exportData();
        });

        // Modal submit
        document.getElementById('modal-submit')?.addEventListener('click', () => this.handleModalSubmit());
    }

    // =====================================================
    // CHARTS
    // =====================================================
    initCharts() {
        this.initThroughputChart();
        this.initPieChart();
        this.initBarChart();
    }

    initThroughputChart() {
        const ctx = document.getElementById('throughput-chart')?.getContext('2d');
        if (!ctx) return;

        const labels = [];
        const data = [];
        for (let i = 0; i < 12; i++) {
            labels.push(new Date(Date.now() - (12 - i) * 5 * 60000).toLocaleTimeString('en-US', { hour: '2-digit', minute: '2-digit' }));
            data.push(Math.floor(Math.random() * 500) + 200);
        }

        this.charts.throughput = new Chart(ctx, {
            type: 'line',
            data: {
                labels,
                datasets: [{
                    label: 'Messages/min',
                    data,
                    borderColor: '#00d4ff',
                    backgroundColor: 'rgba(0, 212, 255, 0.1)',
                    fill: true,
                    tension: 0.4,
                    pointRadius: 0
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                plugins: { legend: { display: false } },
                scales: {
                    x: { grid: { color: 'rgba(255,255,255,0.05)' }, ticks: { color: 'rgba(255,255,255,0.5)' } },
                    y: { grid: { color: 'rgba(255,255,255,0.05)' }, ticks: { color: 'rgba(255,255,255,0.5)' }, beginAtZero: true }
                }
            }
        });
    }

    async initPieChart() {
        const ctx = document.getElementById('entity-pie-chart')?.getContext('2d');
        if (!ctx) return;

        const data = await ApiService.getEntityDistribution();
        const labels = data.map(d => d.name);
        const values = data.map(d => d.count);

        this.charts.pie = new Chart(ctx, {
            type: 'doughnut',
            data: {
                labels,
                datasets: [{
                    data: values,
                    backgroundColor: ['#00d4ff', '#7c3aed', '#ec4899'],
                    borderWidth: 0
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                plugins: {
                    legend: {
                        position: 'bottom',
                        labels: { color: 'rgba(255,255,255,0.7)', padding: 20 }
                    }
                }
            }
        });
    }

    async initBarChart() {
        const ctx = document.getElementById('topics-bar-chart')?.getContext('2d');
        if (!ctx) return;

        const data = await ApiService.getTopicDistribution();
        const labels = data.slice(0, 8).map(d => d.name.length > 15 ? d.name.slice(0, 15) + '...' : d.name);
        const values = data.slice(0, 8).map(d => d.count);

        this.charts.bar = new Chart(ctx, {
            type: 'bar',
            data: {
                labels,
                datasets: [{
                    label: 'Messages',
                    data: values,
                    backgroundColor: 'rgba(124, 58, 237, 0.6)',
                    borderColor: '#7c3aed',
                    borderWidth: 1,
                    borderRadius: 4
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                plugins: { legend: { display: false } },
                scales: {
                    x: {
                        grid: { display: false },
                        ticks: { color: 'rgba(255,255,255,0.5)', maxRotation: 45 }
                    },
                    y: {
                        grid: { color: 'rgba(255,255,255,0.05)' },
                        ticks: { color: 'rgba(255,255,255,0.5)' },
                        beginAtZero: true
                    }
                }
            }
        });
    }

    // =====================================================
    // ANALYTICS CHARTS
    // =====================================================
    async initAnalyticsCharts() {
        // Throughput chart for analytics
        const throughputCtx = document.getElementById('analytics-throughput-chart')?.getContext('2d');
        if (throughputCtx) {
            const labels = [];
            const data = [];
            for (let i = 0; i < 24; i++) {
                labels.push(`${i}:00`);
                data.push(Math.floor(Math.random() * 800) + 200);
            }
            this.charts.analyticsThroughput = new Chart(throughputCtx, {
                type: 'line',
                data: {
                    labels,
                    datasets: [{
                        label: 'Messages/hour',
                        data,
                        borderColor: '#00d4ff',
                        backgroundColor: 'rgba(0, 212, 255, 0.15)',
                        fill: true,
                        tension: 0.4,
                        pointRadius: 2,
                        pointHoverRadius: 6
                    }]
                },
                options: {
                    responsive: true,
                    maintainAspectRatio: false,
                    plugins: { legend: { display: false } },
                    scales: {
                        x: { grid: { color: 'rgba(255,255,255,0.05)' }, ticks: { color: 'rgba(255,255,255,0.5)' } },
                        y: { grid: { color: 'rgba(255,255,255,0.05)' }, ticks: { color: 'rgba(255,255,255,0.5)' }, beginAtZero: true }
                    }
                }
            });
        }

        // Entity pie chart for analytics
        const entityCtx = document.getElementById('analytics-entity-chart')?.getContext('2d');
        if (entityCtx) {
            const data = await ApiService.getEntityDistribution();
            this.charts.analyticsEntity = new Chart(entityCtx, {
                type: 'doughnut',
                data: {
                    labels: data.map(d => d.name),
                    datasets: [{
                        data: data.map(d => d.count),
                        backgroundColor: ['#00d4ff', '#7c3aed', '#ec4899'],
                        borderWidth: 0
                    }]
                },
                options: {
                    responsive: true,
                    maintainAspectRatio: false,
                    plugins: {
                        legend: { position: 'bottom', labels: { color: 'rgba(255,255,255,0.7)', padding: 15 } }
                    }
                }
            });
        }

        // Producer activity bar chart
        const producerCtx = document.getElementById('analytics-producer-chart')?.getContext('2d');
        if (producerCtx) {
            const producers = await ApiService.getProducers();
            const topProducers = producers.slice(0, 8);
            this.charts.analyticsProducer = new Chart(producerCtx, {
                type: 'bar',
                data: {
                    labels: topProducers.map(p => (p.id || '').slice(-10)),
                    datasets: [{
                        label: 'Messages',
                        data: topProducers.map(p => p.messageCount || 0),
                        backgroundColor: 'rgba(124, 58, 237, 0.6)',
                        borderColor: '#7c3aed',
                        borderWidth: 1,
                        borderRadius: 4
                    }]
                },
                options: {
                    responsive: true,
                    maintainAspectRatio: false,
                    indexAxis: 'y',
                    plugins: { legend: { display: false } },
                    scales: {
                        x: { grid: { color: 'rgba(255,255,255,0.05)' }, ticks: { color: 'rgba(255,255,255,0.5)' } },
                        y: { grid: { display: false }, ticks: { color: 'rgba(255,255,255,0.5)' } }
                    }
                }
            });
        }

        // Topic distribution line/area chart
        const topicCtx = document.getElementById('analytics-topic-chart')?.getContext('2d');
        if (topicCtx) {
            const topics = await ApiService.getTopicDistribution();
            const topTopics = topics.slice(0, 10);
            this.charts.analyticsTopic = new Chart(topicCtx, {
                type: 'bar',
                data: {
                    labels: topTopics.map(t => t.name.length > 12 ? t.name.slice(0, 12) + '...' : t.name),
                    datasets: [{
                        label: 'Messages',
                        data: topTopics.map(t => t.count),
                        backgroundColor: 'rgba(236, 72, 153, 0.6)',
                        borderColor: '#ec4899',
                        borderWidth: 1,
                        borderRadius: 4
                    }]
                },
                options: {
                    responsive: true,
                    maintainAspectRatio: false,
                    plugins: { legend: { display: false } },
                    scales: {
                        x: { grid: { display: false }, ticks: { color: 'rgba(255,255,255,0.5)', maxRotation: 45 } },
                        y: { grid: { color: 'rgba(255,255,255,0.05)' }, ticks: { color: 'rgba(255,255,255,0.5)' } }
                    }
                }
            });
        }
    }

    async loadAnalyticsData() {
        const stats = await ApiService.getStats();

        // Update analytics stats
        this.animateValue('db-topics-count', stats.topics);
        this.animateValue('db-producers-count', stats.producers);
        this.animateValue('db-consumers-count', stats.consumers);
        this.animateValue('db-relations-count', stats.relations);

        // Calculate averages
        const avgTopicsPerProducer = stats.producers > 0 ? (stats.topics / stats.producers).toFixed(1) : 0;
        const avgProducersPerTopic = stats.topics > 0 ? (stats.producers / stats.topics).toFixed(1) : 0;

        // Update relationship metrics
        const publishesEl = document.getElementById('publishes-count');
        const subscribesEl = document.getElementById('subscribes-count');
        const avgTopicsEl = document.getElementById('avg-topics-producer');
        const avgProducersEl = document.getElementById('avg-producers-topic');

        if (publishesEl) publishesEl.textContent = this.formatNumber(Math.floor(stats.relations * 0.6));
        if (subscribesEl) subscribesEl.textContent = this.formatNumber(Math.floor(stats.relations * 0.4));
        if (avgTopicsEl) avgTopicsEl.textContent = avgTopicsPerProducer;
        if (avgProducersEl) avgProducersEl.textContent = avgProducersPerTopic;
    }

    // =====================================================
    // DYNAMIC CHART UPDATES
    // =====================================================
    startDynamicChartUpdates() {
        // Update charts every 3 seconds for dynamic feel
        this.chartUpdateInterval = setInterval(() => {
            this.updateDynamicCharts();
        }, 3000);
    }

    updateDynamicCharts() {
        // Update Dashboard throughput chart with sliding window
        if (this.charts.throughput) {
            const chart = this.charts.throughput;
            const data = chart.data.datasets[0].data;

            // Remove first point, add new random point at end (sliding window)
            data.shift();
            data.push(Math.floor(Math.random() * 300) + 300 + Math.sin(Date.now() / 5000) * 100);

            // Update labels with current time
            chart.data.labels.shift();
            chart.data.labels.push(new Date().toLocaleTimeString('en-US', { hour: '2-digit', minute: '2-digit' }));

            chart.update('none'); // 'none' for smooth animation
        }

        // Update Analytics throughput chart if visible
        if (this.charts.analyticsThroughput && this.currentSection === 'analytics') {
            const chart = this.charts.analyticsThroughput;
            const data = chart.data.datasets[0].data;

            // Add some variation to each data point
            for (let i = 0; i < data.length; i++) {
                const variation = (Math.random() - 0.5) * 100;
                data[i] = Math.max(100, Math.min(1000, data[i] + variation));
            }

            chart.update('none');
        }

        // Update pie chart data slightly for animation effect
        if (this.charts.pie) {
            const chart = this.charts.pie;
            const data = chart.data.datasets[0].data;

            // Add tiny variations to show it's live
            for (let i = 0; i < data.length; i++) {
                const variation = Math.floor((Math.random() - 0.5) * 50);
                data[i] = Math.max(0, data[i] + variation);
            }

            chart.update('none');
        }

        // Update bar chart if visible
        if (this.charts.bar) {
            const chart = this.charts.bar;
            const data = chart.data.datasets[0].data;

            for (let i = 0; i < data.length; i++) {
                const variation = Math.floor((Math.random() - 0.5) * 30);
                data[i] = Math.max(0, data[i] + variation);
            }

            chart.update('none');
        }

        // Update analytics entity chart if visible
        if (this.charts.analyticsEntity && this.currentSection === 'analytics') {
            const chart = this.charts.analyticsEntity;
            const data = chart.data.datasets[0].data;

            for (let i = 0; i < data.length; i++) {
                const variation = Math.floor((Math.random() - 0.5) * 100);
                data[i] = Math.max(0, data[i] + variation);
            }

            chart.update('none');
        }

        // Update analytics producer chart
        if (this.charts.analyticsProducer && this.currentSection === 'analytics') {
            const chart = this.charts.analyticsProducer;
            const data = chart.data.datasets[0].data;

            for (let i = 0; i < data.length; i++) {
                const variation = Math.floor((Math.random() - 0.5) * 20);
                data[i] = Math.max(0, data[i] + variation);
            }

            chart.update('none');
        }

        // Update analytics topic chart
        if (this.charts.analyticsTopic && this.currentSection === 'analytics') {
            const chart = this.charts.analyticsTopic;
            const data = chart.data.datasets[0].data;

            for (let i = 0; i < data.length; i++) {
                const variation = Math.floor((Math.random() - 0.5) * 50);
                data[i] = Math.max(0, data[i] + variation);
            }

            chart.update('none');
        }
    }

    updateThroughputChart(hours) {
        if (!this.charts.throughput) return;
        const points = hours === 1 ? 12 : hours === 6 ? 24 : 48;
        const labels = [];
        const data = [];
        for (let i = 0; i < points; i++) {
            labels.push(new Date(Date.now() - (points - i) * (hours * 60 * 60 * 1000 / points)).toLocaleTimeString('en-US', { hour: '2-digit', minute: '2-digit' }));
            data.push(Math.floor(Math.random() * 500) + 200);
        }
        this.charts.throughput.data.labels = labels;
        this.charts.throughput.data.datasets[0].data = data;
        this.charts.throughput.update();
    }

    // =====================================================
    // DATA LOADING
    // =====================================================
    async loadDashboardData() {
        await Promise.all([
            this.updateStats(),
            this.updateTopicsTable(),
            this.updateProducersList()
        ]);
    }

    async updateStats() {
        const stats = await ApiService.getStats();
        this.animateValue('h2-messages', 15000);
        this.animateValue('mysql-topics', stats.topics);
        this.animateValue('mysql-producers', stats.producers);
        this.animateValue('neo4j-nodes', stats.nodes);
        this.animateValue('neo4j-relations', stats.relations);
        this.animateValue('total-topics', stats.topics);
        this.animateValue('total-producers', stats.producers);
        this.animateValue('total-consumers', stats.consumers);
        this.animateValue('total-messages', stats.relations);
    }

    async updateTopicsTable() {
        const topics = await ApiService.getTopics();
        const tbody = document.querySelector('#topics-table tbody');
        if (!tbody) return;

        if (topics.length === 0) {
            tbody.innerHTML = `<tr><td colspan="4" style="text-align:center;color:var(--text-muted);padding:40px;">No topics found</td></tr>`;
            return;
        }

        tbody.innerHTML = topics.slice(0, 10).map(topic => `
            <tr>
                <td>
                    <div class="topic-name">
                        <span class="topic-dot" style="background: ${this.getColor(topic.name)}"></span>
                        ${topic.name}
                    </div>
                </td>
                <td>${this.formatNumber(topic.messageCount || 0)}</td>
                <td>${topic.producerCount || 0}</td>
                <td>
                    <div class="action-btns">
                        <button class="action-btn edit" onclick="dashboard.editTopic('${topic.name}')" title="Edit">
                            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                                <path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"/>
                                <path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"/>
                            </svg>
                        </button>
                        <button class="action-btn delete" onclick="dashboard.confirmDelete('topic', '${topic.name}')" title="Delete">
                            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                                <polyline points="3 6 5 6 21 6"/>
                                <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"/>
                            </svg>
                        </button>
                    </div>
                </td>
            </tr>
        `).join('');
    }

    async updateProducersList() {
        const producers = await ApiService.getProducers();
        const container = document.getElementById('producers-list');
        if (!container) return;

        if (producers.length === 0) {
            container.innerHTML = `<div style="text-align:center;color:var(--text-muted);padding:40px;">No producers found</div>`;
            return;
        }

        container.innerHTML = producers.slice(0, 6).map(p => `
            <div class="producer-item">
                <div class="producer-avatar">${(p.id || 'N/A').slice(-3).toUpperCase()}</div>
                <div class="producer-info">
                    <div class="producer-id">${p.id || 'Unknown'}</div>
                    <div class="producer-topics">${p.topicCount || 0} topics</div>
                </div>
                <div class="producer-messages">
                    <div class="count">${this.formatNumber(p.messageCount || 0)}</div>
                    <div class="label">msgs</div>
                </div>
            </div>
        `).join('');
    }

    // =====================================================
    // CRUD OPERATIONS
    // =====================================================
    editTopic(name) {
        this.currentEditItem = name;
        this.currentEditType = 'topic';
        openModal('topic', true, name);
    }

    async confirmDelete(type, id) {
        document.getElementById('delete-message').textContent = `Are you sure you want to delete ${type} "${id}"?`;
        document.getElementById('delete-modal').classList.add('show');

        document.getElementById('delete-confirm').onclick = async () => {
            closeDeleteModal();
            try {
                if (type === 'topic') await ApiService.deleteTopic(id);
                else if (type === 'producer') await ApiService.deleteProducer(id);
                else if (type === 'consumer') await ApiService.deleteConsumer(id);

                this.showToast(`${type} deleted successfully!`, 'success');
                await this.loadDashboardData();
            } catch (error) {
                this.showToast('Delete failed: ' + error.message, 'error');
            }
        };
    }

    async handleModalSubmit() {
        const type = document.getElementById('modal-type')?.value;
        const isEdit = document.getElementById('modal-edit')?.value === 'true';

        try {
            if (type === 'topic') {
                const name = document.getElementById('topic-name').value;
                const messageCount = parseInt(document.getElementById('topic-messages')?.value) || 0;

                if (!name) {
                    this.showToast('Topic name is required', 'error');
                    return;
                }

                if (isEdit) {
                    await ApiService.updateTopic(this.currentEditItem, { newName: name, messageCount });
                    this.showToast('Topic updated successfully!', 'success');
                } else {
                    await ApiService.createTopic(name, messageCount);
                    this.showToast('Topic created successfully!', 'success');
                }
            } else if (type === 'producer') {
                const id = document.getElementById('producer-id').value;
                if (!id) {
                    this.showToast('Producer ID is required', 'error');
                    return;
                }
                await ApiService.createProducer(id);
                this.showToast('Producer created successfully!', 'success');
            } else if (type === 'consumer') {
                const id = document.getElementById('consumer-id').value;
                const group = document.getElementById('consumer-group')?.value || '';
                if (!id) {
                    this.showToast('Consumer ID is required', 'error');
                    return;
                }
                await ApiService.createConsumer(id, group);
                this.showToast('Consumer created successfully!', 'success');
            }

            closeModal();
            await this.loadDashboardData();
        } catch (error) {
            this.showToast('Operation failed: ' + error.message, 'error');
        }
    }

    // =====================================================
    // REAL-TIME & STREAM
    // =====================================================
    startRealTimeUpdates() {
        this.updateInterval = setInterval(async () => {
            this.useApi = await ApiService.checkHealth();
            this.updateConnectionStatus(this.useApi);
            await this.loadDashboardData();
            document.getElementById('last-update').textContent = 'Updated just now';
        }, 10000);
    }

    startMessageStream() {
        const topics = ['user-events', 'system-logs', 'transactions', 'analytics', 'alerts'];
        const messages = ['User session started', 'Transaction completed', 'System check passed', 'New registration', 'Cache updated'];

        this.streamInterval = setInterval(() => {
            if (!this.isStreaming) return;

            const container = document.getElementById('message-stream');
            if (!container) return;

            const msg = document.createElement('div');
            msg.className = 'message-item';
            msg.innerHTML = `
                <span class="message-time">${new Date().toLocaleTimeString()}</span>
                <span class="message-topic">[${topics[Math.floor(Math.random() * topics.length)]}]</span>
                <span class="message-content">${messages[Math.floor(Math.random() * messages.length)]}</span>
            `;
            container.insertBefore(msg, container.firstChild);

            const items = container.querySelectorAll('.message-item');
            if (items.length > 15) items[items.length - 1].remove();
        }, 2000);
    }

    // =====================================================
    // UTILITIES
    // =====================================================
    async exportData() {
        this.showToast('Exporting data...', 'success');
        const [topics, producers] = await Promise.all([ApiService.getTopics(), ApiService.getProducers()]);
        const blob = new Blob([JSON.stringify({ exportedAt: new Date().toISOString(), topics, producers }, null, 2)], { type: 'application/json' });
        const a = document.createElement('a');
        a.href = URL.createObjectURL(blob);
        a.download = `streaming-export-${Date.now()}.json`;
        a.click();
        this.showToast('Export completed!', 'success');
    }

    animateValue(id, newValue) {
        const el = document.getElementById(id);
        if (!el) return;
        const current = parseInt(el.textContent.replace(/,/g, '')) || 0;
        const diff = newValue - current;
        let step = 0;
        const animate = () => {
            step++;
            el.textContent = this.formatNumber(Math.round(current + (diff * step / 20)));
            if (step < 20) requestAnimationFrame(animate);
        };
        animate();
    }

    formatNumber(num) {
        return num.toString().replace(/\B(?=(\d{3})+(?!\d))/g, ',');
    }

    getColor(name) {
        const colors = ['#00d4ff', '#7c3aed', '#ec4899', '#10b981', '#f59e0b', '#3b82f6'];
        return colors[(name || '').split('').reduce((a, c) => a + c.charCodeAt(0), 0) % colors.length];
    }

    showToast(message, type = 'success') {
        const toast = document.getElementById('toast');
        if (!toast) return;
        toast.className = `toast ${type} show`;
        toast.querySelector('.toast-message').textContent = message;
        toast.querySelector('.toast-icon').innerHTML = type === 'success'
            ? '<svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="2"><polyline points="20,6 9,17 4,12"/></svg>'
            : '<svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/></svg>';
        setTimeout(() => toast.classList.remove('show'), 3000);
    }
}

// =====================================================
// MODAL FUNCTIONS (Global)
// =====================================================
function openModal(type, isEdit = false, editValue = '') {
    const overlay = document.getElementById('modal-overlay');
    const title = document.getElementById('modal-title');
    const body = document.getElementById('modal-body');
    const submitBtn = document.getElementById('modal-submit');

    title.textContent = isEdit ? `Edit ${type.charAt(0).toUpperCase() + type.slice(1)}` : `Add New ${type.charAt(0).toUpperCase() + type.slice(1)}`;
    submitBtn.textContent = isEdit ? 'Update' : 'Create';

    if (type === 'topic') {
        body.innerHTML = `
            <input type="hidden" id="modal-type" value="topic">
            <input type="hidden" id="modal-edit" value="${isEdit}">
            <div class="form-group">
                <label class="form-label">Topic Name</label>
                <input type="text" class="form-input" id="topic-name" value="${editValue}" placeholder="e.g., user-events">
            </div>
            <div class="form-group">
                <label class="form-label">Message Count</label>
                <input type="number" class="form-input" id="topic-messages" value="0" placeholder="0">
            </div>
        `;
    } else if (type === 'producer') {
        body.innerHTML = `
            <input type="hidden" id="modal-type" value="producer">
            <input type="hidden" id="modal-edit" value="false">
            <div class="form-group">
                <label class="form-label">Producer ID</label>
                <input type="text" class="form-input" id="producer-id" placeholder="e.g., producer-001">
            </div>
        `;
    } else if (type === 'consumer') {
        body.innerHTML = `
            <input type="hidden" id="modal-type" value="consumer">
            <input type="hidden" id="modal-edit" value="false">
            <div class="form-group">
                <label class="form-label">Consumer ID</label>
                <input type="text" class="form-input" id="consumer-id" placeholder="e.g., consumer-001">
            </div>
            <div class="form-group">
                <label class="form-label">Consumer Group (optional)</label>
                <input type="text" class="form-input" id="consumer-group" placeholder="e.g., group-1">
            </div>
        `;
    }

    overlay.classList.add('show');
}

function closeModal() {
    document.getElementById('modal-overlay').classList.remove('show');
}

function closeDeleteModal() {
    document.getElementById('delete-modal').classList.remove('show');
}

// Initialize
document.addEventListener('DOMContentLoaded', () => {
    window.dashboard = new StreamingDashboard();
});
