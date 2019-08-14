<p align="center"><img width="335" height="281" src="logo.png" alt="MultiApps logo"></p>

# MultiApps Controller [![Build Status](https://travis-ci.org/cloudfoundry-incubator/multiapps-controller.svg?branch=master)](https://travis-ci.org/cloudfoundry-incubator/multiapps-controller)

The MultiApps Controller (formerly known as *deploy service*) for [Cloud Foundry](https://www.cloudfoundry.org/) is based on the [Multi-Target Application (MTA)](https://www.sap.com/documents/2016/06/e2f618e4-757c-0010-82c7-eda71af511fa.html) model in which CF applications are modeled as modules, while CF services as resources. The MTA model enables the delivery of packaged applications, where any target specific configuration could be specified on deployment time without changing application code.
CF MultiApps Controller provides the possibility to operate (deploy, update, undeploy) [MTA](https://www.sap.com/documents/2016/06/e2f618e4-757c-0010-82c7-eda71af511fa.html) modeled applications via a single command, while ensuring the consistency and completeness of the different application components.

# Getting started with MultiApps

You can find more information about what the MultiApps Controller offers in the way of functionality as well as information about creating MTA archives on the [MultiApps Controller Wiki](https://github.com/cloudfoundry-incubator/multiapps-controller/wiki). If you are planning to deploy on the SAP cloud you can find even more information in the official SAP Help Documentation under [Multitarget Applications](https://help.sap.com/viewer/65de2977205c403bbc107264b8eccf4b/Cloud/en-US/c4f0d850b6ba46089a76d53ab805c9e6.html).

The project has also provided [schema support](https://github.com/cloudfoundry-incubator/multiapps-controller/wiki/Deployment-Descriptor#editor-schema-support) in the open [Schema Store](http://schemastore.org/json/ which would provide auto-completion, tooltips, synthax checking and more when writing MultiApps descriptors.

# Components
## com.sap.cloud.lm.sl.cf.api
Contains the swagger-generated definitions of the REST API models and endpoints. The complete swagger definitions can be found at: https://app.swaggerhub.com/apis/SAP53/mtarest/1.0.0

## com.sap.cloud.lm.sl.cf.core
Contains the domain model, persistence and other core services and utilities.

## com.sap.cloud.lm.sl.cf.client
Extends the [Java Client Library for Cloud Foundry](https://github.com/SAP/cf-java-client-sap) with additional domain model objects and attributes, OAuth token providers and retrying functionality.  

## com.sap.cloud.lm.sl.persistence
Contains utilities for upload and processing of file artifacts. These are used for initial upload of the [MTA](https://www.sap.com/documents/2016/06/e2f618e4-757c-0010-82c7-eda71af511fa.html) archive and descriptors and their processing as part of the deployment.

## com.sap.cloud.lm.sl.cf.process
Contains the concrete workflow definitions for MTA operations like deploy, undeploy, blue-green deploy, etc. These are modeled via [Activiti](https://activiti.org) BPMN process definitions. The process definitions have steps, where each step logic uses the `com.sap.cloud.lm.sl.cf.client` to call the Cloud Controller API from [Cloud Foundry](https://www.cloudfoundry.org/).

## com.sap.cloud.lm.sl.cf.web
Contains REST API implementations for:
- performing MTA applications - executing deploy, undeploy, blue-green deploy
- listing deployed MTA applications
- listing MTA operations - both on-going and historic ones
- reading and writing cross MTA configurations

The result from the build of this component is a WAR file which is the deployable assembly of the MultiApps Controller.

# Building
## Build tools
All components are built with Java 8 and [Apache Maven](http://maven.apache.org/), version `3.3.9` or newer. 

Make sure that your Maven is configured to use Java 8 by configuring the `JAVA_HOME` env to point to the correct Java JDK.
## Compiling and Packaging
To build all components, run the following command from the root directory of the project:
```
$ mvn clean install
```
The deployable result from building components is a WAR file, located at `com.sap.cloud.lm.sl.cf.web/target/com.sap.cloud.lm.sl.cf.web-<version>.war`. 

Additionally, the project uses [Immutables](https://immutables.github.io/) to generate value objects. As a result, it won't compile in IDEs like Eclipse or IntelliJ unless you also have an enabled annotation processor. See [this guide](https://immutables.github.io/apt.html) for instructions on how to configure your IDE.

There is also a certain preprocessing of the [manifest.yml](com.sap.cloud.lm.sl.cf.web/manifests/manifest.yml) that creates a version of the manifest with information about the newly built MultiApps-Controller such as version and path to the built WAR file. 

This manifest is located at `com.sap.cloud.lm.sl.cf.web/target/manifests/manifest.yml`.

# Deploying
The CF Multiapps Controller is deployed as a standard application in [Cloud Foundry](https://www.cloudfoundry.org/). So first you have to get access to a [Cloud Foundry](https://www.cloudfoundry.org/) instance, then login to the CF API and target a org and space, where the CF deploy service application is to be deployed. 

If you do not have a Cloud Foundry instance you can sign up for SAP Cloud Platform trial [here](https://account.hanatrial.ondemand.com/#/home/welcome).

## Configuration
The [manifest.xml](com.sap.cloud.lm.sl.cf.web/manifests/manifest.yml) located at `com.sap.cloud.lm.sl.cf.web/target/manifests/manifest.yml` is used to configure the deployment of the Multiapps Controller to the Cloud Foundry as well as some of it's functionality.

Property | Sample Value | Description
--- | --- | ---
[host](com.sap.cloud.lm.sl.cf.web/manifests/manifest.yml#L4) | deploy-service | The entry can be changed to define a different host name for the application.
[memory](com.sap.cloud.lm.sl.cf.web/manifests/manifest.yml#L5) | 1024M | This setting can be further tuned depending on the expected load. If you set the memory to too low the application might fail to start due to JVM heap memory shortage.
[instances](com.sap.cloud.lm.sl.cf.web/manifests/manifest.yml#L6) | 1 | The CF MultiApps Controller can also scale by instances. The default number of instances is set to 1 to avoid consuming too much resources.

## Required services
In order to function the CF Multiapps Controller requires a PostgreSQL service instance for persistence. So, this should first be created by running the create-service command:
```
$ cf cs <postgresql-service> <postgresql-service-plan> deploy-service-database
```

For example if you are deploying the CF Multiapps Controller on the SAP Cloud Platform the parameters should look like this:
```
$ cf cs postgresql v9.6-dev deploy-service-database
```

Other platforms might have different names and plans for the PostgreSQL service.

## Push the application 
Push the CF Multiapps Controller application to Cloud Foundry by running the following command from the `com.sap.cloud.lm.sl.cf.web` directory:
```
$ cf push -f target/manifests/manifest.yml
```
After the push operation completes then the CF Multiapps Controller should be up and running.
## Usage via CF MTA plugin
In order to use the CF MultiApps Controller you should install the [CF MTA plugin](https://github.com/cloudfoundry-incubator/multiapps-cli-plugin), so follow the instructions in the [Download and installation](https://github.com/cloudfoundry-incubator/multiapps-cli-plugin#download-and-installation) section there. For the set of supported operations and examples refer to the [Usage](https://github.com/cloudfoundry-incubator/multiapps-cli-plugin#usage) section.

If using a different [host](com.sap.cloud.lm.sl.cf.web/manifests/manifest.yml#L4) than the default for your CF Multiapps Controller either set the following env `DEPLOY_SERVICE_URL=<deploy-service-app-url>` or run CF MTA plugin commands with `-u <deploy-service-app-url>` so that they are executed against your instance(s) of the Multiapps Controller.

You could use a modified [spring-music](https://github.com/nvvalchev/spring-music) application, which is extended and adapted to the MTA model to test your newly deployed Multiapps Controller.

# How to contribute
* [Did you find a bug?](CONTRIBUTING.md#did-you-find-a-bug)
* [Do you have a question or need support?](CONTRIBUTING.md#do-you-have-a-question-or-need-support)
* [How to develop, test and contribute to MultiApps Controller](CONTRIBUTING.md#do-you-want-to-contribute-to-the-code-base)

# Further reading
Presentations, documents, and tutorials:
- [Managing Distributed Cloud Native Applications Made Easy (CF Summit EU 2017 slides)](https://www.slideshare.net/NikolayValchev/managing-distributedcloudapps-80697059)
- [Managing Distributed Cloud Native Applications Made Easy (CF Summit EU 2017 video)](https://www.youtube.com/watch?v=d07DZCuUXyk&feature=youtu.be)

# License
Copyright (c) 2017 SAP SE or an SAP affiliate company. All rights reserved.
This file is licensed under the Apache Software License, v. 2 except as noted otherwise in the [LICENSE](LICENSE) file.
