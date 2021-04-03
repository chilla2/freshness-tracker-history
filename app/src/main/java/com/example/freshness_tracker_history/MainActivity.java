package com.example.freshness_tracker_history;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.content.Context;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import android.widget.DatePicker;

public class MainActivity extends AppCompatActivity {
    EditText editTextName;
    DatePicker picker;
    Spinner spinnerCategory;
    ListView listViewItems;
    Button buttonAddItem;
    //a list to store all the artist from firebase database
    List<FoodItem> foodItems;
    //our database reference object
    DatabaseReference databaseItems;
    private static final String TAG = "MainActivity";
    private FirebaseDatabase foodListDB;
    private DatabaseReference foodListDBReference;
    ArrayList<FoodItem> foodItemsList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        databaseItems = FirebaseDatabase.getInstance().getReference("items");
        //getting views
        editTextName = (EditText) findViewById(R.id.editTextName);
        picker=(DatePicker)findViewById(R.id.datePicker);
        spinnerCategory = (Spinner) findViewById(R.id.categories_spinner);
        listViewItems = (ListView) findViewById(R.id.listViewItems);
        buttonAddItem = (Button) findViewById(R.id.buttonAddItem);
        foodItems = new ArrayList<>();//list to store food items

        buttonAddItem.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                addItem();
            }
        });
        listViewItems.setOnItemClickListener(new AdapterView.OnItemClickListener() {//attaching listener to listview
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                FoodItem foodItem = foodItems.get(i);//getting the selected item
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
    private boolean updateItem(String id, int day, int month, int year, String name, String category) {
        DatabaseReference dR = FirebaseDatabase.getInstance().getReference("items").child(id);//getting the specified item reference
        FoodItem foodItem = new FoodItem(id, day, month, year, name, category);//updating item
        dR.setValue(foodItem);
        Toast.makeText(getApplicationContext(), "Item Updated", Toast.LENGTH_LONG).show();
        return true;
    }
    private boolean deleteItem(String id) {
        DatabaseReference dR = FirebaseDatabase.getInstance().getReference("items").child(id);//getting the specified item reference
        dR.removeValue();//removing item
        Toast.makeText(getApplicationContext(), "Item Deleted", Toast.LENGTH_LONG).show();
        return true;
    }
    @Override
    protected void onStart() {
        super.onStart();
        databaseItems.addValueEventListener(new ValueEventListener() { //attaching value event listener
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                foodItems.clear();//clearing the previous items list
                for (DataSnapshot postSnapshot : dataSnapshot.getChildren()) {//iterating through all the nodes
                    FoodItem foodItem = postSnapshot.getValue(FoodItem.class);//getting item
                    foodItems.add(foodItem);//adding item to the list
                }
                FoodItemsList artistAdapter = new FoodItemsList(MainActivity.this, foodItems);//creating adapter
                listViewItems.setAdapter(artistAdapter);//attaching adapter to the listview
            }
            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        });
    }
    /*
     * This method is saving a new item to the
     * Firebase Realtime Database
     * */
    private void addItem() {
        //getting the values to save
        String name = editTextName.getText().toString().trim();
        int day = picker.getDayOfMonth();
        int month = picker.getMonth();
        int year = picker.getYear();
        String category = spinnerCategory.getSelectedItem().toString();
        if (!TextUtils.isEmpty(name)) { //checking if the value is provided
            String id = databaseItems.push().getKey();//getting a unique id using push().getKey() method
            FoodItem foodItem = new FoodItem(id, day, month, year, name, category);//creating an item Object
            databaseItems.child(id).setValue(foodItem); //Saving the item
            editTextName.setText(""); //setting edittext to blank again
            Toast.makeText(this, "Item added", Toast.LENGTH_LONG).show();//displaying a success toast
        } else {
            Toast.makeText(this, "Please enter a name", Toast.LENGTH_LONG).show();//if the value is not given displaying a toast
        }
    }
}