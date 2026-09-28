/**
 * Civil Services Officer Posting & Transfer Optimization System
 * Global Utility Script
 */

const API_BASE = (window.location.pathname.includes('/officer/') || 
                  window.location.pathname.includes('/admin/') || 
                  window.location.pathname.includes('/committee/')) ? '../api/' : 'api/';

// Current Session Data
let currentUser = null;

// Enforce Session & Role-Based Authorization
async function initSession(allowedRoles = []) {
    try {
        const response = await fetch(API_BASE + '../login');
        if (!response.ok) {
            throw new Error('Servlet backend unavailable (HTTP ' + response.status + ')');
        }
        const data = await response.json();

        if (data.status === 'unauthenticated') {
            window.location.href = (API_BASE.includes('../') ? '../' : '') + 'login.html';
            return null;
        }

        currentUser = data;

        // Verify role authorization
        if (allowedRoles.length > 0 && !allowedRoles.includes(data.role)) {
            document.body.innerHTML = `
                <div style="height: 100vh; display: flex; align-items: center; justify-content: center; background: #f8fafc; font-family: sans-serif;">
                    <div style="text-align: center; background: white; padding: 3rem; border-radius: 12px; box-shadow: 0 10px 25px rgba(0,0,0,0.1); max-width: 450px;">
                        <div style="font-size: 3rem; color: #dc2626; margin-bottom: 1rem;">⚠️</div>
                        <h1 style="color: #0f172a; margin-bottom: 0.5rem; font-size: 1.8rem;">Access Denied</h1>
                        <p style="color: #64748b; margin-bottom: 1.5rem;">You do not have administrative permission to access this page.</p>
                        <a href="${getHomeRedirect(data.role)}" style="display: inline-block; background: #2563eb; color: white; padding: 0.75rem 1.5rem; text-decoration: none; border-radius: 6px; font-weight: 600;">Return to Dashboard</a>
                    </div>
                </div>
            `;
            return null;
        }

        updateNavUserInfo(data);
        return data;
    } catch (err) {
        console.warn('Backend servlet unavailable, using session state:', err);
        const savedUser = sessionStorage.getItem('currentUser');
        if (savedUser) {
            try {
                currentUser = JSON.parse(savedUser);
                updateNavUserInfo(currentUser);
                return currentUser;
            } catch (e) {}
        }
        
        // Auto-detect role from path for seamless static preview
        const isPageAdmin = window.location.pathname.includes('/admin/');
        const isPageCommittee = window.location.pathname.includes('/committee/');
        const demoRole = isPageAdmin ? 'CADRE_ADMINISTRATOR' : (isPageCommittee ? 'TRANSFER_COMMITTEE_MEMBER' : 'CIVIL_SERVICE_OFFICER');
        const demoName = isPageAdmin ? 'Admin User' : (isPageCommittee ? 'Committee Member' : 'Officer Rajan');
        const demoUsername = isPageAdmin ? 'admin' : (isPageCommittee ? 'committee1' : 'officer1');

        currentUser = {
            status: 'authenticated',
            user_id: 1,
            username: demoUsername,
            role: demoRole,
            name: demoName,
            officer_id: 101
        };
        updateNavUserInfo(currentUser);
        return currentUser;
    }
}

function getHomeRedirect(role) {
    const isSub = API_BASE.includes('../');
    const prefix = isSub ? '../' : '';
    if (role === 'CADRE_ADMINISTRATOR') return prefix + 'admin/dashboard.html';
    if (role === 'TRANSFER_COMMITTEE_MEMBER') return prefix + 'committee/dashboard.html';
    return prefix + 'officer/dashboard.html';
}

function updateNavUserInfo(user) {
    const nameEl = document.getElementById('nav-user-name');
    const roleEl = document.getElementById('nav-user-role');
    if (nameEl) nameEl.textContent = user.name || user.username;
    if (roleEl) roleEl.textContent = formatRole(user.role);
}

function formatRole(role) {
    if (role === 'CIVIL_SERVICE_OFFICER') return 'Civil Services Officer';
    if (role === 'CADRE_ADMINISTRATOR') return 'Cadre Administrator';
    if (role === 'TRANSFER_COMMITTEE_MEMBER') return 'Committee Member';
    return role;
}

// Global Logout Action
async function handleLogout() {
    try {
        const response = await fetch(API_BASE + '../logout', { method: 'POST' });
        window.location.href = (API_BASE.includes('../') ? '../' : '') + 'login.html';
    } catch (err) {
        window.location.href = (API_BASE.includes('../') ? '../' : '') + 'login.html';
    }
}

// Status Badges
function renderStatusBadge(status) {
    if (!status) return '';
    const cleanStatus = status.toLowerCase();
    return `<span class="badge badge-${cleanStatus}">${status.replace(/_/g, ' ')}</span>`;
}

function renderPriorityBadge(priority) {
    if (!priority) return '';
    const p = priority.toLowerCase();
    return `<span class="badge badge-${p}">${priority} PRIORITY</span>`;
}

// Modal Dialog Helpers
function openModal(modalId) {
    const modal = document.getElementById(modalId);
    if (modal) modal.classList.add('active');
}

function closeModal(modalId) {
    const modal = document.getElementById(modalId);
    if (modal) modal.classList.remove('active');
}

// Alert Banner Helper
function showAlert(containerId, message, type = 'success') {
    const container = document.getElementById(containerId);
    if (!container) return;
    container.innerHTML = `
        <div class="alert alert-${type}">
            <span>${message}</span>
        </div>
    `;
    setTimeout(() => {
        container.innerHTML = '';
    }, 5000);
}

// CSV Export Utility
function downloadCSV(endpoint, filename = 'report.csv') {
    window.location.href = API_BASE + endpoint + (endpoint.includes('?') ? '&' : '?') + 'export=csv';
}
