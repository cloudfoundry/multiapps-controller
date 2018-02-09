#!/bin/bash
script_dir="$(dirname -- "$(realpath -- "${BASH_SOURCE[0]}")")"
cd "${script_dir}";
source "${script_dir}/env.sh"
paralelJobs=();
rc=0;
echo "[INF]Running substep ./SL4XS2/CF Tests/CF Canary Tests/OQ Tests/Test Suites/Functionality Tests/Blue Green with Content Change/1InitSpace"
bash "1InitSpace/run.sh";
if [[ $? -ne 0 ]] ; then 
echo "EXECUTION FAILED!./SL4XS2/CF Tests/CF Canary Tests/OQ Tests/Test Suites/Functionality Tests/Blue Green with Content Change/1InitSpace";
exit 1; fi
echo "[INF]Running substep ./SL4XS2/CF Tests/CF Canary Tests/OQ Tests/Test Suites/Functionality Tests/Blue Green with Content Change/Undeploy MTA leftovers"
bash "Undeploy MTA leftovers/run.sh";
if [[ $? -ne 0 ]] ; then 
echo "EXECUTION FAILED!./SL4XS2/CF Tests/CF Canary Tests/OQ Tests/Test Suites/Functionality Tests/Blue Green with Content Change/Undeploy MTA leftovers";
exit 1; fi
echo "[INF]Running substep ./SL4XS2/CF Tests/CF Canary Tests/OQ Tests/Test Suites/Functionality Tests/Blue Green with Content Change/Delete Space"
bash "Delete Space/run.sh";
if [[ $? -ne 0 ]] ; then 
echo "EXECUTION FAILED!./SL4XS2/CF Tests/CF Canary Tests/OQ Tests/Test Suites/Functionality Tests/Blue Green with Content Change/Delete Space";
exit 1; fi
echo "[INF]Running substep ./SL4XS2/CF Tests/CF Canary Tests/OQ Tests/Test Suites/Functionality Tests/Blue Green with Content Change/Test Normal Deploy Blue content"
bash "Test Normal Deploy Blue content/run.sh";
if [[ $? -ne 0 ]] ; then 
echo "EXECUTION FAILED!./SL4XS2/CF Tests/CF Canary Tests/OQ Tests/Test Suites/Functionality Tests/Blue Green with Content Change/Test Normal Deploy Blue content";
exit 1; fi
echo "[INF]Running substep ./SL4XS2/CF Tests/CF Canary Tests/OQ Tests/Test Suites/Functionality Tests/Blue Green with Content Change/BG Deploy Green with no action"
bash "BG Deploy Green with no action/run.sh";
if [[ $? -ne 0 ]] ; then 
echo "EXECUTION FAILED!./SL4XS2/CF Tests/CF Canary Tests/OQ Tests/Test Suites/Functionality Tests/Blue Green with Content Change/BG Deploy Green with no action";
exit 1; fi
echo "[INF]Running substep ./SL4XS2/CF Tests/CF Canary Tests/OQ Tests/Test Suites/Functionality Tests/Blue Green with Content Change/Test Live and Idle Apps"
bash "Test Live and Idle Apps/run.sh";
if [[ $? -ne 0 ]] ; then 
echo "EXECUTION FAILED!./SL4XS2/CF Tests/CF Canary Tests/OQ Tests/Test Suites/Functionality Tests/Blue Green with Content Change/Test Live and Idle Apps";
exit 1; fi
echo "[INF]Running substep ./SL4XS2/CF Tests/CF Canary Tests/OQ Tests/Test Suites/Functionality Tests/Blue Green with Content Change/Resume BG deploy"
bash "Resume BG deploy/run.sh";
if [[ $? -ne 0 ]] ; then 
echo "EXECUTION FAILED!./SL4XS2/CF Tests/CF Canary Tests/OQ Tests/Test Suites/Functionality Tests/Blue Green with Content Change/Resume BG deploy";
exit 1; fi
echo "[INF]Running substep ./SL4XS2/CF Tests/CF Canary Tests/OQ Tests/Test Suites/Functionality Tests/Blue Green with Content Change/Test New Green Live App content"
bash "Test New Green Live App content/run.sh";
if [[ $? -ne 0 ]] ; then 
echo "EXECUTION FAILED!./SL4XS2/CF Tests/CF Canary Tests/OQ Tests/Test Suites/Functionality Tests/Blue Green with Content Change/Test New Green Live App content";
exit 1; fi
echo "[INF]Running substep ./SL4XS2/CF Tests/CF Canary Tests/OQ Tests/Test Suites/Functionality Tests/Blue Green with Content Change/BG Deploy Blue no confirm"
bash "BG Deploy Blue no confirm/run.sh";
if [[ $? -ne 0 ]] ; then 
echo "EXECUTION FAILED!./SL4XS2/CF Tests/CF Canary Tests/OQ Tests/Test Suites/Functionality Tests/Blue Green with Content Change/BG Deploy Blue no confirm";
exit 1; fi
echo "[INF]Running substep ./SL4XS2/CF Tests/CF Canary Tests/OQ Tests/Test Suites/Functionality Tests/Blue Green with Content Change/Test New Blue Live App content"
bash "Test New Blue Live App content/run.sh";
if [[ $? -ne 0 ]] ; then 
echo "EXECUTION FAILED!./SL4XS2/CF Tests/CF Canary Tests/OQ Tests/Test Suites/Functionality Tests/Blue Green with Content Change/Test New Blue Live App content";
exit 1; fi
echo "[INF]Running substep ./SL4XS2/CF Tests/CF Canary Tests/OQ Tests/Test Suites/Functionality Tests/Blue Green with Content Change/Undeploy MTA"
bash "Undeploy MTA/run.sh";
if [[ $? -ne 0 ]] ; then 
echo "EXECUTION FAILED!./SL4XS2/CF Tests/CF Canary Tests/OQ Tests/Test Suites/Functionality Tests/Blue Green with Content Change/Undeploy MTA";
exit 1; fi
exit $rc;
