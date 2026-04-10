<script setup lang="ts">
import { motion } from 'motion-v'

const props = withDefaults(defineProps<{
  variant: 'main' | 'sub'
  label: string
  active?: boolean
  badge?: number
  icon?: string
  expanded?: boolean
}>(), {
  active: false,
  badge: 0,
  icon: '',
  expanded: false,
})

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
      <span v-if="props.variant === 'main'" class="menu-item__icon" v-html="props.icon" />
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
  min-height: 44px;
  display: flex;
  align-items: center;
  gap: 10px;
  background: transparent;
  border: 1px solid transparent;
  border-left: 4px solid transparent;
  cursor: pointer;
  text-align: left;
  font-family: var(--font-family-ui);
  border-radius: 0;
  transition: background-color var(--transition-fast), border-color var(--transition-fast) ease, color var(--transition-fast);
}

.menu-item__button--main {
  padding: 12px 16px;
}

.menu-item__button--sub {
  padding: 10px 16px 10px 44px;
}

.menu-item__icon {
  width: 18px;
  height: 18px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: color-mix(in srgb, white 72%, transparent);
}

.menu-item__label {
  font-size: 13px;
  font-weight: 500;
  color: color-mix(in srgb, white 92%, transparent);
}

.menu-item--main .menu-item__label {
  font-size: 12px;
  font-weight: 600;
  letter-spacing: 0.07em;
  text-transform: uppercase;
}

.menu-item__badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 18px;
  height: 18px;
  padding: 0 6px;
  background-color: var(--color-danger);
  color: var(--color-primary-foreground);
  font-size: 11px;
  font-weight: 700;
  border-radius: 9999px;
  margin-left: auto;
  box-shadow: 0 1px 0 rgba(0, 0, 0, 0.18);
}

.menu-item__chevron {
  margin-left: 6px;
  color: color-mix(in srgb, white 72%, transparent);
  transform: rotate(-90deg);
  transition: transform var(--transition-base) ease, color var(--transition-fast);
}

.menu-item__chevron--open {
  transform: rotate(0deg);
}

.menu-item__button:hover {
  background-color: rgba(255, 255, 255, 0.08);
}

.menu-item--sub .menu-item__button:hover {
  transition: background-color var(--transition-fast);
  color: var(--color-surface-raised);
}

.menu-item--active .menu-item__button {
  background-color: rgba(255, 255, 255, 0.08);
  border-left-color: var(--color-cta);
  border-color: transparent;
}

.menu-item--active .menu-item__label,
.menu-item--active .menu-item__icon,
.menu-item--active .menu-item__chevron {
  color: var(--color-surface-raised);
}

.menu-item--sub.menu-item--active .menu-item__label {
  color: var(--color-cta);
  font-weight: 600;
}

.menu-item__button:focus-visible {
  outline: 2px solid var(--color-cta);
  outline-offset: -2px;
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
