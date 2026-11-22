@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package app.tracker

import android.app.Activity
import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlin.math.roundToInt

// ---------- app shell ----------
@Composable
fun AppTheme(content: @Composable () -> Unit) {
    val sys = isSystemInDarkTheme()
    val pal = when (Repo.theme) {
        "system" -> THEMES[if (sys) "dark" else "light"]!!
        "custom" -> Repo.custom ?: THEMES["light"]!!
        else -> THEMES[Repo.theme] ?: THEMES["light"]!!
    }
    val view = LocalView.current
    SideEffect {
        (view.context as? Activity)?.window?.let {
            WindowCompat.getInsetsController(it, view).isAppearanceLightStatusBars = !pal.dark
        }
    }
    MaterialTheme(colorScheme = pal.scheme(), content = content)
}

@Composable
fun TrackerApp() {
    AppTheme {
        var screen by rememberSaveable { mutableStateOf("list") }
        var sel by rememberSaveable { mutableStateOf(0L) }
        var sheet by rememberSaveable { mutableStateOf(false) }
        BackHandler(screen != "list") { screen = if (screen == "form" && sel != 0L) "detail" else if (screen == "about") "settings" else "list" }
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            when (screen) {
                "detail" -> DetailScreen(sel, { screen = "list" }, { screen = "form" })
                "form" -> FormScreen(sel, { screen = if (sel != 0L) "detail" else "list" }) { sel = it; screen = "detail" }
                "settings" -> SettingsScreen({ screen = "list" }) { screen = "about" }
                "about" -> AboutScreen { screen = "settings" }
                "stats" -> StatsScreen({ screen = "list" }) { sheet = true }
                else -> ListScreen({ sel = it; screen = "detail" }, { sel = 0L; screen = "form" }, { screen = it }) { sheet = true }
            }
        }
        if (sheet) FilterSheet { sheet = false }
    }
}

@Composable
fun Bar(title: String, back: (() -> Unit)? = null, actions: @Composable RowScope.() -> Unit = {}) {
    TopAppBar(
        title = { Text(title) },
        navigationIcon = {
            if (back != null) IconButton(back) { Icon(Icons.AutoMirrored.Filled.ArrowBack, t("back")) }
        },
        actions = actions
    )
}

@Composable
fun FilterBtn(click: () -> Unit) {
    val n = Flt.count()
    IconButton(click) {
        BadgedBox(badge = { if (n > 0) Badge { Text("$n") } }) { Icon(Icons.Default.FilterList, t("filters")) }
    }
}

// ---------- small pieces ----------
private val COVERS = mapOf(
    "anime" to (Color(0xFFCECBF6) to Color(0xFF3C3489)),
    "book" to (Color(0xFF9FE1CB) to Color(0xFF085041)),
    "manga" to (Color(0xFFF5C4B3) to Color(0xFF712B13)),
    "vn" to (Color(0xFFF4C0D1) to Color(0xFF72243E)),
    "movie" to (Color(0xFFB5D4F4) to Color(0xFF0C447C)),
    "series" to (Color(0xFFFAC775) to Color(0xFF633806))
)

private fun bmp(path: String) = if (path.isEmpty()) null else BitmapFactory.decodeFile(path)?.asImageBitmap()

