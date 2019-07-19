package eu.nimble.service.bp.impl.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.nimble.service.bp.swagger.model.ProcessInstanceGroup;
import eu.nimble.utility.JsonSerializationUtility;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@RunWith(SpringJUnit4ClassRunner.class)
public class Test32_CollaborationGroupTest5 {

    @Autowired
    private MockMvc mockMvc;

    private static String processInstanceId;

    /*
        Firstly, we delete the process instance group and then, delete the associated process instance group.
        In this case, we test that whether all associations between process instance groups are deleted properly or not
     */
    @Test
    public void test1_deleteProcessInstanceGroup() throws Exception {
        // get the process instance group
        MockHttpServletRequestBuilder request = get("/process-instance-groups/" + Test04_BusinessProcessesTest.transportProviderProcessInstanceGroupID)
                .header("Authorization", TestConfig.initiatorPersonId);
        MvcResult mvcResult = this.mockMvc.perform(request).andDo(print()).andExpect(status().isOk()).andReturn();

        ObjectMapper objectMapper = JsonSerializationUtility.getObjectMapper();
        ProcessInstanceGroup processInstanceGroup = objectMapper.readValue(mvcResult.getResponse().getContentAsString(), ProcessInstanceGroup.class);

        // get process instance id
        processInstanceId = processInstanceGroup.getProcessInstanceIDs().get(0);
        // get id of associated process instance group
        String associatedProcessInstanceGroupId = processInstanceGroup.getAssociatedGroups().get(0);
        // delete the group
        request = delete("/process-instance-groups/" + Test04_BusinessProcessesTest.transportProviderProcessInstanceGroupID)
                .header("Authorization", TestConfig.initiatorPersonId);
        mvcResult = this.mockMvc.perform(request).andDo(print()).andExpect(status().isOk()).andReturn();
        // delete the associated group
        request = delete("/process-instance-groups/" + associatedProcessInstanceGroupId)
                .header("Authorization", TestConfig.initiatorPersonId);
        mvcResult = this.mockMvc.perform(request).andDo(print()).andExpect(status().isOk()).andReturn();
    }

    /*
        Try to cancel a completed process instance
     */
    @Test
    public void test2_cancelProcessInstance() throws Exception {
        // cancel the process instance
        MockHttpServletRequestBuilder request = post("/processInstance/"+processInstanceId+"/cancel")
                .header("Authorization", TestConfig.initiatorPersonId);
        MvcResult mvcResult = this.mockMvc.perform(request).andDo(print()).andExpect(status().isOk()).andReturn();
    }

    /*
        Try to cancel a process instance which is already cancelled
     */
    @Test
    public void test3_cancelProcessInstance() throws Exception {
        // cancel the process instance
        MockHttpServletRequestBuilder request = post("/processInstance/"+Test23_CollaborationGroupTest.cancelledProcessInstanceId+"/cancel")
                .header("Authorization", TestConfig.initiatorPersonId);
        MvcResult mvcResult = this.mockMvc.perform(request).andDo(print()).andExpect(status().isBadRequest()).andReturn();
    }
}
