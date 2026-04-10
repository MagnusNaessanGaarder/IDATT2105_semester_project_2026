# Systemadministrasjon

Sysadmin er en plattformrolle som eksisterer utenfor den vanlige organisasjonsstrukturen. En sysadmin kan registrere,
redigere og deaktivere organisasjoner, men har ikke tilgang til noen organisasjons interne data.

---

## Tilgang

| Felt             | Verdi                     |
|------------------|---------------------------|
| Standard e-post  | `sysadmin@ik-kontroll.no` |
| Standard passord | `Test1234!`               |

Sysadmin-brukere opprettes direkte i databasen ved å sette `is_sysadmin = 1` på raden i `app_user`. Det finnes ingen
selvregistrering for denne rollen.

---

## Hva sysadmin ser

Når en sysadmin logger inn sendes de automatisk til `/sysadmin`. De ser ikke den vanlige applikasjonen med sidebar,
moduler eller dashbord — kun administrasjonssiden for organisasjoner.

---

## Organisasjonsstyring

Fra `/sysadmin` kan sysadmin:

- **Registrere** en ny organisasjon med organisasjonsnummer, juridisk navn, visningsnavn og kontaktinfo
- **Redigere** eksisterende organisasjoner (alle felt inkludert aktiv-status)
- **Deaktivere** en organisasjon (soft delete — setter `isActive = false`)

Deaktivering er reversibel via rediger-funksjonen. Brukere tilknyttet en deaktivert organisasjon mister tilgang
umiddelbart ved neste innlogging.

---

## API-endepunkter

Alle endepunkter krever `ROLE_SYSADMIN` og et gyldig Bearer-token.  
Basepath: `/api/v1/sysadmin/organizations`

| Metode   | Sti            | Beskrivelse               |
|----------|----------------|---------------------------|
| `GET`    | `/`            | Hent alle organisasjoner  |
| `POST`   | `/`            | Registrer ny organisasjon |
| `PUT`    | `/{orgNumber}` | Oppdater organisasjon     |
| `DELETE` | `/{orgNumber}` | Deaktiver organisasjon    |

Full dokumentasjon er tilgjengelig i Swagger UI på `/swagger-ui/index.html`.

---

## Tekniske detaljer

- Rollen styres av kolonnen `is_sysadmin TINYINT` i `app_user`-tabellen
- En sysadmin har ingen rader i `user_organization` eller `user_organization_role` — de er ikke medlemmer av noen
  organisasjon
- Spring Security tildeler kun `ROLE_SYSADMIN` ved innlogging; alle andre rolleoppslag hoppes over
- Frontend-routeren blokkerer sysadmin fra alle normale ruter og omdirigerer til `/sysadmin`