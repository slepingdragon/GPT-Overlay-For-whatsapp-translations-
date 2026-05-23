// Web mockup of the Translate app.
// Translation backend: MyMemory (no API key needed, supports EN/ID/SU).

const COLOR_PRESETS = [
  "#00E5FF","#1E88E5","#7C4DFF","#BA68C8",
  "#FF4081","#EF5350","#FF7043","#FFB300",
  "#FFD600","#66BB6A","#26A69A","#00ACC1",
  "#FFFFFF","#B0BEC5","#607D8B","#111111",
];

const LANG_NAMES = { en: "English", id: "Indonesian", su: "Sundanese" };

const state = {
  target: localStorage.getItem("target") || "id",
  source: localStorage.getItem("source") || "",
  translation: localStorage.getItem("translation") || "",
  autoTranslate: localStorage.getItem("autoTranslate") !== "false",
  history: JSON.parse(localStorage.getItem("history") || "[]"),
  deeplKey: localStorage.getItem("deeplKey") || "",
  contactNumber: localStorage.getItem("contactNumber") || "",
  accentColor: localStorage.getItem("accentColor") || "#00E5FF",
  sendColor: localStorage.getItem("sendColor") || "#FFFFFF",
  copyColor: localStorage.getItem("copyColor") || "#00E5FF",
  inputColor: localStorage.getItem("inputColor") || "#8C1A1D24",
  bgImage: localStorage.getItem("bgImage") || "",
  filter: "all",
};

const $ = (id) => document.getElementById(id);
const $$ = (sel, root = document) => Array.from(root.querySelectorAll(sel));

const sourceInput = $("source-input");
const resultBody = $("result-body");
const spinner = $("spinner");
const targetName = $("target-name");
const sendBtn = $("send-btn");
const clearBtn = $("clear-btn");
const copyResultBtn = $("copy-result-btn");
const translateBtnWrap = $("translate-btn-wrap");
const translateBtn = $("translate-btn");
const historyList = $("history-list");

// ----- Theme -----
function applyTheme() {
  const root = document.documentElement.style;
  root.setProperty("--accent", state.accentColor);
  root.setProperty("--send-color", state.sendColor);
  root.setProperty("--copy-color", state.copyColor);
  root.setProperty("--input-color", state.inputColor);
  if (state.bgImage) {
    $("bg-image").style.backgroundImage = `url("${state.bgImage}")`;
    $("bg-image").classList.add("visible");
    $("bg-overlay").classList.add("visible");
  } else {
    $("bg-image").classList.remove("visible");
    $("bg-overlay").classList.remove("visible");
  }
}

// ----- Lang -----
function setTarget(t) {
  state.target = t;
  localStorage.setItem("target", t);
  targetName.textContent = LANG_NAMES[t];
  for (const el of $$(".chip")) {
    el.classList.toggle("active", el.dataset.lang === t);
  }
  if (state.source && state.autoTranslate) translate();
  else if (!state.autoTranslate) {
    state.translation = "";
    renderResult();
  }
}

// ----- Translate -----
function guessSource(text) {
  const lower = " " + text.toLowerCase() + " ";
  const hints = [" yang "," saya "," kamu "," tidak "," sudah "," kalau "," saja "," juga "," ini "," itu "," dan "];
  return hints.some(h => lower.includes(h)) ? "id" : "en";
}

let debounceTimer = null;
let abortCtrl = null;

function scheduleTranslate() {
  if (!state.autoTranslate) {
    renderResult();
    return;
  }
  clearTimeout(debounceTimer);
  debounceTimer = setTimeout(translate, 280);
}

async function translate() {
  if (abortCtrl) abortCtrl.abort();
  const text = state.source.trim();
  if (!text) {
    state.translation = "";
    spinner.hidden = true;
    renderResult();
    return;
  }
  spinner.hidden = false;
  const target = state.target;
  const source = guessSource(text);
  abortCtrl = new AbortController();
  try {
    const url = `https://api.mymemory.translated.net/get?q=${encodeURIComponent(text)}&langpair=${source}|${target}`;
    const resp = await fetch(url, { signal: abortCtrl.signal });
    if (!resp.ok) throw new Error(`HTTP ${resp.status}`);
    const data = await resp.json();
    state.translation = data?.responseData?.translatedText || "";
    localStorage.setItem("translation", state.translation);
  } catch (e) {
    if (e.name === "AbortError") return;
    state.translation = "";
    resultBody.textContent = "⚠ " + e.message;
    resultBody.classList.add("error");
    spinner.hidden = true;
    refreshButtons();
    return;
  }
  spinner.hidden = true;
  renderResult();
}

