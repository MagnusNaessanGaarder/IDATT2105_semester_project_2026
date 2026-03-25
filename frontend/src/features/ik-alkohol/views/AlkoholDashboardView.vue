<script setup lang="ts">
</script>

<template>
  <div class="alkohol-dashboard">
    <header class="page-header">
      <h1>IK-Alkohol Dashboard</h1>
      <p class="subtitle">Ansvarlig alkoholservering</p>
    </header>
    
    <div class="dashboard-grid">
      <div class="card">
        <h2>Daglig kontroll</h2>
        <p>Daglige kontroller for alkoholservering</p>
        <router-link :to="{ name: 'DailyControl' }" class="btn">
          Start kontroll
        </router-link>
      </div>
      
      <div class="card">
        <h2>Sertifiseringer</h2>
        <p>Oversikt over ansattes kunnskapsprøver</p>
        <router-link :to="{ name: 'Certifications' }" class="btn">
          Se sertifiseringer
        </router-link>
      </div>
      
      <div class="card">
        <h2>Regelverk</h2>
        <p>Alkoholloven og skjenkeforskriften</p>
        <router-link :to="{ name: 'Regulations' }" class="btn">
          Se regelverk
        </router-link>
      </div>
    </div>
  </div>
</template>

<style scoped>
.alkohol-dashboard {
  max-width: 1200px;
  margin: 0 auto;
}

.page-header {
  margin-bottom: 32px;
}

.page-header h1 {
  font-size: var(--font-size-2xl);
  font-weight: var(--font-weight-bold);
  color: var(--alkohol-primary);
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
  color: var(--alkohol-primary);
  background: var(--alkohol-bg);
  border: 1px solid var(--alkohol-primary);
  border-radius: var(--radius-md);
  text-decoration: none;
  transition: all var(--transition-fast);
}

.btn:hover {
  background: var(--alkohol-primary);
  color: white;
  text-decoration: none;
}
</style>