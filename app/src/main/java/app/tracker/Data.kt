package app.tracker

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.Locale

val TAGS = listOf("anime", "book", "manga", "vn", "movie", "series")

data class Item(
    val id: Long,
    val title: String,
    val tag: String,
    val creator: String = "",
    val publisher: String = "",
    val release: String = "",
    val start: String = "",
    val end: String = "",
    val labels: List<String> = emptyList(),
    val poster: String = "",
    val episodes: Int = 0
)

// ---------- colors ----------
class Palette(val name: String, val dark: Boolean, val c: List<Long>) {
    // c: background, surface, text, text-secondary, border, container, on-container, accent, on-accent
    fun scheme(): ColorScheme {
        val k = c.map { Color(it) }
        val b = if (dark) darkColorScheme() else lightColorScheme()
        return b.copy(
            background = k[0], surface = k[0], surfaceVariant = k[1],
            onBackground = k[2], onSurface = k[2], onSurfaceVariant = k[3],
            outline = k[4], outlineVariant = k[4],
            primaryContainer = k[5], secondaryContainer = k[5],
            onPrimaryContainer = k[6], onSecondaryContainer = k[6],
            primary = k[7], onPrimary = k[8],
            surfaceContainerLowest = k[0], surfaceContainerLow = k[0],
            surfaceContainer = k[1], surfaceContainerHigh = k[1], surfaceContainerHighest = k[1]
        )
    }
}

fun hx(s: String): Long? {
    var h = s.trim().removePrefix("#")
    if (h.length == 3) h = h.map { "$it$it" }.joinToString("")
    if (h.length != 6) return null
    return h.toLongOrNull(16)?.let { 0xFF000000 or it }
}

fun lum(c: Long): Double = (.299 * ((c shr 16) and 255) + .587 * ((c shr 8) and 255) + .114 * (c and 255)) / 255

private fun pal(n: String, d: Boolean, s: String) = Palette(n, d, s.split(" ").map { hx(it)!! })

val THEMES = mapOf(
    "light" to pal("Light", false, "FEF7FF FFFFFF 1D1B20 49454F CAC4D0 E8DEF8 1D192B 6750A4 FFFFFF"),
    "dark" to pal("Dark", true, "141218 211F26 E6E0E9 CAC4D0 49454F 4A4458 E8DEF8 D0BCFF 381E72"),
    "gruvbox" to pal("Gruvbox dark", true, "282828 3C3836 EBDBB2 A89984 504945 504945 EBDBB2 FABD2F 282828"),
    "solarized" to pal("Solarized dark", true, "002B36 073642 93A1A1 839496 586E75 0B4857 93A1A1 268BD2 002B36"),
    "oled" to pal("OLED", true, "000000 121212 E6E0E9 A8A2AD 2E2E2E 2B2735 E8DEF8 D0BCFF 381E72")
)

val THEME_KEYS = listOf("background", "surface", "text", "text-secondary", "border", "container", "on-container", "accent", "on-accent")

fun parseTheme(text: String): Palette? {
    val got = mutableMapOf<String, Long>()
    var name = ""
    val re = Regex("^([\\w-]+)\\s*[=:]\\s*(.+)$")
    text.removePrefix("\uFEFF").lines().forEach { raw ->
        val l = raw.trim()
        if (l.isEmpty() || l.startsWith("#") || l.startsWith("//")) return@forEach
        val m = re.find(l) ?: return@forEach
        val k = m.groupValues[1].lowercase()
        val v = m.groupValues[2].trim()
        if (k == "name") name = v.take(30) else if (k in THEME_KEYS) hx(v)?.let { got[k] = it }
    }
    if (got.isEmpty()) return null
    val base = THEMES[if ((got["background"]?.let { lum(it) } ?: 0.0) > .5) "light" else "dark"]!!
    val c = THEME_KEYS.mapIndexed { i, k -> got[k] ?: base.c[i] }
    return Palette(name.ifEmpty { "Custom" }, lum(c[0]) <= .5, c)
}

