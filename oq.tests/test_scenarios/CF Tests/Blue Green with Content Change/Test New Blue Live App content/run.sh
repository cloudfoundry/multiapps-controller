#!/bin/bash
script_dir="$(dirname -- "$(realpath -- "${BASH_SOURCE[0]}")")"
cd "${script_dir}";
source "${script_dir}/env.sh"
cd 
bash "${TEST_WORKING_DIRECTORY}/test_scripts/test_bg_live_and_idle_apps.sh"
if [[ $? -ne 0 ]] ; then echo "EXECUTION FAILED!";
exit 1; fi
