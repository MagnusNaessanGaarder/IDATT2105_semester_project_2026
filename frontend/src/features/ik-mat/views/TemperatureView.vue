<script setup lang="ts">
import BaseModal from '@/shared/components/BaseModal.vue'
import { useTemperatureViewState } from '../composables/useTemperatureViewState'

const {
  isMobile,
  actionError,
  isSubmitting,
  desktopMenuPointId,
  selectedMobileCardId,
  pointModalOpen,
  pointModalMode,
  pointForm,
  measurementModalOpen,
  measurementForm,
  editingMeasurementEntryId,
  canManage,
  alerts,
  okCount,
  instances,
  currentMeasurementPoint,
  selectedLocationLabel,
  employeeOptions,
  isLoading,
  error,
  openAddPointModal,
  openEditPointModal,
  closePointModal,
  savePoint,
  removePoint,
  openMeasurementModal,
  closeMeasurementModal,
  submitMeasurement,
  openDeviationFlow,
  toggleDesktopMenu,
  toggleMobileActions,
} = useTemperatureViewState()
</script>

<template>
  <div class="temperature-page">

    <!-- Page Header -->
    <header class="page-header">
      <h1>Temperaturkontroll</h1>
      <p class="subtitle">Kontinuerlig overvaking av kjøl, frys og varmholding</p>
    </header>

    <!-- Alert Banner -->
    <section v-if="alerts.length > 0" class="alert-banner" role="alert">
      <p><strong>{{ alerts.length }}</strong> Målepunkter har temperaturavvik og trenger oppfølging.</p>
      <router-link :to="{ name: 'Deviations' }">Se avvik</router-link>
    </section>

    <!-- Summary Stats -->
    <section class="summary-grid" aria-label="Temperaturstatus">
      <article class="summary-card">
        <p>Temperaturpunkter</p>
        <strong>{{ instances.length }}</strong>
      </article>
      <article class="summary-card summary-card--good">
        <p>Innenfor grense</p>
        <strong>{{ okCount }}</strong>
      </article>
      <article class="summary-card summary-card--warn">
        <p>Avvik</p>
        <strong>{{ alerts.length }}</strong>
      </article>
    </section>

    <!-- Error Messages -->
    <section v-if="error" class="inline-error">{{ error }}</section>
    
    <!-- Error Action: Displays action-specific errors -->
    <section v-else-if="actionError" class="inline-error">{{ actionError }}</section>

    <!-- Desktop display of temperature points: Table -->
    <section v-if="!isMobile" class="desktop-list" aria-label="Temperaturpunkter">
      <header class="desktop-list__header">
        <span>Tittel</span>
        <span>Temperatur</span>
        <span>Gyldig omrade</span>
        <span>Status</span>
        <span>Handlinger</span>
      </header>

      <!-- Temperature Control points instance -->
      <article
        v-for="instance in instances"
        :key="instance.pointId"
        class="desktop-row"
        :class="{ 'desktop-row--alert': instance.isAlert }"
      >
        <!-- Temperature Title + location -->
        <div>
          <p class="desktop-row__title">{{ instance.title }}</p>
          <p class="desktop-row__meta">{{ instance.locationName }}</p>
        </div>

        <!-- Temperature Measurement + DateTime -->
        <div>
          <p class="desktop-row__value">{{ instance.latestTemp === null ? '-' : `${instance.latestTemp}°C` }}</p>
          <p class="desktop-row__meta">{{ instance.latestDate }} {{ instance.latestTime === '-' ? '' : `kl. ${instance.latestTime}` }}</p>
        </div>

        <!-- Temperature Range -->
        <p class="desktop-row__value">{{ instance.minTemp }}°C til {{ instance.maxTemp }}°C</p>

        <!-- Alert Status -->
        <span class="status-pill" :class="instance.isAlert ? 'status-pill--danger' : 'status-pill--good'">
          {{ instance.isAlert ? 'Avvik' : instance.latestTemp === null ? 'Ikke målt' : 'OK' }}
        </span>

        <!-- Desktop Actions: Register measurement / deviation -->
        <div class="desktop-actions">
          <button
            type="button"
            class="action-btn"
            :class="instance.isAlert ? 'action-btn--danger' : ''"
            @click="instance.isAlert ? openDeviationFlow(instance) : openMeasurementModal(instance.pointId)"
          >
            {{ instance.isAlert ? 'Registrer avvik' : instance.latestEntryId ? 'Rediger måling' : 'Registrer måling' }}
          </button>

          <!-- If Admin or Manager, managment options: Delete & Edit-->
          <div v-if="canManage" class="options-menu">
            <button
              class="options-menu__trigger"
              type="button"
              aria-label="Åpne handlinger"
              :aria-expanded="desktopMenuPointId === instance.pointId"
              @click="toggleDesktopMenu(instance.pointId)"
            >
              <span class="dot" />
              <span class="dot" />
              <span class="dot" />
            </button>

            <!-- Dropdown Options Field -->
            <div v-if="desktopMenuPointId === instance.pointId" class="options-menu__list" role="menu">
              <button type="button" role="menuitem" class="options-menu__item" @click="openEditPointModal(instance.pointId)">Rediger</button>
              <button type="button" role="menuitem" class="options-menu__item options-menu__item--danger" @click="removePoint(instance.pointId)">Slett</button>
            </div>
          </div>
        </div>
      </article>

      <!-- Loading Item -->
      <p v-if="instances.length === 0 && !isLoading" class="empty-state">Ingen temperaturpunkter registrert.</p>

      <!-- Add Item Button -->
      <button v-if="canManage" type="button" class="add-item-btn" @click="openAddPointModal">
        + Legg til temperaturpunkt
      </button>
    </section>

    <!-- Mobile Display of Temperature Points: Cards-->
    <section v-else class="mobile-cards" aria-label="Temperaturkort">
      <article
        v-for="instance in instances"
        :key="instance.pointId"
        class="mobile-card"
        :class="{ 'mobile-card--alert': instance.isAlert, 'mobile-card--active': selectedMobileCardId === instance.pointId }"
        @click="toggleMobileActions(instance.pointId)"
      >
        <!-- Management -->
        <div v-if="canManage && selectedMobileCardId === instance.pointId" class="mobile-card__manage" @click.stop>
          <button type="button" class="mobile-card__manage-btn" @click="openEditPointModal(instance.pointId)">Rediger</button>
          <button type="button" class="mobile-card__manage-btn mobile-card__manage-btn--danger" @click="removePoint(instance.pointId)">Slett</button>
        </div>

        <!-- Header Mobile-->
        <header class="mobile-card__header">
          <h2>{{ instance.title }}</h2>
          <span class="status-pill" :class="instance.isAlert ? 'status-pill--danger' : 'status-pill--good'">
            {{ instance.isAlert ? 'Avvik' : instance.latestTemp === null ? 'Ikke målt' : 'OK' }}
          </span>
        </header>

        <!-- Body Mobile -->
        <p class="mobile-card__meta">{{ instance.locationName }}</p>
        <p class="mobile-card__temperature">{{ instance.latestTemp === null ? '-' : `${instance.latestTemp}°C` }}</p>
        <p class="mobile-card__meta">Gyldig: {{ instance.minTemp }}°C til {{ instance.maxTemp }}°C</p>
        <p class="mobile-card__meta">{{ instance.latestDate }} {{ instance.latestTime === '-' ? '' : `kl. ${instance.latestTime}` }}</p>
        <p class="mobile-card__meta">Malt av: {{ instance.latestRecordedBy }}</p>

        <!-- Register Measurement / Deviation -->
        <button
          type="button"
          class="action-btn mobile-card__action"
          :class="instance.isAlert ? 'action-btn--danger' : ''"
          @click.stop="instance.isAlert ? openDeviationFlow(instance) : openMeasurementModal(instance.pointId)"
        >
          {{ instance.isAlert ? 'Registrer avvik' : instance.latestEntryId ? 'Rediger måling' : 'Registrer måling' }}
        </button>
      </article>

      <!-- Empty State -->
      <p v-if="instances.length === 0 && !isLoading" class="empty-state">Ingen temperaturpunkter registrert.</p>

      <!-- Add Item Button: IF Manager or Admin -->
      <button v-if="canManage" type="button" class="add-item-btn" @click="openAddPointModal">
        + Legg til temperaturpunkt
      </button>
    </section>

    <!-- Temperature Point Overlay Modal -->
    <BaseModal
      :open="pointModalOpen"
      :title="pointModalMode === 'add' ? 'Legg til temperaturpunkt' : 'Rediger temperaturpunkt'"
      @close="closePointModal"
    >
      <form class="modal-form" @submit.prevent="savePoint">
        <label>
          Kontrollpunkt
          <input v-model="pointForm.name" type="text" required />
        </label>

        <label>
          Lokasjonsnavn
          <input v-model="pointForm.locationName" type="text" required />
        </label>

        <div class="modal-form__row">
          <label>
            Min temperatur (°C)
            <input v-model="pointForm.minTempC" type="number" step="0.1" required />
          </label>
          <label>
            Maks temperatur (°C)
            <input v-model="pointForm.maxTempC" type="number" step="0.1" required />
          </label>
        </div>

        <p v-if="Number(pointForm.minTempC) > Number(pointForm.maxTempC)" class="modal-form__error">
          Min temperatur kan ikke være høyere enn maks temperatur.
        </p>

        <label>
          Gyldig temperaturintervall
          <input :value="`${pointForm.minTempC || '-'}°C til ${pointForm.maxTempC || '-'}°C`" type="text" disabled />
        </label>

        <p v-if="selectedLocationLabel" class="modal-form__hint">Gyldig temperatur: {{ selectedLocationLabel }}</p>
      </form>

      <template #footer>
        <button type="button" class="modal-btn modal-btn--ghost" @click="closePointModal">Avbryt</button>
        <button type="button" class="modal-btn" :disabled="isSubmitting || Number(pointForm.minTempC) > Number(pointForm.maxTempC)" @click="savePoint">Lagre</button>
      </template>
    </BaseModal>

    <!-- Register Temperature Measurement Modal -->
    <BaseModal
      :open="measurementModalOpen"
      title="Registrer temperaturmaling"
      @close="closeMeasurementModal"
    >
      <form class="modal-form" @submit.prevent="submitMeasurement">
        <p v-if="currentMeasurementPoint" class="modal-form__hint">
          {{ currentMeasurementPoint.title }}: {{ currentMeasurementPoint.minTemp }}°C til {{ currentMeasurementPoint.maxTemp }}°C
        </p>

        <label>
          Målt temperatur (°C)
          <input v-model="measurementForm.temperatureC" type="number" step="0.1" required />
        </label>

        <label>
          Dato
          <input v-model="measurementForm.measuredDate" type="date" required />
        </label>

        <label>
          Tid
          <input v-model="measurementForm.measuredTime" type="time" required />
        </label>

        <label>
          Ansatt
          <select v-if="employeeOptions.length > 0" v-model.number="measurementForm.employeeId">
            <option :value="null">Velg ansatt</option>
            <option v-for="employee in employeeOptions" :key="employee.id" :value="employee.id">{{ employee.label }}</option>
          </select>
          <input v-model="measurementForm.employeeName" type="text" :required="employeeOptions.length === 0" placeholder="Navn på ansatt" />
        </label>
      </form>

      <template #footer>
        <button type="button" class="modal-btn modal-btn--ghost" @click="closeMeasurementModal">Avbryt</button>
        <button type="button" class="modal-btn" :disabled="isSubmitting" @click="submitMeasurement">{{ editingMeasurementEntryId ? 'Oppdater' : 'Send inn' }}</button>
      </template>
    </BaseModal>
  </div>
