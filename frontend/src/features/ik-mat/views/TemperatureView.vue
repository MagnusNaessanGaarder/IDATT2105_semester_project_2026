<script setup lang="ts">
import { ref } from 'vue'
import ikMatData from '@/data/ik-mat.json'
import TemperatureCard from '../components/TemperatureCard.vue'

interface TemperatureRecord {
  id: number
  location: string
  temperature_c: number
  min_temp: number
  max_temp: number
  recorded_by: string
  recorded_date: string
  recorded_time: string
  status: 'ok' | 'warning' | 'critical'
}

const temperatures = ref<TemperatureRecord[]>(ikMatData.temperature as TemperatureRecord[])

const handleViewTemperature = (record: TemperatureRecord) => {
  void record
}
</script>

<template>
  <div class="view-page">
    <header class="page-header">
      <h1>Temperaturkontroll</h1>
      <p class="subtitle">Overvåk temperaturer på kjølte og frossne lagre</p>
    </header>

    <div class="temperatures-grid">
      <TemperatureCard 
        v-for="record in temperatures"
        :key="record.id"
        :record="record"
      />
    </div>

    <div v-if="temperatures.length === 0" class="empty-state">
      <p>Ingen temperaturmålinger registrert</p>
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

.temperatures-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 1.5rem;
  margin-bottom: 2rem;
}

.empty-state {
  text-align: center;
  padding: 3rem 1.5rem;
  background: var(--color-card);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  color: var(--color-gray-600);
}

@media (max-width: 48rem) {
  .temperatures-grid {
    grid-template-columns: 1fr;
  }
}
</style>