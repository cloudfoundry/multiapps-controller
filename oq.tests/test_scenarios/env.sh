#!/bin/bash
current_dir="$(dirname -- "$(realpath -- "${BASH_SOURCE[0]}")")"
parent_dir="$(dirname -- "${current_dir}")";
if [ -f "${parent_dir}/env.sh" ] ; then
  source "${parent_dir}/env.sh";
else 
  export TEST_REPO_ROOT="${parent_dir}"
		
fi
export STEP_PATH="${STEP_PATH}/ROOT"

