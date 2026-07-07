// settings.js
document.addEventListener('DOMContentLoaded', () => {
    const themeToggle = document.getElementById('theme-toggle-switch');
    const apiStatusDot = document.getElementById('api-status-dot');
    const apiStatusText = document.getElementById('api-status-text');

    const currentTheme = localStorage.getItem('theme') || 'dark';
    if (themeToggle) {
        themeToggle.checked = (currentTheme === 'light');
        themeToggle.addEventListener('change', () => {
            const nextMode = theme.toggle();
            themeToggle.checked = (nextMode === 'light');
            utils.showToast(`Switched to ${nextMode} theme`, 'success');
        });
    }

    async function checkApiConnection() {
        if (!apiStatusDot || !apiStatusText) return;
        try {
            await api.get('/user/profile');
            apiStatusDot.className = 'status-dot online';
            apiStatusText.textContent = 'Connected (Active)';
        } catch (err) {
            apiStatusDot.className = 'status-dot offline';
            apiStatusText.textContent = 'Offline / Connection Error';
        }
    }

    checkApiConnection();
});