</template>

<style scoped>
.temperature-page {
  max-width: 1200px;
  margin: 0 auto;
}

.page-header {
  margin-bottom: 1.25rem;
}

.page-header h1 {
  margin: 0;
  font-size: var(--font-size-2xl);
  color: var(--ik-mat-primary);
}

.subtitle {
  margin: 0.35rem 0 0;
  color: var(--color-gray-500);
  font-size: var(--font-size-sm);
}

.alert-banner {
  border: 1px solid color-mix(in srgb, var(--color-danger) 35%, var(--color-border));
  background: var(--color-danger-bg);
  color: color-mix(in srgb, var(--color-danger) 70%, var(--color-foreground));
  border-radius: var(--radius-md);
  padding: 0.75rem 0.9rem;
  display: flex;
  justify-content: space-between;
  gap: 0.75rem;
  margin-bottom: 0.95rem;
}

.alert-banner p {
  margin: 0;
  font-size: var(--font-size-sm);
}

.alert-banner a {
  color: inherit;
  text-decoration: underline;
  font-size: var(--font-size-sm);
  white-space: nowrap;
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(10rem, 1fr));
  gap: 0.75rem;
  margin-bottom: 1rem;
}

.summary-card {
  border: 1px solid var(--color-border);
  background: var(--color-card);
  border-radius: var(--radius-md);
  padding: 0.8rem;
}

