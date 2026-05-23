#!/usr/bin/env python3
"""Tiny upload server so the user can share screenshots from their phone.

Run: python _dev/upload_server.py
Visit from PC: http://localhost:8055
Visit from phone (same Wi-Fi): http://192.168.1.214:8055
"""
import http.server
import os
import re
import socketserver
import sys
from datetime import datetime
from pathlib import Path

PORT = 8055
ROOT = Path(__file__).resolve().parent
UPLOAD_DIR = ROOT / "uploads"
UPLOAD_DIR.mkdir(exist_ok=True)

PAGE = """<!DOCTYPE html>
<html>
<head>
<meta charset="utf-8">
<title>Upload to Claude</title>
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<style>
:root {{
  color-scheme: dark;
}}
body {{
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif;
  max-width: 720px;
  margin: 0 auto;
  padding: 24px 16px 96px;
  background: #0F1115;
  color: #fff;
}}
h1 {{ color: #00E5FF; font-weight: 600; margin: 0 0 16px; }}
h2 {{ color: #a5adba; font-size: 13px; text-transform: uppercase; letter-spacing: .04em; margin: 32px 0 12px; }}
form {{
  background: #1A1D24;
  padding: 18px;
  border-radius: 14px;
  border: 1px solid rgba(0,229,255,0.25);
}}
input[type=file] {{
  width: 100%;
  padding: 14px;
  background: #0F1115;
  color: white;
  border: 1px dashed rgba(0,229,255,0.5);
  border-radius: 10px;
  font-size: 14px;
}}
button {{
  width: 100%;
  padding: 14px;
  background: #00E5FF;
  color: #001017;
  border: none;
  border-radius: 10px;
  font-weight: 700;
  font-size: 15px;
  margin-top: 12px;
}}
.file {{
  padding: 12px;
  background: #1A1D24;
  border-radius: 10px;
  margin-bottom: 8px;
  display: flex;
  align-items: center;
  gap: 12px;
}}
.thumb {{
  width: 80px;
  height: 80px;
  flex-shrink: 0;
  border-radius: 8px;
  background: #0F1115;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
}}
.thumb img {{ width: 100%; height: 100%; object-fit: cover; }}
.meta {{ font-size: 13px; }}
.meta a {{ color: #00E5FF; text-decoration: none; }}
.meta .when {{ color: #6c757d; font-size: 11px; }}
.empty {{ color: #6c757d; font-style: italic; padding: 24px 0; text-align: center; }}
</style>
</head>
<body>
<h1>📤 Upload to Claude</h1>
<form method="POST" action="/upload" enctype="multipart/form-data">
  <input type="file" name="file" accept="image/*,video/*" multiple required>
  <button type="submit">Upload</button>
</form>
<h2>Recent uploads</h2>
{files}
</body>
</html>
"""


class Handler(http.server.SimpleHTTPRequestHandler):
    def do_GET(self):
        if self.path == "/" or self.path == "/index.html":
            files_html = ""
            files = sorted(UPLOAD_DIR.glob("*"), key=lambda p: p.stat().st_mtime, reverse=True)[:30]
            for f in files:
                ext = f.suffix.lower()
                if ext in (".png", ".jpg", ".jpeg", ".gif", ".webp"):
                    thumb = f'<div class="thumb"><img src="/uploads/{f.name}"></div>'
                elif ext in (".mp4", ".mov", ".webm"):
                    thumb = '<div class="thumb">🎥</div>'
                else:
                    thumb = '<div class="thumb">📄</div>'
                when = datetime.fromtimestamp(f.stat().st_mtime).strftime("%H:%M:%S")
                files_html += (
                    f'<div class="file">{thumb}'
                    f'<div class="meta">'
                    f'<a href="/uploads/{f.name}" target="_blank">{f.name}</a><br>'
                    f'<span class="when">{when}</span>'
                    f'</div></div>'
                )
            if not files_html:
                files_html = '<div class="empty">No uploads yet</div>'
            body = PAGE.format(files=files_html).encode("utf-8")
            self.send_response(200)
            self.send_header("Content-Type", "text/html; charset=utf-8")
            self.send_header("Content-Length", str(len(body)))
            self.end_headers()
            self.wfile.write(body)
            return
        if self.path.startswith("/uploads/"):
            super().do_GET()
            return
        self.send_error(404)

    def do_POST(self):
        if self.path != "/upload":
            self.send_error(404)
            return
        ct = self.headers.get("Content-Type", "")
        if not ct.startswith("multipart/form-data"):
            self.send_error(400, "expected multipart/form-data")
            return
        boundary = ct.split("boundary=", 1)[1].encode()
        length = int(self.headers.get("Content-Length", "0"))
        body = self.rfile.read(length)
        parts = body.split(b"--" + boundary)
        saved = []
        for part in parts:
            if b'filename="' not in part:
                continue
            header_end = part.find(b"\r\n\r\n")
            if header_end == -1:
                continue
            headers_blob = part[:header_end].decode("utf-8", errors="ignore")
            content = part[header_end + 4 :]
            if content.endswith(b"\r\n"):
                content = content[:-2]
            m = re.search(r'filename="([^"]+)"', headers_blob)
            if not m or not m.group(1):
                continue
            raw = m.group(1)
            safe = re.sub(r"[^A-Za-z0-9._-]", "_", os.path.basename(raw)) or "upload"
            ts = datetime.now().strftime("%H%M%S")
            dest = UPLOAD_DIR / f"{ts}_{safe}"
            with open(dest, "wb") as f:
                f.write(content)
            saved.append(dest.name)
            print(f"[{datetime.now():%H:%M:%S}] saved {dest}", flush=True)
        self.send_response(303)
        self.send_header("Location", "/")
        self.end_headers()

    def translate_path(self, path):
        # serve /uploads/* from UPLOAD_DIR
        if path.startswith("/uploads/"):
            name = path[len("/uploads/") :]
            return str(UPLOAD_DIR / name)
        return super().translate_path(path)

    def log_message(self, fmt, *args):
        sys.stderr.write(f"[{datetime.now():%H:%M:%S}] {fmt % args}\n")


def main():
    os.chdir(ROOT)
    print(f"Serving on http://localhost:{PORT}")
    print(f"From phone (Wi-Fi): http://192.168.1.214:{PORT}")
    print(f"Uploads land in: {UPLOAD_DIR}")
    print("Ctrl+C to stop.")
    with socketserver.ThreadingTCPServer(("", PORT), Handler) as httpd:
        httpd.serve_forever()


if __name__ == "__main__":
    main()
