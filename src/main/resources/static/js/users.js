/**
 * DB Monitor - User Management
 * JavaScript for managing users and roles
 */

let allUsers = [];
let allRoles = [];

// Load users and roles on page load
document.addEventListener('DOMContentLoaded', function() {
    loadRoles();
    loadUsers();
});

/**
 * Load all roles from the API
 */
async function loadRoles() {
    try {
        allRoles = await API.get('/api/roles');

        // Populate role selects
        const rolesSelect = document.getElementById('rolesSelect');
        const editRolesSelect = document.getElementById('editRolesSelect');

        allRoles.forEach(role => {
            const option = new Option(role.name, role.name);
            rolesSelect.add(option.cloneNode(true));
            editRolesSelect.add(option);
        });
    } catch (error) {
        console.error('Error loading roles:', error);
    }
}

/**
 * Load all users from the API
 */
async function loadUsers() {
    try {
        allUsers = await API.get('/api/users');
        displayUsers(allUsers);
    } catch (error) {
        console.error('Error loading users:', error);
        document.getElementById('usersTableBody').innerHTML =
            '<tr><td colspan="6" class="text-center text-danger">Error loading users</td></tr>';
    }
}

/**
 * Display users in the table
 * @param {Array} users - Array of user objects
 */
function displayUsers(users) {
    const tbody = document.getElementById('usersTableBody');
    if (users.length === 0) {
        tbody.innerHTML = '<tr><td colspan="6" class="text-center">No users found</td></tr>';
        return;
    }

    tbody.innerHTML = users.map(user => `
        <tr>
            <td><strong>${user.username}</strong></td>
            <td>${user.email || '-'}</td>
            <td>
                ${user.roles.map(role => `<span class="badge bg-blue">${role.name}</span>`).join(' ')}
            </td>
            <td>
                ${user.enabled
                    ? '<span class="badge bg-success">Active</span>'
                    : '<span class="badge bg-danger">Disabled</span>'}
                ${user.accountLocked
                    ? '<span class="badge bg-warning ms-1">Locked</span>'
                    : ''}
            </td>
            <td>${Format.date(user.lastLogin) || 'Never'}</td>
            <td>
                <div class="btn-group">
                    <button class="btn btn-sm btn-primary" onclick="editUser(${user.id})" title="Edit user">
                        <i class="ti ti-pencil"></i>
                    </button>
                    ${user.accountLocked ? `
                        <button class="btn btn-sm btn-warning" onclick="unlockUser(${user.id})" title="Unlock account">
                            <i class="ti ti-lock-open"></i>
                        </button>
                    ` : ''}
                    <button class="btn btn-sm btn-danger" onclick="deleteUser(${user.id}, '${user.username}')" title="Delete user">
                        <i class="ti ti-trash"></i>
                    </button>
                </div>
            </td>
        </tr>
    `).join('');
}

/**
 * Save a new user
 */
async function saveUser() {
    const form = document.getElementById('addUserForm');
    const formData = new FormData(form);

    const selectedRoles = Array.from(document.getElementById('rolesSelect').selectedOptions)
        .map(option => option.value);

    if (selectedRoles.length === 0) {
        Toast.warning('Please select at least one role');
        return;
    }

    const userData = {
        username: formData.get('username'),
        password: formData.get('password'),
        email: formData.get('email'),
        phone: formData.get('phone'),
        roles: selectedRoles
    };

    try {
        await API.post('/api/users', userData);
        bootstrap.Modal.getInstance(document.getElementById('addUserModal')).hide();
        form.reset();
        await loadUsers();
        Toast.success('User created successfully');
    } catch (error) {
        // Error already handled by API helper
        console.error('Error creating user:', error);
    }
}

/**
 * Open edit modal for a user
 * @param {number} userId - The user ID
 */
function editUser(userId) {
    const user = allUsers.find(u => u.id === userId);
    if (!user) return;

    document.getElementById('editUserId').value = user.id;
    document.getElementById('editUsername').value = user.username;
    document.getElementById('editEmail').value = user.email || '';
    document.getElementById('editPhone').value = user.phone || '';
    document.getElementById('editEnabled').checked = user.enabled;

    // Select user's roles
    const editRolesSelect = document.getElementById('editRolesSelect');
    Array.from(editRolesSelect.options).forEach(option => {
        option.selected = user.roles.some(role => role.name === option.value);
    });

    new bootstrap.Modal(document.getElementById('editUserModal')).show();
}

/**
 * Update an existing user
 */
async function updateUser() {
    const userId = document.getElementById('editUserId').value;
    const email = document.getElementById('editEmail').value;
    const phone = document.getElementById('editPhone').value;
    const enabled = document.getElementById('editEnabled').checked;

    const selectedRoles = Array.from(document.getElementById('editRolesSelect').selectedOptions)
        .map(option => option.value);

    if (selectedRoles.length === 0) {
        Toast.warning('Please select at least one role');
        return;
    }

    try {
        // Update user details
        await API.put(`/api/users/${userId}`, { email, phone, enabled });

        // Update user roles
        await API.put(`/api/users/${userId}/roles`, { roles: selectedRoles });

        bootstrap.Modal.getInstance(document.getElementById('editUserModal')).hide();
        await loadUsers();
        Toast.success('User updated successfully');
    } catch (error) {
        console.error('Error updating user:', error);
    }
}

/**
 * Unlock a user account
 * @param {number} userId - The user ID
 */
async function unlockUser(userId) {
    confirmAction(
        'Are you sure you want to unlock this user account?',
        async () => {
            try {
                const response = await fetch(`/api/users/${userId}/unlock`, {
                    method: 'POST'
                });

                if (response.ok) {
                    await loadUsers();
                    Toast.success('User account unlocked successfully');
                } else {
                    Toast.error('Error unlocking user account');
                }
            } catch (error) {
                Toast.error('Error unlocking user: ' + error.message);
            }
        },
        {
            title: 'Unlock User Account',
            confirmText: 'Unlock',
            type: 'warning'
        }
    );
}

/**
 * Delete a user
 * @param {number} userId - The user ID
 * @param {string} username - The username
 */
async function deleteUser(userId, username) {
    confirmAction(
        `Delete user "${username}"? This action cannot be undone.`,
        async () => {
            try {
                await API.delete(`/api/users/${userId}`);
                await loadUsers();
                Toast.success('User deleted successfully');
            } catch (error) {
                console.error('Error deleting user:', error);
            }
        },
        {
            title: 'Delete User',
            confirmText: 'Delete',
            type: 'danger'
        }
    );
}

// Search functionality
document.addEventListener('DOMContentLoaded', function() {
    document.getElementById('searchInput').addEventListener('input', function(e) {
        const searchTerm = e.target.value.toLowerCase();
        const filtered = allUsers.filter(user =>
            user.username.toLowerCase().includes(searchTerm) ||
            (user.email && user.email.toLowerCase().includes(searchTerm))
        );
        displayUsers(filtered);
    });
});
