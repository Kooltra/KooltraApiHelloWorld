import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;


public class RESTAPI {
	
	/**
	 * This class gets authenticate the user, create the body of HTTP POST request to create 
	 * two account records and one trade record in Kooltra, send the request to the API and prints
	 * the result 
	 * 
	 * @param args
	 * @throws Exception
	 * 
	 * Author: Payam Izad (payam@kooltra.com)
	 * Feb 2018
	 * 
	 */

	public static void main(String[] args) throws Exception {
		Map<String, String> config = null;

        //read the configuration file to get the credentials
        config = getCredentials();
		
        //Authenticate 
		Map<String,String> oauth;
		try{
			oauth = OauthAuthenticate(config.get("CONSUMER_KEY"), config.get("CONSUMER_SECRET"), config.get("USER"), config.get("PASS"),config.get("TOKEN"));
		}catch(Exception e){
			System.out.println("OAuth Error:" + String.valueOf(e));
			return;
		}
		
		//Generate the body of request to create two accounts
		List<Map<String,String>> accountsToCreate = generateAccountCreateBody();
		List<Map<String,String>> tradesToCreate = generateTradeCreateBody();
		
		//create accounts
		sendRequest(accountsToCreate, oauth.get("instance_url"), oauth.get("access_token"), "staticdata", "account");
		sendRequest(tradesToCreate, oauth.get("instance_url"), oauth.get("access_token"), "transactions", "trade");

	}
	
	/**
	 * does the authentication for a given org and returns a Map<String,String> with the result
	 * @param consumerKey
	 * @param consumerSecret
	 * @param userName
	 * @param password
	 * @param securityToken
	 * @return
	 * @throws Exception
	 */
	public static Map<String,String> OauthAuthenticate(String consumerKey, String consumerSecret, String userName, String password, String securityToken) throws Exception{
	    //Http object for making the http request
	       CloseableHttpClient httpClient = HttpClients.createDefault();
	    //The URL for authentication
	       String url = "https://login.salesforce.com/services/oauth2/token";
	 
	 
	    //HttpPost object to do HTTP POST 
	      HttpPost post = new HttpPost(url);
	    //create the standard Oauth2.0 body for authentication
	       ArrayList<BasicNameValuePair> parametersBody = new ArrayList<BasicNameValuePair>();
	       parametersBody.add(new BasicNameValuePair("grant_type", "password"));      //The grant type is password ( this must be the word "password")
	       parametersBody.add(new BasicNameValuePair("client_id", consumerKey));
	       parametersBody.add(new BasicNameValuePair("client_secret", consumerSecret));
	       parametersBody.add(new BasicNameValuePair("username", userName));
	       parametersBody.add(new BasicNameValuePair("password", password+securityToken));
	       post.setEntity(new UrlEncodedFormEntity(parametersBody, HTTP.UTF_8));
	    //posting 
	      HttpResponse r = httpClient.execute(post);
	      HttpEntity e = r.getEntity();
	      String out = "OauthCallout response: failed to submit.";
	      if (e != null ) {
	        // decode the entity content
	           out = EntityUtils.toString(e);
	         }

	     ObjectMapper p = new ObjectMapper();
	     Map<String,String> theresp = p.readValue(out.getBytes(), Map.class);
	    // NOTE: get the session ID and REST URL as follows:
	     String thesessionID = theresp.get("access_token");
	     String instanceurl = theresp.get("instance_url");
	     System.out.println(thesessionID);
	     System.out.println(instanceurl);
	     return theresp;
	}
	

	public static List<Map<String,String>> generateAccountCreateBody(){
		
		//a list to hold all records
		List<Map<String,String>> outbody = new ArrayList<Map<String,String>>();
		 
		 //creating a record: the map for AccountOne that holds accountOne data
		 Map<String,String> accountOne = new HashMap<String,String>();
		 accountOne.put("ObjectType","Account");
		 accountOne.put("Action","Create");
		 accountOne.put("Name","AccountOneTest");
		 accountOne.put("AccountCode","1111");
		 accountOne.put("Status","Active");
		 accountOne.put("Entity","Forex Global"); 
		 accountOne.put("CounterpartyType","Company"); 
		 accountOne.put("LegalName","One Corp");
		 accountOne.put("SettlementType","Payments");
		 accountOne.put("RequestID","0");
         outbody.add(accountOne);
          
         Map<String,String> accountTwo = new HashMap<String,String>();
         accountTwo.put("ObjectType","Account");
         accountTwo.put("Action","Create");
         accountTwo.put("Name","AccountTwoTest");
         accountTwo.put("AccountCode","2222");
         accountTwo.put("Status","Active");
         accountTwo.put("Entity","Forex Global"); 
         accountTwo.put("CounterpartyType","Company"); 
         accountTwo.put("LegalName","Two Corp");
         accountTwo.put("SettlementType","Vostro");
         accountTwo.put("RequestID","1");
         outbody.add(accountTwo);
 
          
          return outbody;
	}
	
