# Frontend Styling Conventions & Patterns Guide

## Overview
This document outlines the styling patterns, conventions, and design system used throughout the frontend codebase. All styles are built on a comprehensive CSS variable system for consistency and maintainability.

---

## 1. Global Styles & Theme System

### Location of Global Styles
- **Design Tokens**: [src/assets/styles/variables.css](../frontend/src/assets/styles/variables.css)
- **Base Reset & Typography**: [src/assets/styles/base.css](../frontend/src/assets/styles/base.css)
- **Shared Component Classes**: [src/assets/styles/components.css](../frontend/src/assets/styles/components.css)
- **Layout Helpers**: [src/assets/css/main.css](../frontend/src/assets/css/main.css)

### Import Order (from main.ts)
```typescript
import './assets/styles/variables.css'   // Design tokens first
import './assets/styles/base.css'        // Reset & typography
import './assets/styles/components.css'  // Utility classes
import './assets/css/main.css'          // Layout helpers
```

---

## 2. Color Palette & Design Tokens

### Primary Color Scale
- **--color-brand-deep-forest**: #00272B (darkest, primary base)
- **--color-brand-dark-teal**: #13575E (primary hover)
- **--color-brand-mid-teal**: #3a8991 (primary active)
- **--color-brand-light-teal**: #76bcc4
- **--color-brand-ice-teal**: #c6f2f7 (accent, borders)
- **--color-brand-cta-lime**: #e0ff4f (call-to-action)

### Secondary Color Scale (Violets)
- **--color-brand-pale-lavender**: #e8d7ff
- **--color-brand-soft-violet**: #af9bcc
- **--color-brand-medium-violet**: #725699 (secondary)
- **--color-brand-deep-violet**: #402566
- **--color-brand-near-black-violet**: #1a0833

### Surface & Background Colors
- **--color-surface**: #fff9f9 (default background)
- **--color-surface-raised**: #ffffff (elevated surfaces)
- **--color-surface-muted**: #f7fbfb (input backgrounds)
- **--color-card**: #ffffff (card backgrounds with 1px border)
- **--color-background**: Alias for --color-surface

### Semantic Color Tokens
```css
/* Status Colors */
--color-success: #00272B with bg --color-success-bg: #e0ff4f
--color-warning: #1a0833 with bg --color-warning-bg: #af9bcc
--color-danger: #b42318 with hover: #8f1b13
--color-info: #13575e with bg --color-info-bg: #c6f2f7

/* Gray Scale for UI** */
--color-gray-50 through --color-gray-900 (10 steps)
--color-foreground: Uses --color-brand-deep-forest
--color-border: Uses --color-brand-ice-teal
--color-border-strong: Uses --color-brand-light-teal
```

### Feature-Specific Tokens
```css
/* IK-MAT Module */
--ik-mat-primary: var(--color-primary)
--ik-mat-bg: var(--color-info-bg)

/* IK-ALKOHOL Module */
--ik-alkohol-primary: var(--color-brand-medium-violet)
--ik-alkohol-bg: color-mix(in srgb, var(--color-brand-pale-lavender) 60%, white)

/* FELLES (Shared) */
--felles-primary: var(--color-brand-dark-teal)
--felles-bg: var(--color-info-bg)
```

---

## 3. Typography System

### Font Families
- **Body Text**: 'Hind', 'Segoe UI', system-ui, sans-serif (400, 600 weights)
- **Display/UI**: 'Montserrat', 'Segoe UI', system-ui, sans-serif (400-700 weights)

### Font Size Scale
```css
--font-size-xs: 12px
--font-size-sm: 14px
--font-size-base: 16px
--font-size-lg: 18px
--font-size-xl: 20px
--font-size-2xl: 25px
--font-size-3xl: 32px
--font-size-4xl: 40px
```

### Line Height Scale
```css
--line-height-tight: 1.15       /* For tight headings */
--line-height-heading: 1.25     /* For all headings */
--line-height-body: 1.6         /* For body text */
--line-height-relaxed: 1.7      /* For extra readable text */
```

### Font Weights
```css
--font-weight-normal: 400       /* Regular body text */
--font-weight-medium: 500       /* Slightly emphasized */
--font-weight-semibold: 600     /* Most headings */
--font-weight-bold: 700         /* Strong emphasis */
```