@Composable
fun Cover(i: Item, m: Modifier) {
    val b = remember(i.poster) { bmp(i.poster) }
    val c = COVERS[i.tag] ?: COVERS["book"]!!
    Box(m.clip(RoundedCornerShape(12.dp)).background(if (b != null) MaterialTheme.colorScheme.surfaceVariant else c.first), contentAlignment = Alignment.Center) {
        if (b != null) Image(b, null, Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
        else Text(i.title.take(1), color = c.second, fontSize = 44.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun Chip(text: String, accent: Boolean = false) {
    val cs = MaterialTheme.colorScheme
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (accent) cs.primaryContainer else Color.Transparent,
        contentColor = if (accent) cs.onPrimaryContainer else cs.onSurfaceVariant,
        border = if (accent) null else BorderStroke(1.dp, cs.outline)
    ) { Text(text, Modifier.padding(horizontal = 10.dp, vertical = 3.dp), style = MaterialTheme.typography.labelMedium) }
}

@Composable
fun Chips(i: Item) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Chip(t("n_" + i.tag))
        i.labels.forEach { Chip(it, true) }
    }
}

private fun ms(v: String): Long? =
    runCatching { LocalDate.parse(v).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli() }.getOrNull()

private fun iso(m: Long): String = Instant.ofEpochMilli(m).atZone(ZoneOffset.UTC).toLocalDate().toString()

@Composable
fun DateField(label: String, v: String, on: (String) -> Unit, mod: Modifier = Modifier) {
    var open by remember { mutableStateOf(false) }
    Box(mod) {
        OutlinedTextField(
            value = v, onValueChange = {}, readOnly = true, singleLine = true,
            label = { Text(label) }, modifier = Modifier.fillMaxWidth(),
            trailingIcon = { if (v.isNotEmpty()) IconButton({ on("") }) { Icon(Icons.Default.Close, null) } }
        )
        Box(Modifier.matchParentSize().padding(end = if (v.isNotEmpty()) 48.dp else 0.dp).clickable { open = true })
    }
    if (open) {
        val st = rememberDatePickerState(initialSelectedDateMillis = ms(v))
        DatePickerDialog(
            onDismissRequest = { open = false },
            confirmButton = { TextButton({ st.selectedDateMillis?.let { on(iso(it)) }; open = false }) { Text(t("ok")) } }
        ) { DatePicker(st) }
    }
}

// ---------- list ----------
@Composable
fun ListScreen(open: (Long) -> Unit, add: () -> Unit, nav: (String) -> Unit, filters: () -> Unit) {
    var searching by rememberSaveable { mutableStateOf(false) }
    val r = visible()
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (searching) TextField(
                        value = Flt.q, onValueChange = { Flt.q = it }, singleLine = true,
                        placeholder = { Text(t("title")) },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent
                        )
                    ) else Text(t("app"))
                },
                actions = {
                    IconButton({ searching = !searching; if (!searching) Flt.q = "" }) {
                        Icon(if (searching) Icons.Default.Close else Icons.Default.Search, t("search"))
                    }
                    FilterBtn(filters)
                    IconButton({ nav("stats") }) { Icon(Icons.Default.PieChart, t("stats")) }
                    IconButton({ nav("settings") }) { Icon(Icons.Default.Settings, t("settings")) }
                }
            )
        },
        floatingActionButton = { FloatingActionButton(add) { Icon(Icons.Default.Add, t("add")) } }
    ) { pad ->
        Column(Modifier.padding(pad)) {
            Text(
                "${t("found")}: ${r.size} ${t("of")} ${Repo.items.size}",
                Modifier.padding(horizontal = 16.dp), style = MaterialTheme.typography.bodySmall
            )
            if (r.isEmpty()) Text(t("empty"), Modifier.padding(32.dp))
            else LazyVerticalGrid(
                GridCells.Fixed(2),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(r, key = { it.id }) { i ->
                    Column(Modifier.clickable { open(i.id) }) {
                        Cover(i, Modifier.fillMaxWidth().height(150.dp))
                        Text(
                            i.title, Modifier.padding(top = 8.dp, bottom = 4.dp),
                            fontWeight = FontWeight.Medium, maxLines = 2, overflow = TextOverflow.Ellipsis
                        )
                        Chips(i)
                    }
                }
            }
        }
    }
}

