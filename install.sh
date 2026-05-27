#!/bin/sh
set -e

REPO="wsaaaqqq/roudan-jdbc-cli"
VERSION="${VERSION:-latest}"
INSTALL_DIR="${INSTALL_DIR:-$HOME/.roudan-cli}"
CMD_NAME="roudan"

log()  { printf "\033[0;32m[roudan-jdbc-cli]\033[0m %s\n" "$1"; }
err()  { printf "\033[0;31m[error]\033[0m %s\n" "$1"; exit 1; }

log "Installing ${CMD_NAME} ${VERSION}..."

if ! command -v java >/dev/null 2>&1; then
    err "Java 8+ is required. Install from https://adoptium.net"
fi

OS=$(uname -s | tr '[:upper:]' '[:lower:]')
case "$OS" in
    linux|darwin) ;;
    mingw*|msys*|cygwin*) OS="windows" ;;
    *) err "Unsupported OS: $OS" ;;
esac

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

# Create wrapper
WRAPPER="${INSTALL_DIR}/${CMD_NAME}"
if [ "$OS" = "windows" ]; then
    WRAPPER="${INSTALL_DIR}/${CMD_NAME}.bat"
    cat > "$WRAPPER" << 'EOF'
@echo off
set DIR=%~dp0
if exist "%DIR%lib\roudan-jdbc-cli.jar" (
    java -jar "%DIR%lib\roudan-jdbc-cli.jar" %*
) else (
    echo roudan: jar not found. Download from https://github.com/wsaaaqqq/roudan-jdbc-cli/releases/latest
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
    echo "roudan: jar not found. Download from https://github.com/wsaaaqqq/roudan-jdbc-cli/releases/latest"
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

export PATH="${INSTALL_DIR}:$PATH"
if "${WRAPPER}" --version >/dev/null 2>&1; then
    log "Installation successful!"
else
    log "Done. Run: roudan --help"
fi
