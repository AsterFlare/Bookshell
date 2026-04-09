package com.example.testbooks1;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.FirebaseDatabase;

public class AccountDetailsActivity extends AppCompatActivity {
    public static final String EXTRA_OPEN_PASSWORD_DIALOG = "open_password_dialog";

    private EditText etEmail;
    private Button btnSaveEmail;
    private Button btnChangePassword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_account_details);

        etEmail = findViewById(R.id.etEmail);
        btnSaveEmail = findViewById(R.id.btnSaveEmail);
        btnChangePassword = findViewById(R.id.btnChangePassword);

        findViewById(R.id.toolbarBack).setOnClickListener(v -> finish());

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null && user.getEmail() != null) {
            etEmail.setText(user.getEmail());
        }

        btnSaveEmail.setOnClickListener(v -> updateEmail());
        btnChangePassword.setOnClickListener(v -> showChangePasswordDialog());

        if (getIntent().getBooleanExtra(EXTRA_OPEN_PASSWORD_DIALOG, false)) {
            findViewById(android.R.id.content).post(this::showChangePasswordDialog);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshEmailFromAuth();
    }

    private void updateEmail() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            return;
        }
        String email = etEmail.getText().toString().trim();
        if (TextUtils.isEmpty(email) || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this, R.string.toast_invalid_email, Toast.LENGTH_SHORT).show();
            return;
        }
        String oldEmail = user.getEmail();
        if (oldEmail == null || oldEmail.isEmpty()) {
            Toast.makeText(this, R.string.toast_email_update_failed, Toast.LENGTH_LONG).show();
            return;
        }
        if (oldEmail.equalsIgnoreCase(email)) {
            Toast.makeText(this, R.string.toast_no_email_changes, Toast.LENGTH_SHORT).show();
            return;
        }

        promptForCurrentPasswordThenUpdateEmail(user, oldEmail, email);
    }

    private void promptForCurrentPasswordThenUpdateEmail(FirebaseUser user, String oldEmail, String newEmail) {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_confirm_email, null);
        EditText currentPasswordInput = dialogView.findViewById(R.id.etCurrentPasswordConfirm);
        Button btnCancel = dialogView.findViewById(R.id.btnCancelConfirmEmail);
        Button btnSave = dialogView.findViewById(R.id.btnSaveConfirmEmail);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        btnCancel.setOnClickListener(v -> dialog.dismiss());
        btnSave.setOnClickListener(v -> {
            String currentPassword = currentPasswordInput.getText().toString();
            if (TextUtils.isEmpty(currentPassword)) {
                Toast.makeText(this, R.string.toast_enter_current_password_for_email, Toast.LENGTH_SHORT).show();
                return;
            }

            setSaving(true);
            user.reload().addOnCompleteListener(reloadTask -> {
                if (!reloadTask.isSuccessful()) {
                    setSaving(false);
                    Toast.makeText(this, R.string.toast_auth_update_failed, Toast.LENGTH_LONG).show();
                    return;
                }
                AuthCredential credential = EmailAuthProvider.getCredential(oldEmail, currentPassword);
                user.reauthenticate(credential).addOnCompleteListener(reauthTask -> {
                    if (!reauthTask.isSuccessful()) {
                        setSaving(false);
                        Toast.makeText(this, authErrorMessage(reauthTask.getException(), true, false), Toast.LENGTH_LONG).show();
                        return;
                    }
                    @SuppressWarnings("deprecation")
                    com.google.android.gms.tasks.Task<Void> updateTask = user.updateEmail(newEmail);
                    updateTask.addOnCompleteListener(t -> {
                        if (!t.isSuccessful()) {
                            setSaving(false);
                            Toast.makeText(this, authErrorMessage(t.getException(), false, true), Toast.LENGTH_LONG).show();
                            return;
                        }
                        FirebaseDatabase.getInstance().getReference("users")
                                .child(user.getUid())
                                .child("email")
                                .setValue(newEmail)
                                .addOnCompleteListener(task -> {
                                    setSaving(false);
                                    if (task.isSuccessful()) {
                                        dialog.dismiss();
                                        Toast.makeText(this, R.string.toast_email_updated, Toast.LENGTH_SHORT).show();
                                    } else {
                                        Toast.makeText(this, R.string.toast_email_update_failed, Toast.LENGTH_LONG).show();
                                    }
                                });
                    });
                });
            });
        });
        dialog.show();
    }

    private void refreshEmailFromAuth() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            return;
        }
        user.reload().addOnCompleteListener(task -> {
            FirebaseUser refreshed = FirebaseAuth.getInstance().getCurrentUser();
            if (refreshed == null || refreshed.getEmail() == null) {
                return;
            }
            String email = refreshed.getEmail();
            etEmail.setText(email);
            FirebaseDatabase.getInstance().getReference("users")
                    .child(refreshed.getUid())
                    .child("email")
                    .setValue(email);
        });
    }

    private void showChangePasswordDialog() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            return;
        }
        String emailAddr = user.getEmail();
        if (emailAddr == null || emailAddr.isEmpty()) {
            Toast.makeText(this, R.string.toast_auth_update_failed, Toast.LENGTH_LONG).show();
            return;
        }

        View dialogView = getLayoutInflater().inflate(R.layout.dialog_change_password, null);
        EditText currentPasswordInput = dialogView.findViewById(R.id.currentPasswordInput);
        EditText newPasswordInput = dialogView.findViewById(R.id.newPasswordInput);
        EditText confirmPasswordInput = dialogView.findViewById(R.id.confirmPasswordInput);
        Button btnCancel = dialogView.findViewById(R.id.btnCancelPasswordChange);
        Button btnSavePw = dialogView.findViewById(R.id.btnSavePasswordChange);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnSavePw.setOnClickListener(v -> {
            String current = currentPasswordInput.getText().toString();
            String newPass = newPasswordInput.getText().toString();
            String confirm = confirmPasswordInput.getText().toString();

            if (TextUtils.isEmpty(current) || TextUtils.isEmpty(newPass) || TextUtils.isEmpty(confirm)) {
                Toast.makeText(this, R.string.toast_password_fields_required, Toast.LENGTH_SHORT).show();
                return;
            }
            if (newPass.length() < 6) {
                Toast.makeText(this, R.string.toast_password_too_short, Toast.LENGTH_SHORT).show();
                return;
            }
            if (!newPass.equals(confirm)) {
                Toast.makeText(this, R.string.toast_password_mismatch, Toast.LENGTH_SHORT).show();
                return;
            }
            if (newPass.equals(current)) {
                Toast.makeText(this, R.string.toast_password_same_as_current, Toast.LENGTH_SHORT).show();
                return;
            }

            btnSavePw.setEnabled(false);
            btnCancel.setEnabled(false);

            AuthCredential credential = EmailAuthProvider.getCredential(emailAddr, current);
            user.reauthenticate(credential).addOnCompleteListener(task -> {
                if (!task.isSuccessful()) {
                    btnSavePw.setEnabled(true);
                    btnCancel.setEnabled(true);
                    Toast.makeText(this, authErrorMessage(task.getException(), true, false), Toast.LENGTH_SHORT).show();
                    return;
                }
                user.updatePassword(newPass).addOnCompleteListener(t2 -> {
                    btnSavePw.setEnabled(true);
                    btnCancel.setEnabled(true);
                    if (t2.isSuccessful()) {
                        Toast.makeText(this, R.string.toast_password_updated, Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                    } else {
                        Toast.makeText(this, authErrorMessage(t2.getException(), false, false), Toast.LENGTH_LONG).show();
                    }
                });
            });
        });

        dialog.show();
    }

    private void setSaving(boolean saving) {
        btnSaveEmail.setEnabled(!saving);
        btnChangePassword.setEnabled(!saving);
        etEmail.setEnabled(!saving);
    }

    private String authErrorMessage(@Nullable Exception exception, boolean isReauth, boolean isEmailUpdate) {
        if (exception instanceof FirebaseAuthException) {
            FirebaseAuthException fae = (FirebaseAuthException) exception;
            String code = fae.getErrorCode();
            switch (code) {
                case "ERROR_REQUIRES_RECENT_LOGIN":
                    return getString(R.string.toast_auth_recent_login_required);
                case "ERROR_EMAIL_ALREADY_IN_USE":
                    return getString(R.string.toast_email_already_in_use);
                case "ERROR_INVALID_EMAIL":
                    return getString(R.string.toast_invalid_email);
                case "ERROR_WEAK_PASSWORD":
                    return getString(R.string.toast_password_too_weak);
                case "ERROR_WRONG_PASSWORD":
                case "ERROR_INVALID_CREDENTIAL":
                    return getString(R.string.toast_current_password_incorrect);
                default:
                    if (isEmailUpdate) {
                        return getString(R.string.toast_email_update_failed_with_code, code);
                    }
            }
        }
        if (isReauth) {
            return getString(R.string.toast_current_password_incorrect);
        }
        return isEmailUpdate ? getString(R.string.toast_email_update_failed)
                : getString(R.string.toast_auth_update_failed);
    }
}