// ---------- strings (en / ru / de) ----------
private fun pk(s: String) = s.split("|").associate { val i = it.indexOf('='); it.substring(0, i) to it.substring(i + 1) }

val STR: Map<String, Map<String, String>> = mapOf(
    "en" to pk("""app=My tracker|search=Search|filters=Filters|reset=Reset|show=Show|add=Add|edit=Edit|delete=Delete entry|cancel=Cancel|ok=OK|back=Back|save=Save|title=Title|tag=Tag|custom=Custom tags|customPh=textbook, fiction|release=Release date|start=Start date|end=End date|from=From|to=To|found=Found|of=of|empty=Nothing found. Change the filters or add an entry.|poster=Add poster|newE=New entry|editE=Edit entry|stats=Statistics|settings=Settings|theme=Color scheme|th_system=Match system|th_light=Light|th_dark=Dark|themeImp=Import your own scheme|themeHelp=Text file, one line per element: element = #hex. Elements: background, surface, text, text-secondary, border, container, on-container, accent, on-accent. Optional: name = My scheme.|themeOk=Scheme applied: |themeBad=No valid colors found. Check the format: element = #hex.|language=Language|data=Data|export=Export to CSV|import=Import from CSV|csvHelp=Posters are not included. Imported entries are added to existing ones.|exOk=File saved. Entries: |imOk=Imported: |skipped=Skipped: |imBad=Could not read the file.|noTags=No custom tags yet|tagsHelp=Tapping a tag deletes it from all entries.|fcre=Author, director, developer|fpub=Publisher, studio|g_tag=By type|g_cust=By custom tags|m_done=Finished|m_all=All entries|noLb=No tags|other=Other|entries=entries|stEmpty=No data for the chart. Change the filters or add end dates.|stHint=Finished means entries with an end date.|n_anime=anime|n_book=book|n_manga=manga|n_vn=visual novel|c_anime=Director|c_book=Author|c_vn=Developer|p_anime=Studio|p_book=Publisher|p_vn=Publisher|s_anime=Started watching|s_book=Started reading|s_vn=Started playing|e_anime=Finished watching|e_book=Finished reading|e_vn=Finished playing|n_movie=movie|n_series=series|eps=Episodes watched|about=About|version=Version|relInfo=Release"""),
    "ru" to pk("""app=Мой трекер|search=Поиск|filters=Фильтры|reset=Сбросить|show=Показать|add=Добавить|edit=Изменить|delete=Удалить запись|cancel=Отмена|ok=ОК|back=Назад|save=Сохранить|title=Название|tag=Тег|custom=Свои теги|customPh=учебник, художка|release=Дата выхода|start=Дата начала|end=Дата окончания|from=От|to=До|found=Найдено|of=из|empty=Ничего не найдено. Измените фильтры или добавьте запись.|poster=Добавить постер|newE=Новая запись|editE=Изменить запись|stats=Статистика|settings=Настройки|theme=Цветовая схема|th_system=Как в системе|th_light=Светлая|th_dark=Тёмная|themeImp=Импорт своей схемы|themeHelp=Текстовый файл, по строке на элемент: элемент = #hex. Элементы: background, surface, text, text-secondary, border, container, on-container, accent, on-accent. Необязательно: name = Название.|themeOk=Схема применена: |themeBad=Не удалось прочитать цвета. Проверьте формат: элемент = #hex.|language=Язык|data=Данные|export=Экспорт в CSV|import=Импорт из CSV|csvHelp=Постеры в файл не попадают. Импортированные записи добавляются к текущим.|exOk=Файл сохранён. Записей: |imOk=Импортировано: |skipped=Пропущено: |imBad=Не удалось прочитать файл.|noTags=Своих тегов пока нет|tagsHelp=Нажатие на тег удаляет его из всех записей.|fcre=Автор, режиссёр, разработчик|fpub=Издатель, студия|g_tag=По типу|g_cust=По своим тегам|m_done=Завершённые|m_all=Все записи|noLb=Без тегов|other=Другое|entries=записей|stEmpty=Нет данных для диаграммы. Измените фильтры или добавьте даты окончания.|stHint=Завершённые — записи с датой окончания.|n_anime=аниме|n_book=книга|n_manga=манга|n_vn=виз новелла|c_anime=Режиссёр|c_book=Автор|c_vn=Разработчик|p_anime=Студия|p_book=Издательство|p_vn=Издатель|s_anime=Дата начала просмотра|s_book=Дата начала чтения|s_vn=Дата начала прохождения|e_anime=Дата окончания просмотра|e_book=Дата окончания чтения|e_vn=Дата окончания прохождения|n_movie=фильм|n_series=сериал|eps=Просмотрено серий|about=О программе|version=Версия|relInfo=Релиз"""),
    "de" to pk("""app=Mein Tracker|search=Suche|filters=Filter|reset=Zurücksetzen|show=Anzeigen|add=Hinzufügen|edit=Bearbeiten|delete=Eintrag löschen|cancel=Abbrechen|ok=OK|back=Zurück|save=Speichern|title=Titel|tag=Kategorie|custom=Eigene Tags|customPh=Lehrbuch, Belletristik|release=Erscheinungsdatum|start=Startdatum|end=Enddatum|from=Von|to=Bis|found=Gefunden|of=von|empty=Nichts gefunden. Filter ändern oder Eintrag hinzufügen.|poster=Poster hinzufügen|newE=Neuer Eintrag|editE=Eintrag bearbeiten|stats=Statistik|settings=Einstellungen|theme=Farbschema|th_system=Wie System|th_light=Hell|th_dark=Dunkel|themeImp=Eigenes Schema importieren|themeHelp=Textdatei, eine Zeile pro Element: Element = #hex. Elemente: background, surface, text, text-secondary, border, container, on-container, accent, on-accent. Optional: name = Mein Schema.|themeOk=Schema angewendet: |themeBad=Keine gültigen Farben gefunden. Format prüfen: Element = #hex.|language=Sprache|data=Daten|export=Als CSV exportieren|import=Aus CSV importieren|csvHelp=Poster sind nicht enthalten. Importierte Einträge werden zu den vorhandenen hinzugefügt.|exOk=Datei gespeichert. Einträge: |imOk=Importiert: |skipped=Übersprungen: |imBad=Datei konnte nicht gelesen werden.|noTags=Noch keine eigenen Tags|tagsHelp=Tippen auf einen Tag löscht ihn aus allen Einträgen.|fcre=Autor, Regie, Entwickler|fpub=Verlag, Studio|g_tag=Nach Kategorie|g_cust=Nach eigenen Tags|m_done=Abgeschlossen|m_all=Alle Einträge|noLb=Ohne Tags|other=Sonstige|entries=Einträge|stEmpty=Keine Daten für das Diagramm. Filter ändern oder Enddaten ergänzen.|stHint=Abgeschlossen bedeutet: Einträge mit Enddatum.|n_anime=Anime|n_book=Buch|n_manga=Manga|n_vn=Visual Novel|c_anime=Regie|c_book=Autor|c_vn=Entwickler|p_anime=Studio|p_book=Verlag|p_vn=Publisher|s_anime=Angefangen zu schauen|s_book=Lesebeginn|s_vn=Spielbeginn|e_anime=Fertig geschaut|e_book=Lesende|e_vn=Spielende|n_movie=Film|n_series=Serie|eps=Gesehene Folgen|about=Über die App|version=Version|relInfo=Release""")
)

