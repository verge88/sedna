package com.sedna.words.data

import android.content.Context

object SeedCatalog {
    fun load(context: Context): List<WordDraft> =
        context.assets.open("starter_words.tsv").bufferedReader().useLines { lines ->
            lines.drop(1).mapNotNull { line ->
                val fields = line.split('\t')
                if (fields.size < 6) null else WordDraft(
                    level = fields[0], topic = fields[1],
                    term = fields[2], translation = fields[3],
                    definition = fields[4], example = fields[5]
                )
            }.toList()
        }
}
