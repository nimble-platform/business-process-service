package eu.nimble.service.bp.impl.mock;

import eu.nimble.common.rest.datachannel.IDataChannelClient;
import feign.Response;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.List;

@Profile("test")
@Component
public class DataChannelClientMock implements IDataChannelClient {
    public Response getEPCCodesForOrders(@RequestHeader("Authorization") String bearerToken, @RequestParam(value = "orders") List<String> orderIds) {
        return Response.builder().headers(new HashMap<>()).status(HttpStatus.OK.value()).body("[{\"orderId\":\"5b15c501-b90a-4f9c-ab0c-ca695e255237\",\"codes\":[\"2607191506\",\"2607191508\"]}]", Charset.defaultCharset()).build();
    }

    public Response getEPCCodesForOrder(@RequestHeader("Authorization") String bearerToken, @PathVariable("code") String storeId) {
        return Response.builder().headers(new HashMap<>()).status(HttpStatus.OK.value()).body("[{\"orderId\":\"5b15c501-b90a-4f9c-ab0c-ca695e255237\",\"codes\":[\"2607191506\",\"2607191508\"]}]", Charset.defaultCharset()).build();
    }

    public Response createChannel(@RequestHeader("Authorization") String bearerToken, @RequestBody String request){
        return Response.builder().headers(new HashMap<>()).status(HttpStatus.OK.value()).body("{\"channelID\":\"d03c3cfe-f369-496e-9ea9-fd8cb449c2c7-706-1339\"}", Charset.defaultCharset()).build();
    }
}