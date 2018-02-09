#!/bin/bash
current_dir="$(dirname -- "$(realpath -- "${BASH_SOURCE[0]}")")"
parent_dir="$(dirname -- "${current_dir}")";
if [ -f "${parent_dir}/env.sh" ] ; then
        source "${parent_dir}/env.sh";
fi
export STEP_PATH="${STEP_PATH}/BG Deploy Blue no confirm"
export ADDITIONAL_OPTIONS="--no-confirm"
export APP_LOCATION="bg-archives/blue"
export EXPECTED_APPLICATIONS="hello-router-blue hello-backend-blue"
export SLP_ACTION="none"
