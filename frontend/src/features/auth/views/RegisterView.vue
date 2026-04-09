<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const authStore = useAuthStore()

const fullName = ref('')
const email = ref('')
const phone = ref('')
const password = ref('')
const passwordConfirm = ref('')
const error = ref('')
const isLoading = ref(false)

const passwordStrength = computed(() => {
  const p = password.value
  if (!p) return null
  const checks = [
    p.length >= 8,
    /[0-9]/.test(p),
    /[a-z]/.test(p),
    /[A-Z]/.test(p),
    /[@#$%^&+=!]/.test(p),
  ]
  const passed = checks.filter(Boolean).length
  if (passed <= 2) return 'weak'
  if (passed <= 4) return 'fair'
  return 'strong'
})

const passwordStrengthLabel: Record<string, string> = {
  weak: 'Svakt',
  fair: 'Middels',
  strong: 'Sterkt',
}

function validate(): string | null {
  if (!fullName.value.trim() || fullName.value.trim().length < 2) {
    return 'Fullt navn må være minst 2 tegn'
  }
  if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email.value)) {
    return 'Ugyldig e-postadresse'
  }
  const pwPattern = /^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!]).{8,}$/
  if (!pwPattern.test(password.value)) {
    return 'Passordet må ha minst 8 tegn, én stor og liten bokstav, ett tall og ett spesialtegn (@#$%^&+=!)'
  }
  if (password.value !== passwordConfirm.value) {
    return 'Passordene stemmer ikke overens'
  }
  return null
}

const handleRegister = async () => {
  error.value = ''
  const validationError = validate()
  if (validationError) {
    error.value = validationError
    return
  }

  isLoading.value = true
  try {
    await authStore.register({
      fullName: fullName.value.trim(),
      email: email.value.trim(),
      phone: phone.value.trim() || undefined,
      password: password.value,
    })
    router.push({ name: 'Dashboard' })
  } catch {
    error.value = authStore.error?.message ?? 'Registrering feilet. E-posten kan allerede være i bruk.'
  } finally {
    isLoading.value = false
  }
}
</script>

<template>
  <div class="register-page">
    <div
        class="register-container"
        v-motion
        :initial="{ opacity: 0, y: 10 }"
        :enter="{ opacity: 1, y: 0, transition: { duration: 300 } }"
    >
      <form class="register-form" @submit.prevent="handleRegister">
        <p class="register-kicker">Internkontroll</p>
        <h2>Opprett konto</h2>
        <p class="register-subtitle">
          Etter registrering vil din leder legge deg til i organisasjonen.
        </p>

        <div v-if="error" class="error-message" role="alert">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <circle cx="12" cy="12" r="10" />
            <line x1="12" y1="8" x2="12" y2="12" />
            <line x1="12" y1="16" x2="12.01" y2="16" />
          </svg>
          {{ error }}
        </div>

        <div class="form-group">
          <label for="fullName">Fullt navn <span class="required">*</span></label>
          <input
              id="fullName"
              v-model="fullName"
              type="text"
              required
              autocomplete="name"
              placeholder="Ola Nordmann"
          />
        </div>

        <div class="form-group">
          <label for="email">E-post <span class="required">*</span></label>
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
          <label for="phone">Telefon <span class="optional">(valgfritt)</span></label>
          <input
              id="phone"
              v-model="phone"
              type="tel"
              autocomplete="tel"
              placeholder="+47 000 00 000"
          />
        </div>

        <div class="form-group">
          <label for="password">Passord <span class="required">*</span></label>
          <input
              id="password"
              v-model="password"
              type="password"
              required
              autocomplete="new-password"
              placeholder="Minst 8 tegn"
          />
          <div v-if="password" class="password-strength">
            <div class="password-strength__bar">
              <div
                  class="password-strength__fill"
                  :class="`password-strength__fill--${passwordStrength}`"
              />
            </div>
            <span class="password-strength__label" :class="`password-strength__label--${passwordStrength}`">
              {{ passwordStrengthLabel[passwordStrength!] }}
            </span>
          </div>
          <p class="field-hint">Må inneholde stor og liten bokstav, tall og spesialtegn (@#$%^&+=!)</p>
        </div>

        <div class="form-group">
          <label for="passwordConfirm">Bekreft passord <span class="required">*</span></label>
          <input
              id="passwordConfirm"
              v-model="passwordConfirm"
              type="password"
              required
              autocomplete="new-password"
              placeholder="Gjenta passord"
              :class="{ 'input--mismatch': passwordConfirm && password !== passwordConfirm }"
          />
          <p v-if="passwordConfirm && password !== passwordConfirm" class="field-error">
            Passordene stemmer ikke overens
          </p>
        </div>

        <button type="submit" class="register-btn" :disabled="isLoading">
          <span v-if="isLoading">Oppretter konto…</span>
          <span v-else>Opprett konto</span>
        </button>

        <p class="login-link">
          Har du allerede en konto?
          <router-link :to="{ name: 'Login' }">Logg inn</router-link>
        </p>
      </form>
    </div>
  </div>
</template>

<style scoped>
.register-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background:
      radial-gradient(circle at 8% 14%, var(--color-surface-tint), transparent 32%),
      radial-gradient(circle at 90% 88%, var(--color-surface-tint-strong), transparent 35%),
      linear-gradient(180deg, var(--color-card-muted) 0%, var(--color-background) 100%);
  padding: var(--content-padding);
}

