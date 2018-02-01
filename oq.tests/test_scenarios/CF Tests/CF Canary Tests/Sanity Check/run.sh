#!/bin/bash
script_dir="$(dirname -- "$(realpath -- "${BASH_SOURCE[0]}")")"
cd "${script_dir}";
source "${script_dir}/env.sh"
paralelJobs=();
rc=0;

echo "[INF]Running substep ./CF Tests/CF Canary Tests/OQ Tests/Setup/Login and Target"
bash "1Login and Target/run.sh";
if [[ $? -ne 0 ]] ; then 
echo "EXECUTION FAILED!./CF Tests/CF Canary Tests/OQ Tests/Setup/Login and Target";
exit 1; fi

echo "[INF]Running substep ./CF Tests/CF Canary Tests/Sanity Check/Execute Sanity Check/Deploy"
bash "2Deploy/run.sh";
if [[ $? -ne 0 ]] ; then 
echo "EXECUTION FAILED!./CF Tests/CF Canary Tests/Sanity Check/Execute Sanity Check/Deploy";
exit 1; fi

echo "[INF]Running substep ./CF Tests/CF Canary Tests/Sanity Check/Execute Sanity Check/Undeploy"
bash "3Undeploy/run.sh";
if [[ $? -ne 0 ]] ; then 
echo "EXECUTION FAILED!./CF Tests/CF Canary Tests/Sanity Check/Execute Sanity Check/Undeploy";
exit 1; fi

exit $rc;
