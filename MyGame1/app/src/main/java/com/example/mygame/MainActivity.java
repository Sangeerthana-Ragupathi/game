package com.example.mygame;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    EditText usernameInput;
    Button startButton;
    TextView promptText; // To show the prompt

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        usernameInput = findViewById(R.id.username_input);
        startButton = findViewById(R.id.start_button);
        promptText = findViewById(R.id.prompt_text); // New TextView to show prompt

        // Access shared preferences to check if a username is already saved
        SharedPreferences preferences = getSharedPreferences("GAME_PREFS", MODE_PRIVATE);
        String username = preferences.getString("USERNAME", null);

        if (username != null) {
            // If username is already set, go directly to GameActivity
            Intent intent = new Intent(MainActivity.this, GameActivity.class);
            startActivity(intent);
            finish(); // Close MainActivity
        } else {
            // If not, show the input field and prompt
            promptText.setVisibility(View.VISIBLE);
            usernameInput.setVisibility(View.VISIBLE);
            startButton.setVisibility(View.VISIBLE);
        }

        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String username = usernameInput.getText().toString();
                if (!username.isEmpty()) {
                    SharedPreferences.Editor editor = preferences.edit();
                    editor.putString("USERNAME", username);
                    editor.apply();

                    // Navigate to the GameActivity
                    Intent intent = new Intent(MainActivity.this, GameActivity.class);
                    startActivity(intent);
                    finish(); // Close MainActivity
                }
            }
        });
    }
}


