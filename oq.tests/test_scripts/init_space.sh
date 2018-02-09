export PARENT_DIR=$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd );
source $(dirname "${PARENT_DIR}")/test_functions.sh

#required input: 
#USER_NAME= to be set
#USER_PASS= to be set
#ORG_NAME to be set
#SPACE_NAME to be set to target space name
#DEFAULT_SPACE_NAME existing space for initial login
#DEFAULT_ORG_NAME existing org for initial login
#RECREATE_SPACE recreateSpace or YES would trigger recreation
if [[ "${RECREATE_SPACE}" == "recreateSpace"  ||  "${RECREATE_SPACE}" == "YES" ]] ; then
    init_space "recreateSpace";
else
    init_space ;
fi

