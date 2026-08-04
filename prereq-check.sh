#!/usr/bin/env bash
# ============================================================
# Quarkus Workshop — Prerequisites Check
# Run this before the workshop to verify your environment.
# ============================================================

GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[0;33m'
NC='\033[0m' # No Colour

PASS=0
FAIL=0

check_pass() { echo -e "  ${GREEN}✔${NC}  $1"; ((PASS++)); }
check_fail() { echo -e "  ${RED}✗${NC}  $1"; ((FAIL++)); }
check_warn() { echo -e "  ${YELLOW}⚠${NC}  $1"; }

echo ""
echo "============================================"
echo " Quarkus Workshop — Prerequisites Check"
echo "============================================"
echo ""

# ── 1. Java 21+ ─────────────────────────────────
echo "[ Java ]"
if command -v java &>/dev/null; then
  JAVA_VER=$(java -version 2>&1 | head -1 | sed 's/.*version "\([0-9]*\).*/\1/')
  if [ "$JAVA_VER" -ge 21 ] 2>/dev/null; then
    check_pass "Java $JAVA_VER detected (21+ required)"
  else
    check_fail "Java $JAVA_VER detected — Java 21+ required. Install via: sdk install java 21.0.3-tem"
  fi
else
  check_fail "java not found. Install from https://adoptium.net or via sdkman: sdk install java 21.0.3-tem"
fi
echo ""

# ── 2. Quarkus CLI or Maven 3.9+ ─────────────────
echo "[ Build Tool ]"
if command -v quarkus &>/dev/null; then
  QUARKUS_VER=$(quarkus version 2>&1 | head -1)
  check_pass "Quarkus CLI found — $QUARKUS_VER"
elif command -v mvn &>/dev/null; then
  MVN_VER=$(mvn -version 2>&1 | head -1 | sed 's/Apache Maven \([0-9.]*\).*/\1/')
  MVN_MAJOR=$(echo "$MVN_VER" | cut -d. -f1)
  MVN_MINOR=$(echo "$MVN_VER" | cut -d. -f2)
  if [ "$MVN_MAJOR" -ge 3 ] && [ "$MVN_MINOR" -ge 9 ] 2>/dev/null; then
    check_pass "Maven $MVN_VER detected (3.9+ required)"
  else
    check_fail "Maven $MVN_VER detected — 3.9+ required. Upgrade via: sdk install maven 3.9.6"
  fi
else
  check_fail "Neither 'quarkus' CLI nor 'mvn' found. Install Quarkus CLI: sdk install quarkus"
fi
echo ""

# ── 3. Docker or Podman (required for Labs 4 & 5) ─
echo "[ Container Runtime — required for Labs 4 & 5 ]"
DOCKER_OK=false
PODMAN_OK=false

if command -v docker &>/dev/null; then
  if timeout 5 docker ps &>/dev/null; then
    check_pass "Docker is installed and running"
    DOCKER_OK=true
  else
    check_warn "Docker is installed but NOT running — start Docker Desktop before Labs 4 & 5"
  fi
fi

if command -v podman &>/dev/null; then
  if timeout 5 podman ps &>/dev/null; then
    check_pass "Podman is installed and running"
    PODMAN_OK=true
  else
    check_warn "Podman is installed but NOT running — start Podman before Labs 4 & 5"
  fi
fi

if [ "$DOCKER_OK" = false ] && [ "$PODMAN_OK" = false ]; then
  if ! command -v docker &>/dev/null && ! command -v podman &>/dev/null; then
    check_fail "Neither Docker nor Podman found. Install Docker Desktop: https://docs.docker.com/get-docker/"
  fi
fi
echo ""

# ── 4. OpenAI API Key (required for Lab 7) ────────
# The Quarkus LangChain4j extension maps the env var QUARKUS_LANGCHAIN4J_OPENAI_API_KEY
# to the config property quarkus.langchain4j.openai.api-key automatically.
# Setting the env var is equivalent to setting the property in application.properties —
# use the env var form so the key is never written to disk.
echo "[ OpenAI API Key — required for Lab 7 ]"
if [ -n "$QUARKUS_LANGCHAIN4J_OPENAI_API_KEY" ]; then
  MASKED="${QUARKUS_LANGCHAIN4J_OPENAI_API_KEY:0:8}..."
  check_pass "QUARKUS_LANGCHAIN4J_OPENAI_API_KEY is set ($MASKED)"
else
  check_warn "QUARKUS_LANGCHAIN4J_OPENAI_API_KEY is not set — needed only for Lab 7 (AI chatbot)"
  check_warn "Set it with: export QUARKUS_LANGCHAIN4J_OPENAI_API_KEY=sk-..."
fi
echo ""

# ── Summary ───────────────────────────────────────
echo "============================================"
if [ "$FAIL" -eq 0 ]; then
  echo -e " ${GREEN}All required checks passed! ($PASS passed, $FAIL failed)${NC}"
  echo " You're ready for the workshop."
else
  echo -e " ${RED}$FAIL check(s) failed. Please fix the issues above before the workshop.${NC}"
  echo " ($PASS passed, $FAIL failed)"
fi
echo "============================================"
echo ""
