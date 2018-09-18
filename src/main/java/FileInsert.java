
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import org.apache.commons.codec.binary.Base64;
import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;


/**
 * Hello world!
 *
 */
public class FileInsert {
	
	private static String accesstoken = "";
	private static String instanceUrl = "";
	private static String folderId = "";
	private static String authEndpoint = "";
	private static String username = "";
	private static String password = "";
	private static String clientId = "";
	private static String clientPass = "";
	
	
	public static String processFileAndInsert(String folderPath) throws ClientProtocolException, IOException{
		
		String result = "";
		try {
			readPropertiesFile();
		
			String finalEndpoint = authEndpoint+"?grant_type=password&username="+username+"&password="+password+"&client_id="+clientId+"&client_secret="+clientPass;
			System.out.println(finalEndpoint);
		
			getAccessToken(finalEndpoint);
			File folder = new File(folderPath);
			for(File fileEntry : folder.listFiles()){
		        if (!fileEntry.isDirectory()) {
		        	String fileName = fileEntry.getName();
		        	String fileExtension = fileName.substring(fileName.lastIndexOf(".")+1);
		        	if("csv".equalsIgnoreCase(fileExtension)){
		        		insertCsvFiles(fileEntry);
		        	}
		        }
		    }
			
			result = "Success";
		} catch (Exception e) {
			e.printStackTrace();
			result="Error";
		}
		return result;
	}
	
	private static void readPropertiesFile(){
		
		folderId = System.getenv("folderId");
		authEndpoint = System.getenv("authEndpoint");
		username = System.getenv("username");
		password = System.getenv("password");
		clientId = System.getenv("clientId");
		clientPass = System.getenv("clientPass");
		
	}
	
	private static void getAccessToken(String endpoint) throws ClientProtocolException, IOException, JSONException{
		CloseableHttpClient httpclient = HttpClients.createDefault();
		HttpPost httppost = new HttpPost(endpoint);
		CloseableHttpResponse response = httpclient.execute(httppost);
		try {
		    HttpEntity entity = response.getEntity();
		    if (entity != null && response.getStatusLine().getStatusCode() == HttpStatus.SC_OK ) {
		        	String json = EntityUtils.toString(entity);
		        	JSONObject result = new JSONObject(json);
		            System.out.println(json);
		            accesstoken = result.getString("access_token");
		            System.out.println(accesstoken);
		            instanceUrl = result.getString("instance_url");
		        
		    }
		} finally {
		    response.close();
		}
	}
	
	private static void insertCsvFiles(File fileToBeInserted) throws IOException{
		
		String fileName = fileToBeInserted.getName();
		System.out.println(fileName);
		FileInputStream fis = new FileInputStream(fileToBeInserted);
		byte[] bytesArray = new byte[(int) fileToBeInserted.length()];
		fis.read(bytesArray);
		fis.close();
		JSONObject doc = new JSONObject();
		CloseableHttpClient httpclient = HttpClients.createDefault();
		String docId = "";
		try{
			doc.put("Name", fileName);
			doc.put("folderId", folderId);
			doc.put("Type", "csv");
			doc.put("body", new String(Base64.encodeBase64(bytesArray)));
			System.out.println(instanceUrl+"/services/data/v20.0/sobjects/Document/");
			HttpPost httppost = new HttpPost(instanceUrl+"/services/data/v20.0/sobjects/Document/");
			httppost.setHeader("Authorization", "OAuth " +accesstoken);
			StringEntity body = new StringEntity(doc.toString(1));
			body.setContentType("application/json");
	        httppost.setEntity(body);
	        System.out.println(doc.toString(1));

			CloseableHttpResponse response = httpclient.execute(httppost);
			System.out.println("HTTP status"+response.getStatusLine().getStatusCode());
			 if(response.getStatusLine().getStatusCode() == HttpStatus.SC_CREATED ){
			     try{
			         HttpEntity entity = response.getEntity();
			         if(entity != null){
			        	 String json = EntityUtils.toString(entity);
			        	 JSONObject result = new JSONObject(json);
			        	 System.out.println(json);
			        	 docId=result.getString("id");
			             System.out.println("new record id:-" +docId+"\n\n");
			         }
			     }catch(Exception e){
			         e.printStackTrace();
			     }finally {
				    response.close();
				    fileToBeInserted.delete();
				 }
			 }
		}
		catch(Exception e){
		    e.printStackTrace();
		}
		
	}
}
