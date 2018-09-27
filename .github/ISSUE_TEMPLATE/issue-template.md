---
name: Issue Template
about: MultiApps Controller issue template for bugs and feature requests

---

# The issue
Describe your issue here.

# Your environment
*	MultiApps Controller version
*	MultiApps CF CLI Plugin version (if applicable)
*	which CF vendor is used
*	which backing service is used

# Steps to reproduce
Tell us how to reproduce this issue.
Create [GIST(s)](https://gist.github.com/) which is copy of your deployment and extension descriptor and link here (if applicable)

# Expected behaviour
Tell us what should happen

# Actual behaviour
Tell us what happens instead.

If you operate your own MultiApps Controller, provide all application logs. Create a [GIST(s)](https://gist.github.com/) which contains the logs. Please refrain of copying full logs here because it will make the issue hard to read.

If an multi-target app operation fails, download logs of the operation and provide them as a [GIST(s)](https://gist.github.com/).  For more details, see *download-mta-op-logs / dmol* command provided by [CF MTA Plugin](https://github.com/cloudfoundry-incubator/multiapps-cli-plugin). The most important log file is *MAIN_LOG*.
