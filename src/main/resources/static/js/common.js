/**
 * DB Monitor - Common Utilities
 * Shared JavaScript utilities and helper functions
 */

// ============================================
// TOAST NOTIFICATION SYSTEM
// ============================================

const Toast = {
    /**
     * Show a success toast notification
     * @param {string} message - The message to display
     */
    success: function(message) {
        this.show(message, 'success');
    },

    /**
     * Show an error toast notification
     * @param {string} message - The message to display
     */
    error: function(message) {
        this.show(message, 'danger');
    },

    /**
     * Show a warning toast notification
     * @param {string} message - The message to display
     */
    warning: function(message) {
        this.show(message, 'warning');
    },

    /**
     * Show an info toast notification
     * @param {string} message - The message to display
     */
    info: function(message) {
        this.show(message, 'info');
    },

    /**
     * Show a toast notification
     * @param {string} message - The message to display
     * @param {string} type - The type of toast (success, danger, warning, info)
     */
    show: function(message, type) {
        const iconMap = {
            'success': 'ti-check',
            'danger': 'ti-alert-circle',
            'warning': 'ti-alert-triangle',
            'info': 'ti-info-circle'
        };

        const icon = iconMap[type] || 'ti-info-circle';

        const toast = document.createElement('div');
        toast.className = `alert alert-${type} alert-dismissible fade show position-fixed`;
        toast.style.cssText = 'top: 20px; right: 20px; z-index: 9999; min-width: 300px; max-width: 500px; box-shadow: 0 0.5rem 1rem rgba(0,0,0,0.15);';
        toast.innerHTML = `
            <div class="d-flex">
                <div><i class="ti ${icon} icon alert-icon"></i></div>
                <div>${message}</div>
            </div>
            <button type="button" class="btn-close" data-bs-dismiss="alert" aria-label="Close"></button>
        `;

        document.body.appendChild(toast);

        // Auto dismiss after 5 seconds
        setTimeout(() => {
            toast.classList.remove('show');
            setTimeout(() => toast.remove(), 150);
        }, 5000);
    }
};

// ============================================
// API HELPER FUNCTIONS
// ============================================

const API = {
    /**
     * Perform a GET request
     * @param {string} url - The URL to fetch from
     * @returns {Promise<any>} The response data
     */
    get: async function(url) {
        try {
            const response = await fetch(url, {
                method: 'GET',
                headers: {
                    'Accept': 'application/json'
                }
            });

            if (!response.ok) {
                const errorData = await response.json().catch(() => ({}));
                throw new Error(errorData.error || errorData.message || `HTTP error! status: ${response.status}`);
            }

            return await response.json();
        } catch (error) {
            console.error('API GET error:', error);
            Toast.error(error.message || 'Failed to fetch data');
            throw error;
        }
    },

    /**
     * Perform a POST request
     * @param {string} url - The URL to post to
     * @param {object} data - The data to send
     * @returns {Promise<any>} The response data
     */
    post: async function(url, data) {
        try {
            const response = await fetch(url, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'Accept': 'application/json'
                },
                body: JSON.stringify(data)
            });

            if (!response.ok) {
                const errorData = await response.json().catch(() => ({}));
                throw new Error(errorData.error || errorData.message || 'Request failed');
            }

            return await response.json();
        } catch (error) {
            console.error('API POST error:', error);
            Toast.error(error.message || 'Request failed');
            throw error;
        }
    },

    /**
     * Perform a PUT request
     * @param {string} url - The URL to put to
     * @param {object} data - The data to send
     * @returns {Promise<any>} The response data
     */
    put: async function(url, data) {
        try {
            const response = await fetch(url, {
                method: 'PUT',
                headers: {
                    'Content-Type': 'application/json',
                    'Accept': 'application/json'
                },
                body: JSON.stringify(data)
            });

            if (!response.ok) {
                const errorData = await response.json().catch(() => ({}));
                throw new Error(errorData.error || errorData.message || 'Request failed');
            }

            return await response.json();
        } catch (error) {
            console.error('API PUT error:', error);
            Toast.error(error.message || 'Request failed');
            throw error;
        }
    },

    /**
     * Perform a DELETE request
     * @param {string} url - The URL to delete
     * @returns {Promise<boolean>} Success status
     */
    delete: async function(url) {
        try {
            const response = await fetch(url, {
                method: 'DELETE',
                headers: {
                    'Accept': 'application/json'
                }
            });

            if (!response.ok) {
                const errorData = await response.json().catch(() => ({}));
                throw new Error(errorData.error || errorData.message || 'Delete operation failed');
            }

            return true;
        } catch (error) {
            console.error('API DELETE error:', error);
            Toast.error(error.message || 'Delete operation failed');
            throw error;
        }
    }
};

