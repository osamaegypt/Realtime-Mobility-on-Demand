package com.android.gudana.hify.adapters;

import android.app.Activity;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.android.gudana.hify.models.FriendRequest;
import com.android.gudana.hify.ui.activities.friends.FriendProfile;
import com.android.gudana.R;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.github.marlonlom.utilities.timeago.TimeAgo;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;

/**
 * Created by amsavarthan on 22/2/18.
 */

public class FriendRequestAdapter extends RecyclerView.Adapter<FriendRequestAdapter.ViewHolder> {

    public static Activity activity;
    private Context context;
    private List<FriendRequest> usersList;

    public FriendRequestAdapter(List<FriendRequest> usersList, Context context, Activity activity) {
        this.usersList = usersList;
        this.context = context;
        this.activity = activity;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.hi_friend_req_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull final ViewHolder holder, int position) {

        holder.name.setText(usersList.get(position).getName());

        Glide.with(context)
                .setDefaultRequestOptions(new RequestOptions().placeholder(R.drawable.default_user_art_g_2))
                .load(usersList.get(position).getImage())
                .into(holder.image);

        String timeAgo = TimeAgo.using(Long.parseLong(usersList.get(position).getTimestamp()));
        holder.timestamp.setText(timeAgo);

        try {
            FirebaseFirestore.getInstance().collection("Users")
                    .document(usersList.get(position).userId)
                    .get()
                    .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                        @Override
                        public void onSuccess(DocumentSnapshot documentSnapshot) {

                            final String mCurrentId = FirebaseAuth.getInstance().getCurrentUser().getUid();

                            if (!documentSnapshot.getString("name").equals(usersList.get(holder.getAdapterPosition()).getName()) &&
                                    !documentSnapshot.getString("image").equals(usersList.get(holder.getAdapterPosition()).getImage())) {

                                holder.mBar.setVisibility(View.VISIBLE);
                                Map<String, Object> user = new HashMap<>();
                                user.put("name", documentSnapshot.getString("name"));
                                user.put("image", documentSnapshot.getString("image"));

                                FirebaseFirestore.getInstance()
                                        .collection("Users")
                                        .document(mCurrentId)
                                        .collection("Friend_Requests")
                                        .document(documentSnapshot.getString("id"))
                                        .update(user)
                                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                                            @Override
                                            public void onSuccess(Void aVoid) {
                                                Log.i("friend_req_update", "success");
                                                holder.mBar.setVisibility(View.GONE);
                                            }
                                        })
                                        .addOnFailureListener(new OnFailureListener() {
                                            @Override
                                            public void onFailure(@NonNull Exception e) {
                                                Log.i("friend_req_update", "failure");
                                            }
                                        });

                                holder.name.setText(documentSnapshot.getString("name"));

                                Glide.with(context)
                                        .setDefaultRequestOptions(new RequestOptions().placeholder(R.drawable.default_user_art_g_2))
                                        .load(documentSnapshot.getString("image"))
                                        .into(holder.image);


                            } else if (!documentSnapshot.getString("name").equals(usersList.get(holder.getAdapterPosition()).getName())) {

                                holder.mBar.setVisibility(View.VISIBLE);
                                Map<String, Object> user = new HashMap<>();
                                user.put("name", documentSnapshot.getString("name"));

                                FirebaseFirestore.getInstance()
                                        .collection("Users")
                                        .document(mCurrentId)
                                        .collection("Friend_Requests")
                                        .document(documentSnapshot.getString("id"))
                                        .update(user)
                                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                                            @Override
                                            public void onSuccess(Void aVoid) {
                                                Log.i("friend_req_update", "success");
                                                holder.mBar.setVisibility(View.GONE);
                                            }
                                        })
                                        .addOnFailureListener(new OnFailureListener() {
                                            @Override
                                            public void onFailure(@NonNull Exception e) {
                                                Log.i("friend_req_update", "failure");
                                            }
                                        });


                                holder.name.setText(documentSnapshot.getString("name"));

                            } else if (!documentSnapshot.getString("image").equals(usersList.get(holder.getAdapterPosition()).getImage())) {

                                holder.mBar.setVisibility(View.VISIBLE);
                                Map<String, Object> user = new HashMap<>();
                                user.put("image", documentSnapshot.getString("image"));

                                FirebaseFirestore.getInstance()
                                        .collection("Users")
                                        .document(mCurrentId)
                                        .collection("Friend_Requests")
                                        .document(documentSnapshot.getString("id"))
                                        .update(user)
                                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                                            @Override
                                            public void onSuccess(Void aVoid) {
                                                Log.i("friend_req_update", "success");
                                                holder.mBar.setVisibility(View.GONE);
                                            }
                                        })
                                        .addOnFailureListener(new OnFailureListener() {
                                            @Override
                                            public void onFailure(@NonNull Exception e) {
                                                Log.i("friend_req_update", "failure");
                                            }
                                        });


                                Glide.with(context)
                                        .setDefaultRequestOptions(new RequestOptions().placeholder(R.drawable.default_user_art_g_2))
                                        .load(documentSnapshot.getString("image"))
                                        .into(holder.image);

                            }

                        }
                    }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Log.e("Error", e.getMessage());
                }
            });
        }catch (Exception ex){
            Log.w("error","fastscrolled",ex);
        }

        holder.mView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                FriendProfile.startActivity(context,usersList.get(holder.getAdapterPosition()).userId);
            }
        });

    }


    @Override
    public int getItemCount() {
        return usersList.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        private View mView;
        private CircleImageView image;
        private ProgressBar mBar;
        private TextView name,timestamp;

        public ViewHolder(View itemView) {
            super(itemView);

            mView =itemView;
            image = mView.findViewById(R.id.userimage);
            name=mView.findViewById(R.id.username);
            timestamp=mView.findViewById(R.id.timestamp);
            mBar=mView.findViewById(R.id.progressBar);

        }
    }
}
