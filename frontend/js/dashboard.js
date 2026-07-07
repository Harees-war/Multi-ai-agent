// dashboard.js
document.addEventListener('DOMContentLoaded', async () => {
    const user = auth.getUser();
    if (user) {
        document.querySelectorAll('.user-name-display').forEach(el => el.textContent = user.name);
    }

    try {
        utils.showLoader();
        // Load statistics
        const stats = await api.get('/user/profile');
        
        document.getElementById('total-requests').textContent = stats.totalRequests;
        document.getElementById('ai-usage').textContent = stats.codeGenerations;
        document.getElementById('total-reviews').textContent = stats.codeReviews;
        document.getElementById('total-explanations').textContent = stats.codeExplanations;

        // Render chart bars dynamically
        const genBar = document.getElementById('bar-generate');
        const revBar = document.getElementById('bar-review');
        const expBar = document.getElementById('bar-explain');

        if (genBar && revBar && expBar) {
            const total = stats.totalRequests || 1;
            genBar.style.height = `${Math.max(10, (stats.codeGenerations / total) * 100)}%`;
            revBar.style.height = `${Math.max(10, (stats.codeReviews / total) * 100)}%`;
            expBar.style.height = `${Math.max(10, (stats.codeExplanations / total) * 100)}%`;
        }

        // Load recent activity history
        const historyList = await api.get('/history');
        const activityListEl = document.getElementById('activity-list');
        if (activityListEl) {
            activityListEl.innerHTML = '';

            if (historyList && historyList.length > 0) {
                const recent = historyList.slice(0, 5);
                recent.forEach(item => {
                    const activityItem = document.createElement('div');
                    activityItem.className = 'activity-item';
                    
                    let iconClass = 'fa-code';
                    if (item.agent === 'Reviewer') iconClass = 'fa-shield-halved';
                    if (item.agent === 'Explainer') iconClass = 'fa-book';

                    const date = new Date(item.createdAt).toLocaleDateString();

                    activityItem.innerHTML = `
                        <div class="activity-avatar"><i class="fa-solid ${iconClass}"></i></div>
                        <div class="activity-details">
                            <div class="activity-text">Ran <strong>${item.agent}</strong> agent on language <strong>${item.language}</strong></div>
                            <div class="activity-time">${date}</div>
                        </div>
                    `;
                    activityListEl.appendChild(activityItem);
                });
            } else {
                activityListEl.innerHTML = '<div class="activity-item">No recent requests logged. Start generating!</div>';
            }
        }

    } catch (err) {
        console.error(err);
        utils.showToast(err.message || 'Failed to load dashboard metrics', 'error');
    } finally {
        utils.hideLoader();
    }
});
