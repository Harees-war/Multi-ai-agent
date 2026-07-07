// generate.js
document.addEventListener('DOMContentLoaded', () => {
    const requirementTextArea = document.getElementById('requirement-text');
    const charCounter = document.getElementById('char-count');
    const languageSelect = document.getElementById('language-select');
    const generateBtn = document.getElementById('generate-btn');
    const codeTerminal = document.getElementById('code-terminal-pre');
    const copyBtn = document.getElementById('copy-code-btn');
    const downloadBtn = document.getElementById('download-code-btn');
    const clearBtn = document.getElementById('clear-btn');

    if (requirementTextArea) {
        requirementTextArea.addEventListener('input', () => {
            const count = requirementTextArea.value.length;
            charCounter.textContent = count;
        });
    }

    if (generateBtn) {
        generateBtn.addEventListener('click', async () => {
            const requirement = requirementTextArea.value.trim();
            const language = languageSelect.value;

            if (!requirement) {
                utils.showToast('Please describe your requirements first', 'error');
                return;
            }

            try {
                utils.showLoader();
                codeTerminal.textContent = '// Generating code from requirement... Please wait...';
                
                const response = await api.post('/generate', {
                    language,
                    prompt: requirement
                });

                codeTerminal.textContent = response.response;
                utils.showToast('Code generated successfully!', 'success');
            } catch (err) {
                codeTerminal.textContent = `// Error generating code:\n${err.message}`;
                utils.showToast(err.message || 'Error occurred during generation', 'error');
            } finally {
                utils.hideLoader();
            }
        });
    }

    if (copyBtn) {
        copyBtn.addEventListener('click', () => {
            const code = codeTerminal.textContent;
            if (!code || code.startsWith('//')) {
                utils.showToast('No code available to copy', 'error');
                return;
            }
            utils.copyToClipboard(code);
        });
    }

    if (downloadBtn) {
        downloadBtn.addEventListener('click', () => {
            const code = codeTerminal.textContent;
            if (!code || code.startsWith('//')) {
                utils.showToast('No code available to download', 'error');
                return;
            }

            const lang = languageSelect.value.toLowerCase();
            let extension = 'txt';
            if (lang === 'java') extension = 'java';
            else if (lang === 'python') extension = 'py';
            else if (lang === 'javascript') extension = 'js';
            else if (lang === 'cpp') extension = 'cpp';
            else if (lang === 'html') extension = 'html';
            else if (lang === 'css') extension = 'css';
            else if (lang === 'sql') extension = 'sql';

            utils.downloadFile(`generated_code.${extension}`, code);
        });
    }

    if (clearBtn) {
        clearBtn.addEventListener('click', () => {
            requirementTextArea.value = '';
            charCounter.textContent = '0';
            codeTerminal.textContent = '// Output terminal ready. Input a requirement...';
            utils.showToast('Inputs cleared', 'info');
        });
    }
});
