FROM cloudfoundry/cli-ci

LABEL name=xs2ds/oq-cf

ENV VERSION 0.1


RUN mkdir /workspace
#Copy test scripts
COPY test_scripts /workspace/test_scripts
COPY test_resources /workspace/test_resources
COPY test_scenarios /workspace/test_scenarios
COPY *.sh /workspace/
COPY mta_plugin_linux_amd64 /workspace/mta_plugin_linux_amd64
# Install Cloud Foundry cli
ADD https://cli.run.pivotal.io/stable?release=linux64-binary&version=6.26.0 /tmp/cf-cli.tgz
RUN mkdir -p /usr/local/bin && \
  tar -xzf /tmp/cf-cli.tgz -C /usr/local/bin && \
  cf --version && \
  rm -fv /tmp/cf-cli.tgz
# Install latest released cf cli MTA plugin

RUN cd /workspace/ && \
    ls -Faltr . && \
    find . -type f && \
    chmod +x mta_plugin_linux_amd64 && \
    cf install-plugin mta_plugin_linux_amd64 -f &&\
    rm -rf mta_plugin_linux_amd64

WORKDIR /workspace

#TODO find another way to assemble test resources
RUN mkdir /workspace/sanity-check
ADD https://github.com/nvvalchev/spring-music/blob/master/mta-assembly/spring-music.mtar?raw=true /workspace/test_resources/sanity-check/sanity-check.mtar
ADD https://github.com/nvvalchev/spring-music/blob/master/config.mtaext?raw=true /workspace/test_resources/sanity-check/


ENTRYPOINT ["/bin/bash"]

