package br.com.odontolivreval.feature.treatment

import androidx.annotation.StringRes
import br.com.odontolivreval.core.demo.BudgetStatus

@get:StringRes
internal val BudgetStatus.labelResource: Int
    get() = when (this) {
        BudgetStatus.DRAFT -> R.string.treatment_label_draft
        BudgetStatus.PRESENTED -> R.string.treatment_label_presented
        BudgetStatus.APPROVED -> R.string.treatment_label_approved
        BudgetStatus.REJECTED -> R.string.treatment_label_rejected
        BudgetStatus.EXPIRED -> R.string.treatment_label_expired
        BudgetStatus.CANCELLED -> R.string.treatment_label_cancelled
    }