fun t(k: String): String = STR[Repo.lang]?.get(k) ?: STR["en"]!![k] ?: k
fun lab(p: String, tag: String): String {
    val k = when (tag) {
        "manga" -> "book"
        "movie", "series" -> "anime"
        else -> tag
    }
    return t(p + "_" + k)
}

// ---------- storage ----------
object Repo {
    val items = mutableStateListOf<Item>()
    var theme by mutableStateOf("system")
    var lang by mutableStateOf("en")
    var custom by mutableStateOf<Palette?>(null)
    private var file: File? = null

    fun init(c: Context) {
        if (file != null) return
        val f = File(c.filesDir, "data.json")
        file = f
        lang = Locale.getDefault().language.let { if (it == "ru" || it == "de") it else "en" }
        runCatching {
            val o = JSONObject(f.readText())
            val a = o.getJSONArray("items")
            for (n in 0 until a.length()) items.add(fromJson(a.getJSONObject(n)))
            theme = o.optString("theme", "system")
            lang = o.optString("lang", lang)
            o.optJSONObject("custom")?.let { j ->
                val cc = j.getJSONArray("c")
                custom = Palette(j.getString("name"), j.getBoolean("dark"), (0 until cc.length()).map { cc.getLong(it) })
            }
        }
    }

    private fun fromJson(j: JSONObject): Item {
        val l = j.optJSONArray("labels")
        return Item(
            j.getLong("id"), j.getString("title"), j.getString("tag"),
            j.optString("creator"), j.optString("publisher"), j.optString("release"),
            j.optString("start"), j.optString("end"),
            (0 until (l?.length() ?: 0)).map { l!!.getString(it) }, j.optString("poster"), j.optInt("episodes", 0)
        )
    }

