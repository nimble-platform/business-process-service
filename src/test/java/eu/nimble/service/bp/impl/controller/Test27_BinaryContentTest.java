package eu.nimble.service.bp.impl.controller;

import eu.nimble.service.bp.swagger.model.ProcessInstance;
import eu.nimble.service.bp.swagger.model.ProcessInstanceInputMessage;
import eu.nimble.service.model.ubl.commonbasiccomponents.BinaryObjectType;
import eu.nimble.service.model.ubl.iteminformationrequest.ItemInformationRequestType;
import eu.nimble.utility.JsonSerializationUtility;
import eu.nimble.utility.persistence.binary.BinaryContentService;
import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@ActiveProfiles("local_dev")
@RunWith(SpringJUnit4ClassRunner.class)
public class Test27_BinaryContentTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private Environment environment;
    @Autowired
    private BinaryContentService binaryContentService;

    private final String itemInformationRequest = "/controller/itemInformationRequestBinaryContentJSON.txt";

    private static String processInstanceId;

    @Test
    public void test1_startProcessInstance() throws Exception {
        String inputMessageAsString = IOUtils.toString(ProcessInstanceInputMessage.class.getResourceAsStream(itemInformationRequest));

        MockHttpServletRequestBuilder request = post("/start")
                .header("Authorization",environment.getProperty("nimble.test-initiator-person-id"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(inputMessageAsString);
        MvcResult mvcResult = this.mockMvc.perform(request).andDo(print()).andExpect(status().isOk()).andReturn();

        ProcessInstance processInstance = JsonSerializationUtility.getObjectMapper().readValue(mvcResult.getResponse().getContentAsString(), ProcessInstance.class);
        Assert.assertEquals(processInstance.getStatus(), ProcessInstance.StatusEnum.STARTED);

        processInstanceId = processInstance.getProcessInstanceID();
    }

    @Test
    public void test2_retrieveBinaryContents() throws Exception {
        // get document content
        MockHttpServletRequestBuilder request = get("/document/json/983a7b0b-ea82-40ce-9e4e-76195f799487")
                .header("Authorization",environment.getProperty("nimble.test-initiator-person-id"));
        MvcResult mvcResult = this.mockMvc.perform(request).andDo(print()).andExpect(status().isOk()).andReturn();

        ItemInformationRequestType itemInformationRequest = JsonSerializationUtility.getObjectMapper().readValue(mvcResult.getResponse().getContentAsString(), ItemInformationRequestType.class);

        Assert.assertEquals(1, itemInformationRequest.getAdditionalDocumentReference().size());
        BinaryObjectType binaryObject = binaryContentService.retrieveContent(itemInformationRequest.getAdditionalDocumentReference().get(0).getAttachment().getEmbeddedDocumentBinaryObject().getUri());
        Assert.assertNotNull(binaryObject);
        Assert.assertEquals(1, itemInformationRequest.getItemInformationRequestLine().get(0).getSalesItem().get(0).getItem().getProductImage().size());
        binaryObject = binaryContentService.retrieveContent(itemInformationRequest.getItemInformationRequestLine().get(0).getSalesItem().get(0).getItem().getProductImage().get(0).getUri());
        Assert.assertNotNull(binaryObject);
        Assert.assertEquals(1, itemInformationRequest.getItemInformationRequestLine().get(0).getSalesItem().get(0).getItem().getItemSpecificationDocumentReference().size());
        binaryObject = binaryContentService.retrieveContent(itemInformationRequest.getItemInformationRequestLine().get(0).getSalesItem().get(0).getItem().getItemSpecificationDocumentReference().get(0).getAttachment().getEmbeddedDocumentBinaryObject().getUri());
        Assert.assertNotNull(binaryObject);
    }

    @Test
    public void test3_updateProcessInstance() throws Exception {
        // get document content
        MockHttpServletRequestBuilder request = get("/document/json/983a7b0b-ea82-40ce-9e4e-76195f799487")
                .header("Authorization",environment.getProperty("nimble.test-initiator-person-id"));
        MvcResult mvcResult = this.mockMvc.perform(request).andDo(print()).andExpect(status().isOk()).andReturn();

        ItemInformationRequestType itemInformationRequest = JsonSerializationUtility.getObjectMapper().readValue(mvcResult.getResponse().getContentAsString(), ItemInformationRequestType.class);
        // update item information request
        itemInformationRequest.getAdditionalDocumentReference().clear();
        // create a new binary object
        String binaryObjectTypeString = "{\"value\":\"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iigAooooAKKKKAEIDDBGRWbd6RFPlo/lf9K06KAOQuLCe2PzqcetVu+K7Z0V12uoIPY1k3mio+XhO1vSgDnj0pO9WLi0lgYq6EY71CetADaTtmlooASlzzR2o7UAHek+tL3pM0AFLSUUALRSZ5NLigBKKX6UmaACl70lFACd6QLyfrSkd6dEMhgaAADjmjZ+NSBcClXGM0AR7KUJ61KaQntQAJFlgOxrf0cFFdD9awd5wOelbOjSF5HH+zQBtUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRiiigCKSFJl2yIGFZF5omcvAc/7JrcxRQBxUsDwttkQqairs5raK4XEiA+/esW80R1y0J3L6d6AMbrRT3jeNsMpBplACUvaikoAKWkpaACikFLQAUlL3ooAbS0YoNAAaEOHB9qShetAE/ajPpTaDQA7PagnvSdqD1oAK2dBH7xz7VikjFbWg8tIfagDdooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAMUYoooArXFlDcj5159RWHeaNJDlo/mWulxRigDh2VkbBGDTa62606C5BO3a3qKw7vSp7clgNy+ooAzsCilII68UUANpaOlHvQAUd6O9FACUGlpKAEoXGaU0DqKAJccCkozSUAGcUvek+tBNAC8Yrc0HpJWEOlb2gj5JKANqiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKADFIQCMEZHpS0UAZt3pMNwCyDa/wClYV1YTWpwykj1rr8U10V1KuAQexoA4ik6V0N5oquS8Jw3901iTW8kDFZEINAEJ6UUtJ2oAO1Heg0lACUL1opV+9QA+g0lFACnpSUpppoAUV0Og/6iT61zo9a6TQf+PRyf71AGtRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAGKimt4512yKGqWjFAHPXmiOuXg+Yf3e9ZDoyMVYEEV3GK53W4gtwGC4yM0AY9ITSkUE80AIaRetKaReWoAeaPal+lJQAUh5paSgAHpXTaGMWTf71cyBXT6IP9C/GgDTooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAqC4to7lNsg+hqejFAHLajp7WamQt+667vSshL22nk2Rzoz+ma7HV41m0m5jcgKUPevJ31KC608QRuqvGx2MvXPrUt2KjFPc6thQnDVxMVxrKyjF/mMD+LrVo+IbqzVvtMsLBfQcmjmtuNQvsdf3xmjiuAf4gunAtVb/aB4qBvH94/3LeNRU+0iV7GR6NnikJ715o3jXU5DjKKPZaZ/wkupS/euCufQUnViP2L7npu7HXiuh0W8gMXkb1EnXbmvEv7XvWbm5kPP96poNUuoZRIk7hx33UvbLsP2L7n0BmlrzTRfiBJHtivl3r03967vT9Ws9SiD206tn+EnkVcZqWxlKDjuaFFJS1ZIUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFMdgiliQFAySafXnPxI8WHT7Q6XaOBPKP3jDqo9PrUykoq7HGLk7Ix/Hni2XVHfT9MuNtspw7qfvGvMoJrrTbt1COyE+mau2Ln7MD7mrYc8elcU6jvdndGmkrIhXXJuV8l+arXMtzeyoscLL6k1qI2RnaPyqdWOCOPwFJ1WUqZjT2XkWLvIoBOCBWcmcDnvWrq86lFiB+ZjzWcigAEdKUZNrUbjYemR261ZQ7etQKCpx71etbSSdv502xWFQs3A61cgt5XHCnmpf9D09N8zDcOcd6qSa47nFnHx/eYdqFd7CdluaUdm4bLt+dXrSS4s5Q8NwUI9Grl3fU7s5eTaP9kbaT+zrknJnkB/3qrla6ivFnruj+Nnj2xXwDL/AHx1rsrPUba/iD28qtntnmvm1oNbtfntpg/+yw61d0/xnqOjTL9uUwENjcnTNaxqSjvqjGdKMttD6R/GivPtA+I1vdwr9p+Zf7461V1f4nTWVwUhsh5ecLIec1r7SNr3MvZyvax6ZRXi8/xN1xm3wCEoOq7eaI/ihrMgKgxBx1BSl7WI/ZSPZ6WvJbH4rXkUgW+tkde7IMV6Nout2mu2S3No+V6Mp6g1cZqWxEoOO5qUUUVRIUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQBnaxqUek6XNdyEfIvyj1PYV87azeyalqEt1MSzO2eT+lelfE/WCXj02NuANz49a8xkGRzzXJVneVux10YWjfuNsgBFjPerOOar23+rZc8+gqxxWEjaJMjcdeKjubwRRnDZPb61G77VJHAqkwaVs/wg8ZqbF3sRkPLJvc5J9aeVwcAU8LgY96u2Nk0zbyvy+p71QrhZWDSEPJ9zrii/1iOzDQWqhnHG70qLVtU25tLU9sO3+FVLKw3bXI+Y1SS3ZDetkFtbS3jb55GfJzzWzb2caDgA4FPhgCYUdfTFX4oR1PFS5tj5bbjIbdSRmrawRrxtJpyBVIAHHWpWPy8CgCJokYYCHAqrcWNvOB5sKOB0DDOKvKwVhk0jkFvrRewrHHXGk3ejzNdaezPCvLRd/pWvY38GrWux1Hoyt1BrVdO46VzmpWTWN1/aNou0Fv3ydiPWqU77hy2C5sXtnLRuTz0PRqqysj7fMRo3HpW156XVmJUG44yDVWO3a5I2ICretXGxL0KCLIB8jLIB2Neo/C1pVM6ONoZcla4yHT4YguQN4Nek+AbSRUmuXQqrcKcYzVwd5KxnUVou53NFFFdRyhRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFRyuIomdjwoJNP7VkeJLj7LoN3JnHyEfnSbsrjSu7HifiK+bUNaupi2cuQKySM9qe7b3ck9SaaByOa4G7u56CVlYZGNrMOKl6HJqML83Gacc9zUsaIXG446Cjbin7fekwCfeiwx0EPnOFqzq16un2iwREea3b0HrU1mq29vJNIAFVc5zXONK2o37zP3Py59BTirsmTsiWytS8nmMd24jr3roIoBGoAHzVNpFgogadwML90GpVG47j+VEwih0USgbieamB/PvUQOPXFSg8ZFZjHocYqbtxUCn8DUgYkDnB9KpMAbk470hyOMUZyc4oJpXCwHA4qKVEkR1IyG4PvUh6cc0nekMwLBfsV7LYMcqRuT6HtWtpNlNKz20KFmLHaB1xVTVoygiuVHKNhj7HivX/Ben2sOjRXKIDK4+ZyOa2px53YxqS5VczdA8DrEUuNR+ZuojH9a7aOGOBAkaKqjoAKkortjFRVkckpuTuxaKKKokKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigArl/H0vl+Fbgf3iBXUVyHxFyfDDgd3FRP4WVD4keK+3pS9aOaOpzXCd9wUfN0pp6+tO4xQRnp1pDE2/L/jTok3ygAcUh4+uKt2CBpBj1obsgKfiKfybSG2jA3Sfeqppdqdq5A3E1X1KY3mtuuSVRtgHpW5ZKIz04XtVfDEjdms7NHCkAOEAGRTB27VGrFzuPenDBPJ5rNu5aJO+Kd/KmAn8aVSRzSGSJ05pwPy896ZwTnNJuxQBLu5pCR+dNZiB04pQ3HTrQA6kzmkHt0pR60XAgvEEtrIp5yufyr1L4fy+b4StmJyeRzXmbHMT4/u4r0n4ert8LRr6O1dGH+Iwr/CdbRRRXacYUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAlcp8Qk3+FJj/dYV1dY/iPSm1nRZrJHCO+CCfapkrxaQ4u0kz59zjjmnc1pa1od7od15F2nH8L9mrNByf6VwtNOzO9NNXQY6U7ORmm559qXtnPNIYe3FXrciKCSXG3apNUTmrVw2zR7h167cVLGjm7H97dySHqzbgfWuigyFJB69KwtIQYz7VvJjC89Kqb1sTFdS6oAABpwIxxTVPHP504EH0rM0Hg/j6UuaYv3qcOT9KAH7uOn405Rx1phOBx60Bjnp+NAiQjI6U0dad+NNBy2e9ADsg9KXsOKaCAaXPNADo43mYIilmbgAeteseFdNl0vQ47eb75JYj0zXEeDLQXOtqzLlUGa9Trsw8NOY5K89eUWiiiuk5wooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAoxRRQBzHjPRF1fQpdqBp4lLxnHPHavDA4WRkcFXU4IIr6ZIBGCK8R+InhttJ1Y30CH7LOd3H8LelY1KalqbUptaHKqOM+tLWaJZIplweGPQ1d80bsMcGuVxaOqLTRNU1/ldCuMD0quvTg/jU1/t/seZN3JFS90UY2k425JwcVvRjlT1rC00beCMYFbiHBB7UTXvCg9C4ucdKULimq2T16U5WqCh/Qe9LkcetNyKB1GKBkh5GRzQpIHNIPalGDSuIXdxSg85pMY5pB1xRcB3U0oz2FN6c0oBLKo6k0wPQPh9aEJcXRHX5VruaxvDNl9h0SBCMMw3MK2e9elTjyxSPPqS5pNi0UUVZAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFZ2saTb6zp0tncqGRxwfQ+taNFAHzh4n8M3ehXr286sYiT5coHBrlZtQubfKOu8D1r6p1PSrPVrVre8hWRCO45H0ryfxV8J7gb59KIlQ87D1FYzh2N4VOjPJU8TIjEGKRT6ZrZtr06laq4Py5wVFc/rXhzUNOmf7RaSxsp5JQ4pmhXxtbpreY4jk7+hqHTVrotVHezOogQK2e49PStJG3degqgrAf/AFqtRuOg6VjUXU2gy+jcdOtSKRn61XicYIqcdOelYlj1IzS9PpTQeRTs8UDHdqOTjmk4P1o5JoAkBOOtGegzTRzz0pecZ4oEOzxjFbPhvTm1LV4o9vyK25j6CsVEZtqKMseg9a9W8IaL/ZmniaVcTyjJ9hWtGDlLyMqs+WJ0aKEQKOgGKfRRXoHCFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAGKMUUUAUrvTbO/jZLm3jlDddyg15/wCIfg5o2p7pbEm0uOoK9M/SvTaSk0mNNrY+cNU8Lat4cYQ3se5AflmX7rCqEcnzAY5+tfS15ZW9/btBcxJLG3BDDNeaeJfhky77nR2z6xHt9KynTvsbQqa6nAI4IDCrSNuXIqnPZ3VhMY7mF4mXjDLUkbnqh471xzg4s6oyTRdH3sdaUNzmoEkB/GpvqKgoduyOlLyfamjpRk5oAk7UqjPGPpUtra3F7KI7eJnYnHyivQfDnglLUrc6gA0nVU7CtIU5SZnOoorUqeD/AAs5ZL+9TCDlEbv716EAAMDoKRVVVCgAAdAKXpXfCCirI4pzcndi0UUVRIUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABijFFFAGZqOi6fqqFLu2Rz/exz+dcdqXwygkZnsJymeiNXoeKMVMoRluOMpR2Z43ceANZt2+VA6+qnNVf+EU1pG2i2dvwr26isnh4s1VeR45beDNZuDg25QerV0em/DoIQ19cZ/2Er0CjFNUIoUq0mUNP0my0yMJbQKvvjk1oYpKWtUrGV7hRRRTAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooA//Z\",\"mimeCode\":\"image/jpeg\",\"fileName\":\"updated_file.jpg\",\"uri\":\"\"}";
        BinaryObjectType binaryObjectType = JsonSerializationUtility.getObjectMapper().readValue(binaryObjectTypeString, BinaryObjectType.class);

        itemInformationRequest.getItemInformationRequestLine().get(0).getSalesItem().get(0).getItem().getItemSpecificationDocumentReference().get(0).getAttachment().setEmbeddedDocumentBinaryObject(binaryObjectType);

        request = patch("/processInstance")
                .header("Authorization",environment.getProperty("nimble.test-initiator-person-id"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(JsonSerializationUtility.getObjectMapper().writeValueAsString(itemInformationRequest))
                .param("processID", "ITEMINFORMATIONREQUEST")
                .param("processInstanceID", processInstanceId)
                .param("creatorUserID", "1337");
        mvcResult = this.mockMvc.perform(request).andDo(print()).andExpect(status().isOk()).andReturn();

        // get document content
        request = get("/document/json/983a7b0b-ea82-40ce-9e4e-76195f799487")
                .header("Authorization",environment.getProperty("nimble.test-initiator-person-id"));
        mvcResult = this.mockMvc.perform(request).andDo(print()).andExpect(status().isOk()).andReturn();

        itemInformationRequest = JsonSerializationUtility.getObjectMapper().readValue(mvcResult.getResponse().getContentAsString(), ItemInformationRequestType.class);

        Assert.assertEquals(0, itemInformationRequest.getAdditionalDocumentReference().size());
        Assert.assertEquals(1, itemInformationRequest.getItemInformationRequestLine().get(0).getSalesItem().get(0).getItem().getProductImage().size());
        Assert.assertEquals(1, itemInformationRequest.getItemInformationRequestLine().get(0).getSalesItem().get(0).getItem().getItemSpecificationDocumentReference().size());
        Assert.assertEquals("updated_file.jpg", itemInformationRequest.getItemInformationRequestLine().get(0).getSalesItem().get(0).getItem().getItemSpecificationDocumentReference().get(0).getAttachment().getEmbeddedDocumentBinaryObject().getFileName());

        BinaryObjectType binaryObject = binaryContentService.retrieveContent(itemInformationRequest.getItemInformationRequestLine().get(0).getSalesItem().get(0).getItem().getProductImage().get(0).getUri());
        Assert.assertNotNull(binaryObject);
        binaryObject = binaryContentService.retrieveContent(itemInformationRequest.getItemInformationRequestLine().get(0).getSalesItem().get(0).getItem().getItemSpecificationDocumentReference().get(0).getAttachment().getEmbeddedDocumentBinaryObject().getUri());
        Assert.assertEquals("updated_file.jpg", binaryObject.getFileName());
    }
}
