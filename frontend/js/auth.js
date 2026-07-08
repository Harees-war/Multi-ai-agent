// auth.js
const auth = {
    // Check if token exists in LocalStorage
    isAuthenticated() {
        return !!localStorage.getItem('token');
    },

    // Retrieve user session data
    getUser() {
        const userStr = localStorage.getItem('user');
        return userStr ? JSON.parse(userStr) : null;
    },

    // Redirect to login if user not authenticated
    checkSession() {
        const path = window.location.pathname;
        const isAuthPage = path.endsWith('login.html') || 
                           path.endsWith('signup.html') || 
                           path.endsWith('index.html') || 
                           path === '/' || 
                           path === '';

        if (!this.isAuthenticated() && !isAuthPage) {
            window.location.href = 'login.html';
        } else if (this.isAuthenticated() && isAuthPage) {
            // Allow them to look at index.html but not login/signup
            if (path.endsWith('login.html') || path.endsWith('signup.html')) {
                window.location.href = 'dashboard.html';
            }
        }
    },

    // Log the user out of the platform
    logout() {
        localStorage.removeItem('token');
        localStorage.removeItem('user');
        window.location.href = 'login.html';
    }
};

// Run automatically on load
auth.checkSession();

// Load custom profile avatar if stored locally
document.addEventListener('DOMContentLoaded', () => {
    const savedAvatar = localStorage.getItem('user_avatar');
    if (savedAvatar) {
        const avatars = document.querySelectorAll('.user-avatar, .profile-avatar');
        avatars.forEach(img => {
            img.src = savedAvatar;
        });
    }
});
