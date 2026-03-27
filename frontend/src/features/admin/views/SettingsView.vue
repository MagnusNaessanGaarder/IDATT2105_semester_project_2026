<script setup lang="ts">
import { ref } from 'vue'
import adminData from '@/data/admin.json'

interface SettingItem {
  id: string
  label: string
  description: string
  type: 'select' | 'toggle' | 'number' | 'info'
  current_value: unknown
  options?: string[]
  min?: number
  max?: number
}

interface SettingSection {
  section_title: string
  items: SettingItem[]
}

const systemSettings = ref(adminData.settings.system as SettingSection)
const notificationSettings = ref(adminData.settings.notification_preferences as SettingSection)
const securitySettings = ref(adminData.settings.security as SettingSection)
const auditLogSample = adminData.audit_log_sample
</script>

<template>
  <div class="view-page">
    <header class="page-header">
      <h1>Innstillinger</h1>
      <p class="subtitle">Systeminnstillinger og konfigurasjon</p>
    </header>

    <div class="settings-sections">
      <section class="settings-section">
        <h2 class="settings-title">{{ systemSettings.section_title }}</h2>
        <div class="settings-items">
          <div v-for="item in systemSettings.items" :key="item.id" class="setting-item">
            <div class="setting-header">
              <label class="setting-label">{{ item.label }}</label>
              <p class="setting-description">{{ item.description }}</p>
            </div>
            <div v-if="item.type === 'select'" class="setting-control">
              <select :value="item.current_value as string" class="setting-select">
                <option v-for="opt in item.options" :key="opt">{{ opt }}</option>
              </select>
            </div>
            <div v-else-if="item.type === 'toggle'" class="setting-control">
              <input type="checkbox" :checked="Boolean(item.current_value)" class="setting-toggle">
            </div>
            <div v-else-if="item.type === 'number'" class="setting-control">
              <input
                type="number"
                :value="Number(item.current_value)"
                :min="item.min"
                :max="item.max"
                class="setting-input"
              >
            </div>
            <div v-else class="setting-info">{{ item.current_value }}</div>
          </div>
        </div>
      </section>

      <section class="settings-section">
        <h2 class="settings-title">{{ notificationSettings.section_title }}</h2>
        <div class="settings-items">
          <div v-for="item in notificationSettings.items" :key="item.id" class="setting-item">
            <div class="setting-header">
              <label class="setting-label">{{ item.label }}</label>
              <p class="setting-description">{{ item.description }}</p>
            </div>
            <div class="setting-control">
              <input type="checkbox" :checked="Boolean(item.current_value)" class="setting-toggle">
            </div>
          </div>
        </div>
      </section>

      <section class="settings-section">
        <h2 class="settings-title">{{ securitySettings.section_title }}</h2>
        <div class="settings-items">
          <div v-for="item in securitySettings.items" :key="item.id" class="setting-item">
            <div class="setting-header">
              <label class="setting-label">{{ item.label }}</label>
              <p class="setting-description">{{ item.description }}</p>
            </div>
            <div v-if="item.type === 'toggle'" class="setting-control">
              <input type="checkbox" :checked="Boolean(item.current_value)" class="setting-toggle">
            </div>
            <div v-else-if="item.type === 'number'" class="setting-control">
              <input
                type="number"
                :value="Number(item.current_value)"
                :min="item.min"
                :max="item.max"
                class="setting-input"
              >
            </div>
          </div>
        </div>
      </section>

      <section class="settings-section">
        <h2 class="settings-title">Siste hendelser (audit log)</h2>
        <div class="audit-log">
          <article v-for="entry in auditLogSample" :key="entry.id" class="audit-log__item">
            <p class="audit-log__title">{{ entry.action }} - {{ entry.resource }}</p>
            <p class="audit-log__meta">{{ entry.timestamp }} - {{ entry.user }} - {{ entry.result }}</p>
            <p class="audit-log__details">{{ entry.details }}</p>
          </article>
        </div>
      </section>
    </div>

    <div class="settings-footer">
      <button class="btn btn--primary">Lagre endringer</button>
      <button class="btn btn--secondary">Avbryt</button>
    </div>
  </div>
</template>

<style scoped>
.view-page {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 1rem;
}

.page-header {
  margin-bottom: 2rem;
}

.page-header h1 {
  margin: 0;
  font-size: var(--text-2xl);
  font-weight: 700;
  color: var(--color-foreground);
  margin-bottom: 0.5rem;
}

.subtitle {
  margin: 0;
  font-size: var(--text-base);
  color: var(--color-gray-600);
}

.settings-sections {
  display: grid;
  gap: 2rem;
  margin-bottom: 2rem;
}

.settings-section {
  background: var(--color-card);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  padding: 1.5rem;
}

.settings-title {
  margin: 0 0 1rem;
  font-size: var(--text-lg);
  font-weight: 600;
  color: var(--color-foreground);
  border-bottom: 1px solid var(--color-border);
  padding-bottom: 1rem;
}

.settings-items {
  display: grid;
  gap: 1.5rem;
}

.setting-item {
  display: grid;
  grid-template-columns: 1fr auto;
  gap: 1rem;
  align-items: center;
  padding: 1rem 0;
  border-bottom: 1px solid var(--color-border);
}

.setting-item:last-child {
  border-bottom: none;
  padding-bottom: 0;
}

.setting-header {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}

.setting-label {
  font-size: var(--text-base);
  font-weight: 600;
  color: var(--color-foreground);
}

.setting-description {
  margin: 0;
  font-size: var(--text-sm);
  color: var(--color-gray-600);
}

.setting-control {
  display: flex;
  align-items: center;
}

.setting-select,
.setting-input {
  padding: 0.5rem 0.75rem;
  background: var(--color-accent);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  font-size: var(--text-sm);
  color: var(--color-foreground);
}

.setting-select:focus,
.setting-input:focus {
  outline: none;
  border-color: var(--color-foreground);
}

.setting-toggle {
  width: 1.5rem;
  height: 1.5rem;
  cursor: pointer;
}

.setting-info {
  font-size: var(--text-sm);
  color: var(--color-gray-600);
  padding: 0.5rem 0.75rem;
  background: var(--color-accent);
  border-radius: var(--radius-sm);
}

.audit-log {
  display: grid;
  gap: 0.75rem;
}

.audit-log__item {
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  background: var(--color-accent);
  padding: 0.75rem;
}

.audit-log__title {
  margin: 0;
  font-size: var(--text-sm);
  font-weight: 600;
  color: var(--color-foreground);
}

.audit-log__meta,
.audit-log__details {
  margin: 0.25rem 0 0;
  font-size: var(--text-xs);
  color: var(--color-gray-600);
}

.settings-footer {
  display: flex;
  gap: 1rem;
  justify-content: flex-end;
}

.btn {
  padding: 0.75rem 1.5rem;
  border: none;
  border-radius: var(--radius-md);
  font-size: var(--text-sm);
  font-weight: 600;
  cursor: pointer;
  transition: all var(--transition-base);
}

.btn--primary {
  background: var(--color-foreground);
  color: var(--color-background);
}

.btn--primary:hover {
  background: var(--color-gray-900);
}

.btn--secondary {
  background: var(--color-card);
  color: var(--color-foreground);
  border: 1px solid var(--color-border);
}

.btn--secondary:hover {
  background: var(--color-accent);
}

@media (max-width: 48rem) {
  .setting-item {
    grid-template-columns: 1fr;
  }

  .settings-footer {
    flex-direction: column;
  }
}
</style>
