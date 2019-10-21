package eu.nimble.service.bp.impl.controller;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

/**
 * This class specifies the order of tests to be run. The tests are not STATELESS. For many cases, previously started
 * business processes DOES matter. For instance, in {@link StatisticsControllerTest} or {@link DocumentsControllerTest}.
 */
@RunWith(Suite.class)
@Suite.SuiteClasses({
        BusinessProcessWorkflowTests.class,
        StartControllerTest.class,
        ContinueControllerTest.class,
        BusinessProcessExecutionTest.class,
        StatisticsControllerTest.class,
        StartWithDocumentControllerTest.class,
        DocumentsControllerTest.class,
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