    fun save() {
        val a = JSONArray()
        items.forEach { i ->
            a.put(
                JSONObject().put("id", i.id).put("title", i.title).put("tag", i.tag)
                    .put("creator", i.creator).put("publisher", i.publisher).put("release", i.release)
                    .put("start", i.start).put("end", i.end).put("labels", JSONArray(i.labels)).put("poster", i.poster).put("episodes", i.episodes)
            )
        }
        val o = JSONObject().put("items", a).put("theme", theme).put("lang", lang)
        custom?.let { o.put("custom", JSONObject().put("name", it.name).put("dark", it.dark).put("c", JSONArray(it.c))) }
        file?.writeText(o.toString())
    }

    fun upsert(x: Item) {
        val i = items.indexOfFirst { it.id == x.id }
        if (i >= 0) items[i] = x else items.add(0, x)
        save()
    }

    fun delete(x: Item) {
        items.removeAll { it.id == x.id }
        if (x.poster.isNotEmpty()) File(x.poster).delete()
        save()
    }

    fun labels(): List<String> = items.flatMap { it.labels }.distinct()

    fun deleteLabel(l: String) {
        for (i in items.indices) items[i] = items[i].copy(labels = items[i].labels.filter { it != l })
        Flt.lb.remove(l)
        save()
    }
}

// ---------- filters ----------
object Flt {
    var q by mutableStateOf("")
    val tags = mutableStateListOf<String>()
    val cr = mutableStateListOf<String>()
    val pb = mutableStateListOf<String>()
    val lb = mutableStateListOf<String>()
    val rel = mutableStateListOf("", "")
    val st = mutableStateListOf("", "")
    val en = mutableStateListOf("", "")

    fun count(): Int = tags.size + cr.size + pb.size + lb.size + (rel + st + en).count { it.isNotEmpty() }

    fun reset() {
        tags.clear(); cr.clear(); pb.clear(); lb.clear()
        for (r in listOf(rel, st, en)) { r[0] = ""; r[1] = "" }
    }

    private fun rng(v: String, r: List<String>) = (r[0].isEmpty() || v >= r[0]) && (r[1].isEmpty() || v <= r[1])

    fun ok(i: Item) =
        (tags.isEmpty() || i.tag in tags) && (cr.isEmpty() || i.creator in cr) &&
            (pb.isEmpty() || i.publisher in pb) && (lb.isEmpty() || i.labels.any { it in lb }) &&
            rng(i.release, rel) && rng(i.start, st) && rng(i.end, en) &&
            i.title.contains(q.trim(), ignoreCase = true)
}

fun visible(): List<Item> = Repo.items.filter { Flt.ok(it) }

fun parseLabels(s: String): List<String> =
    s.split(",", ";").map { it.trim().take(30) }.filter { it.isNotEmpty() }.distinctBy { it.lowercase() }.take(10)

