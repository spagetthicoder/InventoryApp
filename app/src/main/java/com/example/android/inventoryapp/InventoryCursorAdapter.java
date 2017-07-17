package com.example.android.inventoryapp;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CursorAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.example.android.inventoryapp.data.InventoryContract;
import com.example.android.inventoryapp.data.InventoryProvider;


public class InventoryCursorAdapter extends CursorAdapter {
    public InventoryCursorAdapter(Context context, Cursor c) {
        super(context, c, 0);
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        return LayoutInflater.from(context).inflate(R.layout.list_item, parent, false);
    }

    @Override
    public void bindView(View view, final Context context, Cursor cursor) {
        // Find individual views that we want to modify in the list item layout
        TextView itemNameTextView = (TextView) view.findViewById(R.id.name);
        TextView quantityTextView = (TextView) view.findViewById(R.id.quantity);

        // Find the columns of item attributes that we're interested in
        int itemNameColumnIndex = cursor.getColumnIndex(InventoryContract.InventoryEntry.COLUMN_ITEM_NAME);
        int quantityColumnIndex = cursor.getColumnIndex(InventoryContract.InventoryEntry.COLUMN_NUMBER_OF_ITEMS);

        // Read the item attributes from the Cursor for the current item
        String itemName = cursor.getString(itemNameColumnIndex);
        final String quantity = cursor.getString(quantityColumnIndex);

        // Update the TextViews with the attributes for the current item
        itemNameTextView.setText(itemName);
        quantityTextView.setText(quantity);

        Button saleButton = (Button) view.findViewById(R.id.sell_button);

        final Uri Uri = ContentUris.withAppendedId(InventoryContract.InventoryEntry.CONTENT_URI, cursor.getInt(cursor.getColumnIndexOrThrow(InventoryContract.InventoryEntry._ID)));

        final int quantityInteger = cursor.getInt(quantityColumnIndex);

        saleButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (quantityInteger > 0) {
                    int quantity = quantityInteger - 1;
                    ContentValues values = new ContentValues();
                    values.put(InventoryContract.InventoryEntry.COLUMN_NUMBER_OF_ITEMS, quantity);
                    // Update the database
                    context.getContentResolver().update(Uri, values, null, null);
                } else {
                    Toast.makeText(context, context.getString(R.string.out_of_stock), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
}