function renderResult() {
  resultBody.classList.remove("error", "placeholder");
  if (state.translation) {
    resultBody.textContent = state.translation;
    translateBtnWrap.hidden = true;
    copyResultBtn.hidden = false;
  } else if (!state.autoTranslate && state.source.trim()) {
    resultBody.textContent = "";
    translateBtnWrap.hidden = false;
    copyResultBtn.hidden = true;
  } else if (state.source.trim()) {
    resultBody.textContent = "…";
    resultBody.classList.add("placeholder");
    translateBtnWrap.hidden = true;
    copyResultBtn.hidden = true;
  } else {
    resultBody.textContent = "Translation appears here";
    resultBody.classList.add("placeholder");
    translateBtnWrap.hidden = true;
    copyResultBtn.hidden = true;
  }
  refreshButtons();
}

function refreshButtons() {
  const canSend = state.source.trim() && state.translation.trim();
  sendBtn.disabled = !canSend;
  clearBtn.hidden = !state.source;
}

// ----- History -----
function addToHistory() {
  const src = state.source.trim();
  const tgt = state.translation.trim();
  if (!src || !tgt) return;
  const idx = state.history.findIndex(e => e.source.trim() === src && e.targetLang === state.target);
  const entry = {
    id: crypto.randomUUID ? crypto.randomUUID() : String(Date.now()),
    source: state.source,
    translation: state.translation,
    targetLang: state.target,
    timestamp: Date.now(),
    favorite: false,
  };
  if (idx >= 0) {
    entry.id = state.history[idx].id;
    entry.favorite = state.history[idx].favorite;
    state.history.splice(idx, 1);
  }
  state.history.unshift(entry);
  state.history = state.history.slice(0, 500);
  localStorage.setItem("history", JSON.stringify(state.history));
  renderHistory();
}

function relativeTime(ts) {
  const diff = Date.now() - ts;
  if (diff < 60_000) return "just now";
  if (diff < 3_600_000) return `${Math.floor(diff / 60_000)}m`;
  if (diff < 86_400_000) return `${Math.floor(diff / 3_600_000)}h`;
  if (diff < 604_800_000) return `${Math.floor(diff / 86_400_000)}d`;
  return new Date(ts).toLocaleDateString(undefined, { month: "short", day: "numeric" });
}

const HEART_FILLED = 'M12,21.35l-1.45-1.32C5.4,15.36,2,12.28,2,8.5C2,5.42,4.42,3,7.5,3c1.74,0,3.41,0.81,4.5,2.09 C13.09,3.81,14.76,3,16.5,3C19.58,3,22,5.42,22,8.5c0,3.78-3.4,6.86-8.55,11.54L12,21.35z';
const HEART_OUTLINE = 'M16.5,3c-1.74,0-3.41,0.81-4.5,2.09C10.91,3.81,9.24,3,7.5,3C4.42,3,2,5.42,2,8.5c0,3.78,3.4,6.86,8.55,11.54L12,21.35 l1.45-1.32C18.6,15.36,22,12.28,22,8.5C22,5.42,19.58,3,16.5,3z M12.1,18.55l-0.1,0.1l-0.1-0.1C7.14,14.24,4,11.39,4,8.5 C4,6.5,5.5,5,7.5,5c1.54,0,3.04,0.99,3.57,2.36h1.87C13.46,5.99,14.96,5,16.5,5c2,0,3.5,1.5,3.5,3.5C20,11.39,16.86,14.24,12.1,18.55 z';
const COPY_PATH = 'M16,1H4C2.9,1,2,1.9,2,3v14h2V3h12V1z M19,5H8C6.9,5,6,5.9,6,7v14c0,1.1,0.9,2,2,2h11c1.1,0,2-0.9,2-2V7C21,5.9,20.1,5,19,5z M19,21H8V7h11V21z';
const TRASH_PATH = 'M6,19c0,1.1,0.9,2,2,2h8c1.1,0,2-0.9,2-2V7H6V19z M19,4h-3.5l-1-1h-5l-1,1H5v2h14V4z';

