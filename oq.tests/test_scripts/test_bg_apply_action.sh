export PARENT_DIR=$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd );
source $(dirname "${PARENT_DIR}")/test_functions.sh
generate_local_executable "$0"

#example input:
#MTA_ID="com.sap.example.app" 
#SLP_ACTION="abort"
apply_bg_action