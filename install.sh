#!/bin/sh
set -e

# roudan-jdbc-cli one-line installer
# Usage: curl -fsSL https://raw.githubusercontent.com/wsaaaqqq/roudan-jdbc-cli/main/install.sh | bash

REPO="wsaaaqqq/roudan-jdbc-cli"
VERSION="${VERSION:-latest}"
INSTALL_DIR="${INSTALL_DIR:-$HOME/.roudan-cli}"
BIN_NAME="roudan-jdbc-cli"

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

log()  { printf "${GREEN}[roudan-cli]${NC} %s\n" "$1"; }
warn() { printf "${YELLOW}[warn]${NC} %s\n" "$1"; }
err()  { printf "${RED}[error]${NC} %s\n" "$1"; exit 1; }

# Check prerequisites
log "Checking prerequisites..."

# Java check
if ! command -v java >/dev/null 2>&1; then
    err "Java 8+ is required but not found. Install Java and retry."
fi

JAVA_VER=$(java -version 2>&1 | head -1 | cut -d'"' -f2 | cut -d'.' -f1)
if [ "${JAVA_VER:-0}" = "1" ]; then
    JAVA_VER=$(java -version 2>&1 | head -1 | cut -d'"' -f2 | cut -d'.' -f2)
fi
if [ "${JAVA_VER:-0}" -lt 8 ] 2>/dev/null; then
    warn "Java 8+ recommended, found Java ${JAVA_VER}"
fi

# Detect OS and architecture
OS=$(uname -s | tr '[:upper:]' '[:lower:]')
ARCH=$(uname -m)

case "$ARCH" in
    x86_64|amd64) ARCH="amd64" ;;
    aarch64|arm64) ARCH="arm64" ;;
    *) err "Unsupported architecture: $ARCH" ;;
esac

# Determine download URL
if [ "$VERSION" = "latest" ]; then
    DOWNLOAD_URL="https://github.com/${REPO}/releases/latest/download/roudan-jdbc-cli.zip"
else
    DOWNLOAD_URL="https://github.com/${REPO}/releases/download/${VERSION}/roudan-jdbc-cli.zip"
fi

# Install
log "Installing roudan-jdbc-cli ${VERSION}..."
mkdir -p "$INSTALL_DIR"

TMP_ZIP=$(mktemp)
curl -fsSL "$DOWNLOAD_URL" -o "$TMP_ZIP" || {
    # Fallback: build from jar
    warn "No binary release for ${VERSION}, trying jar..."
    JAR_URL="https://github.com/${REPO}/releases/${VERSION}/download/roudan-jdbc-cli.jar"
    curl -fsSL "$JAR_URL" -o "${INSTALL_DIR}/lib/${BIN_NAME}.jar" || \
        err "Failed to download. Check VERSION or network."
}

# Extract if zip
if [ -f "$TMP_ZIP" ]; then
    unzip -o "$TMP_ZIP" -d "$INSTALL_DIR" >/dev/null 2>&1 || \
        err "Failed to extract archive."
    rm -f "$TMP_ZIP"
fi

# Create wrapper script
WRAPPER="${INSTALL_DIR}/${BIN_NAME}"
cat > "$WRAPPER" << 'WRAPPER_EOF'
#!/bin/sh
DIR="$(dirname "$(readlink -f "$0")")"
exec java -jar "${DIR}/lib/roudan-jdbc-cli.jar" "$@"
WRAPPER_EOF
chmod +x "$WRAPPER"

# Add to PATH
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

# Verify installation
export PATH="${INSTALL_DIR}:$PATH"
if "${INSTALL_DIR}/${BIN_NAME}" --version >/dev/null 2>&1; then
    log "Installation successful!"
    "${INSTALL_DIR}/${BIN_NAME}" --version
else
    warn "Installation may have issues. Try: ${INSTALL_DIR}/${BIN_NAME} --help"
fi

echo ""
log "Done! Restart your shell or run:"
echo "  export PATH=\"${INSTALL_DIR}:\$PATH\""
echo ""
echo "  roudan-jdbc-cli --help"
