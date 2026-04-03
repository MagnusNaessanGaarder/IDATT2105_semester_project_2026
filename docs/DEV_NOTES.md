# IDATT2105 - Semester Project 2026
# GJENSTÅENDE OPPGAVER FØR INNLEVERING

> **Innleveringsfrist:** Fredag 10. april 2026, kl. 14:00
> **Siste oppdatering:** 3. april 2026

---

## KRITISKE OPPGAVER (MÅ gjøres - ellers blir karakteren påvirket)

### Backend (Java/Spring Boot)

| # | Oppgave | Status | Estimert tid | Prioritet |
|---|---------|--------|--------------|-----------|
| 1 | **Temperature Logging Service** - Full implementasjon av temperaturlogging (IK-Mat) | ☐ | 8t | KRITISK |
|   | - TemperatureLogEntry repository | | | |
|   | - TemperatureLogPoint service | | | |
|   | - REST controller med CRUD | | | |
|   | - DTOs og mapping | | | |
|   | - Enhetstester | | | |
| 2 | **Notification System** - Varsling til ansatte | ☐ | 8t | KRITISK |
|   | - Notification service | | | |
|   | - NotificationDelivery repository | | | |
|   | - REST controller | | | |
|   | - Enhetstester | | | |
| 3 | **Export/PDF Service** - Eksport av rapporter | ☐ | 10t | KRITISK |
|   | - PDF-generering (f.eks. OpenPDF/iText) | | | |
|   | - JSON eksport | | | |
|   | - ExportJob service | | | |
|   | - Controller endepunkter | | | |
|   | - Enhetstester | | | |
| 4 | **Training Record Service** (IK-Alkohol) | ☐ | 4t | KRITISK |
|   | - TrainingRecord entity finnes, mangler service | | | |
|   | - CRUD operasjoner | | | |
|   | - Enhetstester | | | |
| 5 | **Testdekning Backend** - Øke fra ~70% til 80%+ | ☐ | 12t | KRITISK |
|   | - Repository tester (16 repositories) | | | |
|   | - Service tester (Notification, Temperature, Export) | | | |
|   | - Scheduler service tester | | | |
|   | - Mapper tester (4 mappers) | | | |

### Frontend (Vue.js)

| # | Oppgave | Status | Estimert tid | Prioritet |
|---|---------|--------|--------------|-----------|
| 6 | **Temperature Logging UI** - Temperatur-logging visning | ☐ | 8t | KRITISK |
|   | - TemperatureView.vue implementasjon | | | |
|   | - TemperaturCard-komponent | | | |
|   | - Skjema for logging | | | |
|   | - API-integrasjon | | | |
| 7 | **Admin Views** - Administrasjon | ☐ | 10t | KRITISK |
|   | - UsersView.vue full implementasjon | | | |
|   | - SettingsView.vue full implementasjon | | | |
|   | - Brukeradministrasjon skjemaer | | | |
| 8 | **Notification UI** - Varslingsvisning | ☐ | 6t | KRITISK |
|   | - NotificationsView.vue implementasjon | | | |
|   | - Notification-komponenter | | | |
| 9 | **Frontend Testdekning** - Fra ~0% til 50%+ | ☐ | 20t | KRITISK |
|   | - Komponent-tester (20+ komponenter) | | | |
|   | - Composables-tester (useAuth, useApi, useForm) | | | |
|   | - Store-tester (Pinia stores) | | | |
| 10 | **E2E Tester** - Cypress tester for hovedflyter | ☐ | 8t | KRITISK |
|   | - Checklist flows | | | |
|   | - Deviation flows | | | |
|   | - Admin flows | | | |

### Dokumentasjon (PDF-filer til Inspera)

| # | Oppgave | Status | Estimert tid | Prioritet |
|---|---------|--------|--------------|-----------|
| 11 | **Systemdokumentasjon (PDF)** | ☐ | 6t | KRITISK |
|   | - Arkitekturskisser/klassediagram | | | |
|   | - Database-skjema (ER-diagram) | | | |
|   | - Oppsett-instruksjoner for utviklere | | | |
| 12 | **API-dokumentasjon (PDF)** | ☐ | 4t | KRITISK |
|   | - Swagger eksport til PDF | | | |
|   | - Detaljerte endepunkt-beskrivelser | | | |
|   | - Attributt-beskrivelser | | | |
| 13 | **README for sensor** | ☐ | 3t | KRITISK |
|   | - Hvordan starte systemet | | | |
|   | - Hvordan kjøre tester | | | |
|   | - Testbrukere og passord | | | |
| 14 | **Testdata-dokumentasjon** | ☐ | 2t | KRITISK |
|   | - Liste over testbrukere | | | |
|   | - Testorganisasjoner | | | |
|   | - Sample data for testing | | | |

---

## HØY PRIORITET (Bør gjøres for A/B karakter)

### Backend

| # | Oppgave | Status | Estimert tid | Prioritet |
|---|---------|--------|--------------|-----------|
| 15 | **Audit Log Service** - Sporbarhet | ☐ | 4t | HØY |
| 16 | **Admin Management Endepunkter** | ☐ | 6t | HØY |
| 17 | **Integrasjonstester** - Flere enn bare Auth | ☐ | 6t | HØY |
| 18 | **PreAuthorize annotasjoner** - Full dekning | ☐ | 2t | HØY |
| 19 | **Database indekser** - Performance | ☐ | 3t | HØY |

### Frontend

