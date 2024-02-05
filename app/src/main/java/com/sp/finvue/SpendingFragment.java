    package com.sp.finvue;

    import android.content.Intent;
    import android.os.Bundle;

    import androidx.fragment.app.Fragment;
    import androidx.recyclerview.widget.LinearLayoutManager;
    import androidx.recyclerview.widget.RecyclerView;

    import android.util.Log;
    import android.view.LayoutInflater;
    import android.view.View;
    import android.view.ViewGroup;
    import android.widget.TextView;

    import com.google.android.material.floatingactionbutton.FloatingActionButton;

    import java.util.ArrayList;
    import java.util.List;
    import java.util.Map;

    import com.android.volley.NetworkResponse;
    import com.android.volley.Request;
    import com.android.volley.RequestQueue;
    import com.android.volley.Response;
    import com.android.volley.VolleyError;
    import com.android.volley.toolbox.JsonObjectRequest;
    import com.android.volley.toolbox.Volley;
    import com.google.firebase.auth.FirebaseAuth;
    import com.google.firebase.auth.FirebaseUser;
    import com.google.firebase.firestore.DocumentReference;
    import com.google.firebase.firestore.FirebaseFirestore;

    import org.json.JSONArray;
    import org.json.JSONException;
    import org.json.JSONObject;

    public class SpendingFragment extends Fragment {

        private RecyclerView recyclerView;
        private TransactionAdapter adapter;
        private int volleyResponseStatus;
        private View empty;
        private TextView emptyView; // Add this line

        private FirebaseFirestore fStore;
        private FirebaseAuth mAuth;
        private String fbuserUUID, fbuserID;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);


            mAuth = FirebaseAuth.getInstance();
            fStore = FirebaseFirestore.getInstance();

            // Get current user
            FirebaseUser user = mAuth.getCurrentUser();
            if (user != null) {
                fbuserID = user.getUid();
                // Fetch data from Firestore
                fetchUserData();
            }
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            // Inflate the layout for this fragment
            View rootView = inflater.inflate(R.layout.fragment_spending, container, false);

            // Find FAB
            FloatingActionButton fab = rootView.findViewById(R.id.newTransactionFAB);

            // Set click listener for FAB
            fab.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // Show the dialog when FAB is clicked
                    Intent intent = new Intent(getActivity(), NewTransaction.class);
                    startActivity(intent);
                }
            });

            // Initialise RecyclerView
            recyclerView = rootView.findViewById(R.id.recyclerView);
            recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));

            // Initialise emptyView
            emptyView = rootView.findViewById(R.id.emptyView);

            // Initialise and set the adapter
            adapter = new TransactionAdapter(getActivity(), new ArrayList<>());
            recyclerView.setAdapter(adapter);

            // Update visibility of emptyView based on the data
            updateEmptyViewVisibility();

            // Initialise VolleyResponse and empty
            volleyResponseStatus = 0;

            getAllUserTransactions();

            return rootView;

        }

        private void fetchUserData() {
            DocumentReference documentReference = fStore.collection("users").document(fbuserID);
            documentReference.get().addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    fbuserUUID = task.getResult().getString("useruuid");
                }
            });
        }

        // Read all user transactions from Astra Database
        private void getAllUserTransactions() {
            String url = TransactionVolleyHelper.transaction_url + fbuserUUID;
            RequestQueue queue = Volley.newRequestQueue(requireContext()); // Make sure to use the correct context

            JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, url, null,
                    new Response.Listener<JSONObject>() {
                        @Override
                        public void onResponse(JSONObject response) {
                            if (volleyResponseStatus == 200) { // Read successfully from database
                                try {
                                    int count = response.getInt("count"); // Number of records from database
                                    Log.d("RecordCount", "Number of records: " + count);
                                    adapter.clear(); // Reset adapter
                                    if (count > 0) { // Has more than 1 record
                                        emptyView.setVisibility(View.INVISIBLE);
                                        JSONArray data = response.getJSONArray("data");// Get all the records as a JSON array
                                        Log.d("data array", String.valueOf(data));
                                        List<Transaction> transactions = new ArrayList<>();

                                        for (int i = 0; i < count; i++) { // Loop through all records
                                            Transaction transaction = new Transaction(
                                                    data.getJSONObject(i).getString("user_id"),
                                                    data.getJSONObject(i).getString("transaction_id"),
                                                    data.getJSONObject(i).getString("category"),
                                                    data.getJSONObject(i).getDouble("cost"),
                                                    data.getJSONObject(i).getString("date"),
                                                    data.getJSONObject(i).getString("location"),
                                                    data.getJSONObject(i).getString("mop"),
                                                    data.getJSONObject(i).getString("name"),
                                                    data.getJSONObject(i).getString("remarks"),
                                                    data.getJSONObject(i).getString("submission_time")
                                            );
                                            transaction.setId(data.getJSONObject(i).getString("transaction_id"));
                                            transactions.add(transaction); // Add the record to the adapter
                                        }

                                        adapter.notifyDataSetChanged();

                                    } else {
                                        emptyView.setVisibility(View.VISIBLE);
                                    }
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    },
                    new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            // TODO: Handle error
                            Log.e("OnErrorResponse", error.toString());
                        }
                    }) {
                @Override
                public Map<String, String> getHeaders() {
                    return TransactionVolleyHelper.getHeader();
                }

                @Override
                protected Response<JSONObject> parseNetworkResponse(NetworkResponse response) {
                    volleyResponseStatus = response.statusCode;
                    return super.parseNetworkResponse(response);
                }
            };

            // Add JsonObjectRequest to the RequestQueue
            queue.add(jsonObjectRequest);
        }

        private void updateEmptyViewVisibility() {
            if (adapter.getItemCount() == 0) {
                emptyView.setVisibility(View.VISIBLE);
            } else {
                emptyView.setVisibility(View.GONE);
            }
        }



    }