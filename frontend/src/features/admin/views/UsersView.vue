<script setup lang="ts">
</script>

<template>
  <div class="view-page">
    <header class="page-header">
      <h1>Brukere</h1>
      <p class="subtitle">Administrer brukere og tilganger</p>
    </header>
    
    <div class="admin-badge">
      <span class="badge">Kun Administrator</span>
    </div>
    
    <div class="content-placeholder">
      <p>Brukeradministrasjon kommer her...</p>
    </div>
  </div>
</template>

<style scoped>
.view-page {
  max-width: 1200px;
  margin: 0 auto;
}

.page-header {
  margin-bottom: 16px;
}

.page-header h1 {
  font-size: var(--font-size-2xl);
  font-weight: var(--font-weight-bold);
  color: var(--color-foreground);
  margin-bottom: 8px;
}

.subtitle {
  font-size: var(--font-size-base);
  color: var(--color-gray-500);
}

.admin-badge {
  margin-bottom: 24px;
}

.badge {
  display: inline-flex;
  align-items: center;
  padding: 6px 12px;
  background-color: var(--color-gray-100);
  color: var(--color-gray-600);
  font-size: var(--font-size-xs);
  font-weight: var(--font-weight-semibold);
  text-transform: uppercase;
  letter-spacing: 0.05em;
  border-radius: var(--radius-sm);
}

.content-placeholder {
  background: var(--color-card);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  padding: 48px;
  text-align: center;
  color: var(--color-gray-500);
}
</style>