// ============================================
// FORMAT UTILITIES
// ============================================

const Format = {
    /**
     * Format a timestamp to a localized date string
     * @param {string|number|Date} timestamp - The timestamp to format
     * @returns {string} Formatted date string
     */
    date: function(timestamp) {
        if (!timestamp) return '-';
        const date = new Date(timestamp);
        return date.toLocaleString();
    },

    /**
     * Format a timestamp to a short date
     * @param {string|number|Date} timestamp - The timestamp to format
     * @returns {string} Formatted date string
     */
    dateShort: function(timestamp) {
        if (!timestamp) return '-';
        const date = new Date(timestamp);
        return date.toLocaleDateString();
    },

    /**
     * Format a timestamp to time only
     * @param {string|number|Date} timestamp - The timestamp to format
     * @returns {string} Formatted time string
     */
    time: function(timestamp) {
        if (!timestamp) return '-';
        const date = new Date(timestamp);
        return date.toLocaleTimeString();
    },

    /**
     * Format a number with specified decimal places
     * @param {number} num - The number to format
     * @param {number} decimals - Number of decimal places (default: 2)
     * @returns {string} Formatted number string
     */
    number: function(num, decimals = 2) {
        if (num === null || num === undefined) return '-';
        return Number(num).toFixed(decimals);
    },

    /**
     * Format a number as percentage
     * @param {number} num - The number to format (0-100)
     * @param {number} decimals - Number of decimal places (default: 1)
     * @returns {string} Formatted percentage string
     */
    percent: function(num, decimals = 1) {
        if (num === null || num === undefined) return '-';
        return Number(num).toFixed(decimals) + '%';
    },

    /**
     * Format bytes to human-readable size
     * @param {number} bytes - The number of bytes
     * @returns {string} Formatted size string
     */
    bytes: function(bytes) {
        if (bytes === 0 || bytes === null || bytes === undefined) return '0 Bytes';
        const k = 1024;
        const sizes = ['Bytes', 'KB', 'MB', 'GB', 'TB', 'PB'];
        const i = Math.floor(Math.log(bytes) / Math.log(k));
        return Math.round(bytes / Math.pow(k, i) * 100) / 100 + ' ' + sizes[i];
    },

    /**
     * Format milliseconds to human-readable duration
     * @param {number} ms - Milliseconds
     * @returns {string} Formatted duration string
     */
    duration: function(ms) {
        if (ms === null || ms === undefined) return '-';
        if (ms < 1000) return ms + ' ms';
        if (ms < 60000) return (ms / 1000).toFixed(2) + ' s';
        if (ms < 3600000) return (ms / 60000).toFixed(2) + ' min';
        return (ms / 3600000).toFixed(2) + ' hr';
    },

    /**
     * Format a large number with K/M/B suffixes
     * @param {number} num - The number to format
     * @returns {string} Formatted number string
     */
    abbreviateNumber: function(num) {
        if (num === null || num === undefined) return '-';
        if (num < 1000) return num.toString();
        if (num < 1000000) return (num / 1000).toFixed(1) + 'K';
        if (num < 1000000000) return (num / 1000000).toFixed(1) + 'M';
        return (num / 1000000000).toFixed(1) + 'B';
    }
};

// ============================================
// CONFIRMATION DIALOG
// ============================================

/**
 * Show a confirmation dialog
 * @param {string} message - The confirmation message
 * @param {Function} callback - Function to call if confirmed
 * @param {Object} options - Additional options
 */
function confirmAction(message, callback, options = {}) {
    const title = options.title || 'Confirm Action';
    const confirmText = options.confirmText || 'Confirm';
    const cancelText = options.cancelText || 'Cancel';
    const type = options.type || 'warning'; // success, danger, warning, info

    // Create modal
    const modalId = 'confirmModal_' + Date.now();
    const modalHtml = `
        <div class="modal fade" id="${modalId}" tabindex="-1" aria-hidden="true">
            <div class="modal-dialog modal-dialog-centered">
                <div class="modal-content">
                    <div class="modal-header">
                        <h5 class="modal-title">${title}</h5>
                        <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
                    </div>
                    <div class="modal-body">
                        <p>${message}</p>
                    </div>
                    <div class="modal-footer">
                        <button type="button" class="btn" data-bs-dismiss="modal">${cancelText}</button>
                        <button type="button" class="btn btn-${type}" id="${modalId}_confirm">${confirmText}</button>
                    </div>
                </div>
            </div>
        </div>
    `;

    // Add modal to body
    document.body.insertAdjacentHTML('beforeend', modalHtml);

    const modalEl = document.getElementById(modalId);
    const modal = new bootstrap.Modal(modalEl);

    // Handle confirm button
    document.getElementById(modalId + '_confirm').addEventListener('click', function() {
        modal.hide();
        callback();
    });

    // Remove modal from DOM after it's hidden
    modalEl.addEventListener('hidden.bs.modal', function() {
        modalEl.remove();
    });

    modal.show();
}

