package com.sap.cloud.lm.sl.cf.core.k8s;

import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.representer.Represent;

import com.sap.cloud.lm.sl.mta.util.YamlRepresenter;

import io.fabric8.kubernetes.api.model.IntOrString;

public class KubernetesModelRepresenter extends YamlRepresenter {

    public KubernetesModelRepresenter() {
        this.representers.put(IntOrString.class, new RepresentIntOrString());
    }

    public class RepresentIntOrString implements Represent {

        @Override
        public Node representData(Object data) {
            IntOrString intOrString = (IntOrString) data;
            Object value = getValue(intOrString);
            return KubernetesModelRepresenter.this.representData(value);
        }

        private Object getValue(IntOrString intOrString) {
            return intOrString.getIntVal() == null ? intOrString.getStrVal() : intOrString.getIntVal();
        }

    }

}