function renderHistory() {
  historyList.innerHTML = "";
  if (state.history.length === 0) {
    const div = document.createElement("div");
    div.className = "empty-state";
    div.textContent = "Your translations show up here";
    historyList.appendChild(div);
    return;
  }
  for (const entry of state.history) {
    const el = document.createElement("div");
    el.className = "bubble";
    el.innerHTML = `
      <div class="translation"></div>
      <div class="source"></div>
      <div class="meta">
        <span class="label"></span>
        <span class="grow"></span>
        <button class="meta-btn fav ${entry.favorite ? 'active' : ''}" data-id="${entry.id}" data-action="fav" title="Favorite">
          <svg viewBox="0 0 24 24"><path d="${entry.favorite ? HEART_FILLED : HEART_OUTLINE}"/></svg>
        </button>
        <button class="meta-btn" data-id="${entry.id}" data-action="copy" title="Copy">
          <svg viewBox="0 0 24 24"><path d="${COPY_PATH}"/></svg>
        </button>
      </div>
    `;
    el.querySelector(".translation").textContent = entry.translation;
    el.querySelector(".source").textContent = entry.source;
    el.querySelector(".label").textContent = `${LANG_NAMES[entry.targetLang] || entry.targetLang} · ${relativeTime(entry.timestamp)}`;
    el.addEventListener("click", (ev) => {
      if (ev.target.closest(".meta-btn")) return;
      state.source = entry.source;
      state.translation = entry.translation;
      sourceInput.value = entry.source;
      autoSizeInput();
      localStorage.setItem("source", state.source);
      localStorage.setItem("translation", state.translation);
      renderResult();
    });
    historyList.appendChild(el);
  }
}

historyList.addEventListener("click", (ev) => {
  const btn = ev.target.closest(".meta-btn");
  if (!btn) return;
  ev.stopPropagation();
  const id = btn.dataset.id;
  const action = btn.dataset.action;
  const entry = state.history.find(e => e.id === id);
  if (!entry) return;
  if (action === "fav") {
    entry.favorite = !entry.favorite;
    localStorage.setItem("history", JSON.stringify(state.history));
    renderHistory();
    renderFavoritesInSettings();
  } else if (action === "copy") {
    navigator.clipboard.writeText(entry.translation);
  }
});

// ----- Input -----
function autoSizeInput() {
  sourceInput.style.height = "auto";
  sourceInput.style.height = Math.min(100, sourceInput.scrollHeight) + "px";
}
sourceInput.addEventListener("input", () => {
  state.source = sourceInput.value;
  localStorage.setItem("source", state.source);
  autoSizeInput();
  scheduleTranslate();
  refreshButtons();
});

// ----- Buttons -----
clearBtn.addEventListener("click", () => {
  state.source = "";
  state.translation = "";
  sourceInput.value = "";
  localStorage.removeItem("source");
  localStorage.removeItem("translation");
  autoSizeInput();
  renderResult();
});
sendBtn.addEventListener("click", () => {
  if (sendBtn.disabled) return;
  addToHistory();
  navigator.clipboard.writeText(state.translation);
  alert("Send via WhatsApp\n(Web mockup — would open WhatsApp with the text pre-filled)\nTranslation copied to clipboard.");
});
copyResultBtn.addEventListener("click", () => {
  if (!state.translation) return;
  addToHistory();
  navigator.clipboard.writeText(state.translation);
});
translateBtn.addEventListener("click", () => translate());

for (const chip of $$(".chip")) {
  chip.addEventListener("click", () => setTarget(chip.dataset.lang));
}

// ----- Sheets -----
$("settings-btn").addEventListener("click", () => {
  $("settings-sheet").hidden = false;
  renderFavoritesInSettings();
});
$("history-btn").addEventListener("click", () => {
  $("history-sheet").hidden = false;
  renderSheetHistory();
});
for (const c of $$("[data-close]")) {
  c.addEventListener("click", () => $(c.dataset.close).hidden = true);
}
for (const backdrop of $$(".sheet-backdrop")) {
  backdrop.addEventListener("click", (ev) => {
    if (ev.target === backdrop) backdrop.hidden = true;
  });
}