	public static List<Map<String,String>> generateTradeCreateBody(){
		
		//a list to hold all records
		List<Map<String,String>> outbody = new ArrayList<Map<String,String>>();
				 
		//creating a record: the map for creating a trade
		Map<String,String> tradeOne = new HashMap<String,String>();
		tradeOne.put("ObjectType","Trade");
		tradeOne.put("Action","Create");
		tradeOne.put("Name","TestTradeOne");
		tradeOne.put("AccountCode","1111");
		tradeOne.put("Type","FXSPOT");
		tradeOne.put("TradeAction","BUY");
		tradeOne.put("CCY1","USD");
		tradeOne.put("CCY2","CAD");
		tradeOne.put("Notional","1000");
		tradeOne.put("Rate","1.3");
		tradeOne.put("CounterAmount","1300"); 
		tradeOne.put("ValueDate","20180223");
		tradeOne.put("Status","Open");
		tradeOne.put("RequestID","0");
		outbody.add(tradeOne);
		
		return outbody;
		
	}
	/**
	 * Sends an http request with recordsToSend as the body and returns the httpEntity of the response
	 * @param recordsToSend
	 * @param url
	 * @param sessionID
	 * @param restlayer
	 * @param objtype
	 * @return
	 * @throws JsonProcessingException 
	 */
	
	public static HttpEntity sendRequest(List<Map<String,String>> recordsToSend, String url, String sessionID, String restlayer, String objtype) throws JsonProcessingException{

		 ObjectMapper objmap = new ObjectMapper();
		
		 //create the JSON string here to set the body of the request
		String JSONstring = "{\"submit\":"+ objmap.writeValueAsString(recordsToSend) +"}";
		
		 //create the http client to send the request
		   CloseableHttpClient httpClient = HttpClients.createDefault();
	
		 //create an http post request using the url ,restlayer and objtype
		  HttpPost post;
	      post = new HttpPost(url+"/services/apexrest/Kooltra/"+restlayer+"/"+objtype);
		  
		   
		//create the body of the request using JSONstring
		   HttpEntity thebody = new StringEntity(JSONstring, HTTP.UTF_8);
		 
		 //set the header of the request
		   post.setHeader("Authorization","Bearer "+sessionID);
		   post.setHeader("Content-Type", "application/json");
		 
		 //set the body of the request
		   post.setEntity(thebody);
		 //posting the request and receiving the response
		   HttpResponse r = null;
		   try{
			   r = httpClient.execute(post);
			   System.out.println("HTTPcallout: Response : " + r.getStatusLine());
			   
			   HttpEntity e = r.getEntity();
			   System.out.println("HTTPcallout: Response : " +e.getContent());
			   String out = EntityUtils.toString(e);
			   ObjectMapper p = new ObjectMapper();
			   List<Map<String,String>> theresp = p.readValue(out.getBytes(), List.class);
				
			   System.out.println("---------------------------");
			   for(int i=0;i<theresp.size();i++){
				   Map<String,String> m = theresp.get(i);
				   System.out.println("Response "+String.valueOf(i));
					  
				   for(String s: m.keySet()){
					   System.out.println(s+":"+m.get(s));
					}
				System.out.println("---------------------------");
			  }
			   return e;
		  }catch(Exception e){
			  System.out.println("HTTPcallout: Response : " + r.getStatusLine());
			  return null;
		  }

	}
	public static Map<String,String> getCredentials(){
		Map<String,String> creds = new HashMap<String,String>();
		creds.put("USER","MyUsernam@kooltra.com");
		creds.put("PASS","MyPassword");
		creds.put("TOKEN","SecurityToken");
		creds.put("CONSUMER_KEY","ConsumerKey");
		creds.put("CONSUMER_SECRET","ConsumerSecret");

		return creds;
	}


}
