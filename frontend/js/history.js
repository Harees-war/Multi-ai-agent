// history.js
document.addEventListener('DOMContentLoaded', () => {
    const historyTableBody = document.getElementById('history-table-body');
    const searchInput = document.getElementById('search-input');
    const prevPageBtn = document.getElementById('prev-page-btn');
    const nextPageBtn = document.getElementById('next-page-btn');
    const pageInfo = document.getElementById('page-info');

    let allHistory = [];
    let filteredHistory = [];
    let currentPage = 1;
    const itemsPerPage = 8;

    async function loadHistory() {
        try {
            utils.showLoader();
            allHistory = await api.get('/history');
            applyFiltersAndRender();
        } catch (err) {
            console.error(err);
            utils.showToast(err.message || 'Failed to fetch history logs', 'error');
        } finally {
            utils.hideLoader();
        }
    }

    function applyFiltersAndRender() {
        const query = searchInput.value.trim().toLowerCase();
        
        if (query) {
            filteredHistory = allHistory.filter(item => {
                return item.language.toLowerCase().includes(query) ||
                       item.agent.toLowerCase().includes(query) ||
                       item.prompt.toLowerCase().includes(query);
            });
        } else {
            filteredHistory = [...allHistory];
        }

        currentPage = 1;
        renderTable();
    }

    function renderTable() {
        if (!historyTableBody) return;
        historyTableBody.innerHTML = '';
        
        if (filteredHistory.length === 0) {
            historyTableBody.innerHTML = `<tr><td colspan="6" style="text-align: center; color: var(--text-muted);">No history records found.</td></tr>`;
            pageInfo.textContent = 'Page 1 of 1';
            prevPageBtn.disabled = true;
            nextPageBtn.disabled = true;
            return;
        }

        const startIndex = (currentPage - 1) * itemsPerPage;
        const endIndex = Math.min(startIndex + itemsPerPage, filteredHistory.length);
        const pageItems = filteredHistory.slice(startIndex, endIndex);

        pageItems.forEach((item, index) => {
            const date = new Date(item.createdAt).toLocaleDateString();
            const time = new Date(item.createdAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
            
            const badgeClass = item.agent.toLowerCase();
            const promptPreview = item.prompt.length > 50 ? item.prompt.substring(0, 50) + '...' : item.prompt;

            const tr = document.createElement('tr');
            tr.className = 'fade-in';
            tr.style.animationDelay = `${index * 0.05}s`;

            tr.innerHTML = `
                <td>${date} <span style="font-size: 0.75rem; color: var(--text-muted); display: block;">${time}</span></td>
                <td><span class="badge ${badgeClass}">${item.agent}</span></td>
                <td><span style="font-family: 'JetBrains Mono', monospace; font-size: 0.85rem;">${item.language}</span></td>
                <td style="max-width: 250px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap;">${escapeHtml(promptPreview)}</td>
                <td><span class="status-indicator"><span class="status-dot online"></span> Completed</span></td>
                <td>
                    <button class="action-icon-btn delete-btn" data-id="${item.id}" title="Delete record">
                        <i class="fa-solid fa-trash-can"></i>
                    </button>
                </td>
            `;
            historyTableBody.appendChild(tr);
        });

        const totalPages = Math.ceil(filteredHistory.length / itemsPerPage);
        pageInfo.textContent = `Page ${currentPage} of ${totalPages}`;
        prevPageBtn.disabled = currentPage === 1;
        nextPageBtn.disabled = currentPage === totalPages;

        // Bind delete events
        document.querySelectorAll('.delete-btn').forEach(btn => {
            btn.clickListener = async (e) => {
                const id = e.currentTarget.getAttribute('data-id');
                if (confirm('Are you sure you want to delete this log?')) {
                    await deleteLog(id);
                }
            };
            btn.addEventListener('click', btn.clickListener);
        });
    }

    async function deleteLog(id) {
        try {
            utils.showLoader();
            await api.delete(`/history/${id}`);
            utils.showToast('Log deleted successfully', 'success');
            allHistory = allHistory.filter(item => item.id != id);
            applyFiltersAndRender();
        } catch (err) {
            utils.showToast(err.message || 'Failed to delete record', 'error');
        } finally {
            utils.hideLoader();
        }
    }

    function escapeHtml(text) {
        return text
            .replace(/&/g, "&amp;")
            .replace(/</g, "&lt;")
            .replace(/>/g, "&gt;");
    }

    if (searchInput) {
        searchInput.addEventListener('input', applyFiltersAndRender);
    }

    if (prevPageBtn) {
        prevPageBtn.addEventListener('click', () => {
            if (currentPage > 1) {
                currentPage--;
                renderTable();
            }
        });
    }

    if (nextPageBtn) {
        nextPageBtn.addEventListener('click', () => {
            const totalPages = Math.ceil(filteredHistory.length / itemsPerPage);
            if (currentPage < totalPages) {
                currentPage++;
                renderTable();
            }
        });
    }

    loadHistory();
});
