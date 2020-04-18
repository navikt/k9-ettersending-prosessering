package no.nav.helse.prosessering.v1.felles

data class Fodselsnummer(private val value: String) : NorskIdent {
    override fun getValue() = value
}
