package com.dteam.twittersearches;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;
import androidx.appcompat.widget.Toolbar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputLayout;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;



public class MainActivity extends AppCompatActivity {
    private static final String SEARCHES = "searches";
    private EditText queryEditText;
    private EditText tagEditText;
    private FloatingActionButton saveFloatingActionButton;
    private SharedPreferences savedSearches;
    private List<String> tags;
    private SearchesAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        this.queryEditText = ((TextInputLayout) findViewById(R.id.queryTextInputLayout)).getEditText();
        this.queryEditText.addTextChangedListener(textWatcher);
        this.tagEditText = ((TextInputLayout) findViewById(R.id.tagTextInputLayout)).getEditText();
        this.tagEditText.addTextChangedListener(textWatcher);
        this.savedSearches = getSharedPreferences(SEARCHES, MODE_PRIVATE);
        this.tags = new ArrayList<>(savedSearches.getAll().keySet());
        Collections.sort(this.tags, String.CASE_INSENSITIVE_ORDER);
        RecyclerView recyclerView = (RecyclerView) findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new SearchesAdapter(tags, itemClickListener, itemLongClickListener);
        recyclerView.setAdapter(adapter);
        recyclerView.addItemDecoration(new ItemDivider(this));
        this.saveFloatingActionButton = (FloatingActionButton) findViewById(R.id.fab);
        this.saveFloatingActionButton.setOnClickListener(this.saveButtonListener);
        this.updateSaveFAB();
    }

    private final TextWatcher textWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {

        }
        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            updateSaveFAB();
        }

        @Override
        public void afterTextChanged(Editable s) {
        }
    };

    private void updateSaveFAB() {
        if (this.queryEditText.getText().toString().isEmpty() || tagEditText.getText().toString().isEmpty())
            this.saveFloatingActionButton.hide();
        else
            this.saveFloatingActionButton.show();
    }

    private final OnClickListener saveButtonListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            String query = queryEditText.getText().toString();
            String tag = tagEditText.getText().toString();
            if (!query.isEmpty() && !tag.isEmpty()){
                ((InputMethodManager) getSystemService(
                        Context.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(v.getWindowToken(), 0);
                addTaggedSearch(tag, query);
                queryEditText.setText("");
                tagEditText.setText("");
                queryEditText.requestFocus();
            }
        }
    };

    private void addTaggedSearch(String tag, String query){
        SharedPreferences.Editor preferencesEditor = savedSearches.edit();
        preferencesEditor.putString(tag, query);
        preferencesEditor.apply();
        if (!this.tags.contains(tag)){
            this.tags.add(tag);
            Collections.sort(this.tags, String.CASE_INSENSITIVE_ORDER);
            this.adapter.notifyDataSetChanged();
        }
    }

    private final OnClickListener itemClickListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            String tag = ((TextView) v).getText().toString();
            String urlString = "http://mobile.twitter.com/search?q=" + Uri.encode(savedSearches.getString(tag, ""), "UTF-8");
            Intent webIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(urlString));
            startActivity(webIntent);
        }
    };

    private final OnLongClickListener itemLongClickListener = new View.OnLongClickListener() {
        @Override
        public boolean onLongClick(View v) {
            final String tag = ((TextView) v).getText().toString();
            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
            builder.setTitle("Share, Edit or Delete the search tagged as" + tag);
            String[] items = new String[] {"Share", "Edit", "Delete"};
            builder.setItems(items, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    switch (which){
                        case 0:
                            shareSearch(tag);
                            break;
                        case 1:
                            tagEditText.setText(tag);
                            queryEditText.setText(savedSearches.getString(tag, ""));
                            break;
                        case 2:
                            deleteSearch(tag);
                            break;
                    }
                }
            });
            builder.setNegativeButton("Cancel", null);
            builder.create().show();
            return true;
        }
    };

    private void shareSearch(String tag){
        String urlString = "http://mobile.twitter.come/search?q=" + Uri.encode(savedSearches.getString(tag, ""), "UTF-8");
        Intent shareIntent = new Intent();
        shareIntent.setAction(Intent.ACTION_SEND);
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Twitter search that might interest you");
        shareIntent.putExtra(Intent.EXTRA_TEXT,"Check out the results of this Twitter search:" + urlString);
        shareIntent.setType("text/plain");
        startActivity(Intent.createChooser(shareIntent, "Share Search to:"));
    }

    private void deleteSearch(final String tag){
        AlertDialog.Builder confirmBuilder = new AlertDialog.Builder(this);
        confirmBuilder.setMessage("Are you sure you want to delete the search" + tag);
        confirmBuilder.setNegativeButton("Cancel", null);
        confirmBuilder.setPositiveButton("Delete", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                tags.remove(tag);
                SharedPreferences.Editor preferencesEditor = savedSearches.edit();
                preferencesEditor.remove(tag);
                preferencesEditor.apply();
                adapter.notifyDataSetChanged();
            }
        });
        confirmBuilder.create().show();
    }

}
