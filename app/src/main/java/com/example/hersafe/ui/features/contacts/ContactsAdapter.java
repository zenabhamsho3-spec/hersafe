package com.example.hersafe.ui.features.contacts;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.hersafe.R;
import com.example.hersafe.data.local.entities.Contact;

import java.util.List;

/**
 * Adapter لعرض جهات الاتصال في RecyclerView
 */
public class ContactsAdapter extends RecyclerView.Adapter<ContactsAdapter.ContactViewHolder> {

    private final Context context;
    private List<Contact> contactList;
    private final OnContactActionListener listener;

    /**
     * Interface للتعامل مع أحداث العناصر
     */
    public interface OnContactActionListener {
        void onDeleteClick(Contact contact, int position);
        void onEditClick(Contact contact, int position);
        void onCallClick(Contact contact);
    }

    public ContactsAdapter(Context context, List<Contact> contactList, OnContactActionListener listener) {
        this.context = context;
        this.contactList = contactList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ContactViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context)
                .inflate(R.layout.item_contact, parent, false);
        return new ContactViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ContactViewHolder holder, int position) {
        Contact contact = contactList.get(position);
        
        holder.tvName.setText(contact.getName());
        holder.tvPhone.setText(contact.getPhone());
        holder.tvRelation.setText(contact.getRelation());

        // زر الحذف
        holder.btnDelete.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDeleteClick(contact, position);
            }
        });

        // زر الاتصال (إن وجد)
        if (holder.btnCall != null) {
            holder.btnCall.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onCallClick(contact);
                }
            });
        }

        // الضغط على العنصر للتعديل
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onEditClick(contact, position);
            }
        });
    }

    @Override
    public int getItemCount() {
        return contactList != null ? contactList.size() : 0;
    }

    /**
     * تحديث قائمة جهات الاتصال
     */
    public void updateContacts(List<Contact> newContacts) {
        this.contactList = newContacts;
        notifyDataSetChanged();
    }

    /**
     * حذف جهة اتصال من القائمة
     */
    public void removeContact(int position) {
        if (position >= 0 && position < contactList.size()) {
            contactList.remove(position);
            notifyItemRemoved(position);
            notifyItemRangeChanged(position, contactList.size());
        }
    }

    /**
     * ViewHolder لعنصر جهة الاتصال
     */
    static class ContactViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvPhone, tvRelation;
        ImageButton btnDelete, btnCall;

        public ContactViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvName);
            tvPhone = itemView.findViewById(R.id.tvNumber);
            tvRelation = itemView.findViewById(R.id.tvRelation);
            btnDelete = itemView.findViewById(R.id.btnDelete);
            // btnCall غير موجود في التصميم الحالي، سنضيفه لاحقاً إذا لزم
            btnCall = null;
        }
    }
}
