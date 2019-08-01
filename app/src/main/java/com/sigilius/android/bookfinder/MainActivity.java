package com.sigilius.android.bookfinder;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.StrictMode;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    /**
     * Tag for the log messages
     */
    public static final String LOG_TAG = MainActivity.class.getSimpleName();
    private static final int MAX_RESULTS = 10;
    private static String mSearchQuery = "";
    private static String GOOGLE_REQUEST_URL = "";
    private static boolean isKeyBoardOpen = false;

    private EditText bookSearch;
    private Button searchButton;
    private TextView instruction;

    private ListView mListView;
    private ArrayList<Book> books;
    private BookAdapter mBookAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        books = new ArrayList<>();

        if (savedInstanceState != null) {
            assert savedInstanceState != null;
            books = savedInstanceState.getParcelableArrayList("key");
        }

        if (savedInstanceState != null && (savedInstanceState.getBoolean("isKeyBoardOpen", false))) {
            this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
        } else {
            this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
        }

        setContentView(R.layout.activity_main);

        // Prepare View
        mBookAdapter = new BookAdapter(MainActivity.this, books);
        mListView = (ListView) findViewById(R.id.list);

        assert mListView != null;
        mListView.setAdapter(mBookAdapter);
        bookSearch = (EditText) findViewById(R.id.searchInput);
        searchButton = (Button) findViewById(R.id.searchButton);
