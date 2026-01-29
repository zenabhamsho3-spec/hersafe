package com.example.hersafe.ui.features.contacts;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.hersafe.R;
import com.example.hersafe.data.local.AppDatabase;
import com.example.hersafe.data.local.dao.ContactDao;
import com.example.hersafe.data.local.entities.Contact;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

/**
 * شاشة إدارة جهات الاتصال الطارئة
 * تعرض قائمة جهات الاتصال المحفوظة مع إمكانية الإضافة والحذف
 */
public class ContactsActivity extends AppCompatActivity implements ContactsAdapter.OnContactActionListener {

    private RecyclerView recyclerView;
    private FloatingActionButton fabAdd;
    private ContactDao contactDao;
    private ContactsAdapter adapter;
    private List<Contact> contactList;
    private TextView tvEmptyState;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contacts);

        // 1. معالجة الحواف
        View mainView = findViewById(R.id.main);
        if (mainView != null) {
            ViewCompat.setOnApplyWindowInsetsListener(mainView, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                return insets;
            });
        }

        // 2. تعريف العناصر
        recyclerView = findViewById(R.id.rvContacts);
        fabAdd = findViewById(R.id.fabAddContact);
        tvEmptyState = findViewById(R.id.tvEmptyState);

        // 3. تهيئة قاعدة البيانات
        contactDao = AppDatabase.getInstance(this).contactDao();
        contactList = new ArrayList<>();

        // 4. إعداد RecyclerView
        adapter = new ContactsAdapter(this, contactList, this);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        // 5. برمجة زر الإضافة للانتقال لصفحة الإضافة
        fabAdd.setOnClickListener(v -> {
            Intent intent = new Intent(ContactsActivity.this, AddContactDialog.class);
            startActivity(intent);
        });
    }

    // هذه الدالة تعمل تلقائياً عند العودة للصفحة لتحديث البيانات
    @Override
    protected void onResume() {
        super.onResume();
        refreshContactsList();
    }

    /**
     * تحديث قائمة جهات الاتصال من قاعدة البيانات
     */
    private void refreshContactsList() {
        contactList = contactDao.getAllContacts();

        if (contactList == null || contactList.isEmpty()) {
            tvEmptyState.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            tvEmptyState.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
            adapter.updateContacts(contactList);
        }
    }

    // ==================== ContactsAdapter.OnContactActionListener ====================

    @Override
    public void onDeleteClick(Contact contact, int position) {
        // عرض تأكيد قبل الحذف
        new AlertDialog.Builder(this)
                .setTitle("حذف جهة الاتصال")
                .setMessage("هل أنت متأكد من حذف \"" + contact.getName() + "\"؟")
                .setPositiveButton("حذف", (dialog, which) -> {
                    contactDao.delete(contact);
                    adapter.removeContact(position);
                    Toast.makeText(this, "تم حذف جهة الاتصال", Toast.LENGTH_SHORT).show();
                    
                    // التحقق إذا أصبحت القائمة فارغة
                    if (adapter.getItemCount() == 0) {
                        tvEmptyState.setVisibility(View.VISIBLE);
                        recyclerView.setVisibility(View.GONE);
                    }
                })
                .setNegativeButton("إلغاء", null)
                .show();
    }

    @Override
    public void onEditClick(Contact contact, int position) {
        // يمكن تنفيذ التعديل لاحقاً
        Toast.makeText(this, "تعديل: " + contact.getName(), Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onCallClick(Contact contact) {
        // الاتصال بجهة الاتصال
        Intent intent = new Intent(Intent.ACTION_DIAL);
        intent.setData(Uri.parse("tel:" + contact.getPhone()));
        startActivity(intent);
    }
}