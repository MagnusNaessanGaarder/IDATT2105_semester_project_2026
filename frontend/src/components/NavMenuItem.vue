<script setup lang="ts">
import { motion } from 'motion-v'

const props = withDefaults(
  defineProps<{
    variant: 'main' | 'sub'
    label: string
    active?: boolean
    hasActiveChild?: boolean
    badge?: number
    icon?: string
    expanded?: boolean
  }> (),
  {
    active: false,
    hasActiveChild: false,
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
      { 'menu-item--has-active-child': props.hasActiveChild },
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
      <span v-if="props.variant === 'main'" class="menu-item__icon" aria-hidden="true">
        <slot name="icon">{{ props.icon }}</slot>
      </span>
      <span class="menu-item__label">{{ props.label }}</span>
      <span v-if="props.badge" class="menu-item__badge">{{ props.badge }}</span>
      <motion.svg
        v-if="props.variant === 'main'"
        width="18"
        height="18"
        viewBox="0 0 24 24"
        fill="none"
        stroke="currentColor"
        stroke-width="2.4"
        class="menu-item__chevron"
        :animate="{ rotate: props.expanded ? 90 : 0 }"
        :transition="{ duration: 0.2, ease: 'easeOut' }"
      >
        <polyline points="9 6 15 12 9 18" />
      </motion.svg>
    </motion.button>
  </motion.div>
</template>

<style scoped>
.menu-item {
  width: 100%;
  margin: 0;
}

.menu-item__button {
  width: 100%;
  min-height: 3.5rem;
  display: flex;
  align-items: center;
  gap: 0.75rem;
  background: transparent;
  border: none;
  cursor: pointer;
  text-align: left;
  font-family: var(--font-family-ui);
  border-radius: 0;
  transition: background-color var(--transition-fast), color var(--transition-fast), box-shadow var(--transition-fast), transform var(--transition-fast);
}

.menu-item__button--main {
  padding: 1.15rem var(--spacing-xl);
}

.menu-item__button--sub {
  padding: 1rem var(--spacing-xl);
}

.menu-item__icon {
  width: 1.125rem;
  height: 1.125rem;
  display: flex;
  align-items: center;
  justify-content: center;
  color: color-mix(in srgb, white 72%, transparent);
  transition: color var(--transition-fast);
}

.menu-item__label {
  font-size: 0.875rem;
  font-weight: var(--font-weight-medium);
  color: color-mix(in srgb, white 92%, transparent);
  flex: 1;
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
  min-width: 1.25rem;
  height: 1.25rem;
  padding: 0 0.4rem;
  background-color: var(--color-danger);
  color: var(--color-primary-foreground);
  font-size: 0.6875rem;
  font-weight: 700;
  border-radius: 624.9375rem;
  margin: 0;
  box-shadow: 0 0.0625rem 0 rgba(0, 0, 0, 0.08);
}

.menu-item__chevron {
  margin: 0;
  color: color-mix(in srgb, white 72%, transparent);
  transform-origin: 50% 50%;
  transition: color var(--transition-fast);
}

.menu-item__button:hover {
  background-color: rgba(255, 255, 255, 0.08);
  transform: translateX(1px);
}

.menu-item--sub .menu-item__button:hover {
  transition: background-color var(--transition-fast);
  color: var(--color-surface-raised);
}

/* Has active child - parent of current page (subtle indicator) */
.menu-item--has-active-child .menu-item__button {
  background-color: rgba(255, 255, 255, 0.1);
  box-shadow: inset 3px 0 0 rgba(255, 255, 255, 0.5);
}

.menu-item--has-active-child .menu-item__label {
  color: white;
  font-weight: 500;
}

.menu-item--has-active-child .menu-item__icon,
.menu-item--has-active-child .menu-item__chevron {
  color: rgba(255, 255, 255, 0.9);
}

/* Full active state - current page (overrides has-active-child) */
.menu-item--active .menu-item__button {
  background-color: var(--color-background);
  color: var(--color-foreground);
  box-shadow: none;
  border-left: none;
}

.menu-item--active .menu-item__label,
.menu-item--active .menu-item__chevron {
  color: var(--color-foreground);
}

.menu-item--active .menu-item__icon {
  color: var(--color-foreground);
}

.menu-item--sub.menu-item--active .menu-item__label {
  color: var(--color-foreground);
  font-weight: 600;
}

.menu-item__button:focus-visible {
  outline: 0.125rem solid var(--color-focus);
  outline-offset: 0.125rem;
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
