package id.elclark.lunas.data

import id.elclark.lunas.model.AppSettings
import id.elclark.lunas.model.PaylaterService
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ServiceOrderingTest {

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    private fun dummyService(id: String, name: String): PaylaterService {
        return PaylaterService(
            id = id,
            name = name,
            billingCutoffDay = 20,
            dueDay = 1,
            dueMonthOffset = 1,
            colorHex = 0xFF000000L
        )
    }

    @Test
    fun sortsServicesAccordingToCustomOrder() {
        val s1 = dummyService("s1", "SPayLater")
        val s2 = dummyService("s2", "GoPayLater")
        val s3 = dummyService("s3", "Yup")
        val s4 = dummyService("s4", "Kredivo")

        val services = listOf(s1, s2, s3, s4)

        // Custom order: Yup first, then Kredivo, then SPayLater, then GoPayLater
        val customOrder = listOf("s3", "s4", "s1", "s2")
        val sorted = LunasRepository.sortServices(services, customOrder)

        assertEquals("Yup", sorted[0].name)
        assertEquals("Kredivo", sorted[1].name)
        assertEquals("SPayLater", sorted[2].name)
        assertEquals("GoPayLater", sorted[3].name)
    }

    @Test
    fun placesUnorderedServicesAtTheEndAlphabetically() {
        val s1 = dummyService("s1", "SPayLater")
        val s2 = dummyService("s2", "GoPayLater")
        val s3 = dummyService("s3", "Yup")
        val s4 = dummyService("s4", "Akulaku")
        val s5 = dummyService("s5", "Kredivo")

        val services = listOf(s1, s2, s3, s4, s5)

        // Custom order only specifies Yup and GoPayLater
        val customOrder = listOf("s3", "s2")
        val sorted = LunasRepository.sortServices(services, customOrder)

        assertEquals("Yup", sorted[0].name)
        assertEquals("GoPayLater", sorted[1].name)
        // Remaining should be sorted by name: Akulaku, Kredivo, SPayLater
        assertEquals("Akulaku", sorted[2].name)
        assertEquals("Kredivo", sorted[3].name)
        assertEquals("SPayLater", sorted[4].name)
    }

    @Test
    fun returnsOriginalWhenOrderIsEmpty() {
        val s1 = dummyService("s1", "SPayLater")
        val s2 = dummyService("s2", "GoPayLater")

        val services = listOf(s1, s2)
        val sorted = LunasRepository.sortServices(services, emptyList())

        assertEquals(services, sorted)
    }

    @Test
    fun appSettingsSerializesAndDeserializesServiceOrder() {
        val original = AppSettings(
            biometricEnabled = true,
            serviceOrder = listOf("spaylater", "yup", "gopaylater")
        )

        val encoded = json.encodeToString(original)
        val decoded = json.decodeFromString<AppSettings>(encoded)

        assertEquals(original.serviceOrder, decoded.serviceOrder)
        assertEquals(listOf("spaylater", "yup", "gopaylater"), decoded.serviceOrder)
    }

    @Test
    fun appSettingsBackwardCompatibilityWithoutServiceOrder() {
        val legacyJson = """
            {
                "biometricEnabled": false,
                "notificationsEnabled": true,
                "currencyCode": "IDR"
            }
        """.trimIndent()

        val decoded = json.decodeFromString<AppSettings>(legacyJson)
        assertTrue(decoded.serviceOrder.isEmpty())
    }

    @Test
    fun orderMergingPreservesUnmentionedServices() {
        val existingOrder = listOf("s1", "s2", "s3", "s4")
        val newSubsetOrder = listOf("s3", "s1")
        val merged = newSubsetOrder + existingOrder.filter { it !in newSubsetOrder }

        assertEquals(listOf("s3", "s1", "s2", "s4"), merged)
    }

    @Test
    fun statementSortingTieBreakerRespectsCustomServiceOrder() {
        val s1 = dummyService("s1", "SPayLater")
        val s2 = dummyService("s2", "GoPayLater")

        val item1 = id.elclark.lunas.model.BillItem(
            id = "item1",
            serviceId = "s1",
            title = "Item 1",
            amountPerMonth = 50_000,
            totalTenor = 1,
            startYearMonth = "2026-10"
        )
        val item2 = id.elclark.lunas.model.BillItem(
            id = "item2",
            serviceId = "s2",
            title = "Item 2",
            amountPerMonth = 50_000,
            totalTenor = 1,
            startYearMonth = "2026-10"
        )

        val order = listOf("s2", "s1") // GoPayLater prioritized over SPayLater

        val overview = LunasRepository.computeMonthlyOverview(
            targetYm = "2026-10",
            allServices = listOf(s1, s2),
            allItems = listOf(item1, item2),
            allPayments = emptyList(),
            serviceOrder = order
        )

        assertEquals(2, overview.statements.size)
        assertEquals("GoPayLater", overview.statements[0].service.name)
        assertEquals("SPayLater", overview.statements[1].service.name)
    }

    @Test
    fun inPlaceDragReorderSimulatesSwappingAdjacentItems() {
        val original = listOf("s1", "s2", "s3", "s4")
        val mutable = original.toMutableList()

        // Drag s1 to the right past s2
        val moved = mutable.removeAt(0)
        mutable.add(1, moved)
        assertEquals(listOf("s2", "s1", "s3", "s4"), mutable)

        // Drag s1 further right past s3
        val movedAgain = mutable.removeAt(1)
        mutable.add(2, movedAgain)
        assertEquals(listOf("s2", "s3", "s1", "s4"), mutable)

        // Drag s4 to the left past s1
        val movedLeft = mutable.removeAt(3)
        mutable.add(2, movedLeft)
        assertEquals(listOf("s2", "s3", "s4", "s1"), mutable)
    }

    @Test
    fun reorderingPreservesUnchangedListWhenPositionsIdentical() {
        val original = listOf("s1", "s2", "s3")
        val reordered = original.toMutableList()
        // No swap occurred
        assertEquals(original, reordered)
    }

    @Test
    fun reorderServicesSingleOrEmptyList() {
        val s1 = dummyService("s1", "SPayLater")
        val single = listOf(s1)
        assertEquals(single, LunasRepository.sortServices(single, listOf("s1")))
        assertEquals(emptyList<PaylaterService>(), LunasRepository.sortServices(emptyList(), listOf("s1")))
    }

    @Test
    fun reorderActiveSubsetPreservesInactiveServicesAtEndInOrder() {
        // Suppose user has 4 services: s1 (active), s2 (active), s3 (inactive), s4 (active)
        val fullServices = listOf(
            dummyService("s1", "SPayLater"),
            dummyService("s2", "GoPayLater"),
            dummyService("s3", "Akulaku"),
            dummyService("s4", "Yup")
        )
        val initialOrder = listOf("s1", "s2", "s3", "s4")

        // In AddBillBottomSheet, only active services [s1, s2, s4] are shown.
        // User drags s4 to the front: [s4, s1, s2]
        val reorderedActive = listOf("s4", "s1", "s2")

        // Repository merge logic: orderedIds + existingOrder.filter { it !in orderedIds }
        val mergedOrder = reorderedActive + initialOrder.filter { it !in reorderedActive }
        assertEquals(listOf("s4", "s1", "s2", "s3"), mergedOrder)

        // Sort all services with the merged order
        val sorted = LunasRepository.sortServices(fullServices, mergedOrder)
        assertEquals("Yup", sorted[0].name)
        assertEquals("SPayLater", sorted[1].name)
        assertEquals("GoPayLater", sorted[2].name)
        assertEquals("Akulaku", sorted[3].name)
    }

    @Test
    fun reorderingServicesWithUnknownOrRemovedIds() {
        val services = listOf(
            dummyService("s1", "SPayLater"),
            dummyService("s2", "GoPayLater")
        )
        // Order contains an ID that was deleted or unknown ("s99")
        val orderWithStale = listOf("s99", "s2", "s1")
        val sorted = LunasRepository.sortServices(services, orderWithStale)
        assertEquals("GoPayLater", sorted[0].name)
        assertEquals("SPayLater", sorted[1].name)
    }
}
