package ai.aligned.ui.map

import android.annotation.SuppressLint
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebView
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import ai.aligned.ui.components.SkeletonCard
import ai.aligned.ui.icons.MorphingIcon
import ai.aligned.ui.icons.MorphingIcons
import ai.aligned.ui.theme.AlignedTokens
import ai.aligned.ui.theme.Tokens
import org.json.JSONArray
import org.json.JSONObject

@Composable
fun MapScreen(vm: MapViewModel = hiltViewModel()) {
    val state by vm.state.collectAsState()
    val c = AlignedTokens.colors
    var selectedIndex by remember { mutableIntStateOf(0) }
    Column(modifier = Modifier.fillMaxSize().background(c.bg)) {
        Header(state)
        when (val s = state) {
            MapState.Loading -> LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) { items(5) { SkeletonCard(heightDp = 80) } }
            is MapState.Error -> ErrorView(s.message, vm::refresh)
            is MapState.Ready -> {
                if (s.markers.isEmpty()) {
                    ErrorView("No locations available right now.", vm::refresh)
                } else {
                    LaunchedEffect(s.markers.size) {
                        selectedIndex = selectedIndex.coerceIn(0, s.markers.lastIndex)
                    }
                    MapReadyContent(
                        markers = s.markers,
                        selectedIndex = selectedIndex,
                        onMarkerSelected = { idx -> selectedIndex = idx.coerceIn(0, s.markers.lastIndex) }
                    )
                }
            }
        }
    }
}

@Composable
private fun Header(state: MapState) {
    val c = AlignedTokens.colors
    val count = (state as? MapState.Ready)?.markers?.size ?: 0
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp)) {
        Text(
            "Map",
            color = c.text,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
            fontSize = 28.sp,
            letterSpacing = (-0.5).sp
        )
        Text(
            if (count > 0) "$count locations · today" else "Where today's news is happening",
            color = c.textSecondary, fontSize = 13.sp
        )
    }
}

@Composable
private fun MapReadyContent(
    markers: List<UiMarker>,
    selectedIndex: Int,
    onMarkerSelected: (Int) -> Unit
) {
    val c = AlignedTokens.colors
    val selected = markers.getOrNull(selectedIndex) ?: markers.first()
    val uri = LocalUriHandler.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        MapWebView(
            markers = markers,
            selectedIndex = selectedIndex,
            onMarkerSelected = onMarkerSelected,
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp)
                .clip(RoundedCornerShape(Tokens.Radius.card))
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(Tokens.Radius.card))
                .background(c.elev1)
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(9.dp).clip(CircleShape).background(selected.color))
                Spacer(Modifier.width(8.dp))
                Text(
                    "${selected.city}, ${selected.country}",
                    color = c.text,
                    fontSize = 15.sp,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold
                )
                Spacer(Modifier.weight(1f))
                Text(
                    selected.groupName,
                    color = c.textSecondary,
                    fontSize = 11.sp,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
                )
            }

            selected.headlines.take(3).forEach { h ->
                Text("• $h", color = c.textSecondary, fontSize = 13.sp, lineHeight = 18.sp, maxLines = 2)
            }

            val firstLink = selected.storyLinks.firstOrNull { it.startsWith("http") }
            if (!firstLink.isNullOrBlank()) {
                Text(
                    "Open source tweet",
                    color = c.accent,
                    fontSize = 13.sp,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                    modifier = Modifier.clickable { uri.openUri(firstLink) }
                )
            }
        }

        MarkersList(markers = markers, selectedId = selected.id, onSelect = onMarkerSelected)
        Spacer(Modifier.height(40.dp))
    }
}

