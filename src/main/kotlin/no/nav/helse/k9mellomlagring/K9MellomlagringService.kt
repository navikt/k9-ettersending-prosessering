package no.nav.helse.k9mellomlagring

import no.nav.helse.CorrelationId
import no.nav.k9.ettersendelse.Ettersendelse
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.net.URI

class K9MellomlagringService(
    val k9MellomlagringGateway: K9MellomlagringGateway
) {

    private val logger: Logger = LoggerFactory.getLogger(K9MellomlagringService::class.java)

    private suspend fun lagreDokument(
        dokument: Dokument,
        correlationId: CorrelationId
    ) : URI {
        return k9MellomlagringGateway.lagreDokmenter(
            dokumenter = setOf(dokument),
            correlationId = correlationId
        ).first()
    }

    internal suspend fun lagreSoknadsOppsummeringPdf(
        pdf: ByteArray,
        dokumentEier: DokumentEier,
        correlationId: CorrelationId,
        dokumentbeskrivelse: String
    ): URI {
        return lagreDokument(
            dokument = Dokument(
                eier = dokumentEier,
                content = pdf,
                contentType = "application/pdf",
                title = dokumentbeskrivelse
            ),
            correlationId = correlationId
        )
    }

    internal suspend fun lagreEttersendingSomJson(
        ettersending: Ettersendelse,
        dokumentEier: DokumentEier,
        correlationId: CorrelationId,
        søknadstype: String
    ) : URI {
        return lagreDokument(
            dokument = Dokument(
                eier = dokumentEier,
                content = Søknadsformat.somJsonEttersending(ettersending),
                contentType = "application/json",
                title = "Ettersendelse $søknadstype som JSON"
            ),
            correlationId = correlationId
        )
    }

    internal suspend fun slettDokumeter(
        urlBolks: List<List<URI>>,
        dokumentEier: DokumentEier,
        correlationId : CorrelationId
    ) {
        k9MellomlagringGateway.slettDokmenter(
            urls = urlBolks.flatten(),
            dokumentEier = dokumentEier,
            correlationId = correlationId
        )
    }
}
