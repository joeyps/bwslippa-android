package joeyp.bwslippa;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.lang.ref.WeakReference;
import java.net.URLEncoder;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.accounts.AccountManager;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;

public class RPCHelper {
	public static final String HOST = "https://bwslippa.appspot.com/";
	
	//get
	public static final String API_GET_ITEM_TAGS = "";
	public static final String API_BIND_WORKSPACE = "bindWorkspace";
	public static final String API_GET_RESERVED = "getReserved";
	public static final String API_QUERY_CUSTOMER = "customer";
	//post
	public static final String API_RESERVE = "reserve";
	
	private static RPCHelper sHelper;
	private static DefaultHttpClient sHttpClient;
	private Set<WeakReference<RPCListener>> mListener;
	private Handler mWorker;
	private HandlerThread mThread;
	
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
	
	public void call() {
		try {
			new AuthenticatedRequestTask().execute(String.format("%srpc?action=getReserved&arg0=%s", HOST, URLEncoder.encode("\"03/13/2014\"", "UTF-8")));
			
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
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
//				if(obj instanceof String) {
//					String p = URLEncoder.encode((String)obj, "UTF-8");
//					jAry.put(p);
//				} else {
//					jAry.put(obj);
//				}
				jAry.put(obj);
			}
			StringEntity e = new StringEntity(jAry.toString());
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
	                if(cookie.getName().equals("ACSID") || cookie.getName().equals("SACSID")) {
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
	
	private class AuthenticatedRequestTask extends AsyncTask<String, Integer, String> {
        @Override
        protected String doInBackground(String... urls) {
            try {
            	
        		String url = urls[0];
                HttpGet http_get = new HttpGet(url);
                HttpResponse response = sHttpClient.execute(http_get);
                String result = EntityUtils.toString(response.getEntity());
				JSONObject jObj = new JSONObject(result);
				String date = jObj.getString("date");
				JSONArray jAry = jObj.getJSONArray("result");
				for(int i = 0; i < jAry.length(); i++) {
					
				}
                return result;
            } catch (ClientProtocolException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
            return null;
        }
        
        protected void onPostExecute(String result) {
               Log.d("joey", "content=" + result);
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
}
