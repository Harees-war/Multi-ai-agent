// profile.js
document.addEventListener('DOMContentLoaded', async () => {
    const editNameInput = document.getElementById('edit-name');
    const profileEmailInput = document.getElementById('profile-email');
    const joinedDateLabel = document.getElementById('joined-date');
    const joinedDateHeading = document.getElementById('profile-joined-heading');
    const statTotalEl = document.getElementById('stat-total-requests');
    const statGenEl = document.getElementById('stat-code-gens');
    const statRevEl = document.getElementById('stat-code-reviews');
    const statExpEl = document.getElementById('stat-code-explains');
    const profileForm = document.getElementById('profile-form');

    async function loadUserProfile() {
        try {
            utils.showLoader();
            const profile = await api.get('/user/profile');

            if (editNameInput) editNameInput.value = profile.name;
            if (profileEmailInput) profileEmailInput.value = profile.email;
            
            const dateStr = new Date(profile.createdAt).toLocaleDateString(undefined, {
                year: 'numeric',
                month: 'long',
                day: 'numeric'
            });
            if (joinedDateLabel) joinedDateLabel.textContent = dateStr;
            if (joinedDateHeading) {
                joinedDateHeading.textContent = `Joined in ${new Date(profile.createdAt).getFullYear()}`;
            }

            if (statTotalEl) statTotalEl.textContent = profile.totalRequests;
            if (statGenEl) statGenEl.textContent = profile.codeGenerations;
            if (statRevEl) statRevEl.textContent = profile.codeReviews;
            if (statExpEl) statExpEl.textContent = profile.codeExplanations;

            document.querySelectorAll('.user-name-display').forEach(el => el.textContent = profile.name);

        } catch (err) {
            console.error(err);
            utils.showToast(err.message || 'Failed to load user profile metadata', 'error');
        } finally {
            utils.hideLoader();
        }
    }

    if (profileForm) {
        profileForm.addEventListener('submit', async (e) => {
            e.preventDefault();

            const name = editNameInput.value.trim();
            const currentPassword = document.getElementById('current-password').value;
            const newPassword = document.getElementById('new-password').value;
            const confirmNewPassword = document.getElementById('confirm-new-password').value;

            if (!name) {
                utils.showToast('Name is required', 'error');
                return;
            }

            const body = { name };

            if (newPassword || currentPassword || confirmNewPassword) {
                if (!currentPassword) {
                    utils.showToast('Current password is required to change password', 'error');
                    return;
                }
                if (newPassword !== confirmNewPassword) {
                    utils.showToast('New passwords do not match!', 'error');
                    return;
                }
                if (newPassword.length < 6) {
                    utils.showToast('New password must be at least 6 characters long', 'error');
                    return;
                }
                body.currentPassword = currentPassword;
                body.newPassword = newPassword;
            }

            try {
                utils.showLoader();
                const response = await api.put('/user/profile', body);
                
                const user = auth.getUser();
                if (user) {
                    user.name = response.name;
                    localStorage.setItem('user', JSON.stringify(user));
                }

                utils.showToast('Profile updated successfully!', 'success');
                
                document.getElementById('current-password').value = '';
                document.getElementById('new-password').value = '';
                document.getElementById('confirm-new-password').value = '';

                loadUserProfile();
            } catch (err) {
                utils.showToast(err.message || 'Error occurred during profile update', 'error');
            } finally {
                utils.hideLoader();
            }
        });
    }

    // Avatar upload setup
    const avatarEditBtn = document.getElementById('avatarEditBtn');
    const avatarFileInput = document.getElementById('avatarFileInput');

    if (avatarEditBtn && avatarFileInput) {
        avatarEditBtn.addEventListener('click', () => {
            avatarFileInput.click();
        });

        avatarFileInput.addEventListener('change', (e) => {
            const file = e.target.files[0];
            if (file) {
                if (file.size > 2 * 1024 * 1024) { // Limit size to 2MB
                    utils.showToast('Image file size must be less than 2MB', 'error');
                    return;
                }
                const reader = new FileReader();
                reader.onload = (event) => {
                    const base64String = event.target.result;
                    localStorage.setItem('user_avatar', base64String);
                    
                    // Update all avatar images on the page immediately
                    const avatars = document.querySelectorAll('.user-avatar, .profile-avatar');
                    avatars.forEach(img => {
                        img.src = base64String;
                    });
                    
                    utils.showToast('Profile picture updated successfully!', 'success');
                };
                reader.readAsDataURL(file);
            }
        });
    }

    loadUserProfile();
});
