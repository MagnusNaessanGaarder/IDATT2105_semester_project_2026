# IDATT2105 Full-stack Application Development — Semester Project 2026

> **Les dette dokumentet nøye og ta hensyn til detaljene!**

---

## Om prosjektet

Prosjektoppgaven er frivillig og kan kun gjennomføres av studenter som har fått C og ønsker å forbedre karakteren til A eller B. Merk at alle deler av prosjektet (programmert løsning, dokumentasjon og presentasjon) forventes å holde **meget god kvalitet** for å oppnå karakter B eller A.

Kravet er å implementere en **full-stack webapplikasjon** som inkluderer:

- **Frontend** — Vue.js (v3) med relaterte rammeverk og biblioteker
- **Backend** — Java (v17, v21 eller v25) med Spring Boot (v3) / Spring Framework (v5 eller v6)
- **Database** — MySQL (v8 eller v9) og/eller H2

Prosjektet inneholder flere deler, og jo mer gruppen gjennomfører med god kvalitet — jo bedre! Hver gruppe bør vurdere hva de prioriterer og fullføre valgt funksjonalitet så godt som mulig. **Ufullstendig funksjonalitet vil ikke bli evaluert og skal ikke være en del av leveransen.**

Evalueringen baseres på alle deler av prosjektutviklingen: god kode, nødvendig dokumentasjon for testing og videreutvikling, brukergrensesnitt og velfungerende funksjonalitet.

> ⚠️ **Plagiat er ikke tillatt.** Dersom kildekoden til to eller flere grupper er for lik, kan karakteren til alle disse gruppene påvirkes negativt og kan resultere i utestengelse fra studiet.

---

## Prioriteringsliste (viktig!)

Prioriter i denne rekkefølgen:

1. **Funksjonalitet** — fungerende basis (full-stack) på plass først! Alt annet er pluss.
2. **Kodekvalitet** — cohesion, separation of concerns osv.
3. **Arkitektur og design**
4. **Sikkerhet** — OWASP og implementasjon på fornuftig/gjenbrukbart vis
5. **Universell utforming**
6. **Testing** — unit, integrasjonstesting (+) og testing for utenforstående (sensor)
7. **Test coverage** — mer er bedre
8. **CI/CD** — og aktiv bruk av det
9. **Prosjektstruktur**
10. **Dokumentasjon** — README, API-doc, systemoversikt/DB-skjema, testings-/installasjonsbeskrivelse osv.
11. **Prosjektpresentasjon**

> 💡 Det er bedre å huke av på alle punktene enn å bare lage masse funksjonalitet. Gjennomfør et godt og helhetlig prosjekt.

> 📌 For å få **A eller B** kreves det at leveransen er *"Meget godt"* eller *"Særdeles godt"* (dvs. imponerende). Leveranser på forventet nivå havner på **C**. Det er ingen automatikk i karakterforbedring fordi man tar prosjektet!

> 👥 Dette er en **gruppeinnlevering** — husk å få med alle i dokumentasjonen!

---

## Generelle krav

### 1. Innleveringsfrist
**Slutten av uke 15 — fredag 10. april 2026, kl. 14:00**

### 2. Innlevering
Påkrevd materiale leveres i **Inspera** (med mindre annet er oppgitt i Blackboard). Én innlevering per gruppe. Husk å skrive navnene på alle deltakere i gruppen.

### 3. Tekniske krav

| Krav | Detaljer |
|------|----------|
| **CSS** | Ren CSS — ingen CSS-rammeverk som Tailwind |
| **Backend** | Spring Boot/REST. Hvert endepunkt skal ha riktig autentisering/autorisering, f.eks. JWT og Spring Security |
| **Database** | Plain JDBC eller Spring JDBC / JPA |
| **Testing** | Løsningen skal inneholde tester. **Minimum 50% kodedekning** |
| **CI/CD** | Skal brukes aktivt under utvikling |
| **Sikkerhet & UU** | OWASP og universell utforming skal benyttes |
| **Sesjon** | Session storage kan brukes for kortvarig innloggingssesjon på frontend |

### 4. Dokumentasjonskrav (per modul)

- **API-dokumentasjon** — Endepunkter må dokumenteres, f.eks. med Swagger. Inkluder forklaring på hva endepunktene gjør og hva de ulike attributtene er. Kode skal dokumenteres med Javadoc.
- **Systemdokumentasjon** — Dokumentasjon som gjør det mulig for en ny utvikler å raskt sette opp prosjektet for testing og videreutvikling. Inkluder arkitekturskisser/klassediagram. Instruksjoner for å kjøre prosjektet kan gjøres som en README-fil, mens annen dokumentasjon bør være PDF.
- **Testdata** — Testdata som kan brukes under testing av appen, f.eks. testbrukerinnlogging og databaseopplysninger.
- **Forutsetninger** — Dokumenter eventuelle avhengigheter mellom moduler/prosjekter.

