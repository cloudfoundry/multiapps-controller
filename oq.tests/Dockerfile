FROM cloudfoundry/cli-ci

LABEL name=xs2ds/oq-cf

ENV VERSION 0.1

# Install Cloud Foundry cli
ADD https://cli.run.pivotal.io/stable?release=linux64-binary&version=6.26.0 /tmp/cf-cli.tgz
RUN mkdir -p /usr/local/bin && \
  tar -xzf /tmp/cf-cli.tgz -C /usr/local/bin && \
  cf --version && \
  rm -fv /tmp/cf-cli.tgz

# Install latest released cf cli MTA plugin
RUN cd plugin-binaries && \
          chmod +x mta_plugin_linux_amd64 && \
	      cf install-plugin mta_plugin_linux_amd64 -f

RUN mkdir /workspace
WORKDIR /workspace

#TODO find another way to assemble test resources
RUN mkdir /workspace/sanity-check
ADD https://github.com/nvvalchev/spring-music/blob/master/mta-assembly/spring-music.mtar?raw=true /workspace/sanity-check/
ADD https://github.com/nvvalchev/spring-music/blob/master/config.mtaext?raw=true /workspace/sanity-check/

#Copy test scripts
COPY test_scripts /workspace/test_scripts
COPY test_resources /workspace/test_resources
COPY test_scenarios /workspace/test_scenarios
COPY *.sh /workspace/

#USER root

ENTRYPOINT ["/bin/bash"]
