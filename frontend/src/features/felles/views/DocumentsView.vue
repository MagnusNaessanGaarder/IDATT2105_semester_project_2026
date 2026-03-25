<script setup lang="ts">
</script>

<template>
  <div class="view-page">
    <header class="page-header">
      <h1>Dokumenter</h1>
      <p class="subtitle">Sentralisert lagring av policyer og opplæringsmateriale</p>
    </header>
    
    <div class="content-placeholder">
      <p>Dokument-funksjonalitet kommer her...</p>
    </div>
  </div>
</template>

<style scoped>
.view-page {
  max-width: 1200px;
  margin: 0 auto;
}

.page-header {
  margin-bottom: 32px;
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

.content-placeholder {
  background: var(--color-card);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  padding: 48px;
  text-align: center;
  color: var(--color-gray-500);
}
</style>