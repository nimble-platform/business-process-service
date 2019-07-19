package eu.nimble.service.bp.impl.controller;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses({
        Test01_StartControllerTest.class,
        Test02_ContinueControllerTest.class,
        Test03_StatisticsControllerTest.class,
        Test04_BusinessProcessesTest.class,
        TransactionSummaryTest.class,
        Test05_ProcessInstanceGroupControllerTest.class,
        Test06_ContentControllerTest.class,
        Test07_PreferenceControllerTest.class,
        Test08_DocumentControllerTest.class,
        Test09_ContractControllerTest.class,
        FrameContractControllerTest.class,
        Test10_ApplicationControllerTest.class,
        Test11_CollaborationGroupTest.class,
        Test12_TrustControllerTest.class,
        Test13_BinaryContentTest.class,
        Test14_HjidCheckTest.class,
        Test15_CollaborationGroupTestSuite.class,
        Test16_CollaborationGroupTestSuite.class,
        Test17_DocumentTypesTest.class
})
public class TestSuite {
}