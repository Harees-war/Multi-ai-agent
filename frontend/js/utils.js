// utils.js
const utils = {
    // Show toast notifications
    showToast(message, type = 'info') {
        let container = document.getElementById('toast-container');
        if (!container) {
            container = document.createElement('div');
            container.id = 'toast-container';
            document.body.appendChild(container);
        }

        const toast = document.createElement('div');
        toast.className = `toast ${type}`;
        
        // Add message text
        toast.textContent = message;
        container.appendChild(toast);

        // Remove toast after 3.5 seconds
        setTimeout(() => {
            toast.style.opacity = '0';
            toast.style.transition = 'opacity 0.3s ease';
            setTimeout(() => {
                toast.remove();
            }, 300);
        }, 3500);
    },

    // Copy text to clipboard
    async copyToClipboard(text) {
        try {
            await navigator.clipboard.writeText(text);
            this.showToast('Copied to clipboard!', 'success');
        } catch (err) {
            console.error('Failed to copy text: ', err);
            this.showToast('Failed to copy to clipboard', 'error');
        }
    },

    // Show loading spinner
    showLoader(elementId = 'loader-overlay') {
        let loader = document.getElementById(elementId);
        if (!loader) {
            loader = document.createElement('div');
            loader.id = elementId;
            loader.style.position = 'fixed';
            loader.style.top = '0';
            loader.style.left = '0';
            loader.style.width = '100vw';
            loader.style.height = '100vh';
            loader.style.background = 'rgba(0, 0, 0, 0.7)';
            loader.style.display = 'flex';
            loader.style.justifyContent = 'center';
            loader.style.alignItems = 'center';
            loader.style.zIndex = '99999';
            
            const spinner = document.createElement('div');
            spinner.className = 'spinner';
            
            loader.appendChild(spinner);
            document.body.appendChild(loader);
        }
        loader.style.display = 'flex';
    },

    // Hide loading spinner
    hideLoader(elementId = 'loader-overlay') {
        const loader = document.getElementById(elementId);
        if (loader) {
            loader.style.display = 'none';
        }
    },

    // Trigger file download
    downloadFile(filename, content, mimeType = 'text/plain') {
        const blob = new Blob([content], { type: mimeType });
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = filename;
        document.body.appendChild(a);
        a.click();
        document.body.removeChild(a);
        URL.revokeObjectURL(url);
        this.showToast(`Downloaded ${filename} successfully!`, 'success');
    }
};
