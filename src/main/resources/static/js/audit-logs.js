/**
 * DB Monitor - Audit Logs
 * JavaScript for audit log viewing and filtering
 */

let allLogs = [];
let currentFilter = 'all';

// Load audit logs on page load
document.addEventListener('DOMContentLoaded', function() {
    loadAuditLogs();
    initializeFilters();
    initializeRefresh();
});

/**
 * Load audit logs from the API
 */
async function loadAuditLogs() {
    try {
        Loading.show('#logsCard');
        allLogs = await API.get('/api/audit-logs/recent?hours=24');
        displayLogs(allLogs);
        updateStats(allLogs);
    } catch (error) {
        console.error('Error loading audit logs:', error);
        document.getElementById('logsTable').innerHTML =
            '<tr><td colspan="7" class="text-center text-danger">Error loading audit logs</td></tr>';
    } finally {
        Loading.hide('#logsCard');
    }
}

/**
 * Display logs in the table
 * @param {Array} logs - Array of log objects
 */
function displayLogs(logs) {
    const tbody = document.getElementById('logsTable');

    if (logs.length === 0) {
        tbody.innerHTML = '<tr><td colspan="7" class="text-center">No audit logs found</td></tr>';
        return;
    }

    tbody.innerHTML = logs.map(log => `
        <tr>
            <td>${Format.date(log.timestamp)}</td>
            <td><strong>${log.username || '-'}</strong></td>
            <td>
                <span class="badge ${getActionBadgeClass(log.action)}">${log.action}</span>
            </td>
            <td>${log.description || '-'}</td>
            <td>${log.ipAddress || '-'}</td>
            <td>${log.userAgent ? formatUserAgent(log.userAgent) : '-'}</td>
            <td>
                <span class="badge ${log.status === 'SUCCESS' ? 'bg-success' : 'bg-danger'}">
                    ${log.status}
                </span>
            </td>
        </tr>
    `).join('');
}

/**
 * Get badge class based on action type
 * @param {string} action - The action type
 * @returns {string} Badge class
 */
function getActionBadgeClass(action) {
    if (action.startsWith('LOGIN')) return 'bg-blue';
    if (action.startsWith('CREATE')) return 'bg-green';
    if (action.startsWith('UPDATE')) return 'bg-yellow';
    if (action.startsWith('DELETE')) return 'bg-red';
    return 'bg-secondary';
}

/**
 * Format user agent string
 * @param {string} userAgent - The user agent string
 * @returns {string} Formatted user agent
 */
function formatUserAgent(userAgent) {
    if (!userAgent) return '-';

    // Extract browser info
    if (userAgent.includes('Chrome')) return 'Chrome';
    if (userAgent.includes('Firefox')) return 'Firefox';
    if (userAgent.includes('Safari')) return 'Safari';
    if (userAgent.includes('Edge')) return 'Edge';

    return 'Other';
}

/**
 * Update statistics
 * @param {Array} logs - Array of log objects
 */
function updateStats(logs) {
    const total = logs.length;
    const successful = logs.filter(log => log.status === 'SUCCESS').length;
    const failed = logs.filter(log => log.status === 'FAILURE').length;
    const successRate = total > 0 ? ((successful / total) * 100).toFixed(1) : 0;

    document.getElementById('totalLogs').textContent = total;
    document.getElementById('successfulLogs').textContent = successful;
    document.getElementById('failedLogs').textContent = failed;
    document.getElementById('successRate').textContent = successRate + '%';
}

/**
 * Initialize filter buttons
 */
function initializeFilters() {
    document.querySelectorAll('.filter-btn').forEach(btn => {
        btn.addEventListener('click', function() {
            currentFilter = this.dataset.filter;

            // Update active state
            document.querySelectorAll('.filter-btn').forEach(b => b.classList.remove('active'));
            this.classList.add('active');

            // Filter logs
            filterLogs();
        });
    });

    // Search functionality
    document.getElementById('searchInput').addEventListener('input', debounce(function(e) {
        filterLogs();
    }, 300));
}

/**
 * Filter logs based on current filter and search
 */
function filterLogs() {
    const searchTerm = document.getElementById('searchInput').value.toLowerCase();

    let filtered = allLogs;

    // Apply action filter
    if (currentFilter !== 'all') {
        filtered = filtered.filter(log => {
            if (currentFilter === 'success') return log.status === 'SUCCESS';
            if (currentFilter === 'failure') return log.status === 'FAILURE';
            if (currentFilter === 'login') return log.action.includes('LOGIN');
            if (currentFilter === 'user') return log.action.includes('USER') || log.action.includes('ROLE');
            return true;
        });
    }

    // Apply search filter
    if (searchTerm) {
        filtered = filtered.filter(log =>
            (log.username && log.username.toLowerCase().includes(searchTerm)) ||
            (log.action && log.action.toLowerCase().includes(searchTerm)) ||
            (log.description && log.description.toLowerCase().includes(searchTerm)) ||
            (log.ipAddress && log.ipAddress.toLowerCase().includes(searchTerm))
        );
    }

    displayLogs(filtered);
}

/**
 * Initialize auto-refresh
 */
function initializeRefresh() {
    // Refresh every 30 seconds
    setInterval(() => {
        loadAuditLogs();
    }, 30000);
}

/**
 * Export logs to CSV
 */
async function exportLogs() {
    try {
        const csv = convertToCSV(allLogs);
        downloadCSV(csv, 'audit-logs-' + new Date().toISOString().split('T')[0] + '.csv');
        Toast.success('Audit logs exported successfully');
    } catch (error) {
        Toast.error('Error exporting logs');
    }
}

/**
 * Convert logs to CSV format
 * @param {Array} logs - Array of log objects
 * @returns {string} CSV string
 */
function convertToCSV(logs) {
    const headers = ['Timestamp', 'Username', 'Action', 'Description', 'IP Address', 'User Agent', 'Status'];
    const rows = logs.map(log => [
        Format.date(log.timestamp),
        log.username || '',
        log.action || '',
        (log.description || '').replace(/,/g, ';'),
        log.ipAddress || '',
        (log.userAgent || '').replace(/,/g, ';'),
        log.status || ''
    ]);

    return [headers, ...rows].map(row => row.join(',')).join('\n');
}

/**
 * Download CSV file
 * @param {string} csv - CSV content
 * @param {string} filename - File name
 */
function downloadCSV(csv, filename) {
    const blob = new Blob([csv], { type: 'text/csv' });
    const url = window.URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = filename;
    a.click();
    window.URL.revokeObjectURL(url);
}