.summary-card p {
  margin: 0;
  color: var(--color-gray-600);
  font-size: var(--font-size-xs);
}

.summary-card strong {
  display: block;
  margin-top: 0.35rem;
  color: var(--color-foreground);
  font-size: 1.45rem;
}

.summary-card--good {
  border-left: 0.25rem solid var(--color-success);
}

.summary-card--warn {
  border-left: 0.25rem solid var(--color-warning);
}

.inline-error {
  margin-bottom: 0.85rem;
  border: 1px solid color-mix(in srgb, var(--color-danger) 40%, var(--color-border));
  background: var(--color-danger-bg);
  color: var(--color-danger);
  border-radius: var(--radius-md);
  padding: 0.65rem 0.75rem;
  font-size: var(--font-size-sm);
}

.desktop-list {
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  overflow: hidden;
  background: var(--color-card);
}

.desktop-list__header,
.desktop-row {
  display: grid;
  grid-template-columns: minmax(10rem, 1.5fr) minmax(8rem, 1fr) minmax(8rem, 1fr) minmax(6rem, auto) minmax(12rem, 1fr);
  gap: 0.75rem;
  align-items: center;
  padding: 0.75rem 0.85rem;
}

.desktop-list__header {
  background: color-mix(in srgb, var(--ik-mat-bg) 55%, var(--color-card));
  border-bottom: 1px solid var(--color-border);
  color: var(--color-gray-600);
  font-size: var(--font-size-xs);
  font-weight: var(--font-weight-semibold);
  text-transform: uppercase;
  letter-spacing: 0.03em;
}

