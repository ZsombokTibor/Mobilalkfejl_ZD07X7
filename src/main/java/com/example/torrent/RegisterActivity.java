package com.example.torrent;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;

public class RegisterActivity extends AppCompatActivity {

    private TextInputLayout tilRegisterEmail, tilRegisterPassword, tilConfirmPassword;
    private TextInputEditText etRegisterEmail, etRegisterPassword, etConfirmPassword;
    private Button btnRegister;
    private TextView tvGoToLogin;
    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);


        tilRegisterEmail = findViewById(R.id.tilRegisterEmail);
        tilRegisterPassword = findViewById(R.id.tilRegisterPassword);
        tilConfirmPassword = findViewById(R.id.tilConfirmPassword);
        etRegisterEmail = findViewById(R.id.etRegisterEmail);
        etRegisterPassword = findViewById(R.id.etRegisterPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        btnRegister = findViewById(R.id.btnRegister);
        tvGoToLogin = findViewById(R.id.tvGoToLogin);

        auth = FirebaseAuth.getInstance();


        btnRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = etRegisterEmail.getText().toString().trim();
                String password = etRegisterPassword.getText().toString().trim();
                String confirmPassword = etConfirmPassword.getText().toString().trim();

                if (validateInputs(email, password, confirmPassword)) {
                    registerUser(email, password);
                }
            }
        });


        tvGoToLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });
    }

    private boolean validateInputs(String email, String password, String confirmPassword) {
        if (TextUtils.isEmpty(email)) {
            tilRegisterEmail.setError("Add meg az email címed");
            return false;
        } else {
            tilRegisterEmail.setError(null);
        }

        if (TextUtils.isEmpty(password)) {
            tilRegisterPassword.setError("Add meg a jelszavad");
            return false;
        } else {
            tilRegisterPassword.setError(null);
        }

        if (password.length() < 6) {
            tilRegisterPassword.setError("A jelszónak legalább 6 karakter hosszúnak kell lennie");
            return false;
        } else {
            tilRegisterPassword.setError(null);
        }

        if (TextUtils.isEmpty(confirmPassword)) {
            tilConfirmPassword.setError("Erősítsd meg a jelszavad");
            return false;
        } else {
            tilConfirmPassword.setError(null);
        }

        if (!password.equals(confirmPassword)) {
            tilConfirmPassword.setError("A jelszavak nem egyeznek");
            return false;
        } else {
            tilConfirmPassword.setError(null);
        }

        return true;
    }

    private void registerUser(String email, String password) {

        btnRegister.setEnabled(false);

        auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {

                            Toast.makeText(
                                    RegisterActivity.this,
                                    "Sikeres regisztráció!",
                                    Toast.LENGTH_SHORT
                            ).show();
                            startActivity(new Intent(RegisterActivity.this, MainActivity.class));
                            finish();
                        } else {

                            Toast.makeText(
                                    RegisterActivity.this,
                                    "Regisztráció sikertelen: " + task.getException().getMessage(),
                                    Toast.LENGTH_LONG
                            ).show();
                            btnRegister.setEnabled(true);
                        }
                    }
                });
    }
}
