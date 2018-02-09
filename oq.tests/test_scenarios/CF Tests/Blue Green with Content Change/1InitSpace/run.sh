#!/bin/bash
script_dir="$(dirname -- "$(realpath -- "${BASH_SOURCE[0]}")")"
cd "${script_dir}";
source "${script_dir}/env.sh"
cd 
bash "${TEST_REPO_ROOT}/test_scripts/init_space.sh"
if [[ $? -ne 0 ]] ; then echo "EXECUTION FAILED!";

exit 1; fi
