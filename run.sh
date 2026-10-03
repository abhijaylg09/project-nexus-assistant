#!/usr/bin/env bash
# ==============================================================================
# PROJECT N.E.X.U.S - macOS & Linux Launcher
# Runs JavaFX Desktop HUD with Hardware Vision and Deep Learning AI
# ==============================================================================
set -e

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" >/dev/null 2>&1 && pwd)"
cd "$DIR"

echo "================================================================"
echo "      PROJECT N.E.X.U.S (Neural EXecutive User System)"
echo "   Centralized Java Orchestrator - macOS & Linux Desktop"
echo "================================================================"

# If standalone fat JAR exists, attempt direct run; otherwise run via Maven JavaFX plugin
if [ -f "target/nexus-ai-assistant-1.0.0.jar" ]; then
    echo "[N.E.X.U.S] Launching packaged executable JAR..."
    java -jar "target/nexus-ai-assistant-1.0.0.jar" || {
        echo "[N.E.X.U.S] Fallback: Launching via Maven JavaFX runner..."
        mvn javafx:run
    }
else
    echo "[N.E.X.U.S] Launching via Maven JavaFX runner..."
    mvn javafx:run
fi
