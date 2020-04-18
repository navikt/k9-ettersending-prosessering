package no.nav.helse.prosessering.v1.ettersending

import io.prometheus.client.Histogram

private val antallVedleggHistogram = Histogram.build()
    .buckets(1.0,2.0,3.0)
    .name("antall_vedlegg_ettersending_histogram")
    .help("Antall vedlegg som det blir ettersendt")
    .labelNames("soknadtype")
    .register()

internal fun PreprosessertEttersendingV1.reportMetrics() {
    antallVedleggHistogram
        .labels(søknadstype.type)
        .observe(dokumentUrls.size.toDouble()-1) //Minus 1 fordi også oppsummeringPDF blir lagt i dokumentUrls
}
