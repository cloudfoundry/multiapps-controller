package com.sap.cloud.lm.sl.cf.process.steps;

import static org.mockito.Mockito.when;

import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.mockito.Mock;

import com.google.gson.reflect.TypeToken;
import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.common.util.JsonUtil;
import com.sap.cloud.lm.sl.common.util.TestUtil;
import com.sap.cloud.lm.sl.cts.FileInfo;
import com.sap.cloud.lm.sl.cts.FileInfoFactory;

@RunWith(Parameterized.class)
public class PrepareDeployParametersStepTest extends AbstractStepTest<PrepareDeployParametersStep> {

    @Parameters
    public static Iterable<Object[]> getParameters() {
        return Arrays.asList(new Object[][] {
// @formatter:off
            // (0) The file list parameter contains information for 0 file(s):
            {
                "file-info-list-parameter-00.json", "file-info-list-00.json", "E:Required parameter \"fileList\" is missing",
            },
            // (1) The file list parameter contains information for 1 file(s):
            {
                "file-info-list-parameter-01.json", "file-info-list-01.json", "a108e830-a207-4144-aa54-f976d17d6289",
            },
            // (2) The file list parameter contains information for 2 file(s):
            {
                "file-info-list-parameter-02.json", "file-info-list-02.json", "a108e830-a207-4144-aa54-f976d17d6289",
            },
            // (3) The file list parameter is  null:
            {
                "file-info-list-parameter-03.json", "file-info-list-03.json", "E:Required parameter \"fileList\" is missing",
            },
// @formatter:on
        });
    }

    @Mock
    private FileInfoFactory fileInfoFactory;

    private String fileInfoListParameterFilePath;
    private String fileInfoFactoryResultFilePath;
    private String expected;

    public PrepareDeployParametersStepTest(String fileInfoListParameterFilePath, String fileInfoFactoryResultFilePath, String expected) {
        this.fileInfoListParameterFilePath = fileInfoListParameterFilePath;
        this.fileInfoFactoryResultFilePath = fileInfoFactoryResultFilePath;
        this.expected = expected;
    }

    @Test
    public void testExecute() throws Exception {
        String fileInfoListParameterString = TestUtil.getResourceAsString(fileInfoListParameterFilePath, getClass());
        String fileInfoFactoryResultString = TestUtil.getResourceAsString(fileInfoFactoryResultFilePath, getClass());
        Type fileInfoListParameterType = new TypeToken<List<Map<String, Map<String, String>>>>() {
        }.getType();
        Type fileInfoFactoryResultType = new TypeToken<List<FileInfo>>() {
        }.getType();
        List<Map<String, Map<String, String>>> parameter = JsonUtil.fromJson(fileInfoListParameterString, fileInfoListParameterType);
        List<FileInfo> factoryResult = JsonUtil.fromJson(fileInfoFactoryResultString, fileInfoFactoryResultType);

        when(fileInfoFactory.fromSlpTable(parameter)).thenReturn(factoryResult);
        step.fileInfoFactorySupplier = () -> fileInfoFactory;

        context.setVariable(Constants.PARAM_FILE_LIST, parameter);

        TestUtil.test(() -> {

            step.execute(context);

            assertStepFinishedSuccessfully();

            return StepsUtil.getCtsFileInfoList(context).get(0).getFileId();

        } , expected, getClass(), false);
    }

    @Override
    protected PrepareDeployParametersStep createStep() {
        return new PrepareDeployParametersStep();
    }

}