### 5. Innleveringsmateriale

- En **zip-fil** med alle moduler/filer
- **Kjørbar kildekode** for hvert prosjekt/modul:
  - Dokumenterte kilde- og testfiler
  - Konfigurasjonsfiler: `pom.xml`, `package.json`, `Dockerfile` osv.
  - Flyway eller vanlige DB-skjemaskript med testdata
- **Beskrivelse** av hvordan man kjører tester og starter systemet — gjør det enkelt (script, Docker/Maven-kommando eller lignende)

---

## Produkt og krav

### Systembeskrivelse

Prosjektet fokuserer på design og utvikling av et **digitalt internkontrollsystem** tilpasset restauranter, barer og andre serveringssteder for mat og alkohol. Systemet skal forenkle etterlevelse av helse-, sikkerhets- og alkoholreguleringer ved å erstatte manuelle sjekklister og papirbaserte rutiner med en strukturert digital løsning.

Plattformen skal hjelpe virksomheter med å:
- Overvåke daglig drift
- Sikre riktig dokumentasjon
- Opprettholde høye standarder for hygiene og ansvarlig alkoholservering
- Sentralisere oppgaver som temperaturlogging, rengjøringsrutiner og opplæringsregistre

Dette forbedrer sporbarhet, reduserer menneskelige feil og gjør det lettere å bestå inspeksjoner fra tilsynsmyndigheter.

### Arkitektur

Systemet er delt inn i to uavhengige tjenester på toppnivå:

- **IK-Mat** — matoverholdelse
- **IK-Alkohol** — alkoholoverholdelse

Disse tjenestene er atskilt for å gjenspeile ulike regulatoriske krav og operasjonelle arbeidsflyter. Begge tjenestene kan likevel benytte **delte, gjenbrukbare komponenter** som:

- Brukeradministrasjon og autentisering
- Varsler og notifikasjoner
- Dokumentlagring
- Rapportering

Systemet skal støtte **multi-tenancy** — flere organisasjoner skal kunne konfigurere og administrere sine egne overholdelsesprosesser uavhengig av hverandre innenfor samme plattform.

> ℹ️ Layout og design av brukergrensesnittelementer er åpent — det er opp til hver gruppe sin kreativitet. Grupper forventes å basere systemet på **autentiske krav** for IK-Mat og IK-Alkohol hentet fra relevante lovmyndigheter og reguleringer.

> 🍣 **Sponsor:** Prosjektet er sponset av den kommende restauranten **Everest Sushi & Fusion AS** (org.nr. 937 219 997). To utvalgte prosjektteam vil motta et spesielt gavekort fra restauranten og kan bli invitert til å videreutvikle løsningene sine etter kurset.

---

## Eksempelfunksjoner

### Digitale sjekklister
Daglige, ukentlige og månedlige oppgavelister for hygiene, rengjøring og sikkerhetsprosedyrer.

### Temperaturlogging
Registrering og overvåking av matlagringstemperaturer med varsler ved avvik.

### Avvikshåndtering
Mulighet for å rapportere, spore og løse hendelser eller tilfeller av manglende overholdelse.

### Alkoholoverholdelse
Dokumentasjon av rutiner knyttet til ansvarlig alkoholservering og aldersverifisering.

### Brukerroller og tilgangskontroll
Ulike tilgangsnivåer for ansatte, ledere og administratorer.

### Revisjons- og inspeksjonsrapporter
Automatisk genererte rapporter for interne gjennomganger og eksterne inspeksjoner.

### Dokumenteksport (PDF/JSON)
Eksport av rapporter, logger og dokumentasjon i PDF (for deling og etterlevelse) eller JSON (for systemintegrasjon og dataportabilitet).

### Varsler og påminnelser
Varsler for forfalte oppgaver, manglende logger eller kritiske problemer.

### Dokumentlagring
Sentralisert lagring av retningslinjer, opplæringsmateriell og sertifiseringer.

### Mobiltilgjengelighet
Responsivt design eller mobilapp for bruk på smarttelefoner og nettbrett.

### Dataanalyse-dashboard
Oversikt over overholdelsestatus, trender og ytelsesmålinger.

---

## Prosjektpresentasjon

Prosjektpresentasjonen gjøres som en **videopresentasjon**, levert som en del av prosjektleveransen. Mer informasjon kommer nærmere presentasjonstidspunktet.

---

## Spørsmål om oppgaven?

Bruk forumet på **Blackboard**. Dersom ønsket funksjonalitet er tvetydig, er gruppen i stor grad fri til å tolke oppgaven selv. Gruppen bør prioritere den viktigste funksjonaliteten og fullføre den, før de tar på seg mer funksjonalitet. **Lag derfor en liste over prioriteringer og begrunnelsen for disse.**
