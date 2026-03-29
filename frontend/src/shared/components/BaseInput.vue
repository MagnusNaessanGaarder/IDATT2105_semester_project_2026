<!--
  BaseInput - Gjenbrukbar input-komponent
  
  Brukes for alle tekst-inputs i skjemaer.
  Støtter typer: text, email, password, number, textarea
  Har innebygd validering, error-visning og WCAG-tilgjengelighet
  
  Eksempel:
  <BaseInput 
    id="email" 
    v-model="email" 
    label="E-post" 
    type="email"
    :error="errors.email"
    required 
  />
-->
<script setup lang="ts">
interface Props {
  id: string
  modelValue: string
  label: string
  type?: 'text' | 'email' | 'password' | 'number' | 'textarea'
  placeholder?: string
  required?: boolean
  disabled?: boolean
  error?: string
}

const props = withDefaults(defineProps<Props>(), {
  type: 'text',
  placeholder: '',
  required: false,
  disabled: false,
  error: '',
})

const emit = defineEmits<{
  'update:modelValue': [value: string]
}>()

const handleInput = (event: Event) => {
  const target = event.target as HTMLInputElement | HTMLTextAreaElement
  emit('update:modelValue', target.value)
}
</script>

<template>
  <div class="base-input">
    <label :for="id" class="base-input__label">
      {{ label }}
      <span v-if="required" class="base-input__required">*</span>
    </label>
    
    <textarea
      v-if="type === 'textarea'"
      :id="id"
      :value="modelValue"
      :placeholder="placeholder"
      :required="required"
      :disabled="disabled"
      :aria-invalid="!!error"
      :aria-describedby="error ? `${id}-error` : undefined"
      class="base-input__field"
      :class="{ 'base-input__field--error': error }"
      @input="handleInput"
    />
    
    <input
      v-else
      :id="id"
      :type="type"
      :value="modelValue"
      :placeholder="placeholder"
      :required="required"
      :disabled="disabled"
      :aria-invalid="!!error"
      :aria-describedby="error ? `${id}-error` : undefined"
      class="base-input__field"
      :class="{ 'base-input__field--error': error }"
      @input="handleInput"
    />
    
    <span
      v-if="error"
      :id="`${id}-error`"
      class="base-input__error"
      role="alert"
    >
      {{ error }}
    </span>
  </div>
</template>

<style scoped>
.base-input {
  display: flex;
  flex-direction: column;
  gap: 0.375rem;
}

.base-input__label {
  font-size: 0.875rem;
  font-weight: 500;
  color: var(--color-foreground);
}

.base-input__required {
  color: var(--color-danger);
}

.base-input__field {
  padding: 0.625rem 0.75rem;
  font-size: 1rem;
  border: 0.0625rem solid var(--color-border);
  background: white;
  transition: border-color 0.15s;
}

.base-input__field:focus {
  outline: none;
  border-color: var(--color-accent);
}

.base-input__field--error {
  border-color: var(--color-danger);
}

.base-input__field:disabled {
  background: var(--color-gray-100);
  cursor: not-allowed;
}

.base-input__error {
  font-size: 0.875rem;
  color: var(--color-danger);
}

textarea.base-input__field {
  min-height: 6.25rem;
  resize: vertical;
}
</style>