@Composable
private fun MarkersList(markers: List<UiMarker>, selectedId: String, onSelect: (Int) -> Unit) {
    val c = AlignedTokens.colors
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        markers.forEachIndexed { idx, m ->
            val active = m.id == selectedId
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(Tokens.Radius.card))
                    .background(if (active) c.elev2 else c.elev1)
                    .clickable { onSelect(idx) }
                    .padding(14.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(m.color))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "${m.city}, ${m.country}",
                        color = c.text,
                        fontSize = 14.sp,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold
                    )
                    Spacer(Modifier.weight(1f))
                    Text(m.groupName, color = c.textSecondary, fontSize = 11.sp)
                }
            }
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun MapWebView(
    markers: List<UiMarker>,
    selectedIndex: Int,
    onMarkerSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val markerJson = remember(markers) { markers.toJson() }
    val html = remember(markerJson) { mapHtml(markerJson) }

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            WebView(ctx).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                setBackgroundColor(android.graphics.Color.TRANSPARENT)
                webChromeClient = WebChromeClient()
                addJavascriptInterface(MapBridge(onMarkerSelected), "AndroidBridge")
                loadDataWithBaseURL(null, html, "text/html", "utf-8", null)
            }
        },
        update = { web ->
            web.evaluateJavascript("window.selectMarker($selectedIndex);", null)
        }
    )
}

private class MapBridge(
    private val onMarkerSelected: (Int) -> Unit
) {
    @JavascriptInterface
    fun onMarkerTap(index: Int) {
        onMarkerSelected(index)
    }
}

private fun List<UiMarker>.toJson(): String {
    val arr = JSONArray()
    forEachIndexed { idx, marker ->
        arr.put(
            JSONObject()
                .put("index", idx)
                .put("lat", marker.lat)
                .put("lng", marker.lng)
                .put("city", marker.city)
                .put("country", marker.country)
                .put("groupName", marker.groupName)
                .put("color", marker.color.toHex())
        )
    }
    return arr.toString()
}

private fun Color.toHex(): String {
    val argb = (value shr 32).toInt()
    val rgb = argb and 0x00FFFFFF
    return "#%06X".format(rgb)
}

private fun mapHtml(markersJson: String) = """
<!doctype html>
<html>
  <head>
    <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0"/>
    <link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css"/>
    <style>
      html, body, #map { margin: 0; height: 100%; width: 100%; background: #111; }
      .dot {
        width: 14px; height: 14px; border-radius: 50%;
        border: 2px solid rgba(255,255,255,0.9); box-sizing: border-box;
      }
    </style>
  </head>
  <body>
    <div id="map"></div>
    <script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>
    <script>
      const markers = $markersJson;
      const map = L.map('map', { zoomControl: false });
      L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
        maxZoom: 19,
        attribution: '&copy; OpenStreetMap contributors'
      }).addTo(map);
      const pinRefs = [];
      markers.forEach((m) => {
        const icon = L.divIcon({
          className: '',
          html: `<div class="dot" style="background:${'$'}{m.color}"></div>`,
          iconSize: [14, 14],
          iconAnchor: [7, 7]
        });
        const marker = L.marker([m.lat, m.lng], { icon }).addTo(map);
        marker.on('click', () => {
          if (window.AndroidBridge && window.AndroidBridge.onMarkerTap) {
            window.AndroidBridge.onMarkerTap(m.index);
          }
        });
        pinRefs.push(marker);
      });

      if (markers.length > 0) {
        const bounds = L.latLngBounds(markers.map(m => [m.lat, m.lng]));
        map.fitBounds(bounds, { padding: [22, 22] });
      } else {
        map.setView([20, 0], 1.8);
      }

      window.selectMarker = function(index) {
        if (index < 0 || index >= markers.length) return;
        const m = markers[index];
        map.flyTo([m.lat, m.lng], Math.max(map.getZoom(), 3), { animate: true, duration: 0.35 });
      }
    </script>
  </body>
</html>
""".trimIndent()

@Composable
private fun ErrorView(msg: String, onRetry: () -> Unit) {
    val c = AlignedTokens.colors
    Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            MorphingIcon(spec = MorphingIcons.globe, size = 28.dp, color = c.destructive)
            Spacer(Modifier.height(8.dp))
            Text(
                "Map unavailable",
                color = c.text,
                fontSize = 17.sp,
                fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold
            )
            Text(msg, color = c.textSecondary, fontSize = 13.sp)
            Spacer(Modifier.height(10.dp))
            Text(
                "Try again",
                color = c.accent,
                fontSize = 14.sp,
                fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                modifier = Modifier.clickable { onRetry() }
            )
        }
    }
}
