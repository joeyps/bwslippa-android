package joeyp.bwslippa;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.lang.ref.WeakReference;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.net.ssl.HttpsURLConnection;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.params.ClientPNames;
import org.apache.http.cookie.Cookie;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.accounts.AccountManager;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.util.Log;

public class RPCHelper {
	
	public static final String HOST = "https://bwslippa.appspot.com/";
	
	private static final String LOG_TAG = "RPC"; 
	private static final String USER_AGENT = "Android";
	private static final int REQUEST_TIMEOUT = 15000;
	private static final String SACSID = "SACSID";
	private static final String ACSID = "ACSID";
	
	//get
	public static final String API_GET_ITEM_TAGS = "";
	public static final String API_BIND_WORKSPACE = "bindWorkspace";
	public static final String API_GET_RESERVED = "getReserved";
	public static final String API_QUERY_CUSTOMER = "customer";
	//post
	public static final String API_RESERVE = "reserve";
	public static final String API_SIGN_IN = "userSignIn";
	
	private static RPCHelper sHelper;
	private static DefaultHttpClient sHttpClient;
	private Set<WeakReference<RPCListener>> mListener;
	private Handler mWorker;
	private HandlerThread mThread;
	private String mToken;
	
	public class RPC implements Runnable {
		
		private String mUrl;
		private RPCCallback mCallback;
		
		public RPC(String url, RPCCallback callback) {
			mUrl = url;
			mCallback = callback; 
		}

		@Override
		public void run() {
			Log.d("joey", "get=" + mUrl);
            HttpGet http_get = new HttpGet(mUrl);
            String result = "";
			try {
//				UrlEncodedFormEntity entity =
//				        new UrlEncodedFormEntity(params, "UTF-8");
//				    post.setEntity(entity);
				
				HttpResponse response = sHttpClient.execute(http_get);
				final int statusCode = response.getStatusLine().getStatusCode();
				
				if(statusCode != 200) {
					UIHandler.get().post(new Runnable() {
						
						@Override
						public void run() {
							onFailed(statusCode);
							return;
						}
					});
				}
				result = EntityUtils.toString(response.getEntity());
				Log.d("joey", "statusCode=" + statusCode + " content=" + result);
				JSONObject jObj = new JSONObject(result);
				if(mCallback != null) {
					mCallback.onCallback(jObj);
				}
			} catch (ClientProtocolException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				
				try {
					JSONArray jAry = new JSONArray(result);
					if(mCallback != null) {
						mCallback.onCallback(jAry);
					}
				} catch (JSONException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
			}
			
		}
		
	}
	
	public static class PostRPC implements Runnable {
		
		private String mUrl;
		private RPCCallback mCallback;
		private HttpEntity mEntity;
		
		public PostRPC(String url, HttpEntity entity, RPCCallback callback) {
			mUrl = url;
			mEntity = entity;
			mCallback = callback; 
		}

		@Override
		public void run() {
			Log.d("joey", "post=" + mUrl);
            HttpPost http_post = new HttpPost(mUrl);
            if(mEntity != null) {
            	http_post.setEntity(mEntity);
            	//sets a request header so the page receving the request
                //will know what to do with it
                http_post.setHeader("Accept", "application/json");
                http_post.setHeader("Content-type", "application/json");
            }
            
            HttpResponse response;
            String result = "";
			try {
				response = sHttpClient.execute(http_post);
				result = EntityUtils.toString(response.getEntity());
				
				JSONObject jObj = new JSONObject(result);
				if(mCallback != null) {
					boolean success = jObj.optBoolean("success", false);
					if(success)
						mCallback.onCallback(jObj);
					else
						mCallback.onFail();
				}
			} catch (ClientProtocolException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				
				try {
					JSONArray jAry = new JSONArray(result);
					if(mCallback != null) {
						mCallback.onCallback(jAry);
					}
				} catch (JSONException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
			}
			
		}
		
	}

	public interface RPCListener {
		public void onInit();
		public void onInitFailed(int errorCode);
	}
	
	public interface RPCCallback {
		public void onCallback(JSONObject obj);
		public void onCallback(JSONArray obj);
		public void onFail();
	}
	
