#!/bin/bash
current_dir="$(dirname -- "$(realpath -- "${BASH_SOURCE[0]}")")"
parent_dir="$(dirname -- "${current_dir}")";
if [ -f "${parent_dir}/env.sh" ] ; then
        source "${parent_dir}/env.sh";
fi
export STEP_PATH="${STEP_PATH}/Blue Green with Content Change"
if [ ! -z "${SPACE_PREFIX}" ] ; then
    export SPACE_NAME="${SPACE_PREFIX}bg-deploy"
else
    export SPACE_NAME="bg-deploy"
fi
RECREATE_SPACE="YES"
