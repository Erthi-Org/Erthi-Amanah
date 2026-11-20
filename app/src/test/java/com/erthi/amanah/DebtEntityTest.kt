package com.erthi.amanah

import com.erthi.amanah.data.db.entity.DebtDirection
import com.erthi.amanah.data.db.entity.DebtEntity
import com.erthi.amanah.data.db.entity.RepaymentStatus
import org.junit.Assert.*
import org.junit.Test

class DebtEntityTest {

    @Test
    fun debtEntity_defaultValues_areCorrect() {
        val debt = DebtEntity(
            direction = DebtDirection.OWED_BY_ME,
            personName = "أحمد",
            amount = 500.0,
        )
        assertEquals("EGP", debt.currency)
        assertEquals(RepaymentStatus.PENDING, debt.status)
        assertTrue(debt.id == 0L)
    }

    @Test
    fun debtEntity_direction_owedToMe() {
        val debt = DebtEntity(
            direction = DebtDirection.OWED_TO_ME,
            personName = "محمد",
            amount = 200.0,
        )
        assertEquals(DebtDirection.OWED_TO_ME, debt.direction)
    }

    @Test
    fun debtEntity_negativeAmount_accepted() {
        // Business logic should validate amounts, but entity itself accepts any double
        val debt = DebtEntity(
            direction = DebtDirection.OWED_BY_ME,
            personName = "Test",
            amount = -100.0,
        )
        assertEquals(-100.0, debt.amount, 0.001)
    }
}