// Settings — basic fields
$("deepl-key").value = state.deeplKey;
$("deepl-key").addEventListener("input", (e) => {
  state.deeplKey = e.target.value;
  localStorage.setItem("deeplKey", state.deeplKey);
});
$("contact-number").value = state.contactNumber;
$("contact-number").addEventListener("input", (e) => {
  state.contactNumber = e.target.value;
  localStorage.setItem("contactNumber", state.contactNumber);
});
const autoToggle = $("auto-translate-toggle");
autoToggle.checked = state.autoTranslate;
autoToggle.addEventListener("change", () => {
  state.autoTranslate = autoToggle.checked;
  localStorage.setItem("autoTranslate", String(state.autoTranslate));
  renderResult();
});

$("test-key-row").addEventListener("click", async () => {
  if (!state.deeplKey) { alert("Enter your DeepL key first."); return; }
  alert("Web mockup uses MyMemory (free) instead of DeepL.\nIn the Android/iOS app, this tests your DeepL key.");
});

// Background image
$("bg-file").addEventListener("change", (ev) => {
  const f = ev.target.files?.[0];
  if (!f) return;
  const reader = new FileReader();
  reader.onload = () => {
    state.bgImage = reader.result;
    localStorage.setItem("bgImage", state.bgImage);
    applyTheme();
  };
  reader.readAsDataURL(f);
});
$("bg-clear").addEventListener("click", () => {
  state.bgImage = "";
  localStorage.removeItem("bgImage");
  applyTheme();
  $("bg-file").value = "";
});

// ===== Color picker rows =====
function buildColorPickers() {
  for (const tpl of $$("ColorPickerRow")) {
    const target = tpl.dataset.target;
    const label = tpl.dataset.label;
    const alpha = tpl.dataset.alpha === "true";
    const row = document.createElement("div");
    row.className = "color-picker-row";
    row.innerHTML = `
      <div class="cp-header">
        <div class="cp-label">${label}</div>
        <div class="cp-preview" data-preview></div>
      </div>
      <div class="cp-swatches" data-swatches></div>
      <div class="cp-hex-row">
        <input class="cp-hex" data-hex maxlength="${alpha ? 8 : 6}" placeholder="${alpha ? 'AARRGGBB' : 'RRGGBB'}" />
      </div>
    `;
    tpl.replaceWith(row);
    const preview = row.querySelector("[data-preview]");
    const swatchesEl = row.querySelector("[data-swatches]");
    const hexEl = row.querySelector("[data-hex]");

    const getCurrent = () => {
      switch (target) {
        case "accent": return state.accentColor;
        case "send": return state.sendColor;
        case "copy": return state.copyColor;
        case "input": return state.inputColor;
      }
    };
    const setCurrent = (c) => {
      switch (target) {
        case "accent": state.accentColor = c; localStorage.setItem("accentColor", c); break;
        case "send": state.sendColor = c; localStorage.setItem("sendColor", c); break;
        case "copy": state.copyColor = c; localStorage.setItem("copyColor", c); break;
        case "input": state.inputColor = c; localStorage.setItem("inputColor", c); break;
      }
      preview.style.background = c;
      hexEl.value = c.replace("#", "");
      for (const s of swatchesEl.children) {
        s.classList.toggle("selected", (s.dataset.color || "").toLowerCase() === c.toLowerCase());
      }
      applyTheme();
    };

    // Build swatches
    for (const c of COLOR_PRESETS) {
      const s = document.createElement("div");
      s.className = "cp-swatch";
      s.dataset.color = c;
      s.style.background = c;
      s.addEventListener("click", () => setCurrent(c));
      swatchesEl.appendChild(s);
    }

    // Hex input
    hexEl.addEventListener("input", () => {
      const v = hexEl.value.replace(/[^a-fA-F0-9]/g, "").slice(0, alpha ? 8 : 6).toUpperCase();
      hexEl.value = v;
      if (v.length === 6 || v.length === 8) setCurrent("#" + v);
    });

    setCurrent(getCurrent());
  }
}

