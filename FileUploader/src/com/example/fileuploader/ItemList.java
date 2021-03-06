package com.example.fileuploader;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.util.HashMap;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.BufferedHttpEntity;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

@SuppressWarnings("deprecation")
public class ItemList extends Activity {
	final int CREATE_ITEM = 100;
	final int LOGIN = 101;
	final String TAG = "fileUploader";
	public MNItemAdapter adapter;
	public Context context;
	MNItem globalIitemArray[];
	
	String mUsername = "";
	String mPassword = "";
	

	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.item_list);
		context = this; // reikalingas onPostExecute -.-
		new ItemDownloader().execute();
		ListView lw = (ListView) findViewById(R.id.listView1);
		lw.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				TextView tw = (TextView) view.findViewById(R.id.invisibleLine);
				Toast.makeText(getApplicationContext(),
						"Pasirinkto item ID: " + tw.getText().toString(),
						Toast.LENGTH_LONG).show();
				
				Intent i = new Intent(ItemList.this, ItemView.class);
				i.putExtra("itemName", globalIitemArray[position].name);
				i.putExtra("itemCategory", globalIitemArray[position].category);
				i.putExtra("itemDescription", globalIitemArray[position].description);
				i.putExtra("itemAddress", globalIitemArray[position].address);
				i.putExtra("itemImage", globalIitemArray[position].image);
				startActivity(i);
			}

		});
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle presses on the action bar items
		switch (item.getItemId()) {
		case R.id.action_newItem:
			Intent newItemIntent = new Intent(ItemList.this, CreateItemActivity.class);
			newItemIntent.putExtra("username", mUsername);
			newItemIntent.putExtra("password", mPassword);
			startActivityForResult(newItemIntent, CREATE_ITEM);
			return true;
		case R.id.action_refresh_list:
			new ItemDownloader().execute();
			return true;
		case R.id.login:
			Intent loginIntent = new Intent(ItemList.this, LoginActivity.class);
			startActivityForResult(loginIntent, LOGIN);
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		// check if the request code is same as what is passed here it is 2
		if (requestCode == CREATE_ITEM && resultCode == RESULT_OK) {

			Toast.makeText(
					getApplicationContext(),
					"If you are seeing this,"
							+ " then the Picture was most probably uploaded.",
					Toast.LENGTH_LONG).show();
		}
		if (requestCode == LOGIN && resultCode == RESULT_OK) {
			mUsername = data.getStringExtra("username");
			mPassword = data.getStringExtra("username");
		}
		
	}

	private class ItemDownloader extends AsyncTask<String, Void, String> {

		MNItem itemArray[]; // sukuriam MNItem tipo objektu masyva
		public JSONArray jArray;
		Bitmap thumbnail;
		HashMap<String, String> mCategories = new HashMap<String, String>();

		@Override
		protected String doInBackground(String... params) {
			String postUrl = "http://www.mannereikia.lt/index.php/item/androiditemrequest";
			final String ASYNC_TASK_OK = "1";
			
			mCategories.put("1", "Dviračiai");
			mCategories.put("2", "Televizoriai");
			mCategories.put("3", "Kompiuteriai");
			mCategories.put("4", "Drabužiai");

			try {
				HttpClient httpClient = new DefaultHttpClient();
				HttpPost postRequest = new HttpPost(postUrl);

				MultipartEntity reqEntity = new MultipartEntity(
						HttpMultipartMode.STRICT);
				reqEntity.addPart("listrequest", new StringBody("listRequest",
						Charset.forName("UTF-8")));
				// reqEntity.addPart("category", new
				// StringBody(null,Charset.forName("UTF-8")));

				postRequest.setEntity(reqEntity);
				HttpResponse response = httpClient.execute(postRequest);
				BufferedReader reader = new BufferedReader(
						new InputStreamReader(
								response.getEntity().getContent(), "UTF-8"));
				String sResponse;
				StringBuilder s = new StringBuilder();
				while ((sResponse = reader.readLine()) != null) {
					s = s.append(sResponse);
				}
				String tempStr = s.toString();
				try {
					JSONObject jObject = new JSONObject(tempStr);
					jArray = jObject.getJSONArray("items");// pasiimam JSON
															// response vardu
															// "items".
															// Pasidarom is jo
															// JSON array

					itemArray = new MNItem[jArray.length()];// inicializuojam
															// objektu masyva
															// pagal JSON
															// elementu kieki

					for (int i = 0; i < jArray.length(); i++) {
						JSONObject e = jArray.getJSONObject(i);
						String itemString = e.getString("item"); // pasiimam jo
																	// elementa
																	// vardu
																	// "item"
						JSONObject jItem = new JSONObject(itemString); // susikuriam
																		// JSON
																		// objekta
						try{
							GetBitmap bitmapGetter = new GetBitmap();
							thumbnail = Bitmap.createScaledBitmap(bitmapGetter.getBitmapFromURL("http://www.mannereikia.lt/images/"+jItem.getString("imageLink")), 110, 150, false);
//							thumbnail = null;
						}
					    catch (NullPointerException inpe) {
					    	InputStream is = getResources().openRawResource(R.drawable.noimage);
							thumbnail = Bitmap.createScaledBitmap(BitmapFactory.decodeStream(is), 110, 150, false);
					    }
						itemArray[i] = new MNItem(jItem.getString("id"),
								jItem.getString("name"),
								mCategories.get(jItem.getString("category")), jItem.getString("description"), jItem.getString("address"), jItem.getString("imageLink"), thumbnail); // uzpildom Item
																// objekta JSON
																// masyvo
																// informacija
					}
				} catch (JSONException e) {
					Log.e(TAG, "JSONObject klaida");
				}
				Log.v(TAG, "Response: " + s);
				httpClient.getConnectionManager().shutdown();
			} catch (FileNotFoundException e) {
				Log.e(TAG, "Nurodytu PATH nuotraukos nera.");
			} catch (UnsupportedEncodingException e) {
				Log.e(TAG,
						"StringBody elementai naudoja netinkamos koduotes teksta.");
			} catch (ClientProtocolException e) {
				Log.e(TAG, "Interneto klaida");
				Toast.makeText(getApplicationContext(), "Interneto klaida",
						Toast.LENGTH_LONG).show();
			} catch (IOException e) {
				Log.e(TAG, "IO klaida");
			} catch (NullPointerException e) {
				Log.e(TAG, "StringBody null pointer klaida");
			}
			return ASYNC_TASK_OK;
		}

		@Override
		protected void onCancelled() {
			// ProgressBar pb = (ProgressBar) findViewById(R.id.progressBar1);
			// pb.setVisibility(View.INVISIBLE);
			// Toast.makeText(getApplicationContext(),
			// "Nepasirinkta nuotrauka arba pasirinkta nuotrauka yra neatidaroma.",
			// Toast.LENGTH_LONG).show();
		}

		@Override
		protected void onPostExecute(String result) {

			// ProgressBar pb = (ProgressBar) findViewById(R.id.progressBar1);
			// pb.setVisibility(View.INVISIBLE);
			adapter = new MNItemAdapter(context, R.layout.list_adapter,
					itemArray); // susikuriam MNAdapter tipo adapteri, kuriam
								// priskiriam susikurta ITEM objektu masyva
			ListView lw = (ListView) findViewById(R.id.listView1);
			lw.setAdapter(adapter);
			globalIitemArray = itemArray;
		}

		@Override
		protected void onPreExecute() {
			// ProgressBar pb = (ProgressBar) findViewById(R.id.progressBar1);
			// pb.setVisibility(View.VISIBLE);
		}
		
		
	}
}