// ============================================
// TABLE UTILITIES
// ============================================

const TableUtils = {
    /**
     * Filter table rows based on search input
     * @param {string} inputId - The ID of the search input
     * @param {string} tableId - The ID of the table
     */
    search: function(inputId, tableId) {
        const input = document.getElementById(inputId);
        const table = document.getElementById(tableId);

        if (!input || !table) return;

        input.addEventListener('keyup', function() {
            const filter = this.value.toLowerCase();
            const rows = table.getElementsByTagName('tr');

            for (let i = 1; i < rows.length; i++) { // Skip header row
                const row = rows[i];
                const cells = row.getElementsByTagName('td');
                let found = false;

                for (let j = 0; j < cells.length; j++) {
                    const cell = cells[j];
                    if (cell.textContent.toLowerCase().indexOf(filter) > -1) {
                        found = true;
                        break;
                    }
                }

                row.style.display = found ? '' : 'none';
            }
        });
    },

    /**
     * Sort table by column
     * @param {HTMLTableElement} table - The table element
     * @param {number} column - The column index
     * @param {boolean} asc - Sort ascending (true) or descending (false)
     */
    sort: function(table, column, asc = true) {
        const tbody = table.tBodies[0];
        const rows = Array.from(tbody.rows);

        rows.sort((a, b) => {
            const aText = a.cells[column].textContent.trim();
            const bText = b.cells[column].textContent.trim();

            // Try to parse as numbers
            const aNum = parseFloat(aText);
            const bNum = parseFloat(bText);

            if (!isNaN(aNum) && !isNaN(bNum)) {
                return asc ? aNum - bNum : bNum - aNum;
            }

            // Sort as strings
            return asc ? aText.localeCompare(bText) : bText.localeCompare(aText);
        });

        rows.forEach(row => tbody.appendChild(row));
    }
};

// ============================================
// LOADING UTILITIES
// ============================================

const Loading = {
    /**
     * Show a loading overlay on an element
     * @param {string|HTMLElement} element - The element or selector
     */
    show: function(element) {
        const el = typeof element === 'string' ? document.querySelector(element) : element;
        if (!el) return;

        const overlay = document.createElement('div');
        overlay.className = 'loading-overlay';
        overlay.innerHTML = '<div class="spinner-border text-primary" role="status"><span class="visually-hidden">Loading...</span></div>';
        el.style.position = 'relative';
        el.appendChild(overlay);
    },

    /**
     * Hide the loading overlay on an element
     * @param {string|HTMLElement} element - The element or selector
     */
    hide: function(element) {
        const el = typeof element === 'string' ? document.querySelector(element) : element;
        if (!el) return;

        const overlay = el.querySelector('.loading-overlay');
        if (overlay) {
            overlay.remove();
        }
    }
};

// ============================================
// DEBOUNCE UTILITY
// ============================================

/**
 * Debounce a function call
 * @param {Function} func - The function to debounce
 * @param {number} wait - The wait time in milliseconds
 * @returns {Function} Debounced function
 */
function debounce(func, wait) {
    let timeout;
    return function executedFunction(...args) {
        const later = () => {
            clearTimeout(timeout);
            func(...args);
        };
        clearTimeout(timeout);
        timeout = setTimeout(later, wait);
    };
}

// ============================================
// INITIALIZE ON DOM READY
// ============================================

document.addEventListener('DOMContentLoaded', function() {
    // Initialize tooltips if Bootstrap is loaded
    if (typeof bootstrap !== 'undefined' && bootstrap.Tooltip) {
        const tooltipTriggerList = document.querySelectorAll('[data-bs-toggle="tooltip"]');
        [...tooltipTriggerList].map(tooltipTriggerEl => new bootstrap.Tooltip(tooltipTriggerEl));
    }

    // Initialize popovers if Bootstrap is loaded
    if (typeof bootstrap !== 'undefined' && bootstrap.Popover) {
        const popoverTriggerList = document.querySelectorAll('[data-bs-toggle="popover"]');
        [...popoverTriggerList].map(popoverTriggerEl => new bootstrap.Popover(popoverTriggerEl));
    }
});
