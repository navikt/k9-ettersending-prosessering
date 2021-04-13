package no.nav.helse.dokument

import no.nav.helse.CorrelationId
import no.nav.helse.prosessering.v1.ettersending.EttersendingV1
import no.nav.helse.prosessering.v1.felles.AktørId
import no.nav.k9.ettersendelse.Ettersendelse
import no.nav.k9.søknad.JsonUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.net.URI

private val logger: Logger = LoggerFactory.getLogger("nav.DokumentService")

class DokumentService(
    private val dokumentGateway: DokumentGateway
) {
    private suspend fun lagreDokument(
        dokument: DokumentGateway.Dokument,
        aktørId: AktørId,
        correlationId: CorrelationId
    ) : URI {
        return dokumentGateway.lagreDokmenter(
            dokumenter = setOf(dokument),
            correlationId = correlationId,
            aktørId = aktørId
        ).first()
    }

    internal suspend fun lagreSoknadsOppsummeringPdf(
        pdf : ByteArray,
        aktørId: AktørId,
        correlationId: CorrelationId,
        dokumentbeskrivelse: String
    ) : URI {
        return lagreDokument(
            dokument = DokumentGateway.Dokument(
                content = pdf,
                contentType = "application/pdf",
                title = dokumentbeskrivelse
            ),
            aktørId = aktørId,
            correlationId = correlationId
        )
    }

    internal suspend fun lagreSoknadsMeldingEttersending(
        ettersending: Ettersendelse,
        aktørId: AktørId,
        correlationId: CorrelationId,
        søknadstype: String
    ) : URI {
        logger.info("SKAL IKKE VISES I PROD. Format som lagres i JOARK: ${JsonUtils.toString(ettersending)}") //TODO 13.04.2021 - Fjerne før prodsetting
        return lagreDokument(
            dokument = DokumentGateway.Dokument(
                content = Søknadsformat.somJsonEttersending(ettersending),
                contentType = "application/json",
                title = "Ettersendelse $søknadstype som JSON"
            ),
            aktørId = aktørId,
            correlationId = correlationId
        )
    }

    internal suspend fun slettDokumeter(
        urlBolks: List<List<URI>>,
        aktørId: AktørId,
        correlationId : CorrelationId
    ) {
        val urls = mutableListOf<URI>()
        urlBolks.forEach { urls.addAll(it) }

        logger.trace("Sletter ${urls.size} dokumenter")
        dokumentGateway.slettDokmenter(
            urls = urls,
            aktørId = aktørId,
            correlationId = correlationId
        )
    }
}

