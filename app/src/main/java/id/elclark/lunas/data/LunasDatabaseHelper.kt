package id.elclark.lunas.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import id.elclark.lunas.model.AppSettings
import id.elclark.lunas.model.BillItem
import id.elclark.lunas.model.PaylaterService
import id.elclark.lunas.model.PaymentStatus

class LunasDatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        const val DATABASE_NAME = "lunas.db"
        const val DATABASE_VERSION = 1

        // Services table
        const val TABLE_SERVICES = "services"
        const val COL_SERVICE_ID = "id"
        const val COL_SERVICE_NAME = "name"
        const val COL_SERVICE_CUTOFF = "billing_cutoff_day"
        const val COL_SERVICE_DUE = "due_day"
        const val COL_SERVICE_OFFSET = "due_month_offset"
        const val COL_SERVICE_COLOR = "color_hex"
        const val COL_SERVICE_ACTIVE = "is_active"

        // Bill items table
        const val TABLE_ITEMS = "bill_items"
        const val COL_ITEM_ID = "id"
        const val COL_ITEM_SERVICE_ID = "service_id"
        const val COL_ITEM_TITLE = "title"
        const val COL_ITEM_AMOUNT = "amount_per_month"
        const val COL_ITEM_TENOR = "total_tenor"
        const val COL_ITEM_START_YM = "start_year_month"
        const val COL_ITEM_MATH = "raw_math_expression"
        const val COL_ITEM_CREATED = "created_at"

        // Payment status table
        const val TABLE_PAYMENTS = "payment_statuses"
        const val COL_PAYMENT_SERVICE_ID = "service_id"
        const val COL_PAYMENT_YM = "year_month"
        const val COL_PAYMENT_IS_PAID = "is_paid"
        const val COL_PAYMENT_PAID_AT = "paid_at"

        // Settings table
        const val TABLE_SETTINGS = "settings"
        const val COL_SETTING_KEY = "key"
        const val COL_SETTING_VAL = "value"
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE $TABLE_SERVICES (
                $COL_SERVICE_ID TEXT PRIMARY KEY,
                $COL_SERVICE_NAME TEXT NOT NULL,
                $COL_SERVICE_CUTOFF INTEGER NOT NULL,
                $COL_SERVICE_DUE INTEGER NOT NULL,
                $COL_SERVICE_OFFSET INTEGER NOT NULL,
                $COL_SERVICE_COLOR INTEGER NOT NULL,
                $COL_SERVICE_ACTIVE INTEGER NOT NULL DEFAULT 1
            )
        """.trimIndent())

        db.execSQL("""
            CREATE TABLE $TABLE_ITEMS (
                $COL_ITEM_ID TEXT PRIMARY KEY,
                $COL_ITEM_SERVICE_ID TEXT NOT NULL,
                $COL_ITEM_TITLE TEXT NOT NULL,
                $COL_ITEM_AMOUNT INTEGER NOT NULL,
                $COL_ITEM_TENOR INTEGER NOT NULL DEFAULT 1,
                $COL_ITEM_START_YM TEXT NOT NULL,
                $COL_ITEM_MATH TEXT,
                $COL_ITEM_CREATED INTEGER NOT NULL
            )
        """.trimIndent())

        db.execSQL("""
            CREATE TABLE $TABLE_PAYMENTS (
                $COL_PAYMENT_SERVICE_ID TEXT NOT NULL,
                $COL_PAYMENT_YM TEXT NOT NULL,
                $COL_PAYMENT_IS_PAID INTEGER NOT NULL DEFAULT 0,
                $COL_PAYMENT_PAID_AT INTEGER,
                PRIMARY KEY ($COL_PAYMENT_SERVICE_ID, $COL_PAYMENT_YM)
            )
        """.trimIndent())

        db.execSQL("""
            CREATE TABLE $TABLE_SETTINGS (
                $COL_SETTING_KEY TEXT PRIMARY KEY,
                $COL_SETTING_VAL TEXT NOT NULL
            )
        """.trimIndent())

        // Seed default paylater services
        seedDefaultServices(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Future migrations
    }

    private fun seedDefaultServices(db: SQLiteDatabase) {
        val defaults = listOf(
            PaylaterService("spaylater", "SPayLater", 21, 1, 1, 0xFFEE4D2D), // Shopee orange
            PaylaterService("gopaylater", "GoPayLater", 15, 1, 1, 0xFF00AA13), // Gojek green
            PaylaterService("yup", "Yup", 13, 22, 0, 0xFFF5A623)               // Yup gold/yellow
        )

        for (svc in defaults) {
            val cv = ContentValues().apply {
                put(COL_SERVICE_ID, svc.id)
                put(COL_SERVICE_NAME, svc.name)
                put(COL_SERVICE_CUTOFF, svc.billingCutoffDay)
                put(COL_SERVICE_DUE, svc.dueDay)
                put(COL_SERVICE_OFFSET, svc.dueMonthOffset)
                put(COL_SERVICE_COLOR, svc.colorHex)
                put(COL_SERVICE_ACTIVE, 1)
            }
            db.insert(TABLE_SERVICES, null, cv)
        }
    }
    // CRUD for Services
    fun getAllServices(): List<PaylaterService> {
        val list = mutableListOf<PaylaterService>()
        val db = readableDatabase
        val cursor = db.query(TABLE_SERVICES, null, null, null, null, null, "$COL_SERVICE_NAME ASC")
        cursor.use { c ->
            while (c.moveToNext()) {
                list.add(
                    PaylaterService(
                        id = c.getString(c.getColumnIndexOrThrow(COL_SERVICE_ID)),
                        name = c.getString(c.getColumnIndexOrThrow(COL_SERVICE_NAME)),
                        billingCutoffDay = c.getInt(c.getColumnIndexOrThrow(COL_SERVICE_CUTOFF)),
                        dueDay = c.getInt(c.getColumnIndexOrThrow(COL_SERVICE_DUE)),
                        dueMonthOffset = c.getInt(c.getColumnIndexOrThrow(COL_SERVICE_OFFSET)),
                        colorHex = c.getLong(c.getColumnIndexOrThrow(COL_SERVICE_COLOR)),
                        isActive = c.getInt(c.getColumnIndexOrThrow(COL_SERVICE_ACTIVE)) == 1
                    )
                )
            }
        }
        return list
    }

    fun upsertService(service: PaylaterService) {
        val db = writableDatabase
        val cv = ContentValues().apply {
            put(COL_SERVICE_ID, service.id)
            put(COL_SERVICE_NAME, service.name)
            put(COL_SERVICE_CUTOFF, service.billingCutoffDay)
            put(COL_SERVICE_DUE, service.dueDay)
            put(COL_SERVICE_OFFSET, service.dueMonthOffset)
            put(COL_SERVICE_COLOR, service.colorHex)
            put(COL_SERVICE_ACTIVE, if (service.isActive) 1 else 0)
        }
        db.insertWithOnConflict(TABLE_SERVICES, null, cv, SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun deleteService(serviceId: String) {
        val db = writableDatabase
        db.delete(TABLE_SERVICES, "$COL_SERVICE_ID = ?", arrayOf(serviceId))
        db.delete(TABLE_ITEMS, "$COL_ITEM_SERVICE_ID = ?", arrayOf(serviceId))
        db.delete(TABLE_PAYMENTS, "$COL_PAYMENT_SERVICE_ID = ?", arrayOf(serviceId))
    }

    // CRUD for Bill Items
    fun getAllItems(): List<BillItem> {
        val list = mutableListOf<BillItem>()
        val db = readableDatabase
        val cursor = db.query(TABLE_ITEMS, null, null, null, null, null, "$COL_ITEM_CREATED DESC")
        cursor.use { c ->
            while (c.moveToNext()) {
                list.add(
                    BillItem(
                        id = c.getString(c.getColumnIndexOrThrow(COL_ITEM_ID)),
                        serviceId = c.getString(c.getColumnIndexOrThrow(COL_ITEM_SERVICE_ID)),
                        title = c.getString(c.getColumnIndexOrThrow(COL_ITEM_TITLE)),
                        amountPerMonth = c.getLong(c.getColumnIndexOrThrow(COL_ITEM_AMOUNT)),
                        totalTenor = c.getInt(c.getColumnIndexOrThrow(COL_ITEM_TENOR)),
                        startYearMonth = c.getString(c.getColumnIndexOrThrow(COL_ITEM_START_YM)),
                        rawMathExpression = c.getString(c.getColumnIndexOrThrow(COL_ITEM_MATH)),
                        createdAt = c.getLong(c.getColumnIndexOrThrow(COL_ITEM_CREATED))
                    )
                )
            }
        }
        return list
    }

    fun insertItem(item: BillItem) {
        val db = writableDatabase
        val cv = ContentValues().apply {
            put(COL_ITEM_ID, item.id)
            put(COL_ITEM_SERVICE_ID, item.serviceId)
            put(COL_ITEM_TITLE, item.title)
            put(COL_ITEM_AMOUNT, item.amountPerMonth)
            put(COL_ITEM_TENOR, item.totalTenor)
            put(COL_ITEM_START_YM, item.startYearMonth)
            put(COL_ITEM_MATH, item.rawMathExpression)
            put(COL_ITEM_CREATED, item.createdAt)
        }
        db.insertWithOnConflict(TABLE_ITEMS, null, cv, SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun deleteItem(itemId: String) {
        val db = writableDatabase
        db.delete(TABLE_ITEMS, "$COL_ITEM_ID = ?", arrayOf(itemId))
    }

    // CRUD for Payment Status
    fun getAllPaymentStatuses(): List<PaymentStatus> {
        val list = mutableListOf<PaymentStatus>()
        val db = readableDatabase
        val cursor = db.query(TABLE_PAYMENTS, null, null, null, null, null, null)
        cursor.use { c ->
            while (c.moveToNext()) {
                list.add(
                    PaymentStatus(
                        serviceId = c.getString(c.getColumnIndexOrThrow(COL_PAYMENT_SERVICE_ID)),
                        yearMonth = c.getString(c.getColumnIndexOrThrow(COL_PAYMENT_YM)),
                        isPaid = c.getInt(c.getColumnIndexOrThrow(COL_PAYMENT_IS_PAID)) == 1,
                        paidAt = if (c.isNull(c.getColumnIndexOrThrow(COL_PAYMENT_PAID_AT))) null else c.getLong(c.getColumnIndexOrThrow(COL_PAYMENT_PAID_AT))
                    )
                )
            }
        }
        return list
    }

    fun setPaymentStatus(serviceId: String, yearMonth: String, isPaid: Boolean) {
        val db = writableDatabase
        val cv = ContentValues().apply {
            put(COL_PAYMENT_SERVICE_ID, serviceId)
            put(COL_PAYMENT_YM, yearMonth)
            put(COL_PAYMENT_IS_PAID, if (isPaid) 1 else 0)
            put(COL_PAYMENT_PAID_AT, if (isPaid) System.currentTimeMillis() else null)
        }
        db.insertWithOnConflict(TABLE_PAYMENTS, null, cv, SQLiteDatabase.CONFLICT_REPLACE)
    }

    // Settings
    fun getSettings(): AppSettings {
        val db = readableDatabase
        var biometric = false
        var notifications = true
        var reminderDays = 2
        var reminderHour = 9

        val cursor = db.query(TABLE_SETTINGS, null, null, null, null, null, null)
        cursor.use { c ->
            while (c.moveToNext()) {
                val key = c.getString(c.getColumnIndexOrThrow(COL_SETTING_KEY))
                val value = c.getString(c.getColumnIndexOrThrow(COL_SETTING_VAL))
                when (key) {
                    "biometric_enabled" -> biometric = value.toBoolean()
                    "notifications_enabled" -> notifications = value.toBoolean()
                    "reminder_days" -> reminderDays = value.toIntOrNull() ?: 2
                    "reminder_hour" -> reminderHour = value.toIntOrNull() ?: 9
                }
            }
        }
        return AppSettings(biometric, notifications, reminderDays, reminderHour)
    }

    fun saveSettings(settings: AppSettings) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            saveSettingKeyVal(db, "biometric_enabled", settings.biometricEnabled.toString())
            saveSettingKeyVal(db, "notifications_enabled", settings.notificationsEnabled.toString())
            saveSettingKeyVal(db, "reminder_days", settings.reminderDaysBefore.toString())
            saveSettingKeyVal(db, "reminder_hour", settings.reminderHour.toString())
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    private fun saveSettingKeyVal(db: SQLiteDatabase, key: String, value: String) {
        val cv = ContentValues().apply {
            put(COL_SETTING_KEY, key)
            put(COL_SETTING_VAL, value)
        }
        db.insertWithOnConflict(TABLE_SETTINGS, null, cv, SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun clearAndRestoreAll(
        services: List<PaylaterService>,
        items: List<BillItem>,
        payments: List<PaymentStatus>,
        settings: AppSettings
    ) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            db.delete(TABLE_SERVICES, null, null)
            db.delete(TABLE_ITEMS, null, null)
            db.delete(TABLE_PAYMENTS, null, null)
            db.delete(TABLE_SETTINGS, null, null)

            for (s in services) upsertService(s)
            for (i in items) insertItem(i)
            for (p in payments) setPaymentStatus(p.serviceId, p.yearMonth, p.isPaid)
            saveSettings(settings)

            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }
}
