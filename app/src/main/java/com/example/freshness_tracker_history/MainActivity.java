package com.example.freshness_tracker_history;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import android.widget.Spinner;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;

public class MainActivity extends AppCompatActivity implements FoodItemAdapter.ListItemClickListener {

    private Spinner mSpinner;
    FloatingActionButton addButton;
    ArrayList<FoodItem> foodItems;//creating one list to contain all items and remains unchanged except when DB is updated, and one list that will be updated depending on needs of view.
    ArrayList<FoodItem> displayList;
    RecyclerView recyclerViewFoodItems;
    FoodItemAdapter adapter;
    DatabaseReference databaseItems;
    private static final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "Calling onCreate method");
        setContentView(R.layout.activity_main);
        addButton = findViewById(R.id.addButton);
        mSpinner = findViewById(R.id.foodType);
        databaseItems = FirebaseDatabase.getInstance().getReference("items");
        foodItems = new ArrayList<>(); //list to store all food items (updated when database changes)
        displayList = new ArrayList<>(); //list to store items in one category only (gets updated in switchToType())
        adapter = new FoodItemAdapter(displayList, this);
        recyclerViewFoodItems = findViewById(R.id.recyclerView2);
        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false);
        recyclerViewFoodItems.setLayoutManager(mLayoutManager);
        recyclerViewFoodItems.setAdapter(adapter);
        addButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                System.out.println("Button Clicked");
                switchToAddItem();
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        databaseItems.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                foodItems.clear();
                displayList.clear();
                String sortSelection = mSpinner.getSelectedItem().toString();

                for(DataSnapshot itemsSnapshot : dataSnapshot.getChildren()) {
                    foodItems.add(itemsSnapshot.getValue(FoodItem.class));
                    displayList.add(itemsSnapshot.getValue(FoodItem.class));
                }
                if (!(foodItems.size() == 0)) {
                    sortByExpiry(foodItems);
                    sortByExpiry(displayList);
                    checkIfExpired(foodItems);
                    checkIfExpired(displayList);
                    displayByType(sortSelection);
                    adapter.notifyDataSetChanged();
                }
            }
            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        });

        mSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                String sortSelection = mSpinner.getSelectedItem().toString();
                displayByType(sortSelection);
            }
            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
            }
        });
    }

    @Override
    public void onListItemClick(int position) {
        FoodItem foodItem = displayList.get(position);
        Log.d("Click", foodItem.getName());
        showUpdateDeleteDialog(foodItem);
    }

    private void showUpdateDeleteDialog(FoodItem foodItem) {
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        final View dialogView = inflater.inflate(R.layout.update_dialogue, null);
        dialogBuilder.setView(dialogView);
        final Button buttonUpdate = (Button) dialogView.findViewById(R.id.buttonUpdateItem);
        final Button buttonDelete = (Button) dialogView.findViewById(R.id.buttonDeleteItem);
        dialogBuilder.setTitle(foodItem.getName());
        final AlertDialog dialog = dialogBuilder.create();
        dialog.show();
        buttonUpdate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                switchToEditItem(foodItem);
            }
        });
        buttonDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (foodItem.getQuantity() == 1) {
                    deleteItem(foodItem.getItemId());
                } else {
                    foodItem.setQuantity(foodItem.getQuantity() - 1);
                }
                dialog.dismiss();
            }
        });
    }

    private void switchToAddItem() {
        Intent switchToAddItemIntent = new Intent(this, AddItemActivity.class);
        startActivity(switchToAddItemIntent);
    }

    private void switchToEditItem(FoodItem foodItem) {
        Intent switchToEditItemIntent = new Intent(this, EditItemActivity.class);//creating an intent
        //adding item data intent
        switchToEditItemIntent.putExtra("itemId", foodItem.getItemId());
        switchToEditItemIntent.putExtra("name", foodItem.getName());
        switchToEditItemIntent.putExtra("day", foodItem.getDay());
        switchToEditItemIntent.putExtra("month", foodItem.getMonth());
        switchToEditItemIntent.putExtra("year", foodItem.getYear());
        switchToEditItemIntent.putExtra("category", foodItem.getFoodType());
        switchToEditItemIntent.putExtra("quantity", foodItem.getQuantity());
        startActivity(switchToEditItemIntent);//starting the edit activity with intent
    }

    private boolean deleteItem(String id) {
        DatabaseReference dR = FirebaseDatabase.getInstance().getReference("items").child(id);//getting the specified item reference
        dR.removeValue();//removing item
        Toast.makeText(this, "Item Deleted", Toast.LENGTH_LONG).show();
        return true;
    }

    public void sortByExpiry(ArrayList<FoodItem> foodItems) {
        if (foodItems.size() != 0) {
            Collections.sort(foodItems, new Comparator<FoodItem>() {
                public int compare(FoodItem o1, FoodItem o2) {
                    Calendar date1 = Calendar.getInstance();
                    date1.set(o1.year, o1.month, o1.day);
                    Calendar date2 = Calendar.getInstance();
                    date2.set(o2.year, o2.month, o2.day);
                    return date1.compareTo(date2);
                }
            });
        }
    }

    private void checkIfExpired(ArrayList<FoodItem> foodItems) {
        for (FoodItem foodItem : foodItems) {
            Calendar expirationDate = Calendar.getInstance();//generate expiration date from item's day/month/year
            expirationDate.set(foodItem.getYear(), foodItem.getMonth(), foodItem.getDay(), 0, 0, 0);
            Calendar currentDate = Calendar.getInstance();//get current date, and set time to 0
            currentDate.set(Calendar.HOUR_OF_DAY, 0);
            currentDate.set(Calendar.MINUTE, 0);
            currentDate.set(Calendar.SECOND, 0);
            currentDate.set(Calendar.MILLISECOND, 0);
            foodItem.setIsExpired(expirationDate.before(currentDate));//check if expiration date is before current date, and set isExpired to the result (true or false)
        }
    }

    private void displayByType(String sortSelection) {
        displayList.clear();
        if (foodItems.size() != 0) {
            if (sortSelection.equals("All")) {//if selection is All, add all items to display list and update adapter
                for (FoodItem foodItem : foodItems) {
                    displayList.add(foodItem);
                }
            } else {
                for (FoodItem foodItem : foodItems) {
                    if (foodItem.getFoodType().equals(sortSelection)) {
                        displayList.add(foodItem);
                    }
                }
            }
            adapter.notifyDataSetChanged();
        }
    }
}