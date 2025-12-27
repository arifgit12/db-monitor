/**
 * DB Monitor - Roles Management
 * JavaScript for managing roles and privileges
 */

let allRoles = [];
let allPrivileges = [];

// Load roles and privileges on page load
document.addEventListener('DOMContentLoaded', function() {
    loadPrivileges();
    loadRoles();
    initializeSearch();
});

/**
 * Load all privileges from the API
 */
async function loadPrivileges() {
    try {
        allPrivileges = await API.get('/api/privileges');

        // Group privileges by category
        const privilegesByCategory = groupPrivilegesByCategory(allPrivileges);

        // Render privilege checkboxes for add modal
        renderPrivilegeCheckboxes('addPrivilegesContainer', privilegesByCategory, []);
    } catch (error) {
        console.error('Error loading privileges:', error);
    }
}

/**
 * Group privileges by category
 * @param {Array} privileges - Array of privilege objects
 * @returns {Object} Privileges grouped by category
 */
function groupPrivilegesByCategory(privileges) {
    const grouped = {};
    privileges.forEach(priv => {
        if (!grouped[priv.category]) {
            grouped[priv.category] = [];
        }
        grouped[priv.category].push(priv);
    });
    return grouped;
}

/**
 * Render privilege checkboxes
 * @param {string} containerId - The container element ID
 * @param {Object} privilegesByCategory - Privileges grouped by category
 * @param {Array} selectedPrivilegeIds - IDs of selected privileges
 */
function renderPrivilegeCheckboxes(containerId, privilegesByCategory, selectedPrivilegeIds) {
    const container = document.getElementById(containerId);
    let html = '';

    for (const [category, privileges] of Object.entries(privilegesByCategory)) {
        html += `<div class="mb-3">`;
        html += `<h4 class="text-primary mb-2">${category}</h4>`;
        privileges.forEach(priv => {
            const isChecked = selectedPrivilegeIds.includes(priv.id) ? 'checked' : '';
            html += `
                <div class="mb-2">
                    <label class="form-check">
                        <input type="checkbox" class="form-check-input" name="privileges" value="${priv.id}" ${isChecked}>
                        <span class="form-check-label">
                            <strong>${priv.name}</strong> - ${priv.description}
                        </span>
                    </label>
                </div>
            `;
        });
        html += `</div>`;
    }

    container.innerHTML = html;
}

/**
 * Load all roles from the API
 */
async function loadRoles() {
    try {
        allRoles = await API.get('/api/roles');
        displayRoles(allRoles);
    } catch (error) {
        console.error('Error loading roles:', error);
        document.getElementById('rolesTableBody').innerHTML =
            '<tr><td colspan="5" class="text-center text-danger">Error loading roles</td></tr>';
    }
}

/**
 * Display roles in the table
 * @param {Array} roles - Array of role objects
 */
function displayRoles(roles) {
    const tbody = document.getElementById('rolesTableBody');
    if (roles.length === 0) {
        tbody.innerHTML = '<tr><td colspan="5" class="text-center">No roles found</td></tr>';
        return;
    }

    tbody.innerHTML = roles.map(role => `
        <tr>
            <td><strong>${role.name}</strong></td>
            <td>${role.description || '-'}</td>
            <td><span class="badge badge-metric bg-azure">${role.privileges.length}</span></td>
            <td>
                <div style="max-width: 400px;">
                    ${role.privileges.slice(0, 5).map(p => `<span class="badge bg-blue me-1 mb-1">${p.name}</span>`).join('')}
                    ${role.privileges.length > 5 ? `<span class="badge bg-secondary">+${role.privileges.length - 5} more</span>` : ''}
                </div>
            </td>
            <td>
                <div class="btn-group">
                    <button class="btn btn-sm btn-primary" onclick="editRole(${role.id})" title="Edit role">
                        <i class="ti ti-pencil"></i>
                    </button>
                    <button class="btn btn-sm btn-danger" onclick="deleteRole(${role.id}, '${role.name}')" title="Delete role">
                        <i class="ti ti-trash"></i>
                    </button>
                </div>
            </td>
        </tr>
    `).join('');
}

/**
 * Save a new role
 */
