package no.nav.helse.prosessering.v1.felles

import com.fasterxml.jackson.annotation.JsonFormat
import java.time.LocalDate

data class Barn(
    val navn: String?,
    val norskIdentifikator: String?,
    @JsonFormat(pattern = "yyyy-MM-dd") val fødselsdato: LocalDate?,
    val aktørId: String?
) {
    override fun toString(): String {
        return "Barn(navn=$navn, aktørId=$aktørId)"
    }
}