### Heading Styles (HTML/CSS)
- **h1**: `clamp(2rem, 2.6vw, 32px)` with letter-spacing -0.01em
- **h2**: `clamp(1.4rem, 1.9vw, 25px)` with letter-spacing -0.008em
- **h3**: `20px` with letter-spacing -0.004em
- Font-family: Display family with semibold weight and balanced text wrapping

---

## 4. Spacing & Sizing System

### Spacing Scale
```css
--spacing-xs: 4px
--spacing-sm: 8px
--spacing-md: 16px      /* Most common */
--spacing-lg: 24px      /* Section spacing */
--spacing-xl: 32px
--spacing-2xl: 48px
--spacing-3xl: 64px

/* Common layouts */
--content-max-width: 1200px
--content-padding: clamp(1rem, 2vw, 2rem)
--sidebar-width: 314px
--topbar-height: 64px
```

### Border Radius Scale
```css
--radius-sm: 4px        /* Small elements, select dropdowns */
--radius-md: 8px        /* Input fields, buttons */
--radius-lg: 16px       /* Cards, panels */
--radius-xl: 24px       /* Large modals */
```

### Touch Target
```css
--touch-target: 44px    /* Minimum for accessible buttons/inputs */
```

### Button Padding
```css
--button-padding-sm: 0.5rem 0.75rem     /* Small buttons */
--button-padding-md: 0.75rem 1rem       /* Medium buttons (default) */
--button-padding-lg: 0.9rem 1.25rem     /* Large buttons */
--input-padding: 0.75rem 1rem           /* Input fields */
```

---

## 5. Shadow & Elevation System

### Shadow Definitions
```css
--shadow-sm: 0 1px 3px rgba(0, 39, 43, 0.08), 
             0 1px 2px rgba(0, 39, 43, 0.04)   /* Subtle */

--shadow-md: 0 8px 24px rgba(0, 39, 43, 0.12), 
             0 2px 8px rgba(0, 39, 43, 0.06)   /* Cards, modals */

--shadow-lg: 0 16px 32px rgba(0, 39, 43, 0.16), 
             0 4px 12px rgba(0, 39, 43, 0.08)  /* Elevated panels */

--shadow-float: 0 18px 40px rgba(0, 39, 43, 0.16)  /* Floating elements */

--shadow-focus: 0 0 0 3px var(--color-brand-ice-teal)  /* Focus rings */
```
**Note**: All shadows use the primary color (deep-forest) at varying opacities for visual consistency.

---

## 6. Transition & Animation System

### Transition Timing
```css
--transition-fast: 0.15s        /* Quick hover states */
--transition-base: 0.2s         /* Most interactive elements (default) */
--transition-slow: 0.28s        /* Important state changes */

--ease-emphasized: cubic-bezier(0.2, 0.8, 0.2, 1)    /* Smooth */
--ease-spring: cubic-bezier(0.34, 1.56, 0.64, 1)     /* Bouncy */
```

### Common Transitions
```css
transition: background-color var(--transition-base),
            color var(--transition-base),
            border-color var(--transition-base);
```

### Loading Spinner Animation
```css
@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

.spinner {
  animation: spin 0.8s linear infinite;
}
```

---

## 7. Shared Component Classes

### Layout Containers

#### .app-surface
```css
background: linear-gradient(180deg, var(--color-card) 0%, var(--color-card-muted) 100%);
border: 1px solid var(--color-border);
border-radius: var(--radius-lg);
box-shadow: var(--shadow-sm);
```

Variants:
- `.app-surface--raised`: Box shadow upgraded to `--shadow-md`
- `.app-surface--accent`: Dark teal left border (4px)

#### .app-section
- `display: grid` with `gap: var(--spacing-md)`
- Combines sections with proper spacing

#### .app-stack
- `display: grid` with `gap: var(--spacing-lg)` (larger gaps for vertical stacking)

#### .app-toolbar
```css
display: flex;
flex-wrap: wrap;
gap: var(--spacing-sm);
align-items: center;
justify-content: space-between;
```

#### .app-grid, .app-grid--2, .app-grid--3
- Responsive grid layouts with 2 or 3 columns
- Responsive: Single column below 48rem (768px)

### Page Layout Classes

#### .view-page
- Max-width: `min(100%, 1200px)`
- Centered with auto margins
- Padding: `var(--content-padding)`
- Grid layout with `gap: var(--spacing-lg)`