// ---------- poster ----------
fun savePoster(c: Context, u: Uri): String {
    val o = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    c.contentResolver.openInputStream(u)?.use { BitmapFactory.decodeStream(it, null, o) }
    var s = 1
    while (maxOf(o.outWidth, o.outHeight) / s > 1200) s *= 2
    val b = c.contentResolver.openInputStream(u)?.use {
        BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = s })
    } ?: return ""
    val f = File(c.filesDir, "poster_${System.currentTimeMillis()}.jpg")
    f.outputStream().use { b.compress(Bitmap.CompressFormat.JPEG, 82, it) }
    return f.absolutePath
}

// ---------- CSV ----------
private fun q(v: String) =
    if (v.any { it == ';' || it == '"' || it == '\n' || it == '\r' }) "\"" + v.replace("\"", "\"\"") + "\"" else v

fun toCsv(): String {
    val head = listOf("Title", "Tag", "Creator", "Publisher", "Release", "Start", "End", "Custom tags", "Episodes")
    val rows = Repo.items.map {
        listOf(it.title, t("n_" + it.tag), it.creator, it.publisher, it.release, it.start, it.end, it.labels.joinToString(", "), if (it.episodes > 0) it.episodes.toString() else "")
    }
    return "\uFEFF" + (listOf(head) + rows).joinToString("\r\n") { r -> r.joinToString(";") { q(it) } }
}

fun csvParse(s0: String, d: Char): List<List<String>> {
    val s = s0.removePrefix("\uFEFF")
    val rows = mutableListOf<List<String>>()
    var row = mutableListOf<String>()
    val f = StringBuilder()
    var inQ = false
    var i = 0
    while (i < s.length) {
        val c = s[i]
        if (inQ) {
            if (c == '"') {
                if (i + 1 < s.length && s[i + 1] == '"') { f.append('"'); i++ } else inQ = false
            } else f.append(c)
        } else if (c == '"') inQ = true
        else if (c == d) { row.add(f.toString()); f.clear() }
        else if (c == '\n' || c == '\r') {
            if (c == '\r' && i + 1 < s.length && s[i + 1] == '\n') i++
            row.add(f.toString()); f.clear(); rows.add(row); row = mutableListOf()
        } else f.append(c)
        i++
    }
    if (f.isNotEmpty() || row.isNotEmpty()) { row.add(f.toString()); rows.add(row) }
    return rows
}

private fun nd(s: String): String {
    val v = s.trim()
    Regex("^(\\d{1,2})\\.(\\d{1,2})\\.(\\d{4})$").find(v)?.let { m ->
        return "%s-%02d-%02d".format(m.groupValues[3], m.groupValues[2].toInt(), m.groupValues[1].toInt())
    }
    return if (Regex("^\\d{4}-\\d{2}-\\d{2}$").matches(v)) v else ""
}

fun importCsv(s: String): String {
    val h = s.lineSequence().firstOrNull() ?: ""
    val d = if (h.count { it == ';' } >= h.count { it == ',' }) ';' else ','
    val rs = csvParse(s, d)
    if (rs.size < 2 || rs[0].size < 2) return t("imBad")
    val map = HashMap<String, String>()
    TAGS.forEach { k -> map[k] = k; STR.values.forEach { m -> map[m["n_$k"]!!.lowercase()] = k } }
    var skip = 0
    val add = mutableListOf<Item>()
    rs.drop(1).forEachIndexed { n, r ->
        val title = r.getOrElse(0) { "" }.trim()
        val k = map[r.getOrElse(1) { "" }.trim().lowercase()]
        if (title.isEmpty() || k == null) {
            if (r.any { it.isNotBlank() }) skip++
        } else add.add(
            Item(
                System.currentTimeMillis() + n, title, k,
                r.getOrElse(2) { "" }.trim(), r.getOrElse(3) { "" }.trim(),
                nd(r.getOrElse(4) { "" }), nd(r.getOrElse(5) { "" }), nd(r.getOrElse(6) { "" }),
                parseLabels(r.getOrElse(7) { "" }),
                episodes = if (k == "series") (r.getOrElse(8) { "" }.trim().toIntOrNull() ?: 0) else 0
            )
        )
    }
    Repo.items.addAll(0, add)
    Repo.save()
    return t("imOk") + add.size + (if (skip > 0) ". " + t("skipped") + skip else "")
}
