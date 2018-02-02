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
RUN cd ~ && \
        export cf_cli_mta_plugin_url="$(curl -s https://github.com/SAP/cf-mta-plugin -k | grep -E "cf-cli-mta-plugin-.*?-linux.bin" | grep -E "<a href=\"(.*?)\">" | cut -d '"' -f2)"  && \
        echo "downloading ${cf_cli_mta_plugin_url}" && \
        wget -q ${cf_cli_mta_plugin_url} -O $PWD/cf-cli-mta-plugin && \
        chmod +x cf-cli-mta-plugin && \
        cf install-plugin cf-cli-mta-plugin -f && \
        rm -vf cf-cli-mta-plugin

RUN pwd

#Copy test scripts
COPY test_scripts test_scripts
COPY test_resources test_resources
COPY test_scenarios test_scenarios
COPY *.sh ./

#USER root

ENTRYPOINT ["/bin/bash"]
