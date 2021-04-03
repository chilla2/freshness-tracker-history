package com.example.freshness_tracker_history;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import android.widget.DatePicker;

public class MainActivity extends AppCompatActivity {
    EditText editItemName;
    DatePicker picker;
    Spinner spinnerCategory;
    ListView listViewItems;
    List<FoodItem> foodItems;//a list to store all the artist from firebase database
    FloatingActionButton addButton;
    DatabaseReference databaseItems;//our database reference object
    private static final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        databaseItems = FirebaseDatabase.getInstance().getReference("items");//getting the reference of items node
        addButton = (FloatingActionButton) findViewById(R.id.addButton);
        listViewItems = (ListView) findViewById(R.id.listViewItems);
        foodItems = new ArrayList<>();//list to store food items
        addButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                System.out.println("Button Clicked");
                Log.d(TAG, "Switching to add item activity");
                switchToAddItem();
            }
        });
        listViewItems.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                FoodItem foodItem = foodItems.get(i); //getting the selected item
            }
        });
        listViewItems.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> adapterView, View view, int i, long l) {
                FoodItem foodItem = foodItems.get(i);
                showUpdateDeleteDialog(foodItem.getItemId(), foodItem.getName());
                return true;
            }
        });
    }

    private void showUpdateDeleteDialog(final String itemId, String itemName) {
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        final View dialogView = inflater.inflate(R.layout.update_dialogue, null);
        dialogBuilder.setView(dialogView);
        final EditText editTextName = (EditText) dialogView.findViewById(R.id.editTextName);
        final Spinner spinnerCategory = (Spinner) dialogView.findViewById(R.id.categories_spinner);
        final Button buttonUpdate = (Button) dialogView.findViewById(R.id.buttonUpdateItem);
        final Button buttonDelete = (Button) dialogView.findViewById(R.id.buttonDeleteItem);
        dialogBuilder.setTitle(itemName);
        final AlertDialog b = dialogBuilder.create();
        b.show();
        buttonUpdate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String name = editTextName.getText().toString().trim();
                String category = spinnerCategory.getSelectedItem().toString();
                int day = picker.getDayOfMonth();
                int month = picker.getMonth();
                int year = picker.getYear();
                if (!TextUtils.isEmpty(name)) {
                    updateItem(itemId, day, month, year, name, category);
                    b.dismiss();
                }
            }
        });
        buttonDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                deleteItem(itemId);
                b.dismiss();
            }
        });
    }

    private void switchToAddItem() {
        Intent switchToAddItemIntent = new Intent(this, AddItemActivity.class);
        startActivity(switchToAddItemIntent);
    }

    private boolean updateItem(String id, int day, int month, int year, String name, String category) {
        DatabaseReference dR = FirebaseDatabase.getInstance().getReference("items").child(id);//getting the specified item reference
        FoodItem foodItem = new FoodItem(id, day, month, year, name, category); //updating item
        dR.setValue(foodItem);
        Toast.makeText(getApplicationContext(), "Item Updated", Toast.LENGTH_LONG).show();
        return true;
    }

    private boolean deleteItem(String id) {
        DatabaseReference dR = FirebaseDatabase.getInstance().getReference("items").child(id);//getting the specified item reference
        dR.removeValue(); //removing item
        Toast.makeText(getApplicationContext(), "Item Deleted", Toast.LENGTH_LONG).show();
        return true;
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "Calling onStart method");
        databaseItems.addValueEventListener(new ValueEventListener() {//attaching value event listener
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Log.d(TAG, "Calling onDataChange method");
                List<FoodItem> workingList = loopThroughDBAndAddToList(dataSnapshot);//Call function to iterate through DB nodes and add items to list
                if (!(workingList.size() == 0)) {
                    Log.d(TAG, "Working list is not empty");
                } else {
                    Log.d(TAG, "Working list is empty");
                }
                FoodItemsList itemAdapter = new FoodItemsList(MainActivity.this, workingList);//creating adapter
                Log.d(TAG, "Attaching adapter to listViewItems");
                listViewItems.setAdapter(itemAdapter);
            }
            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        });
    }

    public List<FoodItem> loopThroughDBAndAddToList(DataSnapshot dataSnapshot) {//This method is called in the onDataChange method (in onStart)
        foodItems.clear();
        for (DataSnapshot postSnapshot : dataSnapshot.getChildren()) {//iterating through all the nodes
            FoodItem foodItem = postSnapshot.getValue(FoodItem.class);//getting item
            Log.d(TAG, "Adding item to list");//adding item to the list
            foodItems.add(foodItem);
        }
        return foodItems;
    }

    /*
     * This method is saving a new item to the
     * Firebase Realtime Database
     * */
    private void addItem(String name, int day, int month, int year, String category) {
        String id = databaseItems.push().getKey();
        FoodItem foodItem = new FoodItem(id, day, month, year, name, category);//creating an item Object
        databaseItems.child(id).setValue(foodItem);//Saving the item
        Toast.makeText(this, "Item added", Toast.LENGTH_LONG).show();
    }
}