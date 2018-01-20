echo "generating timestamp in ${1}"
outputDir="$( dirname ${1} )"
echo "checking directory ${outputDir}"
if [ -d ${outputDir} ] ; then
  echo "containing dir exists"
else
  echo "FAIL!"
  exit 1;
fi
date +%s > "${1}"
echo "Timestmap generated:"
cat ${1}
