package com.sap.cloud.lm.sl.cf.process.util;

/*TODO delete when parallel app push is implemented 
 * This feature has been implemented for the specific case in which custom Devx builders output illegal xml characters, which cause parsing errors when serialized for use in the sl protocol.
 * When the parallel app push is implemented, the buildpack outputs won't be delivered in the slp task list content thus this filtering won't be necessary
 */
public class XMLValueFilter {

    private String input;
    private static final String illegalXmlCharactersRegex = "[\u0000-\u0008\u000B-\u001F\uFFFE\uFFFF]";
    private static final String replacementXmlChar = "*";

    public XMLValueFilter(String input) {
        this.input = input;
    }

    public String getFiltered() {
        return input.replaceAll(illegalXmlCharactersRegex, replacementXmlChar);
    }
}