.desktop-row {
  border-bottom: 1px solid var(--color-border);
}

.desktop-row:last-of-type {
  border-bottom: none;
}

.desktop-row--alert {
  background: color-mix(in srgb, var(--color-danger-bg) 70%, var(--color-card));
}

.desktop-row__title {
  margin: 0;
  color: var(--color-foreground);
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-semibold);
}

.desktop-row__meta {
  margin: 0.2rem 0 0;
  color: var(--color-gray-600);
  font-size: var(--font-size-xs);
}

.desktop-row__value {
  margin: 0;
  color: var(--color-foreground);
  font-size: var(--font-size-sm);
}

.desktop-actions {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  gap: 0.5rem;
  position: relative;
}

.action-btn {
  border: 1px solid var(--ik-mat-primary);
  background: color-mix(in srgb, var(--ik-mat-primary) 10%, var(--color-card));
  color: var(--ik-mat-primary);
  border-radius: var(--radius-sm);
  padding: 0.35rem 0.6rem;
  font-size: var(--font-size-xs);
  font-weight: var(--font-weight-semibold);
}

.action-btn--danger {
  border-color: var(--color-danger);
  background: var(--color-danger-bg);
  color: var(--color-danger);
}

.options-menu {
  position: relative;
}

.options-menu__trigger {
  aspect-ratio: 1 / 1;
  border: 1px solid var(--color-border);
  background: var(--color-card);
  border-radius: var(--radius-sm);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 0.15rem;
  cursor: pointer;
  width: 2rem;
}

.dot {
  width: 0.2rem;
  height: 0.2rem;
  background: var(--color-gray-600);
  border-radius: 100%;
}

.options-menu__list {
  position: absolute;
  top: calc(100% + 0.25rem);
  right: 0;
  z-index: 20;
  min-width: 9rem;
  background: var(--color-card);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  box-shadow: var(--shadow-sm);
  overflow: hidden;
}

