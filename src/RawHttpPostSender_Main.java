import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.http.Consts;
import org.apache.http.HttpHost;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.DefaultProxyRoutePlanner;
import org.apache.http.message.BasicNameValuePair;

public class RawHttpPostSender_Main {
	public static void printHowToUse(){
		System.out.println("How To Use");
		System.out.println("-u : URL , --uport : URL Port");
		System.out.println("-p : Proxy Address , --pport : Proxy Port");
		System.out.println("-h : Http Header, --hvalue : Http Value");
		System.out.println("-output : Output File Name");
		System.out.println("-status : Print Response Status");
		System.out.println("-t : [Choose One] form-data(default), param");
		System.out.println("-d <Not Param> : --dfile : file name or --dvalue : string, --dmime : string , --dpname : string  , --dnname : string");
		System.out.println("-d <Not Param> : --dpname : string , --dvalue : string");
		System.out.println("Excample");
		System.out.println("-u http://192.168.0.6/upload.php -h User-Agent --hvalue \"Mozilla/5.0 (Windows NT 6.1; WOW64; rv:30.0) Gecko/20100101 Firefox/30.0\" -d --dpname \"userfile\" --dfile \"C:/data/data1.txt\" --dnname \"test42342.txt\" --dmime \"text/plane\" -t form-data -p 192.168.0.10 --pport 8080 -status -output c:/data/res.txt");
	}
	public static void main(String[] args) throws ClientProtocolException, IOException {
		/*
		 * -u : URL , --uport : target port
		 * -p : proxy_host, --pport : proxy port
		 * -h : header, --hvalue : value
		 * -output : return value to output file
		 * -status : print status
		 * -t : form-data[default], param
		 *  Choose One ()
		 * -d : --dfile : file name or --dvalue : string, --dmime : string , --dpname : string  , --dnname : string
		 * -d : --dpname : string , --dvalue : string
		 * */
		if(args.length == 0){
			printHowToUse();
			return;
		}

		boolean isProxy = false;
		String proxyAddr = "";
		int proxyPort = 8080;

		boolean isHost = false;
		String hostAddr = "";
		int hostPort = 80;

		boolean isHeader = false;
		ArrayList<Map<String, String>> headers = new ArrayList<Map<String, String>>();

		String sendType = "form-data";

		boolean isData = false;
		ArrayList<Map<String, String>> datas = new ArrayList<Map<String, String>>();

		boolean isOutput = false;
		String outputFilePath = "";

		boolean isStatus = false;

		for(int i = 0 ; i < args.length ; ++i){
			/* PROXY */
			if(args[i].equals("-p")){
				isProxy = true;
				proxyAddr = args[++i];
				continue;
			}
			if(args[i].equals("--pport")){
				proxyPort = Integer.parseInt(args[++i]);
				continue;
			}
			/* HOST */
			if(args[i].equals("-u")){
				isHost = true;
				hostAddr = args[++i];
				continue;
			}
			if(args[i].equals("--uport")){
				hostPort = Integer.parseInt(args[++i]);
				continue;
			}

			/* HEADER */
			if(args[i].equals("-h")){
				String header = args[++i];
				if(args[i+1].equals("--hvalue")){
					++i;
					String data = args[++i];
					Map<String, String> map = new HashMap<String,String>();
					map.put(header, data);
					headers.add(map);
					continue;
				}else{
					/* HEADER Value Error */
					System.out.println("Check Header Value --hvalue");
					return;
				}
			}

			/* SEND TYPE */
			if(args[i].equals("-t")){
				sendType = args[++i];
				continue;
			}

			/* DATA */

			if(args[i].equals("-d")){
				Map<String, String> map = new HashMap<String, String>();
				while(true){
					try{
						if(args[i+1].equals("--dfile") | 
								args[i+1].equals("--dnname") || args[i+1].equals("--dpname") || 
								args[i+1].equals("--dmime") || args[i+1].equals("--dvalue")){
							map.put(args[i+1], args[i+2]);
							i += 2;
						}else{
							break;
						}
					}catch(ArrayIndexOutOfBoundsException ex){
						break;
					}

				}
				datas.add(map);
				continue;
			}



			/* OPTIONS */
			if(args[i].equals("-output")){
				isOutput = true;
				outputFilePath = args[++i];
				continue;
			}
			if(args[i].equals("-status")){
				isStatus = true;
			}
		}

		/* 모든 데이터를 다 읽었다면 실제 패킷을 만들어 보낸다.*/

		/* Set Proxy */
		HttpHost proxy = null;
		CloseableHttpClient httpclient = null;
		if(isProxy == true){
			proxy = new HttpHost(proxyAddr, proxyPort);
			DefaultProxyRoutePlanner routePlanner = new DefaultProxyRoutePlanner(proxy);
			httpclient = HttpClients.custom()
					.setRoutePlanner(routePlanner)			// Set the proxy
					.build();
		}else{
			httpclient = HttpClients.custom().build();
		}

		/* Create Post */
		HttpPost httppost = new HttpPost(hostAddr);

		/* Set Header */
		for(Map<String, String> map : headers){
			Iterator iter = map.entrySet().iterator();
			while(iter.hasNext()){
				Entry entry = (Entry)iter.next();
				httppost.setHeader(entry.getKey().toString(), entry.getValue().toString());
			}
		}

		/* Set Data */
		if(sendType.equals("form-data")){
			MultipartEntityBuilder builder = MultipartEntityBuilder.create();

			for(Map<String, String> data : datas){
				String dfile = data.get("--dfile");
				String dpname = data.get("--dpname");
				String dnname = data.get("--dnname");
				String dmime = data.get("--dmime");
				String dvalue = data.get("--dvalue");

				if(dpname == null){
					/* ERROR */
					System.out.println("--dpname is null");
					return;
				}
				if(dfile != null){
					if(dmime != null && dnname != null){
						builder.addBinaryBody(dpname, new File(dfile), ContentType.create(dmime), dnname);
					}else{
						builder.addBinaryBody(dpname, new File(dfile));
					}

				}else if(dvalue != null){
					if(dmime != null){
						builder.addTextBody(dpname, dvalue, ContentType.create(dmime));
					}else{
						builder.addTextBody(dpname, dvalue);
					}
				}
			}
			httppost.setEntity(builder.build());
		}else if(sendType.equals("param")){
			List<NameValuePair> formParams = new ArrayList<NameValuePair>();
			for(Map<String, String> data : datas){
				String dpname = data.get("--dpname");
				String dvalue = data.get("--dvalue");
				formParams.add(new BasicNameValuePair(dpname, dvalue));
			}
			UrlEncodedFormEntity entity = new UrlEncodedFormEntity(formParams, Consts.UTF_8);
			httppost.setEntity(entity);
		}


		/* Return Result */
		CloseableHttpResponse res = httpclient.execute(httppost);

		/* Output Console */
		if(isStatus) System.out.println(res.getStatusLine().toString());
		InputStream stream = res.getEntity().getContent();
		byte[] bytes = new byte[512];
		stream.read(bytes);
		System.out.println(new String(bytes));

		/* Output File */
		if(isOutput){
			try {
				////////////////////////////////////////////////////////////////
				BufferedWriter out = new BufferedWriter(new FileWriter(outputFilePath));
				out.write(res.getStatusLine().toString());
				out.newLine();
				out.write(new String(bytes));
				out.close();
				////////////////////////////////////////////////////////////////
			} catch (IOException e) {
				System.err.println(e); // 에러가 있다면 메시지 출력
				System.exit(1);
			}
		}
		/* Close */
		res.close();
		httpclient.close();
	}
}