import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import net.minidev.json.JSONArray;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.log4j.BasicConfigurator;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;
import java.io.IOException;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.lessThanOrEqualTo;

public class PostRequests {
    StringBuilder url;
    CloseableHttpClient httpClient;
    HttpPost httpPost;
    HttpEntity stringEntity;
    CloseableHttpResponse response;
    DocumentContext docCtx;
    JSONArray jsonArray;
    String apiKey="3b0359a4820362c5bf2906259462189e";
    String refCounter ="fbe1c3c8-e39f-11ea-8513-b88303659df5";
    String refKharkiv ="db5c88e0-391c-11dd-90d9-001a92567626";
    String refPoltava="db5c8892-391c-11dd-90d9-001a92567626";
    String refStr ="1b1edc1e-e3ba-11e2-874c-d4ae527baec3";
    boolean refIsFound=false;
    String cargoType ="Documents";

    @BeforeTest
    public void setup() {
        url= new StringBuilder("https://api.novaposhta.ua/v2.0/json");
        BasicConfigurator.configure();
        httpClient = HttpClients.createDefault();
    }
    @AfterMethod
    public void clearUrl(){
        url.setLength(0);
        url.append("https://api.novaposhta.ua/v2.0/json");
        refIsFound=false;
    }

    /**
     * /Counterparty/getCounterpartyOptions
     * send without any apiKey -> assert that request is unsuccessful
     * send with apiKey -> assert that it is successful
     */

    @Test
    public void getCounterpartyOptionsAllowedOnlyWithApiKey() throws IOException {
        url.append("/Counterparty/getCounterpartyOptions/");

        httpPost = new HttpPost(String.valueOf(url));
        String jsonBodyUnsuccess = "{\n" +
                "\"apiKey\": \"\",\n" +
                " \"modelName\": \"Counterparty\",\n" +
                " \"calledMethod\": \"getCounterpartyOptions\",\n" +
                " \"methodProperties\": {\n" +
                " \"Ref\": \""+ refCounter +"\"\n" +
                " }\n" +
                "}\n";
        sendRequest(jsonBodyUnsuccess);
        String pathToSuccess = "$..success";
        jsonArray = docCtx.read(pathToSuccess);
        Assert.assertEquals(jsonArray.toString(), "[false]");

        String jsonBodySuccess = "{\n" +
                "\"apiKey\": \"" + apiKey + "\",\n" +
                " \"modelName\": \"Counterparty\",\n" +
                " \"calledMethod\": \"getCounterpartyOptions\",\n" +
                " \"methodProperties\": {\n" +
                " \"Ref\": \"" + refCounter + "\"\n" +
                " }\n" +
                "}\n";
        sendRequest(jsonBodySuccess);
        pathToSuccess = "$..success";
        jsonArray = docCtx.read(pathToSuccess);
        Assert.assertEquals(jsonArray.toString(), "[true]");

    }

