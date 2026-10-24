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

    private fun sortServices(services: List<PaylaterService>, order: List<String>): List<PaylaterService> {
        if (order.isEmpty()) return services
        val orderMap = order.withIndex().associate { it.value to it.index }
        return services.sortedWith(
            compareBy<PaylaterService> { orderMap[it.id] ?: Int.MAX_VALUE }
                .thenBy { it.name }
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
        val sorted = sortServices(services, customOrder)

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
        val sorted = sortServices(services, customOrder)

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
        val sorted = sortServices(services, emptyList())

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

        val order = listOf("s2", "s1") // GoPayLater prioritized over SPayLater
        val orderMap = order.withIndex().associate { it.value to it.index }

        val list = listOf(s1, s2).sortedWith(
            compareBy<PaylaterService> { orderMap[it.id] ?: Int.MAX_VALUE }
                .thenBy { it.name }
        )

        assertEquals("GoPayLater", list[0].name)
        assertEquals("SPayLater", list[1].name)
    }
}
