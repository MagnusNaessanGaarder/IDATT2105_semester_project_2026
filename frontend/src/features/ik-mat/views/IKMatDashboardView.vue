<script setup lang="ts">
</script>

<template>
  <div class="ikmat-dashboard">
    <header class="page-header">
      <h1>IK-Mat Dashboard</h1>
      <p class="subtitle">Matsikkerhet og hygienekontroll</p>
    </header>
    
    <div class="dashboard-grid">
      <div class="card">
        <h2>Sjekklister</h2>
        <p>Oversikt over daglige, ukentlige og månedlige sjekklister</p>
        <router-link :to="{ name: 'Checklists' }" class="btn">
          Se sjekklister
        </router-link>
      </div>
      
      <div class="card">
        <h2>Temperatur</h2>
        <p>Registrering og overvåking av matlagringstemperaturer</p>
        <router-link :to="{ name: 'Temperature' }" class="btn">
          Registrer temperatur
        </router-link>
      </div>
      
      <div class="card">
        <h2>Avvik</h2>
        <p>Rapportering og oppfølging av avvik fra standarder</p>
        <router-link :to="{ name: 'Deviations' }" class="btn">
          Se avvik
        </router-link>
      </div>
      
      <div class="card">
        <h2>HACCP-plan</h2>
        <p>Oversikt over kritiske kontrollpunkter</p>
        <router-link :to="{ name: 'HACCP' }" class="btn">
          Se HACCP-plan
        </router-link>
      </div>
    </div>
  </div>
</template>

<style scoped>
.ikmat-dashboard {
  max-width: 1200px;
  margin: 0 auto;
}

.page-header {
  margin-bottom: 32px;
}

.page-header h1 {
  font-size: var(--font-size-2xl);
  font-weight: var(--font-weight-bold);
  color: var(--ikmat-primary);
  margin-bottom: 8px;
}

.subtitle {
  font-size: var(--font-size-base);
  color: var(--color-gray-500);
}

.dashboard-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
  gap: 24px;
}

.card {
  background: var(--color-card);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  padding: 24px;
  box-shadow: var(--shadow-sm);
}

.card h2 {
  font-size: var(--font-size-lg);
  font-weight: var(--font-weight-bold);
  color: var(--color-foreground);
  margin-bottom: 8px;
}

.card p {
  font-size: var(--font-size-base);
  color: var(--color-gray-500);
  margin-bottom: 16px;
}

.btn {
  display: inline-flex;
  align-items: center;
  padding: 10px 16px;
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-semibold);
  color: var(--ikmat-primary);
  background: var(--ikmat-bg);
  border: 1px solid var(--ikmat-primary);
  border-radius: var(--radius-md);
  text-decoration: none;
  transition: all var(--transition-fast);
}

.btn:hover {
  background: var(--ikmat-primary);
  color: white;
  text-decoration: none;
}
</style>