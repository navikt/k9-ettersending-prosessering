# K9 Ettersending Prosessering
![CI / CD](https://github.com/navikt/k9-ettersending-prosessering/workflows/CI%20/%20CD/badge.svg)
![NAIS Alerts](https://github.com/navikt/k9-ettersending-prosessering/workflows/Alerts/badge.svg)

Tjeneste som prosesserer melding om ettersending for omsorgspenger og pleiepenger.
Leser meldinger fra Kafka topic `privat-k9-ettersending-mottatt` som legges der av [k9-ettersending-mottak](https://github.com/navikt/k9-ettersending-mottak)

## Prosessering
- Genererer Søknad-PDF
- Oppretter Journalpost
- Oppretter Gosys Oppgave
- Sletter mellomlagrede dokumenter

## Feil i prosessering
Ved feil i en av streamene som håndterer prosesseringen vil streamen stoppe, og tjenesten gi 503 response på liveness etter 15 minutter.
Når tjenenesten restarter vil den forsøke å prosessere søknaden på ny og fortsette slik frem til den lykkes.

## Alarmer
Vi bruker [nais-alerts](https://doc.nais.io/observability/alerts) for å sette opp alarmer. Disse finner man konfigurert i [nais/alerterator.yml](nais/alerterator.yml).

## Henvendelser
Spørsmål knyttet til koden eller prosjektet kan stilles som issues her på GitHub.

Interne henvendelser kan sendes via Slack i kanalen #team-düsseldorf.