// Favorites in settings
function renderFavoritesInSettings() {
  const group = $("favorites-group");
  const label = $("favorites-label");
  const favs = state.history.filter(e => e.favorite).slice(0, 5);
  group.innerHTML = "";
  if (favs.length === 0) {
    group.hidden = true;
    label.hidden = true;
    return;
  }
  group.hidden = false;
  label.hidden = false;
  for (const entry of favs) {
    const row = document.createElement("div");
    row.className = "ios-row ios-row-tap";
    row.innerHTML = `
      <div class="ios-row-content">
        <div class="ios-row-title">${escapeHtml(entry.translation)}</div>
        <div class="ios-row-hint">${escapeHtml(entry.source)} · ${LANG_NAMES[entry.targetLang]}</div>
      </div>
    `;
    row.addEventListener("click", () => {
      state.source = entry.source;
      state.translation = entry.translation;
      sourceInput.value = entry.source;
      autoSizeInput();
      localStorage.setItem("source", state.source);
      localStorage.setItem("translation", state.translation);
      renderResult();
      $("settings-sheet").hidden = true;
    });
    group.appendChild(row);
  }
}

function escapeHtml(s) {
  return s.replace(/[&<>"']/g, (c) => ({ "&":"&amp;","<":"&lt;",">":"&gt;","\"":"&quot;","'":"&#39;" }[c]));
}

// ===== History sheet =====
function renderSheetHistory() {
  const list = $("history-sheet-list");
  list.innerHTML = "";
  const items = state.filter === "fav"
    ? state.history.filter(e => e.favorite)
    : state.history;
  if (items.length === 0) {
    list.innerHTML = `<div class="empty-state">${state.filter === "fav" ? "No favorites yet" : "No history yet"}</div>`;
    return;
  }
  const group = document.createElement("div");
  group.className = "ios-group";
  for (const entry of items) {
    const row = document.createElement("div");
    row.className = "ios-row ios-row-tap";
    row.innerHTML = `
      <div class="ios-row-content">
        <div class="ios-row-title">${escapeHtml(entry.translation)}</div>
        <div class="ios-row-hint">${escapeHtml(entry.source)} · ${LANG_NAMES[entry.targetLang]} · ${relativeTime(entry.timestamp)}</div>
      </div>
      <button class="meta-btn ${entry.favorite ? 'active' : ''}" data-id="${entry.id}" data-action="fav-sheet" style="color:${entry.favorite ? state.accentColor : 'rgba(255,255,255,0.55)'}">
        <svg viewBox="0 0 24 24"><path d="${entry.favorite ? HEART_FILLED : HEART_OUTLINE}"/></svg>
      </button>
      <button class="meta-btn" data-id="${entry.id}" data-action="del-sheet">
        <svg viewBox="0 0 24 24"><path d="${TRASH_PATH}"/></svg>
      </button>
    `;
    row.querySelector(".ios-row-content").addEventListener("click", () => {
      state.source = entry.source;
      state.translation = entry.translation;
      sourceInput.value = entry.source;
      autoSizeInput();
      localStorage.setItem("source", state.source);
      localStorage.setItem("translation", state.translation);
      renderResult();
      $("history-sheet").hidden = true;
    });
    group.appendChild(row);
  }
  list.appendChild(group);
}

$("history-sheet-list").addEventListener("click", (ev) => {
  const btn = ev.target.closest("[data-action]");
  if (!btn) return;
  const id = btn.dataset.id;
  const action = btn.dataset.action;
  const idx = state.history.findIndex(e => e.id === id);
  if (idx < 0) return;
  if (action === "fav-sheet") {
    state.history[idx].favorite = !state.history[idx].favorite;
  } else if (action === "del-sheet") {
    state.history.splice(idx, 1);
  }
  localStorage.setItem("history", JSON.stringify(state.history));
  renderSheetHistory();
  renderHistory();
  renderFavoritesInSettings();
});

for (const c of $$(".filter-chip")) {
  c.addEventListener("click", () => {
    state.filter = c.dataset.filter;
    for (const x of $$(".filter-chip")) x.classList.toggle("active", x === c);
    renderSheetHistory();
  });
}

// ----- Init -----
sourceInput.value = state.source;
autoSizeInput();
buildColorPickers();
applyTheme();
setTarget(state.target);
renderHistory();
renderResult();
