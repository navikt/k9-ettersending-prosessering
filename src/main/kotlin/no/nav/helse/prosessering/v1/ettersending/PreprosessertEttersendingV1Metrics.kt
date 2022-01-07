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
        .labels(søknadstype.name) ////TODO 23.03.2021 - Må oppdatere Grafana til å støtte nytt navn
        .observe(vedleggId.size.toDouble()-1) //Minus 1 fordi også oppsummeringPDF blir lagt i dokumentUrls
}