| # | Oppgave | Status | Estimert tid | Prioritet |
|---|---------|--------|--------------|-----------|
| 20 | **HACCP View** - Fullføre implementasjon | ☐ | 4t | HØY |
| 21 | **PDF Export UI** - Nedlasting i frontend | ☐ | 4t | HØY |
| 22 | **Loading States** - Konsistent UX | ☐ | 3t | HØY |
| 23 | **Error Handling** - Felles feilhåndtering | ☐ | 3t | HØY |

### DevOps/CI-CD

| # | Oppgave | Status | Estimert tid | Prioritet |
|---|---------|--------|--------------|-----------|
| 24 | **Test Coverage i CI** - JaCoCo rapporter | ☐ | 3t | HØY |
| 25 | **Frontend Coverage i CI** - Vitest coverage | ☐ | 2t | HØY |
| 26 | **Production Docker Compose** | ☐ | 3t | HØY |

### Sikkerhet

| # | Oppgave | Status | Estimert tid | Prioritet |
|---|---------|--------|--------------|-----------|
| 27 | **Rate Limiting** - API beskyttelse | ☐ | 3t | HØY |
| 28 | **Input Validation** - Strengere validering | ☐ | 2t | HØY |
| 29 | **Security Headers** - CSP, X-Frame-Options | ☐ | 2t | HØY |

---

## MIDDLE PRIORITET (Nice to have)

| # | Oppgave | Status | Estimert tid | Prioritet |
|---|---------|--------|--------------|-----------|
| 30 | **Performance Optimalisering** | ☐ | 4t | MIDDELS |
| 31 | **Mobil Responsivitet** - Polish | ☐ | 6t | MIDDELS |
| 32 | **Universell Utforming (WCAG)** | ☐ | 4t | MIDDELS |
| 33 | **JavaDoc Completion** | ☐ | 4t | MIDDELS |
| 34 | **Penetration Testing** | ☐ | 4t | MIDDELS |
| 35 | **Automated Security Scanning** | ☐ | 3t | MIDDELS |

---

## TOTAL ESTIMAT

| Kategori | Antall oppgaver | Timer | Komplett |
|----------|-----------------|-------|----------|
| **Kritiske** | 14 | ~99t | ☐ |
| **Høy** | 15 | ~48t | ☐ |
| **Middels** | 6 | ~25t | ☐ |
| **TOTALT** | **35** | **~172t** | |

> **Merk:** Dette er estimater. Med 2 personer = ~86 timer per person. Med 3 personer = ~57 timer per person.

---

## ANBEFALT REKKEFØLGE

### Uke 14 (7-13 april) - Siste uke før innlevering!

**Dag 1-2 (Mandag-Tirsdag): Kritiske Backend-funksjoner**
- Temperature Logging Service (#1)
- Notification System (#2)
- Export/PDF Service (#3)

**Dag 3 (Onsdag): Kritiske Frontend-funksjoner**
- Temperature Logging UI (#6)
- Notification UI (#8)

**Dag 4-5 (Torsdag-Fredag): Testing**
- Frontend unit tester (#9)
- Backend testdekning (#5)
- E2E tester (#10)

**Helg (Lørdag-Søndag): Dokumentasjon**
- Systemdokumentasjon (#11)
- API-dokumentasjon (#12)
- README og testdata (#13, #14)

**Dag 6-7 (Mandag-Tirsdag 14-15 april): Polish**
- Admin views (#7)
- Høy prioritet oppgaver
- Testing av hele systemet

**Onsdag 16. april: Finalisering**
- Siste testing
- Pakke til zip-fil
- Levere i Inspera

---

## OPPGAVEFORDELING (Forslag)

**Person 1 - Backend Fokus:**
- Temperature Logging (#1)
- Notification System (#2)
- Export/PDF (#3)
- Training Record (#4)
- Backend testing (#5)

**Person 2 - Frontend Fokus:**
- Temperature Logging UI (#6)
- Notification UI (#8)
- Admin Views (#7)
- Frontend testing (#9)
- E2E testing (#10)

**Felles - Dokumentasjon (siste uke):**
- Systemdokumentasjon (#11)
- API-dokumentasjon (#12)
- README (#13, #14)

---

## VIKTIGE REMINDERS

- [ ] **IKKE start ny funksjonalitet** - Fullfør kritisk først!
- [ ] **Ufullstendig funksjonalitet blir IKKE evaluert**
- [ ] **Minimum 50% testdekning** kreves
- [ ] **All kode må være i GitHub** før innlevering
- [ ] **Zip-fil til Inspera** må inneholde ALLE filer
- [ ] **Test at alt fungerer** på en fresh maskin før levering

---

## DEFINISJON AV "FERDIG"

En oppgave er FERDIG når:
1. [ ] Koden er skrevet og fungerer
2. [ ] Tester er skrevet og passerer
3. [ ] Dokumentasjon er oppdatert
4. [ ] Code review er gjort (av teammedlem)
5. [ ] Committet og pushet til GitHub

---

## DAGLIG STANDUP (Anbefalt)

**Hver dag kl. 09:00:**
1. Hva gjorde jeg i går?
2. Hva skal jeg gjøre i dag?
3. Har jeg noen blokkeringer?

**Holdes kort:** Maks 15 minutter totalt.

---

## KONTAKT

Ved spørsmål om prioriteringer - diskuter i gruppa!
Husk: Det er bedre å levere **komplett** funksjonalitet enn **mye** funksjonalitet.

**Mål:** Alt merket som KRITISK skal være 100% ferdig før innlevering.