.register-container {
  width: 100%;
  max-width: 28rem;
}

.register-form {
  background: linear-gradient(180deg, var(--color-card) 0%, var(--color-card-muted) 100%);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-xl);
  padding: clamp(1.75rem, 4vw, 2.25rem);
  box-shadow: var(--shadow-md);
}

.register-kicker {
  margin-bottom: 0.375rem;
  font-size: var(--font-size-xs);
  letter-spacing: 0.12em;
  text-transform: uppercase;
  color: var(--color-gray-600);
  font-weight: var(--font-weight-semibold);
}

.register-form h2 {
  font-family: var(--font-family-display);
  font-size: clamp(1.6rem, 2vw, var(--font-size-2xl));
  font-weight: 700;
  letter-spacing: -0.01em;
  line-height: var(--line-height-heading);
  color: var(--color-foreground);
  margin-bottom: 0.5rem;
}

.register-subtitle {
  margin-bottom: 1.5rem;
  color: var(--color-gray-600);
  font-size: var(--font-size-sm);
  line-height: var(--line-height-relaxed);
}

.error-message {
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
  padding: 0.75rem 1rem;
  margin-bottom: var(--spacing-md);
  background-color: var(--color-danger-bg);
  color: var(--color-danger-fg);
  border-radius: var(--radius-md);
  border: 1px solid var(--color-danger-border);
  font-size: var(--font-size-sm);
}

.form-group {
  margin-bottom: 1rem;
}

.form-group label {
  display: block;
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-medium);
  color: var(--color-foreground);
  margin-bottom: 6px;
}

.required {
  color: var(--color-danger);
  margin-left: 2px;
}

.optional {
  color: var(--color-gray-400);
  font-weight: var(--font-weight-normal);
  font-size: var(--font-size-xs);
  margin-left: 2px;
}

.form-group input {
  width: 100%;
  padding: var(--input-padding);
  font-size: var(--font-size-base);
  color: var(--color-foreground);
  background: var(--color-card);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  min-height: var(--touch-target);
  box-sizing: border-box;
  transition: border-color var(--transition-fast), box-shadow var(--transition-fast);
}

.form-group input:focus {
  border-color: var(--color-focus);
  box-shadow: var(--shadow-focus);
  outline: none;
}

.form-group input::placeholder {
  color: var(--color-gray-400);
}

.input--mismatch {
  border-color: var(--color-danger) !important;
}

.password-strength {
  display: flex;
  align-items: center;
  gap: 0.6rem;
  margin-top: 0.4rem;
}

.password-strength__bar {
  flex: 1;
  height: 4px;
  background: var(--color-gray-200);
  border-radius: 999px;
  overflow: hidden;
}

.password-strength__fill {
  height: 100%;
  border-radius: inherit;
  transition: width var(--transition-fast), background-color var(--transition-fast);
}

.password-strength__fill--weak   { width: 33%; background: var(--color-danger); }
.password-strength__fill--fair   { width: 66%; background: var(--color-warning); }
.password-strength__fill--strong { width: 100%; background: var(--color-success); }

.password-strength__label {
  font-size: var(--font-size-xs);
  font-weight: var(--font-weight-semibold);
  white-space: nowrap;
}

.password-strength__label--weak   { color: var(--color-danger); }
.password-strength__label--fair   { color: var(--color-warning); }
.password-strength__label--strong { color: var(--color-success); }

.field-hint {
  margin: 0.35rem 0 0;
  font-size: var(--font-size-xs);
  color: var(--color-gray-500);
}

.field-error {
  margin: 0.35rem 0 0;
  font-size: var(--font-size-xs);
  color: var(--color-danger);
}

.register-btn {
  width: 100%;
  padding: var(--button-padding-md);
  margin-top: var(--spacing-xs);
  font-size: var(--font-size-base);
  font-weight: var(--font-weight-semibold);
  color: var(--color-primary-foreground);
  background: var(--color-primary);
  border: none;
  border-radius: var(--radius-md);
  cursor: pointer;
  box-shadow: var(--shadow-sm);
  transition: background-color var(--transition-fast), transform var(--transition-fast), box-shadow var(--transition-fast);
}

.register-btn:hover:not(:disabled) {
  background: var(--color-primary-hover);
  transform: translateY(-1px);
  box-shadow: var(--shadow-md);
}

.register-btn:active:not(:disabled) {
  transform: translateY(0);
}

.register-btn:disabled {
  opacity: 0.65;
  cursor: not-allowed;
}

.login-link {
  margin-top: 1.25rem;
  text-align: center;
  font-size: var(--font-size-sm);
  color: var(--color-gray-600);
}

.login-link a {
  color: var(--color-foreground);
  font-weight: var(--font-weight-semibold);
  text-decoration: underline;
}

@media (max-width: 480px) {
  .register-form {
    padding: 1.5rem;
  }
}
</style>