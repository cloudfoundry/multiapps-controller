package com.sap.cloud.lm.sl.cf.persistence.util;

import java.util.ArrayList;
import java.util.List;

import com.sap.cloud.lm.sl.cf.persistence.model.ProgressMessage;

public class ProgressMessageUtil {

    private ProgressMessageUtil() {
        // only static members
    }

    public static List<String> getProgressMessagesAsString(List<ProgressMessage> progressMessages) {
        List<String> messagesText = new ArrayList<>(progressMessages.size());
        for (ProgressMessage progressMessage : progressMessages) {
            messagesText.add(progressMessage.getText());
        }
        return messagesText;
    }
}
