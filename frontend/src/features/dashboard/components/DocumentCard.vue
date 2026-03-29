<script setup lang="ts">
interface Document {
  id: number
  name: string
  category: string
  file_type: string
  uploaded_by: string
  uploaded_date: string
  size: string
  version: string
  status: 'active' | 'archived'
  description: string
}

const props = defineProps<{
  document: Document
}>()

const emit = defineEmits<{
  view: []
  download: []
}>()

const fileIcon = () => {
  if (props.document.file_type === 'PDF') return '📄'
  if (props.document.file_type === 'Excel') return '📊'
  return '📋'
}
</script>

<template>
  <div class="document-card" :class="{ 'document-card--archived': document.status === 'archived' }">
    <div class="document-card__header">
      <div class="document-card__icon">{{ fileIcon() }}</div>
      <div class="document-card__title-section">
        <h3 class="document-card__title">{{ document.name }}</h3>
        <p class="document-card__description">{{ document.description }}</p>
      </div>
    </div>

    <div class="document-card__body">
      <div class="document-card__meta-row">
        <span class="document-card__category">{{ document.category }}</span>
        <span class="document-card__version">v{{ document.version }}</span>
        <span class="document-card__size">{{ document.size }}</span>
      </div>
      <p class="document-card__upload-info">
        Lastet opp av {{ document.uploaded_by }} den {{ document.uploaded_date }}
      </p>
    </div>

    <div class="document-card__footer">
      <button class="document-card__action-btn" @click="emit('view')">Åpne</button>
      <button class="document-card__action-btn document-card__action-btn--secondary" @click="emit('download')">Last ned</button>
    </div>
  </div>
</template>

<style scoped>
.document-card {
  background: var(--color-card);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  overflow: hidden;
  transition: all var(--transition-base);
  display: flex;
  flex-direction: column;
}

.document-card:hover {
  box-shadow: var(--shadow-md);
  border-color: var(--color-border-focus);
}

.document-card--archived {
  opacity: 0.6;
}

.document-card__header {
  display: flex;
  gap: 1rem;
  padding: 1.5rem;
  border-bottom: 1px solid var(--color-border);
}

.document-card__icon {
  font-size: 2rem;
  flex-shrink: 0;
}

.document-card__title-section {
  flex: 1;
  min-width: 0;
}

.document-card__title {
  margin: 0;
  font-size: var(--text-lg);
  font-weight: 600;
  color: var(--color-foreground);
  word-break: break-word;
}

.document-card__description {
  margin: 0.5rem 0 0;
  font-size: var(--text-sm);
  color: var(--color-gray-600);
}

.document-card__body {
  padding: 1rem 1.5rem;
  flex: 1;
  background: var(--color-accent);
}

.document-card__meta-row {
  display: flex;
  flex-wrap: wrap;
  gap: 1rem;
  margin-bottom: 0.75rem;
}

.document-card__category {
  display: inline-block;
  background: var(--color-card);
  padding: 0.25rem 0.75rem;
  border-radius: var(--radius-sm);
  font-size: var(--text-xs);
  font-weight: 600;
  text-transform: uppercase;
  color: var(--color-gray-600);
}

.document-card__version {
  font-size: var(--text-xs);
  color: var(--color-gray-600);
  font-weight: 600;
}

.document-card__size {
  font-size: var(--text-xs);
  color: var(--color-gray-600);
}

.document-card__upload-info {
  margin: 0;
  font-size: var(--text-xs);
  color: var(--color-gray-600);
}

.document-card__footer {
  display: flex;
  gap: 0.5rem;
  padding: 1rem 1.5rem;
  border-top: 1px solid var(--color-border);
}

.document-card__action-btn {
  flex: 1;
  padding: 0.5rem 1rem;
  background: var(--color-foreground);
  color: var(--color-background);
  border: none;
  border-radius: var(--radius-sm);
  font-size: var(--text-sm);
  font-weight: 600;
  cursor: pointer;
  transition: background-color var(--transition-fast);
}

.document-card__action-btn:hover {
  background: var(--color-gray-900);
}

.document-card__action-btn--secondary {
  background: var(--color-gray-200);
  color: var(--color-foreground);
}

.document-card__action-btn--secondary:hover {
  background: var(--color-gray-300);
}

@media (max-width: 48rem) {
  .document-card__header {
    gap: 0.75rem;
  }

  .document-card__icon {
    font-size: 1.5rem;
  }
}
</style>
