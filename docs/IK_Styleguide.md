# Intern Kontroll (IK) — Brand & UI Styleguide
**Version 1.0 — April 2026**

---

## 1. Introduction & Brand Philosophy

Intern Kontroll (IK) is a professional digital compliance platform for the food and beverage industry, built to simplify and digitise internal control processes relating to food safety (IK-Mat) and responsible alcohol service (IK-Alkohol). The platform serves restaurant owners, managers, and staff who need to meet rigorous regulatory standards from Norwegian and international health and safety authorities.

The brand identity must communicate four core qualities simultaneously: **openness**, **formality**, **cleanliness**, and **professionalism**. Every visual and typographic decision must be evaluated against these four pillars. The product is a tool of accountability — it exists to replace ambiguity with precision. Its design language must reflect exactly that: calm authority, structured clarity, and quiet confidence. There is no place for decorative excess or playful irreverence. Equally, the interface must never feel cold or bureaucratic to the point of alienating the everyday staff member who uses it on a tablet in a busy kitchen. The tension between professional authority and accessible usability is the defining creative challenge of the IK visual identity.

The palette and type choices provided by the client are not decorative choices — they carry strategic meaning. The deep teal greens root the brand in nature, freshness, and biological safety. The purple-violet family signals structure, order, and institutional trust. The accent lime (#E0FF4F) and coral (#FF6666) are operational signals, not aesthetic flourishes — they exist to convey status, urgency, and action. This guide codifies exactly when and how each element may appear.

---

## 2. Colour System

### 2.1 Primary Palette — Teal/Green Scale

This is the dominant brand colour family. It anchors all primary UI surfaces, navigation, headers, and brand-critical elements.

**#00272B — Deep Forest**
The primary brand colour. Used for primary navigation backgrounds, the application shell, page-level headers, primary CTA buttons, and the logotype. This colour commands trust and seriousness. It should be the dominant dark presence on any screen. On white surfaces it reads as near-black with warmth. Do not use this colour as body text on dark surfaces — reserve it for high-authority contexts.

**#13575E — Dark Teal**
The secondary brand colour. Used for section headers, sidebar navigation items in their default state, active state borders, and card header bars. Slightly lighter than Deep Forest, it provides depth layering without breaking the teal family coherence. Useful for hover states on Deep Forest elements.

**#3A8991 — Mid Teal**
A working, mid-tone teal. Used for secondary interactive elements, icon fills in active states, horizontal divider lines in branded contexts, and progress indicators. It is the "action colour" within the teal family — it signals that something is interactive or in motion.

**#76BCC4 — Light Teal**
Used for informational highlights, selected row backgrounds on data tables, tag/badge backgrounds for neutral status indicators, and subtle hover fills on list items. Never use this as a text colour on white — contrast is insufficient. It functions as a background tone only.

**#C6F2F7 — Ice Teal**
The lightest teal. Deployed as a very light background wash for information panels, input field focus rings, and notification toasts of informational character. Use sparingly — it is a supportive surface tone, not a structural element.

### 2.2 Secondary Palette — Purple/Violet Scale

This family is the structural counterpart to the teal scale. Where teal represents life, food, freshness, and environmental safety, purple represents law, procedure, authority, and system structure. It is the "regulatory" colour family — appearing in contexts where formal process, roles, permissions, documentation, and compliance hierarchy are communicated.

**#E8D7FF — Pale Lavender**
The lightest violet. Used as background fills for role-based UI segments (e.g., manager-only views), subtle differentiation of compliance document sections, and as a soft container background for modal dialogs. It pairs well with white text areas and teal headers.

**#AF9BCC — Soft Violet**
Used for secondary navigation labels relating to alcohol compliance (IK-Alkohol) modules, tag backgrounds for "pending review" states, and subtitle text in compliance report headers. Provides clear visual distinction from the teal system without competing with it.

**#725699 — Medium Violet**
A confident mid-tone purple. Used for IK-Alkohol module primary accents, section headings in alcohol compliance documentation, secondary button fills in alcohol-module contexts, and role badges for "Manager" or "Administrator" user levels. Do not use as a primary text colour on backgrounds lighter than #402566.

**#402566 — Deep Violet**
Used for high-authority labels in the alcohol compliance system, such as certification statuses, regulatory deadlines, and admin-only controls. Also appropriate as a background for premium or supervisor-level interface sections. Pairs with white text exclusively.

**#1A0833 — Near-Black Violet**
The deepest violet. Used sparingly and only for maximum authority contexts: final audit confirmations, submission-locked states, critical compliance warnings in IK-Alkohol contexts. Not a general-purpose dark colour — Deep Forest (#00272B) handles that role. This is a punctuation colour that signals irreversibility or regulatory finality.

### 2.3 Neutral Palette

**#FFF9F9 — Off-White**
The primary background colour for all main content areas. Slightly warm, it prevents the pure-white harshness that would feel clinical. All cards, panels, modals, and content containers sit against this surface. It is the default page background.

**#FFFFFF — Pure White**
Used for card surfaces that must "lift" from the #FFF9F9 background, form input fields, modal interiors, and data table row backgrounds. The distinction between #FFF9F9 and #FFFFFF creates a natural depth hierarchy without requiring heavy shadows.

**#00272B at 100% opacity (as near-black text)** — see Text section for usage.

**Black (#000000 or near-black rendering of #00272B)**
The colour palette shows #00272B rendered in both a white context and a black swatch. The implication is that the brand dark operates across a spectrum — #00272B serves as the "brand black." True black (#000000) is reserved for system-level text, dense data tables, and printed export contexts only.

### 2.4 Accent / CTA Colours

These two colours are operational accents. They are not decorative and must not appear in branding, navigation, or large background surfaces. The system should follow a 60-30-10 balance: 60% neutral surfaces, 30% teal or violet structure, and 10% reserved for accent and call-to-action emphasis.

**#E0FF4F — Signal Lime (Accent / CTA)**
Used for primary call-to-action highlights, positive completion states, confirmation badges, and other moments where the interface needs a sharp, intentional emphasis. The lime yellow is high-visibility and energetic — it draws the eye immediately. Because of this power, it must be used with restraint. Never use it for decorative purposes. Never use it as a background for large content areas. It is an accent signal, not a surface.

**#FF6666 — Alert Coral (Accent / Error)**
Used for overdue tasks, deviation alerts, failed compliance checks, missing documentation warnings, and form validation errors. This is the primary alert colour for the system. It carries significant semantic weight. Like Signal Lime, it must never appear in branding or navigation contexts. It is a functional accent only. Use it when the user must pay attention or take corrective action.

### 2.5 Colour Relationships and Module Identity

The two top-level services are visually differentiated through deliberate colour leadership:

**IK-Mat (Food Compliance):** The teal palette is the primary identity. Deep Forest and Mid Teal lead all IK-Mat navigation, headers, and branded elements. Purple is secondary and structural.

**IK-Alkohol (Alcohol Compliance):** Medium Violet (#725699) and Deep Violet (#402566) take greater prominence for module-specific navigation and headers. The teal palette is still present but supports rather than leads. This visual distinction helps users orient themselves instantly within the multi-module platform.

---

## 3. Typography

### 3.1 The Type System

The client has specified two typefaces: **Montserrat** for headings and **Hind** for body text. This is an intentional and strategic pairing. Montserrat is geometric, confident, and authoritative — it carries the institutional weight appropriate for headers, labels, and UI hierarchy. Hind is a humanist sans-serif with excellent legibility at small sizes, designed with multi-script considerations that speak to global usability and clarity of reading. Together they establish a system that is formal without being sterile, and readable without being casual.

### 3.2 Montserrat — Display & Heading Font

Montserrat carries all primary typographic hierarchy. It is the voice of the institution — the font that a user reads when IK is making a statement, establishing a section, labelling an action, or asserting a category.

**Weights in use:**
- **Bold (700):** Primary page headings (H1), primary navigation labels, key data labels in dashboards, button text for primary CTAs.
- **SemiBold (600):** Secondary headings (H2, H3), card titles, modal headers, form section labels.
- **Medium (500):** Tertiary headings (H4), sub-navigation labels, active tab labels.
- **Regular (400):** Caption labels, metadata labels, secondary informational text in non-prose contexts (e.g., table column headers).

**Do not use Montserrat for body text, long-form content, or instructional paragraphs.** Its geometric nature reduces legibility at sustained reading length and small sizes. It is a headline and UI-label font, not a reading font.

**Letter-spacing:** Apply a slight positive tracking of +0.02em to Montserrat Bold uppercase labels to improve readability and reinforce the institutional register. For sentence-case headings, no additional tracking is required.

**Text transform:** Section labels, navigation items, and status badges use uppercase Montserrat SemiBold. All other uses are sentence case. Never apply uppercase to long strings (more than 4 words) — it reduces legibility and creates an aggressive tone.

### 3.3 Hind — Body / Prose Font

Hind handles all reading-length content: instructions, descriptions, compliance notes, checklist item text, notification messages, system feedback, report body text, and documentation.

**Weights in use:**
- **SemiBold (600):** Emphasis within body text (used sparingly, for critical warnings within prose).
- **Regular (400):** All default body text.
- **Light (300):** Supplementary text, timestamps, metadata below cards, breadcrumb trails.

**Line height:** Body text at Regular weight should use a line-height of 1.6. This openness is non-negotiable — compliance content requires careful reading, and cramped line-height increases error rates.

**Paragraph width:** Constrain body text line length to a maximum of 70 characters (approximately 40em at 16px base). Long lines impair readability and the quality of the application's professional impression is partially judged by this discipline.

### 3.4 Type Scale

The type scale is built on a modular ratio of 1.25 (Major Third), which produces clean, proportional jumps that feel structured and stable — not playful or expressive.

| Level | Role | Font | Weight | Size |
|---|---|---|---|---|
| Display | Hero/landing headings | Montserrat | Bold 700 | 40px / 2.5rem |
| H1 | Page titles | Montserrat | Bold 700 | 32px / 2rem |
| H2 | Section headings | Montserrat | SemiBold 600 | 25px / 1.56rem |
| H3 | Sub-section / card titles | Montserrat | SemiBold 600 | 20px / 1.25rem |
| H4 | Group labels | Montserrat | Medium 500 | 16px / 1rem |
| Body Large | Lead paragraphs | Hind | Regular 400 | 18px / 1.125rem |
| Body | Default prose | Hind | Regular 400 | 16px / 1rem |
| Body Small | Supplementary text | Hind | Light 300 | 14px / 0.875rem |
| Label | Form labels, UI labels | Montserrat | SemiBold 600 | 13px / 0.8125rem |
| Caption | Timestamps, metadata | Hind | Light 300 | 12px / 0.75rem |
| Badge/Tag | Status indicators | Montserrat | Bold 700 | 11px / 0.6875rem |

### 3.5 Colour in Typography

**Primary text:** #00272B (Deep Forest) on #FFF9F9 backgrounds. This is not black — the deep teal-black creates a warmer, more considered read than pure black and ties body text to the brand identity.

**Secondary text:** #13575E at 80% opacity, for supplementary labels, timestamps, and metadata.

**Inverted text (on dark backgrounds):** Pure white (#FFFFFF) on Deep Forest, Dark Teal, Deep Violet, and Near-Black Violet backgrounds.

**Interactive text / links:** #3A8991 (Mid Teal). Underline on hover. Never use #E0FF4F or #FF6666 as link colours.

**Disabled text:** #00272B at 40% opacity. Do not use grey — the tinted teal-black maintains brand coherence even in disabled states.

**Error text:** #FF6666 (Alert Coral), Bold weight, used in form validation messages and inline error states.

---

## 4. Spacing & Layout

### 4.1 Spacing Unit

The base spacing unit is **8px**. All spacing — margins, padding, gaps, and layout gutters — must be multiples of 8px. The most commonly used values are: 4px (0.5× — tight internal spacing), 8px (1× — default small gap), 16px (2× — component internal padding), 24px (3× — section separation), 32px (4× — major layout divisions), 48px (6× — page-level vertical rhythm), 64px (8× — hero spacing).

This discipline ensures visual harmony across the interface and prevents the accumulation of arbitrary pixel values that make codebases unmaintainable and interfaces visually inconsistent.

### 4.2 Layout Grid

The application uses a **12-column grid** with a 24px gutter on desktop (≥1280px), a **6-column grid** with 16px gutters on tablet (768px–1279px), and a **2-column grid** (or single column for content) with 16px gutters on mobile (≤767px).

Maximum content width: **1200px**, centred. Sidebar navigation is fixed-width at 240px on desktop, collapsing to an icon-only drawer (64px) or full-screen overlay on tablet and mobile respectively.

### 4.3 Elevation & Depth

The system uses a three-level elevation model, communicated through shadow and background contrast rather than heavy borders.

**Level 0 — Page surface:** #FFF9F9. No shadow.
**Level 1 — Cards and panels:** #FFFFFF. Shadow: `0 1px 3px rgba(0, 39, 43, 0.08), 0 1px 2px rgba(0, 39, 43, 0.04)`. Subtle and professional — no floating effect.
**Level 2 — Modals and popovers:** #FFFFFF. Shadow: `0 8px 24px rgba(0, 39, 43, 0.12), 0 2px 8px rgba(0, 39, 43, 0.06)`. Clearly elevated but not dramatic.

Avoid excessive use of Level 2 elevation — it should feel like a meaningful modal interruption, not a default card style.

---

## 5. Iconography

Icons must be from a single, coherent icon system. The preferred system is a simple outlined icon set (such as Phosphor Icons or Heroicons in their outline variant), never filled/chunky icon styles that suggest consumer apps. Icon weight should visually match the Hind regular weight — consistent stroke width around 1.5–2px at 24px size.

**Icon sizing:** 24px for UI actions and navigation, 20px for inline labels and buttons, 16px for dense data tables and badges, 32px for empty-state illustrations.

**Icon colour:** Icons inherit the text colour of their parent context. Active navigation icons use #3A8991. Disabled icons use #00272B at 30% opacity. Status icons use the functional colours: #E0FF4F for pass/complete, #FF6666 for alert/overdue.

Do not use decorative illustrations or imagery-based icons. The IK interface is a professional tool — iconography must communicate function, not personality.

---

## 6. Components

### 6.1 Buttons

**Primary Button:** Background #00272B, text white, Montserrat Bold 13px uppercase, horizontal padding 24px, vertical padding 12px, border-radius 4px. Hover state: background lightens to #13575E. Active/pressed: #3A8991. Disabled: background #00272B at 30% opacity, no pointer cursor.

**Secondary Button:** Background transparent, border 1.5px solid #00272B, text #00272B, same typographic treatment as primary. Hover: background #C6F2F7. This is the "outline" variant for secondary actions.

**Danger Button:** Background #FF6666, text white. Used only for irreversible destructive actions (e.g., delete a compliance record). Requires a confirmation dialog — never trigger immediate destructive actions with a single click.

**Ghost Button / Text Button:** No background, no border, text #3A8991 Montserrat SemiBold. Used for tertiary actions, cancel operations, and inline links that function as actions.

**Module-specific variant (IK-Alkohol):** Secondary buttons within alcohol compliance modules use #725699 border and text in place of #00272B, maintaining visual module identity.

All buttons must have a visible focus ring for keyboard accessibility: a 2px solid outline offset by 2px, using #3A8991. This is mandatory for WCAG compliance and reflects the OWASP accessibility standards referenced in the project requirements.

### 6.2 Form Inputs

Input fields use a clean, restrained design. White background, 1px border #76BCC4, border-radius 4px, 12px internal padding, Hind Regular 16px placeholder text at 50% opacity of #00272B. On focus: border becomes 2px solid #3A8991, box-shadow `0 0 0 3px #C6F2F7`.

Form labels: Montserrat SemiBold 13px, uppercase, #00272B, placed above the input with 8px spacing. Required fields indicated with a Deep Violet asterisk (*) before the label, not after.

Error state: border becomes #FF6666, error message below the field in Hind Regular 14px #FF6666. Include an error icon (16px) inline with the message.

### 6.3 Data Tables

Tables are a primary UI element in a compliance system — temperature logs, cleaning records, audit histories, and task lists all live in tabular form. They must be highly readable and scannable.

Column headers: Montserrat Bold 700 11px uppercase, #00272B, background #C6F2F7, 12px vertical padding, 16px horizontal padding.

Row data: Hind Regular 14px, #00272B, white row background, alternating rows use #FFF9F9. Row hover: #C6F2F7 background.

Row height: minimum 48px for touch targets on tablet. Status columns use the signal colours as small filled badges (see Badges below).

Table borders: only horizontal rules, 1px solid #C6F2F7. No vertical dividers — they add visual noise without functional benefit.

### 6.4 Badges & Status Indicators

Status badges are the primary mechanism by which the IK system communicates compliance state. They must be instantly legible and semantically unambiguous.

**Compliant / Completed:** Background #E0FF4F, text #00272B, Montserrat Bold 11px uppercase, 4px vertical padding, 8px horizontal padding, border-radius 100px (pill).

**Non-Compliant / Overdue / Failed:** Background #FF6666, text white, same typographic treatment.

**Pending / In Progress:** Background #AF9BCC, text #1A0833, same treatment.

**Not Applicable / Inactive:** Background #C6F2F7, text #13575E, same treatment.

**Locked / Submitted:** Background #00272B, text white, same treatment.

Never use custom badge colours outside this defined set. Introducing new status colours without updating this guide and all related documentation creates compliance ambiguity — a critical failure in a product that exists to reduce ambiguity.

### 6.5 Cards

Cards are the primary content container. They group related information — a checklist module, a compliance report summary, a temperature log entry.

Card structure: #FFFFFF background, Level 1 shadow, 4px border-radius, 24px internal padding. A coloured left border (4px, solid) is used to indicate module ownership or status: #3A8991 for IK-Mat cards, #725699 for IK-Alkohol cards, #FF6666 for cards in a deviation/alert state, and no coloured border for neutral/informational cards.

Card headers use Montserrat SemiBold 16px, #00272B. Supporting metadata uses Hind Light 12px.

### 6.6 Navigation

The primary navigation is a fixed left sidebar. Top-level items use Montserrat SemiBold 13px uppercase, #FFF9F9 on #00272B background. Active item: left border 3px solid #E0FF4F, background #13575E, text white. Hover: background #13575E, text white.

Section dividers within the sidebar are 1px solid #3A8991 at 30% opacity.

The top bar contains the IK logotype (left), application name / current module indicator (centre), and user profile and notification icons (right). Top bar background is #00272B.

Breadcrumbs: Hind Light 14px, #13575E, with a chevron separator in #76BCC4. The current page label is Hind Regular 14px, #00272B.

---

## 7. Imagery & Visual Elements

### 7.1 Photography

The IK application is a professional tool, not a consumer app. Photography should not appear in functional UI areas. If photographs are used in onboarding flows, marketing pages, or document storage previews, they must be: well-lit, professionally composed, and thematically relevant (kitchen environments, professional service settings, documentation contexts). No stock photography clichés — no people pointing at laptops, no generic handshake images. Food photography, if used, must be clinical and well-composed, suggesting professional food service, not lifestyle marketing.

### 7.2 Illustrations

If empty state illustrations or onboarding graphics are required, they must use the brand colour palette exclusively. Line-weight illustrations in the teal/violet palette, with Signal Lime used as a single accent for the focal point of the illustration. No cartoon-style or whimsical illustration — the aesthetic register is closer to a professional infographic than a consumer SaaS onboarding.

### 7.3 Data Visualisations

Dashboard charts and compliance trend visualisations use the following colour assignments:
- Positive / compliant data series: #3A8991
- Negative / non-compliant data series: #FF6666
- Neutral / pending data series: #AF9BCC
- Target / threshold lines: #E0FF4F
- Background grid lines: #C6F2F7

Chart labels: Hind Regular 12px, #00272B. Chart titles: Montserrat SemiBold 14px, #00272B. All data visualisations must include accessible text alternatives.

---

## 8. Tone & Voice in UI Copy

The UI copy is the written voice of the system and it must be consistent with the visual register: clear, formal, and direct. It is not friendly to the point of being casual, nor institutional to the point of being cold.

**Do:** Use complete sentences for instructional text. Use action-oriented verbs for buttons and CTAs. Confirm completed actions with specific, affirmative statements ("Temperature log submitted" rather than "Success!"). Use formal Norwegian compliance vocabulary where relevant.

**Do not:** Use exclamation marks, emoji, or colloquialisms. Do not use vague system messages ("Something went wrong"). Do not use passive voice in error messages — name what failed and what the user should do next.

**Error messages** follow the pattern: [What failed] + [Why, if known] + [What to do next]. Example: "Temperature log could not be submitted. The server could not be reached. Please check your connection and try again."

---

## 9. Accessibility

The IK system serves regulatory-critical functions. Accessibility is not optional — it is a professional and legal obligation.

**Colour contrast:** All text must meet WCAG AA as a minimum, with AA+ (4.5:1 for normal text, 3:1 for large text) as the target. The combination of #00272B on #FFF9F9 comfortably exceeds AA. Signal Lime (#E0FF4F) on #00272B must be verified — this combination is used for active navigation states and badge elements.

**Focus management:** All interactive elements must have a visible focus indicator. The 2px #3A8991 outline system is the default. Do not suppress `:focus-visible` styles.

**Touch targets:** All interactive elements must be a minimum of 44×44px in touch contexts. This is especially important for mobile/tablet use in kitchen or bar environments where users may be wearing gloves or in low-precision conditions.

**Semantic structure:** Headings must follow the defined hierarchy — do not use H3 where H2 is the correct structural element simply for visual sizing purposes. Use CSS classes to control visual weight independently of semantic heading level.

---

## 10. Motion & Interaction

Motion in a compliance application must serve clarity, not delight. Transitions should orient the user, confirm actions, and prevent disorientation — they should never attract attention to themselves.

**Default transition:** 150ms ease-in-out for all interactive state changes (hover, focus, active, checked). This is fast enough to feel responsive and slow enough to be perceivable.

**Page/section transitions:** Fade + translate-Y (8px → 0) on 200ms ease-out. Subtle entrance that confirms navigation without drama.

**Modal open/close:** Scale from 0.97 → 1 with opacity 0 → 1 on open, reversed on close. Duration 200ms.

**Loading states:** Skeleton loaders in #C6F2F7, pulsing at 1.5s interval. No spinner where a skeleton is possible — skeletons reduce perceived load time and prevent layout shift.

**Avoid:** Bouncing animations, overshooting springs, parallax effects, entrance animations that delay access to content, and any animation that cannot be disabled for users with `prefers-reduced-motion`.

---

## 11. Print & Export Styles

IK generates compliance reports for regulatory inspection. PDF exports must follow the print style rules:

Background: #FFFFFF. All teal and violet surfaces revert to white. Text: #000000. The Signal Lime (#E0FF4F) badge must include a visible border (#00272B, 1pt) in print contexts, as yellow prints poorly on some devices. #FF6666 prints acceptably but should be supplemented with a text label or symbol (✗) to ensure monochrome legibility.

Headers in exported reports carry the IK logotype and the module identifier (IK-Mat or IK-Alkohol) in Montserrat Bold. Footer carries the organisation name, report generation date, and page numbers.

Font size in print: body text minimum 10pt, headers minimum 12pt.

---

## 12. Brand Identity Notes

The logotype uses the wordmark "Intern Kontroll" or the initials "IK" in Montserrat Bold, set in #00272B or reversed to white on dark backgrounds. The initials mark "IK" is the compact version for app icons, favicons, and tight-space applications.

The brand voice in external contexts (marketing, onboarding) is: **authoritative, enabling, and trustworthy**. IK does not sell fear of non-compliance — it sells confidence in compliance. Copy should position the product as a partner in professional excellence, not a watchdog.

---

*This styleguide is a living document. Any proposed deviations from the specifications herein must be reviewed against the four brand pillars — openness, formality, cleanliness, and professionalism — and documented as approved amendments.*
