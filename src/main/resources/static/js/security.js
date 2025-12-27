/**
 * DB Monitor - Security Dashboard
 * JavaScript for security monitoring and fraud detection
 */

let analytics = null;
let recentAttempts = [];

// Load security data on page load
document.addEventListener('DOMContentLoaded', function() {
    loadSecurityAnalytics();
    loadLoginAttempts();
    initializeRefresh();
});

/**
 * Load security analytics from the API
 */
async function loadSecurityAnalytics() {
    try {
        analytics = await API.get('/api/security/analytics?hours=24');
        displayAnalytics(analytics);
    } catch (error) {
        console.error('Error loading security analytics:', error);
    }
}

/**
 * Display security analytics
 * @param {Object} data - Analytics data
 */
function displayAnalytics(data) {
    document.getElementById('totalAttempts').textContent = Format.abbreviateNumber(data.totalAttempts || 0);
    document.getElementById('failedAttempts').textContent = Format.abbreviateNumber(data.failedAttempts || 0);
    document.getElementById('failureRate').textContent = Format.percent(data.failureRate || 0);
    document.getElementById('suspiciousIps').textContent = (data.suspiciousIps || []).length;
    document.getElementById('lockedAccounts').textContent = (data.lockedAccounts || []).length;
    document.getElementById('uniqueUsers').textContent = (data.uniqueUsers || []).length;

    // Display suspicious IPs if any
    if (data.suspiciousIps && data.suspiciousIps.length > 0) {
        displaySuspiciousIPs(data.suspiciousIps);
    }
}

/**
 * Display suspicious IPs
 * @param {Array} ips - Array of suspicious IP addresses
 */
function displaySuspiciousIPs(ips) {
    const container = document.getElementById('suspiciousIpsList');
    container.innerHTML = ips.slice(0, 5).map(ip => `
        <div class="d-flex justify-content-between align-items-center mb-2">
            <span class="badge bg-danger">${ip.ipAddress}</span>
            <span class="text-muted">${ip.failedAttempts} failed attempts</span>
        </div>
    `).join('');
}

/**
 * Load recent login attempts
 */
async function loadLoginAttempts() {
    try {
        Loading.show('#attemptsCard');
        recentAttempts = await API.get('/api/security/login-attempts/recent?hours=24');
        displayAttempts(recentAttempts);
    } catch (error) {
        console.error('Error loading login attempts:', error);
        document.getElementById('attemptsTable').innerHTML =
            '<tr><td colspan="6" class="text-center text-danger">Error loading login attempts</td></tr>';
    } finally {
        Loading.hide('#attemptsCard');
    }
}

/**
 * Display login attempts in the table
 * @param {Array} attempts - Array of login attempt objects
 */
function displayAttempts(attempts) {
    const tbody = document.getElementById('attemptsTable');

    if (attempts.length === 0) {
        tbody.innerHTML = '<tr><td colspan="6" class="text-center">No login attempts found</td></tr>';
        return;
    }

    tbody.innerHTML = attempts.map(attempt => `
        <tr>
            <td>${Format.date(attempt.attemptTime)}</td>
            <td><strong>${attempt.username}</strong></td>
            <td>
                <span class="badge bg-secondary">${attempt.ipAddress}</span>
                ${attempt.location ? `<br><small class="text-muted">${attempt.location}</small>` : ''}
            </td>
            <td>${formatUserAgent(attempt.userAgent)}</td>
            <td>
                <span class="badge ${attempt.successful ? 'bg-success' : 'bg-danger'}">
                    ${attempt.successful ? 'Success' : 'Failed'}
                </span>
            </td>
            <td>${attempt.failureReason || '-'}</td>
        </tr>
    `).join('');
}

/**
 * Format user agent string
 * @param {string} userAgent - The user agent string
 * @returns {string} Formatted user agent
 */
function formatUserAgent(userAgent) {
    if (!userAgent) return '-';

    // Extract browser and OS info
    let browser = 'Other';
    let os = '';

    if (userAgent.includes('Chrome')) browser = 'Chrome';
    else if (userAgent.includes('Firefox')) browser = 'Firefox';
    else if (userAgent.includes('Safari')) browser = 'Safari';
    else if (userAgent.includes('Edge')) browser = 'Edge';

    if (userAgent.includes('Windows')) os = 'Windows';
    else if (userAgent.includes('Mac')) os = 'Mac';
    else if (userAgent.includes('Linux')) os = 'Linux';
    else if (userAgent.includes('Android')) os = 'Android';
    else if (userAgent.includes('iOS')) os = 'iOS';

    return os ? `${browser} on ${os}` : browser;
}

/**
 * Initialize auto-refresh
 */
function initializeRefresh() {
    // Refresh every 30 seconds
    setInterval(() => {
        loadSecurityAnalytics();
        loadLoginAttempts();
    }, 30000);
}

/**
 * Filter attempts by status
 * @param {string} status - 'all', 'success', or 'failed'
 */
function filterAttempts(status) {
    let filtered = recentAttempts;

    if (status === 'success') {
        filtered = recentAttempts.filter(a => a.successful);
    } else if (status === 'failed') {
        filtered = recentAttempts.filter(a => !a.successful);
    }

    displayAttempts(filtered);

    // Update active filter button
    document.querySelectorAll('.filter-btn').forEach(btn => {
        btn.classList.toggle('active', btn.dataset.filter === status);
    });
}
