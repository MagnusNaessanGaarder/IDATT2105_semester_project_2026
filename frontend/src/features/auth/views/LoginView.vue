<script setup lang="ts">
import { ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()

const email = ref('Tri@gmail.com')
const password = ref('Tri')
const error = ref('')
const isLoading = ref(false)

const handleLogin = async () => {
  error.value = ''
  isLoading.value = true

  try {
    await authStore.login({
      email: email.value,
      password: password.value,
    })

    // Omdiriger til ønsket side eller dashboard
    const redirect = route.query.redirect as string
    router.push(redirect || { name: 'Dashboard' })
  } catch (e) {
    error.value = 'Ugyldig e-post eller passord'
  } finally {
    isLoading.value = false
  }
}
</script>

<template>
  <div class="login-page">
    <div class="login-container">
      <form class="login-form" @submit.prevent="handleLogin">
        <h2>Logg inn</h2>

        <div v-if="error" class="error-message" role="alert">
          <svg
            width="20"
            height="20"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            stroke-width="2"
          >
            <circle cx="12" cy="12" r="10"></circle>
            <line x1="12" y1="8" x2="12" y2="12"></line>
            <line x1="12" y1="16" x2="12.01" y2="16"></line>
          </svg>
          {{ error }}
        </div>

        <div class="form-group">
          <label for="email">E-post</label>
          <input
            id="email"
            v-model="email"
            type="email"
            required
            autocomplete="email"
            placeholder="din@epost.no"
          />
        </div>

        <div class="form-group">
          <label for="password">Passord</label>
          <input
            id="password"
            v-model="password"
            type="password"
            required
            autocomplete="current-password"
            placeholder="passord"
          />
        </div>

        <button type="submit" class="login-btn" :disabled="isLoading">
          <span v-if="isLoading">Logger inn...</span>
          <span v-else>Logg inn</span>
        </button>
      </form>

      <footer class="login-footer"></footer>
    </div>
  </div>
</template>

<style scoped>
.login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f0f0f0;
  padding: 1.5rem;
}

.login-container {
  width: 100%;
  max-width: 25rem;
}

/* Mock Users Info */
.mock-users-info {
  background: #f0f9ff;
  border: 0.0625rem solid #bae6fd;
  border-radius: var(--radius-md);
  padding: 1.25rem;
  margin-bottom: 1.5rem;
}

.mock-users-info h3 {
  margin: 0 0 0.75rem 0;
  font-size: 0.875rem;
  font-weight: 600;
  color: #0369a1;
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

.mock-user-list {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.mock-user {
  background: white;
  border: 0.0625rem solid #e0f2fe;
  border-radius: var(--radius-sm);
  padding: 0.625rem 0.75rem;
  cursor: pointer;
  transition: all 0.15s ease;
  font-size: 0.8125rem;
  line-height: 1.4;
}

.mock-user:hover {
  background: #f0f9ff;
  border-color: #7dd3fc;
  transform: translateX(0.25rem);
}

.mock-user strong {
  color: #0c4a6e;
}

.mock-user small {
  color: #64748b;
  font-size: 0.6875rem;
}

.mock-hint {
  margin: 0.75rem 0 0 0;
  font-size: 0.6875rem;
  color: #64748b;
  text-align: center;
  font-style: italic;
}

/* Form */
.login-form {
  background: var(--color-card);
  border: 0.0625rem solid var(--color-border);
  border-radius: var(--radius-md);
  padding: 2rem;
  box-shadow: var(--shadow-md);
}

.login-form h2 {
  font-size: var(--font-size-xl);
  font-weight: var(--font-weight-bold);
  color: var(--color-foreground);
  margin-bottom: 1.5rem;
  text-align: center;
}

/* Error Message */
.error-message {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.75rem 1rem;
  margin-bottom: 1.25rem;
  background-color: var(--color-danger-bg);
  color: var(--color-danger-fg);
  border-radius: var(--radius-md);
  font-size: var(--font-size-sm);
}

/* Form Group */
.form-group {
  margin-bottom: 1.25rem;
}

.form-group label {
  display: block;
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-medium);
  color: var(--color-foreground);
  margin-bottom: 0.375rem;
}

.form-group input {
  width: 100%;
  padding: 0.625rem 0.75rem;
  font-size: var(--font-size-base);
  color: var(--color-foreground);
  background: var(--color-gray-50);
  border: 0.0625rem solid var(--color-border);
  border-radius: var(--radius-md);
  transition: all var(--transition-fast);
}

.form-group input:focus {
  outline: none;
  border-color: var(--color-focus);
  box-shadow: 0 0 0 0.1875rem rgba(59, 130, 246, 0.1);
  background: var(--color-card);
}

.form-group input::placeholder {
  color: var(--color-gray-400);
}

/* Login Button */
.login-btn {
  width: 100%;
  padding: 0.75rem 1.25rem;
  margin-top: 0.5rem;
  font-size: var(--font-size-base);
  font-weight: var(--font-weight-semibold);
  color: var(--color-primary-foreground);
  background: var(--color-foreground);
  border: none;
  border-radius: var(--radius-md);
  cursor: pointer;
  transition: all var(--transition-fast);
}

.login-btn:hover:not(:disabled) {
  background: var(--color-gray-800);
  transform: translateY(-0.0625rem);
  box-shadow: var(--shadow-md);
}

.login-btn:active:not(:disabled) {
  transform: translateY(0);
}

.login-btn:disabled {
  opacity: 0.7;
  cursor: not-allowed;
}

/* Responsive */
@media (max-width: 30rem) {
  .login-form {
    padding: 1.5rem;
  }

  .mock-users-info {
    padding: 1rem;
  }
}
</style>
