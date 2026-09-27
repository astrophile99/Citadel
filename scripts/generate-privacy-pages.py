"""
Regenerates PRIVACY.md and docs/privacy/index.html from the in-app policy.

The app's PrivacyPolicy.kt is the single source of truth. Run this after editing it, so
the copy the Play listing links to can never drift from the copy inside the app:

    python scripts/generate-privacy-pages.py
"""

import html
import os
import re

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SOURCE = os.path.join(ROOT, "app", "src", "main", "java", "dev", "atharva", "citadel",
                      "ui", "sanctuary", "PrivacyPolicy.kt")
SECTION = re.compile(r'Section\(\s*"([^"]+)",\s*((?:"(?:[^"\\]|\\.)*"\s*\+?\s*)+)\)')
STRING = re.compile(r'"((?:[^"\\]|\\.)*)"')


def read_policy():
    src = open(SOURCE, encoding="utf-8").read()
    effective = re.search(r'EFFECTIVE = "([^"]+)"', src).group(1)
    contact = re.search(r'CONTACT_URL = "([^"]+)"', src).group(1)
    sections = []
    for match in SECTION.finditer(src):
        body = "".join(STRING.findall(match.group(2)))
        body = body.replace('\\"', '"').replace("$CONTACT_URL", contact)
        sections.append((match.group(1), body))
    if len(sections) < 5:
        raise SystemExit(f"Only found {len(sections)} sections in {SOURCE}; refusing to write.")
    return effective, contact, sections


def write_markdown(effective, sections):
    lines = ["# Citadel — Privacy Policy", "", f"_Effective {effective}_", ""]
    for heading, body in sections:
        lines += [f"## {heading}", "", body, ""]
    lines += [
        "---",
        "",
        "This policy is shown in the app under **Sanctuary → Privacy**, and is published at",
        "<https://astrophile99.github.io/Citadel/privacy/>.",
        "",
    ]
    with open(os.path.join(ROOT, "PRIVACY.md"), "w", encoding="utf-8", newline="\n") as f:
        f.write("\n".join(lines))


def write_html(effective, contact, sections):
    def paragraph(text):
        escaped = html.escape(text)
        link = f'<a href="{contact}">{html.escape(contact)}</a>'
        return escaped.replace(html.escape(contact), link)

    body = "\n".join(
        f"    <section>\n      <h2>{html.escape(h)}</h2>\n      <p>{paragraph(b)}</p>\n    </section>"
        for h, b in sections
    )
    page = f"""<!doctype html>
<html lang="en">
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <title>Citadel — Privacy Policy</title>
  <meta name="description" content="Citadel keeps everything on your phone. No account, no ads, no analytics, no tracking.">
  <style>
    :root {{ color-scheme: dark; --ground: #07050A; --ink: #F4F1EA; --dim: #B7BDC8; --gold: #D4A84F; --edge: #262E3C; }}
    * {{ box-sizing: border-box; }}
    body {{ margin: 0; background: var(--ground); color: var(--ink);
      font: 17px/1.7 "Noto Serif", Georgia, "Times New Roman", serif; }}
    main {{ max-width: 42rem; margin: 0 auto; padding: 4rem 1.5rem 5rem; }}
    .mark {{ font: 600 .72rem/1 system-ui, sans-serif; letter-spacing: .24em; color: var(--gold); }}
    h1 {{ font-weight: 400; font-size: 2.4rem; line-height: 1.15; margin: .9rem 0 .4rem; }}
    .effective {{ color: var(--dim); font-style: italic; margin: 0 0 2.5rem; }}
    section {{ border-top: 1px solid var(--edge); padding: 1.3rem 0; }}
    h2 {{ font-weight: 500; font-size: 1.2rem; margin: 0 0 .4rem; }}
    p {{ margin: 0; color: var(--dim); font-family: system-ui, -apple-system, "Segoe UI", Roboto, sans-serif; font-size: 1rem; }}
    a {{ color: var(--gold); text-underline-offset: 3px; }}
    a:focus-visible {{ outline: 2px solid var(--gold); outline-offset: 3px; }}
  </style>
</head>
<body>
  <main>
    <div class="mark">CITADEL</div>
    <h1>Privacy Policy</h1>
    <p class="effective">Effective {html.escape(effective)}</p>
{body}
  </main>
</body>
</html>
"""
    out_dir = os.path.join(ROOT, "docs", "privacy")
    os.makedirs(out_dir, exist_ok=True)
    with open(os.path.join(out_dir, "index.html"), "w", encoding="utf-8", newline="\n") as f:
        f.write(page)


if __name__ == "__main__":
    effective, contact, sections = read_policy()
    write_markdown(effective, sections)
    write_html(effective, contact, sections)
    print(f"Wrote PRIVACY.md and docs/privacy/index.html ({len(sections)} sections, effective {effective}).")
