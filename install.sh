#!/bin/sh
set -e

# roudan-jdbc-cli one-line installer
# Usage: curl -fsSL https://raw.githubusercontent.com/wsaaaqqq/roudan-jdbc-cli/main/install.sh | bash

REPO="wsaaaqqq/roudan-jdbc-cli"
VERSION="${VERSION:-latest}"
INSTALL_DIR="${INSTALL_DIR:-$HOME/.roudan-cli}"
CMD_NAME="roudan"

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

log()  { printf "${GREEN}[roudan-jdbc-cli]${NC} %s\n" "$1"; }
warn() { printf "${YELLOW}[warn]${NC} %s\n" "$1"; }
err()  { printf "${RED}[error]${NC} %s\n" "$1"; exit 1; }

log "Installing ${CMD_NAME} ${VERSION}..."

# Check java
if ! command -v java >/dev/null 2>&1; then
    err "Java 8+ is required. Install it first: https://adoptium.net"
fi

# Detect OS
OS=$(uname -s | tr '[:upper:]' '[:lower:]')
case "$OS" in
    linux|darwin) ;;
    mingw*|msys*|cygwin*) OS="windows" ;;
    *) err "Unsupported OS: $OS" ;;
esac

# Jar download URL (GitHub Releases)
if [ "$VERSION" = "latest" ]; then
    JAR_URL="https://github.com/${REPO}/releases/latest/download/roudan-jdbc-cli.jar"
else
    JAR_URL="https://github.com/${REPO}/releases/download/${VERSION}/roudan-jdbc-cli.jar"
fi

mkdir -p "$INSTALL_DIR/lib"

log "Downloading ${CMD_NAME} ${VERSION}..."
if ! curl -fL --progress-bar -o "$INSTALL_DIR/lib/roudan-jdbc-cli.jar" "$JAR_URL"; then
    rm -f "$INSTALL_DIR/lib/roudan-jdbc-cli.jar"
    err "Failed to download jar."
fi

# Create wrapper script
WRAPPER="${INSTALL_DIR}/${CMD_NAME}"
if [ "$OS" = "windows" ]; then
    WRAPPER="${INSTALL_DIR}/${CMD_NAME}.bat"
    cat > "$WRAPPER" << 'EOF'
@echo off
set DIR=%~dp0
if exist "%DIR%lib\roudan-jdbc-cli.jar" (
    java -jar "%DIR%lib\roudan-jdbc-cli.jar" %*
) else (
    echo roudan: not installed. Run: npm install -g roudan-jdbc-cli
    exit /b 1
)
EOF
else
    cat > "$WRAPPER" << 'EOF'
#!/bin/sh
DIR="$(dirname "$(readlink -f "$0")")"
if [ -f "$DIR/lib/roudan-jdbc-cli.jar" ]; then
    exec java -jar "$DIR/lib/roudan-jdbc-cli.jar" "$@"
else
    echo "roudan: not installed. Run: npm install -g roudan-jdbc-cli"
    exit 1
fi
EOF
    chmod +x "$WRAPPER"
fi

# Add to PATH
if [ "$OS" != "windows" ]; then
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
if [ "$OS" = "windows" ]; then
    echo "  Add ${INSTALL_DIR} to PATH manually."
    echo "  Run: roudan --help"
else
    echo "  Restart your shell or run:"
    echo "    export PATH=\"${INSTALL_DIR}:\$PATH\""
fi
echo ""
echo "  roudan --help"
