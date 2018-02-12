h1. Deploy Service Output Qualification Integration Tests



h2. Content

* Test scripts
  Individual scripted functionalities used as building blocks for test scenarios
* Test scripts/test_functions.sh Test scripts/test_utils.sh
  shared bash utils
* Test scenarios
  tree of scripts and configurations, describing multipe test scenarios using the "test scripts"
* Test resources
  content used during tets

* Dockerfile
  Used to build concourse image
 
* Env variables:
- TEST_WORKING_DIRECTORY - location of oq tests root dir. test_resources, test_scripts, test_scenarios, utils.sh reside there
- TEST_RESOURCE_DIRECTORY - defaults to location of all test resources ${TEST_WORKING_DIRECTORY}/test_resources
- APP_LOCATION - location of test resources, usually a directory containing mtar/mtad/mtaext, location should be given relative to the ${TEST_RESOURCE_DIRECTORY}
- MTAR_LOCATION --???