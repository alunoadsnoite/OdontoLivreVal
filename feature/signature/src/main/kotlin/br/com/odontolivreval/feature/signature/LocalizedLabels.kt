package br.com.odontolivreval.feature.signature

import androidx.annotation.StringRes
import br.com.odontolivreval.core.demo.SignatureModality
import br.com.odontolivreval.core.demo.SignatureStatus

@get:StringRes
internal val SignatureStatus.labelResource: Int
    get() = when (this) {
        SignatureStatus.PENDING -> R.string.signature_label_pending
        SignatureStatus.SIGNED -> R.string.signature_label_signed
        SignatureStatus.REVOKED -> R.string.signature_label_revoked
    }

@get:StringRes
internal val SignatureModality.labelResource: Int
    get() = when (this) {
        SignatureModality.ELECTRONIC_ACKNOWLEDGEMENT -> R.string.signature_label_electronic_acknowledgement
        SignatureModality.HANDWRITTEN_ON_PAPER -> R.string.signature_label_handwritten_on_paper
        SignatureModality.HANDWRITTEN_DIGITAL -> R.string.signature_label_handwritten_digital
        SignatureModality.CRYPTOGRAPHIC -> R.string.signature_label_cryptographic
    }
