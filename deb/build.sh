#!/usr/bin/env bash
# Build .deb with GraalVM native binary (no JVM at runtime).
# Usage: ./deb/build.sh [version]
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
cd "$PROJECT_ROOT"

if [ -n "${1:-}" ]; then
    VERSION="$1"
elif [ -f "$PROJECT_ROOT/version.txt" ]; then
    VERSION="$(tr -d '[:space:]' < "$PROJECT_ROOT/version.txt")"
else
    VERSION="0.0.0-$(date +%Y%m%d)"
fi
echo "==> Building note-keeper_${VERSION}_amd64.deb (native)"

STAGE=$(mktemp -d)
NATIVE_DIR="${STAGE}/native"
trap 'rm -rf "$STAGE"' EXIT
mkdir -p "$NATIVE_DIR"

find_local_native() {
    local candidate
    for candidate in \
        "${NOTE_KEEPER_NATIVE:-}" \
        "${PROJECT_ROOT}/note-keeper-service/target/note-keeper" \
        "${PROJECT_ROOT}/note-keeper"
    do
        if [ -n "$candidate" ] && [ -f "$candidate" ] && [ -x "$candidate" ]; then
            if file "$candidate" 2>/dev/null | grep -qi 'ELF'; then
                echo "$candidate"
                return 0
            fi
        fi
    done
    return 1
}

extract_native_from_docker() {
    command -v docker >/dev/null 2>&1 || {
        echo "ERROR: docker not found; cannot build native image"
        exit 1
    }
    echo "==> Native image via Dockerfile.native (ol9, x86-64-v2)"
    docker build -f Dockerfile.native -t note-keeper-native:deb "$PROJECT_ROOT"
    local cid
    cid="$(docker create note-keeper-native:deb)"
    docker cp "${cid}:/app/." "$NATIVE_DIR/"
    docker rm "$cid" >/dev/null
}

LOCAL_BIN="$(find_local_native || true)"
if [ -n "${LOCAL_BIN:-}" ]; then
    echo "==> Reusing native binary: ${LOCAL_BIN}"
    cp "$LOCAL_BIN" "${NATIVE_DIR}/note-keeper"
    src_dir="$(dirname "$LOCAL_BIN")"
    find "$src_dir" -maxdepth 1 -name 'lib*.so' -exec cp {} "$NATIVE_DIR/" \; || true
else
    extract_native_from_docker
fi

chmod +x "${NATIVE_DIR}/note-keeper"
if [ ! -f "${NATIVE_DIR}/note-keeper" ]; then
    echo "ERROR: native binary missing after build"
    exit 1
fi
if command -v file >/dev/null 2>&1; then
    file "${NATIVE_DIR}/note-keeper"
fi
ls -lh "$NATIVE_DIR"

PKG="${STAGE}/note-keeper"
mkdir -p "${PKG}/DEBIAN"
mkdir -p "${PKG}/opt/note-keeper/bin"
mkdir -p "${PKG}/opt/note-keeper/lib"
mkdir -p "${PKG}/opt/note-keeper/etc"
mkdir -p "${PKG}/opt/note-keeper/var"
mkdir -p "${PKG}/opt/note-keeper/share"
mkdir -p "${PKG}/var/log/note-keeper"
mkdir -p "${PKG}/etc/logrotate.d"
mkdir -p "${PKG}/lib/systemd/system"

install -m 0755 "${SCRIPT_DIR}/note-keeper.sh" "${PKG}/opt/note-keeper/bin/note-keeper"
install -m 0755 "${NATIVE_DIR}/note-keeper" "${PKG}/opt/note-keeper/lib/note-keeper"
find "$NATIVE_DIR" -maxdepth 1 -name 'lib*.so' -exec install -m 0644 {} "${PKG}/opt/note-keeper/lib/" \; || true
install -m 0644 "${SCRIPT_DIR}/application.yaml" "${PKG}/opt/note-keeper/share/application.yaml"
install -m 0644 "${SCRIPT_DIR}/logrotate" "${PKG}/etc/logrotate.d/note-keeper"
install -m 0644 "${SCRIPT_DIR}/note-keeper.service" "${PKG}/lib/systemd/system/note-keeper.service"

sed "s/^Version:.*/Version: ${VERSION}/" "${SCRIPT_DIR}/DEBIAN/control" \
    > "${PKG}/DEBIAN/control"

for f in preinst postinst prerm postrm; do
    install -m 0755 "${SCRIPT_DIR}/DEBIAN/${f}" "${PKG}/DEBIAN/${f}"
done

mkdir -p dist
DEB="dist/note-keeper_${VERSION}_amd64.deb"
echo "==> Running dpkg-deb"
if command -v dpkg-deb >/dev/null 2>&1; then
    dpkg-deb --build --root-owner-group "${PKG}" "$DEB"
else
    command -v docker >/dev/null 2>&1 || {
        echo "ERROR: dpkg-deb missing and docker not available"
        exit 1
    }
    echo "==> dpkg-deb via debian:bookworm-slim"
    docker run --rm \
        -v "${PKG}:/pkg:ro" \
        -v "${PROJECT_ROOT}/dist:/out" \
        debian:bookworm-slim \
        bash -c "apt-get update -qq && DEBIAN_FRONTEND=noninteractive apt-get install -y -qq dpkg >/dev/null && dpkg-deb --build --root-owner-group /pkg /out/note-keeper_${VERSION}_amd64.deb"
fi

echo ""
echo "==> Package ready: ${DEB}"
ls -lh "$DEB"
echo ""
echo "Install with:"
echo "  sudo dpkg -i ${DEB}"
echo "  sudo apt-get install -f"