// ---------- detail ----------
@Composable
fun DetailScreen(id: Long, back: () -> Unit, edit: () -> Unit) {
    val i = Repo.items.firstOrNull { it.id == id }
    if (i == null) { LaunchedEffect(Unit) { back() }; return }
    var ask by remember { mutableStateOf(false) }
    Scaffold(topBar = { Bar("", back) { IconButton(edit) { Icon(Icons.Default.Edit, t("edit")) } } }) { pad ->
        Column(Modifier.padding(pad).padding(16.dp).verticalScroll(rememberScrollState())) {
            Cover(i, Modifier.fillMaxWidth().height(320.dp))
            Text(i.title, Modifier.padding(top = 16.dp, bottom = 8.dp), style = MaterialTheme.typography.headlineSmall)
            Chips(i)
            Spacer(Modifier.height(8.dp))
            listOf(
                lab("c", i.tag) to i.creator, lab("p", i.tag) to i.publisher, t("release") to i.release,
                lab("s", i.tag) to i.start, lab("e", i.tag) to i.end
            ).plus(if (i.tag == "series") listOf(t("eps") to i.episodes.toString()) else emptyList()).forEach { (k, v) ->
                Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), Arrangement.SpaceBetween) {
                    Text(k, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(v.ifEmpty { "—" })
                }
                HorizontalDivider()
            }
            OutlinedButton({ ask = true }, Modifier.fillMaxWidth().padding(top = 16.dp)) { Text(t("delete")) }
        }
    }
    if (ask) AlertDialog(
        onDismissRequest = { ask = false },
        title = { Text(t("delete")) },
        text = { Text(i.title) },
        confirmButton = { TextButton({ ask = false; Repo.delete(i) }) { Text(t("delete")) } },
        dismissButton = { TextButton({ ask = false }) { Text(t("cancel")) } }
    )
}

