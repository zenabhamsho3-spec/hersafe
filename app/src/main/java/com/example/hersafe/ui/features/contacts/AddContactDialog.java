package com.example.hersafe.ui.features.contacts;

import android.os.Bundle;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.hersafe.R;
import com.example.hersafe.data.local.AppDatabase;
import com.example.hersafe.data.local.dao.ContactDao;
import com.example.hersafe.data.local.entities.Contact;

/**
 * شاشة إضافة جهة اتصال جديدة
 * تحفظ البيانات في قاعدة بيانات Room
 */
public class AddContactDialog extends AppCompatActivity {

    private EditText etName, etPhone, etRelation;
    private ContactDao contactDao;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_add_contact);

        // تعريف قاعدة البيانات
        contactDao = AppDatabase.getInstance(this).contactDao();

        // تعريف الحقول
        etName = findViewById(R.id.etName);
        etPhone = findViewById(R.id.etPhone);
        etRelation = findViewById(R.id.etRelation);

        // زر الحفظ
        findViewById(R.id.btnSave).setOnClickListener(v -> saveContact());

        // زر الإلغاء
        findViewById(R.id.btnCancel).setOnClickListener(v -> finish());
    }

    /**
     * حفظ جهة الاتصال في قاعدة البيانات
     */
    private void saveContact() {
        String name = etName.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();
        String relation = etRelation.getText().toString().trim();

        // التحقق من البيانات
        if (name.isEmpty()) {
            etName.setError("الرجاء إدخال الاسم");
            etName.requestFocus();
            return;
        }

        if (phone.isEmpty()) {
            etPhone.setError("الرجاء إدخال رقم الهاتف");
            etPhone.requestFocus();
            return;
        }

        // إنشاء كائن جهة الاتصال
        Contact contact = new Contact(name, phone, relation);

        // حفظ في قاعدة البيانات
        long id = contactDao.insert(contact);

        if (id > 0) {
            Toast.makeText(this, "تمت إضافة \"" + name + "\" بنجاح", Toast.LENGTH_SHORT).show();
            finish(); // إغلاق الصفحة والعودة للقائمة
        } else {
            Toast.makeText(this, "حدث خطأ أثناء الحفظ", Toast.LENGTH_SHORT).show();
        }
    }
}