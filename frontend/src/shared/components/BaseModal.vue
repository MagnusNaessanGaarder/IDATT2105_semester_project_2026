<!--
  BaseModal - Gjenbrukbar modal/dialog-komponent
  
  Brukes for dialoger, bekreftelser og popup-vinduer.
  Støtter Escape-tast for lukking, klikk utenfor for lukking
  Har Teleport til body for riktig z-index
  
  Eksempel:
  <BaseModal :open="showModal" title="Bekreft sletting" @close="showModal = false">
    <p>Er du sikker?</p>
    <template #footer>
      <BaseButton @click="confirm">Ja</BaseButton>
    </template>
  </BaseModal>
-->
<script setup lang="ts">
  import { onMounted, onUnmounted } from 'vue'

  interface Props {
    open: boolean
    title: string
  }

  const props = defineProps<Props>()

  const emit = defineEmits<{
    close: []
  }>()

  const handleKeydown = (event: KeyboardEvent) => {
    if (event.key === 'Escape' && props.open) {
      emit('close')
    }
  }

  onMounted(() => {
    document.addEventListener('keydown', handleKeydown)
  })

  onUnmounted(() => {
    document.removeEventListener('keydown', handleKeydown)
  })
</script>

<template>
  <Teleport to="body">
    <div
      v-if="open"
      class="modal-overlay"
      @click="emit('close')"
    >
      <div
        class="modal"
        role="dialog"
        aria-modal="true"
        @click.stop
      >
        <div class="modal__header">
          <h2 class="modal__title">{{ title }}</h2>
          <button
            class="modal__close"
            aria-label="Lukk"
            @click="emit('close')"
          >
            <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <line x1="18" y1="6" x2="6" y2="18"></line>
              <line x1="6" y1="6" x2="18" y2="18"></line>
            </svg>
          </button>
        </div>
        
        <div class="modal__content">
          <slot />
        </div>
        
        <div v-if="$slots.footer" class="modal__footer">
          <slot name="footer" />
        </div>
      </div>
    </div>
  </Teleport>
</template>

<style scoped>
  .modal-overlay {
    position: fixed;
    inset: 0;
    background: var(--color-overlay);
    backdrop-filter: blur(4px);
    display: flex;
    align-items: center;
    justify-content: center;
    z-index: 100;
    padding: var(--spacing-md);
  }

  .modal {
    background: var(--color-card);
    width: 100%;
    max-width: 32rem;
    max-height: 90vh;
    overflow-y: auto;
    border: 1px solid var(--color-border);
    border-radius: var(--radius-lg);
    box-shadow: var(--shadow-lg);
    animation: modal-enter var(--transition-base) var(--ease-emphasized);
  }

  .modal__header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: var(--spacing-md);
    border-bottom: 1px solid var(--color-border);
  }

  .modal__title {
    font-family: var(--font-family-display);
    font-size: var(--font-size-xl);
    font-weight: var(--font-weight-semibold);
    margin: 0;
  }

  .modal__close {
    display: flex;
    align-items: center;
    justify-content: center;
    width: 2rem;
    height: 2rem;
    background: transparent;
    border: none;
    cursor: pointer;
    color: var(--color-gray-500);
    border-radius: var(--radius-md);
    transition: background-color var(--transition-fast), color var(--transition-fast);
  }

  .modal__close:hover {
    background: var(--color-info-bg);
    color: var(--color-primary);
  }

  .modal__close:focus-visible {
    outline: 2px solid var(--color-focus);
    outline-offset: 2px;
  }

  .modal__content {
    padding: var(--spacing-md);
  }

  .modal__footer {
    display: flex;
    justify-content: flex-end;
    gap: var(--spacing-sm);
    padding: var(--spacing-md);
    border-top: 1px solid var(--color-border);
  }

  @keyframes modal-enter {
    from {
      opacity: 0;
      transform: translateY(8px) scale(0.97);
    }
    to {
      opacity: 1;
      transform: translateY(0) scale(1);
    }
  }

  @media (prefers-reduced-motion: reduce) {
    .modal {
      animation: none;
    }
  }
</style>