// ---------- form ----------
@Composable
fun FormScreen(id: Long, back: () -> Unit, saved: (Long) -> Unit) {
    val ctx = LocalContext.current
    val o = Repo.items.firstOrNull { it.id == id }
    var title by rememberSaveable { mutableStateOf(o?.title ?: "") }
    var tag by rememberSaveable { mutableStateOf(o?.tag ?: "anime") }
    var cr by rememberSaveable { mutableStateOf(o?.creator ?: "") }
    var pb by rememberSaveable { mutableStateOf(o?.publisher ?: "") }
    var rel by rememberSaveable { mutableStateOf(o?.release ?: "") }
    var st by rememberSaveable { mutableStateOf(o?.start ?: "") }
    var en by rememberSaveable { mutableStateOf(o?.end ?: "") }
    var lb by rememberSaveable { mutableStateOf(o?.labels?.joinToString(", ") ?: "") }
    var img by rememberSaveable { mutableStateOf(o?.poster ?: "") }
    var eps by rememberSaveable { mutableStateOf(o?.episodes?.takeIf { it > 0 }?.toString() ?: "") }
    var err by remember { mutableStateOf(false) }
    val pick = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { u: Uri? ->
        if (u != null) img = savePoster(ctx, u)
    }
    Scaffold(topBar = { Bar(t(if (o == null) "newE" else "editE"), back) }) { pad ->
        Column(
            Modifier.padding(pad).padding(horizontal = 16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                Modifier.align(Alignment.CenterHorizontally).size(130.dp, 190.dp).clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant).clickable { pick.launch("image/*") },
                contentAlignment = Alignment.Center
            ) {
                val b = remember(img) { bmp(img) }
                if (b != null) Image(b, null, Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
                else Text(t("poster"), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            OutlinedTextField(
                title, { title = it; err = false }, Modifier.fillMaxWidth(),
                label = { Text(t("title")) }, isError = err, singleLine = true
            )
            Text(t("tag"), style = MaterialTheme.typography.labelLarge)
            FlowRow {
                TAGS.forEach { k ->
                    FilterChip(selected = tag == k, onClick = { tag = k }, label = { Text(t("n_$k")) }, modifier = Modifier.padding(end = 6.dp))
                }
            }
            OutlinedTextField(cr, { cr = it }, Modifier.fillMaxWidth(), label = { Text(lab("c", tag)) }, singleLine = true)
            OutlinedTextField(pb, { pb = it }, Modifier.fillMaxWidth(), label = { Text(lab("p", tag)) }, singleLine = true)
            DateField(t("release"), rel, { rel = it })
            DateField(lab("s", tag), st, { st = it })
            DateField(lab("e", tag), en, { en = it })
            if (tag == "series") OutlinedTextField(
                eps, { s -> eps = s.filter { it.isDigit() }.take(5) }, Modifier.fillMaxWidth(),
                label = { Text(t("eps")) }, singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
            OutlinedTextField(
                lb, { lb = it }, Modifier.fillMaxWidth(),
                label = { Text(t("custom")) }, placeholder = { Text(t("customPh")) }, singleLine = true
            )
            val cur = parseLabels(lb)
            FlowRow {
                Repo.labels().filter { l -> cur.none { it.equals(l, true) } }.forEach { l ->
                    SuggestionChip(
                        onClick = { lb = if (lb.isBlank()) l else lb.trim().trimEnd(',', ';') + ", " + l },
                        label = { Text(l) }, modifier = Modifier.padding(end = 6.dp)
                    )
                }
            }
            Button(
                {
                    if (title.isBlank()) err = true
                    else {
                        val n = Item(if (o == null) System.currentTimeMillis() else id, title.trim(), tag, cr.trim(), pb.trim(), rel, st, en, parseLabels(lb), img, if (tag == "series") (eps.toIntOrNull() ?: 0) else 0)
                        Repo.upsert(n)
                        saved(n.id)
                    }
                },
                Modifier.fillMaxWidth().padding(vertical = 16.dp)
            ) { Text(t("save")) }
        }
    }
}

// ---------- filters ----------
@Composable
fun Group(title: String, vals: List<String>, sel: MutableList<String>, lab: (String) -> String) {
    if (vals.isEmpty()) return
    Text(title, Modifier.padding(top = 12.dp, bottom = 4.dp), style = MaterialTheme.typography.labelLarge)
    FlowRow {
        vals.forEach { v ->
            FilterChip(
                selected = v in sel, onClick = { if (v in sel) sel.remove(v) else sel.add(v) },
                label = { Text(lab(v)) }, modifier = Modifier.padding(end = 6.dp)
            )
        }
    }
}

@Composable
fun Range(title: String, r: MutableList<String>) {
    Text(title, Modifier.padding(top = 12.dp, bottom = 4.dp), style = MaterialTheme.typography.labelLarge)
    Row {
        DateField(t("from"), r[0], { r[0] = it }, Modifier.weight(1f))
        Spacer(Modifier.width(8.dp))
        DateField(t("to"), r[1], { r[1] = it }, Modifier.weight(1f))
    }
}

@Composable
fun FilterSheet(close: () -> Unit) {
    ModalBottomSheet(onDismissRequest = close, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).navigationBarsPadding()) {
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                Text(t("filters"), style = MaterialTheme.typography.titleLarge)
                TextButton({ Flt.reset() }) { Text(t("reset")) }
            }
            Group(t("tag"), TAGS, Flt.tags) { t("n_$it") }
            Group(t("fcre"), Repo.items.map { it.creator }.filter { it.isNotEmpty() }.distinct(), Flt.cr) { it }
            Group(t("fpub"), Repo.items.map { it.publisher }.filter { it.isNotEmpty() }.distinct(), Flt.pb) { it }
            Group(t("custom"), Repo.labels(), Flt.lb) { it }
            Range(t("release"), Flt.rel)
            Range(t("start"), Flt.st)
            Range(t("end"), Flt.en)
            Button(close, Modifier.fillMaxWidth().padding(vertical = 16.dp)) { Text("${t("show")} (${visible().size})") }
        }
    }
}

// ---------- statistics ----------
private val PAL = listOf(0xFF7F77DD, 0xFF1D9E75, 0xFFD85A30, 0xFFD4537E, 0xFF378ADD, 0xFFBA7517, 0xFF639922, 0xFF888780).map { Color(it) }

