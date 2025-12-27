/**
 * DB Monitor - Dashboard
 * Main dashboard with database metrics and monitoring
 */

let metricsChart = null;
let refreshInterval = null;

// Initialize dashboard
document.addEventListener('DOMContentLoaded', function() {
    initializeCharts();
    startAutoRefresh();
});

/**
 * Initialize Chart.js charts
 */
function initializeCharts() {
    const ctx = document.getElementById('metricsChart');
    if (!ctx) return;

    metricsChart = new Chart(ctx, {
        type: 'line',
        data: {
            labels: [],
            datasets: [
                {
                    label: 'CPU Usage (%)',
                    data: [],
                    borderColor: '#206bc4',
                    backgroundColor: 'rgba(32, 107, 196, 0.1)',
                    tension: 0.4
                },
                {
                    label: 'Memory Usage (%)',
                    data: [],
                    borderColor: '#2fb344',
                    backgroundColor: 'rgba(47, 179, 68, 0.1)',
                    tension: 0.4
                },
                {
                    label: 'Active Connections',
                    data: [],
                    borderColor: '#f76707',
                    backgroundColor: 'rgba(247, 103, 7, 0.1)',
                    tension: 0.4
                }
            ]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            plugins: {
                legend: {
                    position: 'top',
                },
                tooltip: {
                    mode: 'index',
                    intersect: false,
                }
            },
            scales: {
                y: {
                    beginAtZero: true,
                    max: 100
                }
            },
            interaction: {
                mode: 'nearest',
                axis: 'x',
                intersect: false
            }
        }
    });

    // Load initial chart data
    loadChartData();
}

/**
 * Load chart data from API
 */
async function loadChartData() {
    try {
        const connectionId = getSelectedConnectionId();
        if (!connectionId) return;

        const data = await API.get(`/api/connections/${connectionId}/metrics/chart-data?limit=20`);

        if (data && data.length > 0) {
            const labels = data.map(m => new Date(m.timestamp).toLocaleTimeString());
            const cpuData = data.map(m => m.cpuUsage);
            const memoryData = data.map(m => m.memoryUsage);
            const connectionsData = data.map(m => m.activeConnections);

            metricsChart.data.labels = labels;
            metricsChart.data.datasets[0].data = cpuData;
            metricsChart.data.datasets[1].data = memoryData;
            metricsChart.data.datasets[2].data = connectionsData;
            metricsChart.update();
        }
    } catch (error) {
        console.error('Error loading chart data:', error);
    }
}

/**
 * Get selected connection ID from URL
 */
function getSelectedConnectionId() {
    const params = new URLSearchParams(window.location.search);
    return params.get('connectionId');
}

/**
 * Start auto-refresh
 */
function startAutoRefresh() {
    // Refresh every 10 seconds
    refreshInterval = setInterval(() => {
        loadChartData();
        updateLastRefreshTime();
    }, 10000);
}

/**
 * Update last refresh time display
 */
function updateLastRefreshTime() {
    const element = document.getElementById('lastRefreshTime');
    if (element) {
        element.textContent = new Date().toLocaleTimeString();
    }
}

/**
 * Stop auto-refresh
 */
function stopAutoRefresh() {
    if (refreshInterval) {
        clearInterval(refreshInterval);
        refreshInterval = null;
    }
}

// Cleanup on page unload
window.addEventListener('beforeunload', function() {
    stopAutoRefresh();
    if (metricsChart) {
        metricsChart.destroy();
    }
});
