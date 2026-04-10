<!--
  BaseModal - Gjenbrukbar modal/dialog-komponent
  
  Brukes for dialoger, bekreftelser og popup-vinduer.
  
  Tilgjengelighetsfunksjoner:
  - Trap focus: Tab-navigasjon holdes innenfor modalen
  - Escape-tast lukker modalen
  - Klikk utenfor lukker modalen
  - Fokus gjenopprettes til trigger-element ved lukking
  - ARIA-attributter for skjermlesere
  - Teleport til body for riktig z-index
  
  Eksempel:
  <BaseModal :open="showModal" title="Bekreft sletting" @close="showModal = false">
    <p>Er du sikker?</p>
    <template #footer>
      <BaseButton @click="confirm">Ja</BaseButton>
    </template>
  </BaseModal>
-->
<script setup lang="ts">
  import { onMounted, onUnmounted, watch } from 'vue'
  import { useFocusTrap } from '../composables/useFocusTrap'

  interface Props {
    open: boolean
    title: string
  }

  const props = defineProps<Props>()

  const emit = defineEmits<{
    close: []
  }>()

  // Focus trap composable for keyboard accessibility
  const { trapRef, activate, deactivate } = useFocusTrap()

  const handleKeydown = (event: KeyboardEvent) => {
    if (!props.open) {
      return
    }

    if (event.key === 'Escape') {
      emit('close')
      return
    }
  }

  // Watch for modal open/close to activate/deactivate focus trap
  watch(() => props.open, (isOpen) => {
    if (isOpen) {
      activate()
    } else {
      deactivate()
    }
  })

  onMounted(() => {
    document.addEventListener('keydown', handleKeydown)
    // Activate immediately if modal is already open on mount
    if (props.open) {
      activate()
    }
  })

  onUnmounted(() => {
    document.removeEventListener('keydown', handleKeydown)
    deactivate()
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
        ref="trapRef"
        class="modal"
        role="dialog"
        aria-modal="true"
        tabindex="-1"
        :aria-label="title"
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
    background: var(--color-overlay-soft);
    backdrop-filter: blur(6px);
    display: flex;
    align-items: center;
    justify-content: center;
    z-index: 100;
    padding: var(--spacing-md);
    padding-inline: clamp(1rem, 4vw, 2.5rem);
  }

  .modal {
    background: linear-gradient(180deg, var(--color-surface-raised) 0%, var(--color-surface-muted) 100%);
    width: 100%;
    max-width: 32rem;
    max-height: 90vh;
    overflow-y: auto;
    border: 1px solid var(--color-border-strong);
    border-radius: var(--radius-lg);
    box-shadow: var(--shadow-lg), inset 0 1px 0 rgba(255, 255, 255, 0.72);
    animation: modal-enter var(--transition-base) var(--ease-emphasized);
  }

  .modal__header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: var(--spacing-md);
    border-bottom: 1px solid var(--color-border-strong);
    background: color-mix(in srgb, var(--color-surface-muted) 70%, var(--color-surface-raised));
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
    background: var(--color-surface-raised);
  }

  .modal__content :deep(input:not([type='checkbox']):not([type='radio']):not([type='file']):not([type='hidden'])),
  .modal__content :deep(select),
  .modal__content :deep(textarea) {
    border: 1px solid var(--color-gray-400);
    border-radius: var(--radius-md);
    background: var(--color-card);
    color: var(--color-foreground);
    box-shadow: var(--shadow-sm);
  }

  .modal__content :deep(input:not([type='checkbox']):not([type='radio']):not([type='file']):not([type='hidden']):focus),
  .modal__content :deep(select:focus),
  .modal__content :deep(textarea:focus),
  .modal__content :deep(input:not([type='checkbox']):not([type='radio']):not([type='file']):not([type='hidden']):focus-visible),
  .modal__content :deep(select:focus-visible),
  .modal__content :deep(textarea:focus-visible) {
    outline: none;
    border-color: var(--color-focus);
    box-shadow: var(--shadow-focus);
    background: var(--color-surface-raised);
  }

  .modal__footer {
    display: flex;
    justify-content: flex-end;
    gap: var(--spacing-sm);
    padding: var(--spacing-md);
    border-top: 1px solid var(--color-border-strong);
    background: color-mix(in srgb, var(--color-surface-muted) 60%, var(--color-surface-raised));
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
