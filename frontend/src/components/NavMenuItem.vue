<script setup lang="ts">
import { motion } from 'motion-v'

const props = withDefaults(
  defineProps<{
    variant: 'main' | 'sub'
    label: string
    active?: boolean
    badge?: number
    icon?: string
    expanded?: boolean
  }> (), 
  {
    active: false,
    badge: 0,
    icon: '',
    expanded: false,

  }
)

const emit = defineEmits<{
  select: []
}>()

const handleSelect = () => {
  emit('select')
}
</script>

<template>
  <motion.div
    class="menu-item"
    :class="[
      `menu-item--${props.variant}`,
      { 'menu-item--active': props.active },
    ]"
    :initial="{ opacity: 0, y: 4 }"
    :animate="{ opacity: 1, y: 0, transition: { duration: 0.18 } }"
  >
    <motion.button
      class="menu-item__button"
      :class="[`menu-item__button--${props.variant}`]"
      @click="handleSelect"
      :aria-expanded="props.variant === 'main' ? props.expanded : undefined"
      :aria-current="props.active ? 'page' : undefined"
      :while-hover="{ x: props.variant === 'sub' ? 2 : 0 }"
      :while-tap="{ scale: 0.995 }"
      :transition="{ duration: 0.16 }"
    >
      <span v-if="props.variant === 'main'" class="menu-item__icon" aria-hidden="true">{{ props.icon }}</span>
      <span class="menu-item__label">{{ props.label }}</span>
      <span v-if="props.badge" class="menu-item__badge">{{ props.badge }}</span>
      <svg
        v-if="props.variant === 'main'"
        width="14"
        height="14"
        viewBox="0 0 24 24"
        fill="none"
        stroke="currentColor"
        stroke-width="2"
        class="menu-item__chevron"
        :class="{ 'menu-item__chevron--open': props.expanded }"
      >
        <polyline points="6 9 12 15 18 9" />
      </svg>
    </motion.button>
  </motion.div>
</template>

<style scoped>
.menu-item {
  width: 100%;
}

.menu-item__button {
  width: 100%;
  min-height: 2.75rem;
  display: flex;
  align-items: center;
  gap: 0.625rem;
  background: transparent;
  border: none;
  cursor: pointer;
  text-align: left;
  font-family: inherit;
  border-radius: 0;
  transition: background-color var(--transition-fast), color var(--transition-fast), box-shadow var(--transition-fast);
}

.menu-item__button--main {
  padding: var(--spacing-md) var(--spacing-md);
}

.menu-item__button--sub {
  padding: var(--spacing-md) var(--spacing-md);
}

.menu-item__icon {
  width: 1.125rem;
  height: 1.125rem;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--color-gray-400);
  transition: color var(--transition-fast);
}

.menu-item__label {
  font-size: 0.8125rem;
  font-weight: 500;
  color: var(--color-gray-700);
}

.menu-item--main .menu-item__label {
  font-size: 0.75rem;
  font-weight: 600;
  letter-spacing: 0.07em;
  text-transform: uppercase;
}

.menu-item__badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 1.125rem;
  height: 1.125rem;
  padding: 0 0.375rem;
  background-color: var(--color-danger);
  color: var(--color-primary-foreground);
  font-size: 0.6875rem;
  font-weight: 700;
  border-radius: 624.9375rem;
  margin-left: auto;
  box-shadow: 0 0.0625rem 0 rgba(0, 0, 0, 0.08);
}

.menu-item__chevron {
  margin-left: 0.375rem;
  color: var(--color-gray-400);
  transform: rotate(-90deg);
  transition: transform var(--transition-base) ease, color var(--transition-fast);
}

.menu-item__chevron--open {
  transform: rotate(0deg);
}

.menu-item__button:hover {
  background-color: var(--color-accent);
}

.menu-item--sub .menu-item__button:hover {
  transition: background-color var(--transition-fast);
  color: var(--color-primary);
}

.menu-item--active .menu-item__button {
  background-color: var(--color-accent-hover);
  border-left-color: var(--ik-mat-primary);
  border-left-style: solid;
  border-left-width: 0.25rem;
  padding-left: calc(var(--spacing-md) - 0.25rem);
  color: var(--color-primary);
}

.menu-item--active .menu-item__label,
.menu-item--active .menu-item__chevron {
  color: var(--color-gray-900);
}

.menu-item--active .menu-item__icon {
  color: var(--ik-mat-primary);
}

.menu-item--active .menu-item__chevron {
  color: var(--ik-mat-primary);
}

.menu-item--sub.menu-item--active .menu-item__label {
  color: var(--color-primary);
  font-weight: 600;
}

.menu-item__button:focus-visible {
  outline: 0.125rem solid var(--color-focus);
  outline-offset: -0.125rem;
}

@media (prefers-reduced-motion: reduce) {
  .menu-item__button,
  .menu-item__chevron,
  .menu-item--sub .menu-item__button:hover {
    transition: none;
    transform: none;
  }
}
</style>