//        instruction = (TextView) findViewById(R.id.instruction);
        mListView.setEmptyView(findViewById(R.id.instruction));

        // Allows images loading within async thread
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);


        searchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //clear screen on search
                assert mListView != null;
                mListView.setAdapter(null);


                mSearchQuery = bookSearch.getText().toString();

                if (mSearchQuery.isEmpty()) {
                    Toast toast = Toast.makeText(MainActivity.this, "Please enter a search term", Toast.LENGTH_SHORT);
                    toast.setGravity(Gravity.CENTER, 0, 0);
                    toast.show();
                    return;
                }

                if (!isOnline()) {
                    Toast toast = Toast.makeText(MainActivity.this, "The device is not connected to the Internet", Toast.LENGTH_SHORT);
                    toast.setGravity(Gravity.CENTER, 0, 0);
                    toast.show();
                    return;
                }

                // format URL query string
                URI uri = null;
                try {
                    uri = new URI(
                            "https",
                            "www.googleapis.com",
                            "/books/v1/volumes",
                            "q=" + mSearchQuery + "&maxResults=" + MAX_RESULTS,
                            null);
                } catch (URISyntaxException e) {
                    e.printStackTrace();
                }

                GOOGLE_REQUEST_URL = uri.toString();

                // Kick off an {@link AsyncTask} to perform the network request
                BookAsyncTask task = new BookAsyncTask();
                task.execute();
            }
        });
    }

    /**
     * Save List books and keyboard view state
     * @param outState
     */
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putParcelableArrayList("key", books);
        outState.putBoolean("isKeyBoardOpen", isKeyBoardOpen);
        super.onSaveInstanceState(outState);
    }

    private boolean isOnline() {
        // Internet connectivity?
        ConnectivityManager cm =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        boolean isConnected = activeNetwork != null &&
                activeNetwork.isConnectedOrConnecting();
        return isConnected;
    }



    /**
     * {@link AsyncTask} to perform the network request on a background thread, and then
     * update the UI with the  books in the response.
     */
    private class BookAsyncTask extends AsyncTask<URL, Void, List<Book>> {

        public BookAsyncTask() {
        }

        @Override
        protected List<Book> doInBackground(URL... urls) {

            // Create URL object
            URL url = createUrl(GOOGLE_REQUEST_URL);

            // Perform HTTP request to the URL and receive a JSON response back
            String jsonResponse = "";
            try {
                jsonResponse = makeHttpRequest(url);
            } catch (IOException e) {
                Log.e(LOG_TAG, "Problem making the HTTP request.", e);
            }

            // Extract relevant fields from the JSON response and create an {@link ArrayList} object
            // Return the {@link ArrayList} object as the result of the {@link BookAsyncTask}
            return extractBooks(jsonResponse);
        }

        /**
         * Update the screen to display information from the given {@link Book}.
         */
        private void updateUi(List<Book> bookList) {
            assert mListView != null;
            mListView.setAdapter(mBookAdapter);
            mBookAdapter.addAll(bookList);
        }

        /**
         * Update the screen with the given earthquake (which was the result of the
         * {@link BookAsyncTask}).
         */
        @Override
        protected void onPostExecute(List<Book> list) {
            if (list == null) {
                return;
            }
            mBookAdapter.clear();
            updateUi(list);
        }

        /**
         * Returns new URL object from the given string URL.
         */
        private URL createUrl(String stringUrl) {
            URL url = null;
            try {
                url = new URL(stringUrl);
            } catch (MalformedURLException exception) {
                Log.e(LOG_TAG, "Error with creating URL", exception);
                return null;
            }
            return url;
        }

        /**
         * Make an HTTP request to the given URL and return a String as the response.
         */
        private String makeHttpRequest(URL url) throws IOException {
            String jsonResponse = "";
            HttpURLConnection urlConnection = null;
            InputStream inputStream = null;
            try {
                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.setReadTimeout(10000 /* milliseconds */);
                urlConnection.setConnectTimeout(15000 /* milliseconds */);
                urlConnection.connect();
                if (urlConnection.getResponseCode() == 200) {
                    inputStream = urlConnection.getInputStream();
                    jsonResponse = readFromStream(inputStream);
                } else {
                    Log.e(LOG_TAG, "Error response code: " + urlConnection.getResponseCode());
                }
            } catch (IOException e) {
                Log.e(LOG_TAG, "Problem retrieving the earthquake JSON results.", e);
            } finally {
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
                if (inputStream != null) {
                    inputStream.close();
                }
            }
            return jsonResponse;
        }

        /**
         * Convert the {@link InputStream} into a String which contains the
         * whole JSON response from the server.
         */
        private String readFromStream(InputStream inputStream) throws IOException {
            StringBuilder output = new StringBuilder();
            if (inputStream != null) {
                InputStreamReader inputStreamReader = new InputStreamReader(inputStream, Charset.forName("UTF-8"));
                BufferedReader reader = new BufferedReader(inputStreamReader);
                String line = reader.readLine();
                while (line != null) {
                    output.append(line);
                    line = reader.readLine();
                }
            }
            return output.toString();
        }

        /**
         * Return a list of {@link Book} objects that has been built up from
         * parsing a JSON response.
         */
        public List<Book> extractBooks(String bookJSON) {

            List<Book> bookList = new ArrayList<>();
            String author = "";
            String isbn13 = "";
            String isbn10 = "";

            try {
                JSONObject baseJsonResponse = new JSONObject(bookJSON);
                JSONArray itemsArray = baseJsonResponse.getJSONArray("items");


                for (int i = 0; i < itemsArray.length(); i++) {
                    JSONObject values = itemsArray.getJSONObject(i);
                    JSONObject volumeInfo = values.getJSONObject("volumeInfo");

                    // Extract out the title, time, and tsunami values
                    String title = volumeInfo.getString("title");

                    if (volumeInfo.has("authors")) {
                        JSONArray authors = volumeInfo.getJSONArray("authors");
                        author = authors.get(0).toString();
                    }

                    if (volumeInfo.has("industryIdentifiers")) {
                        JSONArray industryIds = volumeInfo.getJSONArray("industryIdentifiers");

                        JSONObject isbn13Object = industryIds.getJSONObject(0);
                        JSONObject isbn10Object = industryIds.getJSONObject(1);
                        if (isbn13Object != null) {
                            isbn13 = isbn13Object.getString("identifier");
                        }

                        if (isbn10Object != null) {
                            isbn10 = isbn10Object.getString("identifier");
                        }
                    }
                    JSONObject imageLinks = volumeInfo.getJSONObject("imageLinks");
                    String imageLink = imageLinks.getString("smallThumbnail");

                    bookList.add(new Book(title, author, isbn13, isbn10, imageLink));
                }
            } catch (JSONException e) {
                Log.e(LOG_TAG, "Problem parsing the book JSON results", e);
            }
            return bookList;
        }
    }
}
