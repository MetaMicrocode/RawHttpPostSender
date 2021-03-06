import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.http.Consts;
import org.apache.http.HttpHost;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.DefaultProxyRoutePlanner;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.apache.http.message.BasicNameValuePair;

public class RawHttpPostSender_Main {
	public static void printHowToUse(){
		System.out.println("==== How To Use ====");
		System.out.println("-u : URL : http://xxx.xxx.xxx.xxx[:xxxx][/...]");
		System.out.println("-p : Proxy Address , --pport : Proxy Port [default : 8080]");
		System.out.println("-h : Http Header --hvalue : Http Value");
		System.out.println("-c : Cookie Name --cvalue : Cookie Value --cdomain : Cookie Domain [default: xxx.xxx.xxx.xxx] --cpath : Cookie Path [default: /]");
		System.out.println("-t : [Choose One] form-data(default), param");
		System.out.println("-d <DON'T USE PARAMETER> : --dpname : string <--dfile : File Name | --dvalue : string | --durl : URL>, [--dmime : string] [--dnname : string]");
		System.out.println("-d <DON'T USE PARAMETER> : --dpname : string --dvalue : string");
		System.out.println("-rs [--rsoutput <path>]");
		System.out.println("-rh --rhname <name> [--rhoutput <path>]");
		System.out.println("-rc [--rcoutput <path>]");
		System.out.println("== Excample ==");
		System.out.println("-u http://192.168.0.2:8000/upload.php -h User-Agent --hvalue \"Mozilla/5.0 (Windows NT 6.1; WOW64; rv:30.0) Gecko/20100101 Firefox/30.0\" -d --dpname \"userfile\" --dfile \"C:/data/data1.txt\" --dnname \"test42342.txt\" --dmime \"text/plane\" -t form-data -p 192.168.0.10 --pport 8080 -status -output c:/data/res.txt");
		System.out.println("== NOTE ==");
		System.out.println("'-c' option (COOKIE) is replaced that '-h' option like -h \"Cookie\" --hvalue \"PHPSESSION=key\"");
	}
	public static void main(String[] args) throws ClientProtocolException, IOException {
		if(args.length == 0){
			printHowToUse();
			return;
		}

		boolean isProxy = false;
		String proxyAddr = "";
		int proxyPort = 8080;

		boolean isHost = false;
		String hostAddr = "";

		boolean isHeader = false;
		ArrayList<Map<String, String>> headers = new ArrayList<Map<String, String>>();

		String sendType = "form-data";

		boolean isData = false;
		ArrayList<Map<String, String>> datas = new ArrayList<Map<String, String>>();

		boolean isOutput = false;
		String outputFilePath = "";

		boolean isStatus = false;
		
		ArrayList<Map<String, String>> resultHeaders = new ArrayList<Map<String, String>>();
		
		BasicCookieStore cookieStore = new BasicCookieStore();
		
		boolean isUseOptionRS = false;
		String outputResultStatusPath = "";
		
		boolean isUseOptionRC = false;
		String outputResultContentsPath = "";

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
						if(i+2 < args.length && args[i+1].equals("--dfile") || i+2 < args.length && args[i+1].equals("--durl") ||
								i+2 < args.length && args[i+1].equals("--dnname") || i+2 < args.length && args[i+1].equals("--dpname") || 
								i+2 < args.length && args[i+1].equals("--dmime") || i+2 < args.length && args[i+1].equals("--dvalue")){
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
			
			/* RESULT */
			if(args[i].equals("-rs")){
				isUseOptionRS = true;
				if(i+1 < args.length && args[i+1].equals("--rsoutput")){
					outputResultStatusPath = args[++i];
				}
				continue;
			}
			if(args[i].equals("-rc")){
				isUseOptionRC = true;
				if(i+1 < args.length && args[i+1].equals("--rcoutput")){
					outputResultContentsPath = args[++i];
				}
				continue;
			}
			if(args[i].equals("-rh")){
				Map<String, String> map = new HashMap<String, String>();
				while(true){
					try{
						if(i+2 < args.length && args[i+1].equals("--rhname") || i+2 < args.length && i+2 < args.length && args[i+1].equals("--rhoutput")){
							if(args[i+1].equals("--rhname"))	map.put("--rhname", args[i+2]);
							if(args[i+1].equals("--rhoutput"))	map.put("--rhoutput", args[i+2]);
							i += 2;
						}else{
							break;
						}
					}catch(ArrayIndexOutOfBoundsException ex){
						break;
					}

				}
				resultHeaders.add(map);
			}
		}
		
		/* Because the URL : It is must be read  */
		for(int i = 0 ; i < args.length ; ++i){
			/* COOKIE */
			if(args[i].equals("-c")){
				String name = args[++i];
				String data = "";
				String domain = new URL(hostAddr).getHost();
				String path = "/";
				while(true){
					if(i+2 < args.length && args[i+1].equals("--cvalue") || i+2 < args.length && args[i+1].equals("--cdomain") || i+2 < args.length && args[i+1].equals("--cpath")){
						if(args[i+1].equals("--cvalue")){
							data = args[i+2];
						}
						if(args[i+1].equals("--cdomain")){
							domain = args[i+2];
						}
						if(args[i+1].equals("--cpath")){
							path = args[i+2];
						}
						i+=2;
					}else{
						break;
					}
				}
				if(data.equals("")){
					/* ERROR */
					System.out.println("Check Cookie Value : --cvalue");
					return;
				}
				BasicClientCookie cookie = new BasicClientCookie(name,data);
				cookie.setDomain(domain);
				cookie.setPath(path);
				cookieStore.addCookie(cookie);
			}
		}

		/* Set Proxy */
		HttpHost proxy = null;
		CloseableHttpClient httpclient = null;
		if(isProxy == true){
			proxy = new HttpHost(proxyAddr, proxyPort);
			DefaultProxyRoutePlanner routePlanner = new DefaultProxyRoutePlanner(proxy);
			httpclient = HttpClients.custom()
					.setRoutePlanner(routePlanner)			// Set the proxy
					.setDefaultCookieStore(cookieStore)
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
				String durl = data.get("--durl");
				
				if(dpname == null){
					/* ERROR */
					System.out.println("--dpname is null");
					return;
				}
				
				if(durl != null){
					URL url = new URL(durl);
					if(dmime != null && dnname != null){
						builder.addBinaryBody(dpname,url.openStream(),ContentType.create(dmime), dnname);
					}if(dmime == null && dnname != null){
						builder.addBinaryBody(dpname,url.openStream(),ContentType.APPLICATION_OCTET_STREAM, dnname);
					}else{
						/* DO NOT CODE : builder.addBinaryBody(dpname,url.openStream()) : This is not exist File Name and To go Error */
						builder.addBinaryBody(dpname,url.openStream(),ContentType.APPLICATION_OCTET_STREAM, FilenameUtils.getName(durl));
					}
				}else if(dfile != null){
					if(dmime != null && dnname != null){
						builder.addBinaryBody(dpname, new File(dfile), ContentType.create(dmime), dnname);
					}else if(dmime == null && dnname != null){
						builder.addBinaryBody(dpname, new File(dfile), ContentType.APPLICATION_OCTET_STREAM, dnname);
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

		/* Output Result Status */
		if(isUseOptionRS) System.out.println(res.getStatusLine().toString());
		if(!outputResultStatusPath.equals("")){
			BufferedWriter out = new BufferedWriter(new FileWriter(outputResultStatusPath));
			out.write(res.getStatusLine().toString());
			out.close();
		}
		
		/* Output Result Header */
		for(Map<String, String> map : resultHeaders){
			String rhname = map.get("--rhname");
			String rhoutput = map.get("--rhoutput");
			if(rhname != null){
				org.apache.http.Header[] hs = res.getHeaders(rhname);
				for(org.apache.http.Header header : hs){
					System.out.println(header.toString());
				}
				if(rhoutput != null){
					BufferedWriter out = new BufferedWriter(new FileWriter(rhoutput));
					for(org.apache.http.Header header : hs){
						out.write(header.toString());
						out.newLine();
					}
					out.close();
				}
			}else{
				/* ERROR */
				System.out.println("Check Header Name '--rhname'");
				return;
			}
		}
		
		/* Output Result Contents */
		if(isUseOptionRC){
			System.out.println(new String(IOUtils.toByteArray(res.getEntity().getContent())));
		}
		if(!outputResultContentsPath.equals("")){
			BufferedWriter out = new BufferedWriter(new FileWriter(outputResultContentsPath));
			out.write(new String(IOUtils.toByteArray(res.getEntity().getContent())));
			out.close();
		}
		
		/* Close */
		res.close();
		httpclient.close();
	}
}
