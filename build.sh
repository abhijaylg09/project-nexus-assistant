#!/usr/bin/env bash
# ==============================================================================
# PROJECT N.E.X.U.S - UNIX Build Script (macOS / Linux)
# Compiles Java 21 codebase and installs Python AI dependencies
# ==============================================================================
set -e

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" >/dev/null 2>&1 && pwd)"
cd "$DIR"

echo "======================================================="
echo "  PROJECT N.E.X.U.S - BUILDING MAVEN CODEBASE (macOS / Linux)"
echo "  Team STI25CS - Java Multimodal Personal AI Assistant"
echo "======================================================="

# Verify Java
if ! command -v java >/dev/null 2>&1; then
    echo "❌ Error: Java 21+ compiler is required but not installed."
    echo "💡 On macOS: brew install openjdk@21"
    echo "💡 On Ubuntu/Debian: sudo apt install openjdk-21-jdk"
    exit 1
fi

JAVA_VER=$(java -version 2>&1 | head -n 1)
echo "✅ Java: $JAVA_VER"

# Verify Maven
if ! command -v mvn >/dev/null 2>&1; then
    echo "❌ Error: Apache Maven ('mvn') is not found."
    echo "💡 On macOS: brew install maven"
    echo "💡 On Ubuntu/Debian: sudo apt install maven"
    exit 1
fi

echo "✅ Maven: $(mvn -v | head -n 1)"

# Check Python environment & install requirements
echo ""
echo "📦 Checking Python deep learning dependencies..."
if command -v python3 >/dev/null 2>&1; then
    python3 -m pip install --upgrade pip 2>/dev/null || true
    python3 -m pip install -r requirements.txt || true
elif command -v python >/dev/null 2>&1; then
    python -m pip install -r requirements.txt || true
else
    echo "⚠️ Notice: Python 3 not found. Deep learning gender classification will use optical fallback."
fi

# Run Maven build
echo ""
echo "🔨 Running Maven package..."
mvn clean package -DskipTests

echo ""
echo "======================================================="
echo "  🎉 BUILD COMPLETED SUCCESSFULLY!"
echo "  To launch N.E.X.U.S on macOS or Linux, run:"
echo "      ./run.sh"
echo "======================================================="
