<script setup lang="ts">
import { ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()

const email = ref('admin@everest-sushi.no')
const password = ref('Test1234!')
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

    // redirect to dashboard
    const redirect = route.query.redirect as string
    router.push(redirect || { name: 'Dashboard' })
  } catch {
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
  padding: 24px;
}

.login-container {
  width: 100%;
  max-width: 400px;
}

/* Form */
.login-form {
  background: var(--color-card);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  padding: 32px;
  box-shadow: var(--shadow-md);
}

.login-form h2 {
  font-size: var(--font-size-xl);
  font-weight: var(--font-weight-bold);
  color: var(--color-foreground);
  margin-bottom: 24px;
  text-align: center;
}

/* Error Message */
.error-message {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 16px;
  margin-bottom: 20px;
  background-color: var(--color-danger-bg);
  color: var(--color-danger-fg);
  border-radius: var(--radius-md);
  font-size: var(--font-size-sm);
}

/* Form Group */
.form-group {
  margin-bottom: 20px;
}

.form-group label {
  display: block;
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-medium);
  color: var(--color-foreground);
  margin-bottom: 6px;
}

.form-group input {
  width: 100%;
  padding: 10px 12px;
  font-size: var(--font-size-base);
  color: var(--color-foreground);
  background: var(--color-gray-50);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  transition: all var(--transition-fast);
}

.form-group input:focus {
  outline: none;
  border-color: var(--color-focus);
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.1);
  background: var(--color-card);
}

.form-group input::placeholder {
  color: var(--color-gray-400);
}

/* Login Button */
.login-btn {
  width: 100%;
  padding: 12px 20px;
  margin-top: 8px;
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
  transform: translateY(-1px);
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
@media (max-width: 480px) {
  .login-form {
    padding: 24px;
  }

  .mock-users-info {
    padding: 16px;
  }
}
</style>
