#!/bin/bash
current_dir="$(dirname -- "$(realpath -- "${BASH_SOURCE[0]}")")"
parent_dir="$(dirname -- "${current_dir}")";
if [ -f "${parent_dir}/env.sh" ] ; then
        source "${parent_dir}/env.sh";
fi
export STEP_PATH="${STEP_PATH}/Test Live and Idle Apps"
export EXPECTED_IDLE_OUTPUT="GREEN"
export EXPECTED_LIVE_OUTPUT="BLUE"
export IDLE_APP_NAME="hello-router-green"
export LIVE_APP_NAME="hello-router"
export REST_API_PATH="hello"
