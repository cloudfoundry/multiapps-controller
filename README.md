# cf-mta-deploy-service [![Build Status](https://travis-ci.org/SAP/cf-mta-deploy-service.svg?branch=master)](https://travis-ci.org/SAP/cf-mta-deploy-service)

MTA deploy service for [Cloud Foundry](https://www.cloudfoundry.org/) is based on the [Multi-Target Application (MTA)](https://www.sap.com/documents/2016/06/e2f618e4-757c-0010-82c7-eda71af511fa.html) model in which CF applications are modelled as modules, while CF services as resources. The MTA model enables the delivery of packaged applications, where any target specific configuration could be specified on deployment time without changing application code.
CF MTA deploy service provides the possibility to operate (deploy, update, undeploy) [MTA](https://www.sap.com/documents/2016/06/e2f618e4-757c-0010-82c7-eda71af511fa.html) modeled applications via a single command, while ensuring the consistency and completeness of the different application components.


# Components
## com.sap.cloud.lm.sl.cf.core
Contains the domain model, persistence and other core services and utilities.

## com.sap.cloud.lm.sl.cf.client
Extends the [Java Client Library for Cloud Foundry](https://github.com/SAP/cf-java-client-sap) with additional domain model objects and attributes, OAuth token providers and retrying functionality.  

## com.sap.cloud.lm.sl.cf.process
Contains the concrete workflow definitions for MTA operations like deploy, undeploy, blue-green deploy, etc. These are modelled via [Activiti](https://activiti.org) BPMN process definitions. The process definitions have steps, where each step logic uses the `com.sap.cloud.lm.sl.cf.client` to call the Cloud Controller API from [Cloud Foundry](https://www.cloudfoundry.org/).

## com.sap.cloud.lm.sl.cf.web
Contains REST API implementations for:
- performing MTA applications - executing deploy, undeploy, blue-green deploy
- listing deployed MTA applications
- listing MTA operations - both on-going and historic ones
- reading and writing cross MTA configurations

The result from the build of this component is a WAR file which is the deployable assembly of the CF deploy service.

# Configuration
The CF MTA deploy service is run as a standard application in [Cloud Foundry](https://www.cloudfoundry.org/). Thus, it uses the widely adopted environment-based configuration mechanism. The configuration file for the application is located at [manifest.yml](https://github.com/SAP/cf-mta-deploy-service/blob/master/com.sap.cloud.lm.sl.cf.web/manifests/manifest.yml). There could be configured the following:

Env Variable Name | Sample Value | Description 
--- | --- | ---
XS_TARGET_URL | http://api.bosh-lite.com | Cloud Foundry API URL.
PLATFORMS_V2 | see [manifest.yml](https://github.com/SAP/cf-mta-deploy-service/blob/master/com.sap.cloud.lm.sl.cf.web/manifests/manifest.yml) | Contains the configuration of the MTA module and resource types. MTA module types are mapped to Cloud Foundry buildpacks and default application parameters, whereas MTA resource types are usually mapped to Cloud Foundry services with concrete service plans and parameters. This is for MTA spec v2.
PLATFORMS_V3 | see [manifest.yml](https://github.com/SAP/cf-mta-deploy-service/blob/master/com.sap.cloud.lm.sl.cf.web/manifests/manifest.yml) | Same as PLATFORMS_V2, but this is for MTA spec v3.
DB_TYPE | POSTGRESQL | The used persistence type. Currently only PostgreSQL is supported.
SKIP_SSL_VALIDATION | true | Skips SSL certificate validation.


# Building
## Prerequisites
All components are built with Java 8 and [Apache Maven](http://maven.apache.org/).
## Compiling and Packaging
To build all components, run the following command from the root directory:
```
$ mvn clean install
```
The deployable result from building components is a WAR file, located at `com.sap.cloud.lm.sl.cf.web/target/com.sap.cloud.lm.sl.cf.web-<version>.war`. Additionally, there is certain preprocessing of the [manifest.yml](https://github.com/SAP/cf-mta-deploy-service/blob/master/com.sap.cloud.lm.sl.cf.web/manifests/manifest.yml) and the build result is located at `com.sap.cloud.lm.sl.cf.web/target/manifests/manifest.yml`.
# Running
## Prerequisites
The CF MTA deploy service is deployed as a standard application in [Cloud Foundry](https://www.cloudfoundry.org/). So first you have to get access to a [Cloud Foundry](https://www.cloudfoundry.org/) instance, then login to the CF API and target a org and space, where the CF deploy service applicaiton is to be deployed.

In order to function the CF MTA deploy service requires a PostgreSQL service instance for persistence (TODO: add resource requirements for the PostgreSQL instance). So, this should first be created by running the following command:
```
$ cf cs <postgresql-service> <postgresql-service-plan> deploy-service-database
```
## Push the application 
Push the CF MTA deploy service application to Cloud Foundry by running the following command from the `com.sap.cloud.lm.sl.cf.web` directory:
```
$ cf push -f target/manifests/manifest.yml
```
After the push operation completes then the CF MTA deploy service should be up and running.
## Usage via CF MTA plugin
In order to use the CF MTA deploy service you should install the [CF MTA plugin](https://github.com/SAP/cf-mta-plugin), so follow the instructions in the [Download and installation](https://github.com/SAP/cf-mta-plugin#download-and-installation) section there. For the set of supported operations and examples refer to the [Usage](https://github.com/SAP/cf-mta-plugin#usage) section.

# How to obtain support
If you need any support, have any question or have found a bug, please report it in the [GitHub bug tracking system](https://github.com/SAP/cf-mta-deploy-service/issues). We shall get back to you.

# Further reading
Presentations, documents, and tutorials:
[Managing Distributed Cloud Native Applications Made Easy (CF Summit EU 2017 slides)](https://www.slideshare.net/NikolayValchev/managing-distributedcloudapps-80697059)
[Managing Distributed Cloud Native Applications Made Easy (CF Summit EU 2017 video)]
(https://www.youtube.com/watch?v=d07DZCuUXyk&feature=youtu.be)

# License
Copyright (c) 2017 SAP SE or an SAP affiliate company. All rights reserved.
This file is licensed under the Apache Software License, v. 2 except as noted otherwise in the [LICENSE](https://github.com/SAP/cf-mta-deploy-service/blob/master/LICENSE) file.