.options-menu__item {
  width: 100%;
  border: 0;
  background: transparent;
  padding: 0.6rem 0.8rem;
  text-align: left;
  color: var(--color-foreground);
  font-size: var(--font-size-sm);
}

.options-menu__item:hover {
  background: var(--color-accent);
}

.options-menu__item--danger {
  color: var(--color-danger);
}

.mobile-cards {
  display: grid;
  gap: 0.85rem;
}

.mobile-card {
  position: relative;
  border: 1px solid var(--color-border);
  background: var(--color-card);
  border-radius: var(--radius-md);
  padding: 0.85rem;
}

.mobile-card--alert {
  border-left: 0.25rem solid var(--color-danger);
  background: var(--color-danger-bg);
}

.mobile-card--active {
  box-shadow: var(--shadow-md);
}

.mobile-card__manage {
  display: flex;
  gap: 0.45rem;
  margin-bottom: 0.7rem;
}

.mobile-card__manage-btn {
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  background: var(--color-card);
  color: var(--color-foreground);
  padding: 0.35rem 0.55rem;
  font-size: var(--font-size-xs);
}

.mobile-card__manage-btn--danger {
  border-color: color-mix(in srgb, var(--color-danger) 40%, var(--color-border));
  color: var(--color-danger);
}

.mobile-card__header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 0.6rem;
}

.mobile-card__header h2 {
  margin: 0;
  color: var(--color-foreground);
  font-size: var(--font-size-base);
}

.mobile-card__temperature {
  margin: 0.4rem 0;
  color: var(--color-foreground);
  font-size: 1.7rem;
  font-weight: var(--font-weight-bold);
}

.mobile-card__meta {
  margin: 0.25rem 0 0;
  color: var(--color-gray-600);
  font-size: var(--font-size-xs);
}

.mobile-card__action {
  margin-top: 0.65rem;
  width: 100%;
}

.status-pill {
  display: inline-flex;
  align-items: center;
  border: 1px solid transparent;
  border-radius: var(--radius-sm);
  padding: 0.2rem 0.45rem;
  font-size: var(--font-size-xs);
  font-weight: var(--font-weight-semibold);
}

.status-pill--good {
  color: var(--color-success);
  background: var(--color-success-bg);
  border-color: color-mix(in srgb, var(--color-success) 35%, var(--color-border));
}

.status-pill--danger {
  color: var(--color-danger);
  background: var(--color-danger-bg);
  border-color: color-mix(in srgb, var(--color-danger) 35%, var(--color-border));
}

.add-item-btn {
  margin-top: 1rem;
  padding: 0.65rem 1rem;
  border: 1px solid var(--ik-mat-primary);
  background: color-mix(in srgb, var(--ik-mat-primary) 7%, var(--color-card));
  color: var(--ik-mat-primary);
  font-weight: var(--font-weight-semibold);
  border-radius: var(--radius-md);
}

.empty-state {
  margin-top: 0.9rem;
  text-align: center;
  color: var(--color-gray-600);
  font-size: var(--font-size-sm);
}

.modal-form {
  display: grid;
  gap: 0.8rem;
}

.modal-form label {
  display: grid;
  gap: 0.3rem;
  font-size: var(--font-size-sm);
  color: var(--color-gray-700);
}

.modal-form input,
.modal-form select {
  border: 1px solid var(--color-border);
  padding: 0.55rem 0.7rem;
  border-radius: var(--radius-sm);
  background: var(--color-card);
}

.modal-form__hint {
  margin: 0;
  color: var(--color-gray-600);
  font-size: var(--font-size-xs);
}

.modal-form__row {
  display: grid;
  gap: 0.75rem;
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.modal-form__error {
  margin: 0;
  color: var(--color-danger);
  font-size: var(--font-size-xs);
}

.modal-btn {
  border: 1px solid var(--ik-mat-primary);
  background: var(--ik-mat-primary);
  color: #fff;
  border-radius: var(--radius-sm);
  padding: 0.45rem 0.8rem;
}

.modal-btn--ghost {
  border-color: var(--color-border);
  background: transparent;
  color: var(--color-foreground);
}

@media (max-width: 47.99rem) {
  .summary-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .modal-form__row {
    grid-template-columns: 1fr;
  }
}
</style>