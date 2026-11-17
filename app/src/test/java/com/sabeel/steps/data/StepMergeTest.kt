package com.sabeel.steps.data

import com.sabeel.steps.data.repo.MergeOutcome
import com.sabeel.steps.data.repo.StepMerge
import org.junit.Assert.assertEquals
import org.junit.Test

/** Restore merge policy: a CSV import must never lower a day's stored total. */
class StepMergeTest {

    @Test
    fun missingDayIsInserted() {
        assertEquals(MergeOutcome.INSERTED, StepMerge.decide(existingSteps = null, importedSteps = 100))
    }

    @Test
    fun largerImportedTotalWins() {
        assertEquals(MergeOutcome.UPDATED, StepMerge.decide(existingSteps = 100, importedSteps = 400))
    }

    @Test
    fun smallerImportedTotalNeverLowersTheDay() {
        assertEquals(MergeOutcome.KEPT, StepMerge.decide(existingSteps = 400, importedSteps = 100))
    }

    @Test
    fun equalTotalsAreKeptToPreserveMeasuredMinutes() {
        assertEquals(MergeOutcome.KEPT, StepMerge.decide(existingSteps = 400, importedSteps = 400))
    }

    @Test
    fun zeroExistingDayIsUpdatedByAnyImportedSteps() {
        assertEquals(MergeOutcome.UPDATED, StepMerge.decide(existingSteps = 0, importedSteps = 5))
    }

    @Test
    fun zeroImportedTotalCannotLowerAnExistingDay() {
        assertEquals(MergeOutcome.KEPT, StepMerge.decide(existingSteps = 5, importedSteps = 0))
    }

    @Test
    fun firstImportOfAZeroStepDayIsInserted() {
        assertEquals(MergeOutcome.INSERTED, StepMerge.decide(existingSteps = null, importedSteps = 0))
    }
}
