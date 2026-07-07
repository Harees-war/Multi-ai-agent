// review.js
document.addEventListener('DOMContentLoaded', () => {
    const codeTextArea = document.getElementById('code-input-text');
    const charCounter = document.getElementById('char-count');
    const languageSelect = document.getElementById('language-select');
    const reviewBtn = document.getElementById('review-btn');
    const reportContainer = document.getElementById('report-container');
    const copyBtn = document.getElementById('copy-report-btn');
    const downloadBtn = document.getElementById('download-report-btn');
    const clearBtn = document.getElementById('clear-btn');

    let fullRawReport = '';

    if (codeTextArea) {
        codeTextArea.addEventListener('input', () => {
            charCounter.textContent = codeTextArea.value.length;
        });
    }

    // Convert markdown review back to stylized HTML structures
    function parseReport(markdownText) {
        fullRawReport = markdownText;
        const sections = markdownText.split(/###\s+/);
        reportContainer.innerHTML = '';

        sections.forEach(sec => {
            const lines = sec.trim().split('\n');
            const title = lines[0].trim();
            if (!title) return;

            const contentLines = lines.slice(1);
            let contentHtml = '';
            let inCodeBlock = false;
            let codeContent = '';

            contentLines.forEach(line => {
                if (line.startsWith('```')) {
                    if (inCodeBlock) {
                        contentHtml += `<pre class="code-terminal" style="margin-top: 10px; margin-bottom: 10px;"><code style="color: #a9b1d6;">${escapeHtml(codeContent)}</code></pre>`;
                        codeContent = '';
                        inCodeBlock = false;
                    } else {
                        inCodeBlock = true;
                    }
                } else if (inCodeBlock) {
                    codeContent += line + '\n';
                } else if (line.trim().startsWith('-') || line.trim().startsWith('*')) {
                    contentHtml += `<li>${line.replace(/^[-*]\s+/, '')}</li>`;
                } else if (line.trim() !== '') {
                    contentHtml += `<p style="margin-bottom: 8px;">${line}</p>`;
                }
            });

            // Wrap bullet lists
            contentHtml = contentHtml.replace(/(<li>.*?<\/li>)/g, '<ul>$1<\/ul>');
            contentHtml = contentHtml.replace(/<\/ul><ul>/g, '');

            const classMap = {
                'bug detection': 'bug',
                'security issues': 'security',
                'performance suggestions': 'perf',
                'best practices & code smells': 'best-practice',
                'optimized code': 'opt-code'
            };

            const lowercaseTitle = title.toLowerCase();
            let matchedClass = 'best-practice';
            for (let key in classMap) {
                if (lowercaseTitle.includes(key)) {
                    matchedClass = classMap[key];
                    break;
                }
            }

            const secEl = document.createElement('div');
            secEl.className = 'report-section';
            secEl.innerHTML = `
                <div class="report-section-header ${matchedClass}">
                    <i class="fa-solid fa-circle-info"></i> ${title}
                </div>
                <div class="report-body">
                    ${contentHtml}
                </div>
            `;
            reportContainer.appendChild(secEl);
        });
    }

    function escapeHtml(text) {
        return text
            .replace(/&/g, "&amp;")
            .replace(/</g, "&lt;")
            .replace(/>/g, "&gt;")
            .replace(/"/g, "&quot;")
            .replace(/'/g, "&#039;");
    }

    if (reviewBtn) {
        reviewBtn.addEventListener('click', async () => {
            const code = codeTextArea.value.trim();
            const language = languageSelect.value;

            if (!code) {
                utils.showToast('Please paste your source code first', 'error');
                return;
            }

            try {
                utils.showLoader();
                reportContainer.innerHTML = '<div style="color: var(--text-muted);">// Reviewing code... Please wait...</div>';

                const response = await api.post('/review', {
                    language,
                    prompt: code
                });

                parseReport(response.response);
                utils.showToast('Code review completed!', 'success');
            } catch (err) {
                reportContainer.innerHTML = `<div style="color: #ff4757;">Error completing review: ${err.message}</div>`;
                utils.showToast(err.message || 'Error occurred during review', 'error');
            } finally {
                utils.hideLoader();
            }
        });
    }

    if (copyBtn) {
        copyBtn.addEventListener('click', () => {
            if (!fullRawReport) {
                utils.showToast('No report available to copy', 'error');
                return;
            }
            utils.copyToClipboard(fullRawReport);
        });
    }

    if (downloadBtn) {
        downloadBtn.addEventListener('click', () => {
            if (!fullRawReport) {
                utils.showToast('No report available to download', 'error');
                return;
            }
            utils.downloadFile('code_review_report.md', fullRawReport);
        });
    }

    if (clearBtn) {
        clearBtn.addEventListener('click', () => {
            codeTextArea.value = '';
            charCounter.textContent = '0';
            reportContainer.innerHTML = '<div style="color: var(--text-muted);">// Paste code and click Review Code to view report insights...</div>';
            fullRawReport = '';
            utils.showToast('Inputs cleared', 'info');
        });
    }
});
