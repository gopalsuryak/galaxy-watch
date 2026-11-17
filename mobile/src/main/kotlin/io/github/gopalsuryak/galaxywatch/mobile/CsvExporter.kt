package io.github.gopalsuryak.galaxywatch.mobile

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import io.github.gopalsuryak.galaxywatch.shared.HealthSample
import java.io.File
import java.time.Instant

object CsvExporter {

    fun toCsv(samples: List<HealthSample>): String = buildString {
        append("type,value,unit,start_utc,end_utc\n")
        for (s in samples) {
            append(s.type.name).append(',')
                .append(s.value).append(',')
                .append(s.type.unit).append(',')
                .append(Instant.ofEpochMilli(s.startMs)).append(',')
                .append(Instant.ofEpochMilli(s.endMs)).append('\n')
        }
    }

    /** Writes the CSV to app cache and returns a share sheet intent for it. */
    fun shareIntent(context: Context, samples: List<HealthSample>): Intent {
        val dir = File(context.cacheDir, "exports").apply { mkdirs() }
        val file = File(dir, "galaxy-watch-samples.csv").apply { writeText(toCsv(samples)) }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.exports", file)
        val send = Intent(Intent.ACTION_SEND)
            .setType("text/csv")
            .putExtra(Intent.EXTRA_STREAM, uri)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        return Intent.createChooser(send, "Export samples")
    }
}
