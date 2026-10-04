package id.elclark.lunas.util

import id.elclark.lunas.model.PaylaterService

object AiPromptTemplates {

    fun generateExtractionPrompt(services: List<PaylaterService>): String {
        val currentYm = DateUtils.currentYearMonth()
        val servicesList = if (services.isNotEmpty()) {
            services.joinToString("\n") { svc ->
                "- ID: \"${svc.id}\" (Nama: ${svc.name})"
            }
        } else {
            "- ID: \"spaylater\" (Nama: SPayLater)\n- ID: \"gopaylater\" (Nama: GoPay Later)\n- ID: \"yup\" (Nama: Yup PayLater)"
        }

        val serviceIdsList = if (services.isNotEmpty()) {
            services.joinToString(", ") { "\"${it.id}\"" }
        } else {
            "\"spaylater\", \"gopaylater\", \"yup\""
        }

        val sampleServiceId = services.firstOrNull()?.id ?: "spaylater"

        return """
Kamu adalah asisten keuangan pribadi untuk aplikasi pelacak tagihan paylater Lunas.
Tugasmu adalah menganalisis gambar screenshot tagihan / rincian pembayaran paylater dan mengekstrak semua rincian tagihan menjadi JSON array murni.

DAFTAR LAYANAN PAYLATER YANG AKTIF DI APLIKASI SAYA:
$servicesList

PENTING: Gunakan salah satu ID layanan di atas untuk field "service" atau "serviceId" yang paling sesuai dengan gambar tagihan!

FORMAT OUTPUT (HANYA KELUARKAN JSON VALID TANPA TEKS LAIN, TANPA TANDA PETIK BACKTICK):
[
  {
    "service": "$sampleServiceId",
    "title": "Nama Barang / Transaksi",
    "amount": 100000,
    "tenor": 3,
    "month": "$currentYm",
    "math": "50000 + 50000"
  }
]

PANDUAN FIELD:
1. "service": Wajib menggunakan salah satu ID layanan: $serviceIdsList.
2. "title": Nama transaksi atau barang yang dibeli (misal: "Sepatu Running", "Belanja Bulanan").
3. "amount": Nominal tagihan per bulan dalam bentuk angka bulat (tanpa simbol Rp, tanpa titik/koma ribuan).
4. "tenor": Jumlah tenor bulan cicilan (1 untuk tempo bayar bulan depan / 1x, 2 untuk 2 bulan, 3 untuk 3 bulan, 6, 12, dst).
5. "month": Bulan mulai jatuh tempo tagihan pertama dalam format YYYY-MM (misal "$currentYm").
6. "math": (Opsional) Jika pada screenshot berupa gabungan beberapa transaksi atau hitungan cepat, tuliskan penjumlahan per transaksi (misal: "12000 + 35000").
""".trimIndent()
    }

    val LLM_EXTRACTION_PROMPT = generateExtractionPrompt(emptyList())
}