    /**
     * /Address/save
     * create Counterparty Address
     * warning is appeared when Address haw already existed
     */
    @Test
    public void createCounterpartyAddressAndGetWarningIfAddressExists() throws IOException {
        Assert.assertEquals(getKharkiv(), "[\"Харьков\"]");
        Assert.assertTrue(getKharkivStreetIsReturnedByRef());
        url.append("/Address/save/");
        httpPost = new HttpPost(String.valueOf(url));
        String jsonCounterAddress="{\n" +
                "\"modelName\": \"Address\",\n" +
                "\"calledMethod\": \"save\",\n" +
                "\"methodProperties\": {\n" +
                "\"CounterpartyRef\": \""+refCounter+"\",\n" +
                "\"StreetRef\": \""+refStr+"\",\n" +
                "\"BuildingNumber\": \"7\",\n" +
                "\"Flat\": \"2\" \n" +
                "},\n" +
                "\"apiKey\": \""+apiKey+"\"\n" +
                "}";
        sendRequest(jsonCounterAddress);
        String pathToSuccess = "$..success";
        jsonArray = docCtx.read(pathToSuccess);
        Assert.assertEquals(jsonArray.toString(), "[true]");
        String pathToWarning="$..warnings";
        jsonArray = docCtx.read(pathToWarning);
        System.out.println(jsonArray.toString());
        boolean bodyContainsText=jsonArray.toString().contains("Address already exists");
        Assert.assertTrue(bodyContainsText);
    }
    /**
     * /getDocumentPrice
     * if price of invoice less than 40 test is passed
     *
     */
    @Test
    public void getDocumentPriceAndPassIfCostLessThan() throws IOException {
        Assert.assertTrue(getCargoType());
        url.append("/getDocumentPrice");
        httpPost = new HttpPost(String.valueOf(url));
        String jsonBody="\n" +
                "{\n" +
                "  \"modelName\": \"InternetDocument\",\n" +
                "  \"calledMethod\": \"getDocumentPrice\",\n" +
                "  \"methodProperties\": {\n" +
                "    \"CitySender\": \""+refPoltava+"\",\n" +
                "    \"CityRecipient\": \""+refKharkiv+"\",\n" +
                "    \"Weight\": \"10\",\n" +
                "    \"ServiceType\": \"WarehouseWarehouse\",\n" +
                "    \"Cost\": \"100\",\n" +
                "    \"CargoType\": \""+ cargoType +"\",\n" +
                "    \"SeatsAmount\": \"1\"}}";
        sendRequest(jsonBody);
        String pathToCost = "$..Cost";
        jsonArray = docCtx.read(pathToCost);
        //System.out.println(jsonArray.toString());
        String costStr = null;
        for (Object o : jsonArray) {
            costStr =o.toString();
        }
        int cost;
        cost=Integer.parseInt(costStr);
        assertThat(cost,lessThanOrEqualTo(45));
    }

    /**
     * method to send request with Content-type header
     */
    public void sendRequest(String body) throws IOException {
        stringEntity = new StringEntity(body);
        httpPost.setEntity(stringEntity);
        httpPost.setHeader("Content-type", "application/json");
        response = httpClient.execute(httpPost);
        docCtx = JsonPath.parse(response.getEntity().getContent());
    }

    /**
     * /Address/getCities
     * get city name by Ref
     * @return String city name
     */
    public String getKharkiv() throws IOException {
        url.append("/Address/getCities");
        httpPost = new HttpPost(String.valueOf(url));

        String jsonCity="{\n" +
                "\"modelName\": \"Address\",\n" +
                "\"calledMethod\": \"getCities\",\n" +
                "\"methodProperties\": {\n" +
                "\"Ref\": \""+refKharkiv+"\"\n" +
                "},\n" +
                "\"apiKey\": \""+apiKey+"\"\n" +
                "}";
        sendRequest(jsonCity);
        String pathToKharkiv = "$..DescriptionRu";
        jsonArray = docCtx.read(pathToKharkiv);
        return jsonArray.toString();
    }

    /**
     * /Address/getStreet
     * get street by Ref
     * @return true if street exists
     */
    public boolean getKharkivStreetIsReturnedByRef() throws IOException {
        url.append("/Address/getStreet/");
        httpPost = new HttpPost(String.valueOf(url));
        String jsonStreet="{\n" +
                "\"modelName\": \"Address\",\n" +
                "\"calledMethod\": \"getStreet\",\n" +
                "\"methodProperties\": {\n" +
                "\"CityRef\": \""+refKharkiv+"\"\n" +
                "},\n" +
                "\"apiKey\": \""+apiKey+"\"\n" +
                "}";
        sendRequest(jsonStreet);
        String pathToStreet="$..Ref";
        jsonArray = docCtx.read(pathToStreet);

        for (Object ref : jsonArray) {
            if(ref.equals(refStr)){
                refIsFound=true;
                break;
            }

        }
       return refIsFound;


    }

    /**
     * /common/getCargoTypes
     * get cargo type by its description
     * @return true if cargoType exists
     */
    public boolean getCargoType() throws IOException {
        url.append("/common/getCargoTypes");
        httpPost = new HttpPost(String.valueOf(url));
        String jsonCargo="{\n" +
                "\"modelName\": \"Common\",\n" +
                "\"calledMethod\": \"getCargoTypes\",\n" +
                "\"methodProperties\": {},\n" +
                "\"apiKey\": \""+apiKey+"\"\n" +
                "}";
        sendRequest(jsonCargo);
        String pathToRef = "$..Ref";
        jsonArray = docCtx.read(pathToRef);
        for (Object ref : jsonArray) {
            if(ref.equals(cargoType)){
                refIsFound=true;
                break;
            }

        }
        return refIsFound;
    }


}
