#!/usr/bin/env bash
# Build .deb package for NoteKeeper
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
echo "==> Building note-keeper_${VERSION}_amd64.deb"

echo "==> Maven package"
./mvnw -B -DskipTests package

JAR="$(ls -1 note-keeper-service/target/note-keeper-service-*.jar 2>/dev/null | grep -v original | head -n 1 || true)"
if [ -z "$JAR" ] || [ ! -f "$JAR" ]; then
    echo "ERROR: boot jar not found in note-keeper-service/target"
    exit 1
fi

STAGE=$(mktemp -d)
trap 'rm -rf "$STAGE"' EXIT

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
install -m 0644 "$JAR" "${PKG}/opt/note-keeper/lib/note-keeper.jar"
install -m 0644 "${SCRIPT_DIR}/application.yaml" "${PKG}/opt/note-keeper/share/application.yaml"
install -m 0644 "${SCRIPT_DIR}/logrotate" "${PKG}/etc/logrotate.d/note-keeper"
install -m 0644 "${SCRIPT_DIR}/note-keeper.service" "${PKG}/lib/systemd/system/note-keeper.service"

sed "s/^Version:.*/Version: ${VERSION}/" "${SCRIPT_DIR}/DEBIAN/control" \
    > "${PKG}/DEBIAN/control"

for f in preinst postinst prerm postrm; do
    install -m 0755 "${SCRIPT_DIR}/DEBIAN/${f}" "${PKG}/DEBIAN/${f}"
done

mkdir -p dist
echo "==> Running dpkg-deb"
dpkg-deb --build --root-owner-group "${PKG}" "dist/note-keeper_${VERSION}_amd64.deb"

echo ""
echo "==> Package ready: dist/note-keeper_${VERSION}_amd64.deb"
ls -lh "dist/note-keeper_${VERSION}_amd64.deb"
echo ""
echo "Install with:"
echo "  sudo dpkg -i dist/note-keeper_${VERSION}_amd64.deb"
echo "  sudo apt-get install -f"
