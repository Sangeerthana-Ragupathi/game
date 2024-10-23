package com.example.mygame;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import android.content.SharedPreferences;
import android.os.Handler;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.IOException;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import android.content.Intent;

public class GameActivity extends AppCompatActivity {

    TextView usernameDisplay, scoreDisplay, wordHint, timerDisplay, leaderboardView;
    EditText guessInput;
    Button submitButton, letterHintButton, wordLengthButton, resetNameButton, fetchButton;

    String rhymeWord;
    String synonym;
    String secretWord = "";
    int score = 100;
    int attempts = 0;
    Handler timerHandler = new Handler();
    long startTime = 0;
    boolean hintGiven = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);

        // Initialize UI components and set up listeners
        usernameDisplay = findViewById(R.id.username_display);
        scoreDisplay = findViewById(R.id.score_display);
        wordHint = findViewById(R.id.word_hint);
        guessInput = findViewById(R.id.guess_input);
        submitButton = findViewById(R.id.submit_button);
        letterHintButton = findViewById(R.id.letter_hint_button);
        wordLengthButton = findViewById(R.id.word_length_button);
        timerDisplay = findViewById(R.id.timer_display);
        resetNameButton = findViewById(R.id.reset_name_button);

        leaderboardView = findViewById(R.id.leaderboard_view);

        // Initialize the fetch leaderboard button
        fetchButton = findViewById(R.id.fetch_button);
        // Set up the click listener for the fetchButton
        fetchButton.setOnClickListener(v -> fetchLeaderboard());

        resetNameButton.setOnClickListener(v -> {
            // Clear the saved username from SharedPreferences
            SharedPreferences preferences = getSharedPreferences("GAME_PREFS", MODE_PRIVATE);
            SharedPreferences.Editor editor = preferences.edit();
            editor.remove("USERNAME");
            editor.apply();

            // Go back to the MainActivity (username input page)
            Intent intent = new Intent(GameActivity.this, MainActivity.class);
            startActivity(intent);
            finish(); // Close GameActivity
        });

        SharedPreferences preferences = getSharedPreferences("GAME_PREFS", MODE_PRIVATE);
        String username = preferences.getString("USERNAME", "Player");

        // Display the username
        usernameDisplay.setText("Welcome : " + username + "!");

        startGame();

        // Timer
        startTime = System.currentTimeMillis();
        timerHandler.postDelayed(timerRunnable, 1000);

        // Submit guess
        submitButton.setOnClickListener(v -> {
            String guess = guessInput.getText().toString().trim(); // Trim input to avoid whitespace issues

            // Check if the guessed word matches the secret word
            if (guess.equalsIgnoreCase(secretWord)) {
                long totalTime = System.currentTimeMillis() - startTime;
                wordHint.setText("Correct! The word was " + secretWord);
                timerHandler.removeCallbacks(timerRunnable);

            } else {
                attempts++;
                score -= 10;
                scoreDisplay.setText("Score: " + score);
                wordHint.setText("Incorrect guess! Try again.");

                // Provide a hint after the fifth attempt
                if (attempts == 5 && !hintGiven) {
                    provideHint(secretWord);
                    hintGiven = true; // Ensure the hint is given only once
                }
            }

            // Check if attempts have reached 10 or if score is less than or equal to 0
            if (attempts == 10 || score <= 0) {
                String message = (attempts == 10)
                        ? "You've reached the maximum attempts! The correct word was: " + secretWord
                        : "Your score is too low. The correct word was: " + secretWord;

                wordHint.setText(message);
                new Handler().postDelayed(this::startGame, 3000); // Restart the game after a 3-second delay
            }
        });

        // Letter hint
        letterHintButton.setOnClickListener(v -> {
            score -= 5;
            scoreDisplay.setText("Score: " + score);
            if (hintGiven) {
                wordHint.setText("Hint: A word that rhymes with the secret word is \"" + rhymeWord + "\".");
            } else {
                wordHint.setText("Hint: A synonym for the secret word is \"" + synonym + "\".");
            }
        });

        // Word length
        wordLengthButton.setOnClickListener(v -> {
            score -= 5;
            scoreDisplay.setText("Score: " + score);
            wordHint.setText("The word has " + secretWord.length() + " letters.");
        });
    }

    private void startGame() {
        // Reset score, attempts, and UI
        score = 100;
        attempts = 0;
        hintGiven = false; // Reset hint given flag
        scoreDisplay.setText("Score: " + score);
        wordHint.setText("Fetching a new word...");

        // Get a new word from the API
        getRandomWord();
    }

    private void getRandomWord() {
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url("")
                .addHeader("X-Api-Key", "") // Ensure your API key is valid
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e("API_CALL", "Request Failed", e);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    try {
                        String json = response.body().string();
                        JSONObject jsonObject = new JSONObject(json);
                        secretWord = jsonObject.getString("word");

                        // Update UI on the main thread after receiving the word
                        runOnUiThread(() -> {
                            wordHint.setText("Guess the new word!");
                            scoreDisplay.setText("Score: " + score);
                        });

                    } catch (Exception e) {
                        Log.e("API_CALL", "JSON Parsing error", e);
                    }
                }
            }
        });
    }



    // Method to provide a hint based on attempts
    private void provideHint(String word) {
        Random random = new Random();
        int hintType = random.nextInt(2); // 0 for rhyme, 1 for synonym

        if (hintType == 0) {
            getRhymingWord(word); // Fetch a rhyming word
        } else {
            getSynonym(word); // Fetch a synonym
        }
    }

    // Get rhyming word method
    private void getRhymingWord(String word) {
        OkHttpClient client = new OkHttpClient();
        String url = "" + word; // Corrected URL

        Request request = new Request.Builder()
                .url(url)
                .addHeader("X-Api-Key", "") // Ensure your API key is valid
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e("API_CALL", "Rhyme request failed", e);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    try {
                        String json = response.body().string();
                        JSONArray jsonArray = new JSONArray(json);
                        if (jsonArray.length() > 0) {
                            JSONObject rhymeObj = jsonArray.getJSONObject(0);
                            GameActivity.this.rhymeWord = rhymeObj.getString("word");

                            // Update UI with the rhyme hint
                            runOnUiThread(() -> {
                                wordHint.setText("Hint: A word that rhymes with the secret word is \"" + rhymeWord + "\".");
                            });
                        } else {
                            Log.e("API_CALL", "No rhymes found");
                        }
                    } catch (Exception e) {
                        Log.e("API_CALL", "Error parsing rhyme response", e);
                    }
                }
            }
        });
    }

    // Get synonym method
    private void getSynonym(String word) {
        OkHttpClient client = new OkHttpClient();
        String url = "" + word; // Corrected URL

        Request request = new Request.Builder()
                .url(url)
                .addHeader("X-Api-Key", "") // Ensure your API key is valid
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e("API_CALL", "Thesaurus request failed", e);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    try {
                        String json = response.body().string();
                        JSONObject jsonObject = new JSONObject(json);
                        JSONArray synonymsArray = jsonObject.getJSONArray("synonyms");

                        if (synonymsArray.length() > 0) {
                            String synonym = synonymsArray.getString(0); // Get the first synonym

                            // Update UI with the synonym hint
                            runOnUiThread(() -> {
                                wordHint.setText("Hint: A synonym for the secret word is \"" + synonym + "\".");
                            });
                        } else {
                            Log.e("API_CALL", "No synonyms found");
                        }
                    } catch (Exception e) {
                        Log.e("API_CALL", "Error parsing synonym response", e);
                    }
                }
            }
        });
    }

    private void fetchLeaderboard() {
        OkHttpClient client = new OkHttpClient();
        String url = ""; // Your private link

        Request request = new Request.Builder()
                .url(url)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e("Leaderboard", "Failed to fetch leaderboard", e);
                runOnUiThread(() -> leaderboardView.setText("Failed to fetch leaderboard."));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    String jsonData = response.body().string();
                    Log.d("Leaderboard", "Received response: " + jsonData);

                    // Parse the JSON response
                    try {
                        JSONObject jsonObject = new JSONObject(jsonData);
                        JSONArray entries = jsonObject.getJSONArray("leaderboard"); // Adjusted for the correct structure

                        StringBuilder leaderboardText = new StringBuilder();

                        // Process leaderboard entries
                        for (int i = 0; i < entries.length(); i++) {
                            JSONObject entry = entries.getJSONObject(i);
                            String username = entry.getString("name");
                            int score = entry.getInt("score");
                            long time = entry.getLong("seconds");

                            // Prepare the text to display on the leaderboard
                            leaderboardText.append("Player: ").append(username)
                                    .append(", Score: ").append(score)
                                    .append(", Time: ").append(time).append("s\n");
                        }

                        // Update the leaderboard UI on the main thread
                        runOnUiThread(() -> {
                            leaderboardView.setText(leaderboardText.toString());
                        });

                    } catch (Exception e) {
                        Log.e("Leaderboard", "Error parsing leaderboard JSON", e);
                        runOnUiThread(() -> leaderboardView.setText("Error parsing leaderboard data."));
                    }
                } else {
                    Log.e("Leaderboard", "Failed to fetch leaderboard: " + response.code());
                    runOnUiThread(() -> leaderboardView.setText("Error fetching leaderboard."));
                }
            }
        });
    }

    private Runnable timerRunnable = new Runnable() {
        @Override
        public void run() {
            long time = System.currentTimeMillis() - startTime;
            long seconds = TimeUnit.MILLISECONDS.toSeconds(time);
            timerDisplay.setText("Time: " + seconds + "s");
            timerHandler.postDelayed(this, 1000);
        }
    };
}
