// theme.js
const theme = {
    init() {
        const activeTheme = localStorage.getItem('theme') || 'dark';
        this.setTheme(activeTheme);
    },

    setTheme(mode) {
        if (mode === 'light') {
            document.documentElement.classList.add('light-theme');
            document.documentElement.classList.remove('dark-theme');
            localStorage.setItem('theme', 'light');
        } else {
            document.documentElement.classList.add('dark-theme');
            document.documentElement.classList.remove('light-theme');
            localStorage.setItem('theme', 'dark');
        }
    },

    toggle() {
        const current = localStorage.getItem('theme') || 'dark';
        const next = current === 'dark' ? 'light' : 'dark';
        this.setTheme(next);
        return next;
    }
};

// Initialize theme
theme.init();
