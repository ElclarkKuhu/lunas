package id.elclark.lunas.data

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import id.elclark.lunas.model.BillItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

class DataImportReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val repository = LunasRepository.getInstance(context.applicationContext)
                when (action) {
                    "id.elclark.lunas.RESTORE_USER_DATA", "com.example.lunas.RESTORE_USER_DATA" -> {
                        val dbHelper = LunasDatabaseHelper(context.applicationContext)
                        val items = listOf(
                            BillItem(
                                id = "spaylater-cicilan-3x",
                                serviceId = "spaylater",
                                title = "Cicilan 3x",
                                amountPerMonth = 115688L,
                                totalTenor = 3,
                                startYearMonth = "2026-10",
                                rawMathExpression = null,
                                createdAt = 1727870400000L
                            ),
                            BillItem(
                                id = "spaylater-scratchpad-1",
                                serviceId = "spaylater",
                                title = "Belanjaan 10.10",
                                amountPerMonth = 116178L,
                                totalTenor = 1,
                                startYearMonth = "2026-10",
                                rawMathExpression = "12182 + 4252 + 1000 + 98744",
                                createdAt = 1727870400001L
                            ),
                            BillItem(
                                id = "gopaylater-cicilan-2x",
                                serviceId = "gopaylater",
                                title = "Cicilan GoPay 2x",
                                amountPerMonth = 31027L,
                                totalTenor = 2,
                                startYearMonth = "2026-10",
                                rawMathExpression = null,
                                createdAt = 1727870400002L
                            ),
                            BillItem(
                                id = "gopaylater-gofood",
                                serviceId = "gopaylater",
                                title = "GoFood",
                                amountPerMonth = 12049L,
                                totalTenor = 1,
                                startYearMonth = "2026-10",
                                rawMathExpression = null,
                                createdAt = 1727870400003L
                            ),
                            BillItem(
                                id = "yup-transaksi-1",
                                serviceId = "yup",
                                title = "Transaksi Yup",
                                amountPerMonth = 10573L,
                                totalTenor = 1,
                                startYearMonth = "2026-10",
                                rawMathExpression = null,
                                createdAt = 1727870400004L
                            )
                        )
                        for (item in items) {
                            dbHelper.insertItem(item)
                        }
                        repository.reloadAll()
                    }
                    "id.elclark.lunas.IMPORT_JSON", "com.example.lunas.IMPORT_JSON" -> {
                        val jsonString = intent.getStringExtra("json_string")
                        val filePath = intent.getStringExtra("json_file")
                        val jsonContent = when {
                            !jsonString.isNullOrBlank() -> jsonString
                            !filePath.isNullOrBlank() -> {
                                val file = File(filePath).canonicalFile
                                val filesDir = context.filesDir.canonicalFile
                                val cacheDir = context.cacheDir.canonicalFile
                                if ((file.startsWith(filesDir) || file.startsWith(cacheDir)) && file.exists() && file.isFile) {
                                    file.readText()
                                } else {
                                    null
                                }
                            }
                            else -> null
                        }
                        if (jsonContent != null) {
                            repository.importBackupJson(jsonContent)
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                pendingResult.finish()
            }
        }
    }
}
