package eu.nimble.service.bp.impl.controller;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses({
        BusinessProcessWorkflowTests.class,
        StartControllerTest.class,
        ContinueControllerTest.class,
        StatisticsControllerTest.class,
        BusinessProcessExecutionTest.class,
        TransactionSummaryTest.class,
        ProcessInstanceGroupControllerTest.class,
        BusinessProcessDefinitionTest.class,
        PreferenceControllerTest.class,
        DocumentControllerTest.class,
        ContractControllerTest.class,
        FrameContractControllerTest.class,
        ApplicationControllerTest.class,
        CollaborationGroupTest.class,
        TrustControllerTest.class,
        ProcessBinaryContentTest.class,
        HjidCheckTest.class,
        CollaborationGroupTest2_GroupDeletion.class,
        CollaborationGroupTest3_GroupDeletionAndMerge.class,
        DocumentTypesTest.class,
        EPCControllerTest.class
})
public class TestSuite {
}