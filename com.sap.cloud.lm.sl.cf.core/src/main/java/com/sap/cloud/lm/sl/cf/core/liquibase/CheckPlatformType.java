package com.sap.cloud.lm.sl.cf.core.liquibase;

import java.text.MessageFormat;

import com.sap.cloud.lm.sl.cf.core.cf.PlatformType;
import com.sap.cloud.lm.sl.cf.core.message.Messages;
import com.sap.cloud.lm.sl.cf.core.util.Configuration;

import liquibase.database.Database;
import liquibase.exception.CustomPreconditionErrorException;
import liquibase.exception.CustomPreconditionFailedException;
import liquibase.precondition.CustomPrecondition;

public class CheckPlatformType implements CustomPrecondition {

    @Override
    public void check(Database database) throws CustomPreconditionFailedException, CustomPreconditionErrorException {

        PlatformType platformType = Configuration.getInstance().getPlatformType();
        if (PlatformType.CF != platformType) {
            throw new CustomPreconditionFailedException(
                MessageFormat.format(Messages.LIQUIBASE_CF_CHECK_PLATFORM_TYPE, platformType.toString()));
        }
    }
}
