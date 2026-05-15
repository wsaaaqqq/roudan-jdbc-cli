#!/bin/sh
set -e

# roudan-jdbc-cli one-line installer
# Usage: curl -fsSL https://raw.githubusercontent.com/wsaaaqqq/roudan-jdbc-cli/main/install.sh | bash

REPO="wsaaaqqq/roudan-jdbc-cli"
VERSION="${VERSION:-latest}"
INSTALL_DIR="${INSTALL_DIR:-$HOME/.roudan-cli}"
BIN_NAME="roudan-jdbc-cli"
CMD_NAME="rd"

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

log()  { printf "${GREEN}[roudan-jdbc-cli]${NC} %s\n" "$1"; }
warn() { printf "${YELLOW}[warn]${NC} %s\n" "$1"; }
err()  { printf "${RED}[error]${NC} %s\n" "$1"; exit 1; }

log "Installing ${BIN_NAME} ${VERSION}..."

# Detect OS and architecture
OS=$(uname -s | tr '[:upper:]' '[:lower:]')
ARCH=$(uname -m)

case "$ARCH" in
    x86_64|amd64) ARCH="x64" ;;
    aarch64|arm64) ARCH="aarch64" ;;
    *) err "Unsupported architecture: $ARCH" ;;
esac

# Map uname to Adoptium OS name
case "$OS" in
    linux)  ADOPT_OS="linux" ;;
    darwin) ADOPT_OS="mac" ;;
    mingw*|msys*|cygwin*) ADOPT_OS="windows" ;;
    *) err "Unsupported OS: $OS" ;;
esac

# JRE download URL (Adoptium Temurin 8 JRE)
JRE_URL="https://api.adoptium.net/v3/binary/latest/8/ga/${ADOPT_OS}/${ARCH}/jre/hotspot/normal/eclipse"

# Jar download URL (GitHub Releases)
if [ "$VERSION" = "latest" ]; then
    JAR_URL="https://github.com/${REPO}/releases/latest/download/roudan-jdbc-cli.jar"
else
    JAR_URL="https://github.com/${REPO}/releases/download/${VERSION}/roudan-jdbc-cli.jar"
fi

mkdir -p "$INSTALL_DIR/lib" "$INSTALL_DIR/jre8"

# Download JRE
log "Downloading JRE 8 (Adoptium Temurin)..."
JRE_TMP_DIR="/tmp/roudan-jre-$$"
mkdir -p "$JRE_TMP_DIR/extract"
JRE_ARCHIVE="$JRE_TMP_DIR/jre-archive"
curl -fsSL -o "$JRE_ARCHIVE" "$JRE_URL" || err "Failed to download JRE."

# Extract JRE
if [ "$ADOPT_OS" = "windows" ]; then
    unzip -o "$JRE_ARCHIVE" -d "$JRE_TMP_DIR/extract" >/dev/null 2>&1 || err "Failed to extract JRE."
else
    tar xzf "$JRE_ARCHIVE" -C "$JRE_TMP_DIR/extract" 2>/dev/null || err "Failed to extract JRE."
fi

# Find java binary (handle nested top-level dir in archive)
JRE_EXTRACTED=$(find "$JRE_TMP_DIR/extract" -type f -name "java" -path "*/bin/java" 2>/dev/null | head -1)
if [ -n "$JRE_EXTRACTED" ]; then
    JRE_SRC=$(dirname "$(dirname "$JRE_EXTRACTED")")
    cp -R "$JRE_SRC"/* "$INSTALL_DIR/jre8/" 2>/dev/null || true
else
    err "JRE extraction failed: java binary not found."
fi

rm -rf "$JRE_TMP_DIR"

# Download jar
log "Downloading ${BIN_NAME} ${VERSION}..."
curl -fsSL -o "$INSTALL_DIR/lib/${BIN_NAME}.jar" "$JAR_URL" || err "Failed to download jar."

# Create wrapper script
WRAPPER="${INSTALL_DIR}/${CMD_NAME}"
if [ "$ADOPT_OS" = "windows" ]; then
    WRAPPER="${INSTALL_DIR}/${CMD_NAME}.bat"
    cat > "$WRAPPER" << 'WRAPPER_EOF'
@echo off
set DIR=%~dp0
set JAVA=%DIR%jre8\bin\java.exe
if exist "%JAVA%" (
    "%JAVA%" -jar "%DIR%lib\roudan-jdbc-cli.jar" %*
) else (
    java -jar "%DIR%lib\roudan-jdbc-cli.jar" %*
)
WRAPPER_EOF
else
    cat > "$WRAPPER" << 'WRAPPER_EOF'
#!/bin/sh
DIR="$(dirname "$(readlink -f "$0")")"
if [ -x "$DIR/jre8/bin/java" ]; then
    exec "$DIR/jre8/bin/java" -jar "$DIR/lib/roudan-jdbc-cli.jar" "$@"
else
    exec java -jar "$DIR/lib/roudan-jdbc-cli.jar" "$@"
fi
WRAPPER_EOF
    chmod +x "$WRAPPER"
fi

# Add to PATH
if [ "$ADOPT_OS" != "windows" ]; then
    SHELL_RC=""
    case "$SHELL" in
        */zsh)  SHELL_RC="$HOME/.zshrc" ;;
        */bash) SHELL_RC="$HOME/.bashrc" ;;
        */fish) SHELL_RC="$HOME/.config/fish/config.fish" ;;
        *)      SHELL_RC="$HOME/.profile" ;;
    esac

    if ! echo "$PATH" | grep -q "$INSTALL_DIR"; then
        echo "export PATH=\"${INSTALL_DIR}:\$PATH\"" >> "$SHELL_RC"
        log "Added ${INSTALL_DIR} to PATH in ${SHELL_RC}"
    fi
fi

# Verify installation
export PATH="${INSTALL_DIR}:$PATH"
if "${WRAPPER}" --version >/dev/null 2>&1; then
    log "Installation successful!"
    "${WRAPPER}" --version
else
    warn "Installation may have issues. Try: ${WRAPPER} --help"
fi

echo ""
log "Done!"
echo ""
if [ "$ADOPT_OS" = "windows" ]; then
    echo "  Add ${INSTALL_DIR} to PATH manually."
    echo "  Then run: ${CMD_NAME} --help"
else
    echo "  Restart your shell or run:"
    echo "    export PATH=\"${INSTALL_DIR}:\$PATH\""
fi
echo ""
echo "  ${CMD_NAME} --help"
