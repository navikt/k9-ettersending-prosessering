package no.nav.helse

import no.nav.helse.prosessering.v1.felles.PreprossesertBarn
import no.nav.helse.prosessering.v1.felles.PreprossesertSøker
import no.nav.k9.søknad.felles.Barn
import no.nav.k9.søknad.felles.NorskIdentitetsnummer
import no.nav.k9.søknad.felles.Søker

fun PreprossesertSøker.tilK9Søker(): Søker = Søker.builder()
    .norskIdentitetsnummer(NorskIdentitetsnummer.of(fødselsnummer))
    .build()

fun PreprossesertBarn.tilK9Barn(): Barn {
    return when {
        !norskIdentifikator.isNullOrBlank() -> Barn.builder().norskIdentitetsnummer(
            NorskIdentitetsnummer.of(
                norskIdentifikator
            )
        ).build()
        fødselsDato != null -> Barn.builder().fødselsdato(fødselsDato).build()
        else -> throw IllegalArgumentException("Ikke tillatt med barn som mangler både fødselsdato og fødselnummer.")
    }
}
