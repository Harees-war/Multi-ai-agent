// validation.js
const validation = {
    validateEmail(email) {
        const re = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
        return re.test(email);
    },

    calculatePasswordStrength(password) {
        if (!password || password.length < 6) {
            return { score: 25, label: 'Weak (Must be 6+ chars)', color: '#ff4757' };
        }
        let score = 1;
        if (/[A-Z]/.test(password)) score++;
        if (/[0-9]/.test(password)) score++;
        if (/[^A-Za-z0-9]/.test(password)) score++;

        switch (score) {
            case 1:
                return { score: 25, label: 'Weak', color: '#ff4757' };
            case 2:
                return { score: 50, label: 'Fair', color: '#ffa502' };
            case 3:
                return { score: 75, label: 'Good', color: '#1e90ff' };
            case 4:
                return { score: 100, label: 'Strong', color: '#2ed573' };
            default:
                return { score: 25, label: 'Weak', color: '#ff4757' };
        }
    }
};
