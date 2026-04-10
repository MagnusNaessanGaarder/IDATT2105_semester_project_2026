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
  import { nextTick, onMounted, onUnmounted, ref, watch } from 'vue'

  interface Props {
    open: boolean
    title: string
  }

  const props = defineProps<Props>()

  const emit = defineEmits<{
    close: []
  }>()

  const modalRef = ref<HTMLElement | null>(null)
  const previouslyFocusedElement = ref<HTMLElement | null>(null)

  const getFocusableElements = () => {
    const root = modalRef.value
    if (!root) {
      return [] as HTMLElement[]
    }

    return Array.from(
      root.querySelectorAll<HTMLElement>(
        [
          'button:not([disabled])',
          '[href]',
          'input:not([disabled])',
          'select:not([disabled])',
          'textarea:not([disabled])',
          '[tabindex]:not([tabindex="-1"])',
        ].join(', ')
      )
    )
  }

  const handleKeydown = (event: KeyboardEvent) => {
    if (!props.open) {
      return
    }

    if (event.key === 'Escape') {
      emit('close')
      return
    }

    if (event.key !== 'Tab') {
      return
    }

    const focusableElements = getFocusableElements()
    if (focusableElements.length === 0) {
      event.preventDefault()
      return
    }

    const firstElement = focusableElements[0]
    const lastElement = focusableElements[focusableElements.length - 1]
    const activeElement = document.activeElement as HTMLElement | null

    if (event.shiftKey && activeElement === firstElement) {
      event.preventDefault()
      lastElement.focus()
    } else if (!event.shiftKey && activeElement === lastElement) {
      event.preventDefault()
      firstElement.focus()
    }
  }

  watch(
    () => props.open,
    async (isOpen) => {
      if (isOpen) {
        previouslyFocusedElement.value = document.activeElement as HTMLElement | null
        await nextTick()
        const focusableElements = getFocusableElements()
        focusableElements[0]?.focus()
        return
      }

      previouslyFocusedElement.value?.focus?.()
    }
  )

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
        ref="modalRef"
        class="modal"
        role="dialog"
        aria-modal="true"
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
    background: rgba(0, 39, 43, 0.58);
    display: flex;
    align-items: center;
    justify-content: center;
    z-index: 100;
    padding: var(--spacing-lg);
  }

  .modal {
    background: var(--color-surface-raised);
    width: 100%;
    max-width: 26rem;
    max-height: 90vh;
    overflow-y: auto;
    border: none;
    border-radius: var(--radius-sm);
    box-shadow: var(--shadow-md);
    display: flex;
    flex-direction: column;
  }

  .modal__header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: var(--spacing-lg);
    border-bottom: 1px solid var(--color-border);
  }

  .modal__title {
    font-family: var(--font-family-display);
    font-size: var(--font-size-xl);
    font-weight: var(--font-weight-semibold);
    color: var(--color-foreground);
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
    color: var(--color-foreground);
    border-radius: var(--radius-sm);
    transition: background-color var(--transition-fast), color var(--transition-fast);
    padding: 0;
    flex-shrink: 0;
  }

  .modal__close:hover {
    background: var(--color-info-bg);
    color: var(--color-primary);
  }

  .modal__close:focus-visible {
    outline: 2px solid var(--color-primary);
    outline-offset: 2px;
  }

  .modal__content {
    padding: var(--spacing-lg);
    background: var(--color-surface-raised);
  }

  .modal__content :deep(input:not([type='checkbox']):not([type='radio']):not([type='file']):not([type='hidden'])),
  .modal__content :deep(select),
  .modal__content :deep(textarea) {
    border: 1px solid var(--color-border-strong);
    border-radius: var(--radius-sm);
    background: var(--color-surface-raised);
    color: var(--color-foreground);
  }

  .modal__content :deep(input:not([type='checkbox']):not([type='radio']):not([type='file']):not([type='hidden']):focus),
  .modal__content :deep(select:focus),
  .modal__content :deep(textarea:focus),
  .modal__content :deep(input:not([type='checkbox']):not([type='radio']):not([type='file']):not([type='hidden']):focus-visible),
  .modal__content :deep(select:focus-visible),
  .modal__content :deep(textarea:focus-visible) {
    outline: none;
    border-color: var(--color-primary);
    box-shadow: 0 0 0 3px var(--color-info-bg);
    background: var(--color-surface-raised);
  }

  .modal__footer {
    display: flex;
    justify-content: flex-end;
    gap: var(--spacing-sm);
    padding: var(--spacing-lg);
    border-top: 1px solid var(--color-border);
  }
</style>