@Composable
fun StatsScreen(back: () -> Unit, filters: () -> Unit) {
    var by by rememberSaveable { mutableStateOf("tag") }
    var mode by rememberSaveable { mutableStateOf("done") }
    val r = visible().filter { mode == "all" || it.end.isNotEmpty() }
    val cnt = mutableMapOf<String, Int>()
    r.forEach { i ->
        val ks = if (by == "tag") listOf(t("n_" + i.tag)) else i.labels.ifEmpty { listOf(t("noLb")) }
        ks.forEach { cnt[it] = (cnt[it] ?: 0) + 1 }
    }
    val sorted = cnt.entries.sortedByDescending { it.value }
    val data = sorted.take(7).map { it.key to it.value } +
        (if (sorted.size > 7) listOf(t("other") to sorted.drop(7).sumOf { it.value }) else emptyList())
    val sum = data.sumOf { it.second }
    Scaffold(topBar = { Bar(t("stats"), back) { FilterBtn(filters) } }) { pad ->
        Column(Modifier.padding(pad).padding(16.dp).verticalScroll(rememberScrollState())) {
            FlowRow {
                FilterChip(selected = by == "tag", onClick = { by = "tag" }, label = { Text(t("g_tag")) }, modifier = Modifier.padding(end = 6.dp))
                FilterChip(selected = by == "cust", onClick = { by = "cust" }, label = { Text(t("g_cust")) }, modifier = Modifier.padding(end = 6.dp))
            }
            FlowRow {
                FilterChip(selected = mode == "done", onClick = { mode = "done" }, label = { Text(t("m_done")) }, modifier = Modifier.padding(end = 6.dp))
                FilterChip(selected = mode == "all", onClick = { mode = "all" }, label = { Text(t("m_all")) }, modifier = Modifier.padding(end = 6.dp))
            }
            if (sum == 0) Text(t("stEmpty"), Modifier.padding(vertical = 32.dp))
            else {
                Box(Modifier.align(Alignment.CenterHorizontally).padding(vertical = 16.dp).size(220.dp), contentAlignment = Alignment.Center) {
                    Canvas(Modifier.fillMaxSize()) {
                        val w = 34.dp.toPx()
                        var a = -90f
                        data.forEachIndexed { i, (_, v) ->
                            val sw = 360f * v / sum
                            drawArc(
                                PAL[i], a, maxOf(sw - (if (data.size > 1) 2f else 0f), 0.5f), false,
                                Offset(w / 2, w / 2), Size(size.width - w, size.height - w), style = Stroke(w)
                            )
                            a += sw
                        }
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("${r.size}", style = MaterialTheme.typography.headlineMedium)
                        Text(t("entries"), style = MaterialTheme.typography.bodySmall)
                    }
                }
                data.forEachIndexed { i, (name, v) ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(10.dp).clip(CircleShape).background(PAL[i]))
                            Spacer(Modifier.width(8.dp))
                            Text(name)
                        }
                        Text("$v (${(v * 100f / sum).roundToInt()}%)")
                    }
                    HorizontalDivider()
                }
                if (mode == "done") Text(t("stHint"), Modifier.padding(top = 12.dp), style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

// ---------- settings ----------
private const val EXAMPLE = "name = My scheme\nbackground = #1e1e2e\nsurface = #313244\ntext = #cdd6f4\ntext-secondary = #a6adc8\nborder = #45475a\ncontainer = #45475a\non-container = #cdd6f4\naccent = #89b4fa\non-accent = #1e1e2e"

private fun readText(c: Context, u: Uri): String =
    c.contentResolver.openInputStream(u)?.use { it.readBytes().toString(Charsets.UTF_8) } ?: ""

@Composable
private fun H(s: String) = Text(s, Modifier.padding(top = 20.dp, bottom = 8.dp), style = MaterialTheme.typography.titleMedium)

@Composable
fun SettingsScreen(back: () -> Unit, about: () -> Unit) {
    val ctx = LocalContext.current
    var msg by remember { mutableStateOf("") }
    val impTheme = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { u: Uri? ->
        if (u != null) {
            val p = parseTheme(readText(ctx, u))
            if (p == null) msg = t("themeBad")
            else { Repo.custom = p; Repo.theme = "custom"; Repo.save(); msg = t("themeOk") + p.name }
        }
    }
    val exp = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { u: Uri? ->
        if (u != null) {
            ctx.contentResolver.openOutputStream(u)?.use { it.write(toCsv().toByteArray(Charsets.UTF_8)) }
            msg = t("exOk") + Repo.items.size
        }
    }
    val imp = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { u: Uri? ->
        if (u != null) msg = importCsv(readText(ctx, u))
    }
    Scaffold(topBar = { Bar(t("settings"), back) }) { pad ->
        Column(Modifier.padding(pad).padding(horizontal = 16.dp).verticalScroll(rememberScrollState())) {
            H(t("theme"))
            FlowRow {
                (listOf("system", "light", "dark", "gruvbox", "solarized", "oled") + (if (Repo.custom != null) listOf("custom") else emptyList())).forEach { id ->
                    FilterChip(
                        selected = Repo.theme == id,
                        onClick = { Repo.theme = id; Repo.save() },
                        label = {
                            Text(
                                when (id) {
                                    "custom" -> Repo.custom?.name ?: "Custom"
                                    "system", "light", "dark" -> t("th_$id")
                                    else -> THEMES[id]!!.name
                                }
                            )
                        },
                        modifier = Modifier.padding(end = 6.dp)
                    )
                }
            }
            OutlinedButton({ impTheme.launch(arrayOf("*/*")) }, Modifier.fillMaxWidth().padding(top = 8.dp)) { Text(t("themeImp")) }
            Text(t("themeHelp"), Modifier.padding(top = 8.dp), style = MaterialTheme.typography.bodySmall)
            Text(EXAMPLE, Modifier.padding(top = 6.dp), fontFamily = FontFamily.Monospace, fontSize = 12.sp)

            H(t("language"))
            FlowRow {
                listOf("ru" to "Русский", "en" to "English", "de" to "Deutsch").forEach { (code, name) ->
                    FilterChip(
                        selected = Repo.lang == code, onClick = { Repo.lang = code; Repo.save() },
                        label = { Text(name) }, modifier = Modifier.padding(end = 6.dp)
                    )
                }
            }

            H(t("custom"))
            Text(t("tagsHelp"), style = MaterialTheme.typography.bodySmall)
            val ls = Repo.labels()
            if (ls.isEmpty()) Text(t("noTags"), Modifier.padding(top = 8.dp))
            FlowRow(Modifier.padding(top = 8.dp)) {
                ls.forEach { l ->
                    InputChip(
                        selected = false, onClick = { Repo.deleteLabel(l) }, label = { Text(l) },
                        trailingIcon = { Icon(Icons.Default.Close, t("delete"), Modifier.size(18.dp)) },
                        modifier = Modifier.padding(end = 6.dp)
                    )
                }
            }

            H(t("data"))
            Text(t("csvHelp"), style = MaterialTheme.typography.bodySmall)
            Button({ exp.launch("content-tracker.csv") }, Modifier.fillMaxWidth().padding(top = 8.dp)) { Text(t("export")) }
            OutlinedButton({ imp.launch(arrayOf("*/*")) }, Modifier.fillMaxWidth().padding(top = 8.dp)) { Text(t("import")) }
            if (msg.isNotEmpty()) Text(msg, Modifier.padding(top = 12.dp), color = MaterialTheme.colorScheme.primary)
            OutlinedButton(about, Modifier.fillMaxWidth().padding(top = 24.dp)) { Text(t("about")) }
            Spacer(Modifier.height(32.dp))
        }
    }
}

// ---------- about ----------
private const val RELEASE = "2026-11-22"

@Composable
fun AboutScreen(back: () -> Unit) {
    val ctx = LocalContext.current
    val name = ctx.applicationInfo.loadLabel(ctx.packageManager).toString()
    val ver = remember {
        runCatching { ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionName ?: "1.0" }.getOrDefault("1.0")
    }
    Scaffold(topBar = { Bar(t("about"), back) }) { pad ->
        Column(Modifier.padding(pad).padding(24.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size(96.dp).clip(RoundedCornerShape(24.dp)).background(Color(0xFF6750A4))) {
                Image(painterResource(R.drawable.ic_launcher_foreground), null, Modifier.fillMaxSize())
            }
            Text(name, Modifier.padding(top = 16.dp, bottom = 24.dp), style = MaterialTheme.typography.headlineSmall)
            Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), Arrangement.SpaceBetween) {
                Text(t("version"), color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(ver)
            }
            HorizontalDivider()
            Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), Arrangement.SpaceBetween) {
                Text(t("relInfo"), color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(RELEASE.split("-").reversed().joinToString("."))
            }
            HorizontalDivider()
        }
    }
}
