/**
 * Validators - Valideringsregler for skjemaer
 * 
 * Brukes med useForm composable for å validere input-felt.
 * Hver regel returnerer true hvis gyldig, eller feilmelding hvis ugyldig.
 * 
 * Tilgjengelige regler:
 * - required(): Påkrevd felt
 * - minLength(n): Minimum lengde
 * - maxLength(n): Maksimum lengde  
 * - email(): Gyldig e-post
 * - temperature(): Temperatur mellom -30 og 100°C
 * 
 * Eksempel:
 * { email: [rules.required(), rules.email()] }
 */
export const rules = {
  required: (msg: string = 'Dette feltet er påkrevd') =>
    (v: unknown) => !!v || msg,

  minLength: (n: number, msg?: string) =>
    (v: string) => v?.length >= n || (msg ?? `Minimum ${n} tegn`),

  maxLength: (n: number, msg?: string) =>
    (v: string) => v?.length <= n || (msg ?? `Maksimum ${n} tegn`),

  min: (n: number, msg?: string) =>
    (v: number) => Number(v) >= n || (msg ?? `Minimum ${n}`),

  max: (n: number, msg?: string) =>
    (v: number) => Number(v) <= n || (msg ?? `Maksimum ${n}`),

  email: (msg: string = 'Ugyldig e-postadresse') =>
    (v: string) => /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(v) || msg,

  temperature: () =>
    (v: number) => (v >= -30 && v <= 100) || 'Temperatur må være mellom -30°C og 100°C',

  notFuture: (msg: string = 'Kan ikke være en fremtidig dato') =>
    (v: string) => new Date(v) <= new Date() || msg,

  positiveNumber: () =>
    (v: number) => Number(v) > 0 || 'Må være et positivt tall',
}
