#!/bin/bash
current_dir="$(dirname -- "$(realpath -- "${BASH_SOURCE[0]}")")"
parent_dir="$(dirname -- "${current_dir}")";
if [ -f "${parent_dir}/env.sh" ] ; then
        source "${parent_dir}/env.sh";
fi
export STEP_PATH="${STEP_PATH}/BG Deploy Green with no action"
export APP_LOCATION="bg-archives/green"
export EXPECTED_APPLICATIONS="hello-router-green hello-backend-green hello-router hello-backend"
export SLP_ACTION="none"
