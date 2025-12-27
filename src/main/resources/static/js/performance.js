/**
 * DB Monitor - Performance Charts
 * Performance metrics visualization with Chart.js
 */

async function initializePerformanceCharts() {
    // Auto-refresh every 20 seconds
    setTimeout(() => location.reload(), 20000);

    // Get selected connection ID
    let connectionId = getConnectionIdFromPage();
    console.log('Performance page - Initial connection ID:', connectionId);

    // If connectionId is 0 or invalid, try to fetch from API
    if (!connectionId || connectionId === 0) {
        console.warn('Connection ID is 0! Attempting to fetch active connections from API...');

        try {
            const connections = await API.get('/api/connections/active');
            console.log('Active connections from API:', connections);

            if (connections && connections.length > 0) {
                const defaultConn = connections.find(c => c.isDefault) || connections[0];
                connectionId = defaultConn.id;
                console.log('Using connection ID from API:', connectionId);
            } else {
                console.error('No active connections found');
                return;
            }
        } catch (error) {
            console.error('Error fetching connections:', error);
            return;
        }
    }

    console.log('Fetching metrics for connection ID:', connectionId);

    try {
        // Fetch metrics data
        const data = await API.get(`/api/connections/${connectionId}/metrics/history?limit=50`);

        // Validate data
        if (!Array.isArray(data) || data.length === 0) {
            throw new Error('No metrics data available');
        }

        const timestamps = data.map(m => new Date(m.timestamp).toLocaleTimeString());
        const activeConnections = data.map(m => m.activeConnections);
        const cpuUsage = data.map(m => m.cpuUsage);
        const memoryUsage = data.map(m => m.memoryUsage);
        const connectionUsage = data.map(m => m.connectionUsagePercent);

        // Calculate summaries
        document.getElementById('avgCpu').textContent =
            (cpuUsage.reduce((a, b) => a + b, 0) / cpuUsage.length).toFixed(1) + '%';
        document.getElementById('avgMemory').textContent =
            (memoryUsage.reduce((a, b) => a + b, 0) / memoryUsage.length).toFixed(1) + '%';
        document.getElementById('peakConnections').textContent =
            Math.max(...activeConnections);
        document.getElementById('avgConnections').textContent =
            (activeConnections.reduce((a, b) => a + b, 0) / activeConnections.length).toFixed(0);

        // Common chart options
        const commonOptions = {
            responsive: true,
            maintainAspectRatio: false,
            interaction: {
                mode: 'index',
                intersect: false
            },
            plugins: {
                legend: {
                    display: true,
                    position: 'top'
                }
            },
            scales: {
                x: {
                    display: true,
                    ticks: {
                        maxRotation: 45,
                        minRotation: 0,
                        autoSkip: true,
                        maxTicksLimit: 10
                    }
                }
            },
            animation: {
                duration: 0
            }
        };

        // Connections Chart
        new Chart(document.getElementById('connectionsChart'), {
            type: 'line',
            data: {
                labels: timestamps,
                datasets: [{
                    label: 'Active Connections',
                    data: activeConnections,
                    borderColor: '#206bc4',
                    backgroundColor: 'rgba(32, 107, 196, 0.1)',
                    tension: 0.3,
                    fill: true,
                    borderWidth: 2,
                    pointRadius: 3,
                    pointHoverRadius: 5
                }]
            },
            options: {
                ...commonOptions,
                scales: {
                    ...commonOptions.scales,
                    y: {
                        beginAtZero: true,
                        ticks: {
                            callback: function(value) {
                                return Math.round(value);
                            }
                        }
                    }
                }
            }
        });

        // CPU Chart
        new Chart(document.getElementById('cpuChart'), {
            type: 'line',
            data: {
                labels: timestamps,
                datasets: [{
                    label: 'CPU Usage (%)',
                    data: cpuUsage,
                    borderColor: '#d63939',
                    backgroundColor: 'rgba(214, 57, 57, 0.1)',
                    tension: 0.3,
                    fill: true,
                    borderWidth: 2,
                    pointRadius: 3,
                    pointHoverRadius: 5
                }]
            },
            options: {
                ...commonOptions,
                scales: {
                    ...commonOptions.scales,
                    y: {
                        beginAtZero: true,
                        max: 100,
                        ticks: {
                            callback: function(value) {
                                return value + '%';
                            }
                        }
                    }
                }
            }
        });

        // Memory Chart
        new Chart(document.getElementById('memoryChart'), {
            type: 'line',
            data: {
                labels: timestamps,
                datasets: [{
                    label: 'Memory Usage (%)',
                    data: memoryUsage,
                    borderColor: '#2fb344',
                    backgroundColor: 'rgba(47, 179, 68, 0.1)',
                    tension: 0.3,
                    fill: true,
                    borderWidth: 2,
                    pointRadius: 3,
                    pointHoverRadius: 5
                }]
            },
            options: {
                ...commonOptions,
                scales: {
                    ...commonOptions.scales,
                    y: {
                        beginAtZero: true,
                        max: 100,
                        ticks: {
                            callback: function(value) {
                                return value + '%';
                            }
                        }
                    }
                }
            }
        });

        // Pool Utilization Chart
        new Chart(document.getElementById('poolChart'), {
            type: 'line',
            data: {
                labels: timestamps,
                datasets: [{
                    label: 'Pool Usage (%)',
                    data: connectionUsage,
                    borderColor: '#f76707',
                    backgroundColor: 'rgba(247, 103, 7, 0.1)',
                    tension: 0.3,
                    fill: true,
                    borderWidth: 2,
                    pointRadius: 3,
                    pointHoverRadius: 5
                }]
            },
            options: {
                ...commonOptions,
                scales: {
                    ...commonOptions.scales,
                    y: {
                        beginAtZero: true,
                        max: 100,
                        ticks: {
                            callback: function(value) {
                                return value + '%';
                            }
                        }
                    }
                }
            }
        });
    } catch (error) {
        console.error('Error loading performance data:', error);
        Toast.error('Failed to load performance data');

        // Display error message on all charts
        ['connectionsChart', 'cpuChart', 'memoryChart', 'poolChart'].forEach(chartId => {
            const canvas = document.getElementById(chartId);
            if (canvas) {
                const ctx = canvas.getContext('2d');
                ctx.font = '16px Arial';
                ctx.fillStyle = '#d63939';
                ctx.textAlign = 'center';
                ctx.fillText('Failed to load chart. Check console for details.', canvas.width / 2, canvas.height / 2);
            }
        });
    }
}

/**
 * Get connection ID from URL or page data
 */
function getConnectionIdFromPage() {
    const params = new URLSearchParams(window.location.search);
    return params.get('connectionId');
}

// Initialize when DOM is ready
if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', initializePerformanceCharts);
} else {
    initializePerformanceCharts();
}