	public static RPCHelper getInstance() {
		if(sHelper == null) {
			sHelper = new RPCHelper();
			sHelper.sHttpClient = new DefaultHttpClient();
			sHelper.mListener = new HashSet<WeakReference<RPCListener>>();
			sHelper.mThread = new HandlerThread("RPCWorker");
			sHelper.mThread.start();
			sHelper.mWorker = new Handler(sHelper.mThread.getLooper());
		}
		return sHelper;
	}
	
	public void init(String token) {
		mWorker.post(new GetCookieTask(token));
	}
	
	public void registerListenter(RPCListener listener) {
		if(!mListener.contains(listener))
			mListener.add(new WeakReference<RPCListener>(listener));
	}
	
	public void unregisterListenter(RPCListener listener) {
		WeakReference<RPCListener> refToRemove = null;
		for(WeakReference<RPCListener> ref : mListener) {
			RPCListener l = ref.get();
			if(l != null && l.equals(listener))
				refToRemove = ref;
		}
		if(refToRemove != null)
			mListener.remove(refToRemove);
	}
	
	public void call(String api, RPCCallback callback, Object... param) {
		try {
			StringBuilder sb = new StringBuilder();
			for(int i = 0; i < param.length; i++) {
				String p = wrap(param[i]).toString();
				p = URLEncoder.encode(p, "UTF-8");
				sb.append(String.format("&arg%d=%s", i, p));
			}
		
			String url = String.format("%srpc?action=%s%s", HOST, api, sb.toString());
			mWorker.post(new RPC(url, callback));			
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void call(String api, List<NameValuePair> params, RPCCallback callback) {
		try {
			StringBuilder sb = new StringBuilder();
			for(NameValuePair p : params) {
				String value = p.getValue();
				value = URLEncoder.encode(value, "UTF-8");
				sb.append(String.format("&%s=%s", p.getName(), value));
			}
		
			if(!sb.toString().isEmpty()) {
				sb.insert(0, "?");
			}
			String url = String.format("%s%s%s", HOST, api, sb.toString());
			mWorker.post(new RPC(url, callback));			
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void post(String api, RPCCallback callback, Object... param) {
		String url = String.format("%srpc", HOST);
		JSONArray jAry = new JSONArray();
		jAry.put(api);
		try {
			for(Object obj : param) {
				jAry.put(obj);
			}
			
			StringEntity e = new StringEntity(jAry.toString(), HTTP.UTF_8);
			mWorker.post(new PostRPC(url, e, callback));
		
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
	}
	
	public void release() {
		
	}
	
	private class GetCookieTask implements Runnable {
		
		private String mToken;
		
		GetCookieTask(String token) {
			mToken = token;
		}

		@Override
		public void run() {
			try {
                // Don't follow redirects
	        	sHttpClient.getParams().setBooleanParameter(ClientPNames.HANDLE_REDIRECTS, false);
	            String url = String.format("%s_ah/login?continue=http://localhost/&auth=%s", HOST, mToken);
	            HttpGet http_get = new HttpGet(url);
	            HttpResponse response;
	            response = sHttpClient.execute(http_get);
	            if(response.getStatusLine().getStatusCode() != 302) {
	            	UIHandler.get().post(new Runnable() {

	    				@Override
	    				public void run() {
	    					onFailed(0);
	    				}
	    	        	
	    	        });
	            }
	                
	            //FIXME Invalid use of SingleClientConnManager: connection still allocated.
	            String content = EntityUtils.toString(response.getEntity());
	//                InputStream is = response.getEntity().getContent();
	//                
	//                BufferedReader reader = new BufferedReader(new InputStreamReader(is));
	//                String line = null;
	//                while ((line = reader.readLine()) != null) {
	//                	//Log.d("joey", "auth=" + line);
	//                }
	//                is.close();
	//                reader.close();
	            Header[] headers = response.getHeaders("Set-Cookie");
	            for(Cookie cookie : sHttpClient.getCookieStore().getCookies()) {
	            	if(cookie.getName().equals("ACSID"))
	            		RPCHelper.this.mToken = cookie.getValue();
	            	else if(cookie.getName().equals("SACSID")) {
	            		RPCHelper.this.mToken = cookie.getValue();
	                }
	            	
	            	if(RPCHelper.this.mToken != null) {
	            		UIHandler.get().post(new Runnable() {

	        				@Override
	        				public void run() {
	        					for(WeakReference<RPCListener> l : mListener) {
	        	        			RPCListener listener = l.get();
	        	        			if(listener != null)
	        	        				listener.onInit();
	        	        		}
	        				}
	        	        	
	        	        });
	                	return;
	            	}
	            }
	        } catch (ClientProtocolException e) {
	                e.printStackTrace();
	        } catch (IOException e) {
	                e.printStackTrace();
	        } finally {
	        	sHttpClient.getParams().setBooleanParameter(ClientPNames.HANDLE_REDIRECTS, true);
	        }
	        UIHandler.get().post(new Runnable() {

				@Override
				public void run() {
					onFailed(401);
				}
	        	
	        });
		}

    }
	
	private void onFailed(int errorCode) {
		for(WeakReference<RPCListener> l : mListener) {
			RPCListener listener = l.get();
			if(listener != null) {
				listener.onInitFailed(errorCode);
			}
		}
	}
	
	public static Object wrap(Object object) {
        try {
            if (object == null) {
                return null;
            }
            if (object instanceof JSONObject || object instanceof JSONArray  || 
                    //NULL.equals(object)      || object instanceof JSONString || 
                    object instanceof Byte   || object instanceof Character  ||
                    object instanceof Short  || object instanceof Integer    ||
                    object instanceof Long   || object instanceof Boolean    || 
                    object instanceof Float  || object instanceof Double
//                    ||object instanceof String
                    ) {
                return object;
            }
            
            if (object instanceof String) {
            	return String.format("\"%s\"", object);
            }
            
            if (object instanceof Collection) {
                return new JSONArray((Collection)object);
            }
//            if (object.getClass().isArray()) {
//                return new JSONArray(object);
//            }
            if (object instanceof Map) {
                return new JSONObject((Map)object);
            }
            Package objectPackage = object.getClass().getPackage();
            String objectPackageName = objectPackage != null ? 
                objectPackage.getName() : "";
            if (
                objectPackageName.startsWith("java.") ||
                objectPackageName.startsWith("javax.") ||
                object.getClass().getClassLoader() == null
            ) {
                return object.toString();
            }
            return new JSONObject();
        } catch(Exception exception) {
            return null;
        }
    }
	
	public JSONObject getJSONObject(String api, Object... param) {
		
		if(Looper.myLooper() == Looper.getMainLooper())
			throw new IllegalThreadStateException();
		
		HttpsURLConnection conn = null;
		try {
			StringBuilder sb = new StringBuilder();
			for(int i = 0; i < param.length; i++) {
				String p = wrap(param[i]).toString();
				p = URLEncoder.encode(p, "UTF-8");
				sb.append(String.format("&arg%d=%s", i, p));
			}
		
			URL url = new URL(String.format("%srpc?action=%s%s", HOST, api, sb.toString()));
			conn = (HttpsURLConnection) url.openConnection();
			conn.setRequestMethod("GET");
			conn.setConnectTimeout(REQUEST_TIMEOUT);
			conn.setRequestProperty("User-Agent", USER_AGENT);
			conn.setRequestProperty("Cookie", String.format("%s=%s", SACSID, mToken));
			conn.connect();
			
			int responseCode = conn.getResponseCode();
			Log.i(LOG_TAG, "Sending 'GET' request to URL :" + url);
			Log.i(LOG_TAG, "Response Code :" + responseCode);
			BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
			String line;
			StringBuffer response = new StringBuffer();
	 
			while ((line = br.readLine()) != null) {
				response.append(line);
			}
			br.close();
			return new JSONObject(response.toString());
	 
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (JSONException e) {
			e.printStackTrace();
		} finally {
			if(conn != null)
				conn.disconnect();
		}
		return null;
	}
}
