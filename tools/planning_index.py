"""The index of PLANNING's archive (2 Nov 2026).

The plan lives in `PLANNING.md` (how it is made, the state, what is open, the architecture, the
phases not done, the log of the current period) and its closed parts in
`docs/planning/NN-<period>.md`, frozen. This script writes `docs/planning/README.md`: every
title (`##`, `###`, `####`) of every archive file, and every entry of a log (a top-level bullet
that opens with a bold title), each with its line, so a reference by name («§11 Phase 10»,
«§15 "Phase 11's device checks done"») resolves with a search of the index and a read from the
right line.

The live plan is not in it: it is short and searched by itself, and the index changes only when
the plan is archived. Then it is run again:

    python3 tools/planning_index.py           # writes the index
    python3 tools/planning_index.py --check   # exits with 1 if the index is behind
"""

import pathlib
import re
import sys

ROOT = pathlib.Path(__file__).resolve().parent.parent
ARCHIVE = ROOT / "docs" / "planning"
INDEX = ARCHIVE / "README.md"

HEADER = """# PLANNING's archive

The closed parts of Passo's plan, one per file, **frozen**: never edited. The live plan (how it
is made, the state, what is open, the architecture, the phases not done, the current log) is
[`PLANNING.md`](../../PLANNING.md), which also says how the plan is archived. A decision that
changes one of these is written there, naming the one it changes.

This index is written by `tools/planning_index.py`: never edited by hand. Every title (`##`,
`###`, `####`) and every log entry (by its bold title) of the archive, with its line, to find a
reference by name and read from the right place. The current log is not here: it is in
`PLANNING.md`, short, and searched there.
"""

HEADING = re.compile(r"^(#{2,4}) (.+?)\s*$")
ENTRY = re.compile(r"^- \*\*(.+?)\*\*")
BOLD = re.compile(r"\*\*(.+?)\*\*")
DATE_ONLY = re.compile(r"^\d{1,2} \w{3,9} \d{4}\.?$")
LONGEST = 140


def files() -> list[pathlib.Path]:
    return sorted(ARCHIVE.glob("[0-9][0-9]-*.md"))


def title(path: pathlib.Path, lines: list[str]) -> str:
    for line in lines:
        if line.startswith("# "):
            return line[2:].strip()
    return path.name


def entry_title(line: str) -> str:
    # An entry headed by its date alone ("**18 Oct 2026.** **Time zones…**") is named by both.
    bolds = BOLD.findall(line)
    name = bolds[0].strip()
    if DATE_ONLY.match(name) and len(bolds) > 1:
        name = f"{name} {bolds[1].strip()}"
    name = name.replace("`", "")
    return name if len(name) <= LONGEST else name[: LONGEST - 1].rstrip() + "…"


def render() -> str:
    out = [HEADER]
    for path in files():
        lines = path.read_text(encoding="utf-8").splitlines()
        out.append(f"\n## [{path.name}]({path.name}): {title(path, lines)}\n")
        fence = False
        depth = 0
        for number, line in enumerate(lines, start=1):
            if line.startswith("```"):
                fence = not fence
            if fence:
                continue
            heading = HEADING.match(line)
            if heading:
                depth = len(heading.group(1)) - 2
                out.append(f"{'  ' * depth}- L{number}: {heading.group(2)}")
                continue
            if ENTRY.match(line):
                out.append(f"{'  ' * (depth + 1)}- L{number}: {entry_title(line)}")
    return "\n".join(out) + "\n"


def main() -> int:
    text = render()
    if "--check" in sys.argv:
        current = INDEX.read_text(encoding="utf-8") if INDEX.exists() else ""
        if current != text:
            print("docs/planning/README.md is behind: python3 tools/planning_index.py")
            return 1
        return 0
    INDEX.write_text(text, encoding="utf-8", newline="\n")
    count = sum(1 for line in text.splitlines() if line.lstrip().startswith("- L"))
    print(f"{INDEX.relative_to(ROOT).as_posix()}: {count} titles")
    return 0


if __name__ == "__main__":
    sys.exit(main())
