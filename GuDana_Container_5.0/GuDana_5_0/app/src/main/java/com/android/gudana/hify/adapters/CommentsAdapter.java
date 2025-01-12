package com.android.gudana.hify.adapters;

import android.app.ProgressDialog;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.android.gudana.hify.models.Comment;
import com.android.gudana.hify.ui.activities.friends.FriendProfile;
import com.android.gudana.R;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.github.marlonlom.utilities.timeago.TimeAgo;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;

/**
 * Created by amsavarthan on 22/2/18.
 */

public class CommentsAdapter extends RecyclerView.Adapter<CommentsAdapter.ViewHolder> {

    private List<Comment> commentList;
    private Context context;
    private FirebaseFirestore mFirestore;
    private FirebaseUser mCurrentUser;
    private boolean isOwner;

    public CommentsAdapter(List<Comment> commentList, Context context, boolean owner) {
        this.commentList = commentList;
        this.context = context;
        this.isOwner=owner;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.hi_item_comment, parent, false);
        mFirestore = FirebaseFirestore.getInstance();
        mCurrentUser= FirebaseAuth.getInstance().getCurrentUser();
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull final ViewHolder holder, int position) {

       if(isOwner){
           enableDeletion(holder);
       }else{

           if (commentList.get(position).getId().equals(mCurrentUser.getUid())){
              enableDeletion(holder);
           }

       }

        holder.username.setText(commentList.get(position).getUsername());
       holder.username.setOnClickListener(new View.OnClickListener() {
           @Override
           public void onClick(View v) {
               FriendProfile.startActivity(context,commentList.get(holder.getAdapterPosition()).getId());
           }
       });

       holder.image.setOnClickListener(new View.OnClickListener() {
           @Override
           public void onClick(View v) {
               FriendProfile.startActivity(context,commentList.get(holder.getAdapterPosition()).getId());
           }
       });

        Glide.with(context)
                .setDefaultRequestOptions(new RequestOptions().placeholder(R.drawable.default_user_art_g_6))
                .load(commentList.get(position).getImage())
                .into(holder.image);

        holder.comment.setText(commentList.get(position).getComment());

        String timeAgo = TimeAgo.using(Long.parseLong(commentList.get(position).getTimestamp()));
        holder.timestamp.setText(String.format(Locale.ENGLISH,"Commented %s", timeAgo));

        try {
            mFirestore.collection("Users")
                    .document(commentList.get(position).getId())
                    .get()
                    .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                        @Override
                        public void onSuccess(final DocumentSnapshot documentSnapshot) {

                            try {
                                if (!documentSnapshot.getString("username").equals(commentList.get(holder.getAdapterPosition()).getUsername()) &&
                                        !documentSnapshot.getString("image").equals(commentList.get(holder.getAdapterPosition()).getImage())) {

                                    Map<String, Object> commentMap = new HashMap<>();
                                    commentMap.put("username", documentSnapshot.getString("username"));
                                    commentMap.put("image", documentSnapshot.getString("image"));

                                    mFirestore.collection("Posts")
                                            .document(commentList.get(holder.getAdapterPosition()).getPost_id())
                                            .collection("Comments")
                                            .document(commentList.get(holder.getAdapterPosition()).commentId)
                                            .update(commentMap)
                                            .addOnSuccessListener(new OnSuccessListener<Void>() {
                                                @Override
                                                public void onSuccess(Void aVoid) {
                                                    Log.i("comment_update", "success");
                                                }
                                            })
                                            .addOnFailureListener(new OnFailureListener() {
                                                @Override
                                                public void onFailure(@NonNull Exception e) {
                                                    Log.i("comment_update", "failure");
                                                }
                                            });

                                    holder.username.setText(documentSnapshot.getString("username"));

                                    Glide.with(context)
                                            .setDefaultRequestOptions(new RequestOptions().placeholder(R.drawable.default_user_art_g_2))
                                            .load(documentSnapshot.getString("image"))
                                            .into(holder.image);


                                } else if (!documentSnapshot.getString("username").equals(commentList.get(holder.getAdapterPosition()).getUsername())) {


                                    Map<String, Object> commentMap = new HashMap<>();
                                    commentMap.put("username", documentSnapshot.getString("username"));

                                    mFirestore.collection("Posts")
                                            .document(commentList.get(holder.getAdapterPosition()).getPost_id())
                                            .collection("Comments")
                                            .document(commentList.get(holder.getAdapterPosition()).commentId)
                                            .update(commentMap)
                                            .addOnSuccessListener(new OnSuccessListener<Void>() {
                                                @Override
                                                public void onSuccess(Void aVoid) {
                                                    Log.i("comment_update", "success");
                                                }
                                            })
                                            .addOnFailureListener(new OnFailureListener() {
                                                @Override
                                                public void onFailure(@NonNull Exception e) {
                                                    Log.i("comment_update", "failure");
                                                }
                                            });

                                    holder.username.setText(documentSnapshot.getString("username"));

                                } else if (!documentSnapshot.getString("image").equals(commentList.get(holder.getAdapterPosition()).getImage())) {

                                    Map<String, Object> commentMap = new HashMap<>();
                                    commentMap.put("image", documentSnapshot.getString("image"));

                                    mFirestore.collection("Posts")
                                            .document(commentList.get(holder.getAdapterPosition()).getPost_id())
                                            .collection("Comments")
                                            .document(commentList.get(holder.getAdapterPosition()).commentId)
                                            .update(commentMap)
                                            .addOnSuccessListener(new OnSuccessListener<Void>() {
                                                @Override
                                                public void onSuccess(Void aVoid) {
                                                    Log.i("comment_update", "success");
                                                }
                                            })
                                            .addOnFailureListener(new OnFailureListener() {
                                                @Override
                                                public void onFailure(@NonNull Exception e) {
                                                    Log.i("comment_update", "failure");
                                                }
                                            });

                                    Glide.with(context)
                                            .setDefaultRequestOptions(new RequestOptions().placeholder(R.drawable.default_user_art_g_2))
                                            .load(documentSnapshot.getString("image"))
                                            .into(holder.image);

                                }


                            }catch(Exception e){
                                e.printStackTrace();
                            }
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Log.e("Error", e.getMessage());
                        }
                    });
        }catch (Exception ex){
            Log.w("error","fastscrolled",ex);
        }

    }

    private void enableDeletion(final ViewHolder holder) {

        holder.delete.setVisibility(View.VISIBLE);
        holder.delete.setAlpha(0.0f);

        holder.delete.animate()
                .alpha(1.0f)
                .start();

        holder.delete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new MaterialDialog.Builder(context)
                        .title("Delete comment")
                        .content("Are you sure do you want to delete your comment?")
                        .positiveText("Yes")
                        .negativeText("No")
                        .onPositive(new MaterialDialog.SingleButtonCallback() {
                            @Override
                            public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                dialog.dismiss();

                                final ProgressDialog progressDialog=new ProgressDialog(context);
                                progressDialog.setMessage("Deleting comment...");
                                progressDialog.setIndeterminate(true);
                                progressDialog.show();

                                mFirestore.collection("Posts")
                                        .document(commentList.get(holder.getAdapterPosition()).getPost_id())
                                        .collection("Comments")
                                        .document(commentList.get(holder.getAdapterPosition()).commentId)
                                        .delete()
                                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                                            @Override
                                            public void onSuccess(Void aVoid) {
                                                notifyItemRemoved(holder.getAdapterPosition());
                                                notifyDataSetChanged();
                                                progressDialog.dismiss();
                                                Toast.makeText(context, "Comment deleted", Toast.LENGTH_SHORT).show();
                                            }
                                        })
                                        .addOnFailureListener(new OnFailureListener() {
                                            @Override
                                            public void onFailure(@NonNull Exception e) {
                                                progressDialog.dismiss();
                                                Toast.makeText(context, "Error deleting comment: "+e.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                                                Log.w("Error","delete comment",e);
                                            }
                                        });

                            }
                        })
                        .onNegative(new MaterialDialog.SingleButtonCallback() {
                            @Override
                            public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                dialog.dismiss();
                            }
                        })
                        .show();
            }
        });

    }

    @Override
    public int getItemCount() {
        return commentList.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        private View mView;
        private CircleImageView image;
        private TextView username, comment, timestamp;
        private ImageView delete;

        public ViewHolder(View itemView) {
            super(itemView);

            mView = itemView;
            image = mView.findViewById(R.id.comment_user_image);
            username = mView.findViewById(R.id.comment_username);
            comment = mView.findViewById(R.id.comment_text);
            timestamp = mView.findViewById(R.id.comment_timestamp);
            delete=mView.findViewById(R.id.delete);

        }
    }
}
