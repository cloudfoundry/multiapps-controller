#!/bin/bash
set -e

PREFIX="$HOME/app/nginx/nginx-build"

cd "nginx" # name of the nginx folder in webapp (~/app/nginx in container)

# Find the nginx tarball (assumes only one .tar.gz file exists)
NGINX_TARBALL=$(ls nginx-*.tar.gz | head -1)
if [ -z "$NGINX_TARBALL" ]; then
    echo "Error: No nginx tarball found (nginx-*.tar.gz)"
    exit 1
fi

echo "Found nginx tarball: $NGINX_TARBALL"
tar -xzf "$NGINX_TARBALL"

# Extract directory name from tarball (remove .tar.gz extension)
NGINX_DIR=$(basename "$NGINX_TARBALL" .tar.gz)
cd "$NGINX_DIR"

./configure --prefix="$PREFIX"
make
make install

# Copy the nginx configuration file
cp ../nginx.conf "$PREFIX/conf/nginx.conf"

# Configure Dynatrace OneAgent for NGINX monitoring
# Find the liboneagentproc.so library
DT_AGENT_PATH="/home/vcap/app/.java-buildpack/dynatrace_one_agent"
ONEAGENT_LIB=$(find "$DT_AGENT_PATH" -name "liboneagentproc.so" 2>/dev/null | head -1)

if [ -n "$ONEAGENT_LIB" ] && [ -f "$ONEAGENT_LIB" ]; then
    echo "Dynatrace OneAgent found: $ONEAGENT_LIB"
    export LD_PRELOAD="$ONEAGENT_LIB"
    export DT_LOGSTREAM=stdout
    echo "Dynatrace monitoring enabled for NGINX"
else
    echo "WARNING: Dynatrace OneAgent library not found - NGINX will run without monitoring"
fi

echo "Starting NGINX (reverse proxy to 8081) on port 8080..."
# Run nginx in the foreground (disable daemon mode) so the process stays alive.
# Otherwise nginx forks to background, parent process exits immediately,
# and Cloud Foundry will think the task/sidecar is done and stop the container.
"$PREFIX/sbin/nginx" -g "daemon off;"