async function saveRole() {
    const form = document.getElementById('addRoleForm');
    const formData = new FormData(form);

    const selectedPrivileges = Array.from(document.querySelectorAll('#addPrivilegesContainer input[name="privileges"]:checked'))
        .map(cb => parseInt(cb.value));

    const roleData = {
        name: formData.get('name'),
        description: formData.get('description')
    };

    // Validate role name format
    if (!roleData.name.startsWith('ROLE_')) {
        Toast.warning('Role name must start with "ROLE_" prefix');
        return;
    }

    try {
        // Create role
        const newRole = await API.post('/api/roles', roleData);

        // Assign privileges if any selected
        if (selectedPrivileges.length > 0) {
            try {
                await API.post(`/api/roles/${newRole.id}/privileges`, { privilegeIds: selectedPrivileges });
            } catch (privError) {
                console.error('Error assigning privileges, but role was created:', privError);
                Toast.warning('Role created but some privileges could not be assigned');
            }
        }

        const modalElement = document.getElementById('addRoleModal');
        const modalInstance = bootstrap.Modal.getInstance(modalElement);
        if (modalInstance) {
            modalInstance.hide();
        }
        form.reset();
        await loadRoles();
        Toast.success('Role created successfully');
    } catch (error) {
        console.error('Error creating role:', error);
    }
}

/**
 * Open edit modal for a role
 * @param {number} roleId - The role ID
 */
async function editRole(roleId) {
    const role = allRoles.find(r => r.id === roleId);
    if (!role) return;

    document.getElementById('editRoleId').value = role.id;
    document.getElementById('editRoleName').value = role.name;
    document.getElementById('editRoleDescription').value = role.description || '';

    // Group privileges by category
    const privilegesByCategory = groupPrivilegesByCategory(allPrivileges);

    // Render privilege checkboxes with current selections
    const selectedPrivilegeIds = role.privileges.map(p => p.id);
    renderPrivilegeCheckboxes('editPrivilegesContainer', privilegesByCategory, selectedPrivilegeIds);

    new bootstrap.Modal(document.getElementById('editRoleModal')).show();
}

/**
 * Update an existing role
 */
async function updateRole() {
    const roleId = document.getElementById('editRoleId').value;
    const description = document.getElementById('editRoleDescription').value;

    const selectedPrivileges = Array.from(document.querySelectorAll('#editPrivilegesContainer input[name="privileges"]:checked'))
        .map(cb => parseInt(cb.value));

    try {
        // Update role description
        await API.put(`/api/roles/${roleId}`, { description });

        // Update privileges
        try {
            await API.post(`/api/roles/${roleId}/privileges`, { privilegeIds: selectedPrivileges });
        } catch (privError) {
            console.error('Error updating privileges, but description was updated:', privError);
            Toast.warning('Description updated but some privileges could not be assigned');
        }

        const modalElement = document.getElementById('editRoleModal');
        const modalInstance = bootstrap.Modal.getInstance(modalElement);
        if (modalInstance) {
            modalInstance.hide();
        }
        await loadRoles();
        Toast.success('Role updated successfully');
    } catch (error) {
        console.error('Error updating role:', error);
    }
}

/**
 * Delete a role
 * @param {number} roleId - The role ID
 * @param {string} roleName - The role name
 */
async function deleteRole(roleId, roleName) {
    confirmAction(
        `Delete role "${roleName}"? This action cannot be undone.`,
        async () => {
            try {
                await API.delete(`/api/roles/${roleId}`);
                await loadRoles();
                Toast.success('Role deleted successfully');
            } catch (error) {
                console.error('Error deleting role:', error);
            }
        },
        {
            title: 'Delete Role',
            confirmText: 'Delete',
            type: 'danger'
        }
    );
}

/**
 * Initialize search functionality
 */
function initializeSearch() {
    document.getElementById('searchInput').addEventListener('input', function(e) {
        const searchTerm = e.target.value.toLowerCase();
        const filtered = allRoles.filter(role =>
            role.name.toLowerCase().includes(searchTerm) ||
            (role.description && role.description.toLowerCase().includes(searchTerm))
        );
        displayRoles(filtered);
    });
}
