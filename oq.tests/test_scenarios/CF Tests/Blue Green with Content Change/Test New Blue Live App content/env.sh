#!/bin/bash
current_dir="$(dirname -- "$(realpath -- "${BASH_SOURCE[0]}")")"
parent_dir="$(dirname -- "${current_dir}")";
if [ -f "${parent_dir}/env.sh" ] ; then
        source "${parent_dir}/env.sh";
fi
export STEP_PATH="${STEP_PATH}/Test New Blue Live App content"
export EXPECTED_LIVE_OUTPUT="BLUE"
export LIVE_APP_NAME="hello-router-blue"
export REST_API_PATH="hello"
