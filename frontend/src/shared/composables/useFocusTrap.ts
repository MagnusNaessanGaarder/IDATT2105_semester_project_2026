import { ref, onMounted, onUnmounted, nextTick, type Ref } from 'vue'

/**
 * useFocusTrap - Composable for trapping focus within a modal/dialog
 *
 * This ensures keyboard users can only tab within the modal,
 * cycling back to the first element when reaching the end.
 *
 * Usage:
 * const { trapRef, activate, deactivate } = useFocusTrap()
 *
 * // In template: <div ref="trapRef">
 * // When modal opens: activate()
 * // When modal closes: deactivate()
 */

export function useFocusTrap() {
  const trapRef: Ref<HTMLElement | null> = ref(null)
  const previousActiveElement: Ref<Element | null> = ref(null)
  let isActive = false

  // Elements that can receive focus
  const focusableSelectors = [
    'button:not([disabled]):not([tabindex="-1"])',
    'a[href]:not([tabindex="-1"])',
    'input:not([disabled]):not([tabindex="-1"])',
    'select:not([disabled]):not([tabindex="-1"])',
    'textarea:not([disabled]):not([tabindex="-1"])',
    '[tabindex]:not([tabindex="-1"]):not([disabled])'
  ].join(', ')

  const getFocusableElements = (): HTMLElement[] => {
    if (!trapRef.value) return []
    return Array.from(trapRef.value.querySelectorAll(focusableSelectors))
      .filter((el): el is HTMLElement => {
        // Check if element is actually visible
        const style = window.getComputedStyle(el)
        return style.display !== 'none' && style.visibility !== 'hidden'
      })
  }

  const getFirstFocusableElement = (): HTMLElement | null => {
    const elements = getFocusableElements()
    return elements[0] || null
  }

  const getLastFocusableElement = (): HTMLElement | null => {
    const elements = getFocusableElements()
    return elements[elements.length - 1] || null
  }

  const handleTabKey = (event: KeyboardEvent) => {
    if (!isActive || !trapRef.value) return

    const focusableElements = getFocusableElements()
    if (focusableElements.length === 0) {
      event.preventDefault()
      return
    }

    const firstElement = focusableElements[0]
    const lastElement = focusableElements[focusableElements.length - 1]
    const activeElement = document.activeElement as HTMLElement

    // Shift + Tab - go backwards
    if (event.shiftKey) {
      if (activeElement === firstElement || !trapRef.value.contains(activeElement)) {
        event.preventDefault()
        if (lastElement) {
          lastElement.focus()
        }
      }
    } else {
      // Tab - go forwards
      if (activeElement === lastElement || !trapRef.value.contains(activeElement)) {
        event.preventDefault()
        if (firstElement) {
          firstElement.focus()
        }
      }
    }
  }

  const handleKeydown = (event: KeyboardEvent) => {
    if (event.key === 'Tab') {
      handleTabKey(event)
    }
  }

  const activate = (options?: { initialFocus?: boolean }) => {
    if (isActive) return

    // Store the element that had focus before opening the modal
    previousActiveElement.value = document.activeElement
    isActive = true

    // Add event listener
    document.addEventListener('keydown', handleKeydown)

    // Focus the first focusable element
    nextTick(() => {
      if (options?.initialFocus !== false) {
        const firstElement = getFirstFocusableElement()
        if (firstElement) {
          firstElement.focus()
        } else if (trapRef.value) {
          // If no focusable elements, focus the modal itself
          trapRef.value.focus()
        }
      }
    })
  }

  const deactivate = () => {
    if (!isActive) return

    isActive = false
    document.removeEventListener('keydown', handleKeydown)

    // Restore focus to the element that had it before
    nextTick(() => {
      if (previousActiveElement.value instanceof HTMLElement) {
        previousActiveElement.value.focus()
      }
    })
  }

  onUnmounted(() => {
    deactivate()
  })

  return {
    trapRef,
    activate,
    deactivate,
    getFocusableElements,
    getFirstFocusableElement,
    getLastFocusableElement
  }
}
