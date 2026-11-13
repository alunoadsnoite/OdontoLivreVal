package br.com.odontolivreval.feature.consent

import androidx.annotation.StringRes
import br.com.odontolivreval.core.demo.ConsentStatus

@get:StringRes
internal val ConsentStatus.labelResource: Int
    get() = when (this) {
        ConsentStatus.DRAFT -> R.string.consent_label_draft
        ConsentStatus.PRESENTED -> R.string.consent_label_presented
        ConsentStatus.PENDING -> R.string.consent_label_pending
        ConsentStatus.ACCEPTED -> R.string.consent_label_accepted
        ConsentStatus.REJECTED -> R.string.consent_label_rejected
        ConsentStatus.REVOKED -> R.string.consent_label_revoked
        ConsentStatus.SUPERSEDED -> R.string.consent_label_superseded
    }