#### .view-page__header
- Flexbox with space-between
- Bottom accent line (gradient from primary to CTA)

#### .section-badge
- Inline badge with uppercase, bold text
- Background: 18% CTA color mixed with card color
- Border-radius: 999px (pill-shaped)
- Typical usage: above page titles

---

## 8. Base Components

### BaseButton
**Location**: [src/shared/components/BaseButton.vue](../frontend/src/shared/components/BaseButton.vue)

#### Variants
```typescript
type: 'button' | 'submit' | 'reset'
variant: 'primary' | 'secondary' | 'danger' | 'ghost'
size: 'sm' | 'md' | 'lg'
disabled?: boolean
loading?: boolean
```

#### Styling Details

**Primary Variant**
- Background: `--color-primary` (#00272B)
- Text: `--color-primary-foreground` (white)
- Hover: `--color-primary-hover` (dark teal)
- Active: `--color-primary-active` (mid teal)
- Shadow: `--shadow-sm` on all states
- Min height: `--touch-target` (44px)

**Secondary Variant**
- Background: Transparent
- Border: 1px solid primary
- Text: Primary color
- Hover: Teal background (--color-info-bg)

**Danger Variant**
- Background: `--color-danger` (#b42318)
- Text: White
- Hover: `--color-danger-hover` (#8f1b13)

**Ghost Variant**
- Background: Transparent
- Text: Link color
- Hover: Light teal background

**Size Classes**
- `.base-button--sm`: Padding sm, font-size sm
- `.base-button--md`: Padding md, font-size base (default)
- `.base-button--lg`: Padding lg, font-size lg

**Disabled/Loading State**
- Opacity: 0.6
- Cursor: not-allowed
- Box-shadow: none
- Spinner shows during loading

### BaseInput
**Location**: [src/shared/components/BaseInput.vue](../frontend/src/shared/components/BaseInput.vue)

#### Props
```typescript
id: string
modelValue: string
label: string
type?: 'text' | 'email' | 'password' | 'number' | 'textarea'
placeholder?: string
required?: boolean
disabled?: boolean
error?: string
```

#### Label Styling
- Font: Montserrat, 13px, semibold
- Text-transform: uppercase
- Letter-spacing: 0.08em
- Red asterisk for required fields

#### Field Styling
```css
border: 1px solid var(--color-border-strong);
border-radius: var(--radius-md);
padding: var(--input-padding);
background: var(--color-surface-muted);
min-height: var(--touch-target);
box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.72);
```

#### Focus State
```css
border-color: var(--color-focus);
background: var(--color-surface-raised);
box-shadow: var(--shadow-focus);
outline: none;
```

#### Error State
- Red border: `--color-danger`
- Error message shown below field with color danger
- Error text is smaller (12px)

#### Textarea Specific
- Minimum height: 100px
- Vertical resize only

### BaseModal
**Location**: [src/shared/components/BaseModal.vue](../frontend/src/shared/components/BaseModal.vue)

#### Props
```typescript
open: boolean
title: string
```

#### Emits
```typescript
close: []
```

#### Structure
```html
<Teleport to="body">
  <div class="modal-overlay">        <!-- Full screen overlay -->
    <div class="modal">              <!-- Focused modal box -->
      <div class="modal__header">    <!-- Title + close button -->
      <div class="modal__content">   <!-- Default slot -->
      <div class="modal__footer">    <!-- Optional footer slot -->
```

#### Overlay Styling
```css
position: fixed;
inset: 0;
background: rgba(0, 39, 43, 0.58);    /* --color-overlay */
display: flex;
align-items: center;
justify-content: center;
z-index: 100;
padding: var(--spacing-lg);
```

#### Modal Box Styling
```css
background: var(--color-surface-raised);
width: 100%;
max-width: 26rem;
max-height: 90vh;
overflow-y: auto;
border-radius: var(--radius-sm);
box-shadow: var(--shadow-md);
display: flex;
flex-direction: column;
```

#### Header Styling
- Flexbox with space-between
- Bottom border: 1px solid border color
- Padding: `var(--spacing-lg)` on all sides
- Close button: 24px square, SVG icon

#### Content & Footer
- Content: Flex column with scrollable overflow
- Footer: Flexbox row, right-aligned, top border

#### Keyboard Handling
- Escape to close
- Tab focus trapping within modal
- Previous focus restored on close

#### Accessibility Features
- `role="dialog"`
- `aria-modal="true"`
- `aria-label` with title
- Close button aria-label: "Lukk" (Close in Norwegian)

### BaseBadge
**Location**: [src/shared/components/BaseBadge.vue](../frontend/src/shared/components/BaseBadge.vue)

#### Variants
- `.base-badge--success`: Lime background with dark text
- `.base-badge--warning`: Violet background
- `.base-badge--danger`: Red background with white text
- `.base-badge--info`: Teal background
- `.base-badge--neutral`: Gray background (default)

#### Styling
```css
display: inline-flex;
padding: 0.25rem 0.5rem;
border-radius: 999px;          /* Pill-shaped */
border: 1px solid transparent;
font-size: 11px (0.6875rem);
font-weight: bold;
text-transform: uppercase;
letter-spacing: 0.08em;
```

### BaseSpinner
- Size variants: sm, md, lg
- 2px border with transparent top
- Rotates 360° in 0.8s linear animation
- Uses currentColor for dynamic coloring

---

## 9. Modal & Overlay Patterns

### Standard Modal Patterns

#### Pattern 1: BaseModal (Recommended)
Used for confirmations, simple forms, and dialogs.

```vue
<template>
  <BaseModal :open="isOpen" title="Dialog Title" @close="handleClose">
    <p>Modal content here</p>
    
    <template #footer>
      <button class="modal-btn modal-btn--ghost" @click="handleClose">
        Avbryt
      </button>
      <button class="modal-btn" @click="handleConfirm">
        Bekreft
      </button>
    </template>
  </BaseModal>
</template>
```

#### Pattern 2: Custom Dialog (DeviationReportForm)
Used for complex forms with custom styling needs.

```vue
<template>
  <Teleport to="body">
    <div v-if="open" class="overlay" @click.self="cancel">
      <div class="dialog" role="dialog" aria-modal="true">
        <div class="dialog__header">
          <h2>Title</h2>
          <button class="dialog__close" @click="cancel">×</button>
        </div>
        <div class="dialog__body">
          <!-- Form content -->
        </div>
        <div class="dialog__footer">
          <!-- Footer actions -->
        </div>
      </div>
    </div>
  </Teleport>
</template>
```

### Modal Button Classes

#### .modal-btn (Primary)
```css
background: var(--ik-alkohol-primary) or --color-primary;
color: #fff;
border: 1px solid primary;
padding: 0.45rem 0.8rem;
border-radius: var(--radius-sm);
font-size: var(--font-size-sm);
min-height: 36px;
```

#### .modal-btn--ghost
```css
background: var(--color-card);
color: var(--color-gray-700);
border: 1px solid var(--color-border);
```

Both have hover opacity adjustment (-10%) and disabled state (opacity 0.6).

---

## 10. Form Styling Patterns

### Form Structure Pattern

#### Simple Field Layout
```vue
<div class="field">
  <label class="label" for="field-id">Field Label <span class="req">*</span></label>
  <input 
    id="field-id" 
    type="text"
    class="input"
    :class="{ 'input--error': hasError }"
  />
  <p v-if="hasError" class="field__error">Error message</p>
</div>
```

#### Two-Column Row
```vue
<div class="field-row">
  <div class="field"><!-- field 1 --></div>
  <div class="field"><!-- field 2 --></div>
</div>
```

### Form Section Classes

#### .field
```css
display: flex;
flex-direction: column;
gap: 0.5rem;
```

#### .label
```css
font-family: var(--font-family-ui);
font-size: 13px (0.8125rem);
font-weight: 600;
text-transform: uppercase;
letter-spacing: 0.08em;
color: var(--color-foreground);
```

#### .input, .field select, .field input, .field textarea
```css
min-height: 44px;
padding: 0.75rem 1rem;
border: 1px solid var(--color-border);
border-radius: var(--radius-md);
background: var(--color-surface-muted);
font-family: var(--font-family-ui);
font-size: var(--font-size-sm);
box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.72);
transition: all var(--transition-base);
```

#### Focus States
```css
outline: none;
border-color: var(--color-focus);
background: var(--color-surface-raised);
box-shadow: var(--shadow-focus);
```

#### Error States
```css
border-color: var(--color-danger);  /* Red */
```

#### Select Dropdowns
- Custom appearance styling with SVG arrow
- Padding-right increased for arrow space (2rem)
- Background-image: SVG chevron icon

### Form Field Patterns from Views

#### From TemperatureView
```css
.modal-form {
  /* standard form in modal */
}

.modal-form__row {
  /* Two-column row layout */
}

.modal-form__error {
  color: var(--color-danger);
  font-size: var(--font-size-sm);
}

.modal-form__hint {
  font-size: var(--font-size-sm);
  color: var(--color-gray-600);
  padding: 0.75rem 1rem;
  background: var(--color-gray-50);
  border-radius: var(--radius-md);
}
```

#### From CertificationsView
```css
.cert-form label {
  display: grid;
  gap: 0.3rem;
}

.form-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 0.75rem;
}
```

#### Segment Button Control (Deviation Report Form)
```css
.segment {
  display: flex;
  gap: 0.5rem;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  background: var(--color-card);
  padding: 0.35rem;
}

.segment__btn {
  flex: 1;
  padding: 0.5rem 1rem;
  border: none;
  background: transparent;
  border-radius: var(--radius-sm);
  cursor: pointer;
  transition: all var(--transition-base);
}

.segment__btn--active {
  background: var(--color-primary);
  color: white;
}
```

---

## 11. Button Style Classes & Patterns

### Standard Button Variants

#### Primary Buttons
```css
.btn-primary {
  background: #00272B;
  color: #ffffff;
  border: none;
  min-height: 36px;
  padding: 0.4rem 1.25rem;
  font-weight: 600;
}

.btn-primary:hover:not(:disabled) {
  background: #13575E;
  opacity: 0.95;
  transform: translateY(-1px);
}
```

#### Ghost Buttons (Outline Style)
```css
.btn-ghost {
  background: transparent;
  border: 1px solid var(--color-border);
  color: var(--color-gray-700);
  min-height: 36px;
  padding: 0.4rem 1rem;
}

.btn-ghost:hover {
  background: var(--color-gray-50);
  border-color: var(--color-gray-400);
}
```

#### Action Buttons (Table/List)
```css
.action-btn {
  background: var(--color-primary);
  color: white;
  min-height: 36px;
  padding: 0.4rem 1.25rem;
  font-weight: 600;
  border: none;
  border-radius: var(--radius-md);
}

.action-btn--danger {
  background: #FF6666;
}
```

#### Row Buttons (Compact)
```css
.row-btn {
  min-height: 32px;
  padding: 0.35rem 0.75rem;
  border: 1px solid var(--color-border);
  background: var(--color-card);
  font-size: var(--font-size-xs);
  font-weight: 600;
}
```

#### Status Pills (Read-Only Badges)
```css
.status-pill {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 999px;
  padding: 0.35rem 0.75rem;
  min-height: 32px;
  font-size: 11px (0.6875rem);
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.04em;
}

.status-pill--good {
  background: #E0FF4F;
  color: #00272B;
}

.status-pill--danger {
  background: #FF6666;
  color: #FFFFFF;
}

.status-pill--idle {
  background: var(--color-gray-100);
  color: var(--color-gray-500);
}
```

#### Common Focus State
```css
:focus-visible {
  outline: 2px solid #3A8991;
  outline-offset: 2px;
  box-shadow: 0 0 0 3px #C6F2F7;   /* Focus ring */
}
```

---

## 12. Status & Alert Components

### Alert Banners
```css
.alert-banner {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  padding: 0.75rem 1rem;
  border: 1px solid color-mix(in srgb, var(--color-danger) 70%, var(--color-border));
  background: var(--color-danger);
  color: white;
  border-radius: var(--radius-md);
  font-size: var(--font-size-sm);
}
```

### Error Messages
```css
.field__error,
.form-error {
  font-size: var(--font-size-sm);
  color: var(--color-danger);
}

.form-error {
  padding: 0.75rem 1rem;
  background: #FF6666;
  color: #FFFFFF;
  border: 1px solid color-mix(in srgb, #FF6666 70%, var(--color-border));
  border-radius: var(--radius-md);
}
```

### Info Boxes
```css
.info-box {
  padding: 14px;
  border: 1px solid color-mix(in srgb, var(--color-brand-soft-violet) 18%, var(--color-border));
  background: color-mix(in srgb, var(--color-brand-pale-lavender) 46%, var(--color-card));
  border-radius: var(--radius-lg);
}
```

### Empty States
```css
.empty-state {
  text-align: center;
  padding: 2rem;
  color: var(--color-gray-600);
  background: var(--color-gray-50);
  border-radius: var(--radius-md);
}

.empty-state--compact {
  padding: 1rem;
  margin: 0;
}
```

---

## 13. Data Table & List Styling

### Table Headers
```css
th {
  font-size: var(--font-size-xs);
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.06em;
  color: var(--color-gray-600);
  background: var(--color-gray-50);
  padding: 0.75rem 1rem;
  border-bottom: 1px solid var(--color-border);
}
```

### Table Rows
```css
td {
  text-align: left;
  padding: 0.75rem 1rem;
  border-bottom: 1px solid var(--color-gray-100);
  vertical-align: middle;
}

tr:last-child td {
  border-bottom: none;
}

.tr--alert {
  background: color-mix(in srgb, var(--color-danger-bg) 60%, var(--color-card));
}
```

### Table Cells
```css
.cell-title {
  font-size: var(--font-size-sm);
  font-weight: 500;
  color: var(--color-foreground);
  margin: 0;
}

.cell-sub {
  font-size: var(--font-size-xs);
  color: var(--color-gray-500);
  margin: 0.1rem 0 0;
}
```

### List Card Pattern
```css
.card {
  background: #FFFFFF;
  border: 1px solid var(--color-border);
  border-left: 4px solid #3A8991;
  border-radius: var(--radius-md);
  padding: 1rem;
  box-shadow: var(--shadow-sm);
  transition: all var(--transition-base);
}

.card:hover {
  box-shadow: var(--shadow-md);
}

.card--alert {
  border-left-color: #FF6666;
  background: color-mix(in srgb, #FF6666 5%, #FFFFFF);
}
```

---

## 14. Icon & Component Library Usage

### Icons Used
- **No external icon library** - inline SVG icons used directly in components
- Close button: `<svg>` with two crossing lines (manual SVG)
- Dropdown arrow: SVG in data-URI format
- Other icons: Added as needed inline

### Example Close Button SVG
```vue
<svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
  <line x1="18" y1="6" x2="6" y2="18"></line>
  <line x1="6" y1="6" x2="18" y2="18"></line>
</svg>
```

### Component Libraries
- **Vue 3** - Latest Composition API
- **Pinia** - State management
- **@vueuse/motion** - Animation plugin (applied to app)
- **No UI framework** - All components custom-built

---

## 15. Responsive Design Patterns

### Main Breakpoint
```css
@media (max-width: 48rem) {  /* 768px */
  /* Mobile-specific styles */
}
```

### Common Responsive Patterns

#### Grid to Single Column
```css
.app-grid--2,
.app-grid--3 {
  grid-template-columns: 1fr;
}
```

#### Hidden Columns in Tables
```css
@media (max-width: 48rem) {
  th:nth-child(3), td:nth-child(3),
  th:nth-child(4), td:nth-child(4) {
    display: none;
  }
}
```

#### Full-Width Buttons
```css
@media (max-width: 48rem) {
  .log-btn {
    width: 100%;
    text-align: center;
  }
}
```

#### Responsive Font Sizes
```css
font-size: clamp(1.5rem, 2.4vw, 2rem);
/* Minimum: 1.5rem, Preferred: 2.4vw, Maximum: 2rem */
```

#### Responsive Padding
```css
padding: clamp(1.25rem, 2vw, 2rem);
```

---

## 16. Accessibility Patterns

### Focus States
```css
*:focus-visible {
  outline: 2px solid var(--color-focus);  /* #3A8991 */
  outline-offset: 2px;
}
```

### Skip Links & Screen Reader Only
```css
.sr-only {
  position: absolute;
  width: 1px;
  height: 1px;
  padding: 0;
  margin: -1px;
  overflow: hidden;
  clip: rect(0, 0, 0, 0);
  white-space: nowrap;
  border: 0;
}
```

### Form Validation
```vue
<input 
  aria-invalid="true"
  aria-describedby="field-error"
/>

<span id="field-error" role="alert">
  Error message
</span>
```

### Touch Targets
All interactive elements maintain minimum 44px height:
```css
min-height: var(--touch-target);  /* 44px */
```

### ARIA Labels
```vue
<button aria-label="Lukk">×</button>
<div role="dialog" aria-modal="true" aria-label="Dialog Title">
```

---

## 17. Animation & Transition Guidelines

### Standard Transition Usage
```css
transition: background-color var(--transition-base),
            color var(--transition-base),
            border-color var(--transition-base),
            box-shadow var(--transition-base);
```

### Transform Effects
```css
/* Button press effect */
.button:active {
  transform: scale(0.98);
}

/* Button hover effect */
.button:hover {
  transform: translateY(-1px);
}
```

### Loading Spinner
```css
@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

.spinner {
  border: 2px solid currentColor;
  border-top-color: transparent;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}
```

---

## 18. Best Practices & Conventions

### 1. **Always Use CSS Variables**
- Never hardcode colors - use design tokens
- Use semantic tokens (`--color-primary`) not base tokens (`--color-brand-deep-forest`)

### 2. **Maintain Spacing Consistency**
- Use spacing scale exclusively
- Avoid arbitrary pixel values

### 3. **Focus Styles**
- Always provide 2px outline with 2px offset for focus states
- Never remove outline without replacement

### 4. **Component Structure**
- Follow BEM-like naming: `.block__element--modifier`
- Use scoped styles in `.vue` files
- Organize by component, not by property

### 5. **Colors for Status**
- Green/CTA (#E0FF4F): Success, good status
- Red (#FF6666): Danger, alerts, failures
- Violet tones: Secondary actions, warnings
- Gray: Disabled, muted, neutral

### 6. **Form Fields**
- Always pair with labels (use `<label for="">`)
- Show required indicator with red asterisk
- Display error messages below field
- Minimum height: 44px (touch-target)

### 7. **Modals**
- Use Teleport to body for correct stacking context
- Implement keyboard handling (Escape, Tab focus trap)
- Include close button with accessible label
- Overlay opacity: 0.58 (--color-overlay)

### 8. **Responsive Images**
```css
img {
  max-width: 100%;
  height: auto;
  display: block;  /* Remove inline spacing */
}
```

### 9. **Line-Height for Readability**
- Body text: 1.6 (line-height-body)
- Headings: 1.25 (line-height-heading)
- Extra readable: 1.7 (line-height-relaxed)

### 10. **Typography Hierarchy**
- Use font families: Hind for body, Montserrat for UI/headings
- Letter spacing for uppercase: 0.04-0.09em
- Heading letter-spacing: negative (-0.01em for h1)

---

## 19. Common Styling Scenarios

### Creating a Status Box
```vue
<div class="status-box status-box--valid">
  <p class="status-box__label">Gyldig</p>
  <p class="status-box__value">5</p>
  <p class="status-box__meta">av 10 total</p>
</div>

<style scoped>
.status-box {
  border: 1px solid var(--color-border);
  border-left: 4px solid var(--color-cta);
  border-radius: var(--radius-lg);
  padding: var(--spacing-md);
  background: var(--color-card);
}

.status-box__label {
  font-size: 0.6875rem;
  text-transform: uppercase;
  color: var(--color-gray-600);
}

.status-box__value {
  font-size: 1.625rem;
  font-weight: 700;
}
</style>
```

### Creating a Modal Form
```vue
<BaseModal :open="true" title="Create Item" @close="close">
  <form @submit.prevent="submit">
    <div class="field">
      <label for="name">Name</label>
      <input id="name" v-model="form.name" type="text" required />
      <p v-if="errors.name" class="field__error">{{ errors.name }}</p>
    </div>
  </form>
  
  <template #footer>
    <button type="button" class="modal-btn modal-btn--ghost" @click="close">
      Avbryt
    </button>
    <button type="button" class="modal-btn" @click="submit">
      Opprett
    </button>
  </template>
</BaseModal>
```

### Creating a Status Indicator
```vue
<span 
  class="status-pill" 
  :class="item.status === 'good' ? 'status-pill--good' : 'status-pill--danger'"
>
  {{ item.status }}
</span>

<style scoped>
.status-pill {
  display: inline-flex;
  justify-content: center;
  align-items: center;
  border-radius: 999px;
  padding: 0.35rem 0.75rem;
  min-height: 32px;
  font-size: 0.6875rem;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.04em;
}

.status-pill--good {
  background: var(--color-success-bg);
  color: var(--color-success);
}

.status-pill--danger {
  background: var(--color-danger-bg);
  color: var(--color-danger);
}
</style>
```

---

## 20. Quick Reference: Common CSS Patterns

### Quick Color Reference
```css
/* Primary Actions */
background: var(--color-primary);           /* Deep forest #00272B */
background: var(--color-primary-hover);     /* Dark teal on hover */

/* Secondary Actions */
background: var(--color-secondary);         /* Medium violet */

/* Backgrounds */
background: var(--color-card);              /* White (#ffffff) */
background: var(--color-surface);           /* Soft white (#fff9f9) */

/* Borders & Accents */
border-color: var(--color-border);          /* Ice teal */
border-color: var(--color-border-strong);   /* Light teal */

/* Status */
color: var(--color-success);                /* Success state */
color: var(--color-danger);                 /* Error/danger */
color: var(--color-warning);                /* Warning */
```

### Quick Spacing Reference
```css
gap: var(--spacing-md);          /* Most common: 16px */
gap: var(--spacing-lg);          /* Sections: 24px */
padding: var(--spacing-md);      /* Content padding: 16px */
margin-bottom: var(--spacing-lg); /* Between sections: 24px */
```

### Quick Border/Radius Reference
```css
border-radius: var(--radius-sm); /* 4px - small elements */
border-radius: var(--radius-md); /* 8px - inputs, buttons */
border-radius: var(--radius-lg); /* 16px - cards, panels */
border-radius: var(--radius-xl); /* 24px - large modals */
border-radius: 999px;            /* Pill-shaped badges */
```

---

## 21. Module-Specific Color Themes

### IK-MAT Module
- **Primary**: Teal colors from main palette
- **Accent Background**: `--ik-mat-bg` (ice-teal background)
- **Usage**: Used for buttons, headers, status indicators in temperature/measurement views

### IK-ALKOHOL Module
- **Primary**: Medium violet (#725699)
- **Accent Background**: Lavender tones mixed with white
- **Usage**: Used for certification management, compliance tracking
- **Custom Buttons**: Often use `var(--ik-alkohol-primary)` for primary actions

### FELLES (Shared) Module
- **Primary**: Dark teal
- **Background**: Ice-teal background
- **Usage**: Shared administrative features

---

## 22. Testing the Styling

### Common Responsive Tests
- Test at 375px (mobile)
- Test at 768px (breakpoint)
- Test at 1200px (desktop)
- Test focus states (Tab key)
- Test color contrast with WCAG checker

### Component Testing Checklist
- [ ] Focus visible outline appears
- [ ] Touch targets are 44px minimum
- [ ] Colors meet WCAG AA contrast
- [ ] Spacing uses design tokens
- [ ] Responsive behavior works
- [ ] Disabled states are clear
- [ ] Error states are visible
- [ ] Animations are smooth

---

## 23. Common Issues & Solutions

### Issue: Focus styles not showing
**Solution**: Check for `outline: none` in global styles. Use `*:focus-visible` selector.

### Issue: Input fields not styled consistently
**Solution**: Ensure all inputs use `.input` class or apply consistent border/padding/height using tokens.

### Issue: Modal appearing behind other content
**Solution**: Verify modal is Teleported to body and has `z-index: 100`.

### Issue: Text wrapping inconsistently
**Solution**: Use `text-wrap: balance` for headings and `text-wrap: pretty` for body text.

### Issue: Colors look different in different views
**Solution**: Always use CSS variables, never hardcode hex values. Check for view-specific color overrides.

---

## 24. Resources & Files

### Key Files to Reference
- Design Tokens: [variables.css](../frontend/src/assets/styles/variables.css)
- Base Styles: [base.css](../frontend/src/assets/styles/base.css)
- Shared Classes: [components.css](../frontend/src/assets/styles/components.css)
- Layout Helpers: [main.css](../frontend/src/assets/css/main.css)

### Component References
- [BaseButton.vue](../frontend/src/shared/components/BaseButton.vue)
- [BaseInput.vue](../frontend/src/shared/components/BaseInput.vue)
- [BaseModal.vue](../frontend/src/shared/components/BaseModal.vue)
- [BaseBadge.vue](../frontend/src/shared/components/BaseBadge.vue)

### Example Views
- [TemperatureView.vue](../frontend/src/features/ik-mat/views/TemperatureView.vue) - Complex data display
- [CertificationsView.vue](../frontend/src/features/ik-alkohol/views/CertificationsView.vue) - Table/card layouts
- [LoginView.vue](../frontend/src/features/auth/views/LoginView.vue) - Form styling

---

**Last Updated**: April 2026
**Created For**: IDATT2105 Semester Project 2026
