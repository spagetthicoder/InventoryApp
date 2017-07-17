package com.example.android.inventoryapp;

import android.app.Activity;
import android.app.LoaderManager;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import com.example.android.inventoryapp.data.InventoryContract;

public class EditorActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor> {

    private static final int EXISTING_ITEM_LOADER = 0;
    private static final int REQUEST_CODE = 1;
    private Uri mCurrentInventoryUri;

    private EditText mItemNameEditText;
    private EditText mSupplierEditText;
    private EditText mNumberOfItemsEditText;
    private EditText mPricePerItemEditText;

    private Bitmap bitmap;

    String itemName;
    String supplier;
    Integer numberOfItems = 0;
    Integer pricePerItem;
    String image;

    /**
     * Boolean flag that keeps track of whether the item has been edited (true) or not (false)
     */
    private boolean mItemHasChanged = false;
    private ImageView mImageView;
    private Uri mCurrentImageUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_editor);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Get a support ActionBar corresponding to this toolbar
        ActionBar ab = getSupportActionBar();
        // Enable the Up button
        ab.setDisplayHomeAsUpEnabled(true);

        // Examine the intent that was used to launch this activity,
        // in order to figure out if we're creating a new or editing an existing one
        Intent intent = getIntent();
        mCurrentInventoryUri = intent.getData();

        // If the intent DOES NOT contain a item content URI, then we know, then we know that we are
        // creating a new item.
        if (mCurrentInventoryUri == null) {
            setTitle("Add an Item");

            // Invalidate the options menu, so the "Delete" menu option can be hidden.
            // (It doesn't make sense to delete a item that hasn't been created yet.)
            invalidateOptionsMenu();
        } else {
            setTitle("Edit an Item");

            // Initialize a loader to read the item data from the database
            // and display the current values in the editor
            getLoaderManager().initLoader(EXISTING_ITEM_LOADER, null, this);
        }

        mItemNameEditText = (EditText) findViewById(R.id.edit_item_name);
        mNumberOfItemsEditText = (EditText) findViewById(R.id.edit_number_of_items);
        mPricePerItemEditText = (EditText) findViewById(R.id.edit_price);
        mSupplierEditText = (EditText) findViewById(R.id.supllier_name);
        mImageView = (ImageView) findViewById(R.id.image_view);

        mItemNameEditText.setOnTouchListener(mTouchListener);
        mNumberOfItemsEditText.setOnTouchListener(mTouchListener);
        mPricePerItemEditText.setOnTouchListener(mTouchListener);
        mSupplierEditText.setOnTouchListener(mTouchListener);

        Button saveItem = (Button) findViewById(R.id.save_button);
        Button deleteButton = (Button) findViewById(R.id.delete_button);
        Button orderButton = (Button) findViewById(R.id.order_button);
        Button minButton = (Button) findViewById(R.id.min_button);
        minButton.setOnTouchListener(mTouchListener);
        Button plusButton = (Button) findViewById(R.id.plus_button);
        plusButton.setOnTouchListener(mTouchListener);
        final Button uploadImage = (Button) findViewById(R.id.upload_image);
        uploadImage.setOnTouchListener(mTouchListener);

        saveItem.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveItem();
            }
        });

        deleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDeleteConfirmationDialog();
            }
        });

        orderButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                orderItem();
            }
        });

        minButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                minButton();
            }
        });

        plusButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                plusButton();
            }
        });

        uploadImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                uploadImage();
            }
        });
    }


    @Override
    public void onBackPressed() {
        // If the item hasn't changed, continue with navigating up to parent activity
        // which is the {@link MainActivity}.
        if (!mItemHasChanged) {
            NavUtils.navigateUpFromSameTask(EditorActivity.this);
        }

        // Otherwise if there are unsaved changes, setup a dialog to warn the user.
        // Create a click listener to handle the user confirming that
        // changes should be discarded.
        DialogInterface.OnClickListener discardButtonClickListener =
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        // User clicked "Discard" button, navigate to parent activity.
                        NavUtils.navigateUpFromSameTask(EditorActivity.this);
                    }
                };

        // Show a dialog that notifies the user they have unsaved changes
        showUnsavedChangesDialog(discardButtonClickListener);
    }

    private void saveItem() {
        // Read from input fields
        // Use trim to eliminate leading or trailing white space
        String itemNameString = mItemNameEditText.getText().toString().trim();
        String supplierString = mSupplierEditText.getText().toString().trim();
        String numberOfItemsString = mNumberOfItemsEditText.getText().toString();
        String pricePerItemString = mPricePerItemEditText.getText().toString().trim();
        String imageString = "";

        // Check if this is supposed to be a new item
        // and check if all the fields in the editor are blank
        if (mCurrentInventoryUri == null &&
                TextUtils.isEmpty(itemNameString) && TextUtils.isEmpty(numberOfItemsString) &&
                TextUtils.isEmpty(pricePerItemString) && TextUtils.isEmpty(supplier)) {
            // Since no fields were modified, we can return early without creating a new item.
            // No need to create ContentValues and no need to do any ContentProvider operations.
            return;
        }

        ContentValues values = new ContentValues();
        values.put(InventoryContract.InventoryEntry.COLUMN_ITEM_NAME, itemNameString);
        values.put(InventoryContract.InventoryEntry.COLUMN_SUPPLIER, supplierString);

        // If the number of items is not provided by the user, don't try to parse the string into an
        // integer value. Use 0 by default.
        int numberOfItems = 0;
        if (!TextUtils.isEmpty(numberOfItemsString)) {
            numberOfItems = Integer.parseInt(numberOfItemsString);
        }

        values.put(InventoryContract.InventoryEntry.COLUMN_NUMBER_OF_ITEMS, numberOfItems);

        int pricePerItem = 0;
        if (!TextUtils.isEmpty(pricePerItemString)) {
            pricePerItem = Integer.parseInt(pricePerItemString);
        }

        values.put(InventoryContract.InventoryEntry.COLUMN_PRICE_PER_ITEM, pricePerItem);

        if (mCurrentImageUri != null) {
            imageString = mCurrentImageUri.toString();
        }

        values.put(InventoryContract.InventoryEntry.COLUMN_IMAGE, imageString);

        // Determine if this is a new or existing item by checking if mCurrentInventoryUri is null or not
        if (mCurrentInventoryUri == null) {
            // This is a NEW item, so insert a new item into the provider,
            // returning the content URI for the new item.
            Uri newUri = getContentResolver().insert(InventoryContract.InventoryEntry.CONTENT_URI, values);

            // Show a toast message depending on whether or not the insertion was successful.
            if (newUri == null) {
                // If the new content URI is null, then there was an error with insertion.
                Toast.makeText(getBaseContext(), getString(R.string.editor_insert_item_failed),
                        Toast.LENGTH_SHORT).show();
            } else {
                // Otherwise, the insertion was successful and we can display a toast.
                Toast.makeText(getBaseContext(), getString(R.string.editor_insert_item_successful),
                        Toast.LENGTH_SHORT).show();
            }
        } else {
            // Otherwise this is an EXISTING item, so update the item with content URI: mCurrentInventoryUri
            // and pass in the new ContentValues. Pass in null for the selection and selection args
            // because mCurrentInventoryUri will already identify the correct row in the database that
            // we want to modify.
            int rowsAffected = getContentResolver().update(mCurrentInventoryUri, values, null, null);

            // Show a toast message depending on whether or not the update was successful.
            if (rowsAffected == 0) {
                // If no rows were affected, then there was an error with the update.
                Toast.makeText(getBaseContext(), getString(R.string.editor_update_item_failed),
                        Toast.LENGTH_SHORT).show();
            } else {
                // Otherwise, the update was successful and we can display a toast.
                Toast.makeText(getBaseContext(), getString(R.string.editor_update_item_successful),
                        Toast.LENGTH_SHORT).show();
            }
        }

        finish();

    }

    private void showDeleteConfirmationDialog() {
        // Create an AlertDialog.Builder and set the message, and click listeners
        // for the positive and negative buttons on the dialog.
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.delete_dialog_msg);
        builder.setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User clicked the "Delete" button, so delete the item.
                deleteItem();
            }
        });
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User clicked the "Cancel" button, so dismiss the dialog
                // and continue editing the item.
                if (dialog != null) {
                    dialog.dismiss();
                }
            }
        });

        // Create and show the AlertDialog
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    /**
     * Perform the deletion of the item in the database.
     */
    private void deleteItem() {
        // Only perform the delete if this is an existing item.
        if (mCurrentInventoryUri != null) {
            // Call the ContentResolver to delete the item at the given content URI.
            // Pass in null for the selection and selection args because the mCurrentInventoryUri
            // content URI already identifies the item that we want.
            int rowsDeleted = getContentResolver().delete(mCurrentInventoryUri, null, null);

            // Show a toast message depending on whether or not the delete was successful.
            if (rowsDeleted == 0) {
                // If no rows were deleted, then there was an error with the delete.
                Toast.makeText(this, getString(R.string.editor_delete_item_failed),
                        Toast.LENGTH_SHORT).show();
            } else {
                // Otherwise, the delete was successful and we can display a toast.
                Toast.makeText(this, getString(R.string.editor_delete_item_successful),
                        Toast.LENGTH_SHORT).show();
            }
        }

        // Close the activity
        finish();
    }

    private void orderItem() {
        Intent emailIntent = new Intent(Intent.ACTION_SENDTO, Uri.fromParts(
                "mailto", supplier, null));
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Order for " + itemName);
        emailIntent.putExtra(Intent.EXTRA_TEXT, "We would like to order more from item: " + itemName);
        startActivity(Intent.createChooser(emailIntent, "Send email..."));
    }


    private void minButton() {
        if (numberOfItems > 0) {
            numberOfItems -= 1;
            mNumberOfItemsEditText.setText(String.valueOf(numberOfItems));
        } else {
            Toast.makeText(getBaseContext(), getString(R.string.cannot_decrease),
                    Toast.LENGTH_SHORT).show();
        }
    }

    private void plusButton() {
        numberOfItems += 1;
        mNumberOfItemsEditText.setText(String.valueOf(numberOfItems));
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        // Since the editor shows all item attributes, define a projection that contains
        // all columns from the item table
        String[] projection = {
                InventoryContract.InventoryEntry._ID,
                InventoryContract.InventoryEntry.COLUMN_ITEM_NAME,
                InventoryContract.InventoryEntry.COLUMN_SUPPLIER,
                InventoryContract.InventoryEntry.COLUMN_NUMBER_OF_ITEMS,
                InventoryContract.InventoryEntry.COLUMN_PRICE_PER_ITEM,
                InventoryContract.InventoryEntry.COLUMN_IMAGE
        };

        // This loader will execute the ContentProvider's query method on a background thread
        return new CursorLoader(this,   // Parent activity context
                mCurrentInventoryUri,         // Query the content URI for the current Item
                projection,             // Columns to include in the resulting Cursor
                null,                   // No selection clause
                null,                   // No selection arguments
                null);                  // Default sort order
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        // Bail early if the cursor is null or there is less than 1 row in the cursor
        if (cursor == null || cursor.getCount() < 1) {
            return;
        }

        // Proceed with moving to the first row of the cursor and reading data from it
        // (This should be the only row in the cursor)
        if (cursor.moveToFirst()) {
            // Find the columns of item attributes that we're interested in
            int itemNameColumnIndex = cursor.getColumnIndex(InventoryContract.InventoryEntry.COLUMN_ITEM_NAME);
            int supplierColumnIndex = cursor.getColumnIndex(InventoryContract.InventoryEntry.COLUMN_SUPPLIER);
            int numberOfItemsColumnIndex = cursor.getColumnIndex(InventoryContract.InventoryEntry.COLUMN_NUMBER_OF_ITEMS);
            int pricePerItemColumnIndex = cursor.getColumnIndex(InventoryContract.InventoryEntry.COLUMN_PRICE_PER_ITEM);
            int imageColumnIndex = cursor.getColumnIndex(InventoryContract.InventoryEntry.COLUMN_IMAGE);

            // Extract out the value from the Cursor for the given column index
            itemName = cursor.getString(itemNameColumnIndex);
            supplier = cursor.getString(supplierColumnIndex);
            numberOfItems = cursor.getInt(numberOfItemsColumnIndex);
            pricePerItem = cursor.getInt(pricePerItemColumnIndex);
            image = cursor.getString(imageColumnIndex);

            // Update the views on the screen with the values from the database
            mItemNameEditText.setText(itemName);
            mSupplierEditText.setText(supplier);
            mNumberOfItemsEditText.setText(Integer.toString(numberOfItems));
            mPricePerItemEditText.setText(Integer.toString(pricePerItem));
            mImageView.setImageURI(Uri.parse(image));
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mItemNameEditText.setText("");
        mSupplierEditText.setText("");
        mNumberOfItemsEditText.setText("");
        mPricePerItemEditText.setText("");
    }

    /**
     * Show a dialog that warns the user there are unsaved changes that will be lost
     * if they continue leaving the editor.
     *
     * @param discardButtonClickListener is the click listener for what to do when
     *                                   the user confirms they want to discard their changes
     */

    /**
     * OnTouchListener that listens for any user touches on a View, implying that they are modifying
     * the view, and we change the mItemHasChanged boolean to true.
     */
    private View.OnTouchListener mTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            mItemHasChanged = true;
            return false;
        }
    };

    private void showUnsavedChangesDialog(
            DialogInterface.OnClickListener discardButtonClickListener) {
        // Create an AlertDialog.Builder and set the message, and click listeners
        // for the positive and negative buttons on the dialog.
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.unsaved_changes_dialog_msg);
        builder.setPositiveButton(R.string.discard, discardButtonClickListener);
        builder.setNegativeButton(R.string.keep_editing, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User clicked the "Keep editing" button, so dismiss the dialog
                // and continue editing the item.
                if (dialog != null) {
                    dialog.dismiss();
                }
            }
        });

        // Create and show the AlertDialog
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    public void uploadImage() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(intent, REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            mCurrentImageUri = data.getData();
            mImageView.setImageURI(mCurrentImageUri);
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

}
