# Contributing to the MultiApps Controller

## Did you find a bug?
* Check if the bug has already been reported and has an open [Issue](https://github.com/cloudfoundry-incubator/multiapps-controller/issues).

* If there is none, create one by using the provided [Issue Template](https://github.com/cloudfoundry-incubator/multiapps-controller/issues/new/choose) for bugs.

* Try to be as detailed as possible when describing the bug. Every bit of information helps!

## Do you have a question or need support?
If you need any support or have any questions regarding the project, you can drop us a message on [Slack](https://cloudfoundry.slack.com/?redir=%2Fmessages%2Fmultiapps-dev) or open an [Issue](https://github.com/cloudfoundry-incubator/multiapps-controller/issues) and we shall get back to you.

## Do you want to contribute to the code base?

### Starter GitHub Issues
If you are looking for what you can contribute to the project, check the GitHub Issues labeled as [Good First Issue](https://github.com/cloudfoundry-incubator/multiapps-controller/issues?q=is%3Aissue+is%3Aopen+label%3A%22good+first+issue%22) to find items that are marked as more beginner friendly.

### Fork the project
* To develop your contribution to the project, first [fork](https://help.github.com/articles/fork-a-repo/) this repository in your own github account. 

* When developing make sure to keep your fork up to date with the origin's master branch or the release branch you want to contribute a fix to.

### How to build and run?
You can read how to build, configure and run the MultiApps Controller [here](https://github.com/cloudfoundry-incubator/multiapps-controller#building).

### Testing
* To ensure no regressions to previous functionality execute `mvn clean test` in the project's root folder to run all the unit tests.

* If you are developing new functionality make sure to add tests covering the new scenarios where applicable!

* The [spring-music](https://github.com/nvvalchev/spring-music) contains a handy sample MTA archive to test your deployed CF MultiApps Controller against a Cloud Foundry instance.

### Formatting
Having the same style of formatting across the project helps a lot with readability.

#### Eclipse
Our team is developing on the [Eclipse](http://www.eclipse.org/) IDE and we have a handy formatter located [here](https://github.com/cloudfoundry-incubator/multiapps/tree/master/ide). In Eclipse you can import the formatter from `Window > Preferences > Java > Code Style > Formatter`

#### IntelliJ
If you're using IntelliJ you can try the [EclipseCodeFormatter](https://github.com/krasa/EclipseCodeFormatter) plugin.

#### NetBeans
NetBeans also provides such a plugin. Just search for `eclipse formatter` in the [PluginPortal](http://plugins.netbeans.org/PluginPortal/).

## Creating a pull request
When creating a pull request please use the provided template. Don't forget to link the [Issue](https://github.com/cloudfoundry-incubator/multiapps-controller/issues) if there is one related to